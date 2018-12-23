package ru.mail.polis.turenkoaa;

import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.HttpSession;
import one.nio.http.Response;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.turenkoaa.util.ClusterInfo;
import ru.mail.polis.turenkoaa.util.PreparedRequest;
import ru.mail.polis.turenkoaa.util.ReplicaResponse;
import ru.mail.polis.turenkoaa.util.ServiceHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static one.nio.http.Response.GATEWAY_TIMEOUT;
import static ru.mail.polis.turenkoaa.util.ServiceHelper.getNodesById;
import static ru.mail.polis.turenkoaa.util.ServiceHelper.replicaRequestHeaders;

public class GetResolver {
    private final ClusterInfo cluster;

    public GetResolver(ClusterInfo clusterInfo) {
        this.cluster = clusterInfo;
    }

    public void resolveGet(@NotNull final HttpSession session, @NotNull final PreparedRequest query) throws IOException, InterruptedException, HttpException {
        if (query.isRequestForReplica()){
            getAsReplica(session, query);
        }   else {
            getWithReplicas(session, query);
        }
    }

    private void getWithReplicas(@NotNull HttpSession session, @NotNull final PreparedRequest query) throws IOException, HttpException, InterruptedException {
        List<Integer> nodes = getNodesById(query.getId(), query.getFrom(), cluster.replicas().size());
        nodes.stream()
            .map(node -> node == cluster.nodeId() ? ownGetResponse(query) : replicaGetResponse(query, node))
            .map(ServiceHelper::joinFutureExceptionally)
            .reduce(this::combineResponses)
            .ifPresent(response -> responseClient(session, query, response));
    }

    private void responseClient(@NotNull HttpSession session, @NotNull PreparedRequest query, ReplicaResponse response) {
        try {
            if (response.getSuccessAck() >= query.getAck()) {
                if (response.getNotFound() == response.getSuccessAck() || response.getRemoved() > 0)
                    session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
                else if (response.getValue() != null) {
                    session.sendResponse(Response.ok(response.getValue()));
                }
                else {
                    session.sendResponse(new Response(GATEWAY_TIMEOUT, Response.EMPTY));
                }
            } else {
                session.sendResponse(new Response(GATEWAY_TIMEOUT, Response.EMPTY));
            }
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    private ReplicaResponse combineResponses(ReplicaResponse r1, ReplicaResponse r2) {
        return new ReplicaResponse()
                .notFound(r1.getNotFound() + r2.getNotFound())
                .removed(r1.getRemoved() + r2.getRemoved())
                .successAck(r1.getSuccessAck() + r2.getSuccessAck())
                .value(r1.getValue() != null ? r1.getValue() : r2.getValue());
    }

    private CompletableFuture<ReplicaResponse> replicaGetResponse(@NotNull PreparedRequest query, Integer node) {
        return supplyAsync(() -> {
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
        });
    }

    private CompletableFuture<ReplicaResponse> ownGetResponse(@NotNull PreparedRequest query) {
        return supplyAsync(() -> {
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
        });

    }

    private void getAsReplica(@NotNull HttpSession session, @NotNull final PreparedRequest query) throws IOException {
        try {
            final String id = query.getId();

            if (cluster.removedIds().contains(id)) {
                session.sendResponse(new Response(Response.FORBIDDEN, Response.EMPTY));
            }

            final byte[] value;
            if (id != null) {
                value = cluster.dao().get(id.getBytes());
                session.sendResponse(Response.ok(value));
            }
        } catch (NoSuchElementException e) {
            session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
        }
    }
}
