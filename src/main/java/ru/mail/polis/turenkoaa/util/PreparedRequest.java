package ru.mail.polis.turenkoaa.util;

public class PreparedRequest {
    private final int ack;
    private final int from;
    private final String id;
    private final byte[] body;
    private final boolean isRequestForReplica;
    private final String uri;

    public PreparedRequest(String id, int ack, int from, byte[] body, boolean isRequestForReplica, String uri) {
        this.id = id;
        this.ack = ack;
        this.from = from;
        this.body = body;
        this.isRequestForReplica = isRequestForReplica;
        this.uri = uri;
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

    public byte[] getBody() {
        return body;
    }

    public boolean isRequestForReplica() {
        return isRequestForReplica;
    }

    public String getUri() {
        return uri;
    }
}
