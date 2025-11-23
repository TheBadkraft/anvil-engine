// src/main/java/dev/badkraft/anvil/api/AnvilValue.java
package dev.badkraft.anvil.api;

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
    String asBare()     throws ClassCastException;

    // === Safe defaults ===
    default String   asString(String def)     { try { return asString(); }   catch (Exception e) { return def; } }
    default long     asLong(long def)         { try { return asLong(); }     catch (Exception e) { return def; } }
    default double   asDouble(double def)     { try { return asDouble(); }   catch (Exception e) { return def; } }
    default boolean  asBoolean(boolean def)   { try { return asBoolean(); }  catch (Exception e) { return def; } }
}

// === Concrete immutable value types (all in one file) ===

