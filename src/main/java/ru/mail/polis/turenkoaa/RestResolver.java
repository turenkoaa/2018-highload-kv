package ru.mail.polis.turenkoaa;

import one.nio.http.HttpException;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.turenkoaa.util.PreparedRequest;

import java.io.IOException;

public interface RestResolver {
    void resolveEntityRequest(@NotNull final HttpSession session, @NotNull final PreparedRequest query) throws InterruptedException, PoolException, HttpException, IOException;
}
