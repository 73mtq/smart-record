# 一木记账界面还原交接文档

## 当前状态

- 日期：2026-10-18。用户要求暂停，本次命令均已结束。
- 阶段2至9尚未完成验收，1:1界面还原尚未完成。
- 任务状态：`work/tasks/yimu-ui-reconstruction.md`。
- 完整日志：`work/logs/yimu-ui-reconstruction/`。
- 当前工作区包含此前工作和本次工作的大量未提交改动；恢复时重新核查源码与用户修改。

## 已加入的内容

- 五标签Pager和原版BottomNavigationView；独立设置、主题、导入导出及备份路由；快捷入口处理。
- 共享账本选择；首页、日历、统计和新增账单的账本参数。
- 首页工具栏、月汇总、日期分组、账本选择、模板抽屉及多选删除。
- 记账四标签、五列分类、二级分类选择、属性区域和数字键盘；纯Kotlin金额状态及真实输入测试。
- CalendarView 3.7.1月视图和周视图、自定义日期金额与农历绘制。
- 资产主页、四种统计页面和MPAndroidChart组件。
- 原版SwitchButton设置页面；导航文字、备注优先、分类精简、显示时间和账户显示设置及备份字段。
- 原版分类图片和本地显示资料；我的页面、部分二级页面工具栏。
- 截图工具的逐页manifest、双次稳定性、环境和哈希核验、必要控件检查。

上述内容仍需逐项检查功能、布局、主题和像素差异。

## 真实验证结果

| 项目 | 结果与日志 |
| --- | --- |
| 构建基线 | 成功，退出状态0；`01-baseline-build.log` |
| 分类模型测试 | 7项通过，退出状态0；`05-classification-test.log` |
| 数字键盘测试和构建 | 成功，退出状态0；`06-calendar-keyboard-build.log` |
| 完整单元测试和构建 | 164项通过、0失败，退出状态0；`10-main-pages-build-test.log` |
| 最新独立验收包构建 | 成功，退出状态0；`33-validation-build.log` |
| 独立验收包安装和真实数据写入 | 成功，退出状态0；`34-validation-install.log`、`35-validation-fixture-restore.log` |
| 五标签仪器测试 | 未通过启动或页面检查；`21`、`22`、`23`、`25`号日志 |
| 最新截图采集 | 失败，退出状态1；`36-validation-initial-capture.log`，实际窗口为`com.miui.home`，预期为验收包 |
| 最新完整回归、Lint和正式截图差异 | 尚未完成 |

完整164项测试之后存在新的界面、设置、分类迁移和资源改动，需要重新运行完整回归。

## 用户应用与数据

- 用户包：`com.nonen.Bookkeeping`。
- 仪器测试工具卸载被测应用后，已重新安装用户包，并恢复安装前保存的数据库和设置。
- 恢复日志：`30-production-data-restore.log`，退出状态0；手机读回文件与恢复文件逐字节一致。
- 账单1970条，支出`-43521.15`、收入`32012.48`；现金账户原有状态由完整数据库保留。
- 原始备份：`work/backup/yimu-ui-before-install/`，包含数据库、WAL、SHM和DataStore设置。
- 恢复数据库：`work/recovery/yimu-ui-production/bookkeeping.db`，SHA-256为`4ef7ce3c6ee0d466bbcce7a7c72f44007aea77a2a452410c3bc97de17bca7f43`。
- 设置SHA-256：`3f3e819c2c3cf7666ba86dc74c2ebec020f251b71d1f82dfd01196f09b9caa3f`。
- 用户包恢复后已正常启动。不得对用户包执行会卸载应用的仪器测试。

## 独立验收环境

- 构建参数：`-PyimuUiValidation=true`。
- 包名：`com.nonen.Bookkeeping.uivalidation`。
- Activity完整名称：`com.nonen.Bookkeeping.MainActivity`。
- 固定APK副本：`work/artifacts/yimu-ui-validation.apk`。
- 当前`app/build/outputs/apk/debug/app-debug.apk`也是独立验收包。生成用户包时不传上述参数，并使用aapt2核验实际包名。
- 独立真实Room参考数据：`work/ui-fixtures/room-reference/`，含1970条真实记录、原版78项分类、参考账本名称及原版空资产状态；完整性和总额均已核验。
- 独立包与用户包使用各自的应用数据目录。
- Windows adb二进制stdin传输已观察到截断；数据恢复脚本使用标准base64经`adb shell -T`传输，并对手机读回内容进行逐字节核查。

## 原版资源与参考

- 参考应用：`com.wangc.bill` 6.1.8，versionCode378。
- 设备：小米15，`95f79212`，1200×2670，密度520，Android16/API36。
- 参考APK：`reference_apk/base.apk`，SHA-256为`e5ec945aa290c6f66fcd43c3f4307eebc5c7796d188e41895dcdf7fed599f8fd`。
- 静态证据：`work/ui-evidence/spec.md`、`apktool/`、`jadx/sources/`。
- 原版截图与UI层级：`work/ui-capture/ref/`；复刻截图：`work/ui-capture/impl/`。
- 小米系统备份：`work/ui-evidence/yimu-system-backup.zip`，SHA-256为`441875B372906EDC2B47A6E54A88E2BBA0A6BE59767A59994CE2DA42FBB15378`。
- 手机备份文件：`/sdcard/MIUI/backup/AllBackup/20261018_215123.zip`，本次仅备份一木记账。
- 第三方解包工具位于`work/deps/miui-backup-extractor/`，commit为`02b3c87d89e270bf68ad0d5a40604d49f25394f4`。
- 已取得62个`lineIcon`文件及原版默认图标，分类图片映射共78项；图片哈希位于`work/ui-evidence/line-icon-hashes.json`。
- `yimu-profile.json`仅包含从原版数据库导出的昵称和注册时间，用于本地显示；账号凭据没有加入应用资源。
- 原版数据库、完整备份和个人账单仅保存在忽略的`work/`目录中。
- 本次尚未新增原版临时模板，原版账单没有因取证发生新增或删除。
- 本次没有改变原有动画缩放、常亮和导航模式设置。

## 恢复后的工作入口

1. 等待用户明确恢复任务，重新读取任务状态、当前源码和git status。
2. 核查独立验收包实际启动结果及应用错误日志；解决UI层级和底栏辅助功能访问。
3. 完成首页弹窗、模板应用、悬浮记账球、多选统计和导出，以及记账页二级分类的列表内部展开和原版顺序。
4. 完成统计资产走势、年度热力与筛选，逐页完成搜索、分类、预算、周期、模板、愿望、规则和数据管理页面的布局与操作验证。
5. 采集非空模板抽屉、夜间、无键盘搜索和各新增、编辑状态；记录原版临时模板标识，采集后仅删除本次新增模板。
6. 核验逐页必要控件配置和manifest，完成正式截图差异报告。保持1.5%整体差异、6%区域差异及3px关键bounds阈值。
7. 在独立测试包完成仪器测试；重新运行完整单元测试、Lint和构建。
8. 单独构建用户包，核查包名、安装结果及用户数据，交付APK、差异报告和完整验证结果。

暂停状态下不继续执行上述操作。
