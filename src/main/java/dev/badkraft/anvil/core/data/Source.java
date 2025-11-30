// dev.badkraft.anvil.core.data.Source.java
package dev.badkraft.anvil.core.data;

import java.util.Objects;

/**
 * Immutable source view. Zero shared mutable state.
 * All interrogators return either boolean or int length.
 * No String materialization unless explicitly requested via substring().
 */
public final class Source {

    private final String source;
    private int pos = 0;
    private int line = 1;
    private int col = 1;

    public Source(String source) {
        this.source = Objects.requireNonNull(source, "source cannot be null");
    }

    // --- Position & EOF ---
    public int position() { return pos; }
    public int line()     { return line; }
    public int column()   { return col; }
    public boolean isEOF() { return pos >= source.length(); }
    public boolean isEOF(int offset) { return pos + offset >= source.length(); }

    public void setPosition(int pos, int line, int col) {
        this.pos = pos;
        this.line = line;
        this.col = col;
    }

    // --- Character peek ---
    public char peek() { return peek(0); }
    public char peek(int offset) {
        int idx = pos + offset;
        return idx < source.length() ? source.charAt(idx) : '\0';
    }

    // --- Exact string match (boolean) ---
    public boolean is(String s) { return is(s, 0); }
    public boolean is(String s, int offset) {
        if (s.isEmpty()) return true;
        for (int i = 0; i < s.length(); i++) {
            if (peek(offset + i) != s.charAt(i)) return false;
        }
        return pos + offset + s.length() <= source.length();
    }

    public boolean is(char c) { return peek() == c; }
    public boolean is(char c, int offset) { return peek(offset) == c; }

    // --- Operator shorthand ---
    public boolean isOperator(Operator op) { return is(op.symbol()); }

    // --- Character classification ---
    public boolean isAlpha(char c)          { return Character.isLetter(c) || c == '_'; }
    public boolean isDigit(char c)          { return c >= '0' && c <= '9'; }
    public boolean isHexDigit(char c)       { return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'); }
    public boolean isIdentifierStart(char c) { return Character.isLetter(c) || c == '_'; }
    public boolean isIdentifierPart(char c)  { return Character.isLetterOrDigit(c) || c == '_' || c == '.'; }

    // --- Escape detection ---
    public boolean isEscaped(int pos) {
        if (pos <= 0) return false;
        int backslashes = 0;
        for (int i = pos - 1; i >= 0 && source.charAt(i) == '\\'; i--) {
            backslashes++;
        }
        return backslashes % 2 == 1;
    }

    // --- Shebang ---
    public boolean isShebang() {
        return is("#!asl") || is("#!aml");
    }

    // --- Length-based interrogators (return int length or 0) ---
    public int matchLength(String s) {
        return is(s) ? s.length() : 0;
    }

    public int matchOperator(Operator op) {
        return matchLength(op.symbol());
    }

    public int matchHexPrefix() {
        return is("#") ? 1 : (is("0x") || is("0X")) ? 2 : 0;
    }

    // --- Consume ---
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

    // --- String extraction (only place we materialize) ---
    public String substring(int start, int end) {
        return source.substring(start, end);
    }

    public String substringFrom(int start) {
        return source.substring(start, source.length());
    }

    // --- Whitespace & comments (length only) ---
    public int skipWhitespaceAndComments() {
        int len = 0;
        while (true) {
            int ws = scanWhitespaceLength(len);
            int line = scanLineCommentLength(len + ws);
            int block = scanBlockCommentLength(len + ws + line);
            if (ws + line + block == 0) break;
            len += ws + line + block;
        }
        return len;
    }

    private int scanWhitespaceLength(int offset) {
        int len = 0;
        while (!isEOF(offset + len)) {
            char c = peek(offset + len);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') len++;
            else break;
        }
        return len;
    }

    private int scanLineCommentLength(int offset) {
        if (!is("//", offset)) return 0;
        int len = 2;
        while (!isEOF(offset + len) && peek(offset + len) != '\n') len++;
        return len;
    }

    private int scanBlockCommentLength(int offset) {
        if (!is("/*", offset)) return 0;
        int len = 2;
        int depth = 1;
        while (depth > 0 && !isEOF(offset + len)) {
            if (is("*/", offset + len)) { len += 2; depth--; }
            else if (is("/*", offset + len)) { len += 2; depth++; }
            else len++;
        }
        return depth == 0 ? len : 0; // incomplete block comment → not skipped
    }

    public void skipWhitespace() {
        consume(skipWhitespaceAndComments());
    }

    // --- Dialect from shebang ---
    public Dialect parseDialect(Dialect hint) {
        skipWhitespace();
        if (isShebang()) {
            String token = source.substring(pos, pos + 5);
            consume(5);
            return Dialect.fromShebang(token);
        }
        return hint;
    }

    public void reset() {
        pos = 0;
        line = 1;
        col = 1;
    }

    public String fullSource() { return source; }
    public int length() { return source.length();}

    @Override
    public String toString() {
        return "Source[pos=%d, line=%d, col=%d, len=%d]".formatted(pos, line, col, source.length());
    }

}