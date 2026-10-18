# 阶段交接文档（HANDOFF.md）

## 1. 状态

- **阶段编号**：S06（T06：完成资产与账户管理）
- **当前状态**：完成
- **阶段目标**：在统一数据模型（Room 版本 5）与分类管理闭环（T05）基础之上，完整构建专职资产数据仓库（`AssetRepository`）、记账与转账资金余额原子联动机制、资产管理视图模型（`AssetManageViewModel`）以及资产管理界面（`AssetManageScreen`）。实现真实资产账户的新增、编辑、隐藏、排序、余额调平及转账闭环；打通记账页面动态真实资产选择与管理账户跳转联动；计算总资产、总负债与净资产；全量通过 128 个 JVM 单元测试、Lint 静态分析（0 错误）、各阶段独立 SQLite 演练验证，并成功构建生成 `app-debug.apk`。

---

## 2. 变更文件清单

| 文件路径 | 改动说明 | 变更原因 |
|---|---|---|
| `docs/plans/t06_asset_and_account_management_plan.md` | 编写 T06 实施计划方案 | 规范资产管理界面、余额原子联动与资金核算方案 |
| `app/src/main/java/com/nonen/Bookkeeping/data/db/FinancialDaos.kt` | 扩充 `AssetDao` 与 `TransferDao` 查询、排序、统计与删除接口 | 支撑资产账户管理、转账操作与引用统计 |
| `app/src/main/java/com/nonen/Bookkeeping/data/db/Daos.kt` | `TransactionDao` 增加 `reassignAsset` 方法 | 支撑自定义资产删除时关联账单安全迁移至现金账户 |
| `app/src/main/java/com/nonen/Bookkeeping/data/repo/AssetRepository.kt` | 编写专职资产数据仓库 | 封装资产增删改查、排序、隐藏、调平与净资产核算业务 |
| `app/src/main/java/com/nonen/Bookkeeping/data/repo/TransactionRepository.kt` | 接入 `AssetDao` 与 `TransferDao`，实现余额联动与转账 | 保证记账与转账操作下账户可用余额原子变动与资金守恒 |
| `app/src/main/java/com/nonen/Bookkeeping/BookkeepingApp.kt` | 实例化 `AssetRepository` 并完成依赖注入 | 提供全局统一资产仓库访问入口 |
| `app/src/main/java/com/nonen/Bookkeeping/ui/screens/AssetManageViewModel.kt` | 编写资产管理视图模型 | 驱动资产列表、分组统计、调平、隐藏、调序与删除 |
| `app/src/main/java/com/nonen/Bookkeeping/ui/screens/AssetManageScreen.kt` | 编写资产管理界面 | 呈现 Material 3 净资产概览卡片、分组列表与调平/转账弹窗 |
| `app/src/main/java/com/nonen/Bookkeeping/ui/screens/AddEditViewModel.kt` | 注入 `AssetRepository`，动态加载可见资产并同步名称 | 驱动记账页面真实账户绑定与外键持久化 |
| `app/src/main/java/com/nonen/Bookkeeping/ui/screens/AddEditScreen.kt` | 升级账户选择为真实资产胶囊并添加管理账户跳转入口 | 实现从记账到资产管理的操作交互闭环 |
| `app/src/main/java/com/nonen/Bookkeeping/ui/screens/SettingsScreen.kt` | 添加资产与账户折叠分组与进入管理按钮 | 提供系统设置直达入口 |
| `app/src/main/java/com/nonen/Bookkeeping/ui/screens/MainScreen.kt` | 传递 `onAssetManage` 回调 | 支持从主界面设置页导航至资产管理 |
| `app/src/main/java/com/nonen/Bookkeeping/ui/AppNavHost.kt` | 注册 `Routes.ASSET_MANAGE` 路由并配置工厂 | 支撑资产管理页面的推入与弹出导航 |
| `app/src/test/java/com/nonen/Bookkeeping/data/repo/AssetRepositoryTest.kt` | 编写资产仓库单元测试 | 验证资产创建、自增 ID 分配、排序、调平与净资产核算 |
| `app/src/test/java/com/nonen/Bookkeeping/data/repo/FinancialSyncTest.kt` | 编写资金余额同步与转账单元测试 | 验证记账增删改下的账户余额原子对冲与转账原子性 |
| `app/src/test/java/com/nonen/Bookkeeping/ui/screens/CategoryManageTest.kt` | `FakeTransactionDao` 补充 `reassignAsset` 实现 | 保证既有分类管理单元测试完全通过 |

