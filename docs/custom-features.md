# 本项目相对上游的自定义功能

本文记录 `Privatexiao/wosbot` 相对原作者仓库 `Shederator/wosbot` 保留的功能扩展和行为修复。它既是用户功能说明，也是以后同步上游代码时的保留清单。

## 维护口径

- “上游行为”指最近一次合入的 `Shederator/wosbot` 基线；不能仅凭提交标题判断，更新前应核对实际代码差异。
- 新增、修改或修复自定义功能时，必须在同一批修改中更新本文，写清上游行为、本项目逻辑、配置项、失败边界和验证证据。
- 上游已经实现等价能力时，应把条目标记为“已上游化”或删除，并说明迁移依据，避免长期把上游功能误记为本项目专属。
- 合并上游时应逐项回归本文清单；不能静默丢失配置、界面入口、模板、调度规则或安全兜底。

## 差异总览

| 领域 | 上游基线 | 本项目扩展 |
| --- | --- | --- |
| 打熊加入 | 按可用加入入口执行常规加入 | 可选高级候选筛选、狂热模式、去重和六队列独立编队 |
| 手动集结加入 | 没有独立的目标筛选加入任务 | 按目标或全部目标扫描绿色 Join，并受出征数和编队配置约束 |
| 医院治疗 | 没有本项目的完整双入口治疗状态机 | 野外/城内入口、批次计算、联盟帮助等待和有界退出 |
| 情报任务 | 常规模板匹配和出征流程 | 关闭即零触屏、任务类型隔离、彩色/灰度双匹配和误触恢复 |
| 极地恶魔/自动集结 | 常规导航与开关读取 | 搜索页签兜底、运行期开关熔断和目标 OCR 选择 |
| 行军队列读取 | 多个流程可能反复开关面板读取 | 单次截图统一分类队列状态，并保证面板收尾 |
| 桌面界面 | 英文界面 | 内置简体中文词典和动态 JavaFX 节点翻译 |
| 异常排查 | 主要依赖运行日志 | 可保存异常帧及脱敏元数据 |

## 1. 打熊高级加入

相关实现：[`BearTrapRoutine.java`](../modules/tasks/src/main/java/dev/frostguard/tasks/combat/BearTrapRoutine.java)、[`BearRallyDecisionPolicy.java`](../modules/tasks/src/main/java/dev/frostguard/tasks/combat/BearRallyDecisionPolicy.java)、[`BearRallyDedupCache.java`](../modules/tasks/src/main/java/dev/frostguard/tasks/combat/BearRallyDedupCache.java)。

上游的常规模式优先保证“发现可加入集结后直接加入”。本项目在其外增加可关闭的高级决策层；`BEAR_TRAP_ADVANCED_JOIN_ENABLED_BOOL=false` 时仍走兼容路径，避免高级识别失败影响原有稳定行为。

高级模式读取候选卡片的成员数、集结总容量、剩余容量和倒计时，通过 `BearRallyDecisionPolicy` 依次检查：

1. `BEAR_TRAP_MIN_MEMBER_COUNT_INT`：最低已有成员数；
2. `BEAR_TRAP_MIN_RALLY_CAPACITY_INT`：最低集结总容量；
3. `BEAR_TRAP_MIN_REMAINING_CAPACITY_INT`：最低剩余可加入兵量；
4. 候选是否已被 `BearRallyDedupCache` 在 TTL 内处理，防止重复加入同一集结。

数值解析器 [`CompactGameNumberParser.java`](../modules/vision/src/main/java/dev/frostguard/vision/convert/CompactGameNumberParser.java) 支持整数、千分位及 `K/M` 缩写，并对溢出做保护。策略层已修复为用“当前成员数”单独判断最低成员门槛，TTL 在精确到期时立即失效。

启用 `BEAR_TRAP_FRENZY_MODE_ENABLED_BOOL` 后，活动达到 `BEAR_TRAP_FRENZY_START_MINUTE_INT`（默认 22 分钟）会放宽成员数限制。`BEAR_TRAP_JOIN_MARCH_1_FLAG_STRING` 至 `..._6_...` 可为六次加入分别选择 1～8 号保存编队或 `No Flag`；不可用编队不会被盲目点击。

