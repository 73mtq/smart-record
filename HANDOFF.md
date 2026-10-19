# HANDOFF — S10 一木记账界面还原

## 1. 状态

- **阶段S10；进行中，用户于2026-10-19 13:44要求暂停。** 阶段2—9未完成验收，没有完成全部验收的用户APK。本次只重写并提交HANDOFF.md，应用实施继续暂停。
- **当前限制：** 最新原生首页、模板入口及5项Compose操作测试尚未构建。`TemplateScreen.kt:57`的`onCreate: () -> Unit`与`AppNavHost.kt:329`传入moduleType的调用不一致，恢复后必须完成接口及模板原版页面；当前不能报告最新源码编译通过。
- **工作目录与Git：** `D:\github-repository\smart-record`，`main`；交接前HEAD为`4c19e2c`（S09），56个跟踪文件修改、2个删除、317个未跟踪文件，暂存区为空。实现主要保存在当前目录，单独检出交接提交不能恢复全部实现。
- **依据：** 已完整重读S09 HANDOFF，重新检查Git状态/差异、当前源码和完整日志。批准实施、暂停及本次交接原文在`work/logs/handoff-s10/user-requests.json`，对应source第115、913、958行。计划及任务状态另见`docs/plans/yimu_exact_ui_reconstruction_plan.md`、`work/tasks/yimu-ui-reconstruction.md`。
- **最后执行：** `39-exhaustive-region-diff.log`记录首页比较退出1；随后进行了尚未构建的源码编辑。暂停时无运行中的构建或ADB命令，子任务已停止，共享Gradle daemon保留。本次交接没有构建、安装或操作手机。
- **应用身份：** 用户包`com.nonen.Bookkeeping`；参考包`com.wangc.bill` 6.1.8/versionCode378；独立包`com.nonen.Bookkeeping.uivalidation`。本阶段未安装或覆盖用户包。

### 数据与产物

- 用户本地备份`work/backup/yimu-ui-s09-current/`：1970条，支出`-43521.15`、收入`32012.48`，94分类、1账户，设置文件缺失。原数据库SHA-256为`4ef7ce3c6ee0d466bbcce7a7c72f44007aea77a2a452410c3bc97de17bca7f43`。本轮复制DB/WAL并通过SQLite backup核验完整性和总额；没有重新读取手机用户包，安装前须重新备份当前数据。
- 原版正式导出`work/ui-fixtures/yimu_all_1973_20261019.xls`：1973条，支出`-43541.17`、收入`32012.48`，SHA-256为`5da4324013b89e2abd1dcab589cf1ac123aa87567ec89458d7787f1df700bf4e`。13号导出页显示1973条，23/24号核验文件，34号正式解析器导入独立包通过。原版午饭和饮料新增支出合计17.82元来自用户数据变化；本阶段未在原版新增或删除账单，临时模板仍未创建。
- 独立包35号采集快照为1973条、78分类、空资产，路径见`work/ui-capture/impl/manifest.json`的`dataset.snapshot_path`，SHA-256为`37965359c618764825d5b00055d7cb52a2f2aea02a3648fda18c93d23f0bda44`；本轮本地重验通过。
- 30号构建、32号安装的应用APK与`work/artifacts/yimu-ui-validation.apk`一致，SHA-256为`158ce7ad18a10e63c088b36977706d900a71c74b2e66e7e46d2d6f8221e183cf`；测试APK为`7a23d7710d1c9babb0ac8586df37f6d3bbb3bfab4922d19155e77232804edd59`。签名SHA-256为`7957a084dc45d666a44a31314bf9e8809d17442beb817cfdbf67a4d1667b462f`。**这些APK早于最新原生首页、模板类型路由及新UI测试。**
- 审计在`work/logs/handoff-s10/`：完整Git状态、跟踪文件补丁、修改及未跟踪文件哈希、任务原文、`artifact-verification.json`。本地审计退出0，仅证明所列产物/副本符合检查，截图仍未通过。Git补丁不含未跟踪内容，须保留当前目录。

