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
            // "return", "if", "else", "for", "while" // reserved for future use
    );
    private final String source;
    private final List<ParseError> errors = new ArrayList<>();
    private boolean hasShebang = false;
    private int pos = 0;
    private int line = 1;
    private int col = 1;

    private AuroraParser(String source) {
        this.source = source;
    }

    public static ParseResult<Module> parse(Path path) {
        try {
            String content = Files.readString(path);
            Dialect dialect = Dialect.fromFileExtension(
                    Utils.getFileExtension(path));
            AuroraParser parser = new AuroraParser(content);
            /*
                here's the problem - before we can start getting models, we need to validate the module:
                - shebang (optional) but must be first non-ws line if present
                - if no shebang, we can still proceed, but file extension (.aml or .asl)
                  can be a hint, otherwise we assume #!asl as least restrictive
                - then we start parsing top-level statement (e.g., assignments, model definitions, etc.)
             */
            var module = parser.parseSource(dialect);
            return parser.errors.isEmpty()
                    ? ParseResult.success(module)
                    : ParseResult.failure(parser.errors);
        } catch (IOException e) {
            return ParseResult.failure(List.of(
                    new ParseError(0, 0, ErrorCode.IO_ERROR)));
        }
    }

    /*
        Rule 1: Advance Only on Success + Minimal Responsibility
        1. **Specialized functions advance only what they match**
        2. **On success: advance matched text only**
        3. **On failure: restore position**
        4. **Never skip whitespace, comments, or line endings**
        5. **Caller handles layout and terminators**

        Why:
        - **Zero hidden navigation**
        - **Full control at caller level**
        - **Extensible parsing strategies**
     */
    private Module parseSource(Dialect hint) {
        /*
             Let 'parseDocument' handle line states, navigation, error reporting, etc.
             Specialized functions should not modify the parser state directly; they should
             only return results that 'parseDocument' can use to update the state accordingly.
         */
        var module = new Module();
        // 1. skip any leading whitespace/comments
        skipWhitespace();
        // 2. shebang; optional, first non-ws line
        module.dialect = detectDialect(hint);

        // 3. Parse top-level statements
        while (!isEOF()) {
            skipWhitespace();
            // check for shebang again (error if found)
            if(errShebang(module)) {
                // error reported inside errShebang
                recover();
                continue;
            }
            // parse identifier ... every top-level statement starts with an identifier
            int len;
            if ((len = isIdentifier()) > 0) {
                String id = consume(len);
                if (isKeyword(id)) {
                    error(ErrorCode.IDENTIFIER_IS_KEYWORD, line, col);
                    recover();
                    continue;
                }
                // for now, we just create a placeholder statement
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
                consume(Operator.ASSIGN.symbol().length());   // consume ":="

                skipWhitespace();
                var valueRes = parseValue();
                if (!valueRes.isSuccess()) {
                    errors.addAll(valueRes.errors());
                    recover();
                    continue;
                }

                // Build the final statement with attributes
                Statement stmt = new Assignment(id, valueRes.value(), attrs);
                module.addStatement(stmt);

                continue;
            }

            // if we reach here, it's an unexpected token
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

//    private Statement parseStatement(String id) {
//        // 1. try assignment operator
//        if(isOperator(Operator.ASSIGN)) {
//            if(!consume(Operator.ASSIGN.symbol().length()).isEmpty()) {
//                skipWhitespace();
//                // we have an assignment operator so we parse value ...
//                var value = parseValue();
//
//                if (value.isSuccess()) {
//                    return new Assignment(id, value.value());
//                } else {
//                    // error reported inside value result
//                    errors.addAll(value.errors());
//                    recover();
//                    return null;
//                }
//            }
//        }
//
//        // no assignment operator
//        error(ErrorCode.EXPECTED_ASSIGN, line, col);
//        return null;
//    }
    
    private ValueParseResult<List<Attribute>> parseAttributeBlock() {
        if (!isOperator(Operator.L_BRACKET)) return success(List.of());
        int startLine = line, startCol = col;
        consume(); // eat '['

        List<Attribute> attrs = new ArrayList<>();
        while (true) {
            skipWhitespace();
            if (isOperator(Operator.R_BRACKET)) { consume(); break; }

            // tag or key=value
            int idLen = isIdentifier();
            if (idLen == 0) return failure(ErrorCode.EXPECTED_IDENTIFIER, line, col);
            String key = consume(idLen);

            Value val = null;
            skipWhitespace();
            if (consumeOperator(Operator.EQUAL)) {
                skipWhitespace();
                var literal = parseLiteral();                 // literal only – no nesting
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
    }

    private @NotNull ValueParseResult<Value> parseValue() {
        // object type
        if (isOperator(Operator.L_BRACE)) {
            return parseAnonymousObject();
        }
        // array type
        if (isOperator(Operator.L_BRACKET)) {
            return parseArray();
        }
        // primitive types
        if (is("@")) {
            return parseFreeform();
        }
        if (is("null")) {
            consume(4);
            return success(new Value.NullValue());
        }
        if (is("true")) {
            consume(4);
            return success((new Value.BooleanValue(true)));
        }
        if (is("false")) {
            consume(5);
            return success((new Value.BooleanValue(false)));
        }
        // ---- anonymous identifier (e.g. block, vanilla) ----
        int idLen = isIdentifier();
        if (idLen > 0) {
            String id = consume(idLen);
            if (!isKeyword(id)) {
                return ValueParseResult.success(new Value.AnonymousValue(id));
            }
            // fall through to keyword handling (true/false/null)
        }
        // string & numeric types
        if (isOperator(Operator.QUOTE)) {
            consumeOperator(Operator.QUOTE);
            String value = matchString();
            if (isOperator(Operator.QUOTE)) {
                consumeOperator(Operator.QUOTE);
                return success(new Value.StringValue(value));
            } else {
                return failure(ErrorCode.UNTERMINATED_STRING, line, col);
            }
        } else {
            NumericParseResult numeric = parseNumber();
            if (numeric.isSuccess()) {
                return ValueParseResult.success(numeric.value());
            } else if (!numeric.errors().isEmpty()) {
                return ValueParseResult.failure(numeric.errors());
            }
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
    
    private NumericParseResult parseNumber() {
        int start = pos;
        int startLine = line, startCol = col;
        boolean isHex = false;

        // Optional sign
        if (peek(0) == '-') consume();

        // Hex: #ff00aa
        if (peek(0) == '#' && isHexDigit(peek(1))) {
            consume(); // #
            isHex = true;
            while (isHexDigit(peek(0))) consume();
        } else {
            // Decimal digits
            while (isDigit(peek(0))) consume();

            // Fractional part
            if (peek(0) == '.') {
                consume();
                while (isDigit(peek(0))) consume();
            }

            // Exponent
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

    private @NotNull ValueParseResult<Value> parseAnonymousObject() {
        Map<String, Value> fields = new HashMap<>();
        List<ParseError> errors = new ArrayList<>();
        int start = pos;
        boolean loop = !isOperator(Operator.R_BRACE);

        consumeOperator(Operator.L_BRACE);
        while (!isEOF() && loop) {
            skipWhitespace();

            // 1. Field (identifier)
            int idLen = isIdentifier();
            if (idLen == 0) {
                errors.add(new ParseError(line, col, ErrorCode.EXPECTED_OBJECT_FIELD));
                recoverToObjectEnd();
                break;
            }
            String field = consume(idLen);
            if(isKeyword(field)) {
                errors.add(new ParseError(line, col, ErrorCode.INVALID_KEY_IN_OBJECT));
                recoverToObjectEnd();
                break;
            }
            // 2. Assignment operator
            skipWhitespace();
            if (!isOperator(Operator.ASSIGN)) {
                errors.add(new ParseError(line, col, ErrorCode.EXPECTED_ASSIGN));
                recoverToObjectEnd();
                break;
            }
            consumeOperator(Operator.ASSIGN);
            // 3. Value
            skipWhitespace();
            var valueResult = parseValue();
            if (!valueResult.isSuccess()) {
                errors.addAll(valueResult.errors());
                recoverToObjectEnd();
                break;
            }
            // 4. Check for duplicate field
            if (fields.containsKey(field)) {
                errors.add(new ParseError(line, col, ErrorCode.DUPLICATE_FIELD_IN_OBJECT));
            } else {
                fields.put(field, valueResult.value());
            }
            // 5. Separator (, or \n) or end block
            if (isOperator(Operator.COMMA)) {
                consumeOperator(Operator.COMMA);
            }
            if (isOperator(Operator.NEWLINE)){
                consumeOperator(Operator.NEWLINE);
            }

            skipWhitespace();
            loop = !isOperator(Operator.R_BRACE);
        }


        // ---- Closing brace ----
        if (!isOperator(Operator.R_BRACE)) {
            errors.add(new ParseError(line, col, ErrorCode.EXPECTED_OBJECT_CLOSE));
            recoverToObjectEnd();
            return ValueParseResult.failure(errors);
        }
        consumeOperator(Operator.R_BRACE);

        String sourceText = source.substring(start, pos);
        return errors.isEmpty()
                ? ValueParseResult.success(new Value.ObjectValue(Map.copyOf(fields), sourceText))
                : ValueParseResult.failure(errors);
    }

    private ValueParseResult<Value> parseArray() {
        int fullStart = pos;
        int startLine = line, startCol = col;
        List<ParseError> errors = new ArrayList<>();
        List<Value> elements = new ArrayList<>();

        consume(); // consume '['

        while (!isEOF() && !isOperator(Operator.R_BRACKET)) {
            skipWhitespace();

            // Optional trailing comma
            if (isOperator(Operator.COMMA)) {
                consumeOperator(Operator.COMMA);
                skipWhitespace();
                if (isOperator(Operator.R_BRACKET)) {
                    errors.add(new ParseError(line, col, ErrorCode.TRAILING_COMMA_IN_ARRAY));
                    break;
                }
                continue;
            }

            ValueParseResult<Value> elemRes = parseValue();
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
            return ValueParseResult.failure(errors);
        }
        consumeOperator(Operator.R_BRACKET);

        String sourceText = source.substring(fullStart, pos);
        return errors.isEmpty()
                ? ValueParseResult.success(new Value.ArrayValue(List.copyOf(elements), sourceText))
                : ValueParseResult.failure(errors);
    }

    private ValueParseResult<Value> parseFreeform() {
        int startLine = line, startCol = col;
        consume();                                 // the leading '@'

        // ---- optional attribute: reuse identifier parser ----
        String attrib = null;
        int idLen = isIdentifier();
        if (idLen > 0) {
            attrib = consume(idLen); // consume identifier
            // attribute cannot be a keyword
            if (isKeyword(attrib)) {
                return failure(ErrorCode.ATTRIBUTE_IS_KEYWORD, startLine, startCol);
            }
        }

        // ---- opening back-tick ----
        if (!isOperator(Operator.BACKTICK)) {
            return failure(ErrorCode.EXPECTED_BACKTICK, startLine, startCol);
        }
        consumeOperator(Operator.BACKTICK);

        // ---- raw content until closing back-tick (escaped ` allowed) ----
        int contentStart = pos;
        while (!isEOF()) {
            if (isOperator(Operator.BACKTICK) && !isEscaped(pos)) break;
            consume();
        }
        String rawContent = source.substring(contentStart, pos);

        // ---- closing back-tick ----
        if (!isOperator(Operator.BACKTICK)) {
            return failure(ErrorCode.UNTERMINATED_FREEFORM, line, col);
        }
        consumeOperator(Operator.BACKTICK);

        return success(new Value.FreeformValue("`" + rawContent + "`", attrib));
    }

    /** true if the back-tick at *pos* is preceded by an odd number of \ */
    private boolean isEscaped(int pos) {
        int backslashes = 0;
        for (int i = pos - 1; i >= 0 && source.charAt(i) == '\\'; i--) backslashes++;
        return backslashes % 2 == 1;
    }

    // --- helpers ---
    private boolean isEOF() {
        return pos >= source.length();
    }
    private boolean isAlpha(char c) {
        return Character.isLetter(c) || c == '_';
    }
    private boolean isAlphaNumeric(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
    private static boolean isHexDigit(char c) {
        return isDigit(c) || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F');
    }

    private char consume() {
        if (isEOF()) return '\0';
        char c = source.charAt(pos++);
        if (c == '\n') {
            line++;
            col = 1;
        } else {
            col++;
        }
        return c;
    }
    // consume n characters and return the string
    private @NotNull String consume(int n) {
        if (n == 0) return "";

        char[] builder = new char[n];
        for (int i = 0; i < n; i++) {
            builder[i] = consume();
        }
        return new String(builder);
    }
    private boolean consumeOperator(Operator op) {
        if (isOperator(op)) {
            consume(op.length());
            return true;
        }

        return false;
    }

    private char peek() {
        return peek(0);
    }
    private char peek(int offset) {
        int ndx = pos + offset;
        return isEOF() ? '\0' : source.charAt(ndx);
    }

    /*
        `match*` functions will consume only on success; otherwise, position is unchanged
        Examples:
            matchKeyword("model")
            matchIdentifier(n)
            ... etc.

        `expect*` function will only look ahead
        Examples:
            expectShebang()
            expect("/*")
            ... etc.
     */
    private @NotNull String matchString() {
        StringBuilder strBuilder = new StringBuilder();
        while (!isEOF() && !isOperator(Operator.QUOTE)) {
            char c = consume();
            if (c == '\\') {
                // escape sequence
                char next = peek();
                switch (next) {
                    case 'n' -> {
                        consume();
                        strBuilder.append('\n');
                    }
                    case 't' -> {
                        consume();
                        strBuilder.append('\t');
                    }
                    case 'r' -> {
                        consume();
                        strBuilder.append('\r');
                    }
                    case '\\' -> {
                        consume();
                        strBuilder.append('\\');
                    }
                    case '"' -> {
                        consume();
                        strBuilder.append('"');
                    }
                    default -> strBuilder.append(c);
                }
            } else {
                strBuilder.append(c);
            }
        }
        return strBuilder.toString();
    }

    private boolean isShebang() {
        // check for either #!asl or #!aml
        boolean isMatch = false;
        if (is("#!asl") || is("#!aml")) {
            hasShebang = true;
            isMatch = true;
        }
        return isMatch;
    }
    private int isIdentifier() {
        // match identifier at current position - should never advance so
        // we never save position -- just report the facts
        int length = 0;
        if (isEOF() || !isAlpha(peek())) return 0;

        while(isAlphaNumeric(peek(length))) {
            length++;
        }

        return length;
    }
    private boolean is(String s) {
        // match string s at current position - should never advance so
        // we never save position -- just report the facts
        boolean isMatch = true;
        for (int i = 0; i < s.length(); i++) {
            if (isEOF()) {
                isMatch = false;
                break;
            }
            else {
                isMatch &= peek(i) == s.charAt(i);
            }
        }
        return isMatch;
    }
    private boolean isOperator(Operator op) {
        return is(op.symbol());
    }
    private boolean isKeyword(String s) {
        return KEYWORDS.contains(s);
    }

    private void skipLeadingLayout() {
        while (!isEOF()) {
            skipWhitespace();
            if (isEOF()) break;
            if (peek() == '/' && peek(1) == '/') {
                skipLineComment();
            } else if (peek() == '/' && peek(1) == '*') {
                skipBlockComment();
            } else {
                break;
            }
        }
    }
    private void skipWhitespace() {
        while (true) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                consume();
            } else if (c == '/' && peek(1) == '/')
                skipLineComment();
            else if (c == '/' && peek(1) == '*')
                skipBlockComment();
            else
                break;
        }
    }
    private void skipLineComment() {
        while (peek() != '\n' && !isEOF())
            consume();
    }
    private void skipBlockComment() {
        consume(2);
        int nest = 1;
        while (nest > 0 && !isEOF()) {
            if (peek() == '*' && peek(1) == '/') {
                consume(2);
                nest--;
            } else if (peek() == '/' && peek(1) == '*') {
                consume(2);
                nest++;
            } else
                consume();
        }
    }

    // error reporting and recovery
    private boolean errShebang(Module module) {
        boolean ifErr = false;
        if (peek() == '#' && isShebang()){
            if (hasShebang) {
                error(ErrorCode.MULTIPLE_SHEBANG, line, col);
                ifErr = true;
            }
            if(module.hasStatements()) {
                error(ErrorCode.SHEBANG_AFTER_STATEMENTS, line, col);
                ifErr = true;
            }
        }

        return ifErr;
    }

    private void error(ErrorCode code, int line, int col) {
        errors.add(new ParseError(line, col, code));
        recover();
    }

    private void recover() {
        while (!isEOF() && peek() != '\n' && peek() != ';')
            consume();
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