> [!WARNING]
> 当前仓库没有可由真实保存帧证明的集结卡片 ROI 和字段解析器。旧实现曾用固定 `1/6`、固定主车名和固定倒计时代替真实识别，现已移除。高级开关开启时任务会明确告警并停止高级加入，不再使用伪造数据做出征决策；关闭高级开关仍可使用上游兼容路径。恢复高级筛选需要提供 `720x1280` 原始集结列表帧并补充保存帧测试。

## 2. 手动集结加入任务

相关实现：[`ManualRallyJoinRoutine.java`](../modules/tasks/src/main/java/dev/frostguard/tasks/combat/ManualRallyJoinRoutine.java) 和 `ManualRallyJoinPreemptionRule`。

该任务是独立于上游自动加入流程的主动扫描能力：检测联盟集结提示后打开列表，根据 `RALLY_TARGET_STRING` 选择指定目标，或在 `everything` 模式下接受任意目标。它只点击像素检测为绿色的可用 Join 按钮，灰色禁用按钮会跳过；颜色检测失败时也保守跳过，不再把未知状态当成绿色。指定目标时还要求目标模板与 Join 按钮处于同一行。

任务最多维持 `RALLY_MARCHES_INT` 个本任务出征，异常配置会限制在 1～6。每一路可通过 `RALLY_MARCH_1_FLAG_STRING` 至 `..._6_...` 指定保存编队；未指定时尝试 Equalize。出征前读取行军时间，按往返时间加缓冲登记预计归队时间；OCR 失败时使用 7 分钟回退。点击 Deploy 后会先排除队列已满和同目标重复出征弹窗，再登记成功；失败时不会虚增活动出征数。

## 3. 医院自动治疗

相关实现：[`HospitalHealRoutine.java`](../modules/tasks/src/main/java/dev/frostguard/tasks/city/HospitalHealRoutine.java)、[`HealBatchCalculator.java`](../modules/tasks/src/main/java/dev/frostguard/tasks/city/hospital/HealBatchCalculator.java) 和 [`HospitalHealState.java`](../modules/tasks/src/main/java/dev/frostguard/tasks/city/hospital/HospitalHealState.java)。

本项目新增医院任务及控制面板。入口由 `HOSPITAL_HEAL_FIELD_ENABLED_BOOL` 和 `HOSPITAL_HEAL_CITY_ENABLED_BOOL` 控制：优先尝试野外快捷入口，失败后可回退到城内医院。两者都关闭时直接退出，不触屏。当前仓库只有野外快捷入口和治疗按钮的真实模板；城内医院模板缺失时会明确告警并跳过，绝不盲点固定坐标。

治疗采用显式状态机：发现入口 → 确认治疗页 → 读取单兵治疗时间 → 计算本批数量 → 治疗/联盟帮助 → 等待或结束。每轮执行都会重置批次和 OCR 状态，避免上一次调度的校准残留。联盟帮助复用已存在的联盟帮助模板；剩余时间超过 `HOSPITAL_HEAL_MAX_WAIT_MINUTES_INT` 时保留当前治疗并退出，不使用缺失的取消模板在未知页面继续输入。速度道具默认不启用，状态循环带 20 步安全上限。

本项目同时补齐 `HOSPITAL_FIELD_ICON`、`HOSPITAL_HEAL_BUTTON` 等模板映射及真实图片资源，否则任务虽存在但无法可靠找到入口和治疗按钮。

## 4. 情报任务稳定性与安全熔断

相关实现：[`IntelligenceRoutine.java`](../modules/tasks/src/main/java/dev/frostguard/tasks/dailies/IntelligenceRoutine.java)。

- **零触屏关停**：进入流程和循环期间都重新读取 `INTEL_BOOL`。主开关关闭，或野兽、火晶野怪、幸存者、探索等子类型全部关闭时，在打开情报页之前退出。
- **类型隔离**：普通野怪与火晶野怪使用独立模板集合；未启用火晶野怪时不会把 `INTEL_BEAST_GRAYSCALE_FC*` 混入普通野怪识别。
- **双模式匹配**：任务卡检测同时使用彩色和灰度模板，提高不同渲染状态下的识别容错。
- **View 点击修复**：模板搜索限制在按钮区域，兜底点改为 `(360, 930)`，避免点击 `Y=730` 附近的奖励图标。若误触后卡片仍残留，会返回关闭遮罩并重试。
- **动画等待**：打开目标、地图平移和出征前增加有界等待，避免在按钮尚未完成动画时继续点击。

