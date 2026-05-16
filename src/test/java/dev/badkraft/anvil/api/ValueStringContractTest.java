// src/test/java/dev/badkraft/anvil/api/ValueStringContractTest.java
package dev.badkraft.anvil.api;

import dev.badkraft.anvil.data.object;
import dev.badkraft.anvil.data.value;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract: value.asString() always returns the raw content — no surrounding quote delimiters.
 * BareLiteral values (unquoted identifiers used as enum-like values) must also resolve via asString().
 */
class ValueStringContractTest {

    // ── Quoted strings ─────────────────────────────────────────────────────

    @Test
    void quotedString_stripsDoubleQuotes() throws IOException {
        root r = Anvil.read("#!aml\nkey := \"hello\"").parse();
        assertEquals("hello", r.get("key").asString());
    }

    @Test
    void quotedString_preservesInternalSpaces() throws IOException {
        root r = Anvil.read("#!aml\nkey := \"hello world\"").parse();
        assertEquals("hello world", r.get("key").asString());
    }

    @Test
    void quotedString_preservesInternalDots() throws IOException {
        root r = Anvil.read("#!aml\nkey := \"menu.online\"").parse();
        assertEquals("menu.online", r.get("key").asString());
    }

    // ── Bare literals ──────────────────────────────────────────────────────

    @Test
    void bareLiteral_returnsIdentifierAsString() throws IOException {
        root r = Anvil.read("#!aml\ntarget := realmsNotificationsScreen").parse();
        assertEquals("realmsNotificationsScreen", r.get("target").asString());
    }

    @Test
    void bareLiteral_inObjectField_returnsString() throws IOException {
        root r = Anvil.read("""
            #!aml
            server := {
                host := "localhost"
                type := plain
            }
            """).parse();
        object obj = r.get("server").asObject();
        assertEquals("localhost", obj.get("host").asString());
        assertEquals("plain",     obj.get("type").asString());
    }

    // ── Wrong-type guards ─────────────────────────────────────────────────

    @Test
    void longValue_asString_throwsClassCastException() throws IOException {
        root r = Anvil.read("#!aml\ncount := 42").parse();
        assertThrows(ClassCastException.class, () -> r.get("count").asString());
    }

    @Test
    void booleanValue_asString_throwsClassCastException() throws IOException {
        root r = Anvil.read("#!aml\nflag := true").parse();
        assertThrows(ClassCastException.class, () -> r.get("flag").asString());
    }

    @Test
    void doubleValue_asString_throwsClassCastException() throws IOException {
        root r = Anvil.read("#!aml\nrate := 3.14").parse();
        assertThrows(ClassCastException.class, () -> r.get("rate").asString());
    }
}

