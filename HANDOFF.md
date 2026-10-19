# HANDOFF — S11 一木记账界面复刻

## 1. 状态

- **阶段：S11；阶段目标：** 在 S10 交接的未提交工作区上继续完成一木记账界面复刻，包括首页差异收敛、真实操作测试、模板抽屉与账本弹窗、其余二级页面、夜间主题、用户包交付和阶段交接。阶段编号依据 `work/logs/yimu-ui-reconstruction-s11/pause/README.md`、`work/logs/yimu-ui-reconstruction-s11/progress.md` 和 `D:\桌面\temp\smart-record-S11-续跑提示词.md`。
- **当前状态：进行中，2026-10-19 21:15 用户要求暂停。** 步骤 1 至 7、9 已完成；步骤 8 与 8.5 已完成；步骤 10 部分完成；步骤 11 至 14 未开始。没有完成全部验收的用户 APK。
- **未完成事项：** 步骤 10 的首页抽屉、账本弹窗和滚动首页验收；步骤 11 的其余 25 个页面；步骤 12 的夜间主题；步骤 13 的用户包交付；步骤 14 的 S11 交接。原版临时模板和 validation 模板尚未删除。
- **仓库：** `D:\github-repository\smart-record`；分支 `main`；本次核查时间 2026-10-19 21:20（Asia/Shanghai）；写作前 HEAD 为 `9251f8caac4bb90f00653d7e3f371fe3dc3d624d`（`handoff: S10 阶段交接`）。暂存区为空；工作区 57 个跟踪文件修改、2 个删除、324 个未跟踪条目；`git status --porcelain -uall` 共 381 行。S11 没有新提交，源码实现全部保存在当前工作区；单独检出交接提交不能恢复全部实现。
- **阶段起点与证据范围：** S11 起点按 S10 交接提交 `9251f8c`。`git log --stat 9251f8c..HEAD` 为空。相对 S10 交接的 `work/logs/handoff-s10/source-before.csv`，当前源码有 17 个修改、9 个新增、1 个移除；相对 2026-10-19 18:40 的 S11 暂停快照有 9 个修改、2 个新增。逐项对照在 `work/logs/handoff-s11/source-scope.txt`，完整状态、补丁和文件哈希在 `work/logs/yimu-ui-reconstruction-s11/pause-20261019-2115/`。
- **本阶段任务要求及来源：**
  - 用户原始续跑提示词在 `D:\桌面\temp\smart-record-S11-续跑提示词.md`（仅本机存在）。要求：从文件中证据续跑；控制上下文；按增量事实处理参考数据 1974 条；完成步骤 8 和步骤 10 至 14；遵守 HANDOFF 的长期决定。
  - 用户批准的 S11 实施计划在 `C:\Users\29602\.qoder\plans\shining-dale-drake.md`（仅本机存在）。计划包含步骤 8、8.5、10 至 14，明确要求先构建验证 banner、给出文字宽度取证路线、完成 29 页日间与夜间采集、用户包交付和阶段交接。
  - 用户在本会话明确批准：全部验收通过后提交仅含 `HANDOFF.md` 的交接提交；为夜间参考采集临时切换原版主题并在采集后恢复。
  - 本次交接指令来自当前会话的用户消息，未另存为独立文件。要求为：核查当前证据、整体重写 `HANDOFF.md`、仅提交这份文档、不改变其他文件及其暂存状态、最后回复提交号与全文。
  - 用户要求与 AI 建议的区分：“续跑、控制上下文、整体重写并提交交接文档”是用户要求；“原生 RecyclerView、16sp 加粗、`createEmptyComposeRule`、抽屉放在工具栏下方”等是 AI 提出并经用户批准的计划内容。计划中尚未执行的部分仍是待执行建议。
- **数据与产物：**
  - 参考正式导出 `work/ui-fixtures/yimu_all_20261019_1922.xls`：1974 条，支出 `-4355617` 分，收入 `3201248` 分，SHA-256 `63dc023d23ea4ef40c99a500d00cbe7ee5ef2026ed89a0a937d771b0274eda20`。2026-10 月收入 282.00、支出 4053.44、结余 -3771.44。
  - validation 数据库与参考 manifest 的 dataset 身份均为 1974 条、支出 `-4355617`、收入 `3201248`；`check_reference_month.py` 在 2026-10-19 21:19 退出 0。
  - 最新 validation 应用 APK：`app/build/outputs/apk/debug/app-debug.apk`，SHA-256 `98f8a042b852c864a19557882a35fd702b43bc17a9dba52ffb2482e01a31d884`；固定副本 `work/artifacts/yimu-ui-validation.apk` 相同。测试 APK：`app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`，SHA-256 `00a74c27facb15b6a2facac80e9a870448e9a153c76d8b87f1a3f1274ddb8408`。validation 包 `lastUpdateTime` 为 2026-10-19 21:14:33。
  - 用户包 `com.nonen.Bookkeeping`：versionCode 5、versionName 0.2.3、`lastUpdateTime` 2026-10-18 22:31:25；S11 未安装或覆盖，用户数据未在 S11 重新读取；安装前必须重新备份核对。
  - 参考包 `com.wangc.bill` 6.1.8/versionCode 378；参考 manifest 的 APK SHA-256 为 `e5ec945aa290c6f66fcd43c3f4307eebc5c7796d188e41895dcdf7fed599f8fd`。
  - 原版临时模板：`食品餐饮-早餐`，金额 12.34，未锁定；已在原版 `home_drawer` 非空截图中出现（119 号日志）。模板只写入 `module_bills`，没有新增账单；账单数最后一次核验为 1974。
  - validation 测试模板：同一分类与金额，已通过 `TemplateFixtureInstrumentationTest#insertTemplate` 写入 validation 数据库（124 号 OK 1 test），尚未删除。
