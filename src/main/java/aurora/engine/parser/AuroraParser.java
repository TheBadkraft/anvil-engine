// src/main/java/aurora/engine/parser/AmlParser.java
package aurora.engine.parser;

import aurora.engine.utilities.Utils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.*;
import java.io.IOException;
import java.util.*;

import static aurora.engine.parser.ValueParseResult.failure;
import static aurora.engine.parser.ValueParseResult.success;

public class AuroraParser {
    public static final Set<String> KEYWORDS = Set.of(
            "true", "false", "null", "vars", "include"
    );

    private final String source;
    private final List<ParseError> errors = new ArrayList<>();
    private int errCount = 0;                     // total errors (even if capped)
    private static final int MAX_ERRORS = 25;    // stop logging after this many
    private boolean inAttributeContext = false;   // true inside @[ … ]
    private boolean hasShebang = false;
    private int pos = 0;
    private int line = 1;
    private int col = 1;

    private AuroraParser(String source) {
        this.source = source;
    }

    /* --------------------------------------------------------------
       PUBLIC ENTRY POINT
       -------------------------------------------------------------- */
    public static ParseResult<Module> parse(Path path) {
        try {
            String content = Files.readString(path);
            Dialect dialect = Dialect.fromFileExtension(
                    Utils.getFileExtension(path));
            AuroraParser parser = new AuroraParser(content);
            parser.errors.clear();
            parser.errCount = 0;
            var module = parser.parseSource(dialect);
            return parser.errors.isEmpty()
                    ? ParseResult.success(module)
                    : ParseResult.failure(parser.errors);
        } catch (IOException e) {
            return ParseResult.failure(List.of(
                    new ParseError(0, 0, ErrorCode.IO_ERROR)));
        }
    }

    /* --------------------------------------------------------------
       BARE-IDENTIFIER HELPERS
       -------------------------------------------------------------- */
    private boolean isValidBareIdentifier(String id) {
        return id.matches("^[a-zA-Z_][a-zA-Z0-9._]*$");
    }

    /* --------------------------------------------------------------
       ERROR REPORTING
       -------------------------------------------------------------- */
    private void error(ErrorCode code, int line, int col) {
        errCount++;
        if (errors.size() < MAX_ERRORS) {
            errors.add(new ParseError(line, col, code));
        }
        recover();
    }

    public int getErrorCount() { return errCount; }

    /* --------------------------------------------------------------
       MAIN PARSING
       -------------------------------------------------------------- */
    private Module parseSource(Dialect hint) {
        var module = new Module();

        skipWhitespace();                     // leading layout
        module.dialect = detectDialect(hint);

        while (!isEOF()) {
            skipWhitespace();

            if (errShebang(module)) {         // shebang after statements?
                recover();
                continue;
            }

            // every top-level statement starts with an identifier
            int len = isIdentifier();
            if (len > 0) {
                String id = consume(len);
                if (isKeyword(id)) {
                    error(ErrorCode.IDENTIFIER_IS_KEYWORD, line, col);
                    recover();
                    continue;
                }
                module.addIdentifier(id);

                // ----- optional attributes -----
                skipWhitespace();
                var attrs = List.<Attribute>of();
                if (consumeOperator(Operator.AT)) {
                    var attrRes = parseAttributeBlock();
                    attrs = attrRes.isSuccess() ? attrRes.value() : List.of();
                    if (!attrRes.isSuccess()) {
                        errors.addAll(attrRes.errors());
                        recover();
                        continue;
                    }
                }

                // ----- := value -----
                skipWhitespace();
                if (!isOperator(Operator.ASSIGN)) {
                    error(ErrorCode.EXPECTED_ASSIGN, line, col);
                    recover();
                    continue;
                }
                consume(Operator.ASSIGN.symbol().length());   // ":="

                skipWhitespace();
                var valueRes = parseValue();
                if (!valueRes.isSuccess()) {
                    errors.addAll(valueRes.errors());
                    recover();
                    continue;
                }

                // --- NEW: Consume optional comma or layout ---
                skipWhitespace();
                if (isOperator(Operator.COMMA)) {
                    consumeOperator(Operator.COMMA);
                    skipWhitespace();
                }

                // build statement
                Statement stmt = new Assignment(id, valueRes.value(), attrs);
                module.addStatement(stmt);
                continue;
            }

            // unexpected token
            error(ErrorCode.UNEXPECTED_TOKEN, line, col);
            recover();
        }

        module.isParsed = true;
        return module;
    }

