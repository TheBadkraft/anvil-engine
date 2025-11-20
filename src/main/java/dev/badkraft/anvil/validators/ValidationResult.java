package dev.badkraft.anvil.validators;

/**
 * Result of a validation process.
 */
public final class ValidationResult {
    private final int code;
    private final String message;

    public ValidationResult(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
