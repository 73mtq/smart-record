# HANDOFF — S08 一木记账界面还原续接

## 1. 状态

- **交接阶段：S08**，承接最近一次交接提交 `c2bf68e`（S07）；界面计划自身使用阶段0—9编号。
- **功能状态：进行中，按用户要求结束本阶段。** 阶段2—9未完成验收，尚无通过全部检查的交付APK。
- **阻塞：** 独立验收包未取得预期页面；最后一次采集实际窗口为 `com.miui.home`，预期为 `com.nonen.Bookkeeping.uivalidation`。原因尚未确定；见 `work/logs/yimu-ui-reconstruction/36-validation-initial-capture.log`。
- **源码位置：** `D:\github-repository\smart-record`，分支 `main`；交接前HEAD为 `6c8cbc1`。本次提交仅包含 `HANDOFF.md`；界面源码、资源以及前序未提交改动保留在当前工作区。单独检出本次提交无法恢复全部实现。
- **任务原文已重新核对：** 用户要求“完成阶段 2—9，交付通过真机验证的 APK、截图差异报告与交接文档”，并要求“保留用户现有数据库”“新增验证使用真实 Room 数据和真实操作”。随后用户发送“暂停”，本轮要求产出并提交交接文档。原文审计副本：`work/logs/handoff-s08/user-requests.json`。
- **当前依据：** `docs/plans/yimu_exact_ui_reconstruction_plan.md`；任务状态 `work/tasks/yimu-ui-reconstruction.md`；上次暂停记录 `docs/reports/yimu_ui_reconstruction_handoff.md`。新会话继续使用本机工作区，按最新源码核查这些记录。
- **证据范围：** 本轮重新读取旧HANDOFF全文、Git状态/差异、指令原文、日志、恢复manifest及APK身份；没有继续界面实施、构建、安装或设备操作。审计文件在 `work/logs/handoff-s08/`，其中 `tracked-changes-before.patch`不包含未跟踪文件。

### 用户数据与设备

- 用户包 `com.nonen.Bookkeeping`；参考包 `com.wangc.bill` 6.1.8 / versionCode378；验收包 `com.nonen.Bookkeeping.uivalidation`。三个包必须明确区分。
- 仪器测试曾卸载用户包；已重新安装并从安装前完整备份恢复，手机读回文件逐字节一致，恢复退出状态0；证据 `30-production-data-restore.log`。最后一次用户包启动返回 `Status: ok`。
- 恢复记录：1970条，支出 `-43521.15`、收入 `32012.48`；用户现金账户原有余额 `-50.90`保留。原始备份：`work/backup/yimu-ui-before-install/`。
- 恢复数据库 `work/recovery/yimu-ui-production/bookkeeping.db` SHA-256：`4ef7ce3c6ee0d466bbcce7a7c72f44007aea77a2a452410c3bc97de17bca7f43`。
- 原设置 `work/backup/yimu-ui-before-install/settings.preferences_pb` SHA-256：`3f3e819c2c3cf7666ba86dc74c2ebec020f251b71d1f82dfd01196f09b9caa3f`。本轮文件哈希复查一致；没有重新操作手机核查当前数据。
- 上次设备基准：小米15，序列号 `95f79212`，Android16/API36/arm64，1200×2670、密度520、字体缩放1.0、简体中文、浅色、手势导航。动画缩放0及常亮来自前序环境，本阶段未更改。
- 已结束本次命令；共享Gradle daemon保持原状。用户未要求恢复任务前，不继续运行界面工作。

## 2. 变更文件清单

路径相对仓库根目录；`src/`表示 `app/src/main/java/com/nonen/Bookkeeping/`。下列文件包含本阶段确认修改或新增的内容；部分文件同时含前序未提交改动，完整状态见 `work/logs/handoff-s08/git-status-before.txt`。

