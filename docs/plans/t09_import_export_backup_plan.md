# 执行单元 T09：完成导入、导出与完整备份实施计划

## 1. 当前进度

依据项目根目录 `HANDOFF.md` 与 Git 提交历史（Commit `6c8cbc1`）核验，当前工程状态如下：

- **已完成阶段**：S08（执行单元 T08：完成报表与统计核算）。
- **Git 提交证据**：最新提交 `6c8cbc1 feat: 完成报表与统计核算（T08）` 完整包含报表计算模型、日历统计、二级分类穿透、资产走势历史逆推、对应界面、单元测试与演练脚本。
- **当前工程基准**：
  - 数据库版本为版本 6，已包含完整的 21 个实体表与升级迁移链（`MIGRATION_1_2` 至 `MIGRATION_5_6`），`6.json` 架构文件完整持久化；
  - 全量 141 个 JVM 单元测试全部通过；
  - Android Lint 静态分析 0 错误 0 告警；
  - 前序各阶段独立自动化演练脚本（`verify_T01_B_deliverables.js` 至 `verify_T08_deliverables.js`）执行结果退出状态均为 0；
  - 工作区干净（`working tree clean`），无未跟踪或未提交文件。

---

## 2. 本阶段目标与验收标准

本阶段完全依据 `HANDOFF.md`「7. 下一阶段交接」与总执行规划 `docs/plans/yimu_offline_rebuild_antigravity.md` 第 14 节制定，执行单元为 **T09：完成导入、导出与完整备份**。

### 2.1 本阶段核心目标
1. **多格式账单导入引擎**：
   - 兼容解析一木记账 23 列标准 Excel（`.xls` 与 `.xlsx` 格式）；
   - 完整适配微信与支付宝官方个人对账账单（GBK/UTF-8 CSV 与 XLSX），准确抽取时间、金额、方向、交易对方与分类线索；
   - 建立确定性防重哈希计算体系，支持重复导入安全幂等忽略。
2. **多维账单导出服务**：
   - 完整输出符合一木记账 23 列规范的 Excel（`.xlsx`）文件；
   - 真实读取数据库中的分类层级（一级分类与二级分类）、资产账户与多账本信息，消除硬编码映射；
   - 支持通用 CSV 格式导出，适配更广泛的离线数据分析需求。
3. **全量数据本地完整备份与还原体系**：
   - 建立包含格式版本（Version 1.0）与 SHA-256 完整性校验机制的 ZIP 归档备份包；
   - 备份覆盖范围：全量流水（`transactions`）、账户（`assets`）、账本（`books`）、分类（`categories`）、转账（`transfers`）、退款（`refunds`）、报销（`reimbursements`）、借贷（`lends`）、分期（`instalments`）、标签（`tags` 与关联）、预算（`budgets` 等 3 表）、周期规则（`cycles` 等 3 表）、模板（`module_bills`）、储蓄愿望（`dreams` 等 2 表）、商户记忆（`merchant_categories`）、分类规则（`category_rules`）、用户偏好配置（DataStore）与账单本地附件文件；
   - 还原事务保障：在写入数据库前执行完整格式校验与关联检查；执行过程置于原子事务中，若发生校验失败或异常，就地中断并完整保留原有数据库与文件，杜绝损坏现有数据。

### 2.2 验收标准
1. 运行 `node work/scripts/yimu-offline-rebuild/t09/verify_T09_deliverables.js` 退出状态为 0；
2. 运行前序全量演练脚本（T01-B 至 T08）退出状态全部为 0；
3. 运行 `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug` 全部通过（测试用例数提升，Lint 0 错误）；
4. 运行 `.\gradlew.bat :app:assembleDebug` 成功构建生成 `app-debug.apk`；
5. 真实参考账单导入、重复导入去重、导出再导入各字段自洽，完整备份还原演练通过。

---

## 3. 疑点清单

1. **一木记账 23 列标准列头与特殊列映射规则**：
   - **事实情况**：一木记账 23 列规范为 `日期, 收支类型, 金额, 类别, 二级分类, 账户, 账本, 退款, 优惠, 备注, 标签, 报销账户, 报销金额, 报销明细, 多币种, 地址, 创建用户, 其他, 附件1, 附件2, 附件3, 附件4, 附件5`。其中“其他”列通常存放哈希校验码或交易对方扩展信息。
   - **拟定方案**：导出时，“其他”列格式化为 `{hash}|{merchant}`；导入时，优先解析“其他”列提取原始防重哈希与商户名称。若无“其他”列，则使用标准规则动态推导生成哈希。
2. **完整备份包的数据载体格式选择**：
   - **事实情况**：当前数据库采用 Room 框架，底层为 SQLite 文件。直接拷贝 SQLite 文件存在 WAL 模式未合并与跨版本驱动兼容性隐患；而结构化 JSON 能够完整表达各个表的字段语义与版本控制。
   - **拟定方案**：完整备份包采用标准的 ZIP 压缩归档格式，根目录包含 `manifest.json`（记录版本、生成时间、表记录计数与 SHA-256 校验和）、`data.json`（包含全部 21 张实体表的完整数据结构与偏好设置）以及 `attachments/`（持久化的附件二进制文件目录）。恢复时逐表在 Room 事务中完成回填。

