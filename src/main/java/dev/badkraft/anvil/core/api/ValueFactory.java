// dev.badkraft.anvil.core.api/ValueFactory.java  (unchanged – kept for completeness)
package dev.badkraft.anvil.core.api;

import dev.badkraft.anvil.core.data.*;

import java.util.List;
import java.util.Map;

public record ValueFactory(Source source) {

    public Value string(int start, int end) {
        return new Value.StringValue(source, start, end);
    }
    public Value blob(String attribute, int start, int end) {
        return new Value.BlobValue(source, attribute, start, end);
    }
    public Value booleanVal(boolean b, int start, int end) {
        return new Value.BooleanValue(source, b, start, end);
    }
    public Value nullVal(int start, int end) {
        return new Value.NullValue(source, start, end);
    }
    public Value longValue(long value, int start, int end) {
        return new Value.LongValue(source, value, start, end);
    }
    public Value doubleValue(double value, int start, int end) {
        return new Value.DoubleValue(source, value, start, end);
    }
    public Value hexValue(long value, int start, int end) {
        return new Value.HexValue(source, value, start, end);
    }
    public Value bare(int start, int end) {
        return new Value.BareLiteral(source, start, end);
    }
    public Value array(List<Value> elements, List<Attribute> attributes, int start, int end) {
        return new Value.ArrayValue(source, elements, attributes, start, end);
    }
    public Value tuple(List<Value> elements, List<Attribute> attributes, int start, int end) {
        return new Value.TupleValue(source, elements, attributes, start, end);
    }
    public Value object(List<Map.Entry<String, Value>> fields, List<Attribute> attributes, int start, int end) {
        return new Value.ObjectValue(source, fields, attributes, start, end);
    }
}