| 路径 | 改动说明 | 原因 |
| --- | --- | --- |
| `app/build.gradle.kts` | 增加SwitchButton依赖和 `yimuUiValidation` 包名后缀开关 | 原版组件绘制与测试数据隔离 |
| `gradle/libs.versions.toml` | AndroidX Test JUnit设为1.3.0，Espresso设为3.7.0 | 核查仪器测试兼容性；启动问题仍未解决 |
| `src/MainActivity.kt`、`src/ui/AppNavHost.kt` | 接入五标签目标、独立设置页面、新建账单账本参数及新Intent处理 | 统一页面与快捷入口 |
| `src/ui/screens/MainScreen.kt`、`MainViewModel.kt` | 五页Pager、BottomNavigationView和共享账本选择 | 主界面结构还原 |
| `src/ui/screens/YimuHomeScreen.kt`、`HomeScreen.kt` | 首页工具栏、汇总、日期分组、账本选择、模板抽屉、多选删除和显示设置读取 | 首页呈现与真实数据查询 |
| `src/data/repo/TransactionRepository.kt` | 增加账本范围查询，月份使用明确日期边界 | 页面账本筛选一致 |
| `src/ui/screens/YimuAddEditScreen.kt`、`AddEditViewModel.kt` | 支出/收入/转账/债务标签、分类、属性、保存保护与再记状态 | 记账流程还原；债务保持占位 |
| `src/ui/screens/AmountKeyboardState.kt`、`NumberKeyboard.kt` | 金额输入状态、四则运算、退格和动态操作键 | 原版键盘交互 |
| `src/data/repo/AssetRepository.kt` | 转账接口接收时间参数 | 使用记账页选择的日期 |
| `src/ui/calendar/YimuCalendarView.kt` | CalendarView及 `ActivityMonthView`、`CustomWeekView`绘制 | 原版日历、农历与金额显示 |
| `src/ui/screens/YimuCalendarScreen.kt`、`CalendarViewModel.kt`、`CalendarScreen.kt` | 原生日历承载、账本筛选及嵌入页面参数 | 日期和账单列表联动 |
| `src/ui/screens/YimuAssetContent.kt`、`AssetManageScreen.kt` | 资产主页、分组和操作入口，复用已有弹窗 | 保留现有账户业务并替换视觉 |
| `src/ui/screens/YimuStatisticsScreen.kt`、`StatsCharts.kt`、`StatisticsScreen.kt` | 四种统计页面、原生图表及账本参数 | 使用真实交易和资产数据 |
| `src/ui/screens/MyScreen.kt` | 用户头部、功能网格和数据管理入口 | 我的页面还原 |
| `src/ui/screens/YimuSettingsPage.kt`、`SettingsScreen.kt`、`SettingsViewModel.kt` | 独立设置、主题、导入导出和备份页面 | 页面职责与操作入口对应 |
| `src/data/prefs/SettingsStore.kt`、`src/export/FullBackupService.kt` | 增加导航文字、所选账本及流水显示字段，并接入备份恢复 | 新增设置持久化与兼容 |
| `src/ui/components/YimuTopAppBar.kt` | 新增50dp工具栏和控件标识 | 二级页面尺寸与自动化定位 |
| `src/ui/screens/BudgetScreen.kt`、`PeriodicScreen.kt`、`SavingsGoalScreen.kt`、`TemplateScreen.kt` | 使用新工具栏 | 二级页面外观接续，整体布局仍待完成 |
| `src/ui/components/YimuCategoryIcon.kt` | 读取原版分类图标与历史分类显示映射 | 保留数据原意并使用真实图片 |
| `src/ui/theme/Theme.kt` | 文字行高、字体内边距等设置 | 核查Compose与TextView测量差异 |
| `src/core/nlp/ClassificationModelService.kt` | 保留名词内部文字，概率按 `catalog` 顺序映射 | 早餐语句解析与分类 |
| `src/data/migration/CategoryMigrationService.kt` | 仅有待迁移记录时播种迁移分类 | 已完整导入的分类目录应保持稳定；需补充回归 |
| `app/src/main/res/layout/view_yimu_calendar.xml` | 原生日历与列表承载布局 | 月/周切换 |
| `app/src/main/res/values/yimu_ids.xml`、`yimu_strings.xml` | 控件ID和部分界面文案 | 资源化与定位 |
| `app/src/main/res/mipmap-xhdpi/`、`app/src/main/assets/yimu/lineIcon/` | 复制原版分类图片 | 本地个人使用的真实图标 |
| `app/src/main/assets/yimu-category-icons.json`、`yimu-profile.json` | 原版分类图片映射、昵称及注册时间 | 本地视觉数据；没有导出账号凭据 |
| `app/src/test/java/com/nonen/Bookkeeping/core/nlp/ClassificationModelServiceTest.kt` | 增加早餐备注完整性断言，保留原分类断言 | 防止时间表达式删除名词文字 |
| `app/src/test/java/com/nonen/Bookkeeping/ui/screens/AmountKeyboardStateTest.kt` | 小数、连续运算、退格、除零及金额限制测试 | 验证真实输入状态 |
| `app/src/androidTest/java/com/nonen/Bookkeeping/ui/YimuNavigationInstrumentationTest.kt` | 五标签启动与页面文字检查 | 真实UI验证；当前失败 |
| `work/ui-harness/{capture.py,diff.py,verification.py,screens.yaml}` | 逐页manifest、稳定性、环境、哈希及必要控件检查 | 防止遗漏控件时误报通过 |
| `work/ui-harness/{device_backup.py,device_restore.py,prepare_room_fixture.py}` | 备份、读回校验和独立真实Room数据 | 保护用户包并隔离验收 |
| `work/ui-harness/{reference_resources.py,ui_action.py}` | 提取实际资源映射及按UI层级操作 | 资源取证与参考页面采集 |
| `docs/reports/yimu_ui_reconstruction_handoff.md`、`work/tasks/yimu-ui-reconstruction.md` | 保存暂停状态与证据入口 | 接续信息；本轮只提交根目录HANDOFF |