## 2. 变更文件清单

`src/`表示`app/src/main/java/com/nonen/Bookkeeping/`，`androidTest/`表示`app/src/androidTest/java/com/nonen/Bookkeeping/`。下表列出S10涉及文件，部分同时包含前序修改。

| 路径 | 改动及原因 |
| --- | --- |
| `src/core/nlp/ClassificationModelService.kt` | 应用0.015门槛，confidence保留真实最大概率，遵循既定模型规则。 |
| `app/src/test/java/com/nonen/Bookkeeping/core/nlp/ClassificationModelServiceTest.kt`、`androidTest/core/nlp/AndroidClassificationModelTest.kt` | 增加真实模型低置信度、金额及早餐名词验证；模型资源未修改。 |
| `src/ui/AppNavHost.kt`、`src/ui/screens/MainScreen.kt` | 增加日期、模板编辑/类型和统计账单路由，连接当前账本及实际编辑页面。 |
| `src/ui/screens/AddEditViewModel.kt`、`YimuAddEditScreen.kt`、`NumberKeyboard.kt` | 日历日期预填、收入分类订阅、表单账本选择、手续费、模板完整编辑及保存。 |
| `src/ui/screens/TemplateViewModel.kt`、`TemplateScreen.kt` | 按当前账本读取模板，复用正式表单创建/编辑；原版列表界面及新建类型回调未完成。 |
| `src/ui/screens/YimuCalendarScreen.kt` | 新增记账携带选中日期。 |
| `src/ui/screens/YimuStatisticsScreen.kt`、`StatisticsBillListScreen.kt` | 分类/子分类/标签进入真实账单列表，保留期间、账本和方向，Room Flow响应编辑删除。 |
| `src/ui/components/YimuTextView.kt`、`app/src/main/res/values/yimu_styles.xml` | 使用AppCompat参考Theme和整数像素尺寸匹配文字测量，已参加30号构建。 |
| `src/ui/screens/YimuHomeScreen.kt`、`src/ui/components/YimuNativeHomeList.kt` | 接入原生RecyclerView、汇总、日期分组与账单，处理测量及FAB遮盖下的控件采集；最新接入未构建。 |
| `app/src/main/res/layout/yimu_home_{summary,day,bill}.xml`、`values/yimu_native_home_ids.xml` | 复用原版原生结构、真实控件ID和尺寸。 |
| `app/build.gradle.kts`、`gradle/libs.versions.toml` | 显式加入已解析传递版本RecyclerView1.1.0；尚未参加构建。 |
| `app/src/main/res/mipmap-xxhdpi/ic_main_account_book.png` | 采用原版工具栏账本图标，来源/目标哈希相同；尚未安装。 |
| `app/src/main/res/layout/{view_yimu_template_manager,item_yimu_template_bill}.xml`、`mipmap-xxhdpi/yimu_template_*.png` | 准备模板原版布局及7项图片；尚未接入TemplateScreen或构建。 |
| `app/src/main/res/values/yimu_strings.xml` | 新增模板、手续费和统计列表文字。 |
| `androidTest/ui/YimuAddEditDateInstrumentationTest.kt`、`StatisticsBillListInstrumentationTest.kt` | 用1970条真实副本验证日期、收入分类、模板和统计；统计类返回类型已修正，待重跑。 |
| `androidTest/ui/YimuAddEditUiInstrumentationTest.kt` | 新增5项正式Compose控件操作测试，覆盖收支、转账、再记、编辑和删除；未构建运行。 |
| `androidTest/export/FullBackupServiceInstrumentationTest.kt`、`FullBackupProcessRecoveryInstrumentationTest.kt` | 验证20类表全字段、15项设置、附件，以及两阶段真实进程终止恢复。 |
| `androidTest/ui/YimuReferenceDataInstrumentationTest.kt`、`YimuCaptureInstrumentationTest.kt` | 导入核验源文件及明确数量/金额参数，正式截图要求必要控件清单非空。 |
| `work/ui-harness/{capture.py,dataset.py,verification.py,diff.py,screens.yaml}` | manifest版本3，记录相对路径、正式导出及实际SQLite快照，核验来源、纯色和任意64×64区域。 |
| `work/ui-harness/{window_action.py,snapshot_share.py,Run-S10Command.ps1,Install-S10Validation.ps1}` | 窗口控件检查、真实命令日志，以及安装前包名和签名核验。 |

