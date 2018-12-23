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
import static ru.mail.polis.turenkoaa.util.ErrorSender.accepted;
import static ru.mail.polis.turenkoaa.util.ErrorSender.gatewayTimeout;
import static ru.mail.polis.turenkoaa.util.RequestUtil.replicaRequestHeaders;

public class DeleteResolver {
    private static Logger logger = Logger.getLogger(DeleteResolver.class.getName());

    private final ClusterSettings cluster;

    public DeleteResolver(ClusterSettings clusterSettings) {
        this.cluster = clusterSettings;
    }

    public void resolveDelete(@NotNull final HttpSession session, @NotNull final PreparedRequest query) {
        cluster.removedIds().add(query.getId());
        if (query.isRequestForReplica()){
            deleteAsReplica(session, query);
        }   else {
            deleteWithReplicas(session, query);
        }
    }

    private void deleteWithReplicas(@NotNull HttpSession session, @NotNull final PreparedRequest query) {
        List<Integer> nodes = getNodesById(query.getId(), query.getFrom(), cluster.replicas().size());

        nodes.parallelStream()
                .map(node -> node == cluster.nodeId() ? createOwnDeleteResponse(query) : createReplicaDeleteResponse(query, node))
                .reduce(ClusterUtil::combineResponses)
                .ifPresent(response -> sendResponse(session, query, response));
    }

    private void sendResponse(@NotNull HttpSession session, @NotNull PreparedRequest query, ReplicaResponse response) {
        if (response.getSuccessAck() >= query.getAck()) {
            accepted(session);
        } else {
            gatewayTimeout(session);
        }
    }

    private ReplicaResponse createReplicaDeleteResponse(@NotNull PreparedRequest query, int node) {
        ReplicaResponse response = new ReplicaResponse();
        try {
            HttpClient replica = cluster.replicas().get(node);

            Response httpResponse = replica.delete(query.getUri(), replicaRequestHeaders);
            if (httpResponse.getStatus() == 202)
                response.successAck(1);
            return response;
        } catch (PoolException e) {
            return response;
        } catch (InterruptedException | IOException | HttpException e) {
            throw new CompletionException(e);
        }
    }

    private ReplicaResponse createOwnDeleteResponse(@NotNull PreparedRequest query) {
        ReplicaResponse response = new ReplicaResponse();
        try {
            cluster.dao().remove(query.getId().getBytes());
        } catch (IOException e) {
            throw new CompletionException(e);
        }
        response.successAck(1);
        return response;
    }

    @SneakyThrows
    private void deleteAsReplica(@NotNull HttpSession session, @NotNull final PreparedRequest query) {
        String id = query.getId();
        if (id != null) {
            cluster.dao().remove(id.getBytes());
            accepted(session);
        }
    }
}
