// src/main/java/aurora/engine/parser/Value.java
package aurora.engine.parser;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public sealed interface Value
        permits Value.NullValue, Value.BooleanValue, Value.NumberValue, Value.StringValue,
        Value.RangeValue, Value.ArrayValue, Value.ObjectValue, Value.FreeformValue,
        Value.AnonymousValue {

    @Override
    String toString();

    /** Null value representation.
     */
    record NullValue() implements Value {
        @Override public @NotNull String toString() { return "null"; }
    }
    /** Boolean value representation.
     */
    record BooleanValue(boolean value) implements Value {
        @Override public @NotNull String toString() { return Boolean.toString(value); }
    }
    /** Number value representation.
     */
    record NumberValue(double value, String source) implements Value {
        @Override public @NotNull String toString() {
            return source;
        }
    }
    /** String value representation.
     */
    record StringValue(String value) implements Value {
        @Override
        public @NotNull String toString() { return "\"" + value + "\""; }
    }
    /** Range value representation.
     */
    record RangeValue(int min, int max) implements Value {
        @Override
        public @NotNull String toString() { return min + ".." + max; }
    }
    /** Array value representation.
     */
    record ArrayValue(List<Value> elements, String source) implements Value {
        public ArrayValue(List<Value> elements) {
            this(elements, null);
        }

        @Override
        public @NotNull String toString() {
            return source != null ? source : "[" + elements.stream()
                    .map(Value::toString)
                    .collect(Collectors.joining(", ")) + "]";
        }
    }
    /** Object value representation.
     */
    record ObjectValue(List<Map.Entry<String, Value>> fields, String source) implements Value {
        public ObjectValue {
            fields = List.copyOf(fields);
            Objects.requireNonNull(source);
        }

        @Override
        public @NotNull String toString() {
            return "{" + fields.stream()
                    .map(e -> e.getKey() + " := " + e.getValue())
                    .collect(Collectors.joining(", ")) + "}";
        }

        public Map<String, Value> asMap(){
            return fields.stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }
    /** Freeform value representation.
     */
    record FreeformValue(String content, String attrib) implements Value {
        public FreeformValue(String content) { this(content, null); }

        @Override
        public @NotNull String toString() {
            return (attrib != null ? "@" + attrib : "") + content;
        }
    }

    /** Anonymous value for non-literal expressions - e.g., an identifier.
     */
    record AnonymousValue(String id) implements Value {
        @Override
        public @NotNull String toString() {
            return id;
        }
    }
}