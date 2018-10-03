package ru.mail.polis.turenkoaa;

import one.nio.http.*;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVDao;
import ru.mail.polis.KVService;

import java.io.IOException;

public class KVEntityService extends HttpServer implements KVService {
    private final RESTResolverProvider restResolver;
    private static final String STATUS_PATH = "/v0/status";

    public KVEntityService(final int port, final KVDao dao) throws IOException {
        super(from(port));
        EntityController controller = new EntityController(dao);
        restResolver = new RESTResolverProvider(controller);
    }

    @Path(STATUS_PATH)
    public Response status() {
        return Response.ok("Server starts ok");
    }

    @Override
    public void handleDefault(@NotNull final Request request, @NotNull final HttpSession session) throws IOException {
        try {
            restResolver.getResolver(request).resolveEntityRequest(request, session);
        } catch (Exception e) {
            session.sendResponse(new Response(Response.INTERNAL_ERROR, e.getMessage().getBytes()));
        }
    }

    private static HttpServerConfig from(final int port) {
        final AcceptorConfig ac = new AcceptorConfig();
        ac.port = port;
        HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{ac};
        return config;
    }
}

