package ru.mail.polis.turenkoaa.util;

import one.nio.http.HttpClient;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVDao;

import java.util.Map;
import java.util.Set;

public class ClusterInfo {
    @NotNull
    private final KVDao dao;
    private final int nodeId;
    private final Map<Integer, HttpClient> replicas;
    private final Set<String> removedIds;

    public ClusterInfo(@NotNull KVDao dao, int nodeId, Map<Integer, HttpClient> replicas, Set<String> removedIds) {
        this.dao = dao;
        this.nodeId = nodeId;
        this.replicas = replicas;
        this.removedIds = removedIds;
    }

    @NotNull
    public KVDao dao() {
        return dao;
    }

    public int nodeId() {
        return nodeId;
    }

    public Map<Integer, HttpClient> replicas() {
        return replicas;
    }

    public Set<String> removedIds() {
        return removedIds;
    }
}
