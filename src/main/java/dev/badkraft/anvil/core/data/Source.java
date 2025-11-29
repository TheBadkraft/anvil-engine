// dev.badkraft.anvil.core.data.Source
package dev.badkraft.anvil.core.data;

import java.util.Objects;

/**
 * Immutable source view with fast interrogators and shared StringBuilder.
 * Parser never owns the source string — Context does.
 */
public final class Source {

    private final String source;
    private final StringBuilder sb;  // reused across all parser calls

    private int pos = 0;
    private int line = 1;
    private int col = 1;

    public Source(String source) {
        this.source = Objects.requireNonNull(source, "source cannot be null");
        this.sb = new StringBuilder(256);
    }

    // --- Core state ---
    public int position()     { return pos; }
    public int line()    { return line; }
    public int column()  { return col; }

    public void setPosition(int pos, int line, int col) {
        this.pos = pos;
        this.line = line;
        this.col = col;
    }

    // --- Interrogators (peek, is*, read*) ---
    public boolean isEOF() { return pos >= source.length(); }
    public boolean isEOF(int offset) { return pos + offset >= source.length(); }

    public char peek() { return peek(0); }
    public char peek(int offset) {
        int idx = pos + offset;
        return idx < source.length() ? source.charAt(idx) : '\0';
    }

    public boolean is(String s) { return is(s, 0); }
    public boolean is(String s, int offset) {
        for (int i = 0; i < s.length(); i++) {
            if (peek(i + offset) != s.charAt(i)) return false;
        }
        return true;
    }

    public boolean is(char c) { return peek() == c; }
    public boolean is(char c, int offset) { return peek(offset) == c; }

    public boolean isOperator(Operator op) { return is(op.symbol()); }

    public boolean isEscaped(int pos) {
        if (pos <=0) return false;

        int backslashes = 0;
        for (int i = pos - 1; i >= 0 && source.charAt(i) == '\\'; i--) backslashes++;
        return backslashes % 2 == 1;
    }

    public boolean isAlpha(char c) {
        return Character.isLetter(c) || c == '_';
    }
    public boolean isAlphaNumeric(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
    public boolean isDigit(char c) { return c >= '0' && c <= '9'; }
    public boolean isHexDigit(char c) {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    public int readOperator(Operator op) {
        return isOperator(op) ? op.length() : 0;
    }

    public int readChars(char[] chars) {
        int len = 0;
        for (char c : chars) {
            if (is(c, len)) len++;
            else break;
        }
        return len;
    }

    public boolean isShebang() {
        return is("#!asl") || is("#!aml");
    }

    // --- Consumers ---
    public char consume() {
        if (isEOF()) return '\0';
        char c = source.charAt(pos++);
        if (c == '\n') { line++; col = 1; } else col++;
        return c;
    }

    public void consume(int n) {
        for (int i = 0; i < n; i++) consume();
    }

    public void consumeOperator(Operator op) {
        consume(op.length());
    }

    // --- StringBuilder helpers ---
    public StringBuilder buffer() {
        sb.setLength(0);
        return sb;
    }

    public String take(int length) {
        if (length <= 0) return "";
        sb.setLength(0);
        sb.append(source, pos, pos + length);
        consume(length);
        return sb.toString();
    }

    /** Parses dialect from shebang */
    public Dialect parseDialect(Dialect hint) {
        skipWhitespace();
        if (isShebang()) {
            String token = take(5);
            return Dialect.fromShebang(token);
        }
        return hint;
    }

    public void reset() {
        pos = 0;
        line = 1;
        col = 1;
    }

    // --- Whitespace & comments (read length only) ---
    public int readWhitespaceLength() {
        int len = 0;
        while (!isEOF(len)) {
            char c = peek(len);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') len++;
            else if (is("//", len)) len += readLineCommentLength(len);
            else if (is("/*", len)) len += readBlockCommentLength(len);
            else break;
        }
        return len;
    }

    public int readLineCommentLength(int offset) {
        int len = offset;
        if (peek(len) == '/' && peek(len + 1) == '/') {
            len += 2;
            while (!isEOF(len) && peek(len) != '\n') len++;
        }
        return len - offset;
    }

    public int readBlockCommentLength(int offset) {
        int len = offset;
        int depth = 0;
        if (peek(len) == '/' && peek(len + 1) == '*') {
            len += 2;
            depth++;
            while (depth > 0 && !isEOF(len)) {
                if (peek(len) == '*' && peek(len + 1) == '/') {
                    len += 2;
                    depth--;
                } else if (peek(len) == '/' && peek(len + 1) == '*') {
                    len += 2;
                    depth++;
                } else len++;
            }
        }
        return len - offset;
    }

    public void skipWhitespace() {
        consume(readWhitespaceLength());
    }

    // --- Utility ---
    public String substring(int start, int end) {
        return source.substring(start, end);
    }

    public String fullSource() {
        return source;
    }

    @Override
    public String toString() {
        return "Source[pos=%d, line=%d, col=%d, len=%d]".formatted(pos, line, col, source.length());
    }
}