    private Dialect detectDialect(Dialect hint) {
        skipLeadingLayout();
        if (isShebang()) {
            String token = consume(5); // "#!asl" or "#!aml"
            return Dialect.fromShebang(token);
        }
        return hint;
    }

    /* --------------------------------------------------------------
       ATTRIBUTE BLOCK @[ … ]
       -------------------------------------------------------------- */
    private ValueParseResult<List<Attribute>> parseAttributeBlock() {
        inAttributeContext = true;
        try {
            if (!isOperator(Operator.L_BRACKET)) return success(List.of());
            int startLine = line, startCol = col;
            consume(); // eat '['

            List<Attribute> attrs = new ArrayList<>();
            while (true) {
                skipWhitespace();
                if (isOperator(Operator.R_BRACKET)) { consume(); break; }

                int idLen = isIdentifier();
                if (idLen == 0) return failure(ErrorCode.EXPECTED_IDENTIFIER, line, col);
                String key = consume(idLen);

                Value val = null;
                skipWhitespace();
                if (consumeOperator(Operator.EQUAL)) {
                    skipWhitespace();
                    var literal = parseLiteral();
                    if (!literal.isSuccess()) return literal.cast();
                    val = literal.value();
                }
                attrs.add(new Attribute(key, val));

                skipWhitespace();
                if (!isOperator(Operator.COMMA)) break;
                consume(); // comma
            }
            if (!consumeOperator(Operator.R_BRACKET)) {
                return failure(ErrorCode.EXPECTED_ARRAY_CLOSE, startLine, startCol);
            }
            return success(attrs);
        } finally {
            inAttributeContext = false;
        }
    }

    /* --------------------------------------------------------------
       VALUE PARSING (including bare identifiers)
       -------------------------------------------------------------- */
    private @NotNull ValueParseResult<Value> parseValue() {
        // BARE IDENTIFIER (stone, block.stone) – only outside attribute context
        if (!inAttributeContext) {
            int idLen = isIdentifier();
            if (idLen > 0) {
                String id = consume(idLen);
                if (!isKeyword(id) && isValidBareIdentifier(id)) {
                    return ValueParseResult.success(new Value.StringValue(id));
                }
            }
        }

        // object
        if (isOperator(Operator.L_BRACE)) {
            return parseAnonymousObject();
        }
        // array
        if (isOperator(Operator.L_BRACKET)) {
            return parseArray();
        }
        // free-form @`…`
        if (is("@")) {
            return parseFreeform();
        }
        // literals
        if (is("null")) {
            consume(4);
            return success(new Value.NullValue());
        }
        if (is("true")) {
            consume(4);
            return success(new Value.BooleanValue(true));
        }
        if (is("false")) {
            consume(5);
            return success(new Value.BooleanValue(false));
        }
        // anonymous identifier (fallback – kept for compatibility)
        int idLen = isIdentifier();
        if (idLen > 0) {
            String id = consume(idLen);
            if (!isKeyword(id)) {
                return ValueParseResult.success(new Value.AnonymousValue(id));
            }
        }
        // string
        if (isOperator(Operator.QUOTE)) {
            consumeOperator(Operator.QUOTE);
            String value = matchString();
            if (isOperator(Operator.QUOTE)) {
                consumeOperator(Operator.QUOTE);
                return success(new Value.StringValue(value));
            } else {
                return failure(ErrorCode.UNTERMINATED_STRING, line, col);
            }
        }

        // number
        NumericParseResult numeric = parseNumber();
        if (numeric.isSuccess()) {
            return ValueParseResult.success(numeric.value());
        } else if (!numeric.errors().isEmpty()) {
            return ValueParseResult.failure(numeric.errors());
        }

        return failure(ErrorCode.EXPECTED_VALUE, line, col);
    }

