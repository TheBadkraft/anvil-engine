package dev.badkraft.anvil.api;

import java.util.List;

public record AnvilTuple(List<AnvilValue> elements) implements AnvilValue {
    public AnvilTuple { elements = List.copyOf(elements); }

    public AnvilValue get(int index) {
        return elements.get(index);
    }

    public String      getString(int index)   { return get(index).asString(); }
    public long        getLong(int index)     { return get(index).asLong(); }
    public double      getDouble(int index)   { return get(index).asDouble(); }
    public boolean     getBoolean(int index)  { return get(index).asBoolean(); }
    public AnvilArray  getArray(int index)    { return get(index).asArray(); }
    public AnvilObject getObject(int index)   { return get(index).asObject(); }
    public AnvilTuple  getTuple(int index)    { return get(index).asTuple(); }
    public AnvilBlob   getBlob(int index)     { return get(index).asBlob(); }
    public String      getBare(int index)     { return get(index).asBare(); }

    public int size() { return elements.size(); }

    @Override public boolean isNull()     { return false; }
    @Override public boolean isBoolean()  { return false; }
    @Override public boolean isNumeric()   { return false; }
    @Override public boolean isString()   { return false; }
    @Override public boolean isArray()    { return false; }
    @Override public boolean isObject()   { return false; }
    @Override public boolean isTuple()    { return true; }
    @Override public boolean isBlob()     { return false; }
    @Override public boolean isBare()     { return false; }
    @Override public AnvilTuple asTuple() { return this; }
    @Override public String asString() { return elements.toString(); }
    private static final ClassCastException ERR = new ClassCastException("Cannot convert tuple to scalar or other composite");
    @Override public long asLong()     throws ClassCastException { throw ERR; }
    @Override public double asDouble() throws ClassCastException { throw ERR; }
    @Override public boolean asBoolean() throws ClassCastException { throw ERR; }
    @Override public AnvilArray asArray() throws ClassCastException { throw ERR; }
    @Override public AnvilObject asObject() throws ClassCastException { throw ERR; }
    @Override public AnvilBlob asBlob() throws ClassCastException { throw ERR; }
    @Override public String asBare() throws ClassCastException { throw ERR; }
}
