# HANDOFF — S11 一木记账界面复刻（2026-10-20 晚间续跑交接）

## 1. 状态

- **阶段：S11；阶段目标：** 继续一木记账界面像素级复刻，本阶段重点为用户指定的首页麦克风按钮快速记账一致性与统计分类占比扇形图。阶段编号依据：HEAD 提交 `287e339` 信息 `handoff: S11 阶段交接`、`work/tasks/yimu-ui-reconstruction.md`、日志目录 `work/logs/yimu-ui-reconstruction-s11/`；编号确定，无歧义。旧版 HANDOFF 存在于 HEAD（`git show 287e339:HANDOFF.md`），本文件为整体重写，不是首次创建。
- **当前状态：进行中，未完成，无技术阻塞。** 本阶段（287e339 之后，2026-10-20 17:58–20:50，日志 871–989 号）完成：clean 基线重建与全量重采、home_scrolled 修复后通过、add_bill_expense 重采通过、统计五页最终一轮收敛（stats_self 2.481%→1.747%）、分类管理页原生化（整体 0.043%）、规则页原生复刻（fix2 后 0.0078% 未判定通过，工具栏调整后 16.30%）、我的/设置页多轮修正（3.46%/4.32%）、语音记账方案按用户否决回退。夜间主题、生产包交付、最终报告仍未开始；没有完成全部验收的用户 APK。
- **仓库：** `D:\github-repository\smart-record`；分支 `main`；核查时间 **2026-10-20 21:15–21:40（Asia/Shanghai）**；写作前 HEAD 为 `287e339f420117853514ea052091a546d2612a51`（`handoff: S11 阶段交接`，提交时间 2026-10-20 17:17:09）。暂存区为空；工作区 62 个跟踪文件修改（+2464/−2358）、2 个删除、`git status --porcelain -uall` 共 478 行。
- **阶段起点与证据范围：** 本阶段起点为 287e339 提交之后的实施；日志 871 号 START 2026-10-20T17:58:29、989 号 END 2026-10-20T20:44:28 全部属于本阶段；`groupC-known-differences.md` 最后更新 20:50。17:17 之前的改动属于 S11 前段及更早阶段，不归入本阶段。
- **本阶段任务要求及来源（原文可从上一会话记录取得）：**
  - 上一会话记录已解压到 `work/tmp/session-12cf8812/session.v4.jsonl`（源文件 `D:\桌面\temp\dsh-session-session-12cf8812-0009-4295-8483-5ca585652b05.zip`，本机存在）。
  - 用户 turn 1（17:28）：/tx-guideline 计划模式，只读探索，依据 HANDOFF.md 生成下一阶段执行计划，等待批准。
  - 用户 turn 2（17:57）：`已连接开始执行`，批准执行计划 `C:\Users\29602\.qoder\plans\rising-fjord-asp.md`（S11 续跑计划，本机存在）。
  - 用户 turn 3（20:17）：`继续，但是着重点为首页的麦克风点击的记账与一木记账对齐一模一样，还有统计部分，分类占比扇形图什么的`。
  - 用户 turn 3（20:29）：`不需要语音，我要的是点击按钮之后的快速记账`。此前 AI 已按语音路线加入 RECORD_AUDIO 权限并构建安装（970/971 号），用户否决后 972 号构建回退，当前 Manifest 无 RECORD_AUDIO。
  - 用户要求与 AI 建议的区分：快速记账用点击按钮进入现有记账页、统计扇形图收敛是用户要求；语音识别入口（已回退）、stats_self 贝塞尔线形、分类管理/规则页原生复刻等实现方案是 AI 提出后执行。
- **数据与产物：**
  - 参考快照 `work/ui-fixtures/yimu_all_20261020_1203.xls`：1977 条，SHA-256 `bb21aa69f18d34a522f9ed86babef4faad4616d67dabe96c39172cd2df473c4f`；2026-10 为 282.00/4071.94/−3789.94。
  - 当前 validation APK：`app/build/outputs/apk/debug/app-debug.apk` 与 `work/artifacts/yimu-ui-validation.apk` 相同，SHA-256 `052531b6df0693f8415c452f57748f026439b8015f9725af647cdaee7eb73cd1`，构建时间 2026-10-20 20:42:36；对应源码最后修改 20:30（YimuHomeScreen.kt），与本阶段最终采集一致。
  - 用户包 `com.nonen.Bookkeeping` 0.2.3/versionCode 5：本阶段未安装、未覆盖、未读取用户数据。
  - 下一日志编号从 **990** 开始。
