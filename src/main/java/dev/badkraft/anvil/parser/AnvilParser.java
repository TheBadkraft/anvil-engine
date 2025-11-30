// dev.badkraft.anvil.parser.AnvilParser.java
package dev.badkraft.anvil.parser;

import dev.badkraft.anvil.core.api.Context;
import dev.badkraft.anvil.core.data.*;
import dev.badkraft.anvil.core.data.Value.*;

import java.util.*;

import static dev.badkraft.anvil.core.data.Operator.*;
import static dev.badkraft.anvil.parser.ErrorCode.*;

public final class AnvilParser {

    private final Context context;
    private final Source source;

    private AnvilParser(Context context) {
        this.context = context;
        this.source = context.source();
    }

    public static void parse(Context context) {
        new AnvilParser(context).parseSource();
    }

    private void parseSource() {
        source.skipWhitespace();

        while (source.is("@[")) {
            List<Attribute> attrs = parseAttributeBlock();
            context.addAllAttributes(attrs);
            source.skipWhitespace();
        }

        while (!source.isEOF()) {
            Statement stmt = parseStatement();
            context.addStatement(stmt);
            source.skipWhitespace();
        }

        context.markParsed();
    }

    private Statement parseStatement() {
        String key = readIdentifier();
        if (key.isEmpty()) raise(EXPECTED_IDENTIFIER);

        String base = null;
        source.skipWhitespace();
        if (source.is(":") && !source.isOperator(ASSIGN)) {
            source.consume(1);
            source.skipWhitespace();
            base = readIdentifier();
            if (base.isEmpty()) raise(EXPECTED_IDENTIFIER);
            source.skipWhitespace();
        }

        List<Attribute> attrs = parseAttributeBlock();
        source.skipWhitespace();

        if (!source.isOperator(ASSIGN)) raise(EXPECTED_ASSIGN);
        source.consumeOperator(ASSIGN);
        source.skipWhitespace();

        int valueStart = source.position();
        Value value = parseValue();
        int valueEnd = source.position();

        if (!attrs.isEmpty()) {
            value.getAttributes().addAll(attrs);
        }

        if (source.isOperator(COMMA)) source.consumeOperator(COMMA);

        Assignment assignment = new Assignment(key, attrs, value, base);
        context.addIdentifier(key);
        return assignment;
    }

    private Value parseValue() {
        if (source.isOperator(L_BRACE))   return parseObject();
        if (source.isOperator(L_BRACKET)) return parseArray();
        if (source.isOperator(L_PAREN))   return parseTuple();
        if (source.is("\""))              return parseString();

        int hexPrefix = source.matchHexPrefix();
        if (hexPrefix > 0) {
            source.consume(hexPrefix);
            return parseHexAfterPrefix(hexPrefix == 1);
        }

        if (source.is("true"))  { source.consume(4); return context.bool(true,  source.position()-4, source.position()); }
        if (source.is("false")) { source.consume(5); return context.bool(false, source.position()-5, source.position()); }
        if (source.is("null"))  { source.consume(4); return context.nullVal(    source.position()-4, source.position()); }

        if (source.isOperator(AT) || source.isOperator(BACKTICK)) return parseBlob();

        int bareLen = matchBareLiteral();
        if (bareLen > 0) {
            source.consume(bareLen);
            return context.bare(source.position() - bareLen, source.position());
        }

        return parseNumber();
    }

    private Value parseObject() {
        int start = source.position();
        source.consumeOperator(L_BRACE);
        source.skipWhitespace();

        if (source.isOperator(R_BRACE)) raise(EMPTY_OBJECT_NOT_ALLOWED);

        List<Map.Entry<String, Value>> fields = new ArrayList<>();
        List<Attribute> objAttrs = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        while (!source.isOperator(R_BRACE)) {
            String key = readIdentifier();
            if (key.isEmpty()) raise(EXPECTED_IDENTIFIER);
            if (!seen.add(key)) raise(DUPLICATE_FIELD_IN_OBJECT);

            source.skipWhitespace();
            List<Attribute> fieldAttrs = parseAttributeBlock();
            source.skipWhitespace();

            if (!source.isOperator(ASSIGN)) raise(EXPECTED_ASSIGN);
            source.consumeOperator(ASSIGN);
            source.skipWhitespace();

            Value value = parseValue();
            if (!fieldAttrs.isEmpty()) value.getAttributes().addAll(fieldAttrs);
            fields.add(Map.entry(key, value));

            source.skipWhitespace();
            if (source.isOperator(COMMA)) {
                source.consumeOperator(COMMA);
                source.skipWhitespace();
            }
        }

        source.consumeOperator(R_BRACE);
        return context.object(fields, objAttrs, start, source.position());
    }

