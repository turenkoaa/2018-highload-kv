package ru.mail.polis.turenkoaa.util;

import one.nio.http.HttpClient;
import one.nio.net.ConnectionString;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.turenkoaa.model.ReplicaResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toMap;

public class ClusterUtil {
    private static final Map<String, List<Integer>> nodeIdsCache = new ConcurrentHashMap<>();

    @NotNull
    public static List<Integer> getNodesById(@NotNull final String id, int from, int topologySize) {
        if (nodeIdsCache.containsKey(id + from)) {
            return nodeIdsCache.get(id + from);
        }
        List<Integer> nodes = new ArrayList<>();
        int hash = Math.abs(id.hashCode());
        for (int i = 0; i < from; i++) {
            int index = (hash + i) % topologySize;
            nodes.add(index);
        }
        nodeIdsCache.put(id + from, nodes);
        return nodes;
    }

    public static Map<Integer, HttpClient> extractReplicas(List<String> topology) {
        return topology.stream()
                .map(path -> new Pair<>(topology.indexOf(path), path))
                .collect(toMap(
                        Pair::getLeft,
                        pair -> createConnectionToReplica(pair.getRight()))

                );
    }

    public static ReplicaResponse combineResponses(ReplicaResponse r1, ReplicaResponse r2) {
        return new ReplicaResponse()
                .notFound(r1.getNotFound() + r2.getNotFound())
                .removed(r1.getRemoved() + r2.getRemoved())
                .successAck(r1.getSuccessAck() + r2.getSuccessAck())
                .value(r1.getValue() != null ? r1.getValue() : r2.getValue());
    }

    private static HttpClient createConnectionToReplica(String path){
        return new HttpClient(new ConnectionString(path));
    }


    private static class Pair<T, P> {
        private final T left;
        private final P right;

        public Pair(T left, P right) {
            this.left = left;
            this.right = right;
        }

        public T getLeft() {
            return left;
        }

        public P getRight() {
            return right;
        }
    }

}
