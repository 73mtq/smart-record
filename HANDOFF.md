# HANDOFF — S11 一木记账界面复刻（2026-10-20 续跑交接）

## 1. 状态

- **阶段：S11；阶段目标：** 在 S10 交接的未提交工作区上完成一木记账界面复刻：29 页日间验收、夜间主题、生产包交付和阶段交接。阶段编号依据：当前 HEAD 提交 `b594e02` 的信息 `handoff: S11 阶段交接`、旧 `HANDOFF.md`、`work/tasks/yimu-ui-reconstruction.md` 顶部的 S11 记录，以及 `C:\Users\29602\.qoder\plans\rising-fjord-asp.md` 的 S11 标题；编号确定，无歧义。旧版 HANDOFF 存在于 HEAD，本文件为整体重写，**不是首次创建**。
- **当前状态：进行中，本阶段未完成，无外部技术阻塞。** 步骤 10 已完成（首页四页重采通过，模板清理完成）；步骤 11 组 A 6/8 通过、组 B 日历两页与资产页整体 ≤1.5% 但各有单 tile 残余、组 C 五页已原生化（normal/month/year/budget 整体 ≤1.5%，`stats_self` 2.48% 未通过，year 等仍有纯色残余）、组 D 两页通过且其余部分未完成；步骤 12 夜间、步骤 13 生产交付、步骤 14 最终报告均未开始。没有完成全部验收的用户 APK。
- **仓库：** `D:\github-repository\smart-record`；分支 `main`；核查时间 **2026-10-20 17:12（Asia/Shanghai）**；写作前 HEAD 为 `b594e02ee73176f8bdfcd4edd3e1dd24468f8993`（`handoff: S11 阶段交接`）。暂存区为空；工作区 57 个跟踪文件修改、2 个删除、404 个未跟踪文件；`git status --short` 344 行；工作区 diff 为 59 文件、+2408/−2060 行。`git diff --cached` 为空。
- **阶段起点与证据范围：** 当前 HEAD `b594e02` 是上一份 S11 交接，之后没有源码提交；本阶段源码全部在工作区。10-20 上午至 15:23 的续跑状态见 `work/logs/yimu-ui-reconstruction-s11/pause-20261020-1523/README.md`；本轮相对 15:23 快照的源码变化为 **12 个修改、31 个新增**（用该快照的 `source-sha256.csv` 逐文件哈希比对得到，见第 2 节）。12:54 快照 `pause-20261020-1254/README.md` 记录了步骤 10 与组 A 的完成情况。更早的 S09/S10/S11 前段改动不得归入本轮，见旧版交接与 `pause-20261019-2115/`。
- **本阶段任务要求及来源：**
  - 用户原始 S11 续跑提示词：`D:\桌面\temp\smart-record-S11-续跑提示词.md`（本机存在，7529 字节）。要求：从落盘证据续跑、控制上下文、优先按当时 1974 条增量事实处理、完成步骤 8 与步骤 10–14、遵守 HANDOFF 长期决定。该文件中的 1974 条数据已被后续 1977 条快照取代。
  - 用户批准的计划：`C:\Users\29602\.qoder\plans\shining-dale-drake.md`（S11 原始计划）、`ember-narrows-stork.md`（S11 收尾计划）、`rising-fjord-asp.md`（2026-10-20 续跑计划，已批准）。计划要求完成 29 页日间/夜间、交付和交接；不得降阈值。
  - 用户当前交接指令来自本会话消息，未另存文件：核查 git 证据、整体重写 `HANDOFF.md`、只提交该文档、不修改其他文件、保留暂存状态。
  - 用户明确决定（2026-10-20）：日期/资产弹窗等 1px 字体/系统阴影差异记录在案并继续后续；用户每天使用原版，参考账单漂移只记录当次快照，不反复重导、不阻塞 UI 验收。
  - 用户要求与 AI 建议的区分：“续跑、完成步骤 10–14、记录 1px 差异后继续、本次只提交 HANDOFF”是用户要求；“原生 XML/RecyclerView、MPAndroidChart 配置、原生统计壳、折叠日历补 12px、设置 SwitchButton 配色”等是 AI 提出并已执行的实现方案；尚未实现的计划仍属建议。
