# 阶段交接文档（HANDOFF.md）

## 1. 状态

- **阶段编号**：S07（T07：完成预算、周期、模板与储蓄目标）
- **当前状态**：完成
- **阶段目标**：在统一数据模型（Room 版本 5）与资产账户管理（T06）基础之上，实现 Room 数据库平滑升级至版本 6（`MIGRATION_5_6` 与 `6.json` schema 导出）；构建月度与分类预算核算体系（`BudgetRepository`）、支持日历规则的周期任务幂等执行引擎（`PeriodicRepository`、`PeriodicTransactionExecutor`）、常用账单模板快速复用（`TemplateRepository`）以及愿望储蓄清单（`SavingsGoalRepository`）；实现 `BudgetScreen`、`PeriodicScreen`、`TemplateScreen`、`SavingsGoalScreen` 4 大界面，打通 `AddEditScreen` 记账联动与 `SettingsScreen` 导航入口；全量通过 138 个 JVM 单元测试、Lint 静态分析（0 错误 0 告警）、全量回归脚本演练验证，并成功构建生成 `app-debug.apk`。

---

## 2. 变更文件清单

| 文件路径 | 改动说明 | 变更原因 |
|---|---|---|
| `docs/plans/t07_budget_periodic_template_savings_plan.md` | 编写 T07 实施计划方案 | 规范预算计算、周期任务调度、模板管理与储蓄目标业务方案 |
| `app/schemas/com.nonen.Bookkeeping.data.db.AppDatabase/6.json` | 导出 Room 版本 6 结构元数据 | 包含 7 张新数据表结构与复合索引定义 |
| `app/src/main/java/com/nonen/Bookkeeping/data/entity/BudgetEntities.kt` | 定义预算领域实体 | 映射 `budgets`、`category_budgets`、`budget_settings` 表 |
| `app/src/main/java/com/nonen/Bookkeeping/data/entity/PeriodicEntities.kt` | 定义周期任务领域实体 | 映射 `cycles`、`cycle_dates`、`cycle_logs` 表 |
| `app/src/main/java/com/nonen/Bookkeeping/data/entity/ModuleBillEntity.kt` | 定义常用账单模板实体 | 映射 `module_bills` 表 |
| `app/src/main/java/com/nonen/Bookkeeping/data/entity/DreamEntities.kt` | 定义储蓄目标与变动日志实体 | 映射 `dreams`、`dream_logs` 表 |
| `app/src/main/java/com/nonen/Bookkeeping/data/db/PlanDaos.kt` | 定义 4 大计划数据访问接口 | 涵盖 `BudgetDao`、`CycleDao`、`ModuleBillDao`、`DreamDao` |
| `app/src/main/java/com/nonen/Bookkeeping/data/db/CategoryDao.kt` | 补充 `findParentCategory` 接口 | 支撑分类预算按一级分类 ID 获取分类实体 |
| `app/src/main/java/com/nonen/Bookkeeping/data/db/Daos.kt` | `TransactionDao` 扩充支出聚合查询方法 | 支撑分类与月度预算支出实时统计计算 |
| `app/src/main/java/com/nonen/Bookkeeping/data/db/AppDatabase.kt` | 升级至 version 6 并实现 `MIGRATION_5_6` | 完成平滑升级迁移与新实体注册 |
| `app/src/main/java/com/nonen/Bookkeeping/data/repo/BudgetRepository.kt` | 编写预算业务仓库 | 聚合计算总预算、分类预算、实时支出与超支比例 |
| `app/src/main/java/com/nonen/Bookkeeping/data/repo/PeriodicRepository.kt` | 编写周期任务配置仓库 | 维护周期账单与周期转账规则增删改查 |
| `app/src/main/java/com/nonen/Bookkeeping/data/service/PeriodicTransactionExecutor.kt` | 编写周期任务调度执行引擎 | 实现按日/周/月/月末自适应日期推算、双重幂等防护与账户余额原子变动 |
| `app/src/main/java/com/nonen/Bookkeeping/data/repo/TemplateRepository.kt` | 编写常用模板仓库 | 支撑记账模板增删与一键记账入库 |
| `app/src/main/java/com/nonen/Bookkeeping/data/repo/SavingsGoalRepository.kt` | 编写储蓄目标愿望清单仓库 | 维护愿望清单创建、存入取出现金流变动与进度统计 |
| `app/src/main/java/com/nonen/Bookkeeping/BookkeepingApp.kt` | 注册 4 大仓库与 1 个执行器并配置启动自动扫描 | 提供全局统一访问入口并在应用冷启动时自动补流入账 |
| `app/src/main/java/com/nonen/Bookkeeping/ui/screens/BudgetViewModel.kt` 与 `BudgetScreen.kt` | 编写预算管理界面与 ViewModel | 提供月度总预算、分类明细列表与月度切换视图 |
| `app/src/main/java/com/nonen/Bookkeeping/ui/screens/PeriodicViewModel.kt` 与 `PeriodicScreen.kt` | 编写周期任务管理界面与 ViewModel | 提供周期规则列表、新增对话框与执行历史记录抽屉 |
| `app/src/main/java/com/nonen/Bookkeeping/ui/screens/TemplateViewModel.kt` 与 `TemplateScreen.kt` | 编写常用模板界面与 ViewModel | 展示模板卡片列表并提供一键入账按钮 |
| `app/src/main/java/com/nonen/Bookkeeping/ui/screens/SavingsGoalViewModel.kt` 与 `SavingsGoalScreen.kt` | 编写储蓄目标界面与 ViewModel | 展示愿望目标进度条、存入弹窗与完成状态切换 |
| `app/src/main/java/com/nonen/Bookkeeping/ui/screens/AddEditViewModel.kt` | 增加从模板读取与存为模板能力 | 支撑记账页面与模板双向闭环 |
| `app/src/main/java/com/nonen/Bookkeeping/ui/screens/SettingsScreen.kt` | 添加预算、周期、模板与储蓄目标入口 | 提供系统设置页直接导航入口 |
| `app/src/main/java/com/nonen/Bookkeeping/ui/screens/MainScreen.kt` | 补充设置菜单跳转回调 | 支持向主导航分发路由指令 |
| `app/src/main/java/com/nonen/Bookkeeping/ui/AppNavHost.kt` | 注册 4 条新增路由与工厂绑定 | 支撑新增二级页面的推入与弹出导航 |
| `app/src/test/java/com/nonen/Bookkeeping/data/db/Migration5To6Test.kt` | 编写 5 到 6 数据库迁移单元测试 | 验证数据无损迁移与新表结构正确建立 |
| `app/src/test/java/com/nonen/Bookkeeping/data/repo/BudgetRepositoryTest.kt` | 编写预算仓库单元测试 | 验证总预算、分类预算、实时支出与超支比例核算 |
| `app/src/test/java/com/nonen/Bookkeeping/data/service/PeriodicExecutorTest.kt` | 编写周期任务执行引擎单元测试 | 验证周期匹配、跨日补记、幂等去重防重复与限次自动终止 |
| `app/src/test/java/com/nonen/Bookkeeping/data/repo/TemplateRepositoryTest.kt` | 编写模板仓库单元测试 | 验证模板保存、列表查询与一键生成账单 |
| `app/src/test/java/com/nonen/Bookkeeping/data/repo/SavingsGoalRepositoryTest.kt` | 编写储蓄目标仓库单元测试 | 验证储蓄目标增改、存入提取与达成率计算 |
| `app/src/test/java/com/nonen/Bookkeeping/data/repo/AssetRepositoryTest.kt` | 适配 `FakeTransactionDao` 支出查询方法 | 保证既有资产测试在接口扩充后完全通过 |
| `app/src/test/java/com/nonen/Bookkeeping/data/repo/FinancialSyncTest.kt` | 适配 `FakeTransactionDao` 支出查询方法 | 保证既有资金流转测试在接口扩充后完全通过 |
| `app/src/test/java/com/nonen/Bookkeeping/ui/screens/CategoryManageTest.kt` | 补充 `findParentCategory` 与支出查询模拟实现 | 保证既有分类测试在接口扩充后完全通过 |
| `work/scripts/yimu-offline-rebuild/t07/verify_T07_deliverables.js` | 编写 T07 独立验收演练脚本 | 校验真实 SQLite 下预算计算、周期幂等入账、模板入账与储蓄进度聚合 |
| `work/tasks/yimu-offline-T07-state.md` | 记录 T07 阶段性任务状态 | 记录任务目标、验证命令与下一步规划 |
| `work/tasks/yimu-offline-rebuild.md` | 更新总任务阶段进展 | 标记 T07 完成并指向 T08 |

