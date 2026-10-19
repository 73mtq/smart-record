# HANDOFF — S09 一木记账界面还原

## 1. 状态

- **阶段S09；功能进行中，按用户要求暂停。** 界面计划阶段2—9尚未完成验收，尚无完成全部验收的用户APK。本次交接只提交HANDOFF.md。
- **当前限制：** 最新首页文字、底栏及截图必要控件检查仅构建通过，尚未安装；全部页面像素验收、二级页面及夜间参考仍待完成。
- **目录与Git：** `D:\github-repository\smart-record`，分支`main`，交接前HEAD为`e51d221`（S08）。大量源码、资源和测试为本机未提交内容；单独检出交接提交无法恢复全部实现，继续使用当前目录。
- **依据：** `docs/plans/yimu_exact_ui_reconstruction_plan.md`、`work/tasks/yimu-ui-reconstruction.md`。已重新核对Git状态/差异、旧HANDOFF全文及用户实施、暂停、交接原文；审计在`work/logs/handoff-s09/`，原文见`user-requests.json`的source第101、1186、1218行记录。
- **最后命令：** 83号日志记录独立应用及测试APK构建成功，退出0；此后未安装或操作设备，共享Gradle daemon保持原状。本轮交接仅检查本地证据、重写及提交文档。
- **三个应用：** 用户包`com.nonen.Bookkeeping`；参考包`com.wangc.bill` 6.1.8/versionCode378；独立包`com.nonen.Bookkeeping.uivalidation`。

### 数据与产物

- 用户备份`work/backup/yimu-ui-s09-current/`：1970条，支出`-43521.15`、收入`32012.48`，94项分类、1个账户，完整性ok，设置文件缺失。数据库SHA-256：`4ef7ce3c6ee0d466bbcce7a7c72f44007aea77a2a452410c3bc97de17bca7f43`。这是最后备份及本轮本地副本核验结果，本轮未重新检查手机。
- 原版真实导出`work/ui-fixtures/yimu_all_1971_20261019.xls`：1971条，SHA-256：`8695fcd627e46957f45ac18ef3e0f20a31fb83b6fb7510c0a1a48393ec157765`。当天早餐属于原版已有数据，本阶段未新增或删除原版账单，临时模板尚未创建。
- 独立包经正式解析器导入1971条，支出`-43523.35`、收入`32012.48`，78项分类、空资产；77、79号日志及`work/ui-fixtures/room-reference-current/`为证据。用户包保持1970条。
- 本轮参考备份核验：SHM与manifest哈希不同，DB/WAL匹配，复制后SQLite完整性和总额通过；核验脚本因此退出1，不能报告全部哈希通过。详见`work/logs/handoff-s09/artifact-verification.json`。SQLite backup完整快照位于`work/logs/handoff-s09/room-reference-current-verified/`，数据库SHA-256：`d706542edc8ee9521b8cf99f3f8509eb7f5bc9d7c698cfb65f9db72793313272`，尚未用于手机恢复。
- 最新`app/build/outputs/apk/debug/app-debug.apk`为`.uivalidation`、`0.2.3-ui-validation`，SHA-256：`3635da39a3aa4b69912670f917776993959f8431e1e985e218c4311365ba311e`；固定副本`work/artifacts/yimu-ui-validation.apk`仍较早（SHA-256：`7adfeddbda7e37c45d95e37579124e468231d0135bc6e34f8d5dfce52a8c9b7a`）。最新应用与测试APK均未安装；完整大小、哈希及包名证据在本轮审计目录。

## 2. 变更文件清单

本节`src/`表示`app/src/main/java/com/nonen/Bookkeeping/`，`androidTest/`表示`app/src/androidTest/java/com/nonen/Bookkeeping/`。列出S09涉及文件；部分文件还含前序改动。完整当前状态、跟踪文件补丁见`work/logs/handoff-s09/git-status-before.txt`、`tracked-changes-before.patch`；补丁不含未跟踪文件内容。

