# -*- coding: utf-8 -*-
beartrap_march_labels = {
    "Join March 1 Preset": "加入集结 队列 1 编队",
    "Join March 2 Preset": "加入集结 队列 2 编队",
    "Join March 3 Preset": "加入集结 队列 3 编队",
    "Join March 4 Preset": "加入集结 队列 4 编队",
    "Join March 5 Preset": "加入集结 队列 5 编队",
    "Join March 6 Preset": "加入集结 队列 6 编队",
    "Call Own Rally": "发起自己的集结",
    "Rally Flag": "发起集结编队",
    "Enable Join Rally": "开启加入他人集结"
}

properties_file = r"fg-app\src\main\resources\i18n\messages_zh_CN.properties"

existing = {}
with open(properties_file, "r", encoding="utf-8") as f:
    for line in f:
        if "=" in line and not line.strip().startswith("#"):
            k, v = line.split("=", 1)
            existing[k.strip()] = v.strip()

existing.update(beartrap_march_labels)

with open(properties_file, "w", encoding="utf-8") as f:
    f.write("# Frostguard Chinese Dictionary with 6 March Dropdown Labels\n\n")
    for k, v in sorted(existing.items()):
        f.write(f"{k}={v}\n")

print(f"Updated properties file with {len(existing)} entries")