---

## 3. 关键决策与理由

1. **采用独立领域实体表架构（Dedicated Room Tables）【长期】**
   - **决策**：在 Room 中为 `Budget`、`CategoryBudget`、`BudgetSetting`、`Cycle`、`CycleDate`、`CycleLog`、`ModuleBill`、`Dream`、`DreamLog` 建立独立 Entity 表，通过 Room 事务统一保证资金联动与原子性。
   - **否决备选**：将所有计划与模板折叠进配置表或以 JSON 字符串存储在既有表中。
   - **理由**：各业务实体拥有独立的自增 ID 体系与生命周期，独立表设计能清晰表达外键约束并防止字段污染。
2. **金额精度存储严格对标原应用使用 Double 类型【长期】**
   - **决策**：在 Room 实体中直接使用 `Double` 存储金额数值，配合统一的保留两位小数运算工具函数。
   - **否决备选**：升级为长整型“分”（`Long`）或 `BigDecimal` 存储。
   - **理由**：保持与原应用物理存储结构完全一致，降低数据转换与历史数据迁移复杂度。
3. **历史数据采用增量平滑迁移（In-place Smooth Migration）【长期】**
   - **决策**：编写严格的数据库升级逻辑，在新建独立表的同时，根据现存记录自动初始化默认资产并回填关联 `assetId`。
   - **否决备选**：将原有表设为只读历史表或清空重建。
   - **理由**：确保用户现存版本的所有账单数据完整可用且无损过渡。
