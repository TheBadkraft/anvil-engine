// src/main/java/aurora/engine/parser/AmlParser.java
package aurora.engine.parser;

import aurora.engine.utilities.Utils;
import org.jetbrains.annotations.NotNull;

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
                    new ParseError(0, 0, "IO error: " + e.getMessage())));
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
            int len = 0;
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
        error("Expected assignment operator ':=' after identifier '" + id + "'");
        return null;
    }

    private @NotNull
    ValueParseResult<Value> parseValue() {
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
        error("Expected value");
        return ValueParseResult.failure("Expected value", line, col);
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

    // consume n characters and return the string
    private @NotNull
    String consume(int n) {
        if (n == 0) return "";

        char[] builder = new char[n];
        for (int i = 0; i < n; i++) {
            builder[i] = consume();
        }
        return new String(builder);
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

    private boolean expectOperator(Operator op) {
        return expect(op.symbol());
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

    private Dialect detectDialect(Dialect hint) {
        skipLeadingLayout();

        if (expectShebang()) {
            String token = consume(5); // "#!asl" or "#!aml"
            return Dialect.fromShebang(token);
        }

        return hint;
    }

    private boolean nextLine() {
        while (!isEOF() && peek() != '\n')
            consume();
        if (!isEOF())
            consume();

        return !isEOF();
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
                error("Multiple shebangs are not allowed.");
                ifErr = true;
            }
            if(doc.hasStatements()) {
                error("Shebang can only appear at the beginning of the file.");
                ifErr = true;
            }
        }

        return ifErr;
    }

    private void error(String msg) {
        errors.add(new ParseError(line, col, msg));
        recover();
    }

    private void recover() {
        while (!isEOF() && peek() != '\n' && peek() != ';')
            consume();
    }
}