- **数据与产物：**
  - 参考快照 `work/ui-fixtures/yimu_all_20261020_1203.xls`：1977 条，支出 `-4357467` 分，收入 `3201248` 分，SHA-256 `bb21aa69f18d34a522f9ed86babef4faad4616d67dabe96c39172cd2df473c4f`；2026-10 为 282.00/4071.94/−3789.94。参考与 validation 的 `manifest.json` 身份一致；validation 交易 SHA-256 `5e8f3562df5bf4d0acf8fb059683d148d9d1580c8e2d4d72c412d98f9a22a2a0`（78 分类、0 资产、1 账本）。
  - 当前 validation APK：`work/artifacts/yimu-ui-validation.apk`，SHA-256 `bbf43ea4331163a41a29bb83265c7775cd2924e8385d3cc3240639ae7379edf3`；设备内同一哈希，`lastUpdateTime=2026-10-20 16:59:40`。测试 APK：`app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`，SHA-256 `7ed80c218826f20b6ea958e0dca38e9a1b1678ed32cd3a5706cc37b1afe4b37c`。
  - 用户包 `com.nonen.Bookkeeping`：0.2.3/versionCode 5，`lastUpdateTime=2026-10-18 22:31:25`；本阶段未安装、未覆盖、未读取用户交易数据。参考包 `com.wangc.bill`：6.1.8/versionCode 378。
  - 最新日志 870（`savings_goal` 比较通过）；下一次日志从 **871** 开始。设备动画缩放已恢复 1.0，validation 已停止，前台系统桌面。
- **检查点：** `work/logs/yimu-ui-reconstruction-s11/pause-20261020-1700/README.md`（本轮结果与下一步）、`pause-20261020-1523/`、`pause-20261020-1254/`、`groupA/B/C/D-known-differences.md`。

## 2. 变更文件清单

本阶段没有提交任何源码；本节只列可确认归属的变更。工作区还包含 S09、S10、S11 前段的累计改动，不能全部归入本阶段。

### 2.1 本轮（15:23 快照之后）的源码变更：12 修改 + 31 新增

证据：`pause-20261020-1523/source-sha256.csv` 逐文件哈希比对；对应报告与日志见括号。

**日历（4 修改）：**
- `app/src/main/java/com/nonen/Bookkeeping/ui/calendar/YimuCalendarView.kt`：移除默认未开启的热力底色；按原版 ActivityMonthView 实现日号颜色（工作日黑、周末主色、未来色）、9dp 金额字号、列表金额上移；周视图未来日期用 #999、农历上移 1px。证据：`report-calendar-month-final2.json`（0.4534%，bounds/纯色通过）、`report-calendar-week-final.json`（0.2446%），日志 697–702。
- `app/src/main/java/com/nonen/Bookkeeping/ui/components/YimuCalendarNativeView.kt`：折叠时隐藏 FAB、折叠态内容补 12px 占位；接月汇总颜色。证据同上。
- `app/src/main/res/layout/view_yimu_calendar_native.xml`：折叠占位、月收支颜色、布局高度/ID。证据同上。
- `app/src/main/res/layout/item_yimu_calendar_bill.xml`：金额容器补 5dp 第二行间距以匹配原版居中。证据同上。

**统计与预算（修改 2 + 新增 17）：**
- 修改 `app/src/main/java/com/nonen/Bookkeeping/ui/screens/YimuStatisticsScreen.kt`：改为原生统计壳并计算日常/月/年/自定义数据；修改 `BudgetScreen.kt`：改为原生预算页并保留原有对话框回调。
- 新增 `ui/components/YimuStatisticsNativeView.kt`（四页签、月/年选择条、汇总卡、柱/线/饼图、年度热力集合）、`ui/components/YimuHeatMapMonthView.kt`（原版 8dp 圆点/3dp 间距热力）、`ui/components/YimuBudgetNativeView.kt`、`ui/chart/RoundedBarChartRenderer.java`（原版 2dp 圆角、0.5 条宽）。
- 新增布局 `yimu_statistics_host.xml`、`yimu_statistics_normal/month/year/self.xml`、`item_month_or_year.xml`、`item_heat_map.xml`、`yimu_budget_manager.xml`、`layout_common_tip.xml`；新增 `fade_asset.xml`、`fade_pay.xml`、`fade_income.xml`、`budget_progress_track.xml`、`values/styles.xml`（autoCheckBox）。
- 证据：`report-stats-normal5.json`、`report-stats-month6.json`、`report-stats-year2.json`、`report-stats-self4.json`、`report-budget6.json`，日志 704–815。