---

## 4. 分步执行方案

### 步骤一：构建通用数据交换模型与格式转换工具
- 新建 `app/src/main/java/com/nonen/Bookkeeping/export/ExportModels.kt`：定义通用账单交换模型 `UnifiedBillRecord`、备份元数据 `BackupManifest` 与备份包数据载体 `FullBackupData`；
- 增强 `app/src/main/java/com/nonen/Bookkeeping/parse/CsvSupport.kt` 与 `XlsxSupport.kt`：增强对 `.xls`（BIFF8）与 `.xlsx` 格式的容错读取，保证空列、特殊符号金额与各类型时间戳安全解析。

### 步骤二：增强与重构 23 列 Excel 及官方账单导入服务
- 重构 `app/src/main/java/com/nonen/Bookkeeping/parse/BackupExcelParser.kt`：
  - 全面适配一木记账 23 列标准格式；
  - 联动 `CategoryRepository`、`AssetDao` 与 `BookDao`：导入时根据类别与二级分类名称自动查找并关联数据库中存在的真实分类 ID；未匹配到的自动作为自定义分类安全留存；根据账户名称与账本名称自动解析并关联对应的 `assetId` 与 `bookId`；
- 增强 `app/src/main/java/com/nonen/Bookkeeping/parse/BillImporter.kt`：
  - 支持导入进度回调与详细错误统计；
  - 结合商户记忆表与规则表进行快速分类判定；
  - 严格执行防重哈希判定，保证重复导入幂等。

### 步骤三：重构多维账单导出服务（关联真实账户与账本）
- 重构 `app/src/main/java/com/nonen/Bookkeeping/export/BackupExporter.kt`：
  - 注入或传入分类、账户与账本查询服务；
  - 遍历流水数据时，通过 `parentCategoryId` 与 `childCategoryId` 反查真实的分类名称，通过 `assetId` 与 `bookId` 反查真实账户与账本名称；
  - 输出严格符合一木 23 列规范的 `.xlsx` 二进制流；
  - 新增 `buildCsv(transactions: List<TransactionEntity>): ByteArray` 方法，支持将账单导出为通用逗号分隔值文件。

### 步骤四：实现带校验与事务保护的全量数据本地备份恢复服务
- 新建 `app/src/main/java/com/nonen/Bookkeeping/export/FullBackupService.kt`：
  - **导出流程**：在后台线程中读取全部 21 张实体表数据与当前设置，构建 `FullBackupData` 并序列化为 JSON；收集附件目录下的所有文件；计算全包哈希并写入 `manifest.json`；使用 `ZipOutputStream` 压缩为单个备份文件输出；
  - **恢复流程**：使用 `ZipInputStream` 读取备份包，提取 `manifest.json` 核对格式版本与数据哈希；读取 `data.json` 进行结构完整性验证；使用 Room 的 `database.withTransaction` 开启事务，清空对应业务表并按依赖顺序完整回填（基础配置 -> 账本与账户 -> 分类 -> 流水与关联）；若过程中发生任何异常直接抛出并触发事务回滚，确保数据库完好无损；校验并恢复附件文件。

### 步骤五：连接设置页交互界面与 ViewModel 异步调度
- 更新 `app/src/main/java/com/nonen/Bookkeeping/BookkeepingApp.kt`：在 `AppContainer` 中注入 `FullBackupService`；
- 更新 `app/src/main/java/com/nonen/Bookkeeping/ui/screens/SettingsViewModel.kt`：
  - 增加全量备份导出 `exportFullBackup(uri: Uri)` 与还原 `importFullBackup(uri: Uri)` 异步调用方法；
  - 增强 Excel 导出与导入的进度展示及结果弹窗反馈；
- 更新 `app/src/main/java/com/nonen/Bookkeeping/ui/screens/SettingsScreen.kt`：
  - 在备份分组中明确提供“一木 23 列 Excel 导出/导入”与“全量数据本地完整备份/恢复”双入口；
  - 增加还原前的安全警示确认对话框。

### 步骤六：编写单元测试、端到端演练脚本与全量构建验证
- 新建 `app/src/test/java/com/nonen/Bookkeeping/parse/YimuExcelParserTest.kt`：验证一木 23 列 Excel 解析、各字段映射与分类匹配；
- 新建 `app/src/test/java/com/nonen/Bookkeeping/export/FullBackupServiceTest.kt`：验证全量数据打包与还原事务性回滚；
- 编写端到端演练脚本 `work/scripts/yimu-offline-rebuild/t09/verify_T09_deliverables.js`：
  - 模拟官方真实 23 列 Excel 数据导入与重复去重检查；
  - 模拟全量数据导出、备份、清空与恢复完整性比对；
- 执行全量回归验证命令。
