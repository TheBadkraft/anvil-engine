// src/test/java/dev/badkraft/anvil/api/ValueTypePredicatesTest.java
package dev.badkraft.anvil.api;

import dev.badkraft.anvil.data.value;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for value.isXxx() type-check predicates.
 *
 * Each predicate returns true only for the matching type; all others return false.
 * Enables branch-without-try/catch — no exception catching required.
 */
class ValueTypePredicatesTest {

    // ── isString ──────────────────────────────────────────────────────────

    @Test
    void isString_trueForStringValue() throws IOException {
        assertTrue(val("#!aml\nk := \"hello\"").isString());
    }

    @Test
    void isString_falseForLong() throws IOException {
        assertFalse(val("#!aml\nk := 42").isString());
    }

    @Test
    void isString_falseForBoolean() throws IOException {
        assertFalse(val("#!aml\nk := true").isString());
    }

    // ── isLong ────────────────────────────────────────────────────────────

    @Test
    void isLong_trueForLongValue() throws IOException {
        assertTrue(val("#!aml\nk := 99").isLong());
    }

    @Test
    void isLong_falseForString() throws IOException {
        assertFalse(val("#!aml\nk := \"hello\"").isLong());
    }

    @Test
    void isLong_falseForDouble() throws IOException {
        assertFalse(val("#!aml\nk := 3.14").isLong());
    }

    // ── isDouble ──────────────────────────────────────────────────────────

    @Test
    void isDouble_trueForDoubleValue() throws IOException {
        assertTrue(val("#!aml\nk := 3.14").isDouble());
    }

    @Test
    void isDouble_falseForLong() throws IOException {
        assertFalse(val("#!aml\nk := 42").isDouble());
    }

    // ── isBoolean ─────────────────────────────────────────────────────────

    @Test
    void isBoolean_trueForBooleanValue() throws IOException {
        assertTrue(val("#!aml\nk := true").isBoolean());
        assertTrue(val("#!aml\nk := false").isBoolean());
    }

    @Test
    void isBoolean_falseForString() throws IOException {
        assertFalse(val("#!aml\nk := \"true\"").isBoolean());
    }

    // ── isNull ────────────────────────────────────────────────────────────

    @Test
    void isNull_trueForNullValue() throws IOException {
        assertTrue(val("#!aml\nk := null").isNull());
    }

    @Test
    void isNull_falseForString() throws IOException {
        assertFalse(val("#!aml\nk := \"null\"").isNull());
    }

    // ── isObject ─────────────────────────────────────────────────────────

    @Test
    void isObject_trueForObjectValue() throws IOException {
        assertTrue(val("#!aml\nk := { x := 1 }").isObject());
    }

    @Test
    void isObject_falseForArray() throws IOException {
        assertFalse(val("#!aml\nk := [1, 2, 3]").isObject());
    }

    // ── isArray ───────────────────────────────────────────────────────────

    @Test
    void isArray_trueForArrayValue() throws IOException {
        assertTrue(val("#!aml\nk := [1, 2, 3]").isArray());
    }

    @Test
    void isArray_falseForTuple() throws IOException {
        assertFalse(val("#!aml\nk := (1, 2, 3)").isArray());
    }

    // ── isTuple ───────────────────────────────────────────────────────────

    @Test
    void isTuple_trueForTupleValue() throws IOException {
        assertTrue(val("#!aml\nk := (10, 64, 10)").isTuple());
    }

    @Test
    void isTuple_falseForArray() throws IOException {
        assertFalse(val("#!aml\nk := [10, 64, 10]").isTuple());
    }

    // ── cross-predicate exclusivity ────────────────────────────────────────

    @Test
    void predicates_areExclusive_forString() throws IOException {
        value v = val("#!aml\nk := \"hello\"");
        assertTrue(v.isString());
        assertFalse(v.isLong());
        assertFalse(v.isDouble());
        assertFalse(v.isBoolean());
        assertFalse(v.isNull());
        assertFalse(v.isObject());
        assertFalse(v.isArray());
        assertFalse(v.isTuple());
    }

    @Test
    void predicates_areExclusive_forObject() throws IOException {
        value v = val("#!aml\nk := { x := 1 }");
        assertTrue(v.isObject());
        assertFalse(v.isString());
        assertFalse(v.isLong());
        assertFalse(v.isArray());
        assertFalse(v.isTuple());
    }

    // ── branch-without-catch usage pattern ────────────────────────────────

    @Test
    void isLong_enablesBranchWithoutCatch() throws IOException {
        value v = val("#!aml\nk := 7");
        int result = v.isLong() ? v.asInt() : 0;
        assertEquals(7, result);
    }

    // ── helper ────────────────────────────────────────────────────────────

    private static value val(String src) throws IOException {
        return Anvil.read(src).parse().get("k");
    }
}


