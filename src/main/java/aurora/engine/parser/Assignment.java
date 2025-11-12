package aurora.engine.parser;

import org.jetbrains.annotations.NotNull;

/*
    An assignment statement, e.g., key := value
 */
public record Assignment(String key, Value value) implements Statement {
    @Override
    public @NotNull String toString() {
        return key + " := " + value;
    }
}
