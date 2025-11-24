package dev.badkraft.anvil;

import java.util.*;

public final class Module {
    // can be set internally by the parser
    boolean parsed = false;
    Dialect dialect = Dialect.NONE;
    private final String namespace;
    private final List<Attribute> attributes = new ArrayList<>();
    private final List<Statement> statements = new ArrayList<>();
    private final Set<String> exportedIdentifiers = new LinkedHashSet<>();

    public Module(String namespace, Dialect dialect) {
        this.namespace = namespace;
        this.dialect = dialect;
    }

    // --- Public read20 access ---
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

}
