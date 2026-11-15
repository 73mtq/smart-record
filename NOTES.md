# NOTES

## 项目

- 仓库 `D:\github-repository\smart-record`：Android 记账应用，以本机一木记账 6.1.8（`com.wangc.bill`，versionCode 378）为基准做像素级界面复刻。
- 复刻包 `com.nonen.Bookkeeping`；像素采集用验收包 `com.nonen.Bookkeeping.uivalidation`（`-PyimuUiValidation=true` 构建）。
- Kotlin + Jetpack Compose + Room + DataStore；原生 View 承载原版布局，Compose 只做容器与对话框。
- 主分支 `main`，远端 `origin`（https://github.com/73mtq/smart-record.git）；用户要求改动完成后直接提交推送。

## 设备与测试工具

- 测试机：小米 15，序列号 `95f79212`，1200×2670，密度 520。
- adb：`D:\Android\Sdk\platform-tools\adb.exe`。
- 像素采集：`work/ui-harness/capture.py --app reference|validation`；对比：`work/ui-harness/diff.py`；场景清单 `work/ui-harness/screens.yaml`（31 页）。
- 验收数据：参考侧 Room 快照 `work/ui-fixtures/room-reference-current`（日常账本、78 分类、0 资产）+ 导出的 xls（2112 条，2026-11-14）。
- 参考侧实机在 2026-11-14 后新增了账单，重新做同日回归前必须重新导出参考数据。

## 已知干扰

- 测试机上的「李跳跳」（`hello.litiaotiao.app`）无障碍服务会抢占 uiautomator 节点树，导致 `uiautomator dump` 只返回少量节点。自动化脚本每次 dump 前必须先 `adb shell am force-stop hello.litiaotiao.app`。
- 键盘弹出会改变 Compose 对话框内输入框的坐标，自动化点击前要先收起键盘。

## 已完成（2026-11-15，提交 f6e36ed）

- 标签体系（v7 迁移）、关于我们、使用指南与本地文档、主题市场主色/四季背景、资产图标与备注（v8 迁移）。
- 分类编辑页（CategoryEditScreen）：名称、线条图标选择网格、仅在当前账本显示、删除；已真机验证新增与图标选择。
- 愿望编辑页（DreamEditScreen）、预算设置页（BudgetEditScreen）、周期设置页（CycleEditScreen）：编译、单测、Lint 通过，未真机逐页操作。
- 验证：编译、单测、Lint、`YimuCompletionInstrumentationTest`、真机标签/主题/关于页/分类新增。

## 未完成

- 分类、预算、周期、愿望、模板编辑页仍为对话框。
- 阶段 1 数据同步与阶段 7 的 31 页回归未执行。
- 规则页图标、分类管理「收入」标签字高残差未复核。
