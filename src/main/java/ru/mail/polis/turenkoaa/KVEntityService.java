package ru.mail.polis.turenkoaa;

import one.nio.http.*;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVDao;
import ru.mail.polis.KVService;
import ru.mail.polis.turenkoaa.model.ClusterSettings;
import ru.mail.polis.turenkoaa.model.PreparedRequest;
import ru.mail.polis.turenkoaa.rest.RestMethodResolver;
import ru.mail.polis.turenkoaa.rest.RestResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import static one.nio.http.Response.*;
import static ru.mail.polis.turenkoaa.util.ClusterUtil.extractReplicas;
import static ru.mail.polis.turenkoaa.util.RequestUtil.*;

public class KVEntityService extends HttpServer implements KVService {
    private final ClusterSettings clusterSettings;
    private final RestResolver restResolver;

    public KVEntityService(final int port, final KVDao dao, final Set<String> topology) throws IOException {
        super(from(port));
        clusterSettings = getClusterSettings(port, dao, topology);
        restResolver = new RestResolver(clusterSettings);
    }

    private ClusterSettings getClusterSettings(final int port, final KVDao dao, final Set<String> topology) {
        List<String> nodePaths = new ArrayList<>(topology);
        int nodeId = nodePaths.indexOf(NODE_PATH + port);
        Map<Integer, HttpClient> replicas = extractReplicas(nodePaths);
        return new ClusterSettings(dao, nodeId, replicas, new ConcurrentSkipListSet<>());
    }

    @Path(STATUS_PATH)
    public Response status() {
        return ok("Server starts ok");
    }

    @Override
    public void handleDefault(@NotNull final Request request, @NotNull final HttpSession session) throws IOException {
        try {
            PreparedRequest query = prepareRequest(request, clusterSettings.replicas().size());
            RestMethodResolver resolver = restResolver.getResolver(request);
            resolver.resolveRequest(session, query);
        } catch (IllegalArgumentException e) {
            session.sendResponse(new Response(BAD_REQUEST, e.getMessage().getBytes()));
        } catch (Exception e) {
            session.sendResponse(new Response(INTERNAL_ERROR, e.getMessage().getBytes()));
        }
    }

}

