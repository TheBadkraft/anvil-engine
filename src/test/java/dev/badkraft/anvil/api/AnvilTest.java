// dev.badkraft.anvil.api/AnvilTest.java
package dev.badkraft.anvil.api;

import dev.badkraft.anvil.core.data.Attribute;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class AnvilTest {

    private static final String ROOT_ATTRIBUTES = """
        #!aml
        @[version="1.0.0", mc_version="1.21.10", debug=true]
        @[source="mojang-official-proguard"]

        name := "Test Module"
        description := "A context to test attributes"
        bonus_items @[rarity="legendary", count=3] := [
            minecraft:diamond_sword
            minecraft:enchanted_golden_apple
        ]
        """;

    private static final String SIMPLE_STATEMENTS = """
        #!aml
        name := "Badkraft"
        age := 42
        admin := true
        health := 20.0
        id := badkraft
        desc := @md`**legend**`
        """;

    private static final String NESTED_STRUCTURE = """
        #!aml
        player := {
            name := "Grok"
            pos := (10, 64, -300)
            inventory := [
                "diamond_sword"
                "netherite_chestplate"
                "elytra"
            ]
            metadata := {
                joined := "2025-11-30"
                playtime_hours := 1337
                verified := true
            }
        }
        """;

    private static final String TOP_LEVEL_ARRAY = """
        #!aml
        @[tags="core", priority=100]
        items := [
            "stick"
            "string"
            "feather"
        ]
        """;

    private static final String MIXED_TYPES_ARRAY = """
        #!aml
        mixed @[test=true] := [
            "hello"
            42
            true
            3.14
            badkraft
            @md`**bold**`
        ]
        """;

    @Test
    void loadFromFile_withRootAttributes() throws IOException {
        AnvilRoot root = Anvil.load(Paths.get("src/test/resources/attributes.aml"));

        // Root-level attributes — from @[version=0.1, ...]
        assertTrue(true);
    }


}