- **最后执行：** 153 号本地 `home_scrolled` 比较（2026-10-19 21:20，退出 1，整体 2.69%、最差区块 29.32%）。最后一次设备操作是恢复动画缩放为 1.0；当前设备前台为系统桌面，没有运行中的仪器进程。本次交接没有构建、安装或操作手机。
- **暂停快照：** `work/logs/yimu-ui-reconstruction-s11/pause-20261019-2115/README.md`、`git-status.txt`、`tracked-changes.patch`、`source-sha256.csv`。S11 前段暂停快照在 `work/logs/yimu-ui-reconstruction-s11/pause/`。

## 2. 变更文件清单

S11 的实现全部未提交。工作区同时包含 S09、S10 阶段遗留的源码修改；本节只把相对 S10 交接和相对 18:40 暂停快照可确认的 S11 变更归入 S11，其他修改保留在“其他未提交改动”中。完整数量与路径见 `work/logs/handoff-s11/source-scope.txt`。

### 本会话相对 18:40 暂停快照的源码修改（9 个）

- `app/src/main/java/com/nonen/Bookkeeping/ui/components/YimuNativeHomeList.kt`（未提交）：banner 高度改为整数除法；汇总金额改为 16sp 加粗；汇总标题、金额和 month_pay 颜色修正；预算占位移回 13sp 与 5dp 上边距；工具栏图标颜色与账本图标；无备注行隐藏 `second_layout`；原生 XML 使用 AppCompatTextView/AppCompatImageView；新增长按新增按钮打开模板抽屉的回调。证据：45 至 83 号日志、49 号 dexdump、65/71/79/83 号比较。
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/YimuHomeScreen.kt`（未提交）：账本图标改为 22dp×45dp、centerInside、`textColorPrimary`；搜索/更多/新增图标颜色统一；模板抽屉改为工具栏下方的 280dp 面板与白色模板卡片；滚动后隐藏悬浮球；增加分类数据订阅和长按打开抽屉。证据：54 至 83 号、120 至 152 号日志。
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/MainScreen.kt`（未提交）：`itemIconSize` 改为 `roundToInt`；向首页传递 `categoryRepository`。证据：68 至 83 号日志、120 号构建。
- `app/build.gradle.kts`、`gradle/libs.versions.toml`（未提交）：显式加入 `androidx.appcompat:appcompat:1.7.0`，供原生首页 XML 与后续夜间资源使用。证据：72 号构建成功。
- `app/src/main/res/layout/yimu_home_summary.xml`（未提交）：预算占位 `layout_marginTop` 改为 5dp；与运行时字体设置配套。证据：65 至 83 号日志。
- `app/src/main/res/values/yimu_strings.xml`（未提交）：新增抽屉空态、无账户、账户、不报销文案。证据：120 号构建。
- `app/src/androidTest/java/com/nonen/Bookkeeping/ui/YimuAddEditUiInstrumentationTest.kt`（未提交）：改用 `createEmptyComposeRule()`；由 shell 启动真实 `MainActivity`，在现有 Activity 上 `setContent`；保留真实 Room、DataStore 和 5 项数据库断言。证据：84 至 89 号日志。
- `app/src/androidTest/java/com/nonen/Bookkeeping/ui/YimuCaptureInstrumentationTest.kt`（未提交）：新增 `long_press`、`accessibility_click` 动作；`launch` 时结束已恢复的 Activity；长按失败时保存调试截图。证据：91 至 152 号日志。
- `app/src/androidTest/java/com/nonen/Bookkeeping/ui/ComposeHostDiagnosticTest.kt`（新增、未跟踪）：诊断 shell 启动宿主后空 Compose 规则能否观察内容。证据：86 号 OK 1 test。

### 本会话新增的第二个测试文件

- `app/src/androidTest/java/com/nonen/Bookkeeping/ui/TemplateFixtureInstrumentationTest.kt`（新增、未跟踪）：向 validation 数据库插入或删除一条与参考一致的模板。证据：124 号 OK 1 test。

### S11 前段相对 S10 交接的源码修改（17 个修改中的其余文件和新增项）