## 5. 极地恶魔与联盟自动集结修复

相关实现：[`PolarTerrorHuntingRoutine.java`](../modules/tasks/src/main/java/dev/frostguard/tasks/combat/PolarTerrorHuntingRoutine.java) 和 [`AllianceAutojoinRoutine.java`](../modules/tasks/src/main/java/dev/frostguard/tasks/alliance/AllianceAutojoinRoutine.java)。

极地恶魔搜索模板未命中时，本项目用 `(260, 913)` 点击搜索页签并等待 800ms，再继续等级选择，避免一次视觉波动直接进入长失败冷却。

联盟自动集结在执行入口重新读取 `ALLIANCE_AUTOJOIN_BOOL`，运行中关闭后不再强制打开联盟战争页。控制面板还可选择极地恶魔、吉娜的复仇和佣兵荣耀；任务按纵向切片 OCR 识别活动名及类似 `50/50` 的进度，只勾选用户启用且未达上限的目标。

## 6. 行军队列单次快照

相关实现：[`MarchHelper.java`](../modules/automation/src/main/java/dev/frostguard/engine/helper/MarchHelper.java)、`CommonGameAreas` 和 `CommonOCRSettings`。

`readMarchQueueSinglePass()` 只打开一次左侧行军面板、截取一次画面、读取全部槽位，并在 `finally` 中关闭面板。每个槽位综合颜色像素、状态/标题模板、活动图标、资源图标和倒计时 OCR，分类为空闲、锁定、不可用、采集、集结、攻击、驻扎或返回等状态。情报、极地恶魔等流程复用同一份结构化结果，减少多次开关面板造成的状态漂移和误触。

## 7. 简体中文界面本地化

相关实现：[`I18nService.java`](../modules/desktop/src/main/java/dev/frostguard/app/i18n/I18nService.java) 和 [`messages_zh_CN.properties`](../modules/desktop/src/main/resources/i18n/messages_zh_CN.properties)。

上游界面以英文为主。本项目启动时加载 UTF-8 中文词典，优先精确匹配，再处理大小写和部分带动态参数的状态文本。服务遍历 JavaFX 场景树，并监听后续动态加入的节点，对标签、按钮、Tab、表格列、对话框和菜单等进行一次性翻译；控制台、模拟器、账号、任务管理和启动器中的运行期文本也显式调用翻译入口。未命中的文本保留英文，不阻断界面加载。

## 8. 异常画面取证

相关实现：[`ExceptionScreenshotService.java`](../modules/automation/src/main/java/dev/frostguard/engine/service/ExceptionScreenshotService.java)。

任务队列遇到非用户取消的执行异常时，会捕获当前帧并保存到所选工作区的 `logs/screenshots/`，同时写入时间、匿名化配置 ID、任务名、原因和分辨率。原始帧先转换为标准 PNG，不再把像素缓冲区伪装成 `.png`。文件名会清洗任务名，不写账号名称或凭据；截图、转换或写入失败只记录警告，不能掩盖原任务异常。

## 9. 打野配置补充

打野界面保留六个独立保存编队配置 `BEAST_HUNTING_MARCH_1_FLAG_STRING` 至 `..._6_...`，并通过 `BEAST_HUNTING_MAX_ATTACKS_INT` 限制单轮攻击次数；当前默认值为 10，`0` 表示仅受可用队列数限制。任务现已在每次部署前按攻击序号读取并验证对应保存编队，而不是只保存 UI 配置却不执行。`No Flag` 表示不强制选择保存编队。

## 当前验证证据

- 打熊候选决策、TTL、紧凑数值解析、医院批次计算、手动集结边界、打野次数限制和异常 PNG 落盘已有自动化单元测试源码。
- 中文界面有 FXML 可加载性测试覆盖基础装载。
- 当前执行环境没有可用 JDK，因此本轮新增测试尚未实际运行；不能声称测试通过。
- 视觉坐标、OCR 阈值、模板识别和真实账号行为仍需保存帧回归及实时账号日志确认；高级打熊卡片和城内医院入口在取得真实帧前保持安全停用。