---

## 3. 关键决策与理由

1. **采用独立领域实体表架构（Dedicated Room Tables）【长期】**
   - **决策**：在 Room 中为 `Asset`、`Transfer`、`Refund`、`Reimbursement`、`Lend`、`Instalment` 建立独立的 Entity 表，通过 Room 事务统一保证资金联动与原子性。
   - **否决备选**：将所有资金流折叠进单一多态 `transactions` 宽表。
   - **理由**：原应用业务逻辑解耦充分，各业务实体拥有独立的自增 ID 体系与生命周期，独立表设计能清晰表达外键约束并防止字段污染。
2. **金额精度存储严格对标原应用使用 Double 类型【长期】**
   - **决策**：在 Room 实体中直接使用 `Double` 存储金额数值，配合统一的保留两位小数运算工具函数。
   - **否决备选**：升级为长整型“分”（`Long`）或 `BigDecimal` 存储。
   - **理由**：保持与原应用物理存储结构完全一致，降低数据转换与历史数据迁移复杂度。
3. **历史数据采用增量平滑迁移（In-place Smooth Migration）【长期】**
   - **决策**：编写严格的数据库升级逻辑，在新建独立表的同时，根据现存记录自动初始化默认资产并回填关联 `assetId`。
   - **否决备选**：将原有表设为只读历史表或清空重建。
   - **理由**：确保用户现存版本 3 的所有账单数据完整可用且无损过渡。
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
10. **数据库版本 3 至版本 4 增量平滑迁移与默认资产自愈回填【长期】**
    - **决策**：在 `MIGRATION_3_4` 中自动注入默认账本（`bookId = 1`）与默认现金资产（`assetId = 1`），历史所有未关联的 transactions 自动关联至默认资产与默认账本。
    - **理由**：消除外键断裂风险，保证历史数据可无缝过渡到多账本与资产管理体系。
11. **确定性映射与多义商品泛词保护原则【长期】**
    - **决策**：对于能唯一对应的一级/二级分类精确转换外键；对于多义泛词（如“购物”包含多种用途，“服饰美容”包含服饰与美妆），严禁随意分配给某一具体商品子类，而是保留二级分类为 null 或作为自定义分类完整保留。
    - **否决备选**：通过模糊猜测将“购物”随机分配给“日常家居”。
    - **理由**：严格遵守《执行计划》第 9 节规定，防止篡改用户真实记账原意。
12. **预置自定义分类独立编号与原意保全机制【长期】**
    - **决策**：为无法在一木 76 项默认分类中唯一定位的 17 项历史分类分配互不冲突的确定性业务 ID，以 `isCustom = true` 自动播种入库。
    - **否决备选**：直接将无法映射的历史分类强制归入“其他”。
    - **理由**：确保分类树中完整可查且与历史记录严格关联，消除外键断裂和分类信息丢失问题。
13. **分类全局隐藏与账本内可见性（hideBook）隔离设计【长期】**
    - **决策**：全局隐藏通过 `hide: Int`（0=显示，1=隐藏）控制，账本内可见性通过 `hideBook: String?` 逗号分隔账本 ID 控制，`MIGRATION_4_5` 完成增量字段扩展。
    - **理由**：完全对齐原应用多账本下分类可见性隔离的实现机制。
