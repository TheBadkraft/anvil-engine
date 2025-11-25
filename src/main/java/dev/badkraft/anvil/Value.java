// src/main/java/dev/badkraft/engine/parser/Value.java
package dev.badkraft.anvil;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public sealed interface Value
        permits Value.NullValue, Value.BooleanValue, Value.LongValue, Value.DoubleValue,
        Value.HexValue, Value.StringValue, Value.BareLiteral, Value.BlobValue,
        Value.ArrayValue, Value.TupleValue, Value.ObjectValue {

    // =====================================================================
    // VIRTUAL ATTRIBUTE FACADE — zero-cost, encapsulated, perfect
    // =====================================================================
    default Attributes getAttributes() {
        return switch (this) {
            case ArrayValue a -> new Attributes(a.attributes);
            case TupleValue t -> new Attributes(t.attributes);
            case ObjectValue o -> new Attributes(o.attributes);
            default -> throw new UnsupportedOperationException(
                    "Attributes are not supported on " + getClass().getSimpleName()
            );
        };
    }

    // This class is intentionally not a record to allow future extensibility
    // and maintain strict control over its behavior.
    final class Attributes {
        private final List<Attribute> backing;

        private Attributes(List<Attribute> backing) {
            this.backing = Objects.requireNonNullElse(backing, new ArrayList<>());
        }

        public void add(Attribute attribute) {
            backing.add(attribute);
        }
        public void addAll(List<Attribute> attributes) {
            backing.addAll(attributes);
        }
        public boolean isEmpty() { return backing.isEmpty(); }
        public int size() { return backing.size(); }
        public Attribute get(int index) { return backing.get(index); }
        public boolean has(String key) {
            return backing.stream().anyMatch(a -> a.key().equals(key));
        }
        public boolean hasTag(String key) {
            return backing.stream()
                    .anyMatch(a -> a.key().equals(key) && a.value() == null);
        }
        public Optional<Attribute> find(String key) {
            return backing.stream()
                    .filter(a -> a.key().equals(key))
                    .findFirst();
        }

        public Stream<Attribute> stream() { return backing.stream(); }
        public Iterator<Attribute> iterator() { return backing.iterator(); }
    }

    // =====================================================================
    // PRIMITIVE VALUES — no attributes
    // =====================================================================
    record NullValue() implements Value {
        @Override public @NotNull String toString() { return "null"; }
    }

    record BooleanValue(boolean value) implements Value {
        @Override public @NotNull String toString() { return Boolean.toString(value); }
    }

    record LongValue(long value, String source) implements Value {
        public LongValue(long value) { this(value, null); }
        @Override public String toString() {
            return source != null ? source : Long.toString(value);
        }
    }

    record DoubleValue(double value, String source) implements Value {
        public DoubleValue(double value) { this(value, null); }
        @Override public String toString() {
            return source != null ? source : Double.toString(value);
        }
    }

    record HexValue(long value, String source) implements Value {
        public HexValue(long value) { this(value, null); }
        @Override public String toString() {
            return source != null ? source : "0x" + Long.toHexString(value).toUpperCase();
        }
    }

    record StringValue(String value) implements Value {
        public StringValue { Objects.requireNonNull(value); }
        @Override public @NotNull String toString() { return value; }
    }

    record BareLiteral(String id) implements Value {
        public BareLiteral { Objects.requireNonNull(id); }
        @Override public @NotNull String toString() { return id; }
    }

    record BlobValue(String content, String attribute) implements Value {
        public BlobValue(String content) { this(content, null); }
        @Override public @NotNull String toString() {
            return (attribute != null ? "@" + attribute : "@") + content;
        }
    }

    // =====================================================================
    // COMPOSITE VALUES — with real attributes
    // =====================================================================
    record ArrayValue(List<Value> elements, List<Attribute> attributes, String source) implements Value {
        public ArrayValue(List<Value> elements, String source) { this(elements, List.of(), source); }
        public ArrayValue {
            elements = List.copyOf(elements);
            attributes = new ArrayList<>(attributes != null ? attributes : List.of());
        }

        @Override public @NotNull String toString() {
            if (source != null) return source;
            return "[" + elements.stream()
                    .map(Value::toString)
                    .collect(Collectors.joining(", ")) + "]";
        }
    }

    record TupleValue(List<Value> elements, List<Attribute> attributes, String source) implements Value {
        public TupleValue(List<Value> elements, String source) { this(elements, List.of(), source); }
        public TupleValue {
            if (elements.size() < 2) {
                throw new IllegalArgumentException("Tuple must have at least 2 elements");
            }
            elements = List.copyOf(elements);
            attributes = new ArrayList<>(attributes != null ? attributes : List.of());
        }

        @Override public @NotNull String toString() {
            if (source != null) return source;
            return "(" + elements.stream()
                    .map(Value::toString)
                    .collect(Collectors.joining(", ")) + ")";
        }
    }

    record ObjectValue(List<Map.Entry<String, Value>> fields, List<Attribute> attributes, String source)
            implements Value {

        public ObjectValue {
            fields = List.copyOf(fields);
            attributes = new ArrayList<>(attributes != null ? attributes : List.of());
            Objects.requireNonNull(source);
        }

        @Override public @NotNull String toString() { return source; }

        public Map<String, Value> asMap() {
            LinkedHashMap<String, Value> map = new LinkedHashMap<>();
            for (var e : fields) {
                if (map.put(e.getKey(), e.getValue()) != null) {
                    throw new IllegalStateException("Duplicate key: " + e.getKey());
                }
            }
            return Map.copyOf(map);
        }
    }
}