    private ValueParseResult<Value> parseLiteral() {
        var vp = parseValue();
        if (!vp.isSuccess()) return vp;

        Value v = vp.value();
        if (v instanceof Value.ObjectValue || v instanceof Value.ArrayValue) {
            return ValueParseResult.failure(ErrorCode.INVALID_VALUE_IN_ATTRIBUTE, line, col);
        }
        return vp;
    }

    /* --------------------------------------------------------------
       OBJECT, ARRAY, FREEFORM, NUMBER, etc.
       -------------------------------------------------------------- */
    private @NotNull ValueParseResult<Value> parseAnonymousObject() {
        List<Map.Entry<String, Value>> fields = new ArrayList<>();
        List<ParseError> errs = new ArrayList<>();
        int start = pos;
        boolean loop = !isOperator(Operator.R_BRACE);

        consumeOperator(Operator.L_BRACE);
        while (!isEOF() && loop) {
            skipWhitespace();

            int idLen = isIdentifier();
            if (idLen == 0) {
                errs.add(new ParseError(line, col, ErrorCode.EXPECTED_OBJECT_FIELD));
                recoverToObjectEnd();
                break;
            }
            String field = consume(idLen);
            if (isKeyword(field)) {
                errs.add(new ParseError(line, col, ErrorCode.INVALID_KEY_IN_OBJECT));
                recoverToObjectEnd();
                break;
            }

            skipWhitespace();
            if (!isOperator(Operator.ASSIGN)) {
                errs.add(new ParseError(line, col, ErrorCode.EXPECTED_ASSIGN));
                recoverToObjectEnd();
                break;
            }
            consumeOperator(Operator.ASSIGN);

            skipWhitespace();
            var valueResult = parseValue();
            if (!valueResult.isSuccess()) {
                errs.addAll(valueResult.errors());
                recoverToObjectEnd();
                break;
            }

            if (fields.contains(field)) {
                errs.add(new ParseError(line, col, ErrorCode.DUPLICATE_FIELD_IN_OBJECT));
            } else {
                fields.add(Map.entry(field, valueResult.value()));
            }

            if (isOperator(Operator.COMMA)) {
                consumeOperator(Operator.COMMA);
            }
            if (isOperator(Operator.NEWLINE)) {
                consumeOperator(Operator.NEWLINE);
            }

            skipWhitespace();
            loop = !isOperator(Operator.R_BRACE);
        }

        if (!isOperator(Operator.R_BRACE)) {
            errs.add(new ParseError(line, col, ErrorCode.EXPECTED_OBJECT_CLOSE));
            recoverToObjectEnd();
            return ValueParseResult.failure(errs);
        }
        consumeOperator(Operator.R_BRACE);

        String sourceText = source.substring(start, pos);
        return errs.isEmpty()
                ? ValueParseResult.success(new Value.ObjectValue(fields, sourceText))
                : ValueParseResult.failure(errs);
    }

