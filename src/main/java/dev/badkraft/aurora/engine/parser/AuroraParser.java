// src/main/java/dev/badkraft/aurora/engine/parser/AuroraParser.java
package dev.badkraft.aurora.engine.parser;

import dev.badkraft.aurora.engine.utilities.Utils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.*;
import java.io.IOException;
import java.util.*;

import static dev.badkraft.aurora.engine.parser.ValueParseResult.failure;
import static dev.badkraft.aurora.engine.parser.ValueParseResult.success;

public class AuroraParser {
    public static final Set<String> KEYWORDS = Set.of(
            "true", "false", "null", "vars", "include"
    );

    private final String namespace;
    private final String source;
    private final List<ParseError> errors = new ArrayList<>();
    private boolean hasShebang = false;
    private int pos = 0;
    private int line = 1;
    private int col = 1;

    private AuroraParser(String namespace, String source) {
        this.namespace = namespace;
        this.source = source;
    }

    public static ParseResult<Module> parse(Path path) {
        try {
            String content = Files.readString(path);
            Dialect dialect = Dialect.fromFileExtension(Utils.getFileExtension(path));
            String name = path.getFileName().toString().replaceFirst("[.][^.]+$", "");
            AuroraParser parser = new AuroraParser(name, content);
            var module = parser.parseSource(dialect);
            return parser.errors.isEmpty()
                    ? ParseResult.success(module)
                    : ParseResult.failure(parser.errors);
        } catch (IOException e) {
            return ParseResult.failure(List.of(new ParseError(0, 0, ErrorCode.IO_ERROR)));
        }
    }

    private Module parseSource(Dialect hint) {
        var module = new Module(namespace);
        skipWhitespace();
        module.dialect = detectDialect(hint);

        while (!isEOF()) {
            skipWhitespace();
            if (errShebang(module)) {
                recover();
                continue;
            }

            int len = isIdentifier();
            if (len == 0) {
                error(ErrorCode.UNEXPECTED_TOKEN, line, col);
                recover();
                continue;
            }

            String id = consume(len);
            if (isKeyword(id)) {
                error(ErrorCode.IDENTIFIER_IS_KEYWORD, line, col);
                recover();
                continue;
            }

            module.addIdentifier(id);

            // Attributes
            List<Attribute> attrs = List.of();
            skipWhitespace();
            if (consumeOperator(Operator.AT)) {
                var attrRes = parseAttributeBlock();
                attrs = attrRes.isSuccess() ? attrRes.value() : List.of();
                if (!attrRes.isSuccess()) {
                    errors.addAll(attrRes.errors());
                    recover();
                    continue;
                }
            }

            // :=
            skipWhitespace();
            if (!isOperator(Operator.ASSIGN)) {
                error(ErrorCode.EXPECTED_ASSIGN, line, col);
                recover();
                continue;
            }
            consumeOperator(Operator.ASSIGN);

            skipWhitespace();
            var valueRes = parseValue();
            if (!valueRes.isSuccess()) {
                errors.addAll(valueRes.errors());
                recover();
                continue;
            }

            Statement stmt = new Assignment(id, valueRes.value(), attrs);
            module.addStatement(stmt);

            skipWhitespace();
        }

        module.isParsed = true;
        return module;
    }

    private Dialect detectDialect(Dialect hint) {
        skipLeadingLayout();
        if (isShebang()) {
            String token = consume(5);
            return Dialect.fromShebang(token);
        }
        return hint;
    }

    private ValueParseResult<List<Attribute>> parseAttributeBlock() {
        if (!isOperator(Operator.L_BRACKET)) return success(List.of());
        int startLine = line, startCol = col;
        consumeOperator(Operator.L_BRACKET);

        List<Attribute> attrs = new ArrayList<>();
        while (true) {
            skipWhitespace();
            if (isOperator(Operator.R_BRACKET)) {
                consumeOperator(Operator.R_BRACKET);
                break;
            }

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
            consumeOperator(Operator.COMMA);
        }

        if (!isOperator(Operator.R_BRACKET)) {
            return failure(ErrorCode.EXPECTED_ARRAY_CLOSE, startLine, startCol);
        }
        consumeOperator(Operator.R_BRACKET);
        return success(attrs);
    }

