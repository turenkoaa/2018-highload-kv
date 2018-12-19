package ru.mail.polis.turenkoaa.util;

public class PreparedRequest {
    private final int ack;
    private final int from;
    private final String id;

    public PreparedRequest(String id, int ack, int from) {
        this.id = id;
        this.ack = ack;
        this.from = from;
    }

    public int getAck() {
        return ack;
    }

    public int getFrom() {
        return from;
    }

    public String getId() {
        return id;
    }
}
