// src/main/java/dev/badkraft/aurora/engine/parser/AuroraParser.java
package dev.badkraft.anvil.parser;

import dev.badkraft.anvil.*;
import dev.badkraft.anvil.Module;
import dev.badkraft.anvil.utilities.Utils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.*;
import java.io.IOException;
import java.util.*;

import static dev.badkraft.anvil.ErrorCode.*;
import static dev.badkraft.anvil.Operator.QUOTE;

public class AnvilParser {
    static final int MAX_SB_SIZE = 256;
    private static final Set<String> KEYWORDS = Set.of(
            "true", "false", "null", "vars", "include"
    );

    private final String source;
    private final char[] attribMarker = {'@', '['};
    private final StringBuilder sb = new StringBuilder(MAX_SB_SIZE);
    private final List<ParseError> errors = new ArrayList<>();
    private Dialect moduleDialect = Dialect.NONE;
    private boolean seenModuleAttribs = false;
    private int pos = 0;
    private int line = 1;
    private int col = 1;

    /**
     * Construct a new AuroraParser
     */
    private AnvilParser(Dialect hintDialect, String namespace, String source) {
        this.source = source;
        this.moduleDialect = parseDialect(hintDialect);
    }

    /**
     * Parser entry
     */
    public static ParseResult<Module> parse(Path path) {
        try {
            String content = Files.readString(path);
            Dialect hintDialect = Dialect.fromFileExtension(Utils.getFileExtension(path));
            String name = path.getFileName().toString().replaceFirst("[.][^.]+$", "");
            AnvilParser parser = new AnvilParser(hintDialect, name, content);
            Module module = new Module(name, parser.moduleDialect);
            parser.parseSource(module);

            return parser.errors.isEmpty()
                    ? ParseResult.success(module)
                    : ParseResult.failure(parser.errors);
        } catch (IOException e) {
            return ParseResult.failure(List.of(new ParseError(0, 0, ErrorCode.IO_ERROR)));
        }
    }

