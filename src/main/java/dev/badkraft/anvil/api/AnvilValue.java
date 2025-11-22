// src/main/java/dev/badkraft/anvil/api/AnvilValue.java
package dev.badkraft.anvil.api;

import java.util.List;

@SuppressWarnings({"ClassEscapesDefinedScope", "unused"})
public sealed interface AnvilValue
        permits AnvilNull, AnvilBoolean, AnvilNumeric, AnvilString,
        AnvilArray, AnvilObject, AnvilTuple, AnvilBlob, AnvilBare {

    // === Type checks ===
    boolean isNull();
    boolean isBoolean();
    boolean isNumeric();
    boolean isString();
    boolean isArray();
    boolean isObject();
    boolean isTuple();
    boolean isBlob();
    boolean isBare();

    // === Coercion (throws ClassCastException on mismatch) ===
    String      asString()   throws ClassCastException;
    long        asLong()     throws ClassCastException;
    double      asDouble()   throws ClassCastException;
    boolean     asBoolean()  throws ClassCastException;
    AnvilArray  asArray()    throws ClassCastException;
    AnvilObject asObject()   throws ClassCastException;
    AnvilTuple  asTuple()    throws ClassCastException;
    AnvilBlob   asBlob()     throws ClassCastException;
    String      asBare()     throws ClassCastException;

    // === Safe defaults ===
    default String   asString(String def)     { try { return asString(); }   catch (Exception e) { return def; } }
    default long     asLong(long def)         { try { return asLong(); }     catch (Exception e) { return def; } }
    default double   asDouble(double def)     { try { return asDouble(); }   catch (Exception e) { return def; } }
    default boolean  asBoolean(boolean def)   { try { return asBoolean(); }  catch (Exception e) { return def; } }
}

// === Concrete immutable value types (all in one file) ===

record AnvilNull() implements AnvilValue {
    @Override public boolean isNull()     { return true; }
    @Override public boolean isBoolean()  { return false; }
    @Override public boolean isNumeric()   { return false; }
    @Override public boolean isString()   { return false; }
    @Override public boolean isArray()    { return false; }
    @Override public boolean isObject()   { return false; }
    @Override public boolean isTuple()    { return false; }
    @Override public boolean isBlob()     { return false; }
    @Override public boolean isBare()     { return false; }

    @Override public String asString() { return "null"; }
    private static final ClassCastException ERR = new ClassCastException("Cannot convert null to primitive or composite type");
    @Override public long asLong()     throws ClassCastException { throw ERR; }
    @Override public double asDouble() throws ClassCastException { throw ERR; }
    @Override public boolean asBoolean() throws ClassCastException { throw ERR; }
    @Override public AnvilArray asArray() throws ClassCastException { throw ERR; }
    @Override public AnvilObject asObject() throws ClassCastException { throw ERR; }
    @Override public AnvilTuple asTuple() throws ClassCastException { throw ERR; }
    @Override public AnvilBlob asBlob() throws ClassCastException { throw ERR; }
    @Override public String asBare() throws ClassCastException { throw ERR; }
}

record AnvilBoolean(boolean value) implements AnvilValue {
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

record AnvilNumeric(Number value) implements AnvilValue {
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

record AnvilString(String value) implements AnvilValue {
    @Override public boolean isNull()     { return false; }
    @Override public boolean isBoolean()  { return false; }
    @Override public boolean isNumeric()   { return false; }
    @Override public boolean isString()   { return true; }
    @Override public boolean isArray()    { return false; }
    @Override public boolean isObject()   { return false; }
    @Override public boolean isTuple()    { return false; }
    @Override public boolean isBlob()     { return false; }
    @Override public boolean isBare()     { return false; }

    @Override public String asString() { return value; }
    private static final ClassCastException ERR = new ClassCastException("Cannot convert string to number or composite");
    @Override public long asLong()     throws ClassCastException { throw ERR; }
    @Override public double asDouble() throws ClassCastException { throw ERR; }
    @Override public boolean asBoolean() throws ClassCastException { throw ERR; }
    @Override public AnvilArray asArray() throws ClassCastException { throw ERR; }
    @Override public AnvilObject asObject() throws ClassCastException { throw ERR; }
    @Override public AnvilTuple asTuple() throws ClassCastException { throw ERR; }
    @Override public AnvilBlob asBlob() throws ClassCastException { throw ERR; }
    @Override public String asBare() throws ClassCastException { throw ERR; }
}

record AnvilArray(List<AnvilValue> elements) implements AnvilValue {
    public AnvilArray { elements = List.copyOf(elements); }

    @Override public boolean isNull()     { return false; }
    @Override public boolean isBoolean()  { return false; }
    @Override public boolean isNumeric()   { return false; }
    @Override public boolean isString()   { return false; }
    @Override public boolean isArray()    { return true; }
    @Override public boolean isObject()   { return false; }
    @Override public boolean isTuple()    { return false; }
    @Override public boolean isBlob()     { return false; }
    @Override public boolean isBare()     { return false; }

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
}

record AnvilObject(AnvilModule object) implements AnvilValue {
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

record AnvilTuple(List<AnvilValue> elements) implements AnvilValue {
    public AnvilTuple { elements = List.copyOf(elements); }

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

record AnvilBlob(String content, String attribute) implements AnvilValue {
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
}

record AnvilBare(String id) implements AnvilValue {
    @Override public boolean isNull()     { return false; }
    @Override public boolean isBoolean()  { return false; }
    @Override public boolean isNumeric()   { return false; }
    @Override public boolean isString()   { return false; }
    @Override public boolean isArray()    { return false; }
    @Override public boolean isObject()   { return false; }
    @Override public boolean isTuple()    { return false; }
    @Override public boolean isBlob()     { return false; }
    @Override public boolean isBare()     { return true; }

    @Override public String asBare() { return id; }
    @Override public String asString() { return id; }
    private static final ClassCastException ERR = new ClassCastException("Cannot convert bare reference to scalar or composite");
    @Override public long asLong()     throws ClassCastException { throw ERR; }
    @Override public double asDouble() throws ClassCastException { throw ERR; }
    @Override public boolean asBoolean() throws ClassCastException { throw ERR; }
    @Override public AnvilArray asArray() throws ClassCastException { throw ERR; }
    @Override public AnvilObject asObject() throws ClassCastException { throw ERR; }
    @Override public AnvilTuple asTuple() throws ClassCastException { throw ERR; }
    @Override public AnvilBlob asBlob() throws ClassCastException { throw ERR; }
}