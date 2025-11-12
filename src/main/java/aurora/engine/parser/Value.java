// src/main/java/aurora/engine/parser/Value.java
package aurora.engine.parser;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public sealed interface Value
        permits Value.NullValue, Value.BooleanValue, Value.NumberValue, Value.StringValue, Value.RangeValue, Value.ArrayValue, Value.ObjectValue {

    @Override
    String toString();

    record NullValue() implements Value {
        @Override public String toString() { return "null"; }
    }

    record BooleanValue(boolean value) implements Value {
        @Override public String toString() { return Boolean.toString(value); }
    }

    record NumberValue(double value) implements Value {
        @Override public String toString() {
            long l = (long) value;
            return value == l ? Long.toString(l) : Double.toString(value);
        }
    }

    record StringValue(String value) implements Value {
        @Override public String toString() { return "\"" + value + "\""; }
    }

    record RangeValue(int min, int max) implements Value {
        @Override public String toString() { return min + ".." + max; }
    }

    record ArrayValue(List<Value> elements) implements Value {
        @Override public String toString() {
            return "[" + elements.stream()
                    .map(Value::toString)
                    .collect(Collectors.joining(", ")) + "]";
        }
    }

    record ObjectValue(Map<String, Value> fields) implements Value {
        @Override public String toString() {
            return "{" + fields.entrySet().stream()
                    .map(e -> e.getKey() + " := " + e.getValue())
                    .collect(Collectors.joining(", ")) + "}";
        }
    }
}