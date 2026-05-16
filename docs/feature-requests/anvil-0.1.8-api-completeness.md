# Feature Request — Anvil Engine 0.1.8: API Completeness

**Project:** `minecraft.ic`
**Requesting module:** `minecraft-ic-launcher` / Phase 2 Hook Engine
**Target version:** `anvil-engine 0.1.8`
**Date:** May 15, 2026
**Author:** The Badkraft

---

## Background

`minecraft.ic` uses `anvil-engine` as its sole data layer. Starting with Phase 2 (the Hook Engine),
all runtime configuration — screen hooks, button removals, event bindings — will be read from
`.anvl` files at startup via the Java Instrumentation agent. The hook registry file looks like this:

```anvl
hooks {
    titleScreen {
        onInit [
            { action = removeWidget  target = "menu.online" }
            { action = disableOverlay  target = realmsNotificationsScreen }
        ]
    }
}
```

The call path in Phase 2 will be:

```java
root r = Anvil.load(hooksPath);
if (r.hasNode("hooks")) {                          // ← MISSING
    node hooks   = r.node("hooks");
    node ts      = hooks.get("titleScreen").asObject()...
    array onInit = hooks.getString("onInit")...    // ← MISSING
    for (value entry : onInit.elements()) {
        String action = entry.asObject().getString("action");   // ← MISSING
        String target = entry.asObject().getString("target");   // ← MISSING
    }
}
```

The three gaps that block Phase 2 are documented below as discrete requests.

---

## Request 1 — `asString()` Contract Formalisation

### Problem

`value.StringValue` stores whatever the parser hands it. For a **quoted** ANVL string:

```anvl
target = "menu.online"
```

it is unclear (and currently untested by contract) whether `asString()` returns:

- `menu.online`  ← correct — content only
- `"menu.online"` ← wrong — includes surrounding quotes

For an **unquoted** bare string:

```anvl
target = realmsNotificationsScreen
```

`asString()` returns `realmsNotificationsScreen` — correct, and consistent.

The issue was first observed in practice: unquoted strings worked; quoted strings had not been
exercised end-to-end in `minecraft.ic` code yet, and the caller would have to defensively strip
quotes (`s.replace("\"", "")`) if the contract wasn't enforced.

### Requested Behaviour

| Input in ANVL file | `asString()` returns | Notes |
|--------------------|----------------------|-------|
| `key = "hello world"` | `hello world` | Quotes stripped by parser |
| `key = bareword` | `bareword` | Unchanged |
| `key = 42` | throws `ClassCastException` | No silent coercion |
| `key = true` | throws `ClassCastException` | No silent coercion |
| `key = null` | throws `ClassCastException` | No silent coercion |
| `key = { ... }` | throws `ClassCastException` | No silent coercion |

The contract must be **enforced in the parser**, not papered over in callers.

### Where to Fix

`AnvilParser.java` — wherever `StringValue` is constructed, the raw token passed in must have
its surrounding `"..."` or `'...'` delimiters stripped before being stored.

`value.StringValue` record itself needs a canonical note in its Javadoc that the stored `value`
is always the **contents** of the string, never the delimiters.

### Test Coverage Required

Golden-file tests for:
- Quoted string round-trip: parse `key = "hello"` → `asString()` == `"hello"` (no quotes)
- Single-quoted variant (if supported)
- Mixed: object with both quoted and unquoted fields — both `asString()` calls correct
- Type-mismatch: `42.asString()` → `ClassCastException`

---

## Request 2 — `root.hasNode(String)` (and `node.hasField(String)`)

### Problem

`root.node(String key)` throws `NoSuchElementException` when the key is absent. There is no
way to check existence without catching that exception:

```java
// Current — forces try/catch or unconditional access
node hooks = root.node("hooks");  // blows up if not present

// Desired
if (root.hasNode("hooks")) {
    node hooks = root.node("hooks");
    ...
}
```

`root.hasAttribute(String)` already exists and works correctly. `hasNode` is the symmetric pair.

Similarly, `node.get(String field)` delegates to `object.get(field)`, which returns **`null`**
for a missing field (silently). That's an inconsistency: `root.node()` throws, `object.get()`
returns null. Both should have a defensive predicate.

### Requested API

```java
// root.java
public boolean hasNode(String key);          // true iff nodes map contains key

// node.java
public boolean hasField(String field);       // delegates to object.has(field) — already exists on object

// object.java — already has has(String field), just needs surfacing on node
```

`object.has(String)` already exists in 0.1.7. `node.hasField` is a one-line passthrough.
`root.hasNode` is a one-line `nodes.containsKey(key)`.

### Notes

- `root.hasAttribute` sets the pattern — `hasNode` follows the same convention.
- Do **not** change `root.node()` to return `null` — the throw-on-missing contract is correct
  and valuable. The goal is to give callers the predicate so they never need to catch.
- Consider an `Optional<node> findNode(String key)` overload in the same pass for callers that
  prefer Optional chaining, but the boolean predicate is the minimum requirement.

---

## Request 3 — Typed Convenience Getters

### Problem

Reading a named field from an `object` currently requires two steps:

