package ru.mail.polis.turenkoaa.rest;

import one.nio.http.HttpSession;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.turenkoaa.model.PreparedRequest;

public interface RestMethodResolver {
    void resolveRequest(@NotNull final HttpSession session, @NotNull final PreparedRequest query);
}