4. **业务状态机规范建立 5 维标准矩阵【长期】**
   - **决策**：对资金业务逐一梳理输入校验、持久化对象与主外键、资产余额计算表达式、修改删除对冲级联规则及统计计入口径。
   - **理由**：杜绝常识推测，提供精确业务实现依据。
5. **排除房贷车贷（Loan）业务【长期】**
   - **决策**：将房贷、车贷等复杂中长期借贷业务移出重建范围。
   - **理由**：遵从用户明确指示，聚焦核心记账与资产流转。
6. **沿用原应用 LibLinear/HanLP 与 Time-NLP 正则引擎【长期】**
   - **决策**：直接采用原 APK 内置提取的 `classification-model.ser` 与 `TimeExp.m` 离线资产构建本地推断能力。
   - **否决备选**：引入云端 NLP 接口或重新训练现代深度学习分类模型。
   - **理由**：保证应用绝对离线运行，免除网络延迟与令牌开销，且分类预测与原应用逻辑达到 1:1 精确对齐。
7. **分类置信度阈值与餐饮时段动态判定规则【长期】**
   - **决策**：分类预测置信度严格执行原应用 `0.015` 门槛值，低于该值统一归入 `其他-其他`；对于命中 `食品餐饮-其他` 的单据，依据 24 小时制进行时段修正：4~10 归为早餐、10~16 归为午餐、其余归为晚餐。
   - **理由**：完全对齐原应用核心逻辑的实际行为。
8. **金额预处理毛角转点与动词剥离规则【长期】**
   - **决策**：前置执行正则替换 `(块|元|点)([1-9一二三四五六七八九])[毛角]` 为 `点$2`，剥离“花了”、“用了”等动词，并将首尾单字单位消除后再行提取数值。
   - **理由**：保证中文数字与阿拉伯数字混合场景均可精确抽取金额并还原干净备注。
9. **统一分类实体唯一约束与收支方向定义【长期】**
   - **决策**：在 `CategoryEntity` 中建立 `(categoryId, parentId, categoryType)` 复合唯一索引，将原应用一级分类 ID 9 及其下属 11 项子分类明确定义为收入（`categoryType = 1`），其余 64 项分类明确定义为支出（`categoryType = 0`）。
   - **理由**：消除原应用中收入依靠魔法数值 9 区分的隐患，提供清晰的收支目录筛选能力。
10. **数据库版本增量平滑迁移与默认资产自愈回填【长期】**
    - **决策**：在迁移中自动注入默认账本（`bookId = 1`）与默认现金资产（`assetId = 1`），历史所有未关联的 transactions 自动关联至默认资产与默认账本。
    - **理由**：消除外键断裂风险，保证历史数据可无缝过渡到多账本与资产管理体系。
11. **确定性映射与多义商品泛词保护原则【长期】**
    - **决策**：对于能唯一对应的一级/二级分类精确转换外键；对于多义泛词，保留二级分类为 null 或作为自定义分类完整保留。
    - **理由**：严格遵守规定，防止篡改用户真实记账原意。
