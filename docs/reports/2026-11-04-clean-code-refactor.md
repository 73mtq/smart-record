# clean-code 重构与验证记录

日期：2026-11-04。

## 源码组织

项目继续使用单个 `app` 模块。应用源码目录包含 146 个 Kotlin 文件和 7 个 Java 文件；检查结合调用关系、构建、Android Lint、JVM 测试和小米 15 的实际运行结果进行。

| 范围 | 当前结构与效果 | 维护成本 |
| --- | --- | --- |
| 应用初始化 | `BookkeepingApp` 负责 Application 初始化，`AppContainer` 单独管理数据库、DAO、Repository 和启动任务；内置规则初始化使用具名函数 | 增加一个源码文件，保留现有 package 与 class 名称 |
| 分类 | `RuleEngine` 分别表达关键词匹配与本地模型匹配，保持既有优先顺序和本地模型判定阈值 `0.15` | 增加两个私有函数，关键词查询直接取得首个有效规则 |
| 资产 | `AssetSummary.kt` 统一资金、信用卡、债权及债务账户计算，账户汇总和历史走势共用；显示金额的舍入仍由 Repository 处理 | 增加一个源码文件和可指定历史余额的计算参数 |
| 资产持久化 | `AssetRepository` 明确要求数据库及相关 DAO；余额调整、账户删除和转账使用真实 Room 事务 | 调用方传入完整依赖 |
| 预算 | 月预算创建和预算查询共用月份区间计算，新建预算的字段集中构建 | 增加两个私有函数，保留月份结束时间的排除边界 |
| 完整备份 | 导出流程统一传递现有 `FullBackupData`，表记录数量由该模型计算；恢复阶段状态文件使用共同写入函数 | 数据模型增加派生属性，序列化内部入口接收一个参数 |
| 账单编辑 | 新增与编辑共用 `buildTransaction`，统一金额、分类、账户、备注及标签组装，编辑保留原记录身份与来源 | 增加一个私有函数 |
| 文件导出 | Excel 与 CSV 共用数据准备和输出流程；CSVPrinter 使用 `use` 关闭 | 增加内部格式 enum 和两个字符串资源 |
| 页面状态 | `HomeViewModel`、`StatsViewModel`、`RulesViewModel`、`StatisticsBillListViewModel` 分别位于独立文件，保留现有调用接口 | 增加四个源码文件，页面文件专注 Compose 内容 |
| 原生控件 | 设置文字引用同级开关容器；账户弹窗使用支持现有 minSdk 的凸路径 Outline API | 两个布局引用和一个 API 调用更新 |

## 数据与应用兼容

- Room 保持版本 6，既有 schema 文件保持原内容。
- 完整备份保持 `formatVersion=1`、ZIP entry 名称、JSON 字段名称及 `tagRefs` 表数量字段。
- 数据库名称、实体字段、hash 计算方式、应用 package 与离线处理范围保持原值。
- AndroidManifest 保持原内容。通过 `aapt dump permissions` 核查发布 APK，确认应用没有声明 `android.permission.INTERNET`。
- 设备测试的目标为 `com.nonen.Bookkeeping.uivalidation`。

## 测试结构与依赖

持久化测试使用真实 `AppDatabase` 和 Room DAO；迁移测试使用官方 `MigrationTestHelper` 校验既有 schema 及迁移后的真实数据。纯计算、分类与解析测试在 JVM 执行。

| 依赖 | 使用范围 | 作用 |
| --- | --- | --- |
| `androidx.room:room-testing:2.8.4` | androidTest | 与现有 Room 版本共同验证数据库迁移 |
| `kotlinx-serialization-json:1.8.1` | Debug | 满足 Room schema 测试需要，并与被测 Debug 应用共用运行库 |
| `spotbugs-annotations:4.9.3` | compileOnly | 提供 Commons CSV 所引用的注解定义，支持 R8 处理 |

## Lint 判定

- `app/lint.xml` 中的 `UseAppTint` 范围限定为原生布局目录。程序化核查确认该目录中的 164 个 `android:tint` 全部用于 framework ImageView，项目 minSdk 为 26。
- `YimuAddBillView` 的两个 `WrongViewCast` 标注对应跨布局复用的 resource ID；当前 `yimu_add_bill.xml` 中实际控件分别为 TextView 和 AppCompatEditText。
- Debug 与 Release Lint 各有 0 条 error、977 条 warning、7 条 hint，包含依赖版本和资源使用提示。

## 最终验证

| 检查 | 结果 | 完整日志 |
| --- | --- | --- |
| JVM 测试 | 149 项通过，失败、错误和跳过均为 0 | `work/logs/clean-code-project-refactor-20261104/resume-gradle.txt`，用例计数来自 `app/build/test-results/testDebugUnitTest/` |
| Debug、androidTest、Release 构建 | 通过，Release 启用 R8 压缩 | `work/logs/clean-code-project-refactor-20261104/resume-gradle.txt` |
| Debug、Release Lint | 通过，错误 0 | `work/logs/clean-code-project-refactor-20261104/resume-gradle.txt`，报告位于 `app/build/reports/` |
| Android 设备测试 | 小米 15 上同一次执行通过全部 45 项 | `work/logs/clean-code-project-refactor-20261104/resume-device-45.txt` |

完整构建命令如下。

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :app:assembleRelease :app:lintRelease -PyimuUiValidation=true --continue --console=plain
```

当前工作区重新执行该命令，退出状态 0，耗时 15 秒，Gradle 重新执行 2 项任务并复用 133 项已验证输出。首次完整执行耗时 12 分 12 秒，记录在 `work/logs/clean-code-project-refactor-20261104/complete-gradle.txt`。

设备为小米 15（`95f79212`），Android API 36。以 SHA-256 核查已安装验收 APK，使用 `adb install -r -t` 更新最终测试 APK，再核查已安装测试 APK 的 SHA-256。使用 `Run-DeviceTests.ps1` 调用 `am instrument`，退出状态 0，JUnit 报告 `OK (45 tests)`，耗时 16.081 秒。

设备用例覆盖真实 Room 迁移、资产、预算、报表、储蓄目标、模板、分类迁移、账单新增编辑、历史数据、备份恢复、日期、统计列表、离线模型、原生记账键盘、设置开关位置和五个主页面导航。完成后停止验收包，确认手机回到桌面。个人包数据库、WAL、SHM 文件的 SHA-256 及安装路径、版本、安装时间和更新时间均与本次测试前一致，相关记录为同一日志目录下的 `resume-personal-*.txt`。

## APK 产物

以下产物保存在 `work/artifacts/clean-code-refactor-20261104/`。校验值记录在同目录的 `SHA256.txt`。

| 文件 | 大小 | SHA-256 |
| --- | ---: | --- |
| `smart-record-validation.apk` | 29,797,376 字节 | `877e9f07cd386e1e64f1f1005f3ddf8c963bf0009b0a544a08f12eb80d3f97f2` |
| `smart-record-release-unsigned.apk` | 13,228,694 字节 | `770a3b87da1478eab5e1520f4d5a90baf81c515d8b145b9a42fc581702b7f583` |

设备验证使用验收 APK。Release APK 为未签名构建产物，验证范围为构建、Lint、包名及权限检查。