- **设备：** 小米 15 `95f79212` 在实施期间在线（构建、安装、采集全部成功）；**交接核查时已断开**（`adb devices` 为空），动画缩放、前台 Activity、validation 包状态均无法现场核验，恢复实施时必须先重新连接并核查设备身份。
- **GitHub 上传状态：** `origin` = https://github.com/73mtq/smart-record.git，可连通；本地 `main` 领先 `origin/main` 17 个提交。用户要求最后将所有东西上传到 GitHub，本次交接提交后执行 `git push origin main`。注意：界面复刻源码全部位于未提交工作区，不在 Git 历史中；`work/` 目录被 Git 忽略，不会上传。

## 2. 变更文件清单

本阶段没有提交任何源码；本节只列 287e339（17:17）之后可确认归属的变更，判定依据为文件修改时间晚于 17:17。工作区还包含 S09、S10、S11 前段的累计改动，不能全部归入本阶段。

### 2.1 本阶段源码变更（全部未提交；未跟踪文件为 S11 前段新增、本阶段继续修改）

**统计（本阶段重点）：**
- `app/src/main/java/com/nonen/Bookkeeping/ui/components/YimuStatisticsNativeView.kt`（未跟踪）：stats_self 收支走势改为 `LineDataSet.Mode.HORIZONTAL_BEZIER`、`cubicIntensity=0.2`、`setDrawCircles(false)`（原版取证）；纵轴刻度标签字号收敛（参考行高 17px，此前实现 26px）；stats_year 柱体基线修正尝试。证据：978–989 号日志、`report-self-bezier.json`、`report-self-axis.json`、`report-stats-final-20261020.json`。

**首页快速记账与语音回退：**
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/YimuHomeScreen.kt`（未跟踪，最后修改 20:30）：FAB 使用 `ic_add_ai_bill` 图标，点击 `onAdd()` 进入现有记账页。
- `app/src/main/AndroidManifest.xml`（跟踪修改，diff 含前段累计）：本阶段曾加入 `RECORD_AUDIO`（970 号构建），用户否决后 972 号回退，当前无该权限。
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/MainScreen.kt`、`app/src/main/java/com/nonen/Bookkeeping/ui/AppNavHost.kt`、`app/src/main/java/com/nonen/Bookkeeping/ui/screens/AddEditViewModel.kt`（跟踪修改，diff 含前段累计）：路由与记账入口调整。

**分类管理原生化：**
- 修改（跟踪）：`app/src/main/java/com/nonen/Bookkeeping/ui/screens/CategoryManageScreen.kt`、`CategoryManageViewModel.kt`。
- 新增（未跟踪）：`app/src/main/java/com/nonen/Bookkeeping/ui/components/YimuCategoryManageNativeView.kt`、`YimuCategoryKeyNativeView.kt`；布局 `app/src/main/res/layout/yimu_category_manage.xml`、`yimu_category_key.xml`、`item_yimu_edit_type_child.xml`、`item_yimu_edit_type_parent.xml`；图标 `banner_category_type.png`、`ic_ai_bill_self_tip.png`、`ic_book_menu.png`、`ic_category_add.png`、`ic_right.png`。证据：928–940 号日志、`report-categorymanage3.json`。

**规则页原生复刻：**
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/RulesScreen.kt`（跟踪修改，最后修改 18:44）：原生布局复刻与工具栏调整。证据：941–967 号日志、`report-rules6.json`。

**我的与设置：**
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/MyScreen.kt`（未跟踪，18:25）、`YimuSettingsPage.kt`（未跟踪，18:28）、`app/src/main/java/com/nonen/Bookkeeping/ui/components/YimuTopAppBar.kt`（未跟踪）：头图/网格/开关/返回图标多轮修正。证据：886–927 号日志、`report-my-final.json`、`report-groupD-partial.json`。
- `app/src/main/res/values/colors.xml`（跟踪）、`yimu_ids.xml`、`yimu_strings.xml`（未跟踪）：颜色与资源调整。

