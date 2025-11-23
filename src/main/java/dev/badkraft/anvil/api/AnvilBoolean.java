package dev.badkraft.anvil.api;

public record AnvilBoolean(boolean value) implements AnvilValue {
    @Override public boolean isNull()     { return false; }
    @Override public boolean isBoolean()  { return true; }
    @Override public boolean isNumeric()   { return false; }
    @Override public boolean isString()   { return false; }
    @Override public boolean isArray()    { return false; }
    @Override public boolean isObject()   { return false; }
    @Override public boolean isTuple()    { return false; }
    @Override public boolean isBlob()     { return false; }
    @Override public boolean isBare()     { return false; }

    @Override public boolean asBoolean() { return value; }
    @Override public String asString() { return Boolean.toString(value); }
    private static final ClassCastException ERR = new ClassCastException("Cannot convert boolean to number or composite");
    @Override public long asLong()     throws ClassCastException { throw ERR; }
    @Override public double asDouble() throws ClassCastException { throw ERR; }
    @Override public AnvilArray asArray() throws ClassCastException { throw ERR; }
    @Override public AnvilObject asObject() throws ClassCastException { throw ERR; }
    @Override public AnvilTuple asTuple() throws ClassCastException { throw ERR; }
    @Override public AnvilBlob asBlob() throws ClassCastException { throw ERR; }
    @Override public String asBare() throws ClassCastException { throw ERR; }
}
