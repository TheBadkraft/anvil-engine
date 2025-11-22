package dev.badkraft.anvil.api;

import dev.badkraft.anvil.Module;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class ImmutableAnvilModule implements AnvilModule {

    private final Module delegate;
    private final Path source;
    private final String namespace;
    private final Set<String> keys;
    private final Map<String, AnvilValue> inMemoryFields;

    public ImmutableAnvilModule(Module delegate, Path source) {
        this(delegate, source, null);
    }
    public ImmutableAnvilModule(Map<String, AnvilValue> inMemoryFields) {
        this(null, null, inMemoryFields);
    }

    ImmutableAnvilModule(Module delegate, Path source, Map<String, AnvilValue> inMemoryFields) {
        this.delegate = delegate;
        this.source = source;
        this.namespace = source != null
                ? source.getFileName().toString().replaceFirst("\\.[^.]+$", "")
                : "<string>";
        this.inMemoryFields = inMemoryFields != null ? Map.copyOf(inMemoryFields) : null;
        this.keys = inMemoryFields != null
                ? Set.copyOf(inMemoryFields.keySet())
                : Set.copyOf(delegate.exportedIdentifiers());
    }

    private static String deriveNamespace(Path source) {
        if (source == null) return "<string>";
        String fileName = source.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    @Override
    public AnvilValue get(String key) throws NoSuchKeyException {
        if (inMemoryFields != null) {
            AnvilValue v = inMemoryFields.get(key);
            if (v != null) return v;
            throw new NoSuchKeyException(namespace, key);
        }

        if (!keys.contains(key)) {
            throw new NoSuchKeyException(namespace, key);
        }

        return delegate.statements().stream()
                .filter(s -> s.identifier().equals(key))
                .findFirst()
                .map(s -> AnvilValueAdapter.adapt(s.value()))
                .get();
    }

    @Override
    public Set<String> keys() {
        return keys;
    }

    @Override
    public Path source() {
        return source;
    }

    @Override
    public String namespace() {
        return namespace;
    }

    @Override
    public String asFormattedString() {
        // TODO: wire up pretty printer later — for now, simple debug view
        return String.format("AnvilModule[%s] { keys: %s }", namespace, keys);
    }
}