**组 D（修改 4 + 新增 13）：**
- 修改 `MyScreen.kt`（云同步文案改红、数据行动间距）、`YimuSettingsPage.kt`（开关配色/返回图标/显示原金额开启、导入导出与备份分支改为原生 AndroidView）、`PeriodicScreen.kt`、`SavingsGoalScreen.kt`。
- 新增 `ui/components/YimuSettingsLists.kt`（导入导出/备份原生绑定）、`YimuPeriodicNativeView.kt`、`YimuDreamNativeView.kt`；新增布局 `yimu_cycle_manager.xml`、`yimu_dream_list.xml`、`yimu_import_export.xml`、`yimu_backup.xml`；新增图标 `ic_bar_more`、`ic_date_setting`、`ic_dream_tip`、`ic_more_setting`、`ic_tip`、`ic_top`。
- 证据：`report-my3.json`、`report-settings5.json`、`report-importbackup2.json`、`report-periodic2.json`、`report-dream3.json`，日志 818–870。

**全局资源（修改 2 + 新增 1）：** `values/colors.xml`（redLight/greenLight）、`values/yimu_ids.xml`（统计/日历/折叠占位 ID）、`values/styles.xml`（新增）。
**仅本机、被 Git 忽略：** `work/logs/.../groupB/C/D-known-differences.md`、`pause-20261020-1700/`、`work/tasks/yimu-ui-reconstruction.md` 与 `progress.md` 的本轮更新、`work/ui-capture/` 的新报告/截图。

### 2.2 同阶段但早于 15:23 的改动（未提交，继承自 15:23 快照）

- 组 A：`YimuAddBillView.kt`、`YimuNumberKeyboardView.kt`、`YimuDateDialog.kt`、`YimuAddEditScreen.kt`、`YimuAssetDialog.kt`、相关布局/ID/依赖；结果 6 页通过、两个弹窗已知 1px 差异。证据：`pause-20261020-1523/README.md`、`report-groupA-final.json`。
- 组 B：`YimuAssetNativeView.kt`、`YimuAssetBannerLayout.kt`、`YimuAssetContent.kt`、`YimuCalendarNativeView.kt`、`DateMonthView.kt`；资产页 0.2695%。证据同上与 `report-asset-final2.json`。
- 12:54 前：步骤 10 首页四页重采通过、模板清理、1977 数据导入。证据 `pause-20261020-1254/README.md`、日志 310/312/320；该通过结果对应当时的 APK，不是当前 APK。

### 2.3 更早的累计未提交改动

- 57 个跟踪修改和 2 个删除中包含 S09、S10、S11 前段的业务/界面改动（Room、仓库、解析、导出、设置、主题、测试等）。归属证据：旧版 HANDOFF 第 2 节、`pause-20261019-2115/source-sha256.csv`、`work/logs/handoff-s11/source-scope.txt`。这些不属于本轮，不得当作本轮成果。
- 404 个未跟踪文件中包含大量原版图标、布局、字体和测试资源；未逐项列出。原版图标来自 `work/ui-evidence/apktool/res/`，测试资源与 `app/src/androidTest/` 为 S11 前段。

### 2.4 来源待确认/无关项

- 根目录 `--help`（0 字节，2026-10-20 12:00）是误建的空文件，已出现在 15:23 快照的 `git-status.txt`；未纳入任何构建，不得提交。因本次交接禁止修改其他文件，保留原样。
- 无暂存内容；未取消或改变任何其他文件的暂存状态。

## 3. 关键决策与理由

### 3.1 长期决定（继承旧版 HANDOFF，不得推翻）