- `app/src/main/java/com/nonen/Bookkeeping/data/prefs/SettingsStore.kt`、`app/src/main/java/com/nonen/Bookkeeping/export/FullBackupService.kt`、`app/src/main/java/com/nonen/Bookkeeping/ui/AppNavHost.kt`、`app/src/main/java/com/nonen/Bookkeeping/ui/screens/AddEditViewModel.kt`、`app/src/main/java/com/nonen/Bookkeeping/ui/screens/MyScreen.kt`、`app/src/main/java/com/nonen/Bookkeeping/ui/screens/TemplateScreen.kt`、`app/src/main/java/com/nonen/Bookkeeping/ui/screens/YimuAddEditScreen.kt`、`app/src/main/res/values/yimu_ids.xml`：来自 S11 前段（16:37 至 18:40），涉及模板编辑、备份、路由、我的页面和 ID 资源。证据：18:40 暂停快照的 `source-sha256.csv`、`README.md` 和 01 至 44 号日志。
- 新增 `app/src/main/java/com/nonen/Bookkeeping/ui/components/YimuNativeTemplateList.kt`、`app/src/main/java/com/nonen/Bookkeeping/ui/screens/SearchScreen.kt`、`app/src/test/java/com/nonen/Bookkeeping/ui/screens/TemplateEditorStateTest.kt`、`app/src/main/res/drawable/line_stroke.xml`、`app/src/main/res/mipmap-xhdpi/ic_transfer_accounts.png`、`app/src/main/res/mipmap-xxhdpi/ic_delete.png`、`app/src/main/res/mipmap-xxhdpi/ic_tick.png`：来自 S11 前段的模板管理页、搜索页和测试资源。
- 删除 `app/src/main/res/values/yimu_native_home_ids.xml`：S11 前段把首页 ID 合并到现有资源，具体原因见 18:40 暂停快照与日志；当前源码不再引用该文件。

### 其他未提交改动（不作为 S11 变更归入）

- 工作区中的 57 个跟踪修改、2 个删除和 324 个未跟踪条目中，除第 2 节列出的 S11 变更外，其余来自 S09、S10 及更早任务；S10 HANDOFF 的变更清单记录了对应来源。`docs/plans/yimu_offline_rebuild_antigravity.md` 等文件在 S10 交接基线中已存在修改，`source-scope.txt` 未把它们列入 S11 相对 S10 的变更。
- `work/` 目录被 Git 忽略，以下文件只在当前机器存在，未进入 Git 差异：
  - `work/ui-harness/screens.yaml`：dataset 更新为 1974 条；`home_top` 增加必要 bounds；`home_drawer` 改为长按 `add_bill_btn`；`home_scrolled` 改为 2000ms 慢速拖动。
  - `work/ui-harness/capture.py`：validation 每个截图页开始前强制停止独立包；本次会话未修改 `dataset.py`、`verification.py`、`diff.py`。
  - `work/ui-harness/window_action.py`：新增长按和无障碍点击参数；新增 `check_reference_month.py`、`extract_font_evidence.ps1`。
  - `work/logs/yimu-ui-reconstruction-s11/` 的 42 至 153 号日志、两份暂停快照、`instrumentation-notes.md`、`progress.md`；`work/ui-capture/` 的参考与 validation 截图、manifest、差异图；`work/ui-fixtures/yimu_all_20261019_1922.xls`；`work/artifacts/yimu-ui-validation.apk`；`work/ui-evidence/` 的反编译与资源证据。
- 本阶段没有提交任何源码。交接只提交 `HANDOFF.md`。

## 3. 关键决策与理由

### 长期决定（继承 S10，不得推翻）

- **【长期】参考与组件：** 依据原版 APK、反编译源码和同一台真机；使用 Material Components 1.10.0、CalendarView 3.7.1、MPAndroidChart v3.1.0、SwitchButton 2.1.0，页面使用 Compose 与 AndroidView。复杂组件复用原版测量与交互，不自行重画。
- **【长期】原版资源：** 保留 62 个 lineIcon 和 78 项分类映射；原版系统备份、私有数据库和凭据保持本地隔离，凭据不加入应用资源。
- **【长期】隔离与验证：** 用户包保留个人数据；参考导入、仪器测试和覆盖恢复限定 `com.nonen.Bookkeeping.uivalidation`；使用真实 Room、DataStore、模型和操作。用户账户余额保持业务含义，不为截图调整；DAO 替身不能作为事务验收依据。
- **【长期】截图标准：** 必要控件有效且检查数量大于零；整体差异不超过 1.5%，任意 64×64 区域不超过 6%，关键 bounds 差不超过 3px，单通道容差 8，纯色区域单通道差不超过 2。保持既定掩膜及数据、环境核验；人工口头确认不能代替报告。应用 APK 变化后重新采集。
- **【长期】页面状态：** 底栏五项；导航文字默认关闭，开启时 11sp；账本共享至首页、日历、统计和新增账单，并纳入备份；模板抽屉 280dp，账本通过工具栏弹窗切换。
- **【长期】持久化：** Budget、CategoryBudget、BudgetSetting、Cycle、CycleLog、ModuleBill、Dream、DreamLog 保持独立生命周期；模板在 `module_bills`，愿望在 `dreams/dream_logs`，不向 transactions 增加模板或愿望标记替代实体。CycleDate 独立持久化要求仍待核实。
- **【长期】金额与迁移：** 金额列沿用 Double 和两位金额规则，键盘使用 BigDecimal；保留 Room 版本 6 和增量迁移；历史默认 bookId/assetId 为 1，历史记录可编辑。
- **【长期】分类：** 唯一索引 `(categoryId,parentId,categoryType)`；一级 ID 9 及其 11 项子分类为收入。唯一映射确定；多义分类保留二级 null 或独立 isCustom 业务 ID；全局 hide 与 hideBook 独立，SQLite hide 为 0/1，hideBook 为逗号分隔账本 ID。保存实际分类 ID 并更新商户记忆；删除自定义分类保留账单文字和金额。
- **【长期】资金：** 增删改和余额在 Room 事务内原子一致；验证输入、关联、余额、编辑删除对冲及统计。保护默认现金账户 1；删除其他账户时重定向历史并更新名称。信用账户欠款为 `totalQuota-assetNumber`，超额余额计入资产；借入账户余额绝对值计入负债；净资产为总资产减负债。
- **【长期】统计：** 历史资产由当前余额减去时点后全部资金变化逆算，包含转账和手续费；期末值对应当前资产汇总。一级与二级分类统一聚合，二级占比分母为所属一级总额。
- **【长期】本地模型：** 保留 `classification-model.ser`、`TimeExp.m` 及 LibLinear/HanLP，不重新训练或接入云端模型。门槛 0.015，低于门槛归入 `其他-其他`；`食品餐饮-其他` 按 [04:00,10:00) 早餐、[10:00,16:00) 午餐、其余晚餐处理。保留金额预处理 `(块|元|点)([1-9一二三四五六七八九])[毛角]` 到 `点$2` 及常见动词、首尾单位规则；中文金额完整流程仍需验收。
- **【长期】周期与备份：** 使用 `cycle_{cycleId}_{yyyyMMdd}` 及执行日志防重；事务内联动余额；每次执行后读取最新状态。23 列 Excel/CSV 交换和完整备份分别处理；完整备份采用标准 ZIP、manifest.json、data.json 及 SHA-256 前置校验，失败保护数据库。
- **【长期】业务范围：** 保留本地支出、收入、转账；债务为视觉占位；会员、账号、云服务、语音、自动记账、桌面微件及房贷车贷等既定业务不扩展。原版只允许新增一个临时模板用于取证，记录标识并删除本次新增项。
- **S10 采集与字符（跨阶段沿用）：** 原版由外部 shell 冷启动，持续式 UIAutomator 读取交互窗口和 View ID 并检查必要控件。文字使用 AppCompat 参考 Theme 和整数像素尺寸；首页使用原生 XML/RecyclerView。JVM JSON 使用真实 `org.json:json:20240303`；externalScope 不能代替 Android 调度验证；TodayWatcher 等待实际注册后取消；金额沿用 Format.kt；Compose 避免依赖不可观察的 Locale.getDefault()。