`work/`、`reference_apk/`及个人数据目录继续忽略。前序删除的FinancialSyncTest、CategoryManageTest未重新加入，它们的真实Room验证在仪器测试中。

## 3. 关键决策与理由

- **【长期】参考与组件：** 依据原版APK、反编译源码和同一台真机；使用Material Components1.10.0、CalendarView3.7.1、MPAndroidChart v3.1.0、SwitchButton2.1.0，页面使用Compose与AndroidView。完全自行绘制复杂组件的方案未采用，原因是需要复用原版测量和交互。
- **【长期】原版资源：** 保留62个lineIcon和78项分类映射，来源/哈希见`work/ui-evidence/line-icon-hashes.json`；原版系统备份、私有数据库和凭据保持本地隔离，凭据不加入应用资源。
- **【长期】隔离与验证：** 用户包保留个人数据，参考导入、仪器测试和覆盖恢复限定`.uivalidation`；使用真实Room、DataStore、模型和操作。用户账户余额保持业务含义，不为截图调整；DAO替身不能作为事务验收依据。
- **【长期】截图标准：** 必要控件有效且检查数量大于零；整体≤1.5%、任意64×64区域≤6%、关键bounds≤3px、单通道容差8、纯色单通道差≤2。保持既定掩膜及数据/环境核验，人工口头确认不能代替报告。S10滑动区域步长为1px，应用APK变化后重新采集。
- **【长期】页面状态：** 底栏五项，导航文字默认关闭、开启11sp；账本共享至首页、日历、统计和新增账单并纳入备份。模板抽屉280dp，账本通过工具栏弹窗切换。
- **【长期】持久化：** Budget、CategoryBudget、BudgetSetting、Cycle、CycleLog、ModuleBill、Dream、DreamLog保持独立生命周期；模板在`module_bills`，愿望在`dreams/dream_logs`，保持独立表，不向transactions增加模板/愿望标记替代实体。CycleDate独立持久化要求仍待核实。
- **【长期】金额/迁移：** 金额列沿用Double及两位金额规则，键盘使用BigDecimal；保留Room版本6和增量迁移，历史默认bookId/assetId为1，历史记录可编辑。
- **【长期】分类：** 唯一索引`(categoryId,parentId,categoryType)`；一级ID9及其11项子分类为收入。唯一映射确定，多义分类保留二级null或独立isCustom业务ID；全局hide与hideBook独立，SQLite hide为0/1，hideBook为逗号分隔账本ID。保存实际分类ID并更新商户记忆；删除自定义分类保留账单文字和金额。
- **【长期】资金：** 增删改和余额在Room事务内原子一致，验证输入、关联、余额、编辑删除对冲及统计。保护默认现金账户1；删除其他账户时重定向历史并更新名称。信用账户欠款为`totalQuota-assetNumber`，超额余额计入资产；借入账户余额绝对值计入负债；净资产为总资产减负债。
- **【长期】统计：** 历史资产由当前余额减去时点后全部资金变化逆算，包含转账和手续费；期末值对应当前资产汇总。一级/二级分类统一聚合，二级占比分母为所属一级总额。
- **【长期】本地模型：** 保留`classification-model.ser`、`TimeExp.m`及LibLinear/HanLP，不重新训练或接入云端模型。门槛0.015，低于门槛归入`其他-其他`；`食品餐饮-其他`按[04:00,10:00)早餐、[10:00,16:00)午餐、其余晚餐处理。保留金额预处理`(块|元|点)([1-9一二三四五六七八九])[毛角]`到`点$2`及常见动词、首尾单位规则；中文金额完整流程仍需验收。
- **【长期】周期/备份：** 使用`cycle_{cycleId}_{yyyyMMdd}`及执行日志防重，事务内联动余额，每次执行后读取最新状态。23列Excel/CSV交换和完整备份分别处理；完整备份采用标准ZIP、manifest.json/data.json及SHA-256前置校验，失败保护数据库。
- **【长期】业务范围：** 保留本地支出、收入、转账，债务为视觉占位；会员、账号、云服务、语音、自动记账、桌面微件及房贷车贷等既定业务不扩展。原版只允许新增一个临时模板用于取证，记录标识并删除本次新增项；当前尚未执行。
- **S10采集与文字：** 原版由外部shell冷启动，持续式UIAutomator读取交互窗口/View ID并检查必要控件。文字采用AppCompat参考Theme和整数像素尺寸；首页进一步采用原生XML/RecyclerView，最新组件须重新取证，尚无通过截图的证据。
- **S10数据与中断：** 业务测试使用1970条用户数据库副本，截图使用原版正式导出的当前数据；原版增加至1973条后已重新导出并正式导入独立包。真实中断使用现有interruptionHook终止独立包进程，再以新PID核验持久化、附件和设置。