    private Value parseArray() {
        int start = source.position();
        source.consumeOperator(L_BRACKET);
        source.skipWhitespace();

        List<Value> elements = new ArrayList<>();
        List<Attribute> attrs = new ArrayList<>();

        while (!source.isOperator(R_BRACKET)) {
            elements.add(parseValue());
            source.skipWhitespace();
            if (!source.isOperator(R_BRACKET)) {
                if (!source.isOperator(COMMA)) raise(MISSING_COMMA_IN_ARRAY);
                source.consumeOperator(COMMA);
                source.skipWhitespace();
            }
        }

        source.consumeOperator(R_BRACKET);
        return context.array(elements, attrs, start, source.position());
    }

    private Value parseTuple() {
        int start = source.position();
        source.consumeOperator(L_PAREN);
        source.skipWhitespace();

        if (source.isOperator(R_PAREN)) {
            raise(EMPTY_TUPLE_ELEMENT);
        }

        List<Value> elements = new ArrayList<>();
        List<Attribute> attrs = new ArrayList<>();

        // First element (required)
        elements.add(parseValue());
        source.skipWhitespace();

        // Zero or more: , value
        while (source.isOperator(COMMA)) {
            source.consumeOperator(COMMA);
            source.skipWhitespace();

            if (source.isOperator(R_PAREN)) {
                raise(EXPECTED_VALUE);
            }

            elements.add(parseValue());
            source.skipWhitespace();
        }

        // Closing parenthesis
        if (!source.isOperator(R_PAREN)) {
            raise(EXPECTED_TUPLE_CLOSE);
        }

        if (elements.size() < 2) {
            raise(TUPLE_TOO_SHORT);
        }

        source.consumeOperator(R_PAREN);
        return context.tuple(elements, attrs, start, source.position());
    }

    private Value parseString() {
        Content content = parseContent(QUOTE);
        return context.string(content.start(), content.end());
    }

    private Value parseBlob() {
        String attribute = null;
        int attrStart = -1;

        if (source.isOperator(AT)) {
            source.consumeOperator(AT);
            attrStart = source.position();
            attribute = readIdentifier();
            if (attribute.isEmpty()) raise(EXPECTED_IDENTIFIER);
        }
        Content content = parseContent(BACKTICK); // includes backticks
        return context.blob(attribute, content.start(), content.end());
    }

    private record Content(int start, int end) {}
    private Content parseContent(Operator delimiter) {
        if (delimiter != QUOTE && delimiter != Operator.BACKTICK) {
            raise(ErrorCode.UNEXPECTED_TOKEN);
        }

        int start = -1;
        // if QUOTE, we want to omit the surrounding quotes
        if (delimiter == QUOTE) {
            source.consumeOperator(delimiter);
            start = source.position();
        } else {
            // if BACKTICK, we want to capture the surrounding backticks
            start = source.position();
            source.consumeOperator(delimiter);
        }
        while (!source.isEOF()) {
            if (source.isOperator(delimiter) && !source.isEscaped(source.position())) {
                break;                       // found unescaped closing delimiter
            }
            source.consume();                       // normal char or escaped sequence
        }

        if (!source.isOperator(delimiter)) {
            raise(delimiter == QUOTE ? UNTERMINATED_STRING : UNTERMINATED_BLOB);
        }

        if (delimiter == QUOTE) {
            // for QUOTE, capture up to but not including closing "
            Content content = new Content(start, source.position());
            source.consumeOperator(delimiter);      // consume closing "
            return content;
        }

        source.consumeOperator(delimiter);          // consume closing `
        // Return the exact text we just parsed (including delimiters)
        return new Content(start, source.position());
    }

