package dev.badkraft.aurora.engine.validators;

/**
 * Validation status for files.
 */
public enum FileStatus {
    NULL (1, "File path is null"),
    EMPTY (2, "File path is empty"),
    NOT_FOUND (3, "File '{file}' not found"),
    SUCCESS (0, "File '{file}' is valid");

    private final int code;
    private final String message;

    FileStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }
    public String getMessage() {
        return message;
    }

    public String formatMessage(String filePath) {
        return message.replace("{file}", filePath != null ? filePath : "null");
    }
}