`work/`、`reference_apk/`已经忽略。数据库、账单、完整系统备份和APK保留在本机，不加入提交。

## 3. 关键决策与理由

### 本阶段方向

- **【长期】参考证据优先。** 基准为原版APK、反编译源码及同一台手机的真实截图；界面复杂组件采用Material Components 1.10.0、CalendarView 3.7.1、MPAndroidChart v3.1.0和SwitchButton 2.1.0，其他页面使用Compose。未采用全部自行绘制复杂组件的方式，原因是需要复用组件的测量、绘制与手势。
- **【长期】使用原版图片。** 已取得62个 `lineIcon`缓存文件及78项分类映射；用户选择获取原版资源，没有授权采用近似分类图标满足验收。源归档 `work/ui-evidence/yimu-system-backup.zip`；源文件及哈希见 `work/ui-evidence/line-icon-hashes.json`。
- **【长期】用户包与验收包分开。** `-PyimuUiValidation=true`只改变debug应用身份；独立包使用真实Room数据，账单总额与原始记录一致，分类、账本名称和资产状态来自参考应用。未采用修改用户现金余额满足截图的做法，原因是必须保留用户数据。
- **【长期】真实操作和严格比较。** 不使用mock或伪造截图；每页必要控件必须存在且检查数量大于零。阈值为未屏蔽区域差异≤1.5%、任意64×64区域≤6%、关键bounds≤3px、像素单通道容差8；不能放宽阈值代替修复。
- 默认底栏仅显示图标；“导航文字”关闭，账本选择由主界面共享，新增设置纳入备份。首页280dp侧栏展示模板，账本切换通过工具栏弹窗。
- **范围沿用本轮批准计划。** 既有本地业务保留；支出、收入、转账需可用，债务保持视觉占位。会员、账号、云服务、语音、自动记账、微件及既定未接入业务不在本阶段扩展范围。
- 用户允许在原版新增一个临时模板用于取证、随后删除该模板；**尚未执行**。不能借此新增原版账单或修改其他模板。