| 路径 | 改动说明与原因 |
| --- | --- |
| `app/build.gradle.kts` | 增加AndroidX UIAutomator 2.3.0，持续读取原生与Compose控件。 |
| `src/BookkeepingApp.kt` | 注入真实AppDatabase、分类和账户Repository，支持事务与模板业务。 |
| `src/data/repo/TransactionRepository.kt` | 账单增删改、转账及删除转账进入Room事务，联动账户余额。 |
| `src/data/repo/TemplateRepository.kt` | 保存真实分类、收入方向、账户、标签及转账，保持模板业务含义。 |
| `src/data/db/CategoryDao.kt`、`src/data/repo/CategoryRepository.kt` | 按原版positionWeight降序展示并对应写入排序权重。 |
| `src/data/prefs/SettingsStore.kt` | 接受真实DataStore实例，使备份测试使用独立设置文件。 |
| `src/stats/StatsCalculator.kt` | 资产走势纳入转出金额、手续费及转入金额。 |
| `src/ui/AppNavHost.kt`、`src/ui/screens/AddEditViewModel.kt` | 新增templateId路由与预填，更新收入/支出默认分类及模板模式。 |
| `src/ui/screens/YimuAddEditScreen.kt` | 二级分类在所属行下方展开，接入模板转账模式。 |
| `src/ui/screens/MainScreen.kt` | Pager与原生底栏使用Column，导航区域计入原生内部padding，标签11sp。 |
| `src/ui/screens/YimuHomeScreen.kt` | 增加模板应用、多选统计/Excel导出、原生悬浮按钮；按原版测量调整工具栏、汇总与账单文字。 |
| `src/ui/components/YimuTextView.kt` | 使用真实TextView控制字号、字体内边距和控件ID，处理文字测量差异。 |
| `src/ui/screens/YimuCalendarScreen.kt`、`YimuAssetContent.kt` | 增加月份联动、月份选择和关键控件标识。 |
| `src/ui/screens/YimuStatisticsScreen.kt`、`StatsCharts.kt` | 增加资产走势、年度热力、图表ID及主题文字颜色。 |
| `src/ui/screens/YimuSettingsPage.kt` | 使用stringResource读取主题名称，满足Compose资源读取要求。 |
| `src/export/FullBackupService.kt` | 统一附件导出/恢复目录，检查格式版本与摘要。 |
| `app/src/main/res/values/yimu_ids.xml`、`yimu_styles.xml`、`mipmap-xxhdpi/ic_add_ai_bill.png` | 提供原生控件ID、底栏文字样式和原版按钮图片。 |
| `androidTest/ui/YimuNavigationInstrumentationTest.kt` | shell明确启动独立包，检查包名、底栏与五个页面。 |
| `androidTest/ui/YimuCaptureInstrumentationTest.kt` | 持续式UIAutomator执行真实操作，读取交互窗口、双次截图与必要控件。 |
| `androidTest/ui/YimuReferenceDataInstrumentationTest.kt` | 限定独立参考环境，使用正式解析器导入原版真实Excel。 |
| `androidTest/data/YimuRealDataInstrumentationTest.kt` | 复制1970条真实数据库，验证资金、模板、分类及设置备份往返。 |
| `androidTest/export/FullBackupServiceInstrumentationTest.kt` | 隔离真实DataStore，验证附件、设置、异常输入与周期事务。 |
| `app/src/test/java/com/nonen/Bookkeeping/{data/repo/FinancialSyncTest.kt,ui/screens/CategoryManageTest.kt}` | 相应验证迁至真实Room仪器测试，原JVM文件删除。 |
| `work/ui-harness/{capture.py,diff.py,ui_action.py,window_action.py,snapshot_share.py,geometry_report.py}` | 持续式采集、窗口操作、全部同名控件比较和几何诊断，防止接受错误页面。 |
| `work/ui-harness/{device_backup.py,device_restore.py,prepare_room_fixture.py,recover_invalid_settings.py}` | 归档成员/远端哈希检查、明确包身份、缺失设置记录及精确内容恢复保护。 |

`work/`、`reference_apk/`已忽略；个人账单、数据库、系统备份及APK保持本地保存。本轮交接未修改应用源码，审计副本在忽略目录中。

