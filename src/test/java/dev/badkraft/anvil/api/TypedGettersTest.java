// src/test/java/dev/badkraft/anvil/api/TypedGettersTest.java
package dev.badkraft.anvil.api;

import dev.badkraft.anvil.data.array;
import dev.badkraft.anvil.data.object;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for typed convenience getters on object and node.
 *
 * Throwing overloads:
 *   - throw NoSuchElementException when key is absent (never NPE)
 *   - throw ClassCastException on type mismatch
 *
 * Default-value overloads:
 *   - NEVER throw; return the supplied default when key is absent or null
 */
class TypedGettersTest {

    private static final String HOOK_SOURCE = """
        #!aml
        hooks := {
            action    := "removeWidget"
            target    := "menu.online"
            count     := 3
            ratio     := 1.5
            enabled   := true
            child     := { name := "nested" }
            tags      := [ "a", "b", "c" ]
        }
        """;

    // ── object typed getters — throwing ───────────────────────────────────

    @Test
    void object_getString_returnsValue() throws IOException {
        object obj = hookObject();
        assertEquals("removeWidget", obj.getString("action"));
    }

    @Test
    void object_getString_throwsNoSuchElementForMissingKey() throws IOException {
        object obj = hookObject();
        assertThrows(NoSuchElementException.class, () -> obj.getString("missing"));
    }

    @Test
    void object_getString_throwsClassCastOnWrongType() throws IOException {
        object obj = hookObject();
        assertThrows(ClassCastException.class, () -> obj.getString("count"));
    }

    @Test
    void object_getLong_returnsValue() throws IOException {
        object obj = hookObject();
        assertEquals(3L, obj.getLong("count"));
    }

    @Test
    void object_getLong_throwsNoSuchElementForMissingKey() throws IOException {
        object obj = hookObject();
        assertThrows(NoSuchElementException.class, () -> obj.getLong("missing"));
    }

    @Test
    void object_getInt_returnsNarrowedValue() throws IOException {
        object obj = hookObject();
        assertEquals(3, obj.getInt("count"));
    }

    @Test
    void object_getDouble_returnsValue() throws IOException {
        object obj = hookObject();
        assertEquals(1.5, obj.getDouble("ratio"), 1e-9);
    }

    @Test
    void object_getBoolean_returnsValue() throws IOException {
        object obj = hookObject();
        assertTrue(obj.getBoolean("enabled"));
    }

    @Test
    void object_getBoolean_throwsClassCastOnWrongType() throws IOException {
        object obj = hookObject();
        assertThrows(ClassCastException.class, () -> obj.getBoolean("action"));
    }

    @Test
    void object_getObject_returnsNestedObject() throws IOException {
        object obj = hookObject();
        object child = obj.getObject("child");
        assertNotNull(child);
        assertEquals("nested", child.getString("name"));
    }

    @Test
    void object_getObject_throwsNoSuchElementForMissingKey() throws IOException {
        object obj = hookObject();
        assertThrows(NoSuchElementException.class, () -> obj.getObject("missing"));
    }

    @Test
    void object_getArray_returnsArray() throws IOException {
        object obj = hookObject();
        array tags = obj.getArray("tags");
        assertNotNull(tags);
        assertEquals(3, tags.size());
    }

    @Test
    void object_getArray_throwsNoSuchElementForMissingKey() throws IOException {
        object obj = hookObject();
        assertThrows(NoSuchElementException.class, () -> obj.getArray("missing"));
    }

    // ── object default-value overloads — never throw ──────────────────────

    @Test
    void object_getString_defaultReturnsDefaultWhenMissing() throws IOException {
        assertEquals("fallback", hookObject().getString("missing", "fallback"));
    }

    @Test
    void object_getString_defaultReturnsActualValueWhenPresent() throws IOException {
        assertEquals("removeWidget", hookObject().getString("action", "fallback"));
    }

    @Test
    void object_getLong_defaultReturnsDefaultWhenMissing() throws IOException {
        assertEquals(99L, hookObject().getLong("missing", 99L));
    }

    @Test
    void object_getInt_defaultReturnsDefaultWhenMissing() throws IOException {
        assertEquals(-1, hookObject().getInt("missing", -1));
    }

    @Test
    void object_getDouble_defaultReturnsDefaultWhenMissing() throws IOException {
        assertEquals(0.0, hookObject().getDouble("missing", 0.0), 1e-9);
    }

    @Test
    void object_getBoolean_defaultReturnsDefaultWhenMissing() throws IOException {
        assertFalse(hookObject().getBoolean("missing", false));
    }

    @Test
    void object_defaultOverload_neverThrows() throws IOException {
        object obj = hookObject();
        // Calling a default overload on a key whose value is the wrong type must not throw
        assertDoesNotThrow(() -> obj.getString("count", "default"));
        assertDoesNotThrow(() -> obj.getLong("action", 0L));
    }

    // ── node typed getters — delegation to object ─────────────────────────

    @Test
    void node_getString_delegatesToObject() throws IOException {
        root r = Anvil.read(HOOK_SOURCE).parse();
        assertEquals("removeWidget", r.node("hooks").getString("action"));
    }

    @Test
    void node_getLong_delegatesToObject() throws IOException {
        root r = Anvil.read(HOOK_SOURCE).parse();
        assertEquals(3L, r.node("hooks").getLong("count"));
    }

    @Test
    void node_getInt_delegatesToObject() throws IOException {
        root r = Anvil.read(HOOK_SOURCE).parse();
        assertEquals(3, r.node("hooks").getInt("count"));
    }

    @Test
    void node_getDouble_delegatesToObject() throws IOException {
        root r = Anvil.read(HOOK_SOURCE).parse();
        assertEquals(1.5, r.node("hooks").getDouble("ratio"), 1e-9);
    }

    @Test
    void node_getBoolean_delegatesToObject() throws IOException {
        root r = Anvil.read(HOOK_SOURCE).parse();
        assertTrue(r.node("hooks").getBoolean("enabled"));
    }

    @Test
    void node_getString_throwsUnsupportedOperationWhenNotObject() throws IOException {
        root r = Anvil.read("#!aml\nport := 25565").parse();
        assertThrows(UnsupportedOperationException.class, () -> r.node("port").getString("anything"));
    }

    // ── helper ────────────────────────────────────────────────────────────

    private static object hookObject() throws IOException {
        return Anvil.read(HOOK_SOURCE).parse().get("hooks").asObject();
    }
}

