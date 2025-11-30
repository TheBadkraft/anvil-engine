// dev.badkraft.anvil.core.api/Context.java
package dev.badkraft.anvil.core.api;

import dev.badkraft.anvil.core.data.*;
import dev.badkraft.anvil.utilities.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class Context {
    boolean parsed = false;
    Dialect dialect = Dialect.NONE;
    private final Source source;

    private final ValueFactory factory;
    private final String namespace;
    private final List<Attribute> attributes = new ArrayList<>();
    private final List<Statement> statements = new ArrayList<>();
    private final Set<String> exportedIdentifiers = new LinkedHashSet<>();

    private Context(Builder builder) {
        this.source = Objects.requireNonNull(builder.source, "source required");
        this.namespace = builder.namespace != null ? builder.namespace : Utils.createNamespace();
        loadHeader(builder);
        this.factory = new ValueFactory(this.source);
    }

    // ------------------------------------------------------------------ //
    // Factory methods used by the parser – all include start/end positions
    // ------------------------------------------------------------------ //
    public Value string(int start, int end) {
        return factory.string(start, end);
    }
    public Value bool(boolean b, int start, int end)          { return factory.booleanVal(b, start, end); }
    public Value nullVal(int start, int end)                  { return factory.nullVal(start, end); }
    public Value longVal(long v, int start, int end)          { return factory.longValue(v, start, end); }
    public Value doubleVal(double v, int start, int end)      { return factory.doubleValue(v, start, end); }
    public Value hex(long v, int start, int end)              { return factory.hexValue(v, start, end); }
    public Value bare(int start, int end)        { return factory.bare(start, end); }
    public Value blob(String attr, int start, int end) {
        return factory.blob(attr, start, end);
    }
    public Value array(List<Value> elements, List<Attribute> attrs, int start, int end) {
        return factory.array(elements, attrs, start, end);
    }
    public Value tuple(List<Value> elements, List<Attribute> attrs, int start, int end) {
        return factory.tuple(elements, attrs, start, end);
    }
    public Value object(List<Map.Entry<String, Value>> fields, List<Attribute> attrs, int start, int end) {
        return factory.object(fields, attrs, start, end);
    }

    // ------------------------------------------------------------------ //
    // Public getters
    // ------------------------------------------------------------------ //
    public Source source()               { return source; }
    public String namespace()            { return namespace; }
    public Dialect dialect()             { return dialect; }
    public boolean isParsed()            { return parsed; }
    public List<Statement> statements()  { return List.copyOf(statements); }
    public Set<String> exportedIdentifiers() { return Set.copyOf(exportedIdentifiers); }
    public List<Attribute> attributes()  { return List.copyOf(attributes); }

    // ------------------------------------------------------------------ //
    // Parser-only mutation API
    // ------------------------------------------------------------------ //
    public void markParsed()               { this.parsed = true; }
    public void addStatement(Statement s)  { statements.add(Objects.requireNonNull(s)); }
    public void addIdentifier(String id)   { exportedIdentifiers.add(Objects.requireNonNull(id)); }
    public void addAllAttributes(List<Attribute> attrs) { attributes.addAll(attrs); }

    private void loadHeader(Builder builder) {
        source.skipWhitespace();
        Dialect fromShebang = builder.source.parseDialect(builder.dialect);
        this.dialect = (builder.dialect != null) ? builder.dialect
                : fromShebang != Dialect.NONE ? fromShebang : Dialect.ASL;
        source.skipWhitespace();
    }

    // ------------------------------------------------------------------ //
    // Builder
    // ------------------------------------------------------------------ //
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Source source;
        private String namespace;
        private Dialect dialect;

        private Builder() {}
        public Builder source(String s)      { this.source = new Source(s); return this; }
        public Builder source(Path p) throws IOException {
            this.source = new Source(Files.readString(p));
            this.namespace = this.namespace != null ? this.namespace : Utils.createNamespaceFromPath(p);
            return this;
        }
        public Builder namespace(String ns)  { this.namespace = ns; return this; }
        public Builder dialect(Dialect d)    { this.dialect = d; return this; }
        public Context build()               { return new Context(this); }
    }
}