### 2.2 误建文件（未跟踪、空文件、不得提交）

- 根目录 `--help`（0 字节，2026-10-20 12:00）、`--package`（0 字节，2026-10-20 20:34）。均为误建，不参与构建。本次交接不得修改其他文件，保留原样，待用户裁决删除。

### 2.3 更早的累计未提交改动（不归本阶段）

- 其余 50 余个跟踪修改、2 个删除与大量未跟踪文件来自 S09、S10、S11 前段（17:17 之前），归属证据见旧版 HANDOFF 第 2 节、`pause-20261020-1700/`、`pause-20261020-1523/` 快照。
- `docs/plans/…` 与 `docs/reports/yimu_ui_reconstruction_handoff.md` 等未跟踪文档均为 2026-10-18 创建，来自更早阶段。

### 2.4 仅本机材料（work/ 被 Git 忽略）

- 日志 `work/logs/yimu-ui-reconstruction-s11/` 871–989 号；`groupC-known-differences.md`（20:50 更新）、`groupD-known-differences.md`；`work/ui-capture/report-*.json` 与两侧截图；`work/tmp/session-12cf8812/session.v4.jsonl`（上一会话记录）；`work/backup/validation-before-restore-20261020-2035/`（validation 恢复前备份）。

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
- **【长期】业务范围：** 本地支出、收入、转账；债务为视觉占位；会员、账号、云服务、语音、自动记账、桌面微件及房贷车贷等既定业务不扩展；原版只允许新增一个临时模板用于取证，记录标识并删除本次新增项。
- **【长期】采集与字符：** 原版外部 shell 冷启动、持续式 UIAutomator 读取真实控件；文字使用 AppCompat 主题与整数像素；首页原生 XML/RecyclerView；JVM JSON 使用真实 org.json；TodayWatcher 等待注册；金额沿用 Format.kt；Compose 避免依赖不可观察的 Locale.getDefault()。

### 3.2 本阶段实际采用的决定

- **快速记账：** FAB 图标为 `ic_add_ai_bill`，点击 `onAdd()` 打开现有记账页。语音识别入口被用户明确否决，RECORD_AUDIO 权限已在 972 号构建回退。
- **stats_self 线形：** 原版 `LineDataSet.Mode.HORIZONTAL_BEZIER` + `cubicIntensity=0.2` + `setDrawCircles(false)`；支出 `fade_pay`、收入 `fade_income` 渐变填充。结果整体 2.481%→1.747%，最差 tile 88.31%→70.43%，纯色失败 3→1。
- **stats_self 纵轴：** 参考轴标签行高 17px，实现此前为 26px；缩小字号后收敛。剩余差异为字形栅格化。
- **stats_year：** 5 个纯色失败源于柱体整体出现约 8px 基线位置差造成的边缘区块；柱体数值、高度、顺序与参考一致，属内容位置差，已记录。
- **分类管理页：** 原生复刻（`yimu_category_manage.xml` + `YimuCategoryManageNativeView`），整体 0.043% 但最差 tile 15.58%。
- **规则页：** 原生复刻；fix2 阶段曾达整体 0.0078%、tile 5.64% 但报告判定未通过；后续工具栏调整（959–966 号）使差异扩大到 16.30%（工具栏高度差 62px）。
- **home_scrolled：** 固定滚动位置差 112px 采集（884/885 号），通过。
- **数据身份：** 878 号全量 diff 报 `home_scrolled: page dataset differs from current manifest`，重新采集后 880 号完成全量比较；数据预检按 `verification.py` 的 manifest 身份校验执行。
- **已批准：** rising-fjord-asp.md 计划执行（用户 17:57 批准）、统计扇形图与快速记账重点（用户 20:17 要求）、语音否决与快速记账路线（用户 20:29 要求）。
- **待用户裁决：** 剩余未过页（rules/category_manage/my/settings/search/stats 单 tile 或纯色残余）的处理方式；未提交源码是否提交并上传 GitHub；`--help`/`--package` 误建文件是否删除。

### 3.3 有证据的失败/否决方案

