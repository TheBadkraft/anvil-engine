package dev.badkraft.anvil.engine;

import dev.badkraft.anvil.ErrorCode;
import dev.badkraft.anvil.Module;
import dev.badkraft.anvil.ParseException;
import dev.badkraft.anvil.ParseResult;
import dev.badkraft.anvil.api.AnvilModule;
import dev.badkraft.anvil.api.ImmutableAnvilModule;
import dev.badkraft.anvil.parser.AnvilParser;

import java.io.IOException;
import java.nio.file.Path;

public class AnvilParserAdapter {
    public static AnvilModule fromFile(Path file) throws IOException {
        ParseResult<Module> result = AnvilParser.parse(file);
        if (!result.errors().isEmpty()) {
            throw new ParseException(ErrorCode.PARSING_FAILED, "Failed to parse Anvil module: ", result.errors());
        }
        return new ImmutableAnvilModule(result.result(), file);
    }

    public static AnvilModule fromString(String source, String name) {
        // TODO: implement string parsing path (virtual Path or in-memory)
        throw new UnsupportedOperationException("String parsing not yet implemented");
    }
}
