// src/main/java/dev/badkraft/engine/parser/Value.java
package dev.badkraft.anvil;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public sealed interface Value permits
        Value.NullValue, Value.BooleanValue,
        Value.LongValue, Value.DoubleValue,
        Value.HexValue, Value.StringValue,
        Value.ArrayValue, Value.ObjectValue,
        Value.BlobValue, Value.BareLiteral,
        Value.TupleValue {

    @Override String toString();

    // === PRIMITIVE VALUES ===
    record NullValue() implements Value {
        @Override
        public @NotNull String toString() { return "null"; }
    }
    record BooleanValue(boolean value) implements Value {
        @Override
        public @NotNull String toString() { return Boolean.toString(value); }
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
    record StringValue(String value) implements Value {  // ← renamed from getValue
        public StringValue { Objects.requireNonNull(value); }
        @Override public @NotNull String toString() {
            return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
    }

    // === COMPOSITE VALUES ===
    record ArrayValue(List<Value> elements, String source) implements Value {
        public ArrayValue(List<Value> elements) { this(elements, null); }
        public ArrayValue { elements = List.copyOf(elements); }
        @Override public @NotNull String toString() {
            if (source != null) return source;
            return "[" + elements.stream()
                    .map(Value::toString)
                    .collect(Collectors.joining(", ")) + "]";
        }
    }
    record ObjectValue(List<Map.Entry<String, Value>> fields, String source) implements Value {
        public ObjectValue {
            fields = List.copyOf(fields);
            Objects.requireNonNull(source);
        }
        @Override public @NotNull String toString() {
            return source;
        }
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
    record BlobValue(String content, String attribute) implements Value {
        public BlobValue(String content) { this(content, null); }
        @Override public @NotNull String toString() {
            return (attribute != null ? "@" + attribute : "@") + content;
        }
    }
    record BareLiteral(String id) implements Value {
        public BareLiteral { Objects.requireNonNull(id); }
        @Override public @NotNull String toString() { return id; }
    }
    record TupleValue(List<Value> elements, String source) implements Value{
        public TupleValue(List<Value> elements) {
            this(elements, null);
        }

        public TupleValue {
            if (elements.size() < 2) {
                // we can throw here because the parser should prevent this
                throw new IllegalArgumentException("Tuple must have at least 2 elements");
            }
            elements = List.copyOf(elements);
        }

        @Override
        public @NotNull String toString() {
            if (source != null) return source;
            return "(" + elements.stream()
                    .map(Value::toString)
                    .collect(Collectors.joining(", ")) + ")";
        }
    }
}