## 4. 已知坑与失败尝试

| 现象 | 原因／证据 | 正确做法及剩余状态 |
| --- | --- | --- |
| 最新源码尚不能直接续建 | TemplateScreen仍为无参onCreate，AppNavHost已传moduleType；原生首页和模板页面续接未验证 | 完成`(Int) -> Unit`接口、0=账单/1=转账入口及页面，再统一构建。 |
| 构建通过后继续编辑，源码与APK不同 | 30号通过后修改首页、RecyclerView、路由和测试 | 32号APK只证明对应版本；最新源码重新构建、安装两APK并更新固定副本。 |
| 首页数据不同 | 10号使用1971条旧导出，原版已有1973条，差额17.82元 | 13/23/24/34号取得并导入当前数据；原版再有变化时正式导出并更新screens.yaml来源及预期值。 |
| 同数据首页仍未通过 | 39号整体4.24596%、最差区域85.2539%，cost数量7/6；列表1—2px差异及FAB遮盖控件尚未验收 | 原生列表已接入、未运行；依据真实bounds和像素报告继续修正。 |
| JUnit初始化失败但ADB退出0 | 38号19项中18项通过，统计类方法应返回void，类未实际运行 | 当前源码6项日期/模板/统计测试显式Unit，待重跑；核查JUnit通过、失败和跳过项。 |
| 分享采集读到导出页或不稳定 | 第一次start_export生成文件，随后按钮为“分享文件”；15号包名不符，19/20号分享截图不稳定 | 根据真实按钮状态再次分享，通过MT管理器取得本地文件；不稳定截图未获验收，22/23号文件窗口和导出文件已核验。 |
| 旧设置导致启动异常 | S09 DataStore CorruptionException，原设置备份是72字节cat错误文本 | 两包已将精确匹配文本移至`files/yimu-recovery-s09/settings.command-error`，设置采用缺省值。没有原设置恢复来源；保留`work/backup/yimu-ui-s09-{full-verified,validation}/`。 |
| 后台启动等待、Activity列表为空 | S09 ActivityTaskManager返回102 | 外部shell启动并核验实际页面；S10 06号导航、31/35号采集通过。 |
| CLI dump遗漏底栏/分享面板 | 原生/Compose与交互窗口读取不完整，底层原因尚未确认 | 持续式UIAutomator配合交互窗口和View ID flags；正式截图核验包名/必要控件。窗口操作只有含launch时才冷启动，连续操作保留当前页面。 |
| 二进制读取/恢复不一致 | S09 shell -T读取改变字节、旧stdin恢复截断 | 读取用exec-out并比对远端SHA-256；恢复使用标准base64并逐字节读回，明确目标身份。 |
| 旧参考SHM与manifest不符 | S09 DB/WAL匹配、SHM不符，变化时机未确认，原核验退出1 | 保留原件及失败结果；旧快照在`work/logs/handoff-s09/room-reference-current-verified/`。S10另用核验后的SQLite backup，不修改旧manifest。 |
| basename哈希覆盖同名文件 | S09字典按文件名作键 | S10改为相对路径，另核验正式导出和真实数据库；manifest版本3要求重采集旧记录。 |
| 附件恢复目录不一致 | S09重复嵌套attachments | 统一附件目录；S10 20类表往返和两阶段真实中断通过，测试使用独立DB/DataStore。 |
| Gradle设备测试卸载用户包 | S08已有真实记录 | 独立包构建、显式安装和am instrument；禁止对用户包运行connectedDebugAndroidTest。 |
| CycleDate与schema要求不一致 | PeriodicEntities/schema6仅有cycles、cycle_logs及日期字段，T07文档提及CycleDate物理模型 | 保留待核实项，核对原版用途和原计划；本阶段保持Room版本6。 |
| 参考清单和全量回归未完成 | 仅当前首页形成新版manifest；夜间、非空模板抽屉、无键盘搜索及新增编辑等仍缺 | 扩充逐页状态和来源，旧PNG或单页结果不能代表整体完成。 |