- **【长期】参考与组件：** 依据原版 APK、反编译源码和同一台真机；使用 Material Components 1.10.0、CalendarView 3.7.1、MPAndroidChart v3.1.0、SwitchButton 2.1.0；页面使用 Compose 与 AndroidView，复杂组件复用原版测量，不自行重画。
- **【长期】原版资源：** 保留 62 个 lineIcon 和 78 项分类映射；原版备份、私有数据库和凭据保持本地隔离，凭据不加入应用资源。
- **【长期】隔离与验证：** 用户包保留个人数据；导入、仪器测试和覆盖恢复限定 `com.nonen.Bookkeeping.uivalidation`；使用真实 Room/DataStore/模型；用户账户余额保持业务含义，不用截图调整；DAO 替身不能作为事务验收。
- **【长期】截图标准：** 必要控件有效且数量大于零；整体 ≤1.5%、任意 64×64 ≤6%、关键 bounds ≤3px、单通道容差 8、纯色区域单通道差 ≤2；保持现有掩膜和数据/环境核验；人工确认不能代替报告；APK 变化后必须重新采集。
- **【长期】页面状态：** 底栏五项；导航文字默认关闭，开启时 11sp；账本共享至首页、日历、统计和新增账单并纳入备份；模板抽屉 280dp，账本通过工具栏弹窗切换。
- **【长期】持久化：** Budget、CategoryBudget、BudgetSetting、Cycle、CycleLog、ModuleBill、Dream、DreamLog 独立生命周期；模板在 `module_bills`，愿望在 `dreams/dream_logs`，不向 transactions 增加标记替代实体。CycleDate 独立持久化要求仍待核实。
- **【长期】金额与迁移：** 金额 Double 与两位规则，键盘使用 BigDecimal；保留 Room 版本 6 和增量迁移；历史默认 bookId/assetId 为 1，历史记录可编辑。
- **【长期】分类：** 唯一索引 `(categoryId,parentId,categoryType)`；一级 ID 9 及其 11 项子分类为收入；唯一映射确定，多义分类保留二级 null 或独立 isCustom 业务 ID；全局 hide 与 hideBook 独立，SQLite hide 为 0/1，hideBook 为逗号分隔账本 ID；保存实际分类 ID 并更新商户记忆；删除自定义分类保留账单文字和金额。
- **【长期】资金：** 增删改和余额在 Room 事务内原子一致；保护默认现金账户 1；删除其他账户时重定向历史并更新名称；信用账户欠款为 `totalQuota-assetNumber`，超额余额计入资产；借入账户余额绝对值计入负债；净资产为总资产减负债；验证关联、余额、编辑删除对冲和统计。
- **【长期】统计：** 历史资产由当前余额逆算时点后全部资金变化，包含转账和手续费；一级/二级分类统一聚合，二级占比分母为所属一级总额。
- **【长期】本地模型：** 保留 `classification-model.ser`、`TimeExp.m`、LibLinear/HanLP，不重训不接云端；门槛 0.015；`食品餐饮-其他` 按 04:00–10:00/10:00–16:00/其余分早餐/午餐/晚餐；保留 `(块|元|点)([1-9一二三四五六七八九])[毛角]`→`点$2` 及常见动词/首尾单位规则；中文金额完整流程仍需验收。
- **【长期】周期与备份：** `cycle_{cycleId}_{yyyyMMdd}` 防重；事务内联动余额；23 列 Excel/CSV 与完整 ZIP 备份分别处理；完整备份含 manifest.json、data.json、SHA-256 前置校验，失败保护数据库。
- **【长期】业务范围：** 本地支出、收入、转账；债务为视觉占位；会员、账号、云服务、语音、自动记账、桌面微件及房贷车贷等既定业务不扩展；原版只允许新增一个临时模板用于取证，记录标识并删除本次新增项（本次已在 12:54 清理）。
- **【长期】采集与字符（S10 沿续）：** 原版外部 shell 冷启动、持续式 UIAutomator 读取真实控件；文字使用 AppCompat 主题与整数像素；首页原生 XML/RecyclerView；JVM JSON 使用真实 org.json；TodayWatcher 等待注册；金额沿用 Format.kt；Compose 避免依赖不可观察的 Locale.getDefault()。

### 3.2 本阶段实际采用的决定

- **Native-first 扩展：** 日历、统计、预算、组 D 空态页均改为原版 XML + AndroidView/MPAndroidChart；原因是 Compose 与原版字体/测量持续产生可测差异，首页/记账户已证明原生路线可达到 0.01% 级。
- **日历：** 参考默认状态未开启热力，故不绘制热力底色；日号颜色按原版 ActivityMonthView；折叠高度差用 3.7dp 占位而非直接平移内容（平移会破坏 CalendarLayout 折叠）；折叠时隐藏 FAB。
- **统计：** 用原版 MATERIAL_COLORS 调色板、2dp 圆角柱渲染器、9dp/8dp 字号与 fade_pay/fade_income/fade_asset 渐变；无共享成员时隐藏成员卡片；分类饼图用父分类显示名。
- **预算：** 无预算时支出/日均/剩余显示 0.00；趋势用双数据集渐变填充；开关/工具栏沿用原版布局。
- **组 D：** 空态页直接复用原版 `activity_cycle_manager.xml`/`activity_dream_list.xml`，隐藏空态外的工具栏图标和提示条；导入导出/备份复用原版列表布局并接现有回调；设置页开关使用原版灰/绿状态色。
- **采集稳定性：** 导入导出/备份原生页禁用 ScrollView 滚动条以消除双帧不稳定；采集前动画缩放设为 0，结束后恢复 1.0。该问题只在采集工具侧，不改应用行为。
- **增量编译异常：** 多次增量构建后出现 `localDateOf`/`YimuTopAppBar` 等“已存在却未解析”错误；`clean` 后消除（日志 765–767、837）。后续遇到同类错误先 clean 构建，不据此改源码。
- **数据漂移：** 用户每日使用原版，参考数据只按 1977 快照记录；不反复重导，不阻塞 UI 验收；交付时另备份用户包。

