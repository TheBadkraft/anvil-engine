package dev.badkraft.anvil.core.api;

import dev.badkraft.anvil.core.data.Attribute;
import dev.badkraft.anvil.core.data.Source;
import dev.badkraft.anvil.core.data.Value;

import java.util.List;
import java.util.Map;

record ValueFactory(Source source) {
    public Value string(String content) {
        int start = source.position() - content.length() - 2; // -2 for ""
        int end = source.position();
        String full = source.substring(start, end);
        return new Value.StringValue(content);
    }

    public Value blob(String fullSource, String attribute) {
        return new Value.BlobValue(fullSource, attribute);
    }

    public Value booleanVal(boolean b) {
        int start = source.position() - (b ? 4 : 5);
        String full = source.substring(start, source.position());
        return new Value.BooleanValue(b);
    }

    public Value hexValue(long value) {
        //  stub source capture
        int start = source.position() - 2; // at least 0x
        return new Value.HexValue(value);
    }

    public Value longValue(long value) {
        // stub source capture
        int start = source.position() - Long.toString(value).length();
        return new Value.LongValue(value);
    }

    public Value doubleValue(double value) {
        // stub source capture
        int start = source.position() - Double.toString(value).length();
        return new Value.DoubleValue(value);
    }

    public Value nullVal() {
        int start = source.position() - 4;
        String full = source.substring(start, source.position());
        return new Value.NullValue();
    }

    public Value bare(String text) {
        int start = source.position() - text.length();
        String full = source.substring(start, source.position());
        return new Value.BareLiteral(text);
    }

    public Value object(List<Map.Entry<String, Value>> fields, List<Attribute> attrs) {
        // capture span from { to }
        int start = source.position(); // we'll set this properly in parser via temp mark
        // temporary — real span set in parser before calling
        String placeholder = "";
        return new Value.ObjectValue(fields, attrs, placeholder);
    }

    public Value array(List<Value> elements) {
        // capture span from [ to ]
        int start = source.position(); // we'll set this properly in parser via temp mark
        // temporary — real span set in parser before calling
        String placeholder = "";
        return new Value.ArrayValue(elements, placeholder);
    }

    public Value tuple(List<Value> elements) {
        // capture span from ( to )
        int start = source.position(); // we'll set this properly in parser via temp mark
        // temporary — real span set in parser before calling
        String placeholder = "";
        return new Value.TupleValue(elements, placeholder);
    }
}