12. **预置自定义分类独立编号与原意保全机制【长期】**
    - **决策**：为无法唯一定位的历史分类分配互不冲突的确定性业务 ID，以 `isCustom = true` 自动播种入库。
    - **理由**：确保分类树中完整可查且与历史记录严格关联，消除外键断裂和分类信息丢失问题。
13. **分类全局隐藏与账本内可见性（hideBook）隔离设计【长期】**
    - **决策**：全局隐藏通过 `hide: Int`（0=显示，1=隐藏）控制，账本内可见性通过 `hideBook: String?` 逗号分隔账本 ID 控制。
    - **理由**：完全对齐原应用多账本下分类可见性隔离的实现机制。
14. **自定义分类删除与既有账单记录安全解绑机制【长期】**
    - **决策**：删除自定义分类时若存在关联账单，自动将对应账单外键置为 null，完整保留交易记录文本分类名称与历史金额。
    - **理由**：保护用户历史资产与记账数据绝对安全，避免产生脏数据或历史报表断崖式失真。
15. **记账闭环双外键驱动与商户分类自主记忆联动【长期】**
    - **决策**：记账保存时将 `parentCategoryId` 与 `childCategoryId` 持久化至 `TransactionEntity`，并在保存成功后自主更新商户分类记忆表。
    - **理由**：形成记账-分类持久化-自动识别记忆的完整闭环。
16. **记账收支与账户余额原子变动及双向对冲原则【长期】**
    - **决策**：在 `TransactionRepository` 中联动 `AssetDao.updateBalance`，新增支出扣减余额、新增收入增加余额；修改账单时先回滚旧账户旧金额再应用新账户新金额；删除账单时反向回滚对冲。
    - **理由**：确保应用在本地记账和修改删除全生命周期中资金守恒、数据原子一致。
17. **默认现金账户（assetId = 1）保护与自定义资产删除重定向机制【长期】**
    - **决策**：系统固定保留默认现金账户禁止删除；用户删除自定义资产账户时，自动将关联历史账单的 `assetId` 重定向至 1，并将 `account` 更新为现金账户名称。
    - **理由**：防止外键孤儿与数据丢失，保证用户历史记账账目完整。
18. **资产负债统计口径与信用卡欠款计算模型【长期】**
    - **决策**：信用卡配置总额度时，欠款负债定义为 `totalQuota - assetNumber`；借入账户余额绝对值计入总负债；净资产核算公式为 `总资产 - 总负债`。
    - **理由**：完全对齐原应用对信用账户与借贷账户的统计定义。
19. **周期记账双重幂等与动态状态刷新机制【本阶段】**
    - **决策**：周期记账推算生成确定性防重键 `cycle_{cycleId}_{yyyyMMdd}`，在同一事务中完成记账、扣减账户余额与写入 `cycle_logs`；且在单次扫描循环中每次执行后重新获取最新的 `CycleEntity`，达到限制次数（`cycleTimes`）时标记 `end = true` 并终止调度。
    - **否决备选**：仅依靠定时任务启动时间判断，不记录历史执行日志；或仅在应用退出时统一保存状态。
    - **理由**：防止跨日补记、多次触发或冷启动时产生重复扣款与脏流水。
20. **常用模板与愿望储蓄目标解耦独立存储【本阶段】**
    - **决策**：模板存储于 `module_bills`，储蓄目标存储于 `dreams` 与 `dream_logs`，不直接入侵核心记账主表结构。
    - **否决备选**：在 `transactions` 表中增加模板标记字段或储蓄标记字段。
    - **理由**：保证业务实体职责单一清晰，记账主表保持高效。

---

## 4. 已知坑与失败尝试

- **现象**：`PeriodicTransactionExecutor` 构造函数接收抽象类 `AppDatabase`，在纯 JVM 单元测试中使用 `Proxy.newProxyInstance` 动态代理抛出 `IllegalArgumentException: ... is not an interface`。
  - **原因**：Java 原生反射动态代理仅支持 Interface，无法直接代理 Abstract Class。
  - **正确做法**：`PeriodicTransactionExecutor` 构造器增加可选参数 `transactionRunner: (suspend (block: suspend () -> Boolean) -> Boolean)? = null`，生产环境默认调用 `database.withTransaction`，测试环境直接传入 `{ it() }`，解耦事务框架依赖。
