package dev.badkraft.anvil;

import java.util.List;

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
    public ParseException(ErrorCode code, String message, List<ParseError> errors) {
        super(message + ": " + errors);
        this.code = code;
        this.line = -1;
        this.col = -1;
        this.message = message;
    }
}