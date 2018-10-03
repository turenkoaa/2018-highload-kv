package ru.mail.polis.turenkoaa;

import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.polis.KVDao;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.NoSuchElementException;

public class EntityController implements Controller {
    private static final String ENTITY_PATH = "/v0/entity";
    @NotNull
    private final KVDao dao;

    public EntityController(@NotNull KVDao dao) {
        this.dao = dao;
    }

    @Override
    public String getPath() {
        return ENTITY_PATH;
    }

    @SneakyThrows
    public void resolveGet(@NotNull final Request request, @NotNull final HttpSession session) {
        try {
            final String id = getParameter(request, session, "id=");
            final byte[] value;
            if (id != null) {
                value = dao.get(id.getBytes());
                session.sendResponse(Response.ok(value));
            }
        } catch (NoSuchElementException e) {
            session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
        }
    }

    @SneakyThrows
    public void resolvePut(@NotNull final Request request, @NotNull final HttpSession session)  {
        final String id = getParameter(request, session, "id=");
        if (id != null) {
            dao.upsert(id.getBytes(), request.getBody());
            session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
        }
    }

    @SneakyThrows
    public void resolveDelete(@NotNull final Request request, @NotNull final HttpSession session) {
        final String id = getParameter(request, session, "id=");
        if (id != null) {
            dao.remove(id.getBytes());
            session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
        }
    }

    @Nullable
    private String getParameter(@NotNull Request request, @NotNull HttpSession session, String parameter) throws IOException {
        final String id = request.getParameter(parameter);
        if (id == null || id.isEmpty()){
            badRequest(request, session);
            return null;
        }
        return id;
    }
}
