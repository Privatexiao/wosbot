# -*- coding: utf-8 -*-
log_translations = {
    # Task Names & Column Items from Screenshots
    "TaskDispatcher": "任务分配调度服务",
    "计划调度Service": "计划调度服务",
    "VIP Points": "VIP 每日积分点数",
    "Trek Supplies": "远征物资补给",
    "Expert Agnes (情报女...": "专家 Agnes 雷达情报",
    "Expert Agnes Intel": "专家 Agnes 雷达情报",
    "Expert Romulus Tag": "专家 Romulus 追踪标记",
    "Expert Romulus Troops": "专家 Romulus 军队管理",
    "Expert Skill 士兵训练": "专家技能与士兵训练",
    "Expert Skill Training": "专家技能与士兵训练",
    "Mail Rewards": "邮件奖励领取",

    # Startup & System Log Messages from Screenshots
    "Whiteout Survival is not running. Launching the game...": "无尽冬日游戏未在运行，正在自动启动游戏...",
    "is running.": "正处于运行状态。",
    "Checking emulator status...": "正在检查模拟器运行状态...",
    "Launching all queues": "正在启动所有账号队列",
    "Custom tasks discovered: 0": "扫描发现自定义任务数量: 0",
    "Initial schedule:": "初始计划执行时间:"
}

properties_file = r"fg-app\src\main\resources\i18n\messages_zh_CN.properties"

existing = {}
with open(properties_file, "r", encoding="utf-8") as f:
    for line in f:
        if "=" in line and not line.strip().startswith("#"):
            k, v = line.split("=", 1)
            existing[k.strip()] = v.strip()

existing.update(log_translations)

with open(properties_file, "w", encoding="utf-8") as f:
    f.write("# Frostguard Complete Chinese Dictionary with Explicit Screenshot Entries\n\n")
    for k, v in sorted(existing.items()):
        f.write(f"{k}={v}\n")

print(f"Updated properties file with {len(existing)} entries")
