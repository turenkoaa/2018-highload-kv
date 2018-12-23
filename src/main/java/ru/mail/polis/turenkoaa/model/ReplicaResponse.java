package ru.mail.polis.turenkoaa.model;


public class ReplicaResponse {
    private int successAck;
    private int notFound;
    private int removed;
    private byte[] value;

    public int getSuccessAck() {
        return successAck;
    }

    public int getNotFound() {
        return notFound;
    }

    public int getRemoved() {
        return removed;
    }

    public byte[] getValue() {
        return value;
    }

    public ReplicaResponse successAck(int successAck) {
        this.successAck = successAck;
        return this;
    }

    public ReplicaResponse notFound(int notFound) {
        this.notFound = notFound;
        return this;
    }

    public ReplicaResponse removed(int removed) {
        this.removed = removed;
        return this;
    }

    public ReplicaResponse value(byte[] value) {
        this.value = value;
        return this;
    }
}
