### FINAL REQUIREMENTS (v0.1.4 Anvil Feature Requests)

1. sample required **Anvil** (AML)
```anvil
#!aml
// A
@[version="1.21.10"]
@[mc_version="1.21.10"]
@[generated="2025-11-24T11:11:00Z"]
@[generator="aurora-mapping-tools/0.3.0"]
@[source="mojang-official-proguard"]
@[license="https://account.mojang.com/documents/minecraft_eula"]

// B
net.minecraft.client.Minecraft @[class] := {
    obf    := "f_91002_"
    source := "Minecraft.java"

    fields := {
// C
        instance @[static returns="Minecraft"] := {
            obf        := "f_91002_"
            descriptor := "Lnet/minecraft/client/Minecraft;"
        }
    }

    methods := {
        getInstance @[static returns="Minecraft"] := {
            obf        := "m_91392_"
            descriptor := "()Lnet/minecraft/client/Minecraft;"
        }
    }
}

com.mojang.blaze3d.audio.Channel @[class] := {
    obf    := "eze"
    source := "Channel.java"

    methods := {
        create @[static returns="Channel"] := {
            obf        := "a"
            descriptor := "()Lcom/mojang/blaze3d/audio/Channel;"
        }

        play := {
            obf        := "c"
            descriptor := "()V"
        }

        setVolume @[returns="void"] := {
            obf        := "b"
            descriptor := "(F)V"
        }
    }
}

// **NOTE**: _not shown_ D
```

### Required Anvil Engine Fixes (0.1.4 → 0.1.5) – Final List

- First, a bug:
| # | Issue                                      | Status     | Priority |
|---|--------------------------------------------|------------|----------|
| 1 | String literals emit `""value""`           | Bug        | Critical |

- Second
| # | Feature                                          | Status     | Priority |
|---|--------------------------------------------------|------------|----------|
| A | Only one `@[…]` module attribute block allowed   | Limitation | Critical |
| B | `.` allowed in keys (???)                        | Limitation | Critical |
| C | Nested objects allowed attribution (???)         | New feature| Required |
| D | Inheritance - single inheritance `Ancestor`      | New feature| Required |

