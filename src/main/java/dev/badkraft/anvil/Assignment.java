package dev.badkraft.anvil;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/*
    An assignment statement, e.g., key := value
 */
public record Assignment(String key, List<Attribute> attributes, Value value) implements Statement {
    public Assignment(String key, Value value, List<Attribute> attributes) {
        this(key, List.copyOf(attributes), value);
    }

    public String identifier() { return key; }

    @Override
    public List<Attribute> attributes() {
        return attributes;
    }

    @Override
    public @NotNull String toString() {
        String attrs = attributes.isEmpty() ? "" :
            " @[" +
            attributes.stream().map(Attribute::toString).collect(Collectors.joining(", ")) +
            "]";

        return key + attrs + " := " + value;
    }
}
