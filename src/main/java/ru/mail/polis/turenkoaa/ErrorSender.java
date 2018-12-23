package ru.mail.polis.turenkoaa;

import lombok.SneakyThrows;
import one.nio.http.HttpSession;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.turenkoaa.util.PreparedRequest;

public class ErrorSender {
    @SneakyThrows
    public static void notSupported(@NotNull final HttpSession session, @NotNull final PreparedRequest query) {
        session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
    }

    @SneakyThrows
    public static void badRequest(@NotNull final HttpSession session, @NotNull final PreparedRequest query) {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }
}
