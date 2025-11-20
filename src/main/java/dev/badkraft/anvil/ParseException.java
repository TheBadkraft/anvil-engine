package dev.badkraft.anvil;

public class ParseException extends RuntimeException {
    public final ErrorCode code;
    public final int line;
    public final int col;
    public final String message;

    public ParseException(ErrorCode code, int line, int col, String message) {
        super(message + " at " + line + ":" + col);
        this.code = code;
        this.line = line;
        this.col = col;
        this.message = message;
    }
}