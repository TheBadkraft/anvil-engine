// src/main/java/aurora/engine/parser/ParseError.java
package aurora.engine.parser;

import org.jetbrains.annotations.NotNull;

public record ParseError(int line, int col, ErrorCode code) {
    public String message() {
        return code.message();
    }

    @Override
    public @NotNull String toString() {
        return "[%d:%d] %s (%s)".formatted(line, col, message(), code);
    }
}