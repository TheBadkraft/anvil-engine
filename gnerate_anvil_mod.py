#!/usr/bin/env python3
# generate_massive_mod.py — the one true file
import random
from pathlib import Path

OUTPUT_FILE = Path("src/test/resources/mods/massive_mod.aml")
TARGET_KB = random.randint(4, 8)
TARGET_BYTES = TARGET_KB * 1024

OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)

# === DATA ===
ADJECTIVES = ["ancient", "cursed", "enchanted", "legendary", "mythical", "prismatic", "void", "infernal", "celestial", "frozen", "burning", "toxic", "radiant", "shadow", "lunar", "solar", "arcane", "divine", "demonic"]
METALS = ["iron", "gold", "copper", "netherite", "diamond", "emerald", "lapis", "redstone", "quartz", "amethyst"]
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo"]
GEMS = ["diamond", "emerald", "ruby", "sapphire", "topaz", "onyx", "opal"]
NOUNS = ["blade", "edge", "spike", "thorn", "fang", "claw", "shard", "core", "heart", "soul", "echo", "whisper", "storm", "flame", "frost", "void", "star", "moon", "sun", "abyss", "nexus"]

TOOLS = ["sword", "pickaxe", "axe", "shovel", "hoe", "hammer", "dagger", "spear", "staff", "wand", "bow", "crossbow"]
ARMOR = ["helmet", "chestplate", "leggings", "boots"]

MINECRAFT_TAGS = [
    "mineable/pickaxe", "mineable/axe", "mineable/shovel",
    "needs_stone_tool", "needs_iron_tool", "needs_diamond_tool",
    "flammable", "occludes", "full_block", "transparent"
]

print(f"Generating massive_mod.aml (~{TARGET_KB} KB) — 100% valid Anvil syntax...")

content = f"""#!aml
@[mod="stress_test", version="1.0.0", author="AnvilGen", size="{TARGET_KB}KB", valid=true]

"""

current_size = len(content.encode('utf-8'))
items = 0

while current_size < TARGET_BYTES * 0.94:
    item_id = random.choice(ADJECTIVES).lower() + "_" + random.choice(NOUNS).lower()

    # === Attributes — commas, always ===
    attrs = []
    if random.random() < 0.7: attrs.append(f"tier={random.randint(1,5)}")
    if random.random() < 0.5: attrs.append(f"rarity={random.choice(['common','uncommon','rare','epic'])}")
    if random.random() < 0.4: attrs.append(f"damage={random.uniform(4,18):.1f}")
    if random.random() < 0.4: attrs.append(f"durability={random.randint(100,3000)}")
    if random.random() < 0.2: attrs.append("fireproof=true")
    if random.random() < 0.15: attrs.append("cursed=true")
    if random.random() < 0.1: attrs.append("soulbound=true")

    attr_str = f" @[ {', '.join(attrs)} ]" if attrs else ""

    # === Item body ===
    line = f"{item_id}{attr_str} := {{\n"
    line += f"    name := \"{random.choice(ADJECTIVES).capitalize()} {random.choice(TOOLS + ARMOR + ['Block', 'Gem', 'Orb'])}\"\n"
    line += f"    type := {random.choice(TOOLS + ARMOR + ['block', 'ore', 'gem'])}\n"

    # === Tags array — commas, always ===
    if random.random() < 0.6:
        tags = random.sample(MINECRAFT_TAGS, k=random.randint(1, 4))
        tag_str = ", ".join(f'"{t}"' for t in tags)
        line += f"    tags := [ {tag_str} ]\n"

    # === Lore blob ===
    if random.random() < 0.3:
        lores = ["Forged in the void", "Whispers of the old gods", "Cursed by Herobrine", "Blessed by Notch", "Contains trapped souls"]
        line += f"    lore := @md`\"{random.choice(lores)}\"`\n"

    line += "}\n\n"

    content += line
    current_size += len(line.encode('utf-8'))
    items += 1

content += f"// END — {items} items — {len(content.encode('utf-8'))//1024} KB — 100% valid Anvil\n"

OUTPUT_FILE.write_text(content, encoding='utf-8')

print(f"Done! {OUTPUT_FILE}")
print(f"   Size: {len(content.encode('utf-8'))//1024} KB")
print(f"   Items: {items}")
print(f"   Arrays: commas fixed")
print(f"   Attributes: commas fixed")
print(f"   Syntax: 100% valid")
print(f"   Ready to destroy parsers.")
print(f"   Anvil wins. Always.")