### 继承的长期要求

编号对应旧HANDOFF。行为要求的继承不代表本阶段已完成全部运行验证。

| 来源 | 决策、理由与边界 |
| --- | --- |
| 【长期，原1、20】 | `Budget`、`CategoryBudget`、`BudgetSetting`、`Cycle`、`CycleDate`、`CycleLog`、`ModuleBill`、`Dream`、`DreamLog`使用独立Room表；模板在 `module_bills`，愿望在 `dreams/dream_logs`。未采用配置JSON或向 `transactions`增加模板/愿望标记，原因是需要独立标识和生命周期。 |
| 【长期，原2】 | 金额持久化沿用 `Double`并保留两位金额计算规则；不迁移为Long分值或BigDecimal列，原因是保护既有数据兼容。`AmountKeyboardState`的BigDecimal仅用于输入运算。 |
| 【长期，原3、10】 | Room继续使用版本6与增量迁移；默认 `bookId=1`、`assetId=1`用于历史记录关联。未采用清空重建或将历史记录设为只读的方案，原因是保持历史数据可编辑。 |
| 【长期，原4】 | 资金业务按输入校验、持久化及关联、余额计算、编辑删除对冲、统计口径五项验证，防止仅凭界面判断业务完成。 |
| 【长期，原5】 | 不扩展房贷、车贷等Loan业务，遵守已确认功能范围。 |
| 【长期，原6】 | 保留原版 `classification-model.ser`、`TimeExp.m`及LibLinear/HanLP本地识别；不改用云端模型或重新训练模型，原因是保持离线处理与原版依据。 |
| 【长期，原7】 | 分类置信度要求为0.015，低于门槛归入 `其他-其他`；`食品餐饮-其他`按 `[04:00,10:00)`早餐、`[10:00,16:00)`午餐、其余晚餐处理。当前服务门槛仍需核查，见第4节。 |
| 【长期，原8】 | 金额预处理保留 `(块|元|点)([1-9一二三四五六七八九])[毛角]`到 `点$2`的规则，以及常见动词和首尾单位处理要求；中文金额完整流程本阶段未重新验证。 |
| 【长期，原9】 | 分类唯一索引为 `(categoryId,parentId,categoryType)`；默认一级ID9及其11项子分类为收入，其他默认分类为支出。用户自定义分类按实际目录核查。 |
| 【长期，原11、12】 | 能唯一识别的历史分类确定性映射；多义分类保留二级null或以 `isCustom=true`和独立业务ID保存，保护原始用途。 |
| 【长期，原13】 | 分类全局隐藏与账本隐藏独立；当前Kotlin字段 `hide:Boolean`，SQLite编码0/1；`hideBook:String?`保存逗号分隔账本ID。 |
| 【长期，原14】 | 删除自定义分类时保留账单文本和金额，仅解除相关分类关联；不得删除历史账单。 |
| 【长期，原15】 | 保存账单持久化 `parentCategoryId/childCategoryId`，成功后更新商户分类记忆。 |
| 【长期，原16】 | 新增、编辑、删除账单须与账户余额原子一致；编辑先对冲旧账户旧金额，再应用新值；删除进行反向对冲。需要真实Room事务验证。 |
| 【长期，原17】 | 用户包保护默认现金账户1；删除其他账户时重定向历史记录至默认账户并更新账户名称。空资产参考状态仅在独立验收数据中准备。 |
| 【长期，原18】 | 信用账户有总额度且余额不足额度时，欠款为 `totalQuota - assetNumber`；超额余额按现有资产规则计入资产；借入账户余额绝对值计入负债，净资产为总资产减总负债。 |
| 【长期，原21】 | 资产走势按账户从当前真实余额逆算：历史时点余额等于当前余额减去该账户在时点结束后全部交易金额之和；期末值应等于当前资产汇总。核查 `StatsCalculator.computeAssetTrend`及转账等场景。 |
| 【长期，原22】 | 一级分类与二级分类明细通过统一聚合规则计算，二级占比分母为所属一级总额。 |

