# Anvil Engine 0.1.8 — Release Notes
## API Completeness (`minecraft.ic` Phase 2 Unblock)

**Released:** May 15, 2026
**Tag:** `Alpha-v0.1.8`
**Author:** The Badkraft

---

## Summary

Version 0.1.8 closes every gap identified in the
[`anvil-0.1.8-api-completeness`](feature-requests/anvil-0.1.8-api-completeness.md)
feature request. All three blocking items for `minecraft.ic` Phase 2
(the Hook Engine) are resolved:

| # | Item | Status |
|---|------|--------|
| 1 | `asString()` contract — quotes stripped, `BareLiteral` resolves as string | ✅ Done |
| 2 | `root.hasNode(String)` + `node.hasField(String)` | ✅ Done |
| 3 | Typed convenience getters on `object` and `node` | ✅ Done |

Bonus items delivered in the same pass:

| # | Item | Status |
|---|------|--------|
| 4 | `value` type-check predicates (`isString()`, `isLong()`, …) | ✅ Done |
| 5 | Canonical `.anvl` file extension for test resources | ✅ Done |

---

## Acceptance Criteria — Verified

### 1 · `asString()` Contract

> `Anvil.load(path).root.node("key").value().asString()` on `key = "quoted"` returns `quoted` (no outer quotes)

**Mechanism:** `AnvilParser.parseContent(QUOTE)` already advanced `start` past the
opening `"` and stopped `end` before the closing `"`. `Value.StringValue.content()`
therefore returns the delimiter-free span. `AnvilConverters.toValue()` passes that
string verbatim into `value.StringValue`.

**What changed:** `BareLiteral` values (unquoted enum-like tokens such as
`type := sword`) were previously unhandled in `AnvilConverters.toValue()`, causing
an `IllegalArgumentException` at runtime. A new `case Value.BareLiteral b`
mapping them to `value.StringValue(b.value())` was added.

**Tests added** (`ValueStringContractTest`):

| Test | Asserts |
|------|---------|
| `quotedString_stripsDoubleQuotes` | `key = "hello"` → `asString()` == `"hello"` |
| `quotedString_preservesInternalSpaces` | `key = "hello world"` → `asString()` == `"hello world"` |
| `quotedString_preservesInternalDots` | `key = "menu.online"` → `asString()` == `"menu.online"` |
| `bareLiteral_returnsIdentifierAsString` | `target := realmsNotificationsScreen` → `asString()` == `"realmsNotificationsScreen"` |
| `bareLiteral_inObjectField_returnsString` | Mixed object with quoted + bare fields — both correct |
| `longValue_asString_throwsClassCastException` | `42.asString()` → `ClassCastException` |
| `booleanValue_asString_throwsClassCastException` | `true.asString()` → `ClassCastException` |
| `doubleValue_asString_throwsClassCastException` | `3.14.asString()` → `ClassCastException` |

---

### 2 · `root.hasNode(String)` and `node.hasField(String)`

> `root.hasNode("missing")` returns `false`; `root.hasNode("present")` returns `true`
> `root.hasNode` + `root.node` replaces all try/catch-on-NoSuchElement patterns

**Implementation:**

```java
// root.java
public boolean hasNode(String key) {
    return nodes.containsKey(key);
}

// node.java
public boolean hasField(String field) {
    return value instanceof object obj && obj.has(field);
}
```

`root.node(String)` still throws `NoSuchElementException` when absent — the predicate
is the guard, not a replacement.
`node.hasField` returns `false` (never throws) when the node's value is not an object.

**Tests added** (`HasNodeTest`):

| Test | Asserts |
|------|---------|
| `hasNode_returnsTrueForPresentKey` | object node found |
| `hasNode_returnsTrueForPrimitiveNode` | scalar node found |
| `hasNode_returnsFalseForMissingKey` | absent key → `false` |
| `hasNode_doesNotThrowForMissingKey` | no exception on absent key |
| `hasNode_falseEnablesConditionalNodeAccess` | guard-then-access pattern works |
| `hasField_returnsTrueForPresentField` | known fields found in object |
| `hasField_returnsFalseForAbsentField` | absent field → `false` |
| `hasField_returnsFalseWhenNodeValueIsNotObject` | scalar node → `false`, no throw |
| `hasField_doesNotThrowForAnyNodeType` | tuple / array / bool nodes — never throws |

---

### 3 · Typed Convenience Getters

> `object.getString("key")` throws `NoSuchElementException` (not NPE) when key is absent
> `object.getString("key")` throws `ClassCastException` (not NPE) when value is wrong type
> `object.getString("key", "default")` returns `"default"` when key is absent or null — never throws

**API added to `object`:**

