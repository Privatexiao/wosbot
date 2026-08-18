---
name: wosbot-dev
description: Frostguard (wosbot) project development, reactor compilation, packaging, and custom feature workflow guide.
---

# Frostguard (wosbot) 项目开发与验证技能指南

本技能为 `wosbot` (Frostguard) 项目的专用开发标准流程，涵盖多模块构建、桌面打包、图样/OCR 校验及专属功能维护。

---

## 1. 常用构建与调试命令

- **单模块快速编译**（不跑测试）：
  ```powershell
  tools\maven\apache-maven-3.9.6\bin\mvn.cmd -pl modules/tasks -am compile -DskipTests
  ```

- **全 Reactor 完整桌面打包**（自动生成完整 Windows 桌面运行环境）：
  ```powershell
  powershell -File "tools\setup_env_and_build.ps1"
  ```

- **启动源码应用**：
  ```powershell
  tools\maven\apache-maven-3.9.6\bin\mvn.cmd -pl modules/desktop javafx:run
  ```

---

## 2. UI 交互与坐标判定基准

- **标准分辨率**：全图与坐标基准统一为 `720x1280` 分辨率。
- **点击交互规范**：
  - 模板匹配结果优先使用 `tapInside(ImageSearchResultData result)`；
  - 固定区域使用 `tapInside(AreaData area)`；
  - 单点坐标使用 `tapNear(PointData point)`（附带默认抖动算法防封）；
  - 严禁使用过时的 `tapPoint` 或 `tapRandomPoint` 旧接口。

---

## 3. 专属功能同步更新铁律

每次修改或新增 `Privatexiao/wosbot` 专属功能时：
1. 必须同步修改 [`docs/custom-features.md`](../../../docs/custom-features.md)；
2. 保持日志前缀规范（如 `routineLogIntelligenceLine`）；
3. 任何代码改动除非用户显式要求 `提交推送git`，否则禁止自动运行 `git push`。

### 与上游合并时的完整性要求

1. `docs/custom-features.md` 中每一项功能都必须保留，功能完整性是合并完成的硬性验收条件；
2. 冲突时禁止直接选择 `ours`、`theirs` 或删除本项目实现，必须理解上游新结构后，把本项目的配置、UI、模板、持久化、调度、安全兜底、日志和测试迁移进去；
3. 合并后逐项对照清单检查并执行相关验证，只有编译通过不足以证明功能完整；未完成的真实画面或实机日志验证必须明确报告；
4. 未经用户明确授权，不得移除、禁用、缩减或替换本项目功能。仅当确认上游实现具备等价能力、配置兼容性和相同安全保证时，才可替换并在清单中标记为已上游化。