旧阶段19、23、24仍须保持：周期记录使用 `cycle_{cycleId}_{yyyyMMdd}`及执行日志幂等，在Room事务中联动余额，每次执行后读取最新状态；23列Excel/CSV数据交换与完整备份分开；完整备份使用标准ZIP、`manifest.json/data.json`、SHA-256前置验证及失败时的数据库事务保护。完整设置、20类表、附件和中断一致性仍需真实验证。

## 4. 已知坑与失败尝试

| 现象 | 已有证据／原因 | 正确做法与状态 |
| --- | --- | --- |
| 仪器测试后用户包被卸载 | 本阶段已观察到；恢复日志30核验成功 | 后续只对 `.uivalidation`执行可能卸载应用的测试；默认恢复脚本会覆盖用户数据，禁止无条件重跑 |
| 仪器测试启动等待120秒或首页文字未出现 | 日志23停留于 `Instrumentation.startActivitySync`；日志25首页检查失败。升级JUnit/Espresso后未取得成功，根因未确定 | 在独立包核查安装身份、实际启动、进程、异常及UI层级；保留真实失败，不能声明由框架版本导致 |
| 最新独立包截图读到桌面 | 日志36实际包为 `com.miui.home` | 先单独核查 `am start -W`输出和应用异常，再进入采集；当前仍阻塞 |
| 底栏能点击但UI层级缺少导航项 | 日志19定位“我的”失败；本阶段曾通过真实点击进入我的页面 | 核查AndroidView与Compose辅助功能关系，补齐真实控件标识；不能用零控件检查判定通过 |
| ADB备份成功但没有资源 | ABE解包结果0条；备份47字节，日志07—08 | 使用已完成的小米本地应用备份；日志12取得真实文件。不可将空归档当作图标来源 |
| Windows adb stdin二进制恢复截断 | 日志27—28读回不一致，期望847872字节、一次读回24571字节；具体底层原因未确定 | 使用 `device_restore.py`的标准base64传输与逐字节读回校验；日志30通过。共享目录复制方式也未成功，见29 |
| 早餐分类失败 | 时间表达式匹配到了“早餐”内部“早”，备注断言复现；日志04—05 | 当前按HanLP名词范围保留内部文字，原分类断言保留，7项测试通过 |
| 0.015门槛的长期要求与当前代码有差异 | 当前 `ClassificationModelService.parseQuickRecord`以 `maxProb=0.0`选择类别 | 新阶段按原版代码核查并补充真实模型测试；当前不能声明门槛已生效 |
| 最新Lint没有有效通过结果 | 日志24请求构建/测试/Lint，但编译失败；后续33仅构建成功 | 对最新代码重新运行Lint和完整测试，不能引用前序Lint替代 |
| 参考manifest与严格工具格式不同 | 参考仍为旧格式，严格比较要求 `schema_version=2`及逐页PNG/XML哈希 | 重新采集或核验真实来源；逐页必要ID必须对照真实XML，缺少、空清单均失败 |
| 未解决的完整备份验证缺口 | 继承旧HANDOFF：设置往返、全部表、附件、失败和中断状态尚缺完整真实验证 | 使用独立包及真实Room/DataStore/文件环境验证；禁止在用户包做覆盖恢复和异常试验 |

继承的复发防护：纯JVM JSON使用真实 `org.json:json:20240303`；Room事务验证使用真实数据库，不能使用抽象类动态代理或直接执行回调代替事务证明；ViewModel纯计算测试可注入 `externalScope`，但UI运行仍须验证Android调度；`TodayWatcherTest`等待实际注册后再取消；资产走势预期值须计入全部流水；金额格式使用 `Format.kt`，不要在Composable内依赖不可观察的 `Locale.getDefault()`。这些旧问题没有在本轮文档阶段重测。

