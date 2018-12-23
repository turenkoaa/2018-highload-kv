package ru.mail.polis.turenkoaa.util;

import lombok.SneakyThrows;
import one.nio.http.HttpSession;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.turenkoaa.model.PreparedRequest;

import static one.nio.http.Response.GATEWAY_TIMEOUT;

public class ErrorSender {
    @SneakyThrows
    public static void notSupported(@NotNull final HttpSession session, @NotNull final PreparedRequest query) {
        session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
    }

    @SneakyThrows
    public static void badRequest(@NotNull final HttpSession session, @NotNull final PreparedRequest query) {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    @SneakyThrows
    public static void gatewayTimeout(@NotNull final HttpSession session) {
        session.sendResponse(new Response(GATEWAY_TIMEOUT, Response.EMPTY));
    }

    @SneakyThrows
    public static void notFound(@NotNull final HttpSession session) {
        session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
    }

    @SneakyThrows
    public static void forbidden(@NotNull final HttpSession session) {
        session.sendResponse(new Response(Response.FORBIDDEN, Response.EMPTY));
    }

    @SneakyThrows
    public static void ok(@NotNull final HttpSession session, byte[] body) {
        session.sendResponse(Response.ok(body));
    }

    @SneakyThrows
    public static void accepted(@NotNull final HttpSession session) {
        session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
    }

    @SneakyThrows
    public static void created(@NotNull final HttpSession session) {
        session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
    }
}
