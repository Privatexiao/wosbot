# -*- coding: utf-8 -*-
beartrap_label_fixes = {
    "Rally Flag": "发起集结编队",
    "Join Flag": "加入集结编队",
    "Rally Flags": "发起集结编队",
    "Join Flags": "加入集结编队",
    "Trap Target": "陷阱与编队设置",
    "Call Own Rally": "发起自己的集结",
    "Enable Join Rally": "开启加入他人集结",
    "Prep minutes": "准备时间 (分钟)",
    "Next Bear Trap (UTC)": "下次熊陷阱时间 (UTC)",
    "Enable Bear Trap": "开启熊陷阱"
}

properties_file = r"fg-app\src\main\resources\i18n\messages_zh_CN.properties"

existing = {}
with open(properties_file, "r", encoding="utf-8") as f:
    for line in f:
        if "=" in line and not line.strip().startswith("#"):
            k, v = line.split("=", 1)
            existing[k.strip()] = v.strip()

existing.update(beartrap_label_fixes)

with open(properties_file, "w", encoding="utf-8") as f:
    f.write("# Frostguard Chinese Dictionary with Bear Trap Formation Preset Labels\n\n")
    for k, v in sorted(existing.items()):
        f.write(f"{k}={v}\n")

print(f"Updated properties file with {len(existing)} entries")
