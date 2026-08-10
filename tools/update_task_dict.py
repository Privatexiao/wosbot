# -*- coding: utf-8 -*-
task_translations = {
    # Task Names from Task Table
    "Initialize": "初始化准备",
    "Alliance Autojoin": "自动加入集结",
    "Alliance Championship": "联盟争霸赛",
    "Alliance Chests": "联盟宝藏箱",
    "Alliance Mobilization": "联盟动员大奖赛",
    "Alliance Pet Treasure": "联盟宠物宝藏",
    "Alliance Shop": "联盟商店",
    "Alliance Tech": "联盟科技捐献",
    "Bank": "理财金库",
    "Bear Trap": "熊陷阱活动",
    "Beast Hunting": "野外野兽猎杀",
    "Character Creation": "角色创建",
    "Chief Orders": "领主指令",
    "City Events": "城市内部事件",
    "City Officer Assignments": "城市官职委派",
    "City Upgrades": "城市建筑升级",
    "Daily Claims": "每日奖励领取",
    "Dummy Task": "示例测试任务",
    "Events": "限时活动",
    "Expert Automation": "专家管理",
    "Fishing Tournament": "钓鱼大赛",
    "Gather": "野外资源采集",
    "Gift Codes": "礼包码领取",
    "Intel": "雷达情报任务",
    "Journey of Light": "逐光之旅",
    "Mercenary Event": "雇佣兵猎杀",
    "Mystery Shop": "神秘黑市",
    "Nomadic Merchant": "游商打折小贩",
    "Pet Automation": "宠物自动化",
    "Polar Terror": "极地恶魔",
    "Research": "科技研究",
    "Skip Tutorial": "跳过新手引导",
    "Training": "士兵训练",
    "Tundra Trek": "苔原远征",
    "Tundra Truck": "苔原货车",
    "Myriad Bazaar": "万象集市",

    # Table Column Headers
    "Task Name": "任务名称",
    "Last Execution": "上次执行状态",
    "Next Execution": "下次计划执行",
    "Actions": "操作选项",

    # Status Values
    "Never": "从未",
    "Ready": "准备就绪",
    "Executing": "正在执行",
    "Just now": "刚刚",

    # Buttons & Controls
    "Timeline View": "时间轴视图",
    "Table View": "表格视图",
    "Search tasks...": "搜索任务...",
    "Resume All Queues": "恢复所有队列",
    "Pause All Queues": "暂停所有队列",
    "Resume All ...": "恢复所有队列",
    "Stop": "停止运行"
}

properties_file = r"fg-app\src\main\resources\i18n\messages_zh_CN.properties"

existing = {}
with open(properties_file, "r", encoding="utf-8") as f:
    for line in f:
        if "=" in line and not line.strip().startswith("#"):
            k, v = line.split("=", 1)
            existing[k.strip()] = v.strip()

existing.update(task_translations)

with open(properties_file, "w", encoding="utf-8") as f:
    f.write("# Frostguard Complete Chinese Dictionary with Task Table Translations\n\n")
    for k, v in sorted(existing.items()):
        f.write(f"{k}={v}\n")

print(f"Updated properties file with {len(existing)} entries")
