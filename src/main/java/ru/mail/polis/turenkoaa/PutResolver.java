package ru.mail.polis.turenkoaa;

import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.HttpSession;
import one.nio.http.Response;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.turenkoaa.util.ClusterInfo;
import ru.mail.polis.turenkoaa.util.PreparedRequest;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;
import static one.nio.http.Response.GATEWAY_TIMEOUT;
import static ru.mail.polis.turenkoaa.util.ServiceHelper.getNodesById;
import static ru.mail.polis.turenkoaa.util.ServiceHelper.replicaRequestHeaders;

public class PutResolver {
    private static Logger logger = Logger.getLogger(PutResolver.class.getName());

    private final ClusterInfo cluster;

    public PutResolver(ClusterInfo clusterInfo) {
        this.cluster = clusterInfo;
    }

    public void resolvePut(@NotNull final HttpSession session, @NotNull final PreparedRequest query) throws InterruptedException, HttpException, PoolException, IOException {
        if (query.isRequestForReplica()){
            put(session, query);
        }   else {
            putWithReplicas(session, query);
        }
    }

    private void putWithReplicas(HttpSession session, @NotNull final PreparedRequest query) throws IOException, InterruptedException, HttpException {
        List<Integer> nodes = getNodesById(query.getId(), query.getFrom(), cluster.replicas().size());

        int successAck = 0;

        for (Integer node : nodes) {
            byte[] body = query.getBody();
            if (node == cluster.nodeId()) {
                cluster.dao().upsert(query.getId().getBytes(), body);
                successAck++;
            } else {
                HttpClient replica = cluster.replicas().get(node);

                try {
                    Response response = replica.put(query.getUri(), body, replicaRequestHeaders);
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

    private void put(@NotNull HttpSession session, @NotNull final PreparedRequest query) throws IOException {
        final String id = query.getId();
        if (id != null) {
            cluster.dao().upsert(id.getBytes(), query.getBody());
            session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
        }
    }
}
