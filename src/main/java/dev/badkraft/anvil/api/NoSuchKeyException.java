package dev.badkraft.anvil.api;

public final class NoSuchKeyException extends RuntimeException {
    public NoSuchKeyException(String module, String key) {
        super("No such key [" + module + "." + key + "]");
    }
}
