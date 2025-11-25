package dev.badkraft.anvil.api;

public record AnvilBlob(String content, String attribute) implements AnvilValue {
    public AnvilBlob { content = content.intern(); }

    @Override public boolean isNull()     { return false; }
    @Override public boolean isBoolean()  { return false; }
    @Override public boolean isNumeric()   { return false; }
    @Override public boolean isString()   { return false; }
    @Override public boolean isArray()    { return false; }
    @Override public boolean isObject()   { return false; }
    @Override public boolean isTuple()    { return false; }
    @Override public boolean isBlob()     { return true; }
    @Override public boolean isBare()     { return false; }
    @Override public boolean isAttribute() { return false; }

    @Override public AnvilBlob asBlob() { return this; }
    @Override public String asString() { return (attribute != null ? "@" + attribute : "") + content; }
    private static final ClassCastException ERR = new ClassCastException("Cannot convert blob to scalar or structured type");
    @Override public long asLong()     throws ClassCastException { throw ERR; }
    @Override public double asDouble() throws ClassCastException { throw ERR; }
    @Override public boolean asBoolean() throws ClassCastException { throw ERR; }
    @Override public AnvilArray asArray() throws ClassCastException { throw ERR; }
    @Override public AnvilObject asObject() throws ClassCastException { throw ERR; }
    @Override public AnvilTuple asTuple() throws ClassCastException { throw ERR; }
    @Override public String asBare() throws ClassCastException { throw ERR; }
    @Override public AnvilAttribute asAttribute() throws ClassCastException { throw ERR; }
}
