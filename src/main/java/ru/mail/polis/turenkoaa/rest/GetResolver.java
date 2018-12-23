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
import java.util.NoSuchElementException;
import java.util.concurrent.CompletionException;

import static ru.mail.polis.turenkoaa.util.ClusterUtil.getNodesById;
import static ru.mail.polis.turenkoaa.util.ErrorSender.*;
import static ru.mail.polis.turenkoaa.util.RequestUtil.replicaRequestHeaders;

public class GetResolver {
    private final ClusterSettings cluster;

    public GetResolver(ClusterSettings clusterSettings) {
        this.cluster = clusterSettings;
    }

    public void resolveRequest(@NotNull final HttpSession session, @NotNull final PreparedRequest query) {
        if (query.isRequestForReplica()){
            getAsReplica(session, query);
        }   else {
            getWithReplicas(session, query);
        }
    }

    private void getWithReplicas(@NotNull HttpSession session, @NotNull final PreparedRequest query) {
        List<Integer> nodes = getNodesById(query.getId(), query.getFrom(), cluster.replicas().size());
        nodes.parallelStream()
            .map(node -> node == cluster.nodeId() ? createOwnGetResponse(query) : createReplicaGetResponse(query, node))
            .reduce(ClusterUtil::combineResponses)
            .ifPresent(response -> sendResponse(session, query, response));
    }

    private void sendResponse(@NotNull HttpSession session, @NotNull PreparedRequest query, ReplicaResponse response) {
        if (response.getSuccessAck() >= query.getAck()) {
            if (response.getNotFound() == response.getSuccessAck() || response.getRemoved() > 0) notFound(session);
            else if (response.getValue() != null) ok(session, response.getValue());
            else gatewayTimeout(session);
        } else {
            gatewayTimeout(session);
        }
    }

    private ReplicaResponse createReplicaGetResponse(@NotNull PreparedRequest query, int node) {
        ReplicaResponse response = new ReplicaResponse();
        try {
            HttpClient replica = cluster.replicas().get(node);
            Response httpResponse = replica.get(query.getUri(), replicaRequestHeaders);
            if (httpResponse.getStatus() == 200) {
                response.value(httpResponse.getBody());
            } else if (httpResponse.getStatus() == 403) {
                response.removed(1);
            } else if (httpResponse.getStatus() == 404) {
                response.notFound(1);
            }
            response.successAck(1);
            return response;
        } catch (PoolException e) {
            return response;
        } catch (InterruptedException | HttpException | IOException e) {
            throw new CompletionException(e);
        }

    }

    private ReplicaResponse createOwnGetResponse(@NotNull PreparedRequest query) {
        ReplicaResponse response = new ReplicaResponse();
        try {
            if (cluster.removedIds().contains(query.getId())) {
                response.removed(1);
            } else {
                response.value(cluster.dao().get(query.getId().getBytes()));
            }
        } catch (NoSuchElementException e) {
            response.notFound(1);
        } catch (IOException e) {
            throw new CompletionException(e);
        }
        response.successAck(1);
        return response;


    }

    @SneakyThrows
    private void getAsReplica(@NotNull HttpSession session, @NotNull final PreparedRequest query) {
        try {
            final String id = query.getId();

            if (cluster.removedIds().contains(id)) {
                forbidden(session);
            }

            final byte[] value;
            if (id != null) {
                value = cluster.dao().get(id.getBytes());
                ok(session, value);
            }
        } catch (NoSuchElementException e) {
            notFound(session);
        }
    }
}