- **语音记账：** 已加 RECORD_AUDIO 并构建安装（970/971 号），用户否决，972 号回退。证据：会话原文 20:29、971 号安装日志（含 RECORD_AUDIO）、当前 Manifest 无该权限。
- **rules 工具栏调整：** 959–966 号尝试后差异从 0.0078% 扩大到 16.30%（`report-rules6.json`，tool_bar bounds delta 62px），未恢复。
- **my 页多轮尝试：** nickname-vip/gridrow/ratio/fixedheight/cardheight 等（886–908 号）未能收敛头图栅格化，最终 3.46%（`report-my-final.json`）。
- **stats_self 坐标轴改动：** 985 号 1.832% 高于 981 号贝塞尔后的 1.747%，最终以 986–989 号贝塞尔版本为准。
- **旧阶段被否决方案继续有效：** `createComposeRule()` 被 MIUI 后台启动限制拦截（result code 102）；整屏 ModalNavigationDrawer 与抽屉遮罩；日历热力底色；导入导出滚动条导致采集不稳定。

## 4. 已知问题与失败尝试

- **rules 页 16.30% 未通过（`report-rules6.json`，966 号）。** 现象：工具栏高度与参考差 62px（bounds delta），最差 tile 100%。过程：fix2 曾达 0.0078%/5.64% 但判定未通过；toolbar 系列尝试后扩大。正确做法：对照 958 号构建的源码状态还原工具栏高度，再逐项收敛；不得改阈值。
- **category_manage 页整体 0.043%、tile 15.58% 未通过（`report-categorymanage3.json`，940 号）。** 整体与 bounds、纯色均通过，仅剩单 tile 超 6%；属字形/图标栅格化类残余。
- **my 页 3.46% 未通过（`report-my-final.json`，909 号）。** 13 个纯色失败（头图/头像/网格图标区域），bounds 通过。多轮尝试未收敛，原因未完全确认。
- **settings 页 4.32% 未通过（`report-groupD-partial.json`，927 号）。** 最差 tile 42.31%（SwitchButton/返回图标区域），纯色 0 失败。
- **search 页无当前有效截图**（880 号 error: missing screenshot），本阶段未处理。
- **统计残余（`report-stats-final-20261020.json`，989 号，退出 1）：** stats_self 1.747%（tile 70.43%，1 个纯色失败，纵轴刻度字形）；stats_year 1.390%（tile 35.23%，5 个纯色失败，柱体约 8px 基线位置差）；stats_month 0.599%（饼图被底栏遮挡的扇区边界）；stats_normal 0.107%（柱体数值字体边缘）；budget 0.331%（趋势填充边界）。数值与参考一致（月 4,071.94/282.00、年 33,248.69/24,277.99、自定义 5,083.96/782.00）。
- **旧阶段未解决项继续有效：** CycleDate 独立持久化要求待核实；原版设置恢复来源缺失（S09 DataStore CorruptionException）；API 26/平板无运行证据；字体缩放/键盘遮挡未验收；夜间主题未开始；生产包交付未开始；当前源码的完整 JVM/Lint/仪器测试未重跑（165 项 JVM 0 失败与 Lint 0 是旧构建结果）。
- **设备状态不可核验：** 交接时 95f79212 已断开，动画缩放三项、前台 Activity、validation 包 lastUpdateTime 均无法现场确认；恢复时先重连并核查。
- **参考数据漂移：** 用户每日使用原版；当前 1977 条快照（12:03 导出）。恢复后每次采集前核对 manifest 身份。
- **误建文件：** 根目录 `--help`、`--package`（0 字节），不得提交，待用户裁决。

## 5. 环境与命令

