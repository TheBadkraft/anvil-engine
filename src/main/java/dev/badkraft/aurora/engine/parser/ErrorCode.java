// src/main/java/dev/badkraft/engine/parser/ErrorCode.java
package dev.badkraft.aurora.engine.parser;

public enum ErrorCode {
    // LEXER / SCANNER (100x)
    UNEXPECTED_CHAR(1001, "Unexpected character"),
    UNTERMINATED_STRING(1002, "Unterminated string literal"),
    IO_ERROR(1003, "I/O error during file reading"),
    EXPECTED_BACKTICK(1004, "Expected freeform literal enclosure"),
    UNTERMINATED_FREEFORM(1005, "Unterminated freeform literal"),
    INVALID_NUMBER(1006, "Invalid number format"),
    INVALID_EXPONENT(1007, "Invalid exponent format"),

    // PARSER - TOP LEVEL (200x)
    EXPECTED_IDENTIFIER(2001, "Expected identifier"),
    EXPECTED_ASSIGN(2002, "Expected ':=' after identifier"),
    EXPECTED_VALUE(2003, "Expected value"),
    MULTIPLE_SHEBANG(2004, "Multiple shebangs not allowed"),
    SHEBANG_AFTER_STATEMENTS(2005, "Shebang must be first non-whitespace line"),
    INVALID_VALUE_IN_ATTRIBUTE(2006, "Attribute value cannot be an object or array"),
    INVALID_IDENTIFIER(2007, "Invalid identifier format"),

    // PARSER - OBJECT (300x)
    EXPECTED_OBJECT_FIELD(3001, "Expected object key"),
    EXPECTED_OBJECT_VALUE(3002, "Expected object value after ':='"),
    EXPECTED_OBJECT_CLOSE(3003, "Expected '}' to close object"),
    TRAILING_COMMA_IN_OBJECT(3004, "Trailing comma in object"),
    TRAILING_COMMA_IN_ARRAY(3006, "Trailing comma in array"),
    EXPECTED_ARRAY_CLOSE(3007, "Expected ']' to close array"),
    EXPECTED_TUPLE_CLOSE(3008, "Expected ')' to close tuple value"),
    ERRORS_IN_OBJECT(3099, "Errors found in object fields"),
    UNEXPECTED_TOKEN(3100, "Unexpected token"),

    // SEMANTIC (400x)
    DUPLICATE_FIELD_IN_OBJECT(4001, "Duplicate identifier in document or object"),
    INVALID_KEY_IN_OBJECT(4002, "Invalid key as identifier"),
    IDENTIFIER_IS_KEYWORD(4003, "Identifier cannot be a keyword"),
    ATTRIBUTE_IS_KEYWORD(4004, "Attribute name cannot be a keyword"),
    TUPLE_TOO_SHORT(4005, "Tuple requires minimum 2 values"),
    EXPECTED_COMMA_IN_TUPLE(4006, "Missing ',' in tuple"),
    EMPTY_TUPLE_ELEMENT(4007, "Missing element in tuple"),
    ROCKET_OP_NOT_VALID(4008, "Rocket operator '=>' is not valid"),
    ARRAY_CANNOT_BE_EMPTY(4009, "Arrays cannot be empty"),
    ASSIGNMENT_NOT_ALLOWED_HERE(4010, "Assignment not valid here");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() { return code; }
    public String message() { return message; }
}