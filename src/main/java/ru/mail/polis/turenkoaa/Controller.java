package ru.mail.polis.turenkoaa;

import lombok.SneakyThrows;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

public interface Controller {
    String getPath();
    void resolveGet(@NotNull final Request request, @NotNull final HttpSession session);
    void resolvePut(@NotNull final Request request, @NotNull final HttpSession session);
    void resolveDelete(@NotNull final Request request, @NotNull final HttpSession session);

    @SneakyThrows
    default void notSupported(@NotNull final Request request, @NotNull final HttpSession session) {
        session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
    }

    @SneakyThrows
    default void badRequest(@NotNull final Request request, @NotNull final HttpSession session) {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }
}
