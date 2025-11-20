# generate_large_aml.py
print("#!aml\n")

# 800 stone variants
for i in range(800):
    print(f"stone_variant_{i} @[type=block, hardness=1.5, tool=pickaxe] := {{")
    print(f"    display_name := \"Stone Variant {i}\";")
    print(f"    drop := stone;")
    print(f"    sounds := {{ break := \"block.stone.break\", place := \"block.stone.place\" }};")
    print(f"}}")
    print()

# 200 ores
for i in range(200):
    metal = "iron" if i < 100 else "gold"
    level = 2 if i < 100 else 3
    print(f"ore_{metal}_{i % 100} @[type=ore, level={level}, drop=raw_{metal}, light=0] := {{")
    print(f"    hardness := 3.0;")
    print(f"    harvest_tool := pickaxe;")
    print(f"    harvest_level := {level};")
    print(f"    texture := \"ore_{metal}\";")
    print(f"}}")
    print()

# 10 decorative + 5 machines + 1 complex
# ... (add manually or extend loop)
