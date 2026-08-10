# -*- coding: utf-8 -*-
log_translations = {
    "Loading module: Statistics": "正在加载模块: 全盘数据统计",
    "Loading module: Character": "正在加载模块: 角色属性数据",
    "Loading module: Skip Tutorial": "正在加载模块: 跳过新手引导",
    "Loading module: Task Builder": "正在加载模块: 任务流程构建器",
    "Loading module: Debugging": "正在加载模块: 图像调试工具",
    "Loading module: Get Giftcodes": "正在加载模块: 领取礼包码",
    "Loading module:": "正在加载模块:",
    "Home screen found.": "已找到游戏主界面。",
    "Verifying character:": "正在验证游戏角色:",
    "Task removed from queue": "任务已从队列中移除",
    "NO LOGS": "暂无运行日志",
    "Close Emulator": "关闭模拟器",
    "Do Nothing": "不作处理",
    "Continuous": "连续模式",
    "ALL_PROFILES": "全部账号配置",
    "ALL_LEVELS": "全部日志级别",
    "All profiles": "全部账号配置",
    "All levels": "全部日志级别"
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
    f.write("# Frostguard Complete Chinese Dictionary with Log & Item Translations\n\n")
    for k, v in sorted(existing.items()):
        f.write(f"{k}={v}\n")

print(f"Updated properties file with {len(existing)} entries")
