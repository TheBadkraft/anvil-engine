// src/main/java/dev/badkraft/engine/parser/NumericParseResult.java
package dev.badkraft.aurora.engine.parser;

import java.util.List;

public record NumericParseResult(
        Value.NumberValue value,
        List<ParseError> errors
) {
    public static NumericParseResult success(Value.NumberValue value) {
        return new NumericParseResult(value, List.of());
    }

    public static NumericParseResult failure(ErrorCode code, int line, int col) {
        return new NumericParseResult(null, List.of(new ParseError(line, col, code)));
    }

    public boolean isSuccess() { return errors.isEmpty(); }
}