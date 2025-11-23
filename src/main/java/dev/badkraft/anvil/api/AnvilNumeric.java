package dev.badkraft.anvil.api;

public record AnvilNumeric(Number value) implements AnvilValue {
    @Override public boolean isNull()     { return false; }
    @Override public boolean isBoolean()  { return false; }
    @Override public boolean isNumeric()   { return true; }
    @Override public boolean isString()   { return false; }
    @Override public boolean isArray()    { return false; }
    @Override public boolean isObject()   { return false; }
    @Override public boolean isTuple()    { return false; }
    @Override public boolean isBlob()     { return false; }
    @Override public boolean isBare()     { return false; }

    @Override public long asLong()     { return value.longValue(); }
    @Override public double asDouble() { return value.doubleValue(); }
    @Override public String asString() { return value.toString(); }
    private static final ClassCastException ERR = new ClassCastException("Cannot convert number to boolean or composite");
    @Override public boolean asBoolean() throws ClassCastException { throw ERR; }
    @Override public AnvilArray asArray() throws ClassCastException { throw ERR; }
    @Override public AnvilObject asObject() throws ClassCastException { throw ERR; }
    @Override public AnvilTuple asTuple() throws ClassCastException { throw ERR; }
    @Override public AnvilBlob asBlob() throws ClassCastException { throw ERR; }
    @Override public String asBare() throws ClassCastException { throw ERR; }
}
