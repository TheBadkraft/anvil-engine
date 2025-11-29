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

    private boolean seenModuleAttributes = false;

    private AnvilParser(Context context) {
        this.context = context;
        this.source = context.source();
    }

    public static void parse(Context context) {
        new AnvilParser(context).parseSource();
    }

    private void parseSource() {
        source.skipWhitespace();

        // Module-level attributes @[ ... ] — zero or more
        while (source.is("@[")) {
            seenModuleAttributes = true;
            List<Attribute> attrs = parseAttributeBlock();
            context.addAllAttributes(attrs);
            source.skipWhitespace();
        }

        // Top-level statements
        while (!source.isEOF()) {
            Statement stmt = parseStatement();
            context.addStatement(stmt);
            source.skipWhitespace();
        }

        context.markParsed();
    }

    private Statement parseStatement() {
        String parent = null;

        // Identifier (optionally followed by : Parent)
        String key = readIdentifier();
        if (key.isEmpty()) raise(EXPECTED_IDENTIFIER);

        source.skipWhitespace();

        // Optional inheritance: key : Parent
        if (source.is(":")) {
            source.consume(1);
            source.skipWhitespace();
            parent = readIdentifier();
            if (parent.isEmpty()) raise(EXPECTED_IDENTIFIER);
            source.skipWhitespace();
        }

        List<Attribute> attrs = parseAttributeBlock();
        source.skipWhitespace();

        if (!source.isOperator(ASSIGN)) raise(EXPECTED_ASSIGN);
        source.consumeOperator(ASSIGN);
        source.skipWhitespace();

        Value value = parseValue();
        if (!attrs.isEmpty()) {
            value.getAttributes().addAll(attrs);
        }

        // Optional terminator
        if (source.isOperator(COMMA)) {
            source.consumeOperator(COMMA);
        }

        Assignment assignment = new Assignment(key, attrs, value, parent);
        context.addIdentifier(key);
        return assignment;
    }

    private Value parseValue() {
        if (source.isOperator(L_BRACE))  return parseObject();
        if (source.isOperator(L_BRACKET)) return parseArray();
        if (source.isOperator(L_PAREN))   return parseTuple();
        if (source.is("\""))              return parseString();
        if (source.is("#") || source.is("0x") || source.is("0X")) return parseHex();
        if (source.is("true"))            { source.consume(4); return context.booleanVal(true); }
        if (source.is("false"))           { source.consume(5); return context.booleanVal(false); }
        if (source.is("null"))            { source.consume(4); return context.nullVal(); }

        if (source.isOperator(AT) || source.isOperator(BACKTICK)) {
            return parseBlob();
        }

        String bare = readBareLiteral();
        if (bare != null) {
            return context.bare(bare);
        }

        return parseNumber();
    }

    private Value parseObject() {
        int start = source.position();
        source.consumeOperator(L_BRACE);
        source.skipWhitespace();

        if (source.isOperator(R_BRACE)) {
            raise(EMPTY_OBJECT_NOT_ALLOWED);
        }

        List<Map.Entry<String, Value>> fields = new ArrayList<>();
        List<Attribute> attrs = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        while (!source.isOperator(R_BRACE)) {
            String key = readIdentifier();
            if (key.isEmpty()) raise(EXPECTED_IDENTIFIER);
            if (!seen.add(key)) raise(DUPLICATE_FIELD_IN_OBJECT);

            source.skipWhitespace();
            attrs = parseAttributeBlock();
            source.skipWhitespace();

            if (!source.isOperator(ASSIGN)) raise(EXPECTED_ASSIGN);
            source.consumeOperator(ASSIGN);
            source.skipWhitespace();

            Value value = parseValue();
            if (!attrs.isEmpty()) value.getAttributes().addAll(attrs);
            fields.add(Map.entry(key, value));

            source.skipWhitespace();
            if (source.isOperator(COMMA)) {
                source.consumeOperator(COMMA);
                source.skipWhitespace();
            }
        }

        source.consumeOperator(R_BRACE);
        String full = source.substring(start, source.position());
        return context.object(fields, attrs);
    }

    private Value parseArray() {
        int start = source.position();
        source.consumeOperator(L_BRACKET);
        source.skipWhitespace();

        List<Value> elements = new ArrayList<>();

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
        String full = source.substring(start, source.position());
        return context.array(elements);
    }

    private Value parseTuple() {
        int start = source.position();
        source.consumeOperator(Operator.L_PAREN); // (

        List<Value> elements = new ArrayList<>();
        source.skipWhitespace();
        // Disallow empty tuple ()
        if (source.isOperator(Operator.R_PAREN)) {
            raise(ErrorCode.EMPTY_TUPLE_ELEMENT);
        }

        while (true) {
            Value element = parseValue();
            elements.add(element);
            source.skipWhitespace();

            if (source.isOperator(Operator.R_PAREN)) {
                break;
            }

            // Comma required between elements
            if (!source.isOperator(Operator.COMMA)) {
                raise(ErrorCode.EXPECTED_COMMA_IN_TUPLE);
            }
            source.consumeOperator(Operator.COMMA);
            source.skipWhitespace();
        }

        // Must have at least 2 elements
        if (elements.size() < 2) {
            raise(ErrorCode.TUPLE_TOO_SHORT);
        }

        source.consumeOperator(Operator.R_PAREN); // )

        String fullSource = source.substring(start, source.position());
        return context.tuple(elements);
    }

    private Value parseString() {
        source.consumeOperator(QUOTE);
        int start = source.position();
        while (!source.isEOF() && !(source.isOperator(QUOTE) && !source.isEscaped(source.position() - 1))) {
            source.consume();
        }
        String content = source.substring(start, source.position());
        source.consumeOperator(QUOTE);
        return context.string(content);
    }

    private Value parseBlob() {
        String attribute = null;
        if (source.isOperator(AT)) {
            source.consumeOperator(AT);
            attribute = readIdentifier();
        }
        int start = source.position();
        source.consumeOperator(BACKTICK);
        while (!source.isEOF() && !(source.isOperator(BACKTICK) && !source.isEscaped(source.position() - 1))) {
            source.consume();
        }
        String full = source.substring(start, source.position() + 1); // include closing `
        source.consumeOperator(BACKTICK);
        return context.blob(full, attribute);
    }

    private Value parseHex() {
        boolean isHash = source.is("#");
        StringBuilder buffer = source.buffer();

        String prefix = isHash ? source.take(1) : source.take(2);
        buffer.setLength(0);
        while (source.isHexDigit(source.peek()) || source.peek() == '_') {
            if (source.isHexDigit(source.peek())) buffer.append(source.consume());
            else source.consume();
        }
        String digits = buffer.toString();
        long value = Long.parseLong(digits, 16);
        String full = prefix + digits;
        return isHash
                ? context.hexValue(value)
                : context.longValue(value);
    }

    private Value parseNumber() {
        int start = source.position();
        StringBuilder buffer = source.buffer();

        if (source.peek() == '+' || source.peek() == '-') buffer.append(source.consume());

        boolean hasDigit = false;
        while (source.isDigit(source.peek()) || source.peek() == '_') {
            if (source.isDigit(source.peek())) { buffer.append(source.consume()); hasDigit = true; }
            else source.consume();
        }

        boolean isFloat = false;
        if (source.peek() == '.') {
            char dot = source.consume();
            buffer.append(dot);
            isFloat = true;
            while (source.isDigit(source.peek()) || source.peek() == '_') {
                if (source.isDigit(source.peek())) buffer.append(source.consume());
                else source.consume();
            }
        }

        if (source.peek() == 'e' || source.peek() == 'E') {
            buffer.append(source.consume());
            isFloat = true;
            if (source.peek() == '+' || source.peek() == '-') buffer.append(source.consume());
            while (source.isDigit(source.peek()) || source.peek() == '_') {
                if (source.isDigit(source.peek())) buffer.append(source.consume());
                else source.consume();
            }
        }

        if (!hasDigit) raise(INVALID_NUMBER);

        String full = source.substring(start, source.position());
        String clean = buffer.toString().replace("_", "");

        if (isFloat) {
            double d = Double.parseDouble(clean);
            return context.doubleValue(d);
        } else {
            long l = Long.parseLong(clean);
            return context.longValue(l);
        }
    }

    private List<Attribute> parseAttributeBlock() {
        if (!source.is("@[")) return List.of();
        source.consume(2); // @[
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

        source.consume(1); // ]
        return List.copyOf(attrs);
    }

    private Value parseLiteralValue() {
        int savePos = source.position();
        Value v = parseValue();
        if (v instanceof ObjectValue || v instanceof ArrayValue || v instanceof TupleValue || v instanceof BlobValue) {
            source.setPosition(savePos, source.line(), source.column());
            raise(INVALID_VALUE_IN_ATTRIBUTE);
        }
        return v;
    }

    private String readIdentifier() {
        int start = source.position();
        while (!source.isEOF() && (source.isAlphaNumeric(source.peek()) || source.peek() == '.' || source.peek() == '_')) {
            source.consume();
        }
        String id = source.substring(start, source.position());
        if (id.isEmpty() || id.startsWith(".") || id.endsWith(".") || id.contains("..")) {
            return "";
        }
        return id;
    }

    private String readBareLiteral() {
        int start = source.position();
        if (!source.isAlpha(source.peek())) return null;
        while (!source.isEOF() && (source.isAlphaNumeric(source.peek()) || ":._".indexOf(source.peek()) != -1)) {
            source.consume();
        }
        return source.substring(start, source.position());
    }

    private void raise(ErrorCode code) {
        throw new ParseException(code, source.line(), source.column());
    }
}