## 3. 关键决策与理由

- **【长期】参考与组件：** 以原版APK、反编译源码、同一台真机为依据；使用Material Components 1.10.0、CalendarView 3.7.1、MPAndroidChart v3.1.0、SwitchButton 2.1.0，页面使用Compose与AndroidView。未采用全部自行绘制复杂组件的方案，原因是需要复用原版测量和交互。
- **【长期】原版资源：** 保留62个lineIcon及78项原版分类映射，来源/哈希见`work/ui-evidence/line-icon-hashes.json`；采用原文件满足个人还原要求。原版系统备份及私有数据库留在忽略目录，账号凭据不得加入应用资源。
- **【长期】隔离与验证：** 用户包保留个人数据，参考数据、仪器测试和覆盖恢复限定`.uivalidation`；不调整用户现金余额满足截图。测试使用真实Room、DataStore和操作，DAO替身不作为事务验收依据。
- **【长期】截图标准：** 必要控件有效且检查数量大于零；整体差异≤1.5%、任意64×64区域≤6%、关键bounds≤3px、单通道容差8、纯色单通道差≤2。保持既定掩膜，核查数据差异来源，人工口头确认不能代替报告。
- **本阶段采集：** 采用持续式UIAutomator及交互窗口/View ID标志。单次CLI dump、仅调整semantics或容器仍未解决全部定位问题。原版在外部shell冷启动，再由测试进程读取，避免在该测试进程内等待原版启动。最新必要控件检查仅编译通过。
- **本阶段文字：** 首页使用真实TextView和ImageView核查字形、尺寸及点击范围；单纯调整Compose字号/行高尚无通过像素检查的证据。最新测量改动仍需真机比较。
- **【长期】页面状态：** 底栏五项，导航文字默认关闭，开启后11sp；账本选择共享至首页、日历、统计和新增账单，并纳入备份。首页模板抽屉280dp，账本通过工具栏弹窗切换。
- **【长期】持久化：** Budget、CategoryBudget、BudgetSetting、Cycle、CycleLog、ModuleBill、Dream、DreamLog保持独立生命周期；模板在`module_bills`，愿望在`dreams/dream_logs`。不向transactions添加模板/愿望标记替代独立表。旧版CycleDate独立持久化要求保留为待核实项，见第4节。
- **【长期】金额/迁移：** 金额列沿用Double和两位金额规则，BigDecimal用于键盘计算；Room版本6及增量迁移保留，历史默认bookId/assetId为1，历史记录保持可编辑。
- **【长期】分类：** 唯一索引`(categoryId,parentId,categoryType)`；默认一级ID9及其11项子分类为收入。唯一映射保持确定性，多义分类保留二级null或独立isCustom业务ID。全局hide与hideBook独立，SQLite hide为0/1，hideBook为逗号分隔账本ID。保存写入parentCategoryId/childCategoryId并更新商户记忆；删除自定义分类只解除关联，保留账单文字和金额。
- **【长期】资金：** 验证输入、持久化/关联、余额、编辑删除对冲及统计口径；增删改与余额原子一致。保护用户默认现金账户1，删除其他账户时重定向历史记录并更新名称。信用账户欠款使用`totalQuota-assetNumber`，超额余额计入资产；借入账户余额绝对值计入负债；净资产为总资产减负债。
- **【长期】统计：** 历史资产由当前余额减去时点后全部资金变化逆算，包含转账及手续费；期末值对应当前资产汇总。一级/二级分类统一聚合，二级占比分母为所属一级总额。
- **【长期】本地模型：** 保留`classification-model.ser`、`TimeExp.m`及LibLinear/HanLP，不重新训练或接入云端模型。置信度要求0.015，低于门槛归入`其他-其他`；`食品餐饮-其他`按[04:00,10:00)早餐、[10:00,16:00)午餐、其余晚餐处理，门槛实现待核查。金额预处理保留`(块|元|点)([1-9一二三四五六七八九])[毛角]`到`点$2`及常见动词、首尾单位规则；中文金额完整流程未重新验收。
- **【长期】周期/备份：** 周期使用`cycle_{cycleId}_{yyyyMMdd}`及执行日志幂等，在Room事务内联动余额，每次执行后读取最新状态。23列Excel/CSV交换与完整备份分开；完整备份采用标准ZIP、manifest.json/data.json及SHA-256前置校验，失败保护数据库。20类表全非空、真实进程中断一致性尚未全部覆盖。
- **【长期】范围：** 保留本地业务及支出、收入、转账；债务为视觉占位。会员、账号、云服务、语音、自动记账、桌面微件及房贷车贷等既定业务不扩展。原版临时模板仅允许新增一个用于取证，记录标识并删除本次新增项；尚未执行。

