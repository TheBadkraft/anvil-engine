# Design Inquiry — Node-Level `@[...]` Attributes on Primitive Values

**Project:** `minecraft.ic` / `anvil-engine`
**Affected version:** `anvil-engine 0.1.7`
**Date:** May 16, 2026
**Author:** The Badkraft

---

## Background

While implementing recipe deletion in `minecraft.ic`, a pattern was attempted where
individual ANVL nodes were annotated with a valueless (tag-form) attribute to signal
that they describe a deletion rather than an addition:

```anvl
wooden_pickaxe @[deleted] := "minecraft:wooden_pickaxe"
stone_axe      @[deleted] := "minecraft:stone_axe"
```

The intent was to detect these nodes in Java using `node.hasAttribute("deleted")`.
The feature never worked. Tracing the failure to its root exposes three layered
problems in the engine: a silent parser crash, an exception swallowed by the registry,
and a secondary API naming confusion on `node`.

---

## Problem 1 — Parser Throws `UnsupportedOperationException` on Primitive Nodes with Attributes

### Constraint in `Value.getAttributes()`

`Value.java` defines `getAttributes()` only for composite types:

```java
// Value.java:35-43
default Attributes getAttributes() {
    return switch (this) {
        case ArrayValue  a -> new Attributes(a.attributes);
        case TupleValue  t -> new Attributes(t.attributes);
        case ObjectValue o -> new Attributes(o.attributes);
        default -> throw new UnsupportedOperationException(
                "Attributes not supported on " + getClass().getSimpleName());
    };
}
```

`StringValue`, `LongValue`, `BooleanValue`, `DoubleValue`, `NullValue`, `BareLiteral`,
`HexValue`, and `BlobValue` all hit `default` and throw.

### Where the Parser Hits It

`AnvilParser.parseStatement()` parses the node-level `@[...]` block *before* the value,
then — after the value is parsed — pushes the attrs into the value's attribute list:

```java
// AnvilParser.java:80-97
List<Attribute> attrs = parseAttributeBlock();   // parses @[deleted]
// ... consume := ...
Value value = parseValue(base);                  // parses "minecraft:wooden_pickaxe"
                                                 //   → StringValue
if (!attrs.isEmpty()) {
    value.getAttributes().addAll(attrs);         // ← THROWS UnsupportedOperationException
}                                                //   Assignment is NEVER constructed
// ...
Assignment assignment = new Assignment(key, attrs, value);   // never reached
```

The `UnsupportedOperationException` is thrown before the `Assignment` record is created.
It propagates through `parseStatement()` → `parseSource()` → into the caller.

### Silent Skip in `ContentRegistry`

`ContentRegistry.loadFile()` wraps the full parse in a broad exception handler:

```java
} catch (Exception e) {
    // ANVL runtime exceptions (parser errors, UnsupportedOperationException, etc.)
    System.err.println("[IC] ContentRegistry: skipped " + path.getFileName()
            + " (parse error: " + e.getClass().getSimpleName()
            + " — " + e.getMessage() + ")");
}
```

Any `UnsupportedOperationException` thrown during parsing causes the **entire file to
be discarded** — no descriptors are added for any node in the file, including nodes that
would have parsed successfully. The log line is the only evidence, and it is easy to
miss among startup output.

### Consequence for `minecraft.ic`

`deletions.anvl` was never loaded. Every launch silently printed:

```
[IC] ContentRegistry: skipped deletions.anvl
    (parse error: UnsupportedOperationException — Attributes not supported on StringValue)
```

No deletion override files were ever generated. All vanilla stone and wooden tool
recipes remained in the game throughout development.

### Recommended Fix

Add an explicit error code and check in `parseStatement()` after `parseValue()`:

```java
// in AnvilParser.parseStatement(), after "Value value = parseValue(base);"
if (!attrs.isEmpty() && !(value instanceof ObjectValue)
                     && !(value instanceof ArrayValue)
                     && !(value instanceof TupleValue)) {
    raise(SCALAR_VALUE_CANNOT_HAVE_ATTRIBUTES);
}
```

This converts the silent `UnsupportedOperationException` into a named `ParseException`
with a line/column, giving the author an actionable error message rather than a
mystery skip.

Add `SCALAR_VALUE_CANNOT_HAVE_ATTRIBUTES` to `ErrorCode` with a description such as:
> `"scalar values (string, number, boolean, null, blob) cannot have @[...] attributes"`

---

## Problem 2 — `node.hasAttribute()` Checks the Value's Namespace, Not the Node's

Even if the parser did not throw, the public API for querying node-level annotations
would mislead callers. `node.java` has two distinct attribute surfaces:

```java
// Surface A — the node's OWN @[...] annotations (stored in node.attributes map)
public List<attribute> attributes() {
    return List.copyOf(attributes.values());
}

// Surface B — hasAttribute dispatches to the VALUE's attribute namespace
public boolean hasAttribute(String key) {
    return switch (value) {
        case object obj -> obj.hasAttribute(key);   // the VALUE's @[...]
        case array arr  -> arr.hasAttribute(key);
        case tuple tup  -> tup.hasAttribute(key);
        case blob b     -> b.hasTag();
        case null, default -> throw new NoSuchElementException("No attribute: @" + key);
    };
}
```

For `flint_shovel @[type="crafting_shaped"] := { ... }` — where the value is an object
— the node-level `@[type=...]` gets merged into the object's attribute list (by the
parser's `value.getAttributes().addAll(attrs)` call). So `node.hasAttribute("type")`
happens to work, because the two surfaces coincide.

For any other composite value type this is confusing but coincidentally functional.
For primitive-valued nodes it crashes before the question is even asked.

