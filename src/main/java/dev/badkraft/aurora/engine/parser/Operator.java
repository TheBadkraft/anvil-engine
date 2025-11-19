package dev.badkraft.aurora.engine.parser;

// We need to separate actual operators from symbols
public enum Operator {
    ASSIGN(":=", 2),
    EQUAL("=", 1),
    AT("@", 1),
    BACKTICK("`", 1), // token, length
    COLON(":", 1),
    COMMA(",", 1),
    DOT_DOT("..", 2),
    L_BRACE("{", 1),
    R_BRACE("}", 1),
    L_BRACKET("[", 1),
    R_BRACKET("]", 1),
    L_PAREN("(", 1),
    R_PAREN(")", 1),
    NEWLINE("\n", 2 ),
    QUOTE("\"", 1),
    S_QUOTE("'", 1),
    ROCKET("=>", 2),;

    private final String symbol;
    private final int length;

    Operator(String symbol, int length) {
        this.symbol = symbol;
        this.length = length;
    }

    public String symbol() {
        return symbol;
    }
    public int length() { return length; }

    public static Operator fromToken(String token) {
        for (Operator op : Operator.values()) {
            if (op.symbol.equals(token)) {
                return op;
            }
        }
        throw new IllegalArgumentException(("Unknown operator: " + token));
    }
}