### S11 实际采用的决定

- **首页测量与颜色：** 原版 `com.wangc.bill.database.action.a1.t` 在绑定汇总时执行 `setTextSize(16.0f)` 与 `Typeface.defaultFromStyle(BOLD)`，覆盖 XML 的 15sp；`month_income`、`month_balance` 为 16sp 加粗，`month_budget` 在未设置预算时为 13sp 常规字重与 5dp 上边距，`month_pay` 保持 25dp。汇总标题为 `#8A8A8A`，values 为黑色，`month_pay` 为 `#48AB93`。证据：49 号 dexdump、65/71/79/83 号比较。
- **原生列表与 AppCompat：** `YimuNativeHomeList` 使用 AppCompatTextView/AppCompatImageView 承载原版 XML，显式加入 appcompat 1.7.0；banner 高度使用整数除法；底栏图标尺寸使用 `roundToInt`。这些决定用于匹配原版测量，避免 Compose 与原生测量产生差异。
- **Compose 测试宿主：** `createComposeRule()` 会由测试进程启动 ComponentActivity，在当前 MIUI 设备上被后台启动限制拦截（result code 102）。改为 `createEmptyComposeRule()`，由 shell 启动真实 `MainActivity`，再在现有 Activity 上 `setContent`。5 项真实 Room 操作测试已通过；不恢复旧宿主方案，除非有新的设备证据。
- **模板抽屉：** 抽屉放在工具栏下方，宽度 280dp，使用白色模板卡片；不再使用覆盖整屏的 `ModalNavigationDrawer` 作为最终形态。原版抽屉打开时右侧不变暗，因此移除遮罩；原版在滚动后隐藏悬浮球，复刻侧已在滚动时隐藏。该决定尚未完成验收。
- **数据预检：** 每次参考采集后先运行 `check_reference_month.py`，核对 2026-10 的 282.00、4053.44、-3771.44 与 1974 条总额；数据不一致时停止截图比较，重新导出并导入参考数据，不用界面改动掩盖数据差异。用户原始提示词中的 1973 条和旧背景结论不再适用。
- **截图测试工具：** 为 validation 每个页面增加冷启动；参考页长按抽屉使用 `long_press`；PopupWindow 使用 `accessibility_click`；`home_scrolled` 使用 2000ms 慢速拖动并等待 1500ms。测试工具改动只用于采集，不改变应用业务。
- **夜间主题的现有事实：** `MainActivity` 当前只根据 `ThemeMode` 改变 Compose 配色，`ThemeMode.DARK` 不会自动切换 `values-night`、night drawable 和原生 XML 资源。步骤 12 需要把主题模式接到 Android 资源层（计划中的 AppCompatActivity + AppCompatDelegate 路线尚未实施）。用户在会话中批准为参考采集临时切换原版主题并在采集后恢复。
- **用户批准与待执行：** 用户批准了上述计划、临时原版模板、交接提交和原版主题临时切换；步骤 11 至 14 的实施仍未执行，不能把计划当作已完成结果。

### 被否决或有证据的失败方案

