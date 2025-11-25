package dev.badkraft.anvil.api;

import java.util.List;

public record AnvilArray(List<AnvilValue> elements, List<AnvilAttribute> attributes) implements AnvilValue {
    public AnvilArray { elements = List.copyOf(elements); }

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
    public boolean hasAttribute(String attrib) { return attributes.stream().anyMatch(a -> a.key().id().equals(attrib)); }

    public int size() { return elements.size(); }
    public boolean isEmpty() { return elements.isEmpty(); }

    @Override public boolean isNull()     { return false; }
    @Override public boolean isBoolean()  { return false; }
    @Override public boolean isNumeric()   { return false; }
    @Override public boolean isString()   { return false; }
    @Override public boolean isArray()    { return true; }
    @Override public boolean isObject()   { return false; }
    @Override public boolean isTuple()    { return false; }
    @Override public boolean isBlob()     { return false; }
    @Override public boolean isBare()     { return false; }
    @Override public boolean isAttribute() {return false;}

    @Override public AnvilArray asArray() { return this; }
    @Override public String asString() { return elements.toString(); }
    private static final ClassCastException ERR = new ClassCastException("Cannot convert array to scalar or other composite");
    @Override public long asLong()     throws ClassCastException { throw ERR; }
    @Override public double asDouble() throws ClassCastException { throw ERR; }
    @Override public boolean asBoolean() throws ClassCastException { throw ERR; }
    @Override public AnvilObject asObject() throws ClassCastException { throw ERR; }
    @Override public AnvilTuple asTuple() throws ClassCastException { throw ERR; }
    @Override public AnvilBlob asBlob() throws ClassCastException { throw ERR; }
    @Override public String asBare() throws ClassCastException { throw ERR; }
    @Override public AnvilAttribute asAttribute() throws ClassCastException {throw ERR;}
}
