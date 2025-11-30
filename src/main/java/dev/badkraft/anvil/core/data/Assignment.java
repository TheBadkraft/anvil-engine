package dev.badkraft.anvil.core.data;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/*
    An assignment statement, e.g., key := content
 */
public record Assignment(String key, List<Attribute> attributes, Value value, String base) implements Statement {
    public Assignment(String key, List<Attribute> attributes, Value value) {
        this(key, List.copyOf(attributes), value, null);
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
