package dev.badkraft.aurora.engine.parser;

public enum Dialect {
    ASL(0, "asl"),
    AML(1, "aml");

    private final int code;
    private final String name;

    Dialect(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    // to string implementation
    @Override
    public String toString() {
        return name.toUpperCase();
    }

    public static Dialect fromFileExtension(String fileExtension) {
        return switch (fileExtension) {
            case "asl" -> ASL;
            case "aml" -> AML;
            default -> throw new IllegalArgumentException("Unsupported file extension: " + fileExtension);
        };
    }

    public static Dialect fromShebang(String token) {
        return switch (token) {
            case "#!asl" -> ASL;
            case "#!aml" -> AML;
            default -> throw new IllegalArgumentException("Unsupported shebang token: " + token);
        };
    }
}