## 4. 已知坑与失败尝试

| 现象 | 原因／证据 | 正确做法与剩余状态 |
| --- | --- | --- |
| 独立包启动后读取桌面 | 01号日志有DataStore CorruptionException；旧设置备份为72字节cat错误文本，exec-out退出0不足以证明读取成功 | 两包精确核验后保存该文本至`files/yimu-recovery-s09/settings.command-error`，当前缺省设置，不能宣称恢复原设置。归档见`work/backup/yimu-ui-s09-{full-verified,validation}/`。 |
| 测试启动等待、Activity列表为空 | ActivityTaskManager记录后台启动请求返回102 | shell明确启动、检查包名及真实页面；31号通过。旧失败不能归因于JUnit/Espresso版本。 |
| CLI dump遗漏底栏或分享面板 | 原生/Compose与交互窗口读取不完整，具体底层原因未确认 | 持续式UIAutomator配合FLAG_RETRIEVE_INTERACTIVE_WINDOWS、FLAG_REPORT_VIEW_IDS；31、69号取得真实节点。 |
| 包名正确但截图为导出页 | 原版热启动保留二级页面；80号XML为账单导出页，82号无首页结论 | 重采集home_top；最新工具加入原版force-stop及必要控件检查，尚未运行验证。 |
| 首页多出当天早餐 | 原版1971条，旧参考数据1970条，差额2.20元 | 原版本地导出及正式解析器已更新独立包；77号核对通过，用户包不增加该记录。 |
| 二进制读取/恢复不一致 | 本机shell -T读取曾改变字节，旧stdin恢复曾截断；03号与S08日志27—29 | 读取用exec-out并比对远端SHA-256；写入用标准base64并逐字节读回。禁止直接用二进制stdin恢复。 |
| 参考备份SHM哈希不符 | 本轮发现SHM变化，发生时机未确认；DB/WAL匹配，SQLite副本完整性ok | 保留原件及失败结果，可用本轮SQLite backup快照；不要修改原manifest掩盖差异，恢复前检查目标身份及数据。 |
| fixture哈希不能区分全部文件 | `capture.py:fixture_hashes`按basename作键，多个目录存在同名db/manifest且包含可变化的SHM | 后续核查相对路径及实际数据集标识；当前未修复，不以全局fixture字典单独判定数据一致。 |
| 附件恢复后路径不符 | filesDirProvider指向根目录，导出曾再次嵌套attachments；52号失败 | 导出/恢复统一attachments目录；57号12项通过，原生ZIP路径校验错误也纳入实际断言。 |
| 仪器测试卸载用户包 | S08 Gradle设备测试曾发生卸载 | 仅对独立包运行；本阶段显式安装、am instrument。ADB退出码与测试结果不同，须检查JUnit的OK及失败项。 |
| 构建与手机/固定副本不同 | 最新83号未安装，本轮SHA与固定副本不同 | 核验包名、签名、SHA，安装最新应用和测试APK，更新apk_hint副本后重新采集。 |
| 分类门槛与继承要求不同 | 当前ClassificationModelService以maxProb=0.0选择类别 | 核查0.015门槛及真实模型结果，不能声明已生效；早餐名词保护的前序修复保留。 |
| CycleDate独立表要求不明确 | 当前PeriodicEntities/schema6只有cycles、cycle_logs及日期字段，限定搜索未找到CycleDate Entity | 继承要求并核对原计划含义；本轮未修改schema，不写为已实现。 |
| 参考清单及完整回归未完成 | 旧PNG/manifest混有不同版本，部分页面缺夜间及新增编辑参考 | 逐页核查必要控件及全部截图；设置、附件、核心数据测试不能代替20类表和真实中断全部验证。 |