- `createComposeRule()`、`ActivityScenario`、预启动宿主、替换宿主 Activity 均被 MIUI 后台启动限制拦截（102）；证据在 `instrumentation-notes.md` 和 86/89 号日志。现行空规则方案已通过 5 项测试。
- 整屏 `ModalNavigationDrawer` 覆盖工具栏，与原版抽屉位置不一致；改为工具栏下方的面板。该改动未完成验收。
- 抽屉遮罩让右侧变暗，与原版截图不一致；已移除遮罩。
- 快速滑动会导致滚动位置不稳定且两侧位置不同；已改为慢速拖动，但当前 `home_scrolled` 仍未通过。
- 通过长按 `add_bill_btn` 打开 validation 抽屉在 152 号采集中失败；原因未确认，不能把该路径视为已验证。

## 4. 已知问题与失败尝试

- **`home_drawer` validation 采集长按失败。** 现象：152 号日志报“找不到长按控件 add_bill_btn”，148 号同一调用中 `home_scrolled` 成功、`home_drawer` 失败。原因未确认。已尝试每页强制停止独立包、结束已恢复 Activity、单独运行 window_action；149 号单独测试时前台为系统桌面。正确做法：先保存采集失败时的前台截图，确认应用和首页列表已显示并位于顶部；必要时在长按前滚动到顶部或对工具栏新增按钮增加长按；不要放宽必要控件检查。该问题未解决。
- **`account_book_popup` validation 未完成。** 现象：125 号日志报缺少 `add_account_book`；当前实现是 Compose AlertDialog，参考是工具栏下方的原生弹窗。原因：弹窗结构和 testTag 暴露方式不同。正确做法：按 `popup_home_account_book.xml` 实现原生弹窗或等价格式，并给关键控件真实资源 ID；重新采集和比较。该页面未验收。
- **`home_scrolled` 当前不通过。** 153 号比较整体 2.69%、最差 64×64 区块 29.32%，退出 1。旧构建曾得到 1.318% 与 100% 最差区块，该结果不属于当前构建。当前问题是滚动位置和列表可见项不一致，可能由拖动距离、首项高度或重置流程造成。正确做法：固定拖动距离或使用明确的滚动动作，先确认两侧列表内容一致，再比较；不能改阈值。
- **抽屉上次比较的最差区块来自悬浮球。** 21:06 的一次有效比较整体约 2.75%，最差区块 100%，参考在该位置显示悬浮球，validation 隐藏了悬浮球；该次报告后来被失败运行覆盖，数值只保留在暂停 README 中。现行代码在顶部显示悬浮球、滚动后隐藏悬浮球，需要重新采集后确认。
- **`home_top` 的通过结果属于旧 APK。** 83 号比较通过时使用 80 号构建的 APK；之后 `YimuHomeScreen` 的抽屉位置、悬浮球条件和首页结构发生变化，当前 APK 尚未重新采集 `home_top`。不能把 83 号结果套用到当前源码；恢复后必须先重采 `home_top`。
- **5 项 Compose 测试的通过属于 20:37 的构建。** 89 号 `OK (5 tests)`、12.912 秒，对应 build 87/88 的测试 APK；之后首页和抽屉源码改变，测试文件没有再次运行。测试本身直接使用 `YimuAddEditScreen`，与首页抽屉改动关联较小，但当前构建仍需重新运行并记录。
- **原版临时模板尚未删除。** 现象：原版 `module_bills` 中存在 `食品餐饮-早餐 12.34`，119 号非空抽屉截图使用该模板。影响：原版用户数据多一条模板，不涉及账单。正确做法：从原版模板管理页删除该项，重新核对账单仍为 1974 条；不得删除其他模板。
- **validation 测试模板尚未删除。** 同一条模板存在于 validation 数据库；删除用 `TemplateFixtureInstrumentationTest#deleteTemplate`，并重新核对 validation 交易仍为 1974 条。
- **夜间资源切换未实现。** `MainActivity` 仍是 `ComponentActivity`，只向 `BookkeepingTheme` 传入 darkTheme；`values-night`、night drawable 和原生 XML 不会随 `ThemeMode.DARK` 切换。风险：步骤 12 的夜间截图会大面积失败。正确做法：按批准计划在资源层应用夜间配置，再采集。
- **旧 pause 的背景结论已过时。** `work/logs/yimu-ui-reconstruction-s11/pause/README.md` 中“背景渲染假设被推翻、bg_main 铺满无法复现参考”的结论与增量事实 C 及当前 83 号通过结果矛盾；以 1974 条数据、当前参考截图和 83 号比较为准，交接后不需要再按旧结论排查背景。
- **参考数据可能再次变化。** 原版应用仍可能新增账单；恢复后每次采集前运行 `check_reference_month.py`。当前参考数据为 1974 条、2026-10 月 282.00/4053.44/-3771.44；变化时必须重新导出并导入。
- **旧阶段未解决项继续有效：** CycleDate 独立持久化要求待核实；原版设置恢复来源仍缺失（S09 DataStore CorruptionException 的处理保留）；API 26 和平板没有运行证据，不扩展模拟器矩阵；全部页面、夜间、字体缩放、键盘遮挡和完整交付未验收。
- **用户包交付未开始。** 没有最终用户 APK、安装后数据保留报告或逐页日间/夜间报告；不能报告完成。

