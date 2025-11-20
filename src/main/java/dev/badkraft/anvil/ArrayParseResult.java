// src/main/java/dev/badkraft/engine/parser/ArrayParseResult.java
package dev.badkraft.anvil;

import java.util.List;

public record ArrayParseResult(
        Value.ArrayValue value,
        List<ParseError> errors
) {
    public static ArrayParseResult success(Value.ArrayValue value) {
        return new ArrayParseResult(value, List.of());
    }

    public static ArrayParseResult failure(ErrorCode code, int line, int col) {
        return new ArrayParseResult(null, List.of(new ParseError(line, col, code)));
    }

    public static ArrayParseResult failure(List<ParseError> errors) {
        return new ArrayParseResult(null, List.copyOf(errors));
    }

    public boolean isSuccess() { return errors.isEmpty(); }
}