## 5. 环境与命令

- Windows PowerShell **7.6.5**、Python **3.12.7**；当前终端Java **24.0.1**，Gradle配置的Java toolchain为**21**；Android SDK/build-tools **37**，根目录运行命令。`sdk.dir`在忽略的 `local.properties`。
- 本阶段运行设备为小米15真机；没有使用VM bash。不要自动启动或下载模拟器。
- 构建依赖由Gradle解析。MIUI资源解包工具已在 `work/deps/miui-backup-extractor/`，检出commit `02b3c87d89e270bf68ad0d5a40604d49f25394f4`；保留现有工具，无需重复下载。

用户明确恢复任务后，使用工作区临时目录；命令完整输出写入新的 `work/logs/`运行目录，并记录 `$LASTEXITCODE`：

```powershell
Set-Location D:\github-repository\smart-record
$env:TEMP = (Resolve-Path work/tmp).Path
$env:TMP = $env:TEMP
$env:TMPDIR = $env:TEMP
$env:PYTHONUTF8 = '1'
$env:JAVA_TOOL_OPTIONS = '-Djava.io.tmpdir=D:/github-repository/smart-record/work/tmp'
```

独立包的首要启动核查：

```powershell
& 'D:\Android\Sdk\platform-tools\adb.exe' -s 95f79212 shell pm path com.nonen.Bookkeeping.uivalidation
& 'D:\Android\Sdk\platform-tools\adb.exe' -s 95f79212 shell am start -W -n com.nonen.Bookkeeping.uivalidation/com.nonen.Bookkeeping.MainActivity
```

构建和测试按用途分别执行，测试结束后重新核查验收包是否保留：

```powershell
# 普通用户包：构建，不自动安装或覆盖数据
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain

# 独立验收包；测试工具可能卸载这个被测包
.\gradlew.bat :app:assembleDebug '-PyimuUiValidation=true' --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest '-PyimuUiValidation=true' '-Pandroid.testInstrumentationRunnerArguments.class=com.nonen.Bookkeeping.ui.YimuNavigationInstrumentationTest' --console=plain
```

确认启动、APK身份、真实数据与必要控件后采集：

```powershell
python work/ui-harness/capture.py --app reference --only home_top,my
python work/ui-harness/capture.py --app validation --only home_top,my
python work/ui-harness/diff.py --only home_top,my
```

- 当前 `app/build/outputs/apk/debug/app-debug.apk`实际身份为 `.uivalidation`，versionName `0.2.3-ui-validation`；用户包构建必须不传 `yimuUiValidation=true`，安装前用 `aapt2 dump badging`核验。
- 固定验收APK：`work/artifacts/yimu-ui-validation.apk`，SHA-256 `5a9cae3429beecc2cd5e58b9e48d51da16f974f17f68b3547426b1b70ea8317b`；采集配置的 `apk_hint`指向此副本，新构建后必须同步副本和哈希。
- 已执行的依赖获取：`git clone --depth 1 https://github.com/pgw00k/MIUIBakupExtract.git work/deps/miui-backup-extractor`；解包入口 `python work/deps/miui-backup-extractor/miui_backup_extractor.py <本地bak路径> -o work/ui-evidence/yimu-private -q`。不要将原版数据库和凭据加入应用资源。
- 数据准备脚本 `prepare_room_fixture.py`要求目标目录尚不存在；不要直接重复执行。`device_restore.py --validation`仍会覆盖验收包数据，使用前确认目标身份；不带参数会指向用户包。

## 6. 验证状态

完整日志目录：`work/logs/yimu-ui-reconstruction/`；下表仅记录已发生的结果。

