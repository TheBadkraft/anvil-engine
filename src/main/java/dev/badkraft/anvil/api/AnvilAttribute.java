package dev.badkraft.anvil.api;

import java.util.Map;

public record AnvilAttribute(Map.Entry<AnvilBare, AnvilValue> attribute) implements AnvilValue {

    private static final AnvilValue ANVIL_NULL = new AnvilNull();
    public AnvilAttribute { }

    // Convenience: create from varargs (key, value, key, value...)
    public static AnvilAttribute of(String key, AnvilValue value) {
        return of(new AnvilBare(key), value);
    }
    public static AnvilAttribute of(AnvilBare key, AnvilValue value) {
        // value cannot be Object, Array, or Tuple
        if (value instanceof AnvilObject || value instanceof AnvilArray || value instanceof AnvilTuple) {
            throw new IllegalArgumentException("Attribute values cannot be Object, Array, or Tuple");
        }
        Map.Entry<AnvilBare, AnvilValue> map = Map.entry(key, value);
        return new AnvilAttribute(map);
    }
    public AnvilBare key() {return attribute.getKey(); }
    public AnvilValue value() { return attribute.getValue(); }

    // Tag form: @[debug] → has("debug") and value is null
    // Type checks
    @Override public boolean isNull() { return false; }
    @Override public boolean isBoolean() { return false; }
    @Override public boolean isNumeric() { return false; }
    @Override public boolean isString() { return false; }
    @Override public boolean isArray() { return false; }
    @Override public boolean isObject() { return false; }
    @Override public boolean isTuple() { return false; }
    @Override public boolean isBlob() { return false; }
    @Override public boolean isBare() { return false; }
    @Override public boolean isAttribute() { return true; }

    // All other conversions throw
    @Override public AnvilAttribute asAttribute() { return this; }
    @Override public String asString() { return attribute.toString(); }
    private static final ClassCastException ERR = new ClassCastException("Not a scalar or composite");
    @Override public long asLong() throws ClassCastException { throw ERR; }
    @Override public double asDouble() throws ClassCastException { throw ERR; }
    @Override public boolean asBoolean() throws ClassCastException { throw ERR; }
    @Override public AnvilArray asArray() throws ClassCastException { throw ERR; }
    @Override public AnvilObject asObject() throws ClassCastException { throw ERR; }
    @Override public AnvilTuple asTuple() throws ClassCastException { throw ERR; }
    @Override public AnvilBlob asBlob() throws ClassCastException { throw ERR; }
    @Override public String asBare() throws ClassCastException { throw ERR; }
}