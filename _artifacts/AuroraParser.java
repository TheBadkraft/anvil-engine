// src/main/java/dev/badkraft/engine/parser/AmlParser.java
package aurora.engine.parser;

import aurora.engine.parser.aml.Model;
import aurora.engine.parser.aml.NamedModel;
import aurora.engine.utilities.Utils;

import java.nio.file.*;
import java.io.IOException;
import java.util.*;

public class AmlParser {
    private final String source;
    private final List<ParseError> errors = new ArrayList<>();
    private int pos = 0;
    private int line = 1;
    private int col = 1;

    private AmlParser(String source) {
        this.source = source;
    }

    public static ParseResult<AuroraDocument> parse(Path path) {
        try {
            String content = Files.readString(path);
            Dialect dialect = Dialect.fromFileExtension(
                    Utils.getFileExtension(path));
            AmlParser parser = new AmlParser(content);
            /*
                here's the problem - before we can start getting models, we need to validate the document:
                - shebang (optional) but must be first line if present
                - if no shebang, we can still proceed, but file extension (.aml or .asl)
                  can be a hint, otherwise we assume #!asl as least restrictive
                - then we start parsing top-level statement (e.g., assignments, model definitions, etc.)
             */
            var doc = parser.parseDocument(dialect);
            return parser.errors.isEmpty()
                    ? ParseResult.success(doc)
                    : ParseResult.failure(parser.errors);
        } catch (IOException e) {
            return ParseResult.failure(List.of(
                    new ParseError(0, 0, "IO error: " + e.getMessage())));
        }
    }

    /*
        Now we have a dialect hint passed from the file extension. So if the shebang is missing,
        we can assume the dialect from the file extension. If the shebang is present, it overrides
        the file extension hint.

        What if the shebang doesn't match the file extension? We can either:
        - throw an error
        - warn and proceed with shebang dialect

        Another option is to ignore the shebang and use the file extension dialect ... except, since
        we are calling the file extension a hint, we should probably let the shebang take precedence.
     */
    private AuroraDocument parseDocument(Dialect hint) {
        // since we allow whitespace and comments prior to shebang, let's skip whitespace first
        skipWhitespace();

//        List<NamedModel> models = new ArrayList<>();
//        skipWhitespace();
//        if (matchShebang())
//            skipLine();
//        while (!isAtEnd()) {
//            models.add(parseNamedObject());
//            skipWhitespace();
//        }
        var doc = new AuroraDocument();
        doc.isParsed = true;

        return doc;
    }

    private NamedModel parseNamedObject() {
        String id = consumeId();
        Map<String, Model.Attribute> attrs = parseAttributes();
        Map<String, Object> fields = new LinkedHashMap<>();
        List<NamedModel> children = new ArrayList<>();
        consume('{');
        skipWhitespace();
        while (peek() != '}') {
            if (isAlpha(peek())) {
                int startPos = pos;
                String potentialId = consumeId();
                skipWhitespace();
                if (peek() == ':') {
                    // field
                    expectSeq(":=");
                    Object value = parseValue();
                    fields.put(potentialId, value);
                } else {
                    // child, rewind
                    pos = startPos;
                    children.add(parseNamedObject());
                }
            } else {
                error("Expected identifier");
            }
            skipWhitespace();
        }
        consume('}');
        String fullId = "main:" + id;
        return new NamedModel(fullId, attrs, Map.copyOf(fields), List.copyOf(children));
    }

    private Map<String, Model.Attribute> parseAttributes() {
        Map<String, Model.Attribute> attrs = new LinkedHashMap<>();
        if (match('@') && match('[')) {
            while (peek() != ']') {
                String key = consumeId();
                String value = "unknown";
                if (match('='))
                    value = consumeId();
                attrs.put(key, new Model.Attribute(key, value));
                if (match(','))
                    continue;
            }
            consume(']');
        } else {
            attrs.put("type", new Model.Attribute("type", "unknown"));
        }
        return attrs;
    }

    private Object parseValue() {
        skipWhitespace();
        char c = peek();
        if (c == '"' || c == '\'')
            return consumeString(c);
        if (c == '#')
            return consumeHex();
        if (isDigit(c) || c == '-')
            return consumeNumber();
        if (match("true"))
            return true;
        if (match("false"))
            return false;
        if (match("null"))
            return null;
        if (match('['))
            return parseArray();
        if (match('{'))
            return parseInlineObject();
        if (isDigit(peek()) && match("..")) {
            int min = (Integer) consumeNumber();
            int max = (Integer) consumeNumber();
            return new int[] { min, max };
        }
        if (match('`'))
            return consumeEmbed();
        error("Invalid value");
        return null;
    }