    private @NotNull ValueParseResult<Value> parseValue() {
        skipWhitespace();

        // := is ONLY allowed at top level — inside containers it's illegal
        if (isOperator(Operator.ASSIGN)) {
            return failure(ErrorCode.ASSIGNMENT_NOT_ALLOWED_HERE, line, col);
        }

        if (isOperator(Operator.L_PAREN)) return parseTuple();
        if (isOperator(Operator.L_BRACE)) return parseObject();
        if (isOperator(Operator.L_BRACKET)) return parseArray();
        if (is("@")) return parseBlob();

        if (is("null")) { consume(4); return success(new Value.NullValue()); }
        if (is("true")) { consume(4); return success(new Value.BooleanValue(true)); }
        if (is("false")) { consume(5); return success(new Value.BooleanValue(false)); }

        if (isOperator(Operator.QUOTE)) {
            consumeOperator(Operator.QUOTE);
            String value = matchString();
            if (!isOperator(Operator.QUOTE)) return failure(ErrorCode.UNTERMINATED_STRING, line, col);
            consumeOperator(Operator.QUOTE);
            return success(new Value.StringValue(value));
        }

        int savedPos = pos, savedLine = line, savedCol = col;
        var numRes = parseNumber();
        if (numRes.isSuccess()) return success(numRes.value());
        pos = savedPos; line = savedLine; col = savedCol;

        int litLen = isBareLiteral();
        if (litLen > 0) {
            String id = consume(litLen);
            if (!isKeyword(id)) return success(new Value.BareLiteral(id));
        }

        return failure(ErrorCode.UNEXPECTED_TOKEN, line, col);
    }

    private ValueParseResult<Value> parseLiteral() {
        var vp = parseValue();
        if (!vp.isSuccess()) return vp;
        Value v = vp.value();
        if (v instanceof Value.ObjectValue || v instanceof Value.ArrayValue ||
                v instanceof Value.TupleValue || v instanceof Value.BlobValue) {
            return failure(ErrorCode.INVALID_VALUE_IN_ATTRIBUTE, line, col);
        }
        return vp;
    }

