---
name: wosbot-vision-debugger
description: Diagnostics and calibration guide for Frostguard OCR, template searching, grayscale matching, and UI coordinates.
---

# Frostguard 图像识别与 OCR 诊断技能

本技能用于诊断 Frostguard 运行过程中的模版找不到、OCR 读数异常、误触遮罩等问题。

---

## 1. 常见视觉异常排查流程

1. **点击误触 / 弹窗未遮罩**：
   - 检查 `PointData(x, y)` 是否落在目标按钮内部（720x1280 坐标）；
   - 确认检索区域 `AreaData` 是否误包含附近的奖品框 / 确认框；
   - 点击后配合 `sleepTask(delayMs)` 留足游戏动画过渡时间。

2. **灰度匹配 (Grayscale) 与多版本模板**：
   - 火晶时代 (FC) 与普通时代图像模板（如 `INTEL_BEAST_GRAYSCALE_FC` vs `INTEL_BEAST_GRAYSCALE`）需区分处理；
   - 模式匹配使用 `locatePatternMono`（单度匹配）或 `locatePattern`（灰度+颜色双通道）。

3. **OCR 倒计时与数字识别优化**：
   - 使用 `TesseractSettingsData.assembler().charWhitelist("0123456789").stripBackground(true)`；
   - 避免直接强转非空，对 `null` 结果保留默认 fallback 避让或重试机制。
