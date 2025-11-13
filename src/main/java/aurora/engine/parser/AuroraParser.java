// src/main/java/aurora/engine/parser/AmlParser.java
package aurora.engine.parser;

import aurora.engine.utilities.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.*;
import java.io.IOException;
import java.util.*;

public class AuroraParser {
    private final String source;
    private final List<ParseError> errors = new ArrayList<>();
    private boolean hasShebang = false;
    private int pos = 0;
    private int line = 1;
    private int col = 1;

    private AuroraParser(String source) {
        this.source = source;
    }

    public static ParseResult<AuroraDocument> parse(Path path) {
        try {
            String content = Files.readString(path);
            Dialect dialect = Dialect.fromFileExtension(
                    Utils.getFileExtension(path));
            AuroraParser parser = new AuroraParser(content);
            /*
                here's the problem - before we can start getting models, we need to validate the document:
                - shebang (optional) but must be first non-ws line if present
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
    private AuroraDocument parseDocument(Dialect hint) {
        /*
             Let 'parseDocument' handle line states, navigation, error reporting, etc.
             Specialized functions should not modify the parser state directly; they should
             only return results that 'parseDocument' can use to update the state accordingly.
         */
        var doc = new AuroraDocument();
        // 1. skip any leading whitespace/comments
        skipWhitespace();
        // 2. shebang; optional, first non-ws line
        doc.dialect = detectDialect(hint);

        // 3. Parse top-level statements
        while (!isEOF()) {
            skipWhitespace();
            // check for shebang again (error if found)
            if(errShebang(doc)) {
                // error reported inside errShebang
                recover();
                continue;
            }
            // parse identifier ... every top-level statement starts with an identifier
            int len;
            if ((len = expectIdentifier()) > 0) {
                String id = consume(len);
                // for now, we just create a placeholder statement
                doc.addIdentifier(id);
                // after statement, expect line ending (, or EOL or EOF)
                skipWhitespace();
                Statement stmt;
                if((stmt = parseStatement(id)) != null) {
                    doc.addStatement(stmt);
                } else {
                    // error reported inside parseStatement
                    recover();
                }

                continue;
            }

            // if we reach here, it's an unexpected token
            // NOT YET: error("Unexpected token: '" + peek() + "'");
            recover();
        }

        doc.isParsed = true;
        return doc;
    }

    private Dialect detectDialect(Dialect hint) {
        skipLeadingLayout();

        if (expectShebang()) {
            String token = consume(5); // "#!asl" or "#!aml"
            return Dialect.fromShebang(token);
        }

        return hint;
    }

    private Statement parseStatement(String id) {
        // 1. try assignment operator
        if(expectOperator(Operator.ASSIGN)) {
            if(!consume(Operator.ASSIGN.symbol().length()).isEmpty()) {
                skipWhitespace();
                // we have an assignment operator so we parse value ...
                var value = parseValue();

                if (value.isSuccess()) {
                    return new Assignment(id, value.value());
                } else {
                    // error reported inside value result
                    errors.addAll(value.errors());
                    recover();
                    return null;
                }
            }
        }

        // no assignment operator
        error(ErrorCode.EXPECTED_ASSIGN);
        return null;
    }

    private @NotNull ValueParseResult<Value> parseValue() {
        if (expectOperator(Operator.L_BRACE)) {
            consumeOperator(Operator.L_BRACE);
            return parseAnonymousObject();
        }
        if (expect("null")) {
            consume(4);
            return ValueParseResult.success(new Value.NullValue());
        }
        if (expect("true")) {
            consume(4);
            return ValueParseResult.success((new Value.BooleanValue(true)));
        }
        if (expect("false")) {
            consume(5);
            return ValueParseResult.success((new Value.BooleanValue(false)));
        }
        if (expectNumber()) {
            int num = matchNumber();
            return ValueParseResult.success(new Value.NumberValue(num));
        }
        if (expectOperator(Operator.QUOTE)) {
            consumeOperator(Operator.QUOTE);
            String value = matchString();
            if (expectOperator(Operator.QUOTE)) {
                consumeOperator(Operator.QUOTE);
                return ValueParseResult.success(new Value.StringValue(value));
            } else {
                return ValueParseResult.failure(ErrorCode.UNTERMINATED_STRING, line, col);
            }
        }

        return ValueParseResult.failure(ErrorCode.EXPECTED_VALUE, line, col);
    }

    private @NotNull ValueParseResult<Value> parseAnonymousObject() {
        Map<String, Value> fields = new HashMap<>();
        List<ParseError> errors = new ArrayList<>();
        int startLine = line, startCol = col;
        boolean loop = !expectOperator(Operator.R_BRACE);
        while (!isEOF() && loop) {
            skipWhitespace();

            // 1. Key (identifier)
            String field = matchIdentifier();
            if (field == null) {
                errors.add(new ParseError(line, col, ErrorCode.EXPECTED_OBJECT_FIELD));
                recoverToObjectEnd();
                break;
            }
            // 2. Assignment operator
            skipWhitespace();
            if (!expectOperator(Operator.ASSIGN)) {
                errors.add(new ParseError(line, col, ErrorCode.EXPECTED_ASSIGN));
                recoverToObjectEnd();
                break;
            } else {
                consumeOperator(Operator.ASSIGN);
            }
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
            if (expectOperator(Operator.COMMA)) {
                consumeOperator(Operator.COMMA);
            }
            if (expectOperator(Operator.NEWLINE)){
                consumeOperator(Operator.NEWLINE);
            }

            skipWhitespace();
            loop = !expectOperator(Operator.R_BRACE);
        }

        if (expectOperator(Operator.R_BRACE)) {
            consumeOperator(Operator.R_BRACE);
            if (errors.isEmpty()) {
                return ValueParseResult.success(new Value.ObjectValue(fields));
            } else {
                errors.add(new ParseError(startLine, startCol, ErrorCode.ERRORS_IN_OBJECT));
            }
        } else {
            errors.add(new ParseError(startLine, startCol, ErrorCode.EXPECTED_OBJECT_CLOSE));
            recoverToObjectEnd();
            return ValueParseResult.failure(errors);
        }

        return errors.isEmpty()
                ? ValueParseResult.success(new Value.ObjectValue(Map.copyOf(fields)))
                : ValueParseResult.failure(errors);
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
    private int consumeOperator(Operator op) {
        String sym = op.symbol();
        int len = sym.length();
        consume(len);
        return len;
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
    private @Nullable String matchIdentifier() {
        int len = expectIdentifier();
        if (len == 0) return null;
        return consume(len);
    }
    private @NotNull String matchString() {
        StringBuilder strBuilder = new StringBuilder();
        while (!isEOF() && !expectOperator(Operator.QUOTE)) {
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
    private int matchNumber() {
        StringBuilder numBuilder = new StringBuilder();
        while (!isEOF() && Character.isDigit(peek())) {
            numBuilder.append(consume());
        }
        return Integer.parseInt(numBuilder.toString());
    }

    private boolean expectShebang() {
        // check for either #!asl or #!aml
        boolean isMatch = false;
        if (expect("#!asl") || expect("#!aml")) {
            hasShebang = true;
            isMatch = true;
        }
        return isMatch;
    }

    private int expectIdentifier() {
        // match identifier at current position - should never advance so
        // we never save position -- just report the facts
        int length = 0;
        if (isEOF() || !isAlpha(peek())) return 0;

        while(isAlphaNumeric(peek(length))) {
            length++;
        }

        return length;
    }

    private boolean expect(String s) {
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
    private boolean expectOperator(Operator op) {
        return expect(op.symbol());
    }
    private boolean expectNumber() {
        return !isEOF() && Character.isDigit(peek());
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
    private boolean errShebang(AuroraDocument doc) {
        boolean ifErr = false;
        if (peek() == '#' && expectShebang()){
            if (hasShebang) {
                error(ErrorCode.MULTIPLE_SHEBANG);
                ifErr = true;
            }
            if(doc.hasStatements()) {
                error(ErrorCode.SHEBANG_AFTER_STATEMENTS);
                ifErr = true;
            }
        }

        return ifErr;
    }

    private void error(ErrorCode code) {
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
            if (peek(0) == '{') depth++;
            if (peek(0) == '}') depth--;
            consume();
        }
    }
}