## 5. 环境与命令

- **运行环境：** Windows 10/11 工作站；Git Bash 和 PowerShell 7.6.5；Python 3.12；终端 Java 24.0.1；Gradle toolchain 为 JDK 21；Android SDK/build-tools 37；`GRADLE_USER_HOME=D:\dev-cache\.gradle`；Pillow 12.2.0、NumPy 1.26.4、PyYAML 6.0.3、xlrd 2.0.2 可用。没有使用系统临时目录，临时文件放在 `work/tmp`。
- **必要环境变量：** `work/ui-harness/Run-S11Command.ps1` 把 `TEMP`、`TMP`、`TMPDIR` 设为 `work/tmp`，设置 `PYTHONUTF8=1` 和 `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=D:/github-repository/smart-record/work/tmp`；Gradle 使用 `GRADLE_USER_HOME=D:\dev-cache\.gradle`。本文件不记录任何凭据。
- **设备：** 小米 15，序列号 `95f79212`，Android 16/API 36/arm64，1200×2670，密度 520，字体缩放 1.0，zh-CN，手势导航。动画缩放三项在暂停时已恢复 1.0；当前前台为系统桌面。
- **应用身份：** 用户包 `com.nonen.Bookkeeping`（0.2.3，versionCode 5）；参考包 `com.wangc.bill`（6.1.8，versionCode 378）；validation 包 `com.nonen.Bookkeeping.uivalidation`。
- **实际执行过的命令：**
  - 构建和安装通过 `work/ui-harness/Run-S11Command.ps1`、`Install-S11Validation.ps1` 记录。完整 JVM、Lint 和两 APK 构建在 80 号日志成功；之后的抽屉修改使用 `:app:assembleDebug :app:assembleDebugAndroidTest -PyimuUiValidation=true` 构建，最新 150 号成功。45、50、54、58、62、68、72、76、80、84、87、91、94、113、120、121、122、127、132、136、139、142、145、150 号日志可查。
  - 5 项 Compose 操作测试：`adb -s 95f79212 shell am instrument -w -r -e class com.nonen.Bookkeeping.ui.YimuAddEditUiInstrumentationTest -e source_sha256 4ef7ce3c6ee0d466bbcce7a7c72f44007aea77a2a452410c3bc97de17bca7f43 com.nonen.Bookkeeping.uivalidation.test/androidx.test.runner.AndroidJUnitRunner`，89 号 `OK (5 tests)`。
  - 诊断测试：`ComposeHostDiagnosticTest`，86 号 `OK (1 test)`。
  - 模板夹具：`TemplateFixtureInstrumentationTest#insertTemplate`，124 号 `OK (1 test)`；删除方法已写好，尚未执行。
  - 截图与比较：`python work/ui-harness/capture.py --app reference --only ...`、`--app validation --only ...`、`python work/ui-harness/diff.py --only ...`；参考数据预检 `python work/ui-harness/check_reference_month.py --config work/ui-harness/screens.yaml --xml work/ui-capture/ref/home_top.xml --month 2026-10`，21:19 退出 0。
  - 原版临时模板通过 `ui_action.py`、`window_action.py` 和 `adb shell input tap` 在真机创建；模板值已记录。采集完成后尚未删除。
  - 设备动画恢复：`adb shell settings put global animator_duration_scale 1.0`、`transition_animation_scale 1.0`、`window_animation_scale 1.0`。
- **下一阶段建议命令（在用户恢复实施后执行）：**
  - 先按第 7 节核查工作区和数据；完整构建使用 HANDOFF 第 5 节的环境变量和 `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest '-PyimuUiValidation=true' --console=plain --max-workers=2`。
  - 安装用 `pwsh -NoProfile -File work/ui-harness/Install-S11Validation.ps1`。
  - 采集与比较按 `screens.yaml` 的页面分组执行；长时间序列不要依赖同一次 validation 采集中的状态保持，状态敏感的页面单独调用。
  - 用户包最终构建不传 `-PyimuUiValidation=true`；安装前运行 `python work/ui-harness/device_backup.py --output work/backup/<新目录>`，核验身份、签名和数据后再 `adb install -r`。
- **禁止写入凭据：** 本文件不记录任何密钥、Token 或账号凭据。

## 6. 验证状态

### 已验证

