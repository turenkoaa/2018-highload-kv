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

public class DeleteResolver {
    private static Logger logger = Logger.getLogger(DeleteResolver.class.getName());

    private final ClusterInfo cluster;

    public DeleteResolver(ClusterInfo clusterInfo) {
        this.cluster = clusterInfo;
    }

    public void resolveDelete(@NotNull final HttpSession session, @NotNull final PreparedRequest query) throws IOException, InterruptedException, HttpException {
        cluster.removedIds().add(query.getId());
        if (query.isRequestForReplica()){
            delete(session, query);
        }   else {
            deleteWithReplicas(session, query);
        }
    }

    private void deleteWithReplicas(HttpSession session, @NotNull final PreparedRequest query) throws IOException, HttpException, InterruptedException {
        List<Integer> nodes = getNodesById(query.getId(), query.getFrom(), cluster.replicas().size());

        int successAck = 0;

        for (Integer node : nodes) {
            if (node == cluster.nodeId()) {
                cluster.dao().remove(query.getId().getBytes());
                successAck++;
            } else {
                HttpClient replica = cluster.replicas().get(node);
                try {
                    Response response = replica.delete(query.getUri(), replicaRequestHeaders);
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

    private void delete(@NotNull HttpSession session, @NotNull final PreparedRequest query) throws IOException {
        String id = query.getId();
        if (id != null) {
            cluster.dao().remove(id.getBytes());
            session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
        }
    }
}
