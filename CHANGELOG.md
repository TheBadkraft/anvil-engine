# Changelog

All notable changes to **anvil-engine** are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versioning follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.1.8] — 2026-05-15

### Added

- **`root.hasNode(String)`** — existence predicate symmetric with `root.hasAttribute`.
  Enables guard-then-access patterns without try/catch on `NoSuchElementException`.
- **`node.hasField(String)`** — delegates to `object.has(field)`; returns `false` (never
  throws) when the node's value is not an `object`.
- **Typed throwing getters on `object`** — `getString`, `getLong`, `getInt`, `getDouble`,
  `getBoolean`, `getObject`, `getArray`. Each throws `NoSuchElementException` on absent
  key (never NPE) and `ClassCastException` on type mismatch.
- **Default-value overloads on `object`** — `getString(field, def)`, `getLong(field, def)`,
  `getInt(field, def)`, `getDouble(field, def)`, `getBoolean(field, def)`. Never throw;
  return the supplied default when the key is absent, null, or the wrong type.
- **Typed getters on `node`** — `getString`, `getLong`, `getInt`, `getDouble`, `getBoolean`
  delegate to the wrapped `object`; throw `UnsupportedOperationException` (with informative
  message) when the node's value is not an `object`.
- **`value` type-check predicates** — `isString()`, `isLong()`, `isDouble()`, `isBoolean()`,
  `isNull()`, `isObject()`, `isArray()`, `isTuple()`. All `default false` on the sealed
  interface; each concrete type overrides its own. Enables branch-without-catch patterns.
- **`BareLiteral → StringValue` conversion** in `AnvilConverters.toValue()`. Unquoted
  enum-style tokens (`type := sword`, `target := realmsNotificationsScreen`) now resolve
  correctly via `asString()`.
- **64 new tests** across four new test classes:
  - `ValueStringContractTest` — quoted/bare string contract, type-mismatch guards
  - `HasNodeTest` — `root.hasNode`, `node.hasField`, all edge cases
  - `TypedGettersTest` — throwing/default-value overloads, node delegation, non-object guard
  - `ValueTypePredicatesTest` — per-type positives, cross-type negatives, exclusivity suites

### Changed

- All test resource files renamed from `.aml` → `.anvl` (canonical extension).
  Shebangs inside files (`#!aml`, `#!amp`, `#!asl`) are unchanged.
- `FileTest.java`, `ContextTest.java`, `ModSpeedTest.java` updated to use `.anvl` paths.

### Fixed

- `AnvilConverters.toValue()` previously threw `IllegalArgumentException` for
  `BareLiteral` values (bare-word identifiers used as values, e.g. `type := sword`).
  Now correctly maps to `value.StringValue`.

### No Breaking Changes

All 0.1.7 public API signatures and contracts are preserved. Full backward compatibility.

---

## [0.1.7] — 2026-01-31

### Added

- **Fluent Runtime Layer** — lowercase native public types: `object`, `array`, `tuple`,
  `blob`, `attribute`, `node`, `root`.
- **Core interfaces** — `IObject`, `IContainer`, `IAttributed`.
- **`MutableObject` / `MutableArray`** builder path → `.build()` → frozen immutable value.
- **Full convenience getters** on all containers (`getString`, `getLong`, `getObject`, etc.).
- **`Anvil.read(String)` / `Anvil.load(Path)`** entry-point API.
- **Inheritance resolution** — `node : base_id := { … }` syntax; `root.resolveBase(id)`.
- **`IResolver` interface** + default `Resolver` implementation.
- **`blob` type** — raw text with type tag (`@md\`…\``).
- **Hex literal support** — `#RRGGBB` and `0xNN` parsed as `LongValue`.
- **Numeric coercion** — `LongValue.asInt()`, `asShort()`, `asByte()` (range-checked);
  `DoubleValue.asFloat()` (precision-checked).
- **Sealed `value` interface** with `asLong`, `asDouble`, `asString`, `asBoolean`,
  `asInt`, `asShort`, `asByte`, `asFloat`, `asObject`, `asArray`, `asTuple`, `asBlob`.

### Changed

- `AnvilRoot` → `root`; `AnvilNode` → `node` — all public types are now lowercase.
- Package restructured into `api`, `data`, `core`, `utilities`, `validators`.

---

## [0.1.6] — 2025-12-20

### Added

- Zero-copy parser — identifiers and values backed by `Source` spans; no `String`
  allocations on the hot path.
- Sealed immutable AST (`Value` hierarchy in `core.data`).
- `AnvilRoot` top-level entry point.
- `Source` interrogator — `peek`, `consume`, `isIdentifierStart/Part`, `isDigit`,
  `skipWhitespace`, `isEOF`.
- Full shebang dialect detection (`#!aml`, `#!amp`, `#!asl`).
- Parser support for: assignments, objects, arrays, tuples, strings, booleans,
  nulls, bare literals, hex literals, blobs, dotted keys, inheritance, attributes.

---

## [0.1.5] and earlier

Initial development iterations — not formally versioned.

---

[0.1.8]: https://github.com/TheBadkraft/anvil-engine/compare/Alpha-v0.1.7...Alpha-v0.1.8
[0.1.7]: https://github.com/TheBadkraft/anvil-engine/compare/Alpha-v0.1.6...Alpha-v0.1.7
[0.1.6]: https://github.com/TheBadkraft/anvil-engine/releases/tag/Alpha-v0.1.6

