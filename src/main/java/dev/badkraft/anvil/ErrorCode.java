// src/main/java/dev/badkraft/engine/parser/ErrorCode.java
package dev.badkraft.anvil;

public enum ErrorCode {
    // LEXER / SCANNER (100x)
    UNEXPECTED_CHAR(1001, "Unexpected character"),
    UNTERMINATED_STRING(1002, "Unterminated string literal"),
    UNTERMINATED_BLOB(1003, "Unterminated blob literal"),
    IO_ERROR(1004, "I/O error during file reading"),
    EXPECTED_BACKTICK(1005, "Expected freeform literal enclosure"),
    UNTERMINATED_FREEFORM(1006, "Unterminated freeform literal"),
    INVALID_HEX_LITERAL(1007, "Invalid hex number format"),
    INVALID_EXPONENT(1008, "Invalid exponent format"),
    INVALID_NUMBER(1009, "Invalid number format"),

    // PARSER - TOP LEVEL (200x)
    EXPECTED_IDENTIFIER(2001, "Expected identifier"),
    EXPECTED_ASSIGN(2002, "Expected ':=' after identifier"),
    EXPECTED_VALUE(2003, "Expected value"),
    MULTIPLE_SHEBANG(2004, "Multiple shebangs not allowed"),
    SHEBANG_AFTER_STATEMENTS(2005, "Shebang must be first non-whitespace line"),
    INVALID_VALUE_IN_ATTRIBUTE(2006, "Attribute value cannot be object, array, tuple, or blob"),
    INVALID_IDENTIFIER(2007, "Invalid identifier format"),
    EMPTY_ATTRIBUTE_BLOCK(2008, "Attribute blocks cannot be empty"),

    // PARSER - OBJECT (300x)
    EXPECTED_OBJECT_FIELD(3001, "Expected object key"),
    EXPECTED_OBJECT_VALUE(3002, "Expected object value after ':='"),
    EXPECTED_OBJECT_CLOSE(3003, "Expected '}' to close object"),
    TRAILING_COMMA_IN_OBJECT(3004, "Trailing comma in object"),
    TRAILING_COMMA_IN_ARRAY(3006, "Trailing comma in array"),
    MISSING_COMMA_IN_ARRAY(3007, "Missing ',' in array"),
    EXPECTED_ARRAY_CLOSE(3008, "Expected ']' to close array"),
    EXPECTED_TUPLE_CLOSE(3009, "Expected ')' to close tuple value"),
    EMPTY_OBJECT_NOT_ALLOWED(3010, "Empty objects are not allowed"),
    EMPTY_ARRAY_NOT_ALLOWED(3011, "Empty arrays are not allowed"),
    MISSING_COMMA_IN_ATTRIBUTES(3012, "Missing ',' in attribute block"),
    UNEXPECTED_MODULE_ATTRIBUTES(3013, "Module attributes must be prior to statements"),
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
    ASSIGNMENT_NOT_ALLOWED_HERE(4010, "Assignment not valid here"),
    INVALID_ATTRIBUTE_BLOCK(4011, "Invalid attribute block; must be `@[ ... ]`"),
    INVALID_ATTRIBUTE(4012, "The attribute identifier is invalid"),
    DUPLICATE_ATTRIBUTE_KEY(4013, "Duplicate attribute identifier"),;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() { return code; }
    public String message() { return message; }
}