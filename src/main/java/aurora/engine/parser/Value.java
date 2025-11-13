// src/main/java/aurora/engine/parser/Value.java
package aurora.engine.parser;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public sealed interface Value
        permits Value.NullValue, Value.BooleanValue, Value.NumberValue, Value.StringValue, Value.RangeValue, Value.ArrayValue, Value.ObjectValue {

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
    record NumberValue(double value) implements Value {
        @Override public @NotNull String toString() {
            long l = (long) value;
            return value == l ? Long.toString(l) : Double.toString(value);
        }
    }
    /** String value representation.
     */
    record StringValue(String value) implements Value {
        @Override public @NotNull String toString() { return "\"" + value + "\""; }
    }
    /** Range value representation.
     */
    record RangeValue(int min, int max) implements Value {
        @Override public @NotNull String toString() { return min + ".." + max; }
    }
    /** Array value representation.
     */
    record ArrayValue(List<Value> elements) implements Value {
        @Override public @NotNull String toString() {
            return "[" + elements.stream()
                    .map(Value::toString)
                    .collect(Collectors.joining(", ")) + "]";
        }
    }
    /** Object value representation.
     */
    record ObjectValue(Map<String, Value> fields) implements Value {
        @Override public @NotNull String toString() {
            return "{" + fields.entrySet().stream()
                    .map(e -> e.getKey() + " := " + e.getValue())
                    .collect(Collectors.joining(", ")) + "}";
        }
    }
}