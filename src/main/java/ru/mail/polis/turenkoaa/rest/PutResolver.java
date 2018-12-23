package ru.mail.polis.turenkoaa.rest;

import lombok.SneakyThrows;
import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.HttpSession;
import one.nio.http.Response;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.turenkoaa.model.ClusterSettings;
import ru.mail.polis.turenkoaa.model.PreparedRequest;
import ru.mail.polis.turenkoaa.model.ReplicaResponse;
import ru.mail.polis.turenkoaa.util.ClusterUtil;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.logging.Logger;

import static ru.mail.polis.turenkoaa.util.ClusterUtil.getNodesById;
import static ru.mail.polis.turenkoaa.util.ErrorSender.created;
import static ru.mail.polis.turenkoaa.util.ErrorSender.gatewayTimeout;
import static ru.mail.polis.turenkoaa.util.RequestUtil.replicaRequestHeaders;

public class PutResolver {
    private static Logger logger = Logger.getLogger(PutResolver.class.getName());

    private final ClusterSettings cluster;

    public PutResolver(ClusterSettings clusterSettings) {
        this.cluster = clusterSettings;
    }

    public void resolvePut(@NotNull final HttpSession session, @NotNull final PreparedRequest query) {
        if (query.isRequestForReplica()) {
            putAsReplica(session, query);
        } else {
            putWithReplicas(session, query);
        }
    }

    private void putWithReplicas(@NotNull HttpSession session, @NotNull final PreparedRequest query) {
        List<Integer> nodes = getNodesById(query.getId(), query.getFrom(), cluster.replicas().size());
        nodes.parallelStream()
            .map(node -> node == cluster.nodeId() ? createOwnPutResponse(query) : createReplicaPutResponse(query, node))
            .reduce(ClusterUtil::combineResponses)
            .ifPresent(response -> sendResponse(session, query, response));
    }

    private void sendResponse(@NotNull HttpSession session, @NotNull PreparedRequest query, ReplicaResponse response) {
        if (response.getSuccessAck() >= query.getAck()) {
            created(session);
        } else {
            gatewayTimeout(session);
        }
    }

    private ReplicaResponse createReplicaPutResponse(@NotNull PreparedRequest query, int node) {
        ReplicaResponse response = new ReplicaResponse();
        try {
            HttpClient replica = cluster.replicas().get(node);
            byte[] body = query.getBody();
            Response httpResponse = replica.put(query.getUri(), body, replicaRequestHeaders);
            if (httpResponse.getStatus() == 201)
                response.successAck(1);
            return response;
        } catch (PoolException e) {
            return response;
        } catch (InterruptedException | IOException | HttpException e) {
            throw new CompletionException(e);
        }
    }

    private ReplicaResponse createOwnPutResponse(@NotNull PreparedRequest query) {
        ReplicaResponse response = new ReplicaResponse();
        byte[] body = query.getBody();
        try {
            cluster.dao().upsert(query.getId().getBytes(), body);
        } catch (IOException e) {
            throw new CompletionException(e);
        }
        response.successAck(1);
        return response;
    }

    @SneakyThrows
    private void putAsReplica(@NotNull HttpSession session, @NotNull final PreparedRequest query) {
        final String id = query.getId();
        if (id != null) {
            cluster.dao().upsert(id.getBytes(), query.getBody());
            created(session);
        }
    }
}