- **运行环境：** Windows；Git Bash + PowerShell 7；Python 3.12（Pillow、NumPy、PyYAML、xlrd）；Gradle toolchain JDK 21；Android SDK `D:\Android\Sdk`、build-tools 37；`GRADLE_USER_HOME=D:\dev-cache\.gradle`；临时文件放 `work/tmp`。
- **环境变量名：** `TEMP`、`TMP`、`TMPDIR`、`PYTHONUTF8`、`JAVA_TOOL_OPTIONS`、`GRADLE_USER_HOME`；不记录任何密钥/Token。
- **设备：** 小米 15 `95f79212`，Android 16/API 36/arm64，1200×2670，密度 520，zh-CN，手势导航。交接时设备已断开，恢复时先 `adb devices`。
- **应用身份：** 用户包 `com.nonen.Bookkeeping` 0.2.3/5；validation `com.nonen.Bookkeeping.uivalidation` 0.2.3-ui-validation/5；参考包 `com.wangc.bill` 6.1.8/378。
- **本阶段已执行命令（日志 871–989）：**
  - 完整构建：`gradlew.bat :app:clean :app:assembleDebug :app:assembleDebugAndroidTest -PyimuUiValidation=true --console=plain --max-workers=2`（871 号，退出 0）。
  - 增量构建：`gradlew.bat :app:assembleDebug -PyimuUiValidation=true --console=plain --max-workers=2`（各修改后）。
  - 安装：`pwsh -NoProfile -File work/ui-harness/Install-S11Validation.ps1`。
  - 采集/比较：`python work/ui-harness/capture.py --app reference|validation --only <页>`、`python work/ui-harness/diff.py --only <页> --report <json>`。
  - 上一会话记录解压：`Expand-Archive` 到 `work/tmp/session-12cf8812/`。
- **建议命令（下一阶段）：** 先 `adb devices` 恢复设备；`git status --porcelain -uall`；按需 clean 构建；采集前设动画缩放 0、结束后恢复 1.0；用户包最终构建不传 `-PyimuUiValidation=true`，安装前运行 `python work/ui-harness/device_backup.py`。
- **日志纪律：** 所有构建/安装/采集走 `Run-S11Command.ps1`，编号从 990 起，不复用。

## 6. 验证状态

### 6.1 已验证（本阶段构建与采集，2026-10-20 17:58–20:50）

- 871 号 clean 完整构建（assembleDebug + assembleDebugAndroidTest）退出 0（1m46s）；此后各增量构建退出 0。
- 880 号全量 baseline diff：`home_top` 0.0209%/3.47%、`home_drawer` 0.0171%、`account_book_popup` 0.0209% 通过；`home_scrolled` 2.7052% 未通过（当时状态）。
- 883 号 `add_bill_expense` 重采通过：0.0172%/5.64%，退出 0。
- 885 号 `home_scrolled` offset112 后通过：0.0058%/1.39%，退出 0。
- 936/940 号 `category_manage`：0.043%/15.58%，bounds/纯色通过，单 tile 未过。
- 958 号 `rules` fix2：0.0078%/5.64% 但判定未通过；966 号 toolbar 调整后 16.30%。
- 927 号组 D 部分：`my` 3.46%（bounds 通过、13 纯色失败）、`settings` 4.32%（纯色 0 失败）。
- 989 号统计五页最终 diff（退出 1）：数字见第 4 节；对应最终 APK `052531b6`。
- 本阶段 Manifest 最终无 RECORD_AUDIO（972 号回退后状态，本机文件核查确认）。

### 6.2 历史结果（对应各自构建，不能视为当前 APK）

- 组 A 6 页通过、日期/资产弹窗 1px 差异（`report-groupA-final.json`）；组 B 日历两页 0.4534%/0.2446%、资产页 0.2695%；组 D periodic 0.0078%、savings_goal 0.0066% 通过。均见 17:00 快照 README 与旧版 HANDOFF。
- 首页四页重采通过（12:54 快照）、165 项 JVM 0 失败与 Lint 0、5 项 Compose 操作测试通过：均为更早构建结果。

### 6.3 未验证/风险

- 当前源码未重新运行完整 JVM/Lint/仪器测试。
- rules/category_manage/my/settings/search 未通过；夜间主题、生产包交付、安装后数据一致性未开始。
- 设备已断开，设备侧状态（动画缩放、validation 安装版本、前台）未核验。
- 参考数据可能继续漂移；GitHub 上传内容不含未提交源码与 `work/`。

## 7. 下一阶段交接

### 第一项操作

