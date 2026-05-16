# Anvil Parser Core — Parity Analysis
## Java (`anvil-engine`) vs .Net (`anvil.net`)

**Date:** May 15, 2026
**Author:** The Badkraft
**Scope:** Parser grammar, Source interrogator, Keywords, Symbols/Operators, Error codes.
**Out of scope:** Public API surface, runtime types, serializer, query layer.

---

## Principle

> The APIs are intentionally different — each implementation wears the idioms of its
> host platform. The parser **core** is the invariant: the same grammar, the same
> keyword table, the same operator table, the same error taxonomy. A valid `.anvl`
> file must parse identically on both sides.

---

## 1. Parse Methods — Gap Table

| Feature | .Net method | Java status | Roadmap |
|---------|-------------|-------------|---------|
| Source, module-level loop | `ParseSource` | ✅ present | — |
| Assignment statement | `ParseStatement` | ✅ present | — |
| Object `{ }` | `ParseObject` | ✅ present | — |
| Array `[ ]` | `ParseArray` | ✅ present | — |
| Tuple `( )` | `ParseTuple` | ✅ present | — |
| Blob `` @tag`…` `` | `ParseBlob` | ✅ present | — |
| Scalar (string / number / bool / null / bare) | `ParseScalarValue` | ✅ present | — |
| Attribute block `@[ … ]` | `ParseAttributeBlock` | ✅ present | — |
| Identifier reader | `ReadIdentifier` | ✅ present | — |
| **`vars { }` block** | `ParseVarsBlock` | ❌ missing | **0.2.0** |
| **`$identifier` var-ref** | `ParseVarRef` | ❌ missing | **0.2.0** |
| **`$"Hello {name}!"` interpolation** | `ParseInterpolatedString` | ❌ missing | **0.2.0** |
| **`import "path" [as alias]`** | `ParseImportDecl` | ❌ missing | **0.3.0** |
| **`using ModuleName`** | (in `ParseFunctionDef` scope) | ❌ missing | **0.4.0** |
| **Function definition** | `ParseFunctionDef` | ❌ missing | **0.4.0** |
| **Anonymous inline object** | `ParseAnonObject` | ❌ missing | **0.3.0** |

---

## 2. Keywords — Gap Table

| Keyword | .Net | Java | Dialect |
|---------|------|------|---------|
| `vars` | ✅ | ❌ | AML |
| `import` | ✅ | ❌ | AML |
| `if` | ✅ | ❌ | ASL |
| `else` | ✅ | ❌ | ASL |
| `return` | ✅ | ❌ | ASL |
| `using` | ✅ | ❌ | ASL |
| `for` | ✅ | ❌ | ASL |
| `break` | ✅ | ❌ | ASL |
| `continue` | ✅ | ❌ | ASL |
| `true` / `false` / `null` | ✅ | ✅ | AML |

**Note:** `fn`, `let`, `mut`, `export` are **not** reserved in either implementation.
`as` is contextual (import alias only) — valid identifier everywhere else.

---

## 3. Symbols / Operators — Gap Table

| Symbol | Literal | .Net | Java | Needed for |
|--------|---------|------|------|------------|
| `At` | `@` | ✅ | ✅ | attributes, blobs |
| `` Backtick `` | `` ` `` | ✅ | ✅ | blobs |
| `Colon` | `:` | ✅ | ✅ | inheritance |
| `Comma` | `,` | ✅ | ✅ | all collections |
| `LBrace` / `RBrace` | `{` `}` | ✅ | ✅ | objects |
| `LBracket` / `RBracket` | `[` `]` | ✅ | ✅ | arrays |
| `LParen` / `RParen` | `(` `)` | ✅ | ✅ | tuples |
| `Quote` | `"` | ✅ | ✅ | strings |
| `Assign` | `:=` | ✅ | ✅ | assignment |
| `Equal` | `=` | ✅ | ✅ | attribute values |
| **`SQuote`** | `'` | ✅ | ❌ | single-quoted strings |
| **`DotDot`** | `..` | ✅ | ❌ | future (range?) |
| **`Arrow`** | `=>` | ✅ | ❌ | ASL (fat arrow) |
| **`Eq`** | `==` | ✅ | ❌ | ASL comparison |
| **`NotEq`** | `!=` | ✅ | ❌ | ASL comparison |
| **`LtEq`** / **`GtEq`** | `<=` `>=` | ✅ | ❌ | ASL comparison |
| **`Lt`** / **`Gt`** | `<` `>` | ✅ | ❌ | ASL comparison |
| **`Inc`** / **`Dec`** | `++` `--` | ✅ | ❌ | ASL mutation |
| **`Bang`** | `!` | ✅ | ❌ | ASL negation |
| **`Plus`** / **`Minus`** | `+` `-` | ✅ | ❌ | ASL arithmetic |
| **`Star`** / **`Slash`** / **`Percent`** | `*` `/` `%` | ✅ | ❌ | ASL arithmetic |