```java
// Throwing overloads
String  getString(String field);
long    getLong(String field);
int     getInt(String field);
double  getDouble(String field);
boolean getBoolean(String field);
object  getObject(String field);
array   getArray(String field);

// Default-value overloads — never throw
String  getString(String field,  String  def);
long    getLong(String field,    long    def);
int     getInt(String field,     int     def);
double  getDouble(String field,  double  def);
boolean getBoolean(String field, boolean def);
```

Internal helper `requireField(String)` throws `NoSuchElementException` (not NPE) on
absent keys and delegates to the typed `asXxx()` accessor on the value, which throws
`ClassCastException` on mismatch.
Default-value overloads catch `ClassCastException` and return `def` — callers that
know a field may be absent or variable type never need a try/catch.

**API added to `node` (delegation to `object`):**

```java
String  getString(String field);
long    getLong(String field);
int     getInt(String field);
double  getDouble(String field);
boolean getBoolean(String field);
```

Each method calls the private `requireObject()` guard — throws
`UnsupportedOperationException` with an informative message when the node's value
is not an `object`, consistent with the existing `node.get(String field)` contract.

**Tests added** (`TypedGettersTest` — 26 tests):
Covers all throwing overloads (present → correct value, absent → `NoSuchElementException`,
wrong type → `ClassCastException`), all default-value overloads (absent → default,
present → actual, wrong-type → default, never throws), and node delegation
(correct values + non-object guard).

---

### 4 · `value` Type-Check Predicates (bonus)

> `value.isLong()`, `value.isString()`, etc. enable branch-without-catch patterns

All `default false` on the sealed interface; each concrete type overrides its own:

| Type | Returns `true` for |
|------|--------------------|
| `value.StringValue` | `isString()` |
| `value.LongValue` | `isLong()` |
| `value.DoubleValue` | `isDouble()` |
| `value.BooleanValue` | `isBoolean()` |
| `value.NullValue` | `isNull()` |
| `object` | `isObject()` |
| `array` | `isArray()` |
| `tuple` | `isTuple()` |

Usage pattern (no exception handling required):

```java
value v = obj.get("count");
int count = v.isLong() ? v.asInt() : 0;
```

**Tests added** (`ValueTypePredicatesTest` — 22 tests):
Per-type positives, cross-type negatives, full exclusivity suites for string and object,
and a branch-without-catch usage pattern test.

---

### 5 · Canonical `.anvl` Extension (bonus)

All test resource files under `src/test/resources/` renamed from `.aml` to `.anvl`.
`FileTest.java`, `ContextTest.java`, and `ModSpeedTest.java` updated accordingly.
Shebangs inside files (`#!aml`, `#!amp`, `#!asl`) are **unchanged** — they are
dialect markers, not file extension declarations.

---

## Files Changed

| File | Change |
|------|--------|
| `src/main/java/.../data/value.java` | Type-check predicate defaults + per-record overrides |
| `src/main/java/.../data/object.java` | Typed getters (throwing + default-value); `isObject()` |
| `src/main/java/.../data/array.java` | `isArray()` override |
| `src/main/java/.../data/tuple.java` | `isTuple()` override |
| `src/main/java/.../api/root.java` | `hasNode(String)` |
| `src/main/java/.../api/node.java` | `hasField(String)`; typed getter delegation; `requireObject()` |
| `src/main/java/.../utilities/AnvilConverters.java` | `BareLiteral → StringValue` mapping |
| `src/test/resources/**/*.aml` | Renamed to `*.anvl` (22 files) |
| `src/test/java/.../api/ContextTest.java` | Updated path to `attributes.anvl` |
| `src/test/java/.../parser/FileTest.java` | Updated all `.aml` references to `.anvl` |
| `src/test/java/.../parser/ModSpeedTest.java` | Updated `.aml` filter to `.anvl` |
| `src/test/java/.../api/ValueStringContractTest.java` | **New** — 8 tests |
| `src/test/java/.../api/HasNodeTest.java` | **New** — 9 tests |
| `src/test/java/.../api/TypedGettersTest.java` | **New** — 26 tests |
| `src/test/java/.../api/ValueTypePredicatesTest.java` | **New** — 22 tests |
| `build.gradle.kts` | `version = "0.1.8"` |

---

## No Breaking Changes

All changes are **additive**. Every method present in 0.1.7 has the same signature
and contract. Callers targeting 0.1.7 compile and run unchanged against 0.1.8.

---

## What 0.1.9 Addresses

The `Optional<node> findNode(String)` overload and `root`-level shorthand getters
(`root.getString(nodeKey, fieldKey)`) were explicitly deferred — `Optional` requires
a design review against the "no Optional" aesthetic of the public API. See the
roadmap for 0.1.9 scope.