    private NumericParseResult parseNumber() {
        int start = pos, startLine = line, startCol = col;
        boolean isHex = false;

        if (peek() == '-') consume();

        if (peek() == '#' && isHexDigit(peek(1))) {
            consume(); isHex = true;
            while (isHexDigit(peek())) consume();
        } else {
            while (isDigit(peek())) consume();
            if (peek() == '.') {
                consume();
                while (isDigit(peek())) consume();
            }
            if (peek() == 'e' || peek() == 'E') {
                consume();
                if (peek() == '+' || peek() == '-') consume();
                if (!isDigit(peek())) return NumericParseResult.failure(ErrorCode.INVALID_EXPONENT, startLine, startCol);
                while (isDigit(peek())) consume();
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

    private @NotNull ValueParseResult<Value> parseObject() {
        int start = pos;
        List<Map.Entry<String, Value>> fields = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();

        consumeOperator(Operator.L_BRACE);

        while (!isEOF() && !isOperator(Operator.R_BRACE)) {
            skipWhitespace();

            int idLen = isIdentifier();
            if (idLen == 0) {
                errors.add(new ParseError(line, col, ErrorCode.EXPECTED_OBJECT_FIELD));
                recoverToObjectEnd();
                break;
            }
            String field = consume(idLen);
            if (isKeyword(field)) {
                errors.add(new ParseError(line, col, ErrorCode.INVALID_KEY_IN_OBJECT));
                recoverToObjectEnd();
                break;
            }

            skipWhitespace();
            if (!isOperator(Operator.ASSIGN)) {
                errors.add(new ParseError(line, col, ErrorCode.EXPECTED_ASSIGN));
                recoverToObjectEnd();
                break;
            }
            consumeOperator(Operator.ASSIGN);

            skipWhitespace();
            var valueRes = parseValue();
            if (!valueRes.isSuccess()) {
                errors.addAll(valueRes.errors());
                recoverToObjectEnd();
                break;
            }

            fields.add(Map.entry(field, valueRes.value()));

            skipWhitespace();
            if (isOperator(Operator.COMMA)) {
                consumeOperator(Operator.COMMA);
                skipWhitespace();
            }
        }

        if (!isOperator(Operator.R_BRACE)) {
            errors.add(new ParseError(line, col, ErrorCode.EXPECTED_OBJECT_CLOSE));
            recoverToObjectEnd();
            return failure(errors);
        }
        consumeOperator(Operator.R_BRACE);

        String sourceText = source.substring(start, pos);
        return errors.isEmpty()
                ? success(new Value.ObjectValue(fields, sourceText))
                : failure(errors);
    }

    private ValueParseResult<Value> parseArray() {
        int start = pos;
        List<Value> elements = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();

        consumeOperator(Operator.L_BRACKET);

        while (!isEOF() && !isOperator(Operator.R_BRACKET)) {
            skipWhitespace();

            // Do NOT allow leading commas — only between elements
            // This is the key difference from JSON
//            if (isOperator(Operator.COMMA)) {
//                consumeOperator(Operator.COMMA);
//                skipWhitespace();
//                continue;
//            }

            var elemRes = parseValue();
            if (!elemRes.isSuccess()) {
                errors.addAll(elemRes.errors());
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
            errors.add(new ParseError(line, col, ErrorCode.EXPECTED_ARRAY_CLOSE));
            recoverToArrayEnd();
            return failure(errors);
        }
        consumeOperator(Operator.R_BRACKET);

        String sourceText = source.substring(start, pos);
        return success(new Value.ArrayValue(List.copyOf(elements), sourceText));
    }

    private @NotNull ValueParseResult<Value> parseTuple() {
        int start = pos;
        List<Value> elements = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();

        consumeOperator(Operator.L_PAREN);
        boolean first = true;

        while (!isEOF() && !isOperator(Operator.R_PAREN)) {
            skipWhitespace();

            if (!first) {
                if (!isOperator(Operator.COMMA)) break;
                consumeOperator(Operator.COMMA);
                skipWhitespace();
            }
            first = false;

            var elemRes = parseValue();
            if (!elemRes.isSuccess()) {
                errors.addAll(elemRes.errors());
                recoverToTupleEnd();
                break;
            }
            elements.add(elemRes.value());
        }

        if (elements.size() < 2) {
            errors.add(new ParseError(line, col, ErrorCode.TUPLE_TOO_SHORT));
        }

        if (!isOperator(Operator.R_PAREN)) {
            errors.add(new ParseError(line, col, ErrorCode.EXPECTED_TUPLE_CLOSE));
            recoverToTupleEnd();
            return failure(errors);
        }
        consumeOperator(Operator.R_PAREN);

        String sourceText = source.substring(start, pos);
        skipWhitespace();

        if (isOperator(Operator.ROCKET)) {
            error(ErrorCode.ROCKET_OP_NOT_VALID, line, col);
            recover();
        }

        return errors.isEmpty()
                ? success(new Value.TupleValue(elements, sourceText))
                : failure(errors);
    }

    private ValueParseResult<Value> parseBlob() {
        int startLine = line, startCol = col;
        consume();

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
        while (!isEOF() && !(isOperator(Operator.BACKTICK) && !isEscaped(pos))) {
            consume();
        }
        String content = source.substring(contentStart, pos);

        if (!isOperator(Operator.BACKTICK)) {
            return failure(ErrorCode.UNTERMINATED_FREEFORM, line, col);
        }
        consumeOperator(Operator.BACKTICK);

        return success(new Value.BlobValue(content, attrib));
    }

    private boolean isEscaped(int pos) {
        int backslashes = 0;
        for (int i = pos - 1; i >= 0 && source.charAt(i) == '\\'; i--) backslashes++;
        return backslashes % 2 == 1;
    }

    private int isBareLiteral() {
        int length = 0;
        char first = peek(length);
        if (!Character.isLetter(first) && first != '_') return 0;
        length++;

        while (true) {
            char c = peek(length);
            if (c == '\0') break;
            if (Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == ':') {
                length++;
            } else {
                break;
            }
        }
        return length;
    }

    // === Layout & Helpers ===
    private boolean isEOF() { return pos >= source.length(); }
    private char peek() { return peek(0); }
    private char peek(int offset) {
        int index = pos + offset;
        return index < source.length() ? source.charAt(index) : '\0';
    }
    private char consume() {
        if (isEOF()) return '\0';
        char c = source.charAt(pos++);
        if (c == '\n') { line++; col = 1; } else col++;
        return c;
    }
    private String consume(int n) {
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

    private void skipWhitespace() {
        while (!isEOF()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') consume();
            else if (c == '/' && peek(1) == '/') skipLineComment();
            else if (c == '/' && peek(1) == '*') skipBlockComment();
            else break;
        }
    }

    private void skipLeadingLayout() {
        while (!isEOF()) {
            skipWhitespace();
            if (isEOF()) break;
            if (peek() == '/' && peek(1) == '/') skipLineComment();
            else if (peek() == '/' && peek(1) == '*') skipBlockComment();
            else break;
        }
    }

    private void skipLineComment() { while (peek() != '\n' && !isEOF()) consume(); }
    private void skipBlockComment() {
        consume(2);
        int depth = 1;
        while (depth > 0 && !isEOF()) {
            if (peek() == '*' && peek(1) == '/') { consume(2); depth--; }
            else if (peek() == '/' && peek(1) == '*') { consume(2); depth++; }
            else consume();
        }
    }

    private boolean isShebang() {
        if (is("#!asl") || is("#!aml")) {
            hasShebang = true;
            return true;
        }
        return false;
    }

    private int isIdentifier() {
        int len = 0;
        if (isEOF() || !isAlpha(peek())) return 0;
        while (isAlphaNumeric(peek(len))) len++;
        return len;
    }

    private boolean isAlpha(char c) { return Character.isLetter(c) || c == '_'; }
    private boolean isAlphaNumeric(char c) { return Character.isLetterOrDigit(c) || c == '_'; }
    private static boolean isDigit(char c) { return c >= '0' && c <= '9'; }
    private static boolean isHexDigit(char c) {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private boolean is(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (peek(i) != s.charAt(i)) return false;
        }
        return true;
    }

    private boolean isOperator(Operator op) { return is(op.symbol()); }
    private boolean isKeyword(String s) { return KEYWORDS.contains(s); }

    private @NotNull String matchString() {
        StringBuilder sb = new StringBuilder();
        while (!isEOF() && !isOperator(Operator.QUOTE)) {
            char c = consume();
            if (c == '\\') {
                char next = peek();
                sb.append(switch (next) {
                    case 'n' -> { consume(); yield '\n'; }
                    case 't' -> { consume(); yield '\t'; }
                    case 'r' -> { consume(); yield '\r'; }
                    case '\\', '"' -> { consume(); yield next; }
                    default -> c;
                });
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void error(ErrorCode code, int line, int col) {
        errors.add(new ParseError(line, col, code));
        recover();
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

    private void recoverToTupleEnd() {
        int depth = 1;
        while (!isEOF() && depth > 0) {
            if (isOperator(Operator.L_PAREN)) depth++;
            if (isOperator(Operator.R_PAREN)) depth--;
            consume();
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
}