// src/main/java/dev/badkraft/engine/parser/ParseResult.java
package dev.badkraft.anvil;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Collections;

public record ParseResult<T>(
        T result,
        List<ParseError> errors
) {
    public static <T> ParseResult<T> success(T result) {
        return new ParseResult<>(result, Collections.emptyList());
    }

    public static <T> ParseResult<T> failure(List<ParseError> errors) {
        return new ParseResult<>(null, List.copyOf(errors));
    }

    public boolean isSuccess() { return errors.isEmpty(); }

    @Override
    public @NotNull String toString() {
        if (isSuccess()) {
            return "ParseResult[success]";
        } else {
            return "ParseResult[failure=" + errors + "]";
        }
    }
}