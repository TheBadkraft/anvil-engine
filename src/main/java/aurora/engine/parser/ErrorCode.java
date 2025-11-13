// src/main/java/aurora/engine/parser/ErrorCode.java
package aurora.engine.parser;

public enum ErrorCode {
    // LEXER / SCANNER (100x)
    UNEXPECTED_CHAR(1001, "Unexpected character"),
    UNTERMINATED_STRING(1002, "Unterminated string literal"),
    IO_ERROR(1003, "I/O error during file reading"),

    // PARSER - TOP LEVEL (200x)
    EXPECTED_IDENTIFIER(2001, "Expected identifier"),
    EXPECTED_ASSIGN(2002, "Expected ':=' after identifier"),
    EXPECTED_VALUE(2003, "Expected value"),
    MULTIPLE_SHEBANG(2004, "Multiple shebangs not allowed"),
    SHEBANG_AFTER_STATEMENTS(2005, "Shebang must be first non-whitespace line"),

    // PARSER - OBJECT (300x)
    EXPECTED_OBJECT_FIELD(3001, "Expected object key"),
    EXPECTED_OBJECT_VALUE(3002, "Expected object value after ':='"),
    EXPECTED_OBJECT_CLOSE(3003, "Expected '}' to close object"),
    TRAILING_COMMA_IN_OBJECT(3004, "Trailing comma in object"),
    ERRORS_IN_OBJECT(3099, "Errors found in object fields"),

    // SEMANTIC (400x)
    DUPLICATE_FIELD_IN_OBJECT(4001, "Duplicate identifier in document or object"),
    INVALID_KEY_IN_OBJECT(4002, "Invalid key as identifier");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() { return code; }
    public String message() { return message; }
}