# Anvil Formal Roadmap  
**2025–2026** — The next 12 months of domination

| Version       | Target Release | Status       | Purpose & Deliverables                                                                                                                     | One-Liner Roast                                                                                          |
|---------------|----------------|--------------|--------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| **0.1.6**     | Dec 2025       | Released     | Current stable alpha – zero-copy parser, sealed immutable AST, `AnvilRoot` entry point                                                   | “JSON just realized it’s been parsing whitespace wrong for 25 years.”                                   |
| **0.1.7**     | Jan–Feb 2026   | In Progress  | **The Fluent Runtime Layer**<br>• Lowercase native types: `object`, `array`, `tuple`, `blob`<br>• Core interfaces: `IObject`, `ICollection`, `IContainer`, `IAttributed`<br>• `MutableObject`, `MutableArray`, `ImmutableTuple`<br>• `.build()` → frozen AST path<br>• Full convenience getters on all containers (`getString`, `getLong`, `getObject`, etc.) | “Your config library still uses `Map<String, Object>`? Cute.”                                            |
| **0.1.8**     | Mar 2026       | Planned      | **Absolute Zero-Copy**<br>• Identifiers and attribute keys become `int start/end` only – **zero** `String` allocations during parse<br>• `SourceView` utilities for on-demand materialization<br>• Final measurable allocation count: **0** on hot path | “We just made the JVM forget `String.substring()` exists.”                                               |
| **0.1.9**     | Apr 2026       | Planned      | **Resolver Framework**<br>• `IResolver` interface + default implementation<br>• `Context.builder().resolver(...)`<br>• `Anvil.load(path).withResolver(...)`<br>• Inheritance topology builder (DAG, cycle detection)<br>• Base/derived resolution (runtime merge) | “Inheritance that actually works without copying the entire document. YAML is crying in a corner.”       |
| **0.2.0**     | May–Jun 2026   | Planned      | **vars Block & Variable References**<br>• `vars { key = value }` (flat module scope)<br>• `$key` → var-ref, fail-soft (unresolved → literal `$key`)<br>• `$"Hello, {name}!"` interpolation, fail-soft<br>• `needsResolution` flag on statements/values | “Your config still hard-codes player names? We literally invented variables.”                           |
| **0.2.1**     | Jul 2026       | Planned      | **AnvilWriter & Round-Tripping**<br>• `AnvilWriter` with `AnvilWriterOptions`<br>• Preserves comments, whitespace, attribute order, original formatting<br>• Mutable → write → parse → identical source (golden master) | “We can now hot-reload your config without turning it into unreadable garbage. Unlike literally everyone else.” |
| **0.3.0**     | Sep–Oct 2026   | Planned      | **Path-Based Resolution & Namespaces**<br>• `$player.health`, `$blocks.stone.hardness`<br>• Multi-file module namespaces (`mod:weapons`, `mod:armor`)<br>• Cross-module inheritance and var resolution | “Your data is now a proper object graph. TOML is still pretending arrays of tables were a good idea.”    |
| **0.4.0**     | Dec 2026       | Planned      | **ASL Dialect Launch**<br>• Imperative scripting dialect<br>• Functions, control flow, state<br>• Seamless interop with AML objects<br>• `#!asl` shebang | “We just made config files executable. The industry wasn’t ready.”                                      |
| **1.0.0**     | Q1 2027        | Target       | **Production Ready**<br>• All above features stable<br>• Comprehensive test suite (10k+ golden files)<br>• Performance regression suite<br>• Official VS Code + IntelliJ plugins | “Anvil is now the default data layer for every serious project. Everything else is legacy.”             |

### Guiding Principles (Non-Negotiable)

1. **Parser remains 100% immutable and zero-copy** — forever  
2. **Mutability lives exclusively in the runtime API layer** — never in the AST  
3. **Fail-soft everywhere** — unresolved `$key` or `{var}` never crashes  
4. **Lowercase native types** — `object`, `array`, `tuple`, `blob` — are the public face of Anvil  
5. **Round-tripping is not optional** — if we can’t write it back exactly, we don’t ship it  
6. **Resolver is pluggable from day one** — no hidden magic

### Current North Star (next 6 months)

> **By June 2026, a user will be able to:**
> - [✅] Parse a 10 KB AML file in <150 µs with **zero allocations**
> - [ ] Receive native `object`/`array`/`tuple` types (mutable or immutable)
> - [ ] Use `$player_name` and `$"Hello, {name}!"` with graceful degradation
> - [ ] Inherit from any base, forward or backward referenced
> - [ ] Modify the model in memory and write it back **identically**

When that milestone is hit, the war is over.

The rest is just collecting the bodies.

**Anvil** — because your data deserves better than 1998.