### 3.3 有证据的失败/否决方案

- `createComposeRule()`、`ActivityScenario`、预启动/替换宿主均被 MIUI 后台启动限制拦截（result code 102）；改用空 Compose 规则 + shell 启动（旧交接证据 86/89 号）。
- 整屏 `ModalNavigationDrawer` 与原版抽屉位置不符；快速滑动导致滚动位置不稳定；均被否决并改为工具栏下方面板/慢速拖动。
- 日历热力底色在参考默认状态不存在；尝试保留会制造大块差异，已移除。
- FAB 图标通过 `maxImageSize`/`InsetDrawable` 微调对截图无效果（日志 647–658）；剩余 2–3px 图标边缘作为已知差异记录，不再盲调。
- 导入导出/备份的滚动条导致采集不稳定；禁用滚动条后双帧稳定，但左侧滚动条与原版存在小差异。
- 预算趋势最初用粉色单填充，与原版暖灰渐变不符；改为 fade_pay+fade_income 双渐变后差异降至 0.33%。

### 3.4 已批准与待批准

- 已批准：S11 三份计划（`shining-dale-drake.md`、`ember-narrows-stork.md`、`rising-fjord-asp.md`）、记录 1px 差异后继续、原版夜间主题临时切换（尚未执行）、只提交 HANDOFF 的交接方式。
- 待用户裁决：是否为剩余未过页继续像素收敛直至全部通过，还是按已记录差异进入夜间/交付；是否接受 `stats_self`、`stats_year`、`my`、`settings`、分类管理/自定义分类/搜索的当前状态；是否删除根目录误建的 `--help`。

## 4. 已知问题与失败尝试

- **`stats_self` 2.48%，最差 tile 88%，3 个纯色失败（`report-stats-self4.json`，日志 812–815）。** 现象：收支走势的收入线与折线位置和参考不同。原因未完全确认；已确认柱状图/日历数据总量与参考一致。正确做法：继续对照原版 `StatisticsSelfFragment`/i4 的 W2/B2 与 `Q0` 配置，取线形/收入线断点证据；不得改阈值。
- **`stats_year` 整体 1.39% 但 5 个纯色失败、最差 tile 35%（`report-stats-year2.json`）。** 来源为年份条字体、热力点边缘。正确做法：逐点比较圆点直径/间距/强度与年份条字形；当前未解决。
- **统计单 tile 残余：** `stats_normal` 7.25%（柱值字体边缘，`report-stats-normal5.json`）、`stats_month` 37%（底部饼图扇区边界，`report-stats-month6.json`）、`budget` 36%（趋势填充边界，`report-budget6.json`）。整体均 ≤1.5%、bounds 通过，但单 tile 未过。
- **日历：** `calendar_month` 0.4534%，唯一超阈值 tile 为 FAB 图标边缘 20.8%；`calendar_week` 0.2446%，唯一超阈值 tile 为选中日“20”字形 12.8%（`report-calendar-month-final2.json`、`report-calendar-week-final.json`）。按用户决定记录，不能写成完全通过。
- **组 A/资产：** 日期弹窗 0.5735%/22.36% tile、资产弹窗 0.1885%/12.08% tile、资产页 0.2695%/29.3% 标题 tile；均为 1px 字体/阴影类残余（`report-groupA-final.json`、`report-asset-final2.json`、15:23 README）。这些报告对应旧 APK。
- **组 D：** `my` 3.58%（12 个纯色失败，头图/图标/行距）、`settings` 4.35%（1 个纯色失败，SwitchButton/返回图标）、`import_export` 0.926%、`backup` 0.890%（各 1 个纯色失败，返回箭头灰度/位置）。`periodic` 0.008%、`savings_goal` 0.007% 通过。证据：`report-my3.json`、`report-settings5.json`、`report-importbackup2.json`、`report-periodic2.json`、`report-dream3.json`。
- **未完成页面：** `category_manage` 仍为旧 Compose 页面，采集失败（日志 829）；`rules` 采集失败（日志 830）；`search` 采集不稳定（日志 816）。原因未确认；下一步先检查 `screens.yaml` 的导航文本/控件和页面标题，再复刻原版布局。
- **采集不稳定：** 导入导出曾因 ScrollView 滚动条双帧不稳定（日志 851/854），禁用滚动条后恢复（日志 857）。search 的不稳定未解决。
- **当前 APK 未逐页重采：** 所有页面报告对应各自构建（日历 697、normal 724、month 761、year 771、budget 804、self 812、my 825、settings 846、import/backup 855、periodic 859、savings 867）；当前 APK `bbf43e` 只有 savings_goal 在最终构建上重新验证。不能把历史报告当作当前 APK 全部通过。
- **当前源码未重新跑完整 JVM/Lint：** 本轮只反复执行 `assembleDebug`/`assembleDebugAndroidTest`；唯一一次 clean assemble 在 16:45（日志 837）成功，但早于最后几处组 D 源码改动。165 项 JVM 0 失败、Lint 0 是旧构建结果（旧交接 80 号），不适用于当前源码。
- **5 项 Compose 操作测试** 的 `OK (5 tests)` 属于 20:37 构建；之后源码变化，未重跑。真实 Room/DataStore 测试仍以旧结果为准。
- **旧交接遗留：** CycleDate 独立持久化要求待核实；原版设置恢复来源缺失（S09 DataStore CorruptionException）；API 26/平板无运行证据；字体缩放/键盘遮挡未验收；用户包交付未开始。原版/validation 临时模板已在 10-20 12:54 复核中清理（`pause-20261020-1254/README.md`），不再列为未解决。旧交接的 home_drawer/account_book_popup/home_scrolled 也已在同日步骤 10 复核通过（首页四页 310/312），但当前 APK 未重采首页，不能套用旧结果。
- **参考数据可能继续漂移：** 原版由用户每日使用；本次快照 1977 条，未再导出。下次采集前先核对 dataset/manifest，数据不一致时重新导出导入，不用界面改动掩盖。
- **根目录 `--help`：** 0 字节误建文件，已在 15:23 快照；不参与构建，未提交；本次交接不得删除其他文件，保留给下一阶段处理。