- **S11 前段步骤 1 至 7、9（历史结果，对应 18:40 前的构建）：** 三处构建阻塞修复、模板管理页、删除链路、首页字号与账户区域修正、完整构建与安装、五批仪器测试通过、27 个必需控件有实现。证据：`work/logs/yimu-ui-reconstruction-s11/pause/README.md` 和 01 至 44 号日志；165 项 JVM 测试 0 失败，Lint 0，两个 APK 构建安装成功；仪器批次为导航 1 项、分类模型 2 项、统计账单 2 项、日期模板 4 项、真实数据与完整备份 12 项。该结果适用于对应构建，不自动适用于当前源码。
- **步骤 8 首页（历史结果，对应 80 号构建）：** 83 号 `diff.py --only home_top` 退出 0，整体差异 0.0190%，最差 64×64 区块 3.47%，bounds 33 项通过，纯色区域 220 项通过。关键修正和证据在 45 至 83 号日志、49 号 dexdump。当前 APK 已变化，需重新采集。
- **步骤 8.5 Compose 操作（对应 20:37 构建）：** 86 号诊断 `OK (1 test)`；89 号 `YimuAddEditUiInstrumentationTest` `OK (5 tests)`，12.912 秒；真实 Room、DataStore、`source_sha256` 校验通过。当前源码未重新运行。
- **validation 模板夹具：** 124 号 `OK (1 test)`；validation 数据库交易 1974 条，模板写入成功。
- **参考数据与截图身份：** 1974 条、支出 `-4355617`、收入 `3201248`；`ref/manifest.json` 与 `impl/manifest.json` 的 dataset 身份一致；21:19 月度预检退出 0。该预检使用 19:25 的参考 `home_top.xml`；参考应用在 21:05 的采集没有重新显示月汇总，恢复后必须重新采集 `home_top` 并再次预检，不能把 21:19 的结果当作现场最新数据。
- **参考侧采集：** `home_top`（19:25）、`home_scrolled`（21:05，慢速拖动）、`home_drawer` 非空（20:57，119 号）、`account_book_popup`（20:42）、`import_export`（更早）。参考截图仍属于参考 APK `e5ec945a...`。
- **validation 侧采集：** `home_top`（20:34，旧 APK）、`home_scrolled`（21:11，当前 APK 记录）、`home_drawer`（21:07，旧 APK 记录，当前 manifest 下记录与新 APK 不一致）、`account_book_popup` 缺失。
- **最新比较：** 153 号 `home_scrolled` 退出 1，整体 2.69%、最差区块 29.32%；83 号 `home_top` 通过但属于旧 APK；`home_drawer` 21:06 的有效比较约 2.75%、最差区块 100%，报告被后续失败运行覆盖，未形成独立日志；`account_book_popup` 没有 validation 截图，不能比较。
- **用户包状态：** `lastUpdateTime` 2026-10-18 22:31:25，与 S11 续跑提示词要求一致；版本 0.2.3/versionCode 5；S11 未安装或覆盖。用户交易数据在 S11 未重新读取，安装前必须重新备份并核对，不能把 S10 的 1970 条当作当前值。
- **设备状态：** 动画缩放三项 1.0；前台系统桌面；没有运行中的仪器进程；validation 包最后一次安装时间为 21:14:33。

### 未验证或风险

- 当前源码的完整 JVM、Lint 和两 APK 构建没有在抽屉改动后重新执行；最新 150 号只证明 assemble 成功。风险等级：中。
- 当前 APK 的 `home_top` 没有重新采集和比较；抽屉改动后的首页可能存在回归。风险等级：高。
- 当前 APK 的 5 项 Compose 操作测试没有重新运行；测试源码未变，但应用依赖和首页源码已变。风险等级：中。
- `home_scrolled` 当前 2.69%/29.32% 不通过；`home_drawer` 当前采集失败，尚未形成当前 APK 的有效比较；`account_book_popup` 未采集。步骤 10 未完成。
- 其余 25 个二级页面、夜间主题、字体缩放、键盘遮挡、导入导出/备份界面、用户包交付和完整数据保留均未验收。
- 原版临时模板和 validation 模板尚未删除；原版账单数最后一次核验为 1974，删除模板后必须再次核对。
- 参考数据在恢复后可能变化；每次采集前必须预检。原版设置恢复来源、CycleDate 独立持久化要求仍未解决。
- 当前 `report.json` 只保留 153 号 `home_scrolled` 的结果；`home_top` 的证据在 83 号日志，`home_drawer` 的 2.75% 数字只在暂停 README 中，不能把三者混为同一构建。

## 7. 下一阶段交接

### 第一项操作

1. 在 `D:\github-repository\smart-record` 读取本文件、`work/tasks/yimu-ui-reconstruction.md` 顶部、`work/logs/yimu-ui-reconstruction-s11/progress.md` 和 `work/logs/yimu-ui-reconstruction-s11/pause-20261019-2115/README.md`；运行 `git status --porcelain -uall`，对照 `work/logs/handoff-s11/source-scope.txt` 与 `pause-20261019-2115/source-sha256.csv` 核查用户期间的改动。
2. 运行 `git rev-parse --show-toplevel`、`git branch --show-current`、`git rev-parse HEAD`；记录本次 S11 交接提交号，确认该提交只包含 `HANDOFF.md`，写作前基准 HEAD 为 9251f8c；确认 `git diff --cached` 为空。
3. 核验用户包 `lastUpdateTime` 仍为 2026-10-18 22:31:25；不要安装用户包。运行 `check_reference_month.py`，确认 1974 条和 2026-10 月三项金额；出现变化时先重新导出并导入参考数据。
4. 处理 validation `home_drawer` 长按失败：先启动 validation 主界面，等待首页列表出现并截图确认前台；单独运行 `capture.py --app validation --only home_drawer`。若仍失败，检查 `_long_press_debug.png`（设备 external-files）和长按前的列表位置，改为滚动到顶部后长按或对工具栏新增按钮增加长按；不要放宽必要控件检查。
5. 重新构建并采集当前 APK 的 `home_top`，确认抽屉改动没有引入首页回归；在此之前不把 83 号结果当作当前结果。

### 待完成事项（按依赖顺序）