14. **自定义分类删除与既有账单记录安全解绑机制【长期】**
    - **决策**：删除自定义分类时若存在关联账单，自动执行 `clearTransactionParentCategory` 或 `clearTransactionChildCategory` 将对应账单外键置为 null，完整保留交易记录文本分类名称与历史金额。
    - **否决备选**：级联删除已有记账记录或阻止删除。
    - **理由**：保护用户历史资产与记账数据绝对安全，避免产生脏数据或历史报表断崖式失真。
15. **记账闭环双外键驱动与商户分类自主记忆联动【长期】**
    - **决策**：记账保存时将 `parentCategoryId` 与 `childCategoryId` 作为一等公民持久化至 `TransactionEntity`，并在保存成功后触发 `learnFromEdit` 自主更新商户分类记忆表。
    - **理由**：形成记账-分类持久化-自动识别记忆的完整闭环。
16. **记账收支与账户余额原子变动及双向对冲原则【长期】**
    - **决策**：在 `TransactionRepository` 中联动 `AssetDao.updateBalance`，新增支出扣减余额、新增收入增加余额；修改账单时先回滚旧账户旧金额再应用新账户新金额；删除账单时反向回滚对冲。
    - **理由**：确保应用在本地记账和修改删除全生命周期中资金守恒、数据原子一致。
17. **默认现金账户（assetId = 1）保护与自定义资产删除重定向机制【长期】**
    - **决策**：系统固定保留默认现金账户禁止删除；用户删除自定义资产账户时，自动将关联历史账单的 `assetId` 重定向至 1，并将 `account` 更新为现金账户名称。
    - **否决备选**：级联删除已有记账记录或直接阻止删除。
    - **理由**：防止外键孤儿与数据丢失，保证用户历史记账账目完整。
18. **资产负债统计口径与信用卡欠款计算模型【本阶段】**
    - **决策**：信用卡（`assetType == 2`）配置总额度（`totalQuota > 0`）时，欠款负债定义为 `totalQuota - assetNumber`；借入账户（`assetType == 7`）余额绝对值计入总负债；净资产核算公式为 `总资产 - 总负债`。
    - **理由**：完全对齐原应用对信用账户与借贷账户的统计定义。

---

## 4. 已知坑与失败尝试

