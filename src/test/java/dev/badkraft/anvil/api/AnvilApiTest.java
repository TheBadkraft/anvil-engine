// src/test/java/dev/badkraft/anvil/parser/AnvilParserTest.java
package dev.badkraft.anvil.api;

import dev.badkraft.anvil.*;
import dev.badkraft.anvil.Module;
import dev.badkraft.anvil.parser.AnvilParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnvilApiTest {

    private static final Path TEST_RESOURCES = Paths.get("src/test/resources");

    @Test
    void parsesMultipleModuleAttributesCorrectly() {
        Path path = TEST_RESOURCES.resolve("multi_module_attribs.aml");
        ParseResult<Module> result = AnvilParser.parse(path);

        assertTrue(result.isSuccess(), "Parse should succeed without errors");
        Module module = result.result();

        // Dialect check
        assertEquals(Dialect.AML, module.dialect());

        // Attributes merged and ordered
        List<Attribute> attrs = module.attributes();
        assertEquals(8, attrs.size());

        // Spot-check values (using API access)
        assertEquals("version", attrs.get(0).key());
        assertInstanceOf(Value.StringValue.class, attrs.get(0).value());
        assertEquals("1.0.0", ((Value.StringValue) attrs.get(0).value()).value());

        assertEquals("debug", attrs.get(6).key());
        assertInstanceOf(Value.BooleanValue.class, attrs.get(6).value());
        assertTrue(((Value.BooleanValue) attrs.get(6).value()).value());

        assertEquals("experimental", attrs.get(7).key());
        assertNull(attrs.get(7).value());  // tag form

        // Duplicate rejection: Manual test by editing source to duplicate a key → expect failure
        // (Add your own failure case file if needed)
    }

    // Add more tests here for other features as we go
}