    private List<Object> parseArray() {
        List<Object> list = new ArrayList<>();
        consume('[');
        skipWhitespace();
        if (peek() == ']') {
            consume(']');
            return list;
        }
        do {
            list.add(parseValue());
            skipWhitespace();
        } while (match(','));
        consume(']');
        return list;
    }

    private Map<String, Object> parseInlineObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        consume('{');
        skipWhitespace();
        while (peek() != '}') {
            String key = consumeId();
            expectSeq(":=");
            map.put(key, parseValue());
            match(',');
            skipWhitespace();
        }
        consume('}');
        return map;
    }

    // --- primitives ---
    private String consumeId() {
        StringBuilder sb = new StringBuilder();
        while (isAlphaNumeric(peek()))
            sb.append(advance());
        if (sb.isEmpty()) {
            error("Expected identifier");
            return "";
        }
        return sb.toString();
    }

    private String consumeString(char quote) {
        StringBuilder sb = new StringBuilder();
        advance();
        while (peek() != quote && !isEOF()) {
            if (peek() == '\n') {
                line++;
                col = 1;
            }
            sb.append(advance());
        }
        if (isEOF()) {
            error("Unterminated string");
            return "";
        }
        advance();
        return sb.toString();
    }

    private String consumeHex() {
        advance();
        StringBuilder sb = new StringBuilder("#");
        for (int i = 0; i < 6; i++) {
            char c = advance();
            if (!isHexDigit(c)) {
                error("Invalid hex");
                break;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private Number consumeNumber() {
        boolean negative = match('-');
        StringBuilder sb = new StringBuilder();
        if (negative)
            sb.append('-');
        while (isDigit(peek()))
            sb.append(advance());
        if (match('.') && isDigit(peek())) {
            sb.append('.');
            while (isDigit(peek()))
                sb.append(advance());
            return Double.parseDouble(sb.toString());
        }
        return Integer.parseInt(sb.toString());
    }

    private String consumeEmbed() {
        advance();
        StringBuilder sb = new StringBuilder();
        while (peek() != '`' && !isEOF())
            sb.append(advance());
        if (isEOF()) {
            error("Unterminated embed");
            return "";
        }
        advance();
        return sb.toString();
    }

    // --- helpers ---
    private boolean isEOF() {
        return pos >= source.length();
    }

    private char peek() {
        return isEOF() ? '\0' : source.charAt(pos);
    }

    private char peekNext() {
        return pos + 1 >= source.length() ? '\0' : source.charAt(pos + 1);
    }

    // prefer an advance n and return current char - default n=1
    private char advance() {
        col++;
        return source.charAt(pos++);
    }

    private boolean match(char expected) {
        if (isEOF() || source.charAt(pos) != expected)
            return false;
        pos++;
        col++;
        return true;
    }

    private boolean match(String s) {
        if (source.regionMatches(pos, s, 0, s.length())) {
            pos += s.length();
            col += s.length();
            return true;
        }
        return false;
    }

    private void consume(char expected) {
        if (!match(expected))
            error("Expected '" + expected + "'");
    }

    private void expectSeq(String seq) {
        if (!match(seq))
            error("Expected '" + seq + "'");
    }

    private void skipWhitespace() {
        while (true) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r')
                advance();
            else if (c == '\n') {
                line++;
                col = 1;
                advance();
            } else if (c == '/' && peekNext() == '/')
                skipLineComment();
            else if (c == '/' && peekNext() == '*')
                skipBlockComment();
            else if (c == '#') {
                if (matchShebang()) {
                    skipLine();
                    break;
                } else
                    skipLineComment();
            } else
                break;
        }
    }

    private void skipLineComment() {
        while (peek() != '\n' && !isEOF())
            advance();
    }

    private void skipBlockComment() {
        advance();
        advance();
        int nest = 1;
        while (nest > 0 && !isEOF()) {
            if (peek() == '*' && peekNext() == '/') {
                advance();
                advance();
                nest--;
            } else if (peek() == '/' && peekNext() == '*') {
                advance();
                advance();
                nest++;
            } else
                advance();
        }
    }

    private void skipLine() {
        while (peek() != '\n' && !isEOF())
            advance();
        if (!isEOF()) {
            line++;
            col = 1;
            advance();
        }
    }

    private boolean matchShebang() {
        int save = pos;
        int saveLine = line, saveCol = col;
        skipWhitespace();
        boolean matched = source.startsWith("#!aurora aml", pos);
        if (matched) {
            pos += "#!aml".length();
            col += "#!aml".length();
        } else {
            pos = save;
            line = saveLine;
            col = saveCol;
        }
        return matched;
    }

    private void error(String msg) {
        errors.add(new ParseError(line, col, msg));
        recover();
    }

    private void recover() {
        while (!isEOF() && peek() != '\n' && peek() != ';')
            advance();
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private static boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private static boolean isHexDigit(char c) {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
}