- **现象**：周期任务限制执行 2 次，但在经过 5 天后执行测试时断言 `updatedCycle?.end == true` 失败。
  - **原因**：在外层循环遍历待执行日期时，传入的始终为同一个初始 `cycle` 实体内存对象，导致每次执行时计算的 `addTimes` 始终基于 0 累加，无法正确感知已执行次数与结束状态。
  - **正确做法**：在执行循环中维护当前 `currentCycle` 状态指针，每次执行入账成功后通过 `cycleDao.getCycleById(currentCycle.cycleId)` 重新拉取最新实体，并在进入单次执行前增加 `if (currentCycle.end) break` 守卫判定。
- **现象**：Compose Material 3 基础图标库中 `Icons.Default.ChevronLeft` 和 `ChevronRight` 不存在导致编译失败。
  - **原因**：Compose 核心图标库中未内置 Chevron 系列，左右翻页导航标准图标位于 AutoMirrored 分组。
  - **正确做法**：采用 `Icons.AutoMirrored.Filled.KeyboardArrowLeft` 与 `Icons.AutoMirrored.Filled.KeyboardArrowRight`，完全兼容镜像支持。
- **现象**：在单元测试中实例化包含 `viewModelScope.launch` 的 Android ViewModel 时，纯 JVM 环境下抛出 `IllegalStateException: Module with the Main dispatcher had failed to initialize`。
  - **原因**：纯 JVM 测试环境无 Android Looper，`viewModelScope` 缺省绑定 `Dispatchers.Main.immediate`。
  - **正确做法**：ViewModel 构造器提供可选注入 `externalScope: CoroutineScope? = null`，内部通过 `private val scope: CoroutineScope get() = externalScope ?: viewModelScope` 调度；测试中使用 `CoroutineScope(Dispatchers.Unconfined)`，解耦调度器。
- **现象**：`TodayWatcherTest` 在多核并发调度下偶发断言失败（预期注销 1 次但实际为 0 次）。
  - **原因**：测试用例先调用 `watcher.register(scope)`（内部走 `Dispatchers.Default` 异步注册），紧接着立即调用 `scope.cancel()`；若调度线程尚未执行到 `registrations()`，协程未启动便被取消，导致未能注销。
  - **正确做法**：在取消作用域之前，先循环等待注册回调执行完成，再执行 `scope.cancel()`，消除调度时序竞争。

---

## 5. 环境与命令

- **系统平台**：Windows 11，PowerShell 7.6.7，Node.js 24.16.0，JDK 21，Android SDK 37（路径 `D:\Android\Sdk`）。
- **成果核验命令**：
  - T07 独立验收：
    ```powershell
    node work/scripts/yimu-offline-rebuild/t07/verify_T07_deliverables.js
    ```
  - 前序独立验收（T01-B 至 T06）：
    ```powershell
    node work/scripts/yimu-offline-rebuild/t06/verify_T06_deliverables.js
    node work/scripts/yimu-offline-rebuild/t05/verify_T05_deliverables.js
    node work/scripts/yimu-offline-rebuild/t04/verify_T04_deliverables.js
    node work/scripts/yimu-offline-rebuild/t03/verify_T03_deliverables.js
    node work/scripts/yimu-offline-rebuild/t02/verify_T02_deliverables.js
    node work/scripts/yimu-offline-rebuild/financial/verify_T01_B_deliverables.js
    ```
  - 全量单元测试与 Lint 静态分析：
    ```powershell
    .\gradlew.bat :app:testDebugUnitTest :app:lintDebug
    ```
  - 打包生成 APK：
    ```powershell
    .\gradlew.bat :app:assembleDebug
    ```

---

## 6. 验证状态

