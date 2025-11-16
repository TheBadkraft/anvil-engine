// src/main/java/aurora/engine/parser/Attribute.java
package aurora.engine.parser;

import org.jetbrains.annotations.NotNull;
import java.util.Objects;

/**
 * A single attribute attached to a statement.
 * <ul>
 *   <li>Tag form:   {@code debug}</li>
 *   <li>K-V form:   {@code type=block}</li>
 * </ul>
 * The value, when present, is **not** a nested construct – only a literal (string, number, boolean, null).
 */
public record Attribute(@NotNull String key, Value value) {
    public Attribute(String key) { this(key, null); }

    @Override
    public @NotNull String toString() {
        return value == null ? key : key + "=" + value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Attribute a)) return false;
        return key.equals(a.key) && Objects.equals(value, a.value);
    }

    @Override
    public int hashCode() { return Objects.hash(key, value); }
}