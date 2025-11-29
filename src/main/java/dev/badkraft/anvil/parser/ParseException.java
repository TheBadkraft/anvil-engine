package dev.badkraft.anvil.parser;

import java.util.List;

public class ParseException extends RuntimeException {
    public final ErrorCode code;
    public final int line;
    public final int col;
    public final String message;

    public ParseException(ErrorCode code, int line, int col) {
        super(code.message() + " at " + line + ":" + col);
        this.code = code;
        this.line = line;
        this.col = col;
        this.message = code.message();
    }
    public ParseException(ErrorCode code, String message) {
        super(message);
        this.code = code;
        this.line = -1;
        this.col = -1;
        this.message = message;
    }
}