1. **步骤 10：** 修复 `home_drawer` 采集；比较并修正抽屉卡片、面板和悬浮球；实现原生账本弹窗并采集 `account_book_popup`；重新采集 `home_scrolled` 并修正滚动位置；删除原版临时模板并核对 1974 条；删除 validation 模板并核对 1974 条。
2. **步骤 11：** 按四组完成 25 个页面：记账页 8 页、日历与资产 3 页、统计与预算 5 页、我的与设置 9 页。每组执行参考采集、必要 bounds 与掩膜核查、validation 采集、比较、修复、重建和重采。
3. **步骤 12：** 把 `ThemeMode` 接到 Android 资源层，补齐夜间 `values-night`、night drawable 和系统栏样式；按批准的方案临时切换原版夜间主题，采集 29 页夜间参考，validation 采集并比较，结束后恢复原版主题和系统状态。
4. **步骤 13：** 构建生产包；用 `aapt2`/`apksigner` 核验身份和签名；安装前用 `device_backup.py` 备份用户数据；`adb install -r` 后与安装前最新备份核对记录数、支出、收入和数据库完整性（S10 备份为 1970 条、支出 -43521.15、收入 32012.48，仅作历史参考，不能替代新的备份核验）；冷启动和五标签烟测；记录 APK SHA-256 和备份路径。
5. **步骤 14：** 写完整用户包与 29 页日间/夜间报告；整体重写 HANDOFF；更新任务与进度；仅提交 HANDOFF。

### 验收标准

- `home_top`、`home_scrolled`、`home_drawer`、`account_book_popup` 在当前 APK、当前参考数据和当前截图标准下全部通过；原版临时模板删除后账单仍为 1974 条；validation 模板删除后交易仍为 1974 条。
- 29 个页面日间全部达到整体差异不超过 1.5%、最差 64×64 区块不超过 6%、关键 bounds 不超过 3px、纯色区域通过；夜间 29 页达到同一标准。
- 当前源码通过完整 JVM、Lint 和独立包 assemble；5 项 Compose 操作测试在真实 Room/DataStore 上通过；其余业务仪器测试重新运行。
- 生产包含 `com.nonen.Bookkeeping`，签名和身份核验通过；安装前后用户数据一致；交付 APK、报告和日志。

### 需要用户裁决的事项

- 当前没有新的裁决事项。用户已批准 S11 计划、临时原版模板、原版夜间主题临时切换以及交接提交。
- 若恢复时选择改变参考模板取证方式、跳过某个页面或调整截图阈值，必须先取得用户明确同意；不得自行放宽验收。

### 禁止事项与本阶段排除范围

- 未收到恢复实施指令前，不修改应用源码、不构建、不安装、不操作手机；本次交接只写并提交 `HANDOFF.md`。
- 不对用户包运行会卸载应用的仪器测试；不清除或覆盖个人数据；参考导入、截图和仪器测试限定 `.uivalidation`。
- 不使用 Git 恢复代码；不读写系统临时目录；不使用 mock、伪造数据/截图、空控件清单或放宽阈值报告通过；源码使用文件编辑工具修改。
- 不新增网络权限或账号、云服务、会员、语音、自动记账、桌面微件等既定业务；不启动或下载模拟器；不重启手机、不修改网络、系统账号和权限。
- 原版只允许删除本次创建的临时模板；不得删除其他模板、账单或修改账户余额。

### 继续所需的材料位置

- 当前实现与全部未提交源码：`D:\github-repository\smart-record` 工作区；只提交 HANDOFF 后仍保留。
- 交接审计：`work/logs/handoff-s11/`（git-status、diff-stat、log、baseline、source-scope、S10 提交记录）；S11 暂停快照：`work/logs/yimu-ui-reconstruction-s11/pause-20261019-2115/`；前段暂停快照：`work/logs/yimu-ui-reconstruction-s11/pause/`。
- 命令日志：`work/logs/yimu-ui-reconstruction-s11/` 的 01 至 153 号；本会话日志从 45 号开始，42 至 44 为上一会话末尾；交接相关为 83、86、89、119、124、152、153。
- 截图与差异：`work/ui-capture/ref/`、`work/ui-capture/impl/`、`work/ui-capture/diff/`、`work/ui-capture/report.json`、两侧 `manifest.json`。
- 参考数据与证据：`work/ui-fixtures/yimu_all_20261019_1922.xls`、`work/ui-evidence/`、`work/ui-evidence/apktool/res/`、`work/ui-evidence/jadx/sources/`、`work/ui-evidence/yimu-system-backup.zip`、`reference_apk/base.apk`。
- 产物：`work/artifacts/yimu-ui-validation.apk`；测试 APK 在 `app/build/outputs/apk/androidTest/debug/`；用户备份在 `work/backup/`。
- 既有计划与任务：`docs/plans/yimu_exact_ui_reconstruction_plan.md`、`docs/plans/yimu_offline_rebuild_antigravity.md`、`work/tasks/yimu-ui-reconstruction.md`；两者的当前状态以本文件和 `work/logs/` 为准。
- 仅本机存在的计划与原文：`C:\Users\29602\.qoder\plans\shining-dale-drake.md`、`D:\桌面\temp\smart-record-S11-续跑提示词.md`。
- 旧版 HANDOFF 通过 Git 历史读取：`git show 9251f8c:HANDOFF.md`。`work/` 被忽略，不能从 Git 提交恢复，必须保留当前目录。