## 5. 环境与命令

- **运行环境：** Windows 10/11；Git Bash + PowerShell 7.6.5；Python 3.12；终端 Java 24.0.1；Gradle toolchain JDK 21；Android SDK/build-tools 37；`GRADLE_USER_HOME=D:\dev-cache\.gradle`；Pillow 12.2.0、NumPy 1.26.4、PyYAML 6.0.3、xlrd 2.0.2。临时文件放 `work/tmp`。
- **环境变量名：** `TEMP`、`TMP`、`TMPDIR`、`PYTHONUTF8`、`JAVA_TOOL_OPTIONS`、`GRADLE_USER_HOME`；不记录任何密钥/Token。
- **设备：** 小米 15，`95f79212`，Android 16/API 36/arm64，1200×2670，密度 520，字体 1.0，zh-CN，手势导航。当前动画三项 1.0、前台桌面、validation 已停止。采集时需临时设 0，结束后恢复 1.0。
- **应用身份：** 用户包 `com.nonen.Bookkeeping` 0.2.3/5（2026-10-18 22:31:25）；validation `com.nonen.Bookkeeping.uivalidation` 0.2.3-ui-validation/5（2026-10-20 16:59:40）；参考包 `com.wangc.bill` 6.1.8/378。
- **已执行命令（本轮）：**
  - 构建：`pwsh -NoProfile -Command "& '...Run-S11Command.ps1' -LogName '<n>-...log' -FilePath '...gradlew.bat' -CommandArguments @(':app:assembleDebug',':app:assembleDebugAndroidTest','-PyimuUiValidation=true','--console=plain','--max-workers=2')"`；日志 632–870。
  - 增量解析异常后用 `:app:clean :app:assembleDebug -PyimuUiValidation=true` 成功（837）；测试 APK 用 `:app:assembleDebugAndroidTest`（841）。
  - 安装：`pwsh -NoProfile -File work/ui-harness/Install-S11Validation.ps1`（842、851、860、864、868）。
  - 采集/比较：`python work/ui-harness/capture.py --app reference|validation --only <页>`、`python work/ui-harness/diff.py --only <页> --report <json>`；参考数据预检用 `check_reference_month.py`（本轮未重新执行，manifest 身份已核对）。
  - 设备：`adb shell settings put global animator_duration_scale/transition_animation_scale/window_animation_scale`；结束时恢复 1.0、强制停止 validation、回到桌面。
- **建议命令（下一阶段）：** 先 `git status --porcelain -uall`；再按需 clean 构建、`Install-S11Validation.ps1`；采集前设动画 0；用户包最终构建不传 `-PyimuUiValidation=true`，安装前运行 `python work/ui-harness/device_backup.py --output work/backup/<新目录>`，核验身份/签名/数据后再 `adb install -r`。
- **日志纪律：** 所有构建/安装/采集走 `Run-S11Command.ps1`，编号从 871 起，不复用；长输出入日志，对话只报结论。

## 6. 验证状态

