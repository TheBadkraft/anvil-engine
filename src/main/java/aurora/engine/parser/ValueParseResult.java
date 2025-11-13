// src/main/java/aurora/engine/parser/ValueParseResult.java
package aurora.engine.parser;

import java.util.List;

public record ValueParseResult<T>(
        T value,
        List<ParseError> errors
) {
    public static <T> ValueParseResult<T> success(T value) {
        return new ValueParseResult<>(value, List.of());
    }

    public static <T> ValueParseResult<T> failure(ErrorCode message, int line, int col) {
        return new ValueParseResult<>(null, List.of(new ParseError(line, col, message)));
    }
    public static <T> ValueParseResult<T> failure(List<ParseError> errors) {
        return new ValueParseResult<>(null, errors);
    }

    public boolean isSuccess() { return errors.isEmpty(); }
}