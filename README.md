# Anvil: A Next-Gen Data Language

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)  
[![Java Version](https://img.shields.io/badge/java-21%2B-orange.svg)](https://adoptium.net/)  
[![Version](https://img.shields.io/badge/version-0.1.6--alpha-blue)](#)  
[![Build](https://img.shields.io/badge/build-passing-brightgreen.svg)](#)

**Anvil** — the data language that causes JSON, YAML, and TOML to admit they're embarrassing failures at ... everything - readability, portability, expressiveness, and speed.

> “JSON is what happens when a programmer hates humans.  
> YAML is what happens when a human tries to write JSON while drunk.  
> TOML is what happens when a config file tries to cosplay as INI.  
> Anvil is what happens when a software engineer decides _gooed enough_ sucks.”
>
> ~ Badkraft, 2025

Anvil is a **zero-copy**, **typeless**, **blazing-fast** modeling language (AML dialect) built in pure Java 21.  
It parses real-world 6–8 KB modded configs in **~120 µs warm**, with **zero heap allocations** for skipped content.

One JAR. No dependencies. No apologies.

## Core Features (AML – Anvil Modelling Language)

### Rich Literals
- **Strings** – double quotes only (`"stone"`) – because single quotes are for people who hate consistency  
- **Hex values** – `#FF5733` (2–8 digits) and classic `0xDEADBEEF`  
- **Bare literals** – unquoted identifiers (`stone`, `minecraft:dirt`, `needs_iron_tool`) – because quotes are for cowards  
- Full scientific notation, booleans, `null`, and mixed-type collections – because types are a suggestion

### Attributes – Metadata That Doesn’t Suck
```aml
@[mod="anvil_test", version="1.0.0", debug=true]
@[author="Badkraft", generated="2025-12-01"]
```
- Multiple top-level `@[…]` blocks allowed (unlike certain *other* formats that choke on two lines)  
- Attach to statements, objects, arrays, tuples  
- Pure metadata – parser doesn’t care, your runtime does

### Inheritance – For Engineers, By Engineers
```aml
base_block @[tier=1] := { hardness := 2.0 }

derived_block : base_block @[tier=2] := { hardness := 5.0 }
```
- `base` → `derived`, not "parent-child" ... learn the terms 
- Parser records the relationship **flat** – no merging during parse  
- Resolution and topology construction happen **at runtime** when the consumer asks  
- Base does **not** need to appear before derived – forward references allowed  
- Single inheritance only – because multiple inheritance is a war crime

### Collections Done Right
```aml
// Arrays – order matters
tags := [ "flammable", "occludes" ]

// Tuples – fixed size, semantic grouping
position := (100.5, 64.0, -200.3)

// Objects – because curly braces aren’t just for JavaScript anymore
player := {
    name := "Notch"
    health := 20.0
    inventory := [ (stone, 64), (diamond, 1) ]
}
```

### Near-Zero-Copy Parser
- Scans source without materializing strings unless explicitly requested  
- Whitespace and comments skipped in a single pass  
- Immutable sealed hierarchy of `record` types  
- Thread-safe, allocation-minimal, GC-friendly

> "0.1.5? That was yesterday’s bug. Today we ship awesome-sauce.  
>  ... oh, did we mention AML is minifiable?"
>
> ~ Badkraft
---
## Java API (v0.1.7 Fluent, Intuitive)

Given the following config:
```anvl
//	server.anvl
#!aml
@[version=2, experimental]

server @[core] := {
    name := "Anvil Survival"
    port := 25565
    motd := "Forged in fire."
}

world @[seed=1337] := {
    spawn @[respawn] := (0, 64, 0)
    rules @[hardcore] := [
        "pvp", "keepInventory=false", "naturalRegeneration=true"
    ]
}

motd @[pinned] := {
    readme := @md`*** # Anvil Server Forged in fire. Built to last.***`
}

player := ("Notch", 100, true)
```
You should be able to use the object topography intuitively, naturally, organically ... the way _it should be_.
```java
public final class AnvilServer {

    private final root config;

    private AnvilServer(root config) {
        this.config = config;
    }

    public static AnvilServer load(Path path) throws IOException {
        return new AnvilServer(Anvil.load(path));
    }

    // ──────────────────────────────────────────────────────────────
    // Real-world usage — the way a server actually reads its config
    // ──────────────────────────────────────────────────────────────

    public String serverName() {
        return config.node("server").get("name").asString();
    }

    public int port() {
        return config.node("server").get("port").asInt();
    }

    public String motd() {
        return config.node("server").get("motd").asString();
    }

    public long worldSeed() {
        return config.node("world").attribute("seed").asLong();
    }

    public Vector3i spawnPoint() {
        var spawn = config.node("world").get("spawn");
        return new Vector3i(
            spawn.get(0).asInt(),
            spawn.get(1).asInt(),
            spawn.get(2).asInt()
        );
    }

    public List<String> gameRules() {
        return config.node("world")
                     .get("rules")
                     .asArray()
                     .elements()
                     .stream()
                     .map(v -> v.asString())
                     .toList();
    }

    public String readmeMarkdown() {
        return config.node("motd")
                     .get("readme")
                     .asBlob()
                     .content();
    }

    public boolean isHardcore() {
        return config.node("world")
                     .get("rules")
                     .hasAttribute("hardcore");
    }

    public PlayerInfo player() {
        var p = config.node("player");
        return new PlayerInfo(
            p.get(0).asString(),           // name
            p.get(1).asInt(),              // health
            p.get(2).asBoolean()           // isFounder flag
        );
    }

    public boolean isExperimental() {
        return config.hasAttribute("experimental");
    }

    // ──────────────────────────────────────────────────────────────
    // Tiny value objects — because sometimes you want a real type
    // ──────────────────────────────────────────────────────────────

    public record Vector3i(int x, int y, int z) {}
    public record PlayerInfo(String name, int health, boolean founder) {}
}
```
... and that makes it easy to have real data modelled like we use it, like we see it:
```java
AnvilServer server = AnvilServer.load("server.anvl");

System.out.println("Starting " + server.serverName());
System.out.println("Port: " + server.port());
System.out.println("MOTD: " + server.motd());

Vector3i spawn = server.spawnPoint();
System.out.printf("Spawn: %d, %d, %d%n", spawn.x(), spawn.y(), spawn.z());

System.out.println("Game rules:");
server.gameRules().forEach(System.out::println);

System.out.println("Readme:");
System.out.println(server.readmeMarkdown());

if (server.isHardcore()) {
    System.out.println("⚔ Hardcore mode enabled");
}

if (server.isExperimental()) {
    System.out.println("Warning: Running experimental build");
}
```

## Roadmap

| Version   | Focus                                      | Status                 |
|-----------|--------------------------------------------|------------------------|
| 0.1.6     | Current stable alpha – parser + core API   | Released [Alpha-0.1.6] |
| 0.1.7     | Fluent wrapper API (`AnvilObject`, etc.)   | Release  (this)        |
| 0.1.8		| Absolute Zero-Copy                         | Planned                |
| 0.2.0     | ASL dialect, multi-file modules, vars block| Planned                |


## License
MIT © 2025 Badkraft & Contributors

> “Type doesn't matter … so `default_config := null // wrong type!` … so what? You can make it whatever you want … just document your template with a comment.”  
> 
> ~ Badkraft, probably drunk on victory

**Anvil** – because your data deserves better than 1998.