### 6.1 已完成验证
- [x] **APK 真实身份与资产提取**：SHA-256 校验通过，提取出 `classification-model.ser` 与 `TimeExp.m`。
- [x] **证据链物理核验**：12 个关键反编译类与方法 JSON 证据文件均物理存在且包含唯一 `evidence_id`。
- [x] **分类模型与类定义解构**：导出 70 个二级分类标准标签。
- [x] **时间自然语言解析模式**：解压提取 7,327 字符高精度时间匹配正则，测试 100% 通过。
- [x] **金额提取与文本清洗规则**：毛角转点与数字提取逻辑，测试 100% 通过。
- [x] **餐饮时段状态机**：边界时刻状态转换测试 100% 通过。
- [x] **统一 Room 数据库实体与增量迁移构建**：建立 9 大新增实体表，导出 Room `4.json` schema，实现 `MIGRATION_3_4`，官方 SQLite 演练迁移链与数据完整性验证通过。
- [x] **历史分类映射与分类管理迁移**：实现确定性映射矩阵与自定义分类保留，完成 `CategoryRepository` 与 `CategoryMigrationService`，单元测试通过，`verify_T04_deliverables.js` 真实 SQLite 演练通过。
- [x] **分类管理与记账闭环**：实现 `CategoryEntity` `hideBook` 账本级过滤与 `MIGRATION_4_5`，完成 `CategoryManageScreen`、`CategoryManageViewModel`、`AddEditScreen`、`AddEditViewModel` 双外键驱动与商户自主学习记忆闭环，编写 `Migration4To5Test` 与 `CategoryManageTest` 单元测试通过，导出 Room `5.json` schema，`verify_T05_deliverables.js` 真实 SQLite 演练通过。
- [x] **资产与账户管理**：构建 `AssetRepository`，实现记账与转账账户余额原子联动与双向对冲；实现 `AssetManageScreen` 与 `AssetManageViewModel`，涵盖资金/信用卡/借贷账户分组展示、净资产/总资产/总负债聚合计算、余额调平与安全删除重定向；打通记账页动态真实账户选择与管理跳转；全量测试与演练通过。
- [x] **预算、周期、模板与储蓄目标**：实现 Room 版本 6 平滑升级（`MIGRATION_5_6` 与 `6.json`）；实现月度预算与分类子预算核算（`BudgetRepository`）、周期任务幂等执行引擎（`PeriodicTransactionExecutor`）、账单模板快速复用（`TemplateRepository`）与愿望清单（`SavingsGoalRepository`）；实现配套 4 大界面与 ViewModel；全量 138 个单元测试全部通过，Lint 0 错误，T01-B 至 T07 独立演练全部通过，`assembleDebug` 成功打包生成 `app-debug.apk`（约 15.8MB）。

### 6.2 未验证与待后续阶段执行
- [ ] **专用模拟器断网参考界面层级与操作核查（归属 T01-C）**：
  - 风险判定：中等风险。因当前未启动模拟器截屏比对，部分次级界面细节需在后续界面搭建阶段结合 aapt2 dump 进一步核对。
- [ ] **完成报表与统计核算（归属 T08）**：
  - 风险判定：低风险。后续聚焦日历图表、月/年聚合报表、资产趋势图与收支排行统计。

---

## 7. 下一阶段交接

- **第一步**：新会话读取本交接文档与总执行计划 [yimu_offline_rebuild_antigravity.md](file:///D:/github-repository/smart-record/docs/plans/yimu_offline_rebuild_antigravity.md)，基于已构建的预算、周期与模板体系，开启 **T08（完成报表与统计核算）**。
- **验收标准**：
  - 运行 `node work/scripts/yimu-offline-rebuild/t07/verify_T07_deliverables.js` 退出状态为 0；
  - 运行 `node work/scripts/yimu-offline-rebuild/t06/verify_T06_deliverables.js` 退出状态为 0；
  - 运行 `node work/scripts/yimu-offline-rebuild/t05/verify_T05_deliverables.js` 退出状态为 0；
  - 运行 `node work/scripts/yimu-offline-rebuild/t04/verify_T04_deliverables.js` 退出状态为 0；
  - 运行 `node work/scripts/yimu-offline-rebuild/t03/verify_T03_deliverables.js` 退出状态为 0；
  - 运行 `node work/scripts/yimu-offline-rebuild/t02/verify_T02_deliverables.js` 退出状态为 0；
  - 运行 `node work/scripts/yimu-offline-rebuild/financial/verify_T01_B_deliverables.js` 退出状态为 0；
  - 运行 `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug` 全部通过（当前基准 138 个测试用例，0 错误）；
  - 运行 `.\gradlew.bat :app:assembleDebug` 编译打包成功；
  - 完成统计报表（收支日历、月/年度总览、分类排行与资产趋势曲线）。
- **禁止事项**：
  - 严禁擅自使用 Git 进行回滚或重置操作；
  - 严禁直接抹除现有数据库表或破坏版本 6 现有数据结构；
  - 严禁违背已共识的 20 项长期与阶段关键决策。
