package dev.badkraft.anvil.api;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

/**
 * Public, immutable representation of an Anvil module.
 */
public sealed interface AnvilModule permits ImmutableAnvilModule {
    AnvilValue get(String key);     // throws NoSuchKeyException
    default Optional<AnvilValue> tryGet(String key) {
        // the point of `try*` is to avoid throwing ... simply return the
        //  Optional.empty() instead of using exceptions for control flow ... it's
        //  up to the consumer to evaluate the return
        return !contains(key) ? Optional.empty() : Optional.of(get(key));
    }

    Set<String> keys();
    default boolean contains(String key) { return keys().contains(key); }

    //  Convenience - throw on wrong type
    default String      getString(String key)      { return get(key).asString(); }
    default long        getLong(String key)        { return get(key).asLong(); }
    default double      getDouble(String key)      { return get(key).asDouble(); }
    default boolean     getBoolean(String key)     { return get(key).asBoolean(); }
    default AnvilArray  getArray(String key)       { return get(key).asArray(); }
    default AnvilObject getObject(String key)      { return get(key).asObject(); }
    default AnvilTuple  getTuple(String key)       { return get(key).asTuple(); }

    //  Metadata
    Path source();          //  Source file path if applicable; null if parsed from string
    String namespace();     //  derived from file name or sourceName

    //  Debug / serialization
    String asFormattedString();

    //  Future extension points (not yet implemented)
    //  - interface Mutable extends AnvilModule { ... }
    //  - interface Builder extends AnvilModule { ... }
}