### 已验证（当前构建/当前设备）

- 当前源码的最近一次 clean 构建是 16:45:45–16:46:34（日志 837，退出 0），对应当时的源码；`assembleDebugAndroidTest` 成功（841，退出 0）；此后 settings/import/backup/periodic/savings 的增量构建 846、850、855、859、863、867 均成功，最终安装 868（退出 0）。当前 APK SHA-256 `bbf43e...`，设备内哈希一致，`lastUpdateTime=16:59:40`。**当前源码尚未在最终改动后重新 clean 构建。**
- `savings_goal` 在当前最终构建（867/868）上采集并比较：整体 0.0066%，最差 4.81%，bounds/纯色通过（`report-dream3.json`，日志 869/870，退出 0）。
- 数据身份：参考/validation manifest 均为 1977 条、`bb21aa...` 源、2026-10 三项金额；validation 交易 SHA `5e8f...`。设备动画已恢复 1.0，validation 停止，前台桌面。
- 用户包未被本阶段安装或覆盖；版本/更新时间与旧交接一致。

### 历史结果（对应各自构建，不能视为当前 APK）

- 组 A：6 页通过（整体 0.009%–0.017%，最差 5.64%）；日期弹窗 0.5735%/22.36% tile、资产弹窗 0.1885%/12.08% tile；`report-groupA-final.json`。资产页 0.2695%/标题 29.3% tile；`report-asset-final2.json`。均见 15:23 README。
- 日历：month 0.4534%（报告 `report-calendar-month-final2.json`，构建 697，bounds/纯色通过，FAB tile 20.8%）；week 0.2446%（`report-calendar-week-final.json`，构建 697，bounds/纯色通过，选中日 tile 12.8%）。
- 组 C：normal 0.1066%（`report-stats-normal5.json`，构建 724，7.25% 字体 tile）；month 0.5991%（`report-stats-month6.json`，构建 761，饼图边界 tile 37%）；year 1.3896%（`report-stats-year2.json`，构建 771，5 个纯色失败，tile 35%）；self 2.481%（`report-stats-self4.json`，构建 812，**未通过**，tile 88%，3 个纯色失败）；budget 0.3313%（`report-budget6.json`，构建 804，tile 36%）。
- 组 D：my 3.577%（`report-my3.json`，构建 825，12 个纯色失败）；settings 4.3479%（`report-settings5.json`，构建 846，1 个纯色失败）；import_export 0.9257%、backup 0.8904%（`report-importbackup2.json`，构建 855，各 1 个纯色失败）；periodic **通过** 0.0078%（`report-periodic2.json`，构建 859）；savings_goal **通过** 0.0066%（构建 867）。
- 步骤 10：首页四页重采通过（310/312 号），模板清理完成，validation 1977 条（12:54 README）；对应当时 APK，当前 APK 未重采首页。
- 旧构建完整 JVM 165 项 0 失败、Lint 0；5 项 Compose 操作 `OK (5 tests)`（旧交接 80/89 号）；当前源码未重验。

### 未验证/风险

- 当前 APK 未重采的页面包括首页四页、组 A 全部、资产、日历、统计/预算、组 D 的 my/settings/import/backup/periodic；只有 savings_goal 是当前最终构建的通过证据。风险：后续改动可能引入回归，必须重采。
- `category_manage`、`rules`、`search` 没有当前有效截图/比较；采集失败或不稳定（日志 816/829/830）。
- 当前源码的 JVM/Lint/全部仪器测试未重跑；5 项 Compose 测试未在当前源码运行。
- 夜间主题未实施/未采集；production 包、签名、备份、安装后数据一致性、烟测均未开始。
- 参考数据会随用户使用变化；本轮未重新导出，1977 条仅代表 12:03 快照。
- 旧遗留：CycleDate 持久化、原版设置恢复来源、API 26/平板矩阵、字体缩放/键盘遮挡。

## 7. 下一阶段交接

### 第一项操作

1. 在 `D:\github-repository\smart-record` 读取本文件、`work/tasks/yimu-ui-reconstruction.md` 顶部、`work/logs/yimu-ui-reconstruction-s11/progress.md`、`pause-20261020-1700/README.md` 和 `groupD-known-differences.md`。
2. 运行 `git status --porcelain -uall`、`git rev-parse HEAD`、`git diff --cached`；确认新交接提交只含 `HANDOFF.md`，工作区源码仍保留，`--help` 等未跟踪项不动。
3. 先重建/安装当前源码（若遇增量解析异常先 `:app:clean`），设动画缩放 0，采集 `category_manage` 参考；对照 `screens.yaml` 的导航/控件失败日志修复采集动作，再按原生布局完成该页。

