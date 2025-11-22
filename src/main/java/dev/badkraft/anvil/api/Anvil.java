package dev.badkraft.anvil.api;

import dev.badkraft.anvil.engine.AnvilParserAdapter;
import java.io.IOException;
import java.nio.file.Path;

public final class Anvil {
    private Anvil() {}

    public static AnvilModule parse(Path file) throws IOException {
        return AnvilParserAdapter.fromFile(file);
    }

    public static AnvilModule parse(String source, String sourceName) {
        return AnvilParserAdapter.fromString(source, sourceName);
    }

    public static AnvilModule parse(String source) {
        return parse(source, "<string>");
    }

    // Future public surface lands here or in sibling types
    // public static AnvilModule merge(AnvilModule... modules) { ... }
    // public static void hotReload(Path changed) { ... }
}