    // --- Main parser functions ---
    /** Parses dialect from shebang */
    private Dialect parseDialect(Dialect hint) {
        skipWhitespace();
        if (isShebang()) {
            String token = new String(consume(5));
            return Dialect.fromShebang(token);
        }
        return hint;
    }
    /** Module is our container for each file */
    private void parseSource(Module module) {
        //  1. skip whitespace
        skipWhitespace();
        List<Attribute> moduleAttribs = parseAttributeBlock();
        if (!moduleAttribs.isEmpty()) {
            seenModuleAttribs = true;
            module.addAllAttributes(moduleAttribs);
            skipWhitespace();
        }
        while(!isEOF()) {
            //  2. parseStatement
            List<String> identifiers = new ArrayList<>();
            Statement statement = parseStatement(identifiers);
            module.addStatement(statement);
            module.addAllIdentifiers(identifiers);
        }
        module.markParsed();
    }
    private Statement parseStatement(List<String> identifiers) {
        //  do not expect module attribs here
        if (!seenModuleAttribs && readChars(attribMarker) != 0) {
            raise(ErrorCode.UNEXPECTED_MODULE_ATTRIBUTES, line, col);
        }
        //  1. Expect an identifier
        int idLen = readIdentifier();
        if (idLen == 0) {
            raise(ErrorCode.EXPECTED_IDENTIFIER, line, col);
        }
        String id = take(idLen);
        skipWhitespace();
        //  2. Look for attributes
        List<Attribute> attributes = parseAttributeBlock();
        skipWhitespace();
        //  3. Expect Operator.ASSIGN
        int opLen = readOperator(Operator.ASSIGN);
        if (opLen == 0) {
            raise(ErrorCode.EXPECTED_ASSIGN, line, col);
        }
        consume(opLen);
        skipWhitespace();
        //  4. Expect value
        Value value = parseValue();
        skipWhitespace();
        //  5. Expect Operator.COMMA, NL, or EOF
        int termLen = readOperator(Operator.COMMA);
        if (termLen > 0) {
            consume(termLen);
            skipWhitespace();
        }
        //  6. Construct and return Assignment
        identifiers.add(id);
        return new Assignment(id, value, attributes);
    }
    private Value parseValue() {
        // 1. OBJECT
        if (isOperator(Operator.L_BRACE)) {
            return parseObject();
        }
        //  2. ARRAY
        if (isOperator(Operator.L_BRACKET)) {
            return parseArray();
        }
        //  3. TUPLE
        if (isOperator(Operator.L_PAREN)) {
            return parseTuple();
        }
        //  4. STRING
        if (is("\"")) {
            return parseString();
        }
        //  5. HEX VALUE
        if (is("#") || is("0x") || is("0X")) {
            // let's flag which kind and send to parser function
            return parseHexLiteral(is("#"));
        }
        //  6. Keywords — boolean and null
        if (is("true")) {
            consume(4);
            return new Value.BooleanValue(true);
        }
        if (is("false")) {
            consume(5);
            return new Value.BooleanValue(false);
        }
        if (is("null")) {
            consume(4);
            return new Value.NullValue();
        }
        //  7. BLOB - attribut is optional
        boolean hasAttrib = false;
        if ((hasAttrib = isOperator(Operator.AT)) || isOperator(Operator.BACKTICK)) {
            return parseBlob(hasAttrib);
        }
        //  8. BARE LITERAL (unquoted string)
        String bare = readBareLiteral();
        if (bare != null) {
            return new Value.BareLiteral(bare);
        }
        //  9. DECIMAL NUMBER - last chance fallback
        return parseDecimalNumber();
    }
    private Value parseObject() {
        int start = pos;
        consumeOperator(Operator.L_BRACE);

        List<Map.Entry<String, Value>> fields = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        skipWhitespace();

        if (isOperator(Operator.R_BRACE)) {
            raise(ErrorCode.EMPTY_OBJECT_NOT_ALLOWED, line, col);
        }

        while (!isOperator(Operator.R_BRACE)) {
            int keyLen = readIdentifier();
            if (keyLen == 0) raise(ErrorCode.EXPECTED_IDENTIFIER, line, col);
            String key = take(keyLen);

            if (!seen.add(key)) {
                raise(ErrorCode.DUPLICATE_FIELD_IN_OBJECT, line, col);
            }

            skipWhitespace();
            if (!isOperator(Operator.ASSIGN)) raise(ErrorCode.EXPECTED_ASSIGN, line, col);
            consumeOperator(Operator.ASSIGN);
            skipWhitespace();

            Value value = parseValue();
            fields.add(Map.entry(key, value));

            skipWhitespace();
            if (isOperator(Operator.COMMA)) {
                consumeOperator(Operator.COMMA);
                skipWhitespace();
            }
        }

        consumeOperator(Operator.R_BRACE);

        String fullSource = source.substring(start, pos);
        return new Value.ObjectValue(fields, fullSource);
    }
    private Value parseArray() {
        int start = pos;
        consumeOperator(Operator.L_BRACKET); // [

        List<Value> elements = new ArrayList<>();

        skipWhitespace();

        // Disallow empty array []
        if (isOperator(Operator.R_BRACKET)) {
            raise(ErrorCode.EMPTY_ARRAY_NOT_ALLOWED, line, col);
        }

        while (!isOperator(Operator.R_BRACKET)) {
            Value element = parseValue();
            elements.add(element);

            skipWhitespace();

            if (!isOperator(Operator.R_BRACKET)) {
                if (!isOperator(Operator.COMMA)) {
                    raise(ErrorCode.MISSING_COMMA_IN_ARRAY, line, col);
                }
                consumeOperator(Operator.COMMA);
                skipWhitespace();
            }
        }

        consumeOperator(Operator.R_BRACKET);

        String fullSource = source.substring(start, pos);
        return new Value.ArrayValue(elements, fullSource);
    }
    private Value parseTuple() {
        int start = pos;
        consumeOperator(Operator.L_PAREN); // (

        List<Value> elements = new ArrayList<>();

        skipWhitespace();

        // Disallow empty tuple ()
        if (isOperator(Operator.R_PAREN)) {
            raise(ErrorCode.EMPTY_TUPLE_ELEMENT, line, col);
        }

        while (true) {
            Value element = parseValue();
            elements.add(element);
            skipWhitespace();

            if (isOperator(Operator.R_PAREN)) {
                break;
            }

            // Comma required between elements
            if (!isOperator(Operator.COMMA)) {
                raise(ErrorCode.EXPECTED_COMMA_IN_TUPLE, line, col);
            }
            consumeOperator(Operator.COMMA);
            skipWhitespace();
        }

        // Must have at least 2 elements
        if (elements.size() < 2) {
            raise(ErrorCode.TUPLE_TOO_SHORT, line, col);
        }

        consumeOperator(Operator.R_PAREN); // )

        String fullSource = source.substring(start, pos);
        return new Value.TupleValue(elements, fullSource);
    }
    private List<Attribute> parseAttributeBlock() {
        final char[] attribMarker = {'@', '['};
        int isAttrib = readChars(attribMarker);
        switch (isAttrib) {
            case 0:
                return List.of(); // no attribute block
            case 1:
                raise(ErrorCode.INVALID_ATTRIBUTE_BLOCK, line, col);
                break;
            case 2:
                // continue parsing
                break;
            default:
                throw new AssertionError();
        }
        int startLine = line, startCol = col;
        consume(isAttrib); // @[

        List<Attribute> attrs = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        skipWhitespace();

        while (!is("]")) {
            // Key — identifier only
            int keyLen = readIdentifier();
            if (keyLen == 0) raise(ErrorCode.INVALID_ATTRIBUTE, line, col);
            String key = take(keyLen);

            if (!seen.add(key)) {
                raise(ErrorCode.DUPLICATE_ATTRIBUTE_KEY, line, col);
            }

            skipWhitespace();

            Value value = null;
            if (isOperator(Operator.EQUAL)) {
                consumeOperator(Operator.EQUAL);
                skipWhitespace();
                value = parseLiteralValue();  // only literals allowed
                skipWhitespace();
            }

            attrs.add(new Attribute(key, value));

            // Comma required if not last
            if (!is("]")) {
                if (!isOperator(Operator.COMMA)) {
                    raise(ErrorCode.MISSING_COMMA_IN_ATTRIBUTES, line, col);
                }
                consumeOperator(Operator.COMMA);
                skipWhitespace();
            }
        }
        if (attrs.isEmpty()){
            raise(ErrorCode.EMPTY_ATTRIBUTE_BLOCK, line, col);
        }

        consume(1); // ]
        return List.copyOf(attrs);
    }
    private Value parseLiteralValue() {
        int saveLine = line, saveCol = col;
        Value v = parseValue();

        if (v instanceof Value.ObjectValue ||
                v instanceof Value.ArrayValue ||
                v instanceof Value.TupleValue ||
                v instanceof Value.BlobValue) {
            line = saveLine; col = saveCol;
            raise(ErrorCode.INVALID_VALUE_IN_ATTRIBUTE, line, col);
        }

        return v;
    }
    private Value parseString() {
        String fullSource = parseContent(Operator.QUOTE);
        return new Value.StringValue(fullSource);   // stores the whole "…"
    }
    private Value parseHexLiteral(boolean isHash) {
        int startLine = line;
        int startCol = col;

        // sanity check ... ???
        boolean is0x   = is("0x") || is("0X");
        if (!isHash && !is0x) throw new AssertionError();

        String prefix = isHash ? take(1) : take(2);  // consume "0x" or "0X"

        sb.setLength(0);
        boolean hasDigit = false;

        while (true) {
            char c = peek();
            if (isHexDigit(c)) {
                // Don't format the data, just collect it
                sb.append(consume());
                hasDigit = true;
            } else if (c == '_') {
                consume();
            } else {
                break;
            }
        }

        if (!hasDigit) {
            raise(ErrorCode.INVALID_HEX_LITERAL, startLine, startCol);
        }

        String digits = sb.toString().replace("_", "");
        String fullSource = isHash ? "#" + digits : prefix + digits;

        long value = 0;
        try {
            value = Long.parseLong(digits, 16);
        } catch (NumberFormatException e) {
            raise(ErrorCode.INVALID_HEX_LITERAL, startLine, startCol);
        }

        return isHash ?
                new Value.HexValue(value, fullSource) :
                new Value.LongValue(value, fullSource);
    }
    private Value parseDecimalNumber() {
        int startLine = line;
        int startCol = col;

        sb.setLength(0);
        boolean hasDigit = false;

        // Optional sign
        if (peek() == '+' || peek() == '-') {
            sb.append(consume());
        }

        // Integer part
        while (true) {
            char c = peek();
            if (c >= '0' && c <= '9') {
                sb.append(consume());
                hasDigit = true;
            } else if (c == '_') {
                consume();
            } else {
                break;
            }
        }

        boolean isFloat = false;

        // Fractional part
        if (peek() == '.') {
            sb.append(consume());
            isFloat = true;
            while (true) {
                char c = peek();
                if (c >= '0' && c <= '9') {
                    sb.append(consume());
                    hasDigit = true;
                } else if (c == '_') {
                    consume();
                } else {
                    break;
                }
            }
        }

        // Exponent
        if (peek() == 'e' || peek() == 'E') {
            sb.append(consume());
            isFloat = true;
            if (peek() == '+' || peek() == '-') {
                sb.append(consume());
            }
            boolean hasExpDigit = false;
            while (true) {
                char c = peek();
                if (c >= '0' && c <= '9') {
                    sb.append(consume());
                    hasExpDigit = true;
                } else if (c == '_') {
                    consume();
                } else {
                    break;
                }
            }
            if (!hasExpDigit) {
                raise(ErrorCode.INVALID_EXPONENT, line, col);
            }
        }

        if (!hasDigit) {
            raise(ErrorCode.INVALID_NUMBER, startLine, startCol);
        }

        String source = sb.toString();
        String clean = source.replace("_", "");

        try {
            if (isFloat) {
                double d = Double.parseDouble(clean);
                return new Value.DoubleValue(d, source);
            } else {
                long l = Long.parseLong(clean);
                return new Value.LongValue(l, source);
            }
        } catch (NumberFormatException e) {
            raise(ErrorCode.INVALID_NUMBER, startLine, startCol);
        }
        return null; // unreachable
    }
    private Value parseBlob(boolean hasAttribute) {
        String attribute = null;
        int attrStart = -1;

        if (hasAttribute) {
            attrStart = pos;
            consume(1);                         // @
            int len = readIdentifier();
            if (len > 0) {
                attribute = take(len);
                if (KEYWORDS.contains(attribute))
                    raise(ATTRIBUTE_IS_KEYWORD, line, col);
            }
        }

        String blobSource = parseContent(Operator.BACKTICK);   // includes the backticks

        // If there was an @attribute, prepend it to the source text
        if (attribute != null) {
            String attrPart = source.substring(attrStart, pos - blobSource.length());
            blobSource = attrPart + blobSource;
        }

        return new Value.BlobValue(blobSource, attribute);
    }
    private String parseContent(Operator delimiter) {
        if (delimiter != QUOTE && delimiter != Operator.BACKTICK) {
            raise(ErrorCode.UNEXPECTED_TOKEN, line, col);
        }

        int start = pos;                     // <-- remember where we started
        consumeOperator(delimiter);          // consume opening " or `

        while (!isEOF()) {
            if (isOperator(delimiter) && !isEscaped(pos)) {
                break;                       // found unescaped closing delimiter
            }
            consume();                       // normal char or escaped sequence
        }

        if (!isOperator(delimiter)) {
            raise(delimiter == QUOTE ? UNTERMINATED_STRING : UNTERMINATED_BLOB, line, col);
        }
        consumeOperator(delimiter);          // consume closing " or `

        // Return the exact text we just parsed (including delimiters)
        return source.substring(start, pos);
    }