### 待办（按依赖排序）

1. **完成组 D：** `category_manage`、`rules`、`search` 原生化与验收；重采/修复 `my`（头图、图标、行距）、`settings`（SwitchButton/返回图标）、`import_export`/`backup`（返回箭头纯色区）。每页闭环：参考采集→validation→diff→修复→重建重采。
2. **关闭组 C 残余：** `stats_self` 线形、`stats_year` 热力点/年份条、month 饼图边界、normal 字体 tile、budget 填充边界；不得把已知差异当作通过。
3. **步骤 12 夜间：** `MainActivity` 接 AppCompat/AppCompatDelegate，按 `SettingsStore.themeMode` 应用；补齐 `values-night`、night drawable/mipmap；新建 `screens-night.yaml` 采 29 页两侧，结束后恢复原版主题、`ThemeMode.SYSTEM`、`cmd uimode night no`，重采日间 `home_top`。
4. **步骤 13 交付：** 不传 validation 标志构建 production；`aapt2`/`apksigner` 核验 `com.nonen.Bookkeeping`；`device_backup.py` 备份并记录真实数据；`adb install -r` 后冷启动、五标签、新增记账烟测；前后数据一致；记录 APK SHA 和备份路径。
5. **步骤 14：** 生成日间/夜间报告、组 A–D 报告、构建/数据记录；整体重写 HANDOFF；只提交 HANDOFF。

### 验收标准

- 29 页日间与 29 页夜间均达到整体 ≤1.5%、任意 64×64 ≤6%、bounds ≤3px、纯色通过；所有报告对应最终 APK；已知 1px 差异单独列明，不降阈值。
- 当前源码 JVM/Lint/完整构建通过；真实 Room/DataStore 测试通过；用户包安装前后数据一致。
- production 包含 `com.nonen.Bookkeeping`，签名/身份核验通过；交付 APK、SHA、备份路径、报告和日志。

### 需要用户裁决

- 是否接受剩余未过页（stats_self/year/my/settings/import/backup 的单 tile 或纯色残余）作为记录差异后进入夜间/交付，还是必须先全部收敛。
- `category_manage`/`rules`/`search` 是否必须在交付前完成像素级复刻。
- 是否删除根目录误建的 `--help`；是否允许下一阶段在必要时重新导出参考数据并重新导入 validation（用户数据会继续漂移）。

### 禁止事项与排除范围

- 未收到下一阶段恢复指令前，不修改应用源码、不构建、不安装、不操作手机；本次交接只写并提交 `HANDOFF.md`。
- 未完成验收前不安装/覆盖用户包；不对用户包运行会卸载的仪器测试；参考导入、截图、仪器测试限定 `.uivalidation`。
- 不执行 `git reset`/`git checkout`/`git clean` 等会丢弃工作区的操作；不删除其他模板/账单；不改账户余额；不降阈值；不伪造截图/报告/控件清单。
- 不新增网络权限、账号、云服务、会员、语音、自动记账、桌面微件等业务；不启动模拟器；不修改系统账号/权限/网络。
- 本阶段只提交 `HANDOFF.md`；其他文件及暂存状态保持不变。

### 材料位置

- 当前源码与未提交变更：`D:\github-repository\smart-record` 工作区；只提交 HANDOFF 不会带走源码。
- 本轮快照与报告：`work/logs/yimu-ui-reconstruction-s11/pause-20261020-1700/`、`pause-20261020-1523/`、`pause-20261020-1254/`、`groupA/B/C/D-known-differences.md`、`work/ui-capture/report-*.json`、两侧 `manifest.json`。
- 数据：`work/ui-fixtures/yimu_all_20261020_1203.xls`（1977 条，SHA `bb21...`）；validation 快照 `work/ui-capture/impl/_dataset/`。
- 证据：`work/ui-evidence/apktool/res/`、`work/ui-evidence/jadx/sources/`、`reference_apk/base.apk`、`work/ui-evidence/yimu-system-backup.zip`。
- 工具：`work/ui-harness/{capture,diff,check_reference_month,Install-S11Validation,Run-S11Command,device_backup}.py|ps1`、`screens.yaml`。
- 仅本机：`C:\Users\29602\.qoder\plans\{shining-dale-drake,ember-narrows-stork,rising-fjord-asp}.md`、`D:\桌面\temp\smart-record-S11-续跑提示词.md`；`work/` 被 Git 忽略，不能从提交恢复。
- 旧版交接：`git show b594e02:HANDOFF.md`（本文件将提交在 `b594e02` 之后，可用新 HEAD 查看）。
