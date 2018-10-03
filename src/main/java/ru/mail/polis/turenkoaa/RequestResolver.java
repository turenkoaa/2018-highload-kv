package ru.mail.polis.turenkoaa;

import one.nio.http.HttpSession;
import one.nio.http.Request;
import org.jetbrains.annotations.NotNull;

public interface RequestResolver {
    void resolveEntityRequest(@NotNull final Request request, @NotNull final HttpSession session);
}
