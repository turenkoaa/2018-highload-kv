package ru.mail.polis.turenkoaa;

import lombok.SneakyThrows;
import one.nio.http.Request;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class RESTResolverProvider {
    private final Controller controller;
    private final Map<Integer, RequestResolver> resolverMap;

    public RESTResolverProvider(Controller controller) {
        this.controller = controller;
        resolverMap = new HashMap<>();
        init(controller);
    }

    private void init(Controller controller) {
        resolverMap.put(Request.METHOD_GET, controller::resolveGet);
        resolverMap.put(Request.METHOD_PUT, controller::resolvePut);
        resolverMap.put(Request.METHOD_DELETE, controller::resolveDelete);
    }

    @SneakyThrows
    public RequestResolver getResolver(@NotNull final Request request){
        if (request.getPath().equals(controller.getPath())) {
            final int method = request.getMethod();
            return resolverMap.computeIfAbsent(method, smth -> controller::notSupported);
        } else {
            return controller::badRequest;
        }
    }
}