    private ValueParseResult<Value> parseArray() {
        int fullStart = pos;
        int startLine = line, startCol = col;
        List<ParseError> errs = new ArrayList<>();
        List<Value> elements = new ArrayList<>();

        consume(); // '['

        while (!isEOF() && !isOperator(Operator.R_BRACKET)) {
            skipWhitespace();

            if (isOperator(Operator.COMMA)) {
                consumeOperator(Operator.COMMA);
                skipWhitespace();
                if (isOperator(Operator.R_BRACKET)) {
                    errs.add(new ParseError(line, col, ErrorCode.TRAILING_COMMA_IN_ARRAY));
                    break;
                }
                continue;
            }

            ValueParseResult<Value> elemRes = parseValue();
            if (!elemRes.isSuccess()) {
                errs.addAll(elemRes.errors());
                recoverToArrayEnd();
                break;
            }
            elements.add(elemRes.value());

            skipWhitespace();
            if (isOperator(Operator.COMMA)) {
                consumeOperator(Operator.COMMA);
                skipWhitespace();
            }
        }

        if (!isOperator(Operator.R_BRACKET)) {
            errs.add(new ParseError(line, col, ErrorCode.EXPECTED_ARRAY_CLOSE));
            recoverToArrayEnd();
            return ValueParseResult.failure(errs);
        }
        consumeOperator(Operator.R_BRACKET);

        String sourceText = source.substring(fullStart, pos);
        return errs.isEmpty()
                ? ValueParseResult.success(new Value.ArrayValue(List.copyOf(elements), sourceText))
                : ValueParseResult.failure(errs);
    }

    private ValueParseResult<Value> parseFreeform() {
        int startLine = line, startCol = col;
        consume(); // '@'

        String attrib = null;
        int idLen = isIdentifier();
        if (idLen > 0) {
            attrib = consume(idLen);
            if (isKeyword(attrib)) {
                return failure(ErrorCode.ATTRIBUTE_IS_KEYWORD, startLine, startCol);
            }
        }

        if (!isOperator(Operator.BACKTICK)) {
            return failure(ErrorCode.EXPECTED_BACKTICK, startLine, startCol);
        }
        consumeOperator(Operator.BACKTICK);

        int contentStart = pos;
        while (!isEOF()) {
            if (isOperator(Operator.BACKTICK) && !isEscaped(pos)) break;
            consume();
        }
        String rawContent = source.substring(contentStart, pos);

        if (!isOperator(Operator.BACKTICK)) {
            return failure(ErrorCode.UNTERMINATED_FREEFORM, line, col);
        }
        consumeOperator(Operator.BACKTICK);

        return success(new Value.FreeformValue("`" + rawContent + "`", attrib));
    }

    private boolean isEscaped(int pos) {
        int backslashes = 0;
        for (int i = pos - 1; i >= 0 && source.charAt(i) == '\\'; i--) backslashes++;
        return backslashes % 2 == 1;
    }

    private NumericParseResult parseNumber() {
        int start = pos;
        int startLine = line, startCol = col;
        boolean isHex = false;

        if (peek(0) == '-') consume();

        if (peek(0) == '#' && isHexDigit(peek(1))) {
            consume(); // #
            isHex = true;
            while (isHexDigit(peek(0))) consume();
        } else {
            while (isDigit(peek(0))) consume();

            if (peek(0) == '.') {
                consume();
                while (isDigit(peek(0))) consume();
            }

            if (peek(0) == 'e' || peek(0) == 'E') {
                consume();
                if (peek(0) == '+' || peek(0) == '-') consume();
                if (!isDigit(peek(0))) {
                    return NumericParseResult.failure(ErrorCode.INVALID_EXPONENT, startLine, startCol);
                }
                while (isDigit(peek(0))) consume();
            }
        }

        String numStr = source.substring(start, pos);
        try {
            double value = isHex
                    ? Long.parseLong(numStr.substring(1), 16)
                    : Double.parseDouble(numStr);
            return NumericParseResult.success(new Value.NumberValue(value, numStr));
        } catch (NumberFormatException e) {
            return NumericParseResult.failure(ErrorCode.INVALID_NUMBER, startLine, startCol);
        }
    }

    /* --------------------------------------------------------------
       LOW-LEVEL HELPERS
       -------------------------------------------------------------- */
    private boolean isEOF() { return pos >= source.length(); }

    private boolean isAlpha(char c) { return Character.isLetter(c) || c == '_'; }

    private boolean isAlphaNumeric(char c) { return Character.isLetterOrDigit(c) || c == '_'; }

