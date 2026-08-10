# -*- coding: utf-8 -*-
log_translations = {
    # Console Log Messages
    "Queue paused": "队列已暂停",
    "Queue started": "队列已启动",
    "Queue stopped": "队列已停止",
    "Queue resumed": "队列已恢复",
    "TaskQueue": "任务队列调度器",
    "Manager": "管理器",
    "Profile Manager": "账号配置管理器",
    "ProfileManager": "账号配置管理器",
    "Successfully updated profile": "成功更新账号配置",
    "Successfully created profile": "成功创建账号配置",
    "Successfully deleted profile": "成功删除账号配置",
    "Task removed from queue": "任务已从队列中移除",
    "Executing task": "正在执行任务",
    "Task completed": "任务执行完成",
    "Task failed": "任务执行失败",
    "Home screen found": "已检测到游戏主界面",
    "Verifying character": "正在验证游戏角色"
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
    f.write("# Frostguard Complete Chinese Dictionary with Dynamic Log Messages\n\n")
    for k, v in sorted(existing.items()):
        f.write(f"{k}={v}\n")

print(f"Updated properties file with {len(existing)} entries")