- **现象**：在 `AppNavHost.kt` 的 `composable(Routes.MAIN)` 内部传递 `onEdit = { nav.navigate(Routes.add(it)) }` 时编译失败，提示 `Argument type mismatch: actual type is 'NavBackStackEntry', but 'Long' was expected`。
  - **原因**：外层 `composable` lambda 缺省隐式参数名为 `it`（类型为 `NavBackStackEntry`），内部未显式命名参数的子 lambda 将 `it` 遮蔽捕获为外层上下文对象。
  - **正确做法**：内部回调显式声明命名参数：`onEdit = { txId -> nav.navigate(Routes.add(txId)) }`，消除上下文作用域参数遮蔽歧义。
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
  - T06 独立验收：
    ```powershell
    node work/scripts/yimu-offline-rebuild/t06/verify_T06_deliverables.js
    ```
  - T05 独立验收：
    ```powershell
    node work/scripts/yimu-offline-rebuild/t05/verify_T05_deliverables.js
    ```
  - T04 独立验收：
    ```powershell
    node work/scripts/yimu-offline-rebuild/t04/verify_T04_deliverables.js
    ```
  - T03 独立验收：
    ```powershell
    node work/scripts/yimu-offline-rebuild/t03/verify_T03_deliverables.js
    ```
  - T02 独立验收：
    ```powershell
    node work/scripts/yimu-offline-rebuild/t02/verify_T02_deliverables.js
    ```
  - T01-B 独立验收：
    ```powershell
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
- [x] **时间自然语言解析模式**：解压提取 7,327 字符高精度时间匹配正则，6 组典型句子测试 100% 通过。
- [x] **金额提取与文本清洗规则**：毛角转点与数字提取逻辑，6 组典型记账句子测试 100% 通过。
- [x] **餐饮时段状态机**：4 组边界时刻状态转换测试 100% 通过。
- [x] **统一 Room 数据库实体与增量迁移构建**：建立 9 大新增实体表，导出 Room `4.json` schema，实现 `MIGRATION_3_4`，官方 SQLite 演练迁移链与数据完整性验证通过。
- [x] **历史分类映射与分类管理迁移**：实现确定性映射矩阵与自定义分类保留，完成 `CategoryRepository` 与 `CategoryMigrationService`，单元测试通过，`verify_T04_deliverables.js` 真实 SQLite 演练通过。
- [x] **分类管理与记账闭环**：实现 `CategoryEntity` `hideBook` 账本级过滤与 `MIGRATION_4_5`，完成 `CategoryManageScreen`、`CategoryManageViewModel`、`AddEditScreen`、`AddEditViewModel` 双外键驱动与商户自主学习记忆闭环，编写 `Migration4To5Test` 与 `CategoryManageTest` 单元测试通过，导出 Room `5.json` schema，`verify_T05_deliverables.js` 真实 SQLite 演练通过。
- [x] **资产与账户管理**：构建 `AssetRepository`，实现记账与转账账户余额原子联动与双向对冲；实现 `AssetManageScreen` 与 `AssetManageViewModel`，涵盖资金/信用卡/借贷账户分组展示、净资产/总资产/总负债聚合计算、余额调平与安全删除重定向；打通记账页动态真实账户选择与管理跳转；全量 128 个单元测试全部通过，Lint 0 错误，`verify_T06_deliverables.js` 真实 SQLite 演练通过，`assembleDebug` 打包成功（`app-debug.apk` 约 15.7MB）。

### 6.2 未验证与待后续阶段执行
- [ ] **专用模拟器断网参考界面层级与操作核查（归属 T01-C）**：
  - 风险判定：中等风险。因当前未启动模拟器截屏比对，部分次级界面细节需在后续界面搭建阶段结合 aapt2 dump 进一步核对。
- [ ] **完成预算、周期、模板与储蓄目标（归属 T07）**：
  - 风险判定：低风险。后续聚焦预算周期控制、周期记账幂等执行器、账单模板与储蓄目标业务。

---

## 7. 下一阶段交接

- **第一步**：新会话读取本交接文档与总执行计划 [yimu_offline_rebuild_antigravity.md](file:///D:/github-repository/smart-record/docs/plans/yimu_offline_rebuild_antigravity.md)，基于已构建的资产与账户管理体系，开启 **T07（完成预算、周期、模板与储蓄目标）**。
- **验收标准**：
  - 运行 `node work/scripts/yimu-offline-rebuild/t06/verify_T06_deliverables.js` 退出状态为 0；
  - 运行 `node work/scripts/yimu-offline-rebuild/t05/verify_T05_deliverables.js` 退出状态为 0；
  - 运行 `node work/scripts/yimu-offline-rebuild/t04/verify_T04_deliverables.js` 退出状态为 0；
  - 运行 `node work/scripts/yimu-offline-rebuild/t03/verify_T03_deliverables.js` 退出状态为 0；
  - 运行 `node work/scripts/yimu-offline-rebuild/t02/verify_T02_deliverables.js` 退出状态为 0；
  - 运行 `node work/scripts/yimu-offline-rebuild/financial/verify_T01_B_deliverables.js` 退出状态为 0；
  - 运行 `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug` 全部通过（当前基准 128 个测试用例，0 错误）；
  - 运行 `.\gradlew.bat :app:assembleDebug` 编译打包成功；
  - 完成预算周期设定、周期账单自动幂等触发、账单模板与储蓄目标业务。
- **禁止事项**：
  - 严禁擅自使用 Git 进行回滚或重置操作；
  - 严禁直接抹除现有数据库表或破坏版本 5 现有数据结构；
  - 严禁违背已共识的 18 项长期与阶段关键决策。
