// src/test/java/dev/badkraft/anvil/api/HasNodeTest.java
package dev.badkraft.anvil.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for root.hasNode(String) and node.hasField(String).
 *
 * root.hasNode — symmetric pair of root.hasAttribute; never throws.
 * node.hasField — delegates to object.has(field); never throws.
 */
class HasNodeTest {

    private static final String SOURCE = """
        #!aml
        @[secure]

        server @[core] := {
            ip   := "127.0.0.1"
            port := 25565
        }
        motd := "Welcome"
        """;

    // ── root.hasNode ───────────────────────────────────────────────────────

    @Test
    void hasNode_returnsTrueForPresentKey() throws IOException {
        root r = Anvil.read(SOURCE).parse();
        assertTrue(r.hasNode("server"));
    }

    @Test
    void hasNode_returnsTrueForPrimitiveNode() throws IOException {
        root r = Anvil.read(SOURCE).parse();
        assertTrue(r.hasNode("motd"));
    }

    @Test
    void hasNode_returnsFalseForMissingKey() throws IOException {
        root r = Anvil.read(SOURCE).parse();
        assertFalse(r.hasNode("missing"));
    }

    @Test
    void hasNode_doesNotThrowForMissingKey() throws IOException {
        root r = Anvil.read(SOURCE).parse();
        // Must not throw NoSuchElementException (unlike node())
        assertDoesNotThrow(() -> r.hasNode("definitely_not_there"));
    }

    @Test
    void hasNode_falseEnablesConditionalNodeAccess() throws IOException {
        root r = Anvil.read(SOURCE).parse();
        // The guard-then-access pattern that motivated this feature
        if (r.hasNode("server")) {
            assertNotNull(r.node("server"));
        } else {
            fail("Expected server node to be present");
        }
    }

    // ── node.hasField ──────────────────────────────────────────────────────

    @Test
    void hasField_returnsTrueForPresentField() throws IOException {
        root r = Anvil.read(SOURCE).parse();
        assertTrue(r.node("server").hasField("ip"));
        assertTrue(r.node("server").hasField("port"));
    }

    @Test
    void hasField_returnsFalseForAbsentField() throws IOException {
        root r = Anvil.read(SOURCE).parse();
        assertFalse(r.node("server").hasField("name"));
    }

    @Test
    void hasField_returnsFalseWhenNodeValueIsNotObject() throws IOException {
        root r = Anvil.read(SOURCE).parse();
        // motd is a StringValue, not an object — hasField must not throw
        assertFalse(r.node("motd").hasField("anything"));
    }

    @Test
    void hasField_doesNotThrowForAnyNodeType() throws IOException {
        root r = Anvil.read("""
            #!aml
            pos    := (10, 64, 10)
            scores := [1, 2, 3]
            flag   := true
            """).parse();
        assertDoesNotThrow(() -> r.node("pos").hasField("x"));
        assertDoesNotThrow(() -> r.node("scores").hasField("0"));
        assertDoesNotThrow(() -> r.node("flag").hasField("val"));
    }
}

