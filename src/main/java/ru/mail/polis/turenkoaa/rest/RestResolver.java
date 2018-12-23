package ru.mail.polis.turenkoaa.rest;

import one.nio.http.Request;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.turenkoaa.model.ClusterSettings;
import ru.mail.polis.turenkoaa.util.ErrorSender;

import java.util.HashMap;
import java.util.Map;

import static ru.mail.polis.turenkoaa.util.RequestUtil.ENTITY_PATH;

public class RestResolver {
    private final Map<Integer, RestMethodResolver> resolverMap;

    public RestResolver(ClusterSettings clusterSettings) {
        this.resolverMap = new HashMap<>();
        GetResolver getResolver = new GetResolver(clusterSettings);
        PutResolver putResolver = new PutResolver(clusterSettings);
        DeleteResolver deleteResolver = new DeleteResolver(clusterSettings);

        resolverMap.put(Request.METHOD_GET, getResolver::resolveRequest);
        resolverMap.put(Request.METHOD_PUT, putResolver::resolvePut);
        resolverMap.put(Request.METHOD_DELETE, deleteResolver::resolveDelete);
    }

    @NotNull
    public RestMethodResolver getResolver(@NotNull Request request) {
        RestMethodResolver restMethodResolver;
        if (request.getPath().equals(ENTITY_PATH)) {
            final int method = request.getMethod();
            restMethodResolver = resolverMap.computeIfAbsent(method, __ -> ErrorSender::notSupported);
        } else {
            restMethodResolver = ErrorSender::badRequest;
        }
        return restMethodResolver;
    }

}