| 检查 | 结果／证据 | 当前适用范围 |
| --- | --- | --- |
| 构建基线 | 01日志成功，退出0 | 本阶段开始时 |
| 分类模型7项 | 05日志成功，退出0 | 当时源码；保留原断言并增加备注断言 |
| 键盘输入测试及构建 | 06日志成功，退出0 | 当时源码 |
| 完整单元测试及构建 | 10日志成功，退出0；当前JUnit XML合计164项、0失败、0错误 | 此后仍有界面、设置、迁移和资源改动，不能代表最新源码全部通过 |
| 最新独立包构建 | 33日志成功，退出0 | 当前构建产物；本轮重新核对APK包名和固定副本哈希 |
| 独立包安装／真实数据写入 | 34、35日志退出0，数据库与设置读回一致 | 安装及写入成立，页面启动未通过 |
| 用户包恢复 | 30日志退出0，1970条及总额一致，文件读回一致 | 本轮再次核对本地恢复文件哈希；没有新操作设备 |
| 五标签仪器测试 | 21—23、25日志失败，退出1 | 未取得五标签全部运行通过证据 |
| 最新截图 | 36日志失败，退出1 | 未得到有效的最新页面比较结果 |
| 最新完整回归、Lint和像素报告 | 未完成 | 风险高；禁止标记还原完成 |

未验证风险：**高**——启动、辅助功能、全部记账/转账真实操作、新显示设置的备份往返、分类迁移条件和截图工具严格检查；**中**——二级页面新增编辑、跨月和多账本、字体缩放/键盘遮挡、夜间主题；**尚无覆盖**——API26设备与平板。本阶段目标仍集中于小米15，不能自动扩展模拟器验证。

## 7. 下一阶段交接

### 第一步

新会话读取本文件和 `work/tasks/yimu-ui-reconstruction.md`，运行 `git status --short`并核查当前源码。随后在用户恢复授权下执行第5节独立包 `pm path`和 `am start -W`，核对真实启动结果、对应APK身份和应用异常。启动问题解决前，不批量重跑截图或仪器测试。

### 待完成内容与验收标准

1. 完成原生底栏与Compose的真实定位、点击、滑动和快捷入口验证；实际包和预期页面必须一致。
2. 完成首页账本弹窗、模板应用、悬浮球、多选统计/导出；记账页按参考实现分类顺序及列表内部二级分类展开；保留保存、编辑删除、转账及再记要求。
3. 完成统计资产走势、年度热力和筛选，逐页完成搜索、分类、预算、周期、模板、愿望、规则和数据管理布局及操作。
4. 补齐非空模板抽屉、夜间、无键盘搜索及二级新增编辑参考。临时原版模板必须记录标识、采集后仅删除本次新增项，并核对原版账单数量。
5. 逐页核实必要ID与manifest来源，完成全部严格截图比较；满足第3节阈值，数据不一致不得用掩膜掩盖。
6. 对最新源码运行完整JVM测试、Lint、构建和独立包仪器测试，真实退出0；完整设置/备份恢复及用户包数据保护须有证据。最终用户包另行构建、核验包名和签名、保留数据安装后，交付APK、SHA-256与报告。

### 禁止事项与接续技能

- 本轮仅交接并提交文档，不恢复界面实施；后续按用户新指令继续。
- 禁止对用户包执行会卸载应用的测试，禁止无条件运行默认数据恢复脚本、清除应用数据或修改用户账户余额满足参考截图；原版应用数据保持受保护状态。
- 禁止Git重置或用Git恢复代码，禁止读取或写入系统Temp、擅自调用视觉功能、伪造测试数据或截图；代码修改使用文件编辑工具。
- 不新增网络权限，不接入账号、云服务、会员、语音、微件及既定排除业务；不重启手机、变更网络或系统账号。
- 后续完整构建与测试不等于像素验收；此前164项通过不能覆盖后续修改。不得将未跟踪源码和忽略的本地证据误认为已经提交。
- 建议技能：`tx-guideline`；需要重新核查APK时使用 `reverse-engineer-anything`；后续交接继续重写本文件。用户规定的文件位置与本轮七节结构优先于技能默认模板。