继承防护：JVM JSON使用真实`org.json:json:20240303`；externalScope不能代替Android调度验证；TodayWatcher等待实际注册后取消；金额沿用Format.kt，Compose避免依赖不可观察的Locale.getDefault()。原版资源使用既有小米系统备份，47字节ADB空备份不能用于资源证明。

## 5. 环境与命令

- Windows PowerShell7.6.5、Python3.12.7、终端Java24.0.1；Gradle toolchain为JDK21，SDK/build-tools37，`GRADLE_USER_HOME=D:\dev-cache\.gradle`。Pillow12.2.0、NumPy1.26.4、PyYAML6.0.3、xlrd2.0.2可用；本阶段未安装Python依赖，证据见审计目录runtime.txt。
- 唯一运行设备小米15：`95f79212`，Android16/API36/arm64，1200×2670、密度520、字体1.0、zh-CN、手势导航。命令由PowerShell执行，Gradle与APK签名工具使用bat入口；没有使用VM bash或模拟器。本次交接仅读取本地证据。
- 组件由Gradle解析，UIAutomator2.3.0已参与构建；显式RecyclerView1.1.0尚未构建。参考资料在`work/ui-evidence/{apktool,jadx/sources}/`，模板适配器补充导出为`work/ui-evidence/module-adapter-he.java`。
- 本阶段完整命令/退出状态在`work/logs/yimu-ui-reconstruction-s10/`；`Run-S10Command.ps1`设置临时目录并保存日志，`Install-S10Validation.ps1`核验身份/签名后安装两APK和更新固定副本。

以下命令只在用户恢复实施、完成第7节源码续接后执行；完整输出存入新日志目录，逐条记录真实退出状态：

```powershell
Set-Location D:\github-repository\smart-record
$env:TEMP = (Resolve-Path work/tmp).Path
$env:TMP = $env:TEMP
$env:TMPDIR = $env:TEMP
$env:PYTHONUTF8 = '1'
$env:JAVA_TOOL_OPTIONS = '-Djava.io.tmpdir=D:/github-repository/smart-record/work/tmp'
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest '-PyimuUiValidation=true' --console=plain --max-workers=2

# 检查构建真实退出状态后，核验身份并安装独立包
& pwsh -NoProfile -File work/ui-harness/Install-S10Validation.ps1
$adb = 'D:\Android\Sdk\platform-tools\adb.exe'
$runner = 'com.nonen.Bookkeeping.uivalidation.test/androidx.test.runner.AndroidJUnitRunner'
& $adb -s 95f79212 shell am instrument -w -r -e class com.nonen.Bookkeeping.ui.YimuNavigationInstrumentationTest $runner
python work/ui-harness/capture.py --app reference --only home_top
python work/ui-harness/capture.py --app validation --only home_top
python work/ui-harness/diff.py --only home_top
```

