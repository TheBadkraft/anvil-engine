package aurora.engine.parser;

import java.util.ArrayList;
import java.util.List;

public class AuroraDocument {
    // can be set internally by the parser
    boolean isParsed = false;
    Dialect dialect = null;
    private final List<Statement> statements = new ArrayList<>();
    private final List<String> fields = new ArrayList<>();

    public AuroraDocument() {

    }

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
}