继承的防护：纯JVM JSON使用真实`org.json:json:20240303`；ViewModel可注入externalScope，UI仍需Android调度验证；TodayWatcher等待实际注册后取消；金额格式沿用Format.kt，Compose内不要依赖不可观察的Locale.getDefault()。原版图片使用既有小米系统备份，47字节ADB空备份不能用于资源证明。

## 5. 环境与命令

- Windows PowerShell 7.6.5、Python 3.12.2、终端Java 24.0.1；Gradle toolchain为JDK21，Android SDK/build-tools37，GRADLE_USER_HOME为`D:\dev-cache\.gradle`。
- 唯一运行设备：小米15，`95f79212`，Android16/API36/arm64，1200×2670、密度520、字体1.0、简体中文、手势导航。本轮交接未操作设备，没有使用VM bash或模拟器。
- 依赖由Gradle解析，UIAutomator 2.3.0已参与构建。原版资源/反编译代码在`work/ui-evidence/{apktool,jadx/sources}/`；既有工具`work/deps/miui-backup-extractor/`保留，无需重新安装。

以下设备、构建命令仅在用户明确恢复实施后执行；完整输出写入新日志并记录真实退出状态：

```powershell
Set-Location D:\github-repository\smart-record
$env:TEMP = (Resolve-Path work/tmp).Path
$env:TMP = $env:TEMP
$env:TMPDIR = $env:TEMP
$env:PYTHONUTF8 = '1'
$env:JAVA_TOOL_OPTIONS = '-Djava.io.tmpdir=D:/github-repository/smart-record/work/tmp'

.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest '-PyimuUiValidation=true' --console=plain --max-workers=2
$adb = 'D:\Android\Sdk\platform-tools\adb.exe'
& 'D:\Android\Sdk\build-tools\37.0.0\aapt2.exe' dump badging app/build/outputs/apk/debug/app-debug.apk
& $adb -s 95f79212 install -r app/build/outputs/apk/debug/app-debug.apk
& $adb -s 95f79212 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
Copy-Item app/build/outputs/apk/debug/app-debug.apk work/artifacts/yimu-ui-validation.apk
& $adb -s 95f79212 shell am start -W -n com.nonen.Bookkeeping.uivalidation/com.nonen.Bookkeeping.MainActivity
& $adb -s 95f79212 shell am instrument -w -r -e class com.nonen.Bookkeeping.ui.YimuNavigationInstrumentationTest com.nonen.Bookkeeping.uivalidation.test/androidx.test.runner.AndroidJUnitRunner

python work/ui-harness/capture.py --app reference --only home_top
python work/ui-harness/capture.py --app validation --only home_top
python work/ui-harness/diff.py --only home_top

# 最终用户包另行构建，核查身份后再安装
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain --max-workers=2
```

- Room/备份12项：显式指定`com.nonen.Bookkeeping.data.YimuRealDataInstrumentationTest,com.nonen.Bookkeeping.export.FullBackupServiceInstrumentationTest`，传入`-e source_sha256 4ef7ce3c6ee0d466bbcce7a7c72f44007aea77a2a452410c3bc97de17bca7f43`。源文件在独立包external-files下`yimu-validation-source.db`；数据准备与截图类单独调用，避免无参数运行全部测试类。结果见57号日志，原命令见会话审计。
- 安装用户包前：`python work/ui-harness/device_backup.py --output work/backup/<新目录>`。恢复脚本必须指定`--validation`或`--production`；旧默认参考目录不适合作为当前数据源，需明确传`--backup`并检查manifest身份、哈希及总额。prepare_room_fixture.py要求新目标目录，不能覆盖重跑。
- 本轮`work/logs/handoff-s09/verify_artifacts.py`退出1的原因见第4节；它只生成本地核验副本，没有恢复手机。

