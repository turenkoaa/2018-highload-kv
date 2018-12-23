package ru.mail.polis.turenkoaa;

import one.nio.http.*;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVDao;
import ru.mail.polis.KVService;
import ru.mail.polis.turenkoaa.util.ClusterInfo;
import ru.mail.polis.turenkoaa.util.PreparedRequest;

import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.toMap;
import static one.nio.http.Response.*;
import static ru.mail.polis.turenkoaa.util.ServiceHelper.*;

public class KVEntityService extends HttpServer implements KVService {
    private final Map<Integer, RestResolver> resolverMap;
    private final ClusterInfo clusterInfo;

    public KVEntityService(final int port, final KVDao dao, Set<String> topology) throws IOException {
        super(from(port));

        ArrayList<String> nodePaths = new ArrayList<>(topology);
        int nodeId = nodePaths.indexOf(NODE_PATH + port);
        Map<Integer, HttpClient> replicas = extractReplicas(port, nodePaths);

        clusterInfo = new ClusterInfo(dao, nodeId, replicas, new HashSet<>());

        GetResolver getResolver = new GetResolver(clusterInfo);
        PutResolver putResolver = new PutResolver(clusterInfo);
        DeleteResolver deleteResolver = new DeleteResolver(clusterInfo);

        resolverMap = new HashMap<>();
        resolverMap.put(Request.METHOD_GET, getResolver::resolveGet);
        resolverMap.put(Request.METHOD_PUT, putResolver::resolvePut);
        resolverMap.put(Request.METHOD_DELETE, deleteResolver::resolveDelete);
    }

    @Path(STATUS_PATH)
    public Response status() {
        return ok("Server starts ok");
    }

    private String getPath() {
        return ENTITY_PATH;
    }

    @NotNull
    private RestResolver restResolver(@NotNull Request request) {
        RestResolver restResolver;
        if (request.getPath().equals(getPath())) {
            final int method = request.getMethod();
            restResolver = resolverMap.computeIfAbsent(method, __ -> ErrorSender::notSupported);
        } else {
            restResolver = ErrorSender::badRequest;
        }
        return restResolver;
    }

    @Override
    public void handleDefault(@NotNull final Request request, @NotNull final HttpSession session) throws IOException {
        try {
            PreparedRequest query = prepareRequest(request, clusterInfo.replicas().size());
            restResolver(request).resolveEntityRequest(session, query);
        } catch (IllegalArgumentException e) {
            session.sendResponse(new Response(BAD_REQUEST, e.getMessage().getBytes()));
        } catch (Exception e) {
            session.sendResponse(new Response(INTERNAL_ERROR, e.getMessage().getBytes()));
        }

    }

}