    // --- Utility interrogators ---
    /*
        Interrogators - peek, is*, read* -  are passive and do not
        consume (advance) the position in the stream.
     */
    private boolean isEOF() { return isEOF(0); }
    private boolean isEOF(int offset) {return pos + offset >= source.length();}
    private char peek() { return peek(0); }
    private char peek(int offset) {
        int index = pos + offset;
        return index < source.length() ? source.charAt(index) : '\0';
    }
    private boolean is(String s) {
        return is(s, 0);
    }
    private boolean is(String s, int offset) {
        for (int i = 0; i < s.length(); i++) {
            if(peek(i + offset) != s.charAt(i)) return false;
        }
        return true;
    }
    private boolean is(char c, int offset) {
        return peek(offset) == c;
    }
    private boolean isShebang() {
        return is("#!asl") || is("#!aml");
    }
    private boolean isAlpha(char c) {
        return Character.isLetter(c) || c == '_';
    }
    private boolean isAlphaNumeric(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
    private static boolean isDigit(char c) { return c >= '0' && c <= '9'; }
    private static boolean isHexDigit(char c) {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
    private boolean isOperator(Operator op) { return is(op.symbol()); }
    private boolean isKeyword(String s) { return KEYWORDS.contains(s); }
    private boolean isEscaped(int pos) {
        if (pos <=0) return false;

        int backslashes = 0;
        for (int i = pos - 1; i >= 0 && source.charAt(i) == '\\'; i--) backslashes++;
        return backslashes % 2 == 1;
    }
    private int readIdentifier() {
        int len = 0;
        if (!isEOF() && isAlpha(peek())) {
            while (!isEOF() && isAlphaNumeric(peek(len))) len++;
        }
        return len;
    }
    private String readBareLiteral() {
        int length = 0;
        char first = peek(length);
        if (!Character.isLetter(first) && first != '_') return null;
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

        if (length == 0) return null;
        return take(length); // consumes + returns
    }
    private int readToEOL() {
        int len = 0;
        while (!isEOF() && peek() != '\n') len++;
        // we want to include EOL (`\n`)
        len++;
        return len;
    }
    private int readWhitespace() {
        int len = 0;
        while (!isEOF(len)) {
            char c = peek(len);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') len++;
            else if (is("//", len)) len += readLineComment(len);
            else if (is("/*", len)) len += readBlockComment(len);
            else break;
        }
        return len;
    }
    private int readLineComment() { return readLineComment(0); }
    private int readLineComment(int offset) {
        int len = offset;
        if (peek(len) == '/' && peek(len + 1) == '/') {
            len += 2;
            while(!isEOF(len) && peek(len) != '\n') len++;
        }
        return len - offset;
    }
    private int readBlockComment() { return readBlockComment(0); }
    private int readBlockComment(int offset) {
        int len = offset;
        int depth = 0;
        if (peek(len) == '/' && peek(len + 1) == '*') {
            len += 2;
            depth++;

            while (depth > 0 && !isEOF(len)) {
                if (peek(len) == '*' && peek(len + 1) == '/') {
                    len += 2;
                    depth--;
                }
                else if (peek(len) == '/' && peek(len + 1) == '*') {
                    len += 2;
                    depth++;
                }
                else len++;
            }
        }
        return len - offset;
    }
    private int readOperator(Operator op) {
        int len = 0;
        if (isOperator(op)) len = op.length();
        return len;
    }
    private int readChars(char[] chars) {
        if (chars.length == 0) return 0;
        /*
            read each char and expect in source ... returning int is the last element
            passing interrogation
         */
        int charLen = 0;
        for(int ndx = 0; ndx < chars.length; ndx++) {
            if (is(chars[ndx], ndx)) {
                charLen++;
                continue;
            }
            else {
                break;
            }
        }

        return charLen;
    }

    // --- Utility consumers ---
    /*
        Consumers - skip*, consume* - advance the position in the stream. All
        consumers not taking an INT parameter should 'read*' first to verify
        what is being consumed and receive a non-negative, non-zero (>0) result.
     */
    private char consume() {
        if (isEOF()) return '\0';
        char c = source.charAt(pos++);
        if (c == '\n') { line++; col = 1; } else col++;
        return c;
    }
    private char @NotNull [] consume(int n) {
        char[] buf = new char[n];
        for (int i = 0; i < n; i++) buf[i] = consume();
        return buf;
    }
    private void consumeOperator(Operator op) {
        consume(op.length());
    }
    private String take(int length) {
        if (length <= 0) return "";
        sb.setLength(0);
        sb.append(source, pos, pos + length);
        consume(length);
        return sb.toString();
    }
    // although not used we are keeping it for artifact completeness
    private @NotNull String matchText(Operator delimiter) {
        if (delimiter != QUOTE && delimiter != Operator.BACKTICK) {
            raise(ErrorCode.UNEXPECTED_TOKEN, line, col);
        }
        sb.setLength(0);
        while (!isEOF() && !isOperator(delimiter)) {
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
    private void skipWhitespace() {
        consume(readWhitespace());
    }
    private void skipLineComment() {
        consume(readLineComment());
    }
    private void skipBlockComment() {
        consume(readBlockComment());
    }

    // --- Utility error & recovery functions ---
    /** report the error */
    private void raise(ErrorCode code, int line, int col) {
        String msg = code.message();
        throw new ParseException(code, line, col, msg);
        // never recover here because we don't know what kind of recovery
    }
    /** generic recovery to start of next line */
    private void recover() {
        consume(readToEOL());
    }
}