> **AML-only deliverable (before ASL):** `SQuote` for single-quoted strings.
> Everything else in the gap is ASL-only — defer until 0.4.0.

---

## 4. Error Codes — Gap Table

Only AML-relevant gaps listed. ASL errors deferred to 0.4.0.

| Error | .Net | Java | Milestone |
|-------|------|------|-----------|
| `EmptyAttributeBlock` | ✅ | ❌ | now / 0.1.9 |
| `AttributesNotAllowedOnType` | ✅ | ❌ | now / 0.1.9 |
| `RocketOpNotValid` | ✅ | ❌ | 0.1.9 |
| `ArrayCannotBeEmpty` | ✅ | ❌ | (Java uses `EMPTY_OBJECT_NOT_ALLOWED` but allows empty arrays — check) |
| `TrailingCommaInObject` | ✅ | ❌ | 0.1.9 |
| `AssignmentNotAllowedHere` | ✅ | ❌ | 0.1.9 |
| `VarsBlockAlreadyDefined` | ✅ | ❌ | **0.2.0** |
| `VarsBlockNotFirst` | ✅ | ❌ | **0.2.0** |
| `DuplicateVarsKey` | ✅ | ❌ | **0.2.0** |
| `ImportNotAllowedInMemory` | ✅ | ❌ | **0.3.0** |
| `ImportAmpForbidden` | ✅ | ❌ | **0.3.0** |
| `ImportNotFirst` | ✅ | ❌ | **0.3.0** |
| `ImportFileNotFound` | ✅ | ❌ | **0.3.0** |
| `ImportDuplicateAlias` | ✅ | ❌ | **0.3.0** |
| `ImportCyclicDependency` | ✅ | ❌ | **0.3.0** |
| `ImportNamespaceCollision` | ✅ | ❌ | **0.3.0** |

---

## 5. Source Interrogator — Status

Both implementations share the same zero-copy `Source` design
(char-array + position/line/col, no string allocations on hot path).
Java's `Source.java` (210 lines) vs .Net's `Source.cs` (304 lines) — the
gap is mostly the extra interrogator helpers .Net needs for ASL operators.

No AML-blocking gaps in `Source`. Additions needed for ASL:

- `.MatchLength(string)` / multi-char lookahead (Java only has `is(String)`)
- Single-quote detection (for `SQuote`)
- ASL operator scanning (all deferred to 0.4.0)

---

## 6. Migration Priority

### 0.1.9 — Parser Hardening (no new grammar)
- Add missing error codes to `ErrorCode.java`: `TrailingCommaInObject`, `EmptyAttributeBlock`,
  `AttributesNotAllowedOnType`, `RocketOpNotValid`, `AssignmentNotAllowedHere`
- Harden existing parse paths to raise them where .Net does
- `SQuote` support — single-quoted strings (`'hello'` == `"hello"`)

### 0.2.0 — `vars` Block & Variable References (per roadmap)
- `ParseVarsBlock` — flat module-scope variable declarations
- `ParseVarRef` — `$key` references; fail-soft (unresolved → literal `$key`)
- `ParseInterpolatedString` — `$"Hello, {name}!"` fail-soft
- Keywords: `vars`
- Error codes: `VarsBlockAlreadyDefined`, `VarsBlockNotFirst`, `DuplicateVarsKey`
- `needsResolution` flag on values/statements

### 0.3.0 — Path Resolution, Namespaces & Import
- `ParseImportDecl` — `import "path" [as alias]`
- `ParseAnonObject` — inline anonymous objects
- Cycle detection, namespace collision detection
- All import error codes

### 0.4.0 — ASL Dialect (AnvilScript)
- All ASL keywords: `if`, `else`, `return`, `using`, `for`, `break`, `continue`
- All ASL symbols: `=>`, `==`, `!=`, `<=`, `>=`, `<`, `>`, `++`, `--`, `!`, `+`, `-`, `*`, `/`, `%`
- `ParseFunctionDef`, control-flow parsing
- ASL evaluator, execution context, scope — port from `Anvil.Net/src/Asl*.cs`

---

## 7. What This Is NOT

The following are **intentionally different** between .Net and Java and will NOT be aligned:

- Public API types and method names (`AnvlDoc` vs `root`, `AnvilNode` vs `node`)
- Collection/container interfaces (`IAnvilModule` vs `IObject`/`IContainer`)
- Error delivery mechanism (Java throws `ParseException`; .Net uses a thread-local `AnvilError` state)
- Builder pattern (Java uses `Context.builder()`; .Net uses `AnvilContext` constructors)
- Serializer / code-gen (Java will have its own design in 0.1.9+)
- Query layer (`Anvil.Net.Query` ↔ Java fluent API — different paradigms, same goal)

