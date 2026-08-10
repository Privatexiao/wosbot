# -*- coding: utf-8 -*-
beast_preset_labels = {
    "Beast March 1 Preset": "打野怪 队列 1 编队",
    "Beast March 2 Preset": "打野怪 队列 2 编队",
    "Beast March 3 Preset": "打野怪 队列 3 编队",
    "Beast March 4 Preset": "打野怪 队列 4 编队",
    "Beast March 5 Preset": "打野怪 队列 5 编队",
    "Beast March 6 Preset": "打野怪 队列 6 编队",
    "Beast Hunting March Presets": "打野怪 出征队列编队设置"
}

properties_file = r"fg-app\src\main\resources\i18n\messages_zh_CN.properties"

existing = {}
with open(properties_file, "r", encoding="utf-8") as f:
    for line in f:
        if "=" in line and not line.strip().startswith("#"):
            k, v = line.split("=", 1)
            existing[k.strip()] = v.strip()

existing.update(beast_preset_labels)

with open(properties_file, "w", encoding="utf-8") as f:
    f.write("# Frostguard Chinese Dictionary with Beast Hunting March Preset Labels\n\n")
    for k, v in sorted(existing.items()):
        f.write(f"{k}={v}\n")

print(f"Updated properties file with {len(existing)} entries")