- 业务类为`AndroidClassificationModelTest`、`YimuRealDataInstrumentationTest`、`FullBackupServiceInstrumentationTest`、`YimuAddEditDateInstrumentationTest`、`StatisticsBillListInstrumentationTest`、`YimuAddEditUiInstrumentationTest`；完整命名空间和调用见25/38号日志及对应源码。显式指定class并传`-e source_sha256 4ef7ce3c6ee0d466bbcce7a7c72f44007aea77a2a452410c3bc97de17bca7f43`，源文件为独立包external-files下`yimu-validation-source.db`。
- 数据准备类`YimuReferenceDataInstrumentationTest`单独调用，要求`export_sha256`、`export_count`、`export_expense_cents`、`export_income_cents`，当前值见第1节及34号完整命令；输入为独立包external-files下`yimu-current-reference.xls`。禁止无参数运行全部测试类。
- 真实中断测试：`FullBackupProcessRecoveryInstrumentationTest#interruptRestoreAtCheckpoint`传`allowProcessTermination=true`、`recoveryCheckpoint=STAGED`或`DB_COMMITTED`、唯一`recoveryRunId`；再以相同参数运行`#verifyRecoveryAfterProcessRestart`。完整命令见`work/logs/yimu-ui-reconstruction-s10/backup/README.md`及26—29号日志。第一条产生进程终止报告，第二条必须JUnit通过；只使用独立命名的磁盘DB和DataStore。
- 用户包最终单独运行`.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain --max-workers=2`，不传独立包属性；安装前运行`python work/ui-harness/device_backup.py --output work/backup/<新目录>`，核验身份/签名后使用`adb install -r`。
- 恢复脚本须指定`--validation`或`--production`，明确`--backup`并核验manifest身份、哈希和总额；`prepare_room_fixture.py`要求新的目标目录。当前截图快照不能未经核验直接覆盖手机。

## 6. 验证状态

下表日志位于`work/logs/yimu-ui-reconstruction-s10/`。本次交接重读完整输出及本地副本，没有重新运行构建或设备测试。

| 项目 | 真实证据 | 适用范围 |
| --- | --- | --- |
| 分类模型 | model目录01号退出0，9项真实模型JVM通过；25/38号Android模型通过 | 门槛、早餐名词和指定语句，模型文件哈希保持一致。 |
| 完整JVM/Lint/两APK | 04、30号BUILD SUCCESSFUL、退出0；本轮JUnit XML汇总161项，0失败/错误/跳过 | 早于最新原生首页、依赖、模板类型路由及5项UI测试。 |
| 身份/签名/安装/导航 | 05/32号退出0，06号OK (1 test) | 对应独立包；用户包未参与。 |
| Room/备份/日期收入编辑 | 25号OK (16 tests)、退出0 | 包含1970条副本、20类表全字段、15项设置、附件及周期资金；不能代替全部界面流程。 |
| 真实进程终止恢复 | 26/28号Process crashed，27/29号各OK (1 test)、退出0 | STAGED和DB_COMMITTED检查点、新PID、磁盘DB/附件/DataStore；其他终止时机未覆盖。 |
| 模板/统计扩展测试 | 38号19项，1项初始化失败，ADB退出0；其余18项通过 | 两项模板业务已运行；统计类未运行，Unit修正尚未重验。 |
| 正式Compose操作 | YimuAddEditUiInstrumentationTest已有5项源码 | 未编译运行，收支/再记/转账/编辑删除UI仍未验收。 |
| 当前参考数据 | 13/23/24号核验，34号OK (1 test)，35号SQLite快照 | 原版正式导出1973条、78分类、空资产。 |
| 首页截图 | 31/35号采集退出0，39号比较退出1 | 整体4.24596%、任意64×64最差85.2539%、cost数量7/6；最新原生首页无截图。 |
| 本轮产物及本地副本 | `work/logs/handoff-s10/artifact-verification.log`退出0 | 文件哈希、本地1970/1973条副本及JUnit XML；没有手机最新数据或完整交付结论。 |