    private static boolean isDigit(char c) { return c >= '0' && c <= '9'; }

    private static boolean isHexDigit(char c) {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private char consume() {
        if (isEOF()) return '\0';
        char c = source.charAt(pos++);
        if (c == '\n') { line++; col = 1; } else { col++; }
        return c;
    }

    private @NotNull String consume(int n) {
        if (n == 0) return "";
        char[] buf = new char[n];
        for (int i = 0; i < n; i++) buf[i] = consume();
        return new String(buf);
    }

    private boolean consumeOperator(Operator op) {
        if (isOperator(op)) {
            consume(op.length());
            return true;
        }
        return false;
    }

    private char peek() { return peek(0); }

    private char peek(int offset) {
        int ndx = pos + offset;
        return ndx < source.length() ? source.charAt(ndx) : '\0';
    }

    private @NotNull String matchString() {
        StringBuilder sb = new StringBuilder();
        while (!isEOF() && !isOperator(Operator.QUOTE)) {
            char c = consume();
            if (c == '\\') {
                char next = peek();
                switch (next) {
                    case 'n' -> { consume(); sb.append('\n'); }
                    case 't' -> { consume(); sb.append('\t'); }
                    case 'r' -> { consume(); sb.append('\r'); }
                    case '\\' -> { consume(); sb.append('\\'); }
                    case '"' -> { consume(); sb.append('"'); }
                    default -> sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private boolean isShebang() {
        boolean match = is("#!asl") || is("#!aml");
        if (match) hasShebang = true;
        return match;
    }

    private int isIdentifier() {
        int len = 0;
        if (isEOF() || !isAlpha(peek())) return 0;
        while (isAlphaNumeric(peek(len))) len++;
        return len;
    }

    private boolean is(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (isEOF() || peek(i) != s.charAt(i)) return false;
        }
        return true;
    }

    private boolean isOperator(Operator op) { return is(op.symbol()); }

    private boolean isKeyword(String s) { return KEYWORDS.contains(s); }

    private void skipLeadingLayout() {
        while (!isEOF()) {
            skipWhitespace();
            if (isEOF()) break;
            if (peek() == '/' && peek(1) == '/') skipLineComment();
            else if (peek() == '/' && peek(1) == '*') skipBlockComment();
            else break;
        }
    }

    private void skipWhitespace() {
        while (true) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') consume();
            else if (c == '/' && peek(1) == '/') skipLineComment();
            else if (c == '/' && peek(1) == '*') skipBlockComment();
            else break;
        }
    }

    private void skipLineComment() {
        while (peek() != '\n' && !isEOF()) consume();
    }

    private void skipBlockComment() {
        consume(2);
        int nest = 1;
        while (nest > 0 && !isEOF()) {
            if (peek() == '*' && peek(1) == '/') { consume(2); nest--; }
            else if (peek() == '/' && peek(1) == '*') { consume(2); nest++; }
            else consume();
        }
    }

    private boolean errShebang(Module module) {
        if (peek() == '#' && isShebang()) {
            if (hasShebang) error(ErrorCode.MULTIPLE_SHEBANG, line, col);
            if (module.hasStatements()) error(ErrorCode.SHEBANG_AFTER_STATEMENTS, line, col);
            return true;
        }
        return false;
    }

    private void recover() {
        while (!isEOF() && peek() != '\n' && peek() != ';') consume();
    }

    private void recoverToObjectEnd() {
        int depth = 1;
        while (!isEOF() && depth > 0) {
            if (isOperator(Operator.L_BRACE)) depth++;
            if (isOperator(Operator.R_BRACE)) depth--;
            consume();
        }
    }

    private void recoverToArrayEnd() {
        int depth = 1;
        while (!isEOF() && depth > 0) {
            if (isOperator(Operator.L_BRACKET)) depth++;
            if (isOperator(Operator.R_BRACKET)) depth--;
            consume();
        }
    }
}