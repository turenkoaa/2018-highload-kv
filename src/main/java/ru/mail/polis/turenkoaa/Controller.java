package ru.mail.polis.turenkoaa;

import lombok.SneakyThrows;
import one.nio.http.HttpException;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.turenkoaa.util.PreparedRequest;

import java.io.IOException;

public interface Controller {
    String getPath();
    void resolveGet(@NotNull final HttpSession session, @NotNull final PreparedRequest query) throws IOException, InterruptedException, PoolException, HttpException;
    void resolvePut(@NotNull final HttpSession session, @NotNull final PreparedRequest query) throws InterruptedException, HttpException, PoolException, IOException;
    void resolveDelete(@NotNull final HttpSession session, @NotNull final PreparedRequest query) throws IOException, InterruptedException, PoolException, HttpException;

    @SneakyThrows
    default void notSupported(@NotNull final HttpSession session, @NotNull final PreparedRequest query) {
        session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
    }

    @SneakyThrows
    default void badRequest(@NotNull final HttpSession session, @NotNull final PreparedRequest query) {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }
}
