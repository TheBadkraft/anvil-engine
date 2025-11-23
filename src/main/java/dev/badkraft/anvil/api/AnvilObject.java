package dev.badkraft.anvil.api;

import java.util.Optional;

public record AnvilObject(AnvilModule object) implements AnvilValue {
    // === Convenience delegation — the way it should have been from day one ===
    public String      getString(String key)   { return object.getString(key); }
    public long        getLong(String key)     { return object.getLong(key); }
    public double      getDouble(String key)   { return object.getDouble(key); }
    public boolean     getBoolean(String key)  { return object.getBoolean(key); }
    public AnvilArray  getArray(String key)    { return object.getArray(key); }
    public AnvilObject getObject(String key)   { return object.getObject(key); }
    public AnvilTuple  getTuple(String key)    { return object.getTuple(key); }
    public AnvilBlob   getBlob(String key)     { return object.getBlob(key); }
    public String      getBare(String key)     { return object.getBare(key); }

    // Safe versions
    public Optional<String> tryString(String key)  { return object.tryGet(key).map(AnvilValue::asString); }
    public Optional<Long>        tryLong(String key)    { return object.tryGet(key).map(AnvilValue::asLong); }
    public Optional<AnvilObject> tryObject(String key)  { return object.tryGet(key).filter(AnvilValue::isObject).map(AnvilValue::asObject); }

    @Override public boolean isNull()     { return false; }
    @Override public boolean isBoolean()  { return false; }
    @Override public boolean isNumeric()   { return false; }
    @Override public boolean isString()   { return false; }
    @Override public boolean isArray()    { return false; }
    @Override public boolean isObject()   { return true; }
    @Override public boolean isTuple()    { return false; }
    @Override public boolean isBlob()     { return false; }
    @Override public boolean isBare()     { return false; }

    @Override public AnvilObject asObject() { return this; }
    @Override public String asString() { return object.toString(); }
    private static final ClassCastException ERR = new ClassCastException("Cannot convert object to scalar or other composite");
    @Override public long asLong()     throws ClassCastException { throw ERR; }
    @Override public double asDouble() throws ClassCastException { throw ERR; }
    @Override public boolean asBoolean() throws ClassCastException { throw ERR; }
    @Override public AnvilArray asArray() throws ClassCastException { throw ERR; }
    @Override public AnvilTuple asTuple() throws ClassCastException { throw ERR; }
    @Override public AnvilBlob asBlob() throws ClassCastException { throw ERR; }
    @Override public String asBare() throws ClassCastException { throw ERR; }
}