```java
// Current — verbose, two method calls, unchecked cast on value
String action = entry.asObject().get("action").asString();
int    count  = entry.asObject().get("count").asInt();
```

For config-heavy code (Phase 2 will call this hundreds of times per startup), this is noisy and
error-prone. A missing field returns `null` from `object.get()`, so the `asString()` call then
throws `NullPointerException` rather than a meaningful error.

### Requested API — `object`

```java
// Throwing overloads — NoSuchElementException if key absent, ClassCastException on type mismatch
String  getString(String key);
long    getLong(String key);
int     getInt(String key);
double  getDouble(String key);
boolean getBoolean(String key);
object  getObject(String key);
array   getArray(String key);

// Default-value overloads — never throw
String  getString(String key, String  defaultValue);
long    getLong(String key,   long    defaultValue);
int     getInt(String key,    int     defaultValue);
double  getDouble(String key, double  defaultValue);
boolean getBoolean(String key, boolean defaultValue);
```

### Requested API — `node`

`node` wraps a `value` that is often an `object`. Surface the same getters when the inner
value is an `object`, delegating to `object.getString(...)` etc.:

```java
String  getString(String key);
long    getLong(String key);
int     getInt(String key);
// ... same set as object
```

Throws `UnsupportedOperationException` if the node's value is not an `object` (same pattern
as the existing `node.get(String field)`).

### Requested API — `root`

`root` holds named nodes. A shorthand for the common case (node whose value is an object with
a single typed field) saves one `.node()` call:

```java
// root-level convenience — equivalent to root.node(nodeKey).getString(fieldKey)
String  getString(String nodeKey, String fieldKey);
// ... only if the team feels this is worth the surface area
```

This is lower priority than the `object` getters — if it adds complexity, skip it.

### Type-Check Predicates on `value` (Nice-to-Have)

While implementing the getters, add type-check predicates to the `value` sealed interface:

```java
// value.java — default impls all return false; each record overrides its own
default boolean isString()  { return false; }
default boolean isLong()    { return false; }
default boolean isInt()     { return false; }  // same as isLong for now; int is a narrowing
default boolean isDouble()  { return false; }
default boolean isBoolean() { return false; }
default boolean isNull()    { return false; }
default boolean isObject()  { return false; }
default boolean isArray()   { return false; }
default boolean isTuple()   { return false; }
```

These allow callers to branch without try/catch:

```java
value v = obj.get("count");
int count = v.isLong() ? v.asInt() : 0;
```

---

## Priority Order

| # | Item | Complexity | Blocks Phase 2 |
|---|------|------------|----------------|
| 1 | `asString()` contract (parser fix) | Low | Yes — correctness bug |
| 2 | `root.hasNode(String)` | Trivial | Yes — defensive check at entry point |
| 3 | `object` typed getters (throwing) | Low | Yes — used in every hook entry |
| 4 | `object` typed getters (default overloads) | Low | No — but eliminates caller noise |
| 5 | `node` typed getter delegation | Low | No — sugar |
| 6 | `value` type-check predicates | Low | No — sugar |
| 7 | `root` shorthand getters | Medium | No — debatable surface area |

Items 1–3 are the minimum bar for `minecraft.ic` Phase 2 to begin.

---

## API Summary (Diff View)

### `root.java`
```java
// ADD
public boolean hasNode(String key) {
    return nodes.containsKey(key);
}
```

### `node.java`
```java
// ADD
public boolean hasField(String field) {
    return value instanceof object obj && obj.has(field);
}
public String  getString(String field) { return get(field).asString(); }
public long    getLong(String field)   { return get(field).asLong(); }
public int     getInt(String field)    { return get(field).asInt(); }
public double  getDouble(String field) { return get(field).asDouble(); }
public boolean getBoolean(String field){ return get(field).asBoolean(); }
```

### `object.java`
```java
// ADD
public String  getString(String field) {
    value v = get(field);
    if (v == null) throw new NoSuchElementException("No field: " + field);
    return v.asString();
}
public String  getString(String field, String def) {
    value v = get(field);
    return (v == null || v instanceof value.NullValue) ? def : v.asString();
}
// ... getLong, getInt, getDouble, getBoolean — same pattern
```

### `value.java` (parser contract)
```java
// StringValue.asString() must return content only — no surrounding quotes.
// Enforce in AnvilParser where StringValue is constructed.
record StringValue(String value) implements value {
    /** @param value the string content — delimiters must be stripped before construction */
    @Override public String asString() { return value; }
}
```

---

## Acceptance Criteria

- [ ] `Anvil.load(path).root.node("key").value().asString()` on `key = "quoted"` returns `quoted` (no quotes)
- [ ] `root.hasNode("missing")` returns `false`; `root.hasNode("present")` returns `true`
- [ ] `root.hasNode` + `root.node` replaces all try/catch-on-NoSuchElement patterns in callers
- [ ] `object.getString("key")` throws `NoSuchElementException` (not NPE) when key is absent
- [ ] `object.getString("key")` throws `ClassCastException` (not NPE) when value is wrong type
- [ ] `object.getString("key", "default")` returns `"default"` when key is absent or null — never throws
- [ ] All new methods covered by unit tests and at least one golden-file integration test
- [ ] No breaking changes to existing 0.1.7 API

