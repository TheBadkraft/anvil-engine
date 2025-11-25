// src/test/java/dev/badkraft/anvil/parser/AnvilParserTest.java
package dev.badkraft.anvil.api;

import dev.badkraft.anvil.*;
import dev.badkraft.anvil.Module;
import dev.badkraft.anvil.parser.AnvilParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnvilApiTest {
    /*
            API tests go through the Anvil API (anvil.api.Anvil) to parse AML files.
            Do not use AnvilParser directly here, as that bypasses the API layer.
     */
    private static final Path TEST_RESOURCES = Paths.get("src/test/resources");

    @Test
    void parsesMultipleModuleAttributesCorrectly() throws IOException {
        Path path = TEST_RESOURCES.resolve("multi_module_attribs.aml");
        AnvilModule module = Anvil.parse(path);

        assertNotNull(module, "Module should be parsed successfully");

        // Attributes merged and ordered
        assertEquals(8, module.attributes().size());

        // Spot-check values (using API access)
        assertEquals("1.0.0", module.getAttribute("version").value().asString());
        assertEquals("1.21.10", module.getAttribute("mc_version").value().asString());
        assertTrue(module.getAttribute("debug").value().asBoolean());

        // Duplicate rejection: Manual test by editing source to duplicate a key → expect failure
        // (Add your own failure case file if needed)
    }

    @Test
    void dottedKeys_areParsedCorrectly() throws IOException {
        var path = Paths.get("src/test/resources/dotted_keys.aml");
        AnvilModule module = Anvil.parse(path);

        assertNotNull(module, "Module should be parsed successfully");

        assertTrue(module.contains("net.minecraft.client.Minecraft"));
        assertTrue(module.contains("com.mojang.blaze3d.audio.Channel"));

        // Not supported in API yet:
//        Assignment bare = (Assignment) stmts.get(2);
//        assertEquals(".default_material", bare.identifier());
//        assertInstanceOf(Value.BareLiteral.class, bare.value());
//        assertEquals(".minecraft:stone", ((Value.BareLiteral) bare.value()).id());
    }

    @Test
    void nestedAttribution_onlyOnObjectFields() throws IOException {
        var path = Paths.get("src/test/resources/nested_attribution.aml");

        AnvilModule module = Anvil.parse(path);

        assertNotNull(module, "Module should be parsed successfully");
        var player = module.getObject("player");

        assertInstanceOf(AnvilTuple.class, player.getTuple("position"));
        assertTrue(player.getTuple("position").hasAttribute("dim"));
        assertTrue(player.getArray("inventory").hasAttribute("max"));

        // position has attributes → value is tuple → should be REJECTED
//        var position = ;
//        var positionField = player.fields().stream()
//                .filter(e -> e.getKey().equals("position"))
//                .findFirst().orElseThrow();


    }
}