    private Value parseHexAfterPrefix(boolean isHash) {
        int start = source.position() - (isHash ? 1 : 2);
        StringBuilder digits = new StringBuilder();

        while (source.isHexDigit(source.peek()) || source.peek() == '_') {
            char c = source.peek();
            if (source.isHexDigit(c)) digits.append(c);
            source.consume();
        }

        long value = Long.parseLong(digits.toString(), 16);
        return isHash
                ? context.hex(value, start, source.position())
                : context.longVal(value, start, source.position());
    }

    private Value parseNumber() {
        int start = source.position();
        StringBuilder buf = new StringBuilder();

        if (source.peek() == '+' || source.peek() == '-') buf.append(source.consume());

        boolean hasDigit = false;
        while (source.isDigit(source.peek()) || source.peek() == '_') {
            char c = source.peek();
            if (source.isDigit(c)) { buf.append(c); hasDigit = true; }
            source.consume();
        }

        boolean isFloat = false;
        if (source.peek() == '.') {
            buf.append(source.consume());
            isFloat = true;
            while (source.isDigit(source.peek()) || source.peek() == '_') {
                if (source.isDigit(source.peek())) buf.append(source.consume());
                else source.consume();
            }
        }

        if (source.peek() == 'e' || source.peek() == 'E') {
            buf.append(source.consume());
            isFloat = true;
            if (source.peek() == '+' || source.peek() == '-') buf.append(source.consume());
            while (source.isDigit(source.peek()) || source.peek() == '_') {
                if (source.isDigit(source.peek())) buf.append(source.consume());
                else source.consume();
            }
        }

        if (!hasDigit) raise(INVALID_NUMBER);

        String clean = buf.toString().replace("_", "");
        if (isFloat) {
            double d = Double.parseDouble(clean);
            return context.doubleVal(d, start, source.position());
        } else {
            long l = Long.parseLong(clean);
            return context.longVal(l, start, source.position());
        }
    }

    private List<Attribute> parseAttributeBlock() {
        if (!source.is("@[")) return List.of();
        source.consume(2); // "@["
        source.skipWhitespace();

        List<Attribute> attrs = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        while (!source.is("]")) {
            String key = readIdentifier();
            if (key.isEmpty()) raise(INVALID_ATTRIBUTE);
            if (!seen.add(key)) raise(DUPLICATE_ATTRIBUTE_KEY);

            Value value = null;
            source.skipWhitespace();
            if (source.isOperator(EQUAL)) {
                source.consumeOperator(EQUAL);
                source.skipWhitespace();
                value = parseLiteralValue();
            }

            attrs.add(new Attribute(key, value));

            source.skipWhitespace();
            if (!source.is("]") && !source.isOperator(COMMA)) raise(MISSING_COMMA_IN_ATTRIBUTES);
            if (source.isOperator(COMMA)) source.consumeOperator(COMMA);
            source.skipWhitespace();
        }

        source.consume(1); // "]"
        return List.copyOf(attrs);
    }

    private Value parseLiteralValue() {
        int save = source.position();
        Value v = parseValue();
        if (v instanceof ObjectValue || v instanceof ArrayValue || v instanceof TupleValue || v instanceof BlobValue) {
            source.setPosition(save, source.line(), source.column());
            raise(INVALID_VALUE_IN_ATTRIBUTE);
        }
        return v;
    }

    // ------------------------------------------------------------------ //
    // Identifier / bare literal helpers (now fully Source-clean)
    // ------------------------------------------------------------------ //
    private String readIdentifier() {
        int len = matchIdentifier();
        if (len == 0) raise(EXPECTED_IDENTIFIER);
        String id = source.substring(source.position(), source.position() + len);
        source.consume(len);
        context.addIdentifier(id);
        return id;
    }

    private int matchIdentifier() {
        if (!source.isIdentifierStart(source.peek())) return 0;
        int len = 1;
        while (!source.isEOF(len) && source.isIdentifierPart(source.peek(len))) len++;
        return len;
    }

    private int matchBareLiteral() {
        if (!source.isAlpha(source.peek())) return 0;
        int len = 1;
        while (!source.isEOF(len)) {
            char c = source.peek(len);
            if (!(Character.isLetterOrDigit(c) || ":._".indexOf(c) != -1)) break;
            len++;
        }
        return len;
    }

    private void raise(ErrorCode code) {
        throw new ParseException(code, source.line(), source.column());
    }
}