**高风险未验证：** 当前源码构建、原生首页、模板原版页面、统计类、实际Compose操作、全部页面及深浅主题像素检查、完整操作和数据保留安装。**中风险未验证：** 日历月周/跨月、多账本、年度热力/筛选、多选导出、二级新增编辑、字体缩放及键盘遮挡。CycleDate及原设置恢复来源仍未解决。API26和平板没有运行证据，本阶段不扩展模拟器矩阵。

## 7. 下一阶段交接

### 第一步

1. 读取本文件、`work/tasks/yimu-ui-reconstruction.md`及`work/logs/yimu-ui-reconstruction-s10/pause/README.md`，运行`git status --short`，对照`work/logs/handoff-s10/source-before.csv`核查当前源码和用户修改。当前继续暂停，收到恢复实施指令后才修改应用或操作设备。
2. 完成TemplateScreen的`onCreate: (Int) -> Unit`、账单/转账创建入口及原版页面，配合`Routes.editTemplate(0L,moduleType)`和`initialTransactionType`；参考已有两个模板XML、7项图片、ModuleManagerActivity、item_module_bill和module-adapter-he.java，保留完整表单业务。
3. 核查并构建YimuNativeHomeList及三个首页XML，其内部已含15dp横向边距，外层不重复添加。无账户显示时asset_icon/asset为GONE，保留账户区域5dp顶部间距；以实际bounds核验测量。
4. 按第5节构建安装最新独立两APK，运行导航、统计及实际Compose操作，再采集首页。核验原版当前数据是否变化，变化时正式重新导出；首页通过后继续全部页面与主题。

### 完成标准

- 完成阶段2—9剩余交互、全部二级页面和深浅主题，真实验证账本、模板、分类、资金、日历、统计及导入导出/备份。
- 补齐非空模板抽屉、无键盘搜索、夜间及新增编辑参考；临时原版模板记录标识，采集后仅删除该项并核对原版账单数量。
- 全部页面来源、安装APK、manifest、数据和必要控件有效，满足第3节全部截图标准；缺失或错误页面不能通过。
- 最新源码通过完整JVM、Lint和独立业务/UI验证；单独构建用户包，核验身份、签名、安装前最新备份及安装后数据保留，交付APK、SHA-256、逐页差异报告和日志。

### 禁止事项

- 本次只交接并提交HANDOFF.md，不恢复应用实施、构建、安装、设备操作或提交其他源码。
- 不对用户包运行会卸载应用的仪器测试，不清除/覆盖个人数据或修改现金余额满足截图；参考导入和恢复限定独立包。
- 不使用Git恢复代码，不读写系统Temp，不主动调用视觉工具，不使用mock、伪造数据/截图、空控件清单或放宽阈值报告通过；源码使用文件编辑工具修改。
- 不新增网络权限或账号/云服务/会员/语音/自动记账/微件等既定业务，不启动/下载模拟器，不重启手机或修改网络、系统账号及权限。
- 状态与完整日志保存在忽略的work目录，阶段交接整体重写根目录HANDOFF.md；旧版通过`git show 4c19e2c:HANDOFF.md`读取。继续使用tx-guideline；界面和原版取证分别遵循mobile-android-design、reverse-engineer-anything。
