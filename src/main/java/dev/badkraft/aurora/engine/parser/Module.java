package dev.badkraft.aurora.engine.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Module {
    // can be set internally by the parser
    boolean isParsed = false;
    Dialect dialect = null;
    private final String namespace;
    private final List<Statement> statements = new ArrayList<>();
    private final List<String> fields = new ArrayList<>();

    public Module(String namespace) {
        this.namespace = namespace;
    }
    public String namespace() { return namespace; }

    /*
        Returns true if the document contains no content.
     */
    public boolean isEmpty() {
        return hasNoDialect() && statements.isEmpty();
    }
    /*
        Returns true if the document was fully parsed.
     */
    public boolean isParsed() {
        return isParsed;
    }
    /*
        Returns true if the document has a dialect.
     */
    public boolean hasNoDialect() {
        return dialect == null;
    }
    /*
        Returns the document dialect, or null if not set.
     */
    public Dialect getDialect() {
        return dialect;
    }
    /*
        Returns an unmodifiable list of top-level statements in the document.
     */
    public List<Statement> statements() {
        return List.copyOf(statements);
    }
    /*
        Returns an unmodifiable list of fields defined in the document.
     */
    public List<String> fields() {
        return List.copyOf(fields);
    }
    /*
        Returns true if the document has any statements.
     */
    public boolean hasStatements() {
        return !statements.isEmpty();
    }

    /**
        Validates the document structure.
        Ensures all top-level fields are unique and all assignment values are valid.
     */
    public boolean isValid() {
        Set<String> topLevel = new HashSet<>();
        for (String field : fields) {
            if (!topLevel.add(field)) return false;
        }
        return statements.stream()
                .allMatch(stmt -> stmt instanceof Assignment a && isValidValue(a.value()));
    }

    /*
        Internal method to add a statement to the document.
     */
    void addStatement(Statement stmt) {
        statements.add(stmt);
    }
    /*
        Internal method to add an identifier to the document.
     */
    void addIdentifier(String field) {
        fields.add(field);
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
