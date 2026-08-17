# Frostguard / wosbot 本项目对比原作者库（Shederator/wosbot）专属新增功能与优化清单

本文档详细记录了本项目仓库（`Privatexiao/wosbot`）相比于原作者仓库（`Shederator/wosbot`）所独有、新增、重构或修补的所有功能与特性。

> [!IMPORTANT]
> **维护规则**：每次在本项目中新增、修改或修复任何相比原作者库独有的功能时，必须同步更新本文档，确保清单的完整与准确。

---

## 1. 打熊高级候选筛选与狂热模式 (Bear Trap Advanced Rally Join)

文件位置：[BearTrapRoutine.java](file:///E:/MeComputer/Desktop/wosbot/modules/tasks/src/main/java/dev/frostguard/tasks/combat/BearTrapRoutine.java)

### 1.1 高级筛选开关与多维度容量/门槛筛选 (`BEAR_TRAP_ADVANCED_JOIN_ENABLED_BOOL` 等)
- **变更背景**：原作者打熊加入逻辑只抓取第一个集结 Plus 图标即盲目出征，无法根据成员人数、车头总容量、剩余空位兵量、剩余倒计时或重复发起的集结进行智能甄别。
- **优化实现**：
  - 引入 `BEAR_TRAP_ADVANCED_JOIN_ENABLED_BOOL` 主开关，勾选后展开专属配置子面板 `vBoxAdvancedJoinOptions`；
  - 恢复 `textFieldMinMemberCount`（`BEAR_TRAP_MIN_MEMBER_COUNT_INT`，最少队伍人数门槛，如 `4` 人才加入，`0` 表示不限制）；
  - 新增 `textFieldMinRallyCapacity`（`BEAR_TRAP_MIN_RALLY_CAPACITY_INT`，别人最大集结量门槛，别人集结总容量需达到该值才加入）；
  - 新增 `textFieldMinRemainingCapacity`（`BEAR_TRAP_MIN_REMAINING_CAPACITY_INT`，最少剩余可加入兵量门槛，别人集结还能加入的兵量需达到该值才加入）；
  - 开关关闭时 **100% 保持原作者稳定加入逻辑**；开启时智能调用候选卡片评估器 `BearRallyDecisionPolicy` 与 TTL 去重缓存 `BearRallyDedupCache`。

### 1.2 K/M 数值解析扩展 (`CompactGameNumberParser`)
- **优化实现**：新增 `CompactGameNumberParser.java`，完美支持 `1200`、`1,200`、`1.2K`、`1.5M` 等卡片数值的精准解析与防溢出转换。

### 1.3 狂热模式 (Frenzy Mode)
- **优化实现**：支持设置活动后半段（如活动第 22 分钟后）自动激活狂热模式，放宽对队伍人数的限制，最大化打熊收益与满车率。

### 1.4 六出征队列独立旗帜选择 (`BEAR_TRAP_JOIN_MARCH_1_FLAG_STRING` ~ `BEAR_TRAP_JOIN_MARCH_6_FLAG_STRING`)
- **优化实现**：在 UI 集结设置中恢复了【编队 1】至【编队 6】独立下拉框选择，可为每次出征队伍指定对应的英雄队列旗帜（1~8 或 No Flag），并在 `BearTrapRoutine` 中轮询匹配生效。

---

## 2. 医院治疗自动化任务 (Hospital Heal Routine)

文件位置：[HospitalHealRoutine.java](file:///E:/MeComputer/Desktop/wosbot/modules/tasks/src/main/java/dev/frostguard/tasks/city/HospitalHealRoutine.java)

### 2.1 野外与城镇双入口支持 (`HOSPITAL_HEAL_FIELD_ENABLED_BOOL` / `HOSPITAL_HEAL_CITY_ENABLED_BOOL`)
- **优化实现**：支持从大地图快捷野外医院图标（WORLD）与城内医院建筑（HOME）自动进入，检测伤兵并进行安全批量治疗与联盟求助。

### 2.2 医院治疗图像匹配模板修复 (Hospital Heal Image Templates Fix)
- **修复实现**：补充了原提交缺失的 `HOSPITAL_FIELD_ICON`、`HOSPITAL_HEAL_BUTTON` 图像识别基准模板并完成属性映射，彻底解决机器人因“睁眼瞎”导致跳过治疗任务的问题。

---

## 3. 运行时异常取证与有界恢复 (Exception Evidence Service)

文件位置：[ExceptionScreenshotService.java](file:///E:/MeComputer/Desktop/wosbot/modules/automation/src/main/java/dev/frostguard/engine/service/ExceptionScreenshotService.java)

### 3.1 异常帧截屏与元数据自动落盘
- **优化实现**：当界面发生未知遮罩、弹窗或任务卡死时，自动将当前 RawImageData 截屏与脱敏元数据存储至 `logs/screenshots/` 目录，供后续排查与真实帧测试回归。

---

## 4. 情报任务 (Intelligence Routine)

文件位置：[IntelligenceRoutine.java](file:///E:/MeComputer/Desktop/wosbot/modules/tasks/src/main/java/dev/frostguard/tasks/dailies/IntelligenceRoutine.java)

### 4.1 精确 Y 坐标与按键匹配 (`Y = 930`)
- **变更背景**：在 720x1280 分辨率下，原作者代码在未匹配到 View 模板时采用 `Y = 730` 或区域中点进行兜底点击。但在实际游戏中，`Y = 730` 正好处于情报卡片中间的**奖励物品图标框（Activity Triumph Points / 火晶 / 英雄碎片）**，导致点击后弹出物品详解 Tooltip 遮罩，阻止后续出征。
- **优化实现**：
  - 将蓝底 **View（前往）** 按钮默认点击目标点修正为 `(360, 930)`（位于蓝色按钮中心）；
  - 将模板匹配检索区域收窄为 `(200, 850)` 到 `(520, 1000)`，提高识别准确率。

### 4.2 遮罩弹窗自动消除与二次重试
- **优化实现**：点击 View 按钮后，增加二次卡片残留检查。若检测到情报卡片因误触物品 Tooltip 仍处于打开状态，自动调用 `pressBack()` 消除遮罩，并重新精确点击 `(360, 930)`。

### 4.3 地图平移与面板滑动动画延迟平滑
- **优化实现**：
  - 点击 View 按钮后 sleep `2500ms`，充分等待大地图镜头平移和目标面板上滑动画；
  - `clickMapActionOrView` 增加 `1200ms` 前置延迟与 `1500ms` 后置延迟；
  - `deployIntelMarch` 增加 `1000ms` 出征前置延迟，确保出征按钮完全加载。

### 4.4 控制台关停“0 秒零触屏”前置熔断防护
- **变更背景**：原作者代码在检查情报任务可用性 `hasAnyIntelMissionAvailableFlow()` 的第一行直接调用了 `intelScreenHelper.ensureOnIntelScreen()`（强制打开情报界面）。导致即便用户在 GUI 控制台取消勾选【开启情报任务】，脚本在执行时仍会先去点开游戏内的情报页面。
- **优化实现**：
  - 在 `execute()` 顶部及 `hasAnyIntelMissionAvailableFlow()` 第一行，优先读取 `INTEL_BOOL` 配置；
  - 一旦配置为 `false`，立刻输出日志并 `return false` 退出，**做到 0 秒零触屏防护，绝对不再强制打开情报界面**。

### 4.5 全子任务关闭自动拦截
- **优化实现**：当用户开启情报主开关，但取消勾选了所有情报子类型（野兽、火晶野怪、幸存者、探索）时，前置拦截，不触摸屏幕，不打开情报界面。

### 4.6 火晶野怪（火晶巨兽/Master Bounty）与普通野怪独立解耦
- **变更背景**：原作者逻辑将火晶野怪模板（`INTEL_BEAST_GRAYSCALE_FC`）与普通野怪放在同一个数组中。当用户只勾选普通野怪、未勾选火晶野怪时，依然会匹配并打击火晶野兽。
- **优化实现**：
  - 彻底拆分 `fireBeastTemplates`（火晶野怪）与 `beastTemplates`（普通野怪）；
  - 当 `fireBeastsEnabled` 为 `false` 时，完全屏蔽 `INTEL_BEAST_GRAYSCALE_FC` 和 `INTEL_BEAST_GRAYSCALE_FC1` 模板；
  - 只有当剩余任务全部为未勾选类型时，情报任务能正常识别并优雅进入冷却。

### 4.7 运行期配置动态感知 (Dynamic Config Re-hydration)
- **优化实现**：在 `while (processingTask)` 循环内加入 `hydrateConfiguration()` 与 `INTEL_BOOL` 校验，如果在脚本运行过程中用户在控制台取消勾选情报，循环将在 `0.1s` 内感知并立即退出。

---

## 5. 极地恶魔任务 (Polar Terror Hunting Routine)

文件位置：[PolarTerrorHuntingRoutine.java](file:///E:/MeComputer/Desktop/wosbot/modules/tasks/src/main/java/dev/frostguard/tasks/combat/PolarTerrorHuntingRoutine.java)

### 5.1 搜寻 Tab 坐标 `(260, 913)` 强力兜底
- **变更背景**：在切页搜寻极地恶魔时，若大地图 Search 图标或 `POLAR_TERROR_SEARCH_ICON` 模板未被准确识别，旧代码直接抛出失败，导致任务异常终止并莫名进入 `30 分钟` 的长失败冷却。
- **优化实现**：
  - 在 `openUpPolarsMenu` 中，若模板未检索到极地恶魔图标，自动执行兜底坐标点击 `(260, 913)`（极地恶魔搜寻 Tab 索引位置）；
  - 点击后 sleep `800ms` 继续后续等级选择与打怪，大幅降低极地恶魔搜寻失败率。

---

## 6. 自动集结任务 (Alliance Autojoin Routine)

文件位置：[AllianceAutojoinRoutine.java](file:///E:/MeComputer/Desktop/wosbot/modules/tasks/src/main/java/dev/frostguard/tasks/alliance/AllianceAutojoinRoutine.java)

### 6.1 控制台开关动态感知与关停退出
- **优化实现**：在 `execute()` 入口加入 `ALLIANCE_AUTOJOIN_BOOL` 前置校验，运行中在控制台关闭自动集结后，任务直接退出，不再强制打开联盟战争界面。

### 6.2 基于 OCR 的 Auto-Join 目标智能勾选
- **优化实现**：
  - 在 UI 中新增**极地恶魔 (Polar Terror)**、**吉娜的复仇 (Gina's Revenge)** 和 **佣兵荣耀 (Mercenary Prestige)** 的目标自动加入勾选项。
  - 在进入 Auto-Join 设置面板后，采用多片段纵向切片进行 OCR（`processOcrSlice`），精准解析列表中的活动名称及其进度（如 `50/50`）。
  - 若用户配置了加入该目标，且进度未满（如不是 `50/50`），则自动根据切片坐标（`Y` 轴）点击复选框；若已达上限，则保证不加入，节省队列与体力。

---

## 7. 打野怪独立队列与最大攻击次数限制 (Beast Hunting Presets & Max Attacks)

文件位置：[BeastHuntingLayoutController.java](file:///E:/MeComputer/Desktop/wosbot/modules/desktop/src/main/java/dev/frostguard/app/panel/combat/BeastHuntingLayoutController.java) / [BeastHuntingLayout.fxml](file:///E:/MeComputer/Desktop/wosbot/modules/desktop/src/main/resources/layout/BeastHuntingLayout.fxml)

### 7.1 队列 1 ~ 6 独立英雄队伍旗帜配置 (`BEAST_HUNTING_MARCH_1_FLAG_STRING` ~ `BEAST_HUNTING_MARCH_6_FLAG_STRING`)
- **优化实现**：在 UI 界面中保留【March 1】至【March 6】的 6 个独立出征队列旗帜选择下拉框，可为每次出征队伍指定对应的英雄队列旗帜（1~8 或 No Flag），避免盲目使用默认队伍出征。

### 7.2 最大攻击次数限制 (`BEAST_HUNTING_MAX_ATTACKS_INT`)
- **优化实现**：在 UI 界面中保留最大攻击次数设置输入框（`0` 表示不限制），精准控制体力消耗与刷野上限。

---

## 8. 构建与工程规范 (Build & Engineering Rules)

- **手动 Git 提交机制**：除非用户明确发出 `提交推送git` 指令，否则所有本地代码修改与打包测试**严禁自动运行 `git push`**。
- **完整 Reactor 编译验证**：每次修改或同步上游代码后，必须通过 `mvn.cmd compile -DskipTests` 或 `tools/setup_env_and_build.ps1` 验证全 10 模块 `BUILD SUCCESS`。
