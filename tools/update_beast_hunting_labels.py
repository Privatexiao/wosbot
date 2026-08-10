# -*- coding: utf-8 -*-
beast_labels = {
    "Enable Beast Hunting": "开启野外野兽猎杀",
    "Active Marches": "并发出征队列数",
    "Beast Level": "猎杀野兽等级",
    "Stamina Reserve": "保留体力数值 (低于此体力停止)",
    "Max Attacks Limit (0 = Unlimited)": "打怪上限次数 (0为不限制)",
    "Use Stamina Items when Low": "体力不足时自动使用体力补充药水",
    "Stamina Item Reserve": "保留体力药水数量",
    "Beast Hunting Settings": "野外打怪与体力管理设置"
}

properties_file = r"fg-app\src\main\resources\i18n\messages_zh_CN.properties"

existing = {}
with open(properties_file, "r", encoding="utf-8") as f:
    for line in f:
        if "=" in line and not line.strip().startswith("#"):
            k, v = line.split("=", 1)
            existing[k.strip()] = v.strip()

existing.update(beast_labels)

with open(properties_file, "w", encoding="utf-8") as f:
    f.write("# Frostguard Chinese Dictionary with Beast Hunting Settings\n\n")
    for k, v in sorted(existing.items()):
        f.write(f"{k}={v}\n")

print(f"Updated properties file with {len(existing)} entries")
