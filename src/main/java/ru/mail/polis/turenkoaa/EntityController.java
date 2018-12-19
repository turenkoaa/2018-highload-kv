package ru.mail.polis.turenkoaa;


import one.nio.http.*;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.polis.KVDao;
import lombok.SneakyThrows;
import ru.mail.polis.turenkoaa.util.PreparedRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;
import static one.nio.http.Response.GATEWAY_TIMEOUT;
import static org.apache.log4j.Level.ERROR;
import static ru.mail.polis.turenkoaa.util.ServiceHelper.*;

public class EntityController implements Controller {

    static Logger logger = Logger.getLogger(EntityController.class.getName());

    @NotNull
    private final KVDao dao;
    private final Map<Integer, RestResolver> resolverMap;
    private final int nodeId;
    private final Map<Integer, HttpClient> replicas;

    public EntityController(@NotNull KVDao dao, int nodeId, Map<Integer, HttpClient> replicas) {
        this.dao = dao;
        this.nodeId = nodeId;
        this.replicas = replicas;
        resolverMap = new HashMap<>();
        init();
    }

    @Override
    public String getPath() {
        return ENTITY_PATH;
    }

    private void init() {
        resolverMap.put(Request.METHOD_GET, this::resolveGet);
        resolverMap.put(Request.METHOD_PUT, this::resolvePut);
        resolverMap.put(Request.METHOD_DELETE, this::resolveDelete);
    }

    public void resolveRequest(@NotNull final Request request, @NotNull final HttpSession session) throws InterruptedException, HttpException, IOException, PoolException {
        final RestResolver restResolver;
        PreparedRequest query = prepareRequest(request.getQueryString(), replicas.size());
        if (request.getPath().equals(getPath())) {
            final int method = request.getMethod();
            restResolver = resolverMap.computeIfAbsent(method, __ -> this::notSupported);
        } else {
            restResolver = this::badRequest;
        }
        restResolver.resolveEntityRequest(request, session, query);
    }

    public void resolveGet(@NotNull final Request request, @NotNull final HttpSession session, @NotNull final PreparedRequest query) throws IOException, InterruptedException, PoolException, HttpException {
        if (Boolean.valueOf(request.getHeader(HEADER_REPLICA_REQUEST))){
            get(request, session, query);
        }   else {
            getWithReplicas(request, session, query);
        }
    }

    public void getWithReplicas(@NotNull Request request, @NotNull HttpSession session, @NotNull final PreparedRequest query) throws IOException, InterruptedException, HttpException {
        List<Integer> nodes = getNodesById(query.getId(), query.getFrom(), replicas.size());

        int successAck = 0;
        int notFoundCount = 0;
        byte[] value = null;

        for (Integer node : nodes) {
            if (node == nodeId) {
                try {
                    if (value == null)
                        value = dao.get(query.getId().getBytes());
                    successAck++;
                } catch (NoSuchElementException e) {
                    notFoundCount++;
                    successAck++;
                }
            } else {
                HttpClient replica = replicas.get(node);

                try {
                    Response response = replica.get(request.getURI(), replicaRequestHeaders);
                    if (response.getStatus() == 200) {
                        successAck++;
                        if (value == null)
                            value = response.getBody();
                    }
                    else if (response.getStatus() == 404){
                        notFoundCount++;
                        successAck++;
                    }
                } catch (PoolException e) {
                    logger.log(WARNING, e.toString());
                }
            }
        }

        if (successAck >= query.getAck()){
            if (notFoundCount > 0)
                session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
            else if (value != null)
                session.sendResponse(Response.ok(value));
            else
                session.sendResponse(new Response(GATEWAY_TIMEOUT, Response.EMPTY));
        }
        else
            session.sendResponse(new Response(GATEWAY_TIMEOUT, Response.EMPTY));
    }

    public void get(@NotNull Request request, @NotNull HttpSession session, @NotNull final PreparedRequest query) throws IOException {
        try {
            final String id = query.getId();
            final byte[] value;
            if (id != null) {
                value = dao.get(id.getBytes());
                session.sendResponse(Response.ok(value));
            }
        } catch (NoSuchElementException e) {
            session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
        }
    }

    public void resolvePut(@NotNull final Request request, @NotNull final HttpSession session, @NotNull final PreparedRequest query) throws InterruptedException, HttpException, PoolException, IOException {
        if (Boolean.valueOf(request.getHeader(HEADER_REPLICA_REQUEST))){
            put(request, session, query);
        }   else {
            putWithReplicas(request, session, query);
        }
    }

    private void putWithReplicas(Request request, HttpSession session, @NotNull final PreparedRequest query) throws IOException, InterruptedException, HttpException, PoolException {
        List<Integer> nodes = getNodesById(query.getId(), query.getFrom(), replicas.size());

        int successAck = 0;

        for (Integer node : nodes) {
            byte[] body = request.getBody();
            if (node == nodeId) {
                dao.upsert(query.getId().getBytes(), body);
                successAck++;
            } else {
                HttpClient replica = replicas.get(node);

                try {
                    Response response = replica.put(request.getURI(), body, replicaRequestHeaders);
                    if (response.getStatus() == 201)
                        successAck++;
                } catch (PoolException e) {
                    logger.log(WARNING, e.toString());
                }


            }
        }
        if (successAck >= query.getAck()){
            session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
        }
        else {
            session.sendResponse(new Response(GATEWAY_TIMEOUT, Response.EMPTY));
        }
    }

    public void put(@NotNull Request request, @NotNull HttpSession session, @NotNull final PreparedRequest query) throws IOException {
        final String id = query.getId();
        if (id != null) {
            dao.upsert(id.getBytes(), request.getBody());
            session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
        }
    }

    public void resolveDelete(@NotNull final Request request, @NotNull final HttpSession session, @NotNull final PreparedRequest query) throws IOException, InterruptedException, PoolException, HttpException {
        if (Boolean.valueOf(request.getHeader(HEADER_REPLICA_REQUEST))){
            delete(request, session, query);
        }   else {
            deleteWithReplicas(request, session, query);
        }
    }

    private void deleteWithReplicas(Request request, HttpSession session, @NotNull final PreparedRequest query) throws IOException, InterruptedException, HttpException {
        List<Integer> nodes = getNodesById(query.getId(), query.getFrom(), replicas.size());

        int successAck = 0;

        for (Integer node : nodes) {
            if (node == nodeId) {
                dao.remove(query.getId().getBytes());
                successAck++;
            } else {
                HttpClient replica = replicas.get(node);
                try {
                    Response response = replica.delete(request.getURI(), replicaRequestHeaders);
                    if (response.getStatus() == 202)
                        successAck++;
                } catch (PoolException e) {
                    logger.log(WARNING, e.toString());
                }
            }
        }
        if (successAck >= query.getAck()){
            session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
        }
        else {
            session.sendResponse(new Response(GATEWAY_TIMEOUT, Response.EMPTY));
        }
    }

    public void delete(@NotNull Request request, @NotNull HttpSession session, @NotNull final PreparedRequest query) throws IOException {
        String id = query.getId();
        if (id != null) {
            dao.remove(id.getBytes());
            session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
        }
    }

}