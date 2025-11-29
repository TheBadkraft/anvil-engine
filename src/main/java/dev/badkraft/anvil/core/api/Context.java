package dev.badkraft.anvil.core.api;

import dev.badkraft.anvil.core.data.*;
import dev.badkraft.anvil.utilities.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class Context {
    // can be set internally by the parser
    boolean parsed = false;
    @SuppressWarnings("UnusedAssignment")
    Dialect dialect = Dialect.NONE;
    Source source;

    private final ValueFactory factory;
    private final String namespace;
    private final List<Attribute> attributes = new ArrayList<>();
    private final List<Statement> statements = new ArrayList<>();
    private final Set<String> exportedIdentifiers = new LinkedHashSet<>();

    private Context(Builder builder) {
        this.source   = Objects.requireNonNull(builder.source, "source required");
        this.namespace = builder.namespace != null
                ? builder.namespace
                : Utils.createNamespace();

        loadHeader(builder);
        this.factory = new ValueFactory(this.source);
    }

    // === Internal API ===
    public Source source() { return source; }
    public Value booleanVal(boolean b) { return (Value.BooleanValue) factory.booleanVal(b); }
    public Value hexValue(long value) { return factory.hexValue(value); }
    public Value longValue(long value) { return factory.longValue(value); }
    public Value doubleValue(double value) { return factory.doubleValue(value); }
    public Value nullVal() { return factory.nullVal(); }
    public Value string(String content) { return factory.string(content); }
    public Value blob(String fullSource, String attribute) { return factory.blob(fullSource, attribute); }
    public Value bare(String text) { return factory.bare(text); }
    public Value object(List<Map.Entry<String, Value>> fields, List<Attribute> attrs) {
        return factory.object(fields, attrs);
    }
    public Value array(List<Value> elements) {
        return factory.array(elements);
    }
    public Value tuple(List<Value> elements) {
        return factory.tuple(elements);
    }

    // === Public access API ===
    public String namespace() { return namespace; }
    public Dialect dialect() { return dialect; }
    public boolean isParsed() { return parsed; }
    public boolean isEmpty() { return statements.isEmpty() && dialect == Dialect.NONE; }

    public List<Statement> statements() { return List.copyOf(statements); }
    public Set<String> exportedIdentifiers() { return Set.copyOf(exportedIdentifiers); }

    public boolean hasUniqueTopLevelIdentifiers() {
        return exportedIdentifiers.size() == statements.size();
    }

    // --- Parser-only mutation API (public by necessity) ---
    public void markParsed() {
        this.parsed = true;
    }
    public void addStatement(Statement statement) {
        statements.add(Objects.requireNonNull(statement));
    }
    public void addIdentifier(String identifier) {
        exportedIdentifiers.add(Objects.requireNonNull(identifier));
    }
    public void addAllIdentifiers(Collection<String> identifiers) {
        this.exportedIdentifiers.addAll(identifiers);
    }
    public void addAllAttributes(List<Attribute> moduleAttribs) {
        attributes.addAll(moduleAttribs);
    }
    public List<Attribute> attributes() { return List.copyOf(attributes); }

    /**
        Validates the document structure.
        Ensures all top-level fields are unique and all assignment values are valid.
     */
    public boolean isValid() {
        Set<String> topLevel = new HashSet<>();
        for (String field : exportedIdentifiers) {
            if (!topLevel.add(field)) return false;
        }
        return statements.stream()
                .allMatch(stmt -> stmt instanceof Assignment a && isValidValue(a.value()));
    }

    private boolean isValidValue(Value v) {
        if (v instanceof Value.ObjectValue ov) {
            Set<String> keys = new HashSet<>();
            for (var entry : ov.fields()) {
                String key = entry.getKey();
                if (!keys.add(key)) {
                    return false; // duplicate key
                }
                if (!isValidValue(entry.getValue())) {
                    return false;
                }
            }
            return true;
        }
        if (v instanceof Value.ArrayValue av) {
            return av.elements().stream().allMatch(this::isValidValue);
        }
        return true;
    }
    private void loadHeader(Builder builder) {
        // skip whitespace and comments
        this.source.skipWhitespace();
        // Dialect resolution order (the law)
        Dialect fromShebang = builder.source.parseDialect(builder.dialect); // hint is only fallback for shebang
        this.dialect = (builder.dialect != null)
                ? builder.dialect                                // explicit override wins everything
                : fromShebang != Dialect.NONE
                ? fromShebang                                    // shebang wins
                : Dialect.ASL;                                   // chaos default
        // skip whitespace and comments to position at first meaningful token
        this.source.skipWhitespace();
    }
    // -----------------------------------------------------------------------
    // Builder – tiny, private, grows forever without breaking anyone
    // -----------------------------------------------------------------------
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Source source;
        private String namespace;
        private Dialect dialect;
        // future fields go here – no one else ever sees them

        private Builder() {}

        public Builder source(String source)           { this.source = new Source(source); return this; }
        public Builder source(Path path) throws IOException {
            this.source = new Source(Files.readString(path));
            this.namespace = this.namespace != null ? this.namespace : Utils.createNamespaceFromPath(path);
            return this;
        }

        public Builder namespace(String ns)            { this.namespace = ns; return this; }
        public Builder dialect(Dialect d)              { this.dialect = d; return this; }

        // future experimental hooks – just keep adding
        // public Builder registry(FunctionRegistry r) { this.registry = r; return this; }
        // public Builder addFile(Path extra)          { extras.add(extra); return this; }

        public Context build() {
            return new Context(this);
        }
    }
}