1. 在 `D:\github-repository\smart-record` 读取本文件与 `work/tasks/yimu-ui-reconstruction.md` 顶部、`work/logs/yimu-ui-reconstruction-s11/progress.md`、`groupC-known-differences.md`、`groupD-known-differences.md`。
2. 运行 `git status --porcelain -uall`、`git rev-parse HEAD`、`git diff --cached`；确认本交接提交只含 `HANDOFF.md`。
3. 连接小米 15：`adb devices`；确认 `95f79212` 在线后核查动画缩放三项、validation 包版本与 lastUpdateTime、用户包版本；设备不在时先恢复设备。
4. 继续界面收敛前，先按当前源码重新完整构建并安装 validation，再逐页采集。

### 待办（按依赖排序）

1. **关闭组 C 统计残余：** stats_self 纵轴刻度字形（17px 行高）、stats_year 柱体 8px 基线位置差、month 饼图扇区边界、normal 柱体数值字体、budget 填充边界；不得把已知差异当作通过。
2. **组 D 残余：** rules 还原 toolbar 高度（对照 958 号状态）、category_manage 单 tile、my 头图栅格化、settings SwitchButton、search 采集。
3. **步骤 12 夜间主题：** AppCompat/AppCompatDelegate 接 `SettingsStore.themeMode`，补齐 values-night 资源，screens-night.yaml 采 29 页两侧，结束后恢复原版主题与系统状态。
4. **步骤 13 交付：** 不传 validation 标志构建 production；aapt2/apksigner 核验身份签名；`device_backup.py` 备份用户数据；`adb install -r` 后烟测与数据一致性核对；记录 APK SHA-256 与备份路径。
5. **步骤 14：** 完整报告；整体重写 HANDOFF；只提交 HANDOFF。

### 验收标准

- 29 页日间与夜间全部达到整体 ≤1.5%、任意 64×64 ≤6%、bounds ≤3px、纯色通过；所有报告对应最终 APK；已知 1px 差异单独列明，不降阈值。
- 当前源码 JVM/Lint/完整构建通过；生产包含 `com.nonen.Bookkeeping`；安装前后用户数据一致。

### 需要用户裁决

- rules/category_manage/my/settings/search 与统计单 tile 残余：继续收敛直到全部通过，还是记录差异进入夜间与交付。
- 未提交源码是否提交并上传 GitHub：当前 GitHub 只有提交历史（交接文档与 T01–T08 功能提交），界面复刻源码与资源仅在本机工作区；`work/` 目录（日志、报告、参考数据、APK）被 Git 忽略，不会上传。若用户离开较长时间且需要源码备份，需明确批准提交源码。
- 误建文件 `--help`、`--package` 是否删除。

### 禁止事项与排除范围

- 本次交接只写并提交 `HANDOFF.md`；不修改其他文件、不取消或提交其他暂存内容。
- 不使用 Git 回滚/checkout/clean/stash；源码修改用文件编辑工具。
- 未完成验收前不安装/覆盖用户包；参考导入、截图、仪器测试限定 `.uivalidation`；不读取用户交易数据。
- 不新增网络权限、账号、云服务、会员、语音、自动记账、桌面微件；不降阈值；不伪造截图/报告。
- `work/` 含个人账单参考数据，不上传 GitHub。

### 材料位置

- 当前源码与未提交变更：`D:\github-repository\smart-record` 工作区；GitHub（origin 73mtq/smart-record）只有提交历史。
- 日志：`work/logs/yimu-ui-reconstruction-s11/`（871–989 号）；快照 `pause-20261020-1700/`、`pause-20261020-1523/` 等。
- 报告：`work/ui-capture/report-*.json`（本阶段新增 report-stats-final-20261020.json、report-categorymanage3.json、report-rules6.json、report-my-final.json、report-groupD-partial.json、report-self-bezier.json、report-self-axis.json 等）。
- 数据：`work/ui-fixtures/yimu_all_20261020_1203.xls`（SHA `bb21…`）；validation 数据 `work/ui-capture/impl/_dataset/`。
- 上一会话记录：`work/tmp/session-12cf8812/session.v4.jsonl`；本机计划 `C:\Users\29602\.qoder\plans\rising-fjord-asp.md`。
- 工具：`work/ui-harness/`；旧版交接：`git show 287e339:HANDOFF.md`、`git show b594e02:HANDOFF.md`。