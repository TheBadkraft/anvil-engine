package dev.badkraft.anvil.core.data;

public enum Dialect {
    ASL(0, "asl"),
    AML(1, "aml"),
    NONE(-1, "nil"),;

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
            // let's return NONE for unsupported extensions, let the caller handle it
            default -> NONE;
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