### Recommended Fix

Add a `node.hasOwnAttribute(String key)` predicate that checks only the node's own
annotation map, independent of what the value holds:

```java
/** Returns {@code true} iff this node carries the given {@code @[key]} annotation. */
public boolean hasOwnAttribute(String key) {
    return attributes.containsKey(key);
}
```

Document `hasAttribute(key)` explicitly as querying the *value's* attribute namespace,
which is the correct semantic for cases like `@[deprecated]` on an object value.

---

## Problem 3 — Attribution vs. Attributes

"Attribution" in source-processing tools means tracking the provenance of a parsed
value back to its origin in the source text — line number, column, start offset, end
offset. In the ANVL engine this is handled by `Source.java`:

```java
public int position() { return pos; }
public int line()     { return line; }
public int column()   { return col; }
```

`Source` records where the parser cursor is; it does **not** represent user-authored
metadata. Attribution in this sense is never stored on `Attribute` objects — `Attribute`
records user-supplied `@[key=value]` annotations.

The confusion matters because:

- A tag-form attribute (`@[deleted]`) stores `null` as its value (`new Attribute("deleted")`
  → `this(key, null)`).
- Any code that calls `attr.value()` on a tag attribute receives `null` and risks NPE
  if it tries to call `.asString()` etc. without a null-guard.
- The `Attribute.toString()` Javadoc shows the tag form as `@[code debug]` but the
  correct ANVL syntax is `@[deleted]` (bare identifier inside brackets). The Javadoc
  should be corrected and the null value should be explicitly documented as
  "tag attributes have no value; callers must check before accessing".

### Recommended Fix

In `Attribute.java`:

```java
/**
 * A single attribute attached to a statement.
 * <ul>
 *   <li>Tag form:   {@code @[deleted]}     — {@code value} is {@code null}</li>
 *   <li>K-V form:   {@code @[type=block]}  — {@code value} is non-null</li>
 * </ul>
 * A tag attribute records a boolean flag; its {@code value()} is always {@code null}.
 * Callers must check {@code isTag()} or null-guard before calling {@code value().asX()}.
 */
public record Attribute(@NotNull String key, @Nullable Value value) {
    /** Returns {@code true} iff this is a tag attribute with no associated value. */
    public boolean isTag() { return value == null; }
}
```

---

## Problem 4 — Using Tag Attributes as Structural Discriminators Is Wrong

Beyond the API incorrectness, the `@[deleted]` approach has a deeper semantic flaw:
the annotation was trying to encode **structural intent** (is this node a deletion or
an addition?) via a node-level tag. But intent is already expressed at the **file level**
through the root attribute `type="recipe_deletion"` vs `type="recipe"`.

Per-node `@[deleted]` tags are redundant metadata: every node in a `recipe_deletion`
file is by definition a deletion. The file type is the only discriminator needed.

Furthermore, a deletion list is not a map of named nodes — it is a collection of
resource IDs. A flat array is the natural ANVL representation:

```anvl
#!aml
@[version="1.0", type="recipe_deletion"]

recipes := [
    "minecraft:wooden_pickaxe",
    "minecraft:stone_axe",
    "minecraft:furnace"
]
```

The consuming Java code then dispatches on `r.attribute("type").value().asString()`:

```java
String rootType = r.hasAttribute("type")
    ? r.attribute("type").value().asString()
    : "recipe";

if ("recipe_deletion".equals(rootType)) {
    array ids = r.get("recipes").asArray();
    for (value v : ids.elements()) {
        generateRecipeDeletion(packRoot, v.asString());
    }
    return;
}
// else: process recipe-addition nodes normally
```

This is unambiguous, does not depend on node-level attributes at all, and correctly
uses the ANVL type system (array of strings).

---

## Summary of Required Changes

| # | Severity | Location | Change |
|---|----------|----------|--------|
| 1 | **Bug** | `AnvilParser.java` | After `parseValue()`, check value type and raise `SCALAR_VALUE_CANNOT_HAVE_ATTRIBUTES` if attrs non-empty and value is not object/array/tuple |
| 2 | **Bug** | `ErrorCode.java` | Add `SCALAR_VALUE_CANNOT_HAVE_ATTRIBUTES` with message `"scalar values (string, number, boolean, null, blob) cannot have @[...] attributes"` |
| 3 | **API** | `node.java` | Add `hasOwnAttribute(String key)` checking the node's own annotation map |
| 4 | **Docs** | `node.java` | Document that `hasAttribute()` queries the *value's* attribute namespace |
| 5 | **Docs** | `Attribute.java` | Document null value for tag form; fix `@{code` Javadoc typo; add `isTag()` predicate |

Items 1–2 are crash-prevention fixes. Item 3 is a new API. Items 4–5 are documentation.

---

## Acceptance Criteria

- [ ] Parsing `foo @[deleted] := "a_string"` raises `ParseException(SCALAR_VALUE_CANNOT_HAVE_ATTRIBUTES)`
      with correct line and column — instead of `UnsupportedOperationException`
- [ ] Parsing `foo @[type="x"] := { ... }` still succeeds (no regression)
- [ ] `node.hasOwnAttribute("type")` returns `true` for a node parsed from
      `foo @[type="crafting_shaped"] := { ... }`
- [ ] `node.hasAttribute("type")` on the same node still returns `true` (no regression;
      the parser merges node-level attrs into the object value's attribute list)
- [ ] `Attribute.isTag()` returns `true` iff `value` is `null`; `false` otherwise
- [ ] Calling `attr.isTag()` before `attr.value().asString()` is the documented pattern
      for tag-attribute code paths
- [ ] All new methods and the new error code covered by unit tests

