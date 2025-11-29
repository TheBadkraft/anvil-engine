package dev.badkraft.anvil.api;

import dev.badkraft.anvil.core.api.Context;
import dev.badkraft.anvil.parser.AnvilParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class ContextTest {
    @Test
    public void initContext() throws IOException {
        Context context = Context.builder()
                .namespace("testNamespace")
                .source(Paths.get("src/test/resources/attributes.aml"))
                .build();

        // Verify that the context is initialized correctly
        assertNotNull(context);
        assertEquals("testNamespace", context.namespace());

        // Verify peek is looking at the first non-whitespace character after shebang
        assertEquals('@', context.source().peek());

        // Passing verifies that source is properly positioned and ready for parsing
    }
}