## 6. 验证状态

实施日志：`work/logs/yimu-ui-reconstruction-s09/`。本轮交接只重读日志、核验本地文件和数据副本，未重新运行构建或真机测试。

| 项目 | 真实证据 | 适用范围 |
| --- | --- | --- |
| 五标签/原生控件 | 31、39号：OK (1 test) | 当时安装版本，最新底栏测量未安装 |
| Lint | 49号：BUILD SUCCESSFUL、EXIT_CODE=0 | 此后仍有备份、日历、首页等修改 |
| JVM/构建 | 54号：EXIT_CODE=0；当前JUnit XML共159项，0失败/错误/跳过 | 5项原JVM验证迁至Android，后续界面修改未完整回归 |
| Room、备份、附件、设置和周期 | 57号：OK (12 tests)，外层检查成功 | 独立真实数据库及DataStore；含1970条副本备份往返，未覆盖所有表全非空和真实进程中断 |
| 原版当前数据导入 | 77号：OK (1 test)；79号及本轮SQLite核验 | 独立包1971条、78分类、空资产，用户备份仍1970条 |
| 最新应用/测试APK | 83号：BUILD SUCCESSFUL、EXIT_CODE=0 | 编译与打包通过，尚未安装 |
| 截图比较 | 61号有数据差异，82号原版页面不符 | 均未通过正式验收，不以其比例说明最新界面质量 |
| 本轮文件/数据核验 | `work/logs/handoff-s09/artifact-verification.json` | DB/WAL和总额通过，参考SHM不符，文件级核验整体退出1 |

**高风险未验证：** 最新原生文字/底栏、截图必要控件和fixture标识；完整支出/收入/转账UI保存、编辑删除及再记；模板预填/锁定、多选导出；全部页面像素比较、分类迁移、真实恢复中断及20类表覆盖。**中风险未验证：** 日历跨月/月份选择、多账本、年度热力与筛选、二级新增编辑、夜间、字体缩放、键盘遮挡。API26和平板没有本阶段运行证据，不扩展模拟器矩阵。

## 7. 下一阶段交接

### 第一步

读取本文件、`work/tasks/yimu-ui-reconstruction.md`及当前源码，执行`git status --short`。当前保持暂停；收到恢复指令后，按第5节核验并安装最新独立应用和测试APK、更新固定副本，运行五标签检查，再重采集home_top两侧真实页面。记录真实退出状态，必要控件或页面不符时停止该次比较。

### 完成标准

1. 完成阶段2—9剩余交互、全部二级页面和深浅主题；真实操作验证账本、模板、分类、资金、日历及统计。
2. 补齐非空模板抽屉、无键盘搜索、夜间及新增编辑参考；临时原版模板记录标识，采集后仅删除该项并核对原版账单数量。
3. 核验截图数据集标识、全部manifest来源和必要控件，全部页面满足第3节标准，数据与环境一致。
4. 最新源码通过完整JVM、Lint及独立包业务/UI验证；单独构建用户包，核验身份、签名、安装前最新备份和保留数据安装结果，交付APK、SHA-256、完整差异报告及日志。

### 禁止事项

- 本次只交接并提交HANDOFF.md，保持界面实施暂停；后续以用户恢复指令为准。
- 禁止对用户包运行会卸载应用的仪器测试、清除/覆盖个人数据或修改账户余额满足截图；1971条参考数据仅用于独立包。
- 禁止Git恢复代码、读写系统Temp、主动调用视觉工具、伪造测试数据/截图、放宽阈值或用空控件清单报告通过。源码修改使用文件编辑工具。
- 不新增网络权限或账号/云服务/会员/语音/自动记账/微件等既定业务，不启动/下载模拟器，不重启手机或修改网络、系统账号及权限。
- 状态和日志留在忽略的work目录；交接整体重写根目录HANDOFF.md，旧版通过`git show e51d221:HANDOFF.md`等历史读取。建议继续使用tx-guideline；界面与APK取证分别按mobile-android-design、reverse-engineer-anything执行。
