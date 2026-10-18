# 阶段交接文档（HANDOFF.md）

## 1. 状态

- **阶段编号**：S03（T03：统一数据模型与数据库增量平滑迁移构建）
- **当前状态**：完成
- **阶段目标**：基于 T01 与 T02 解构成果，在 Room 中建立覆盖 9 大新增业务领域（Asset, Transfer, Refund, Reimbursement, Lend, Instalment, Category, Book, Tag）的独立实体表；扩充 `TransactionEntity` 关联外键并实现 `MIGRATION_3_4`；预置一木 76 项默认分类目录、默认账本与资产，保持历史流水完整性，通过全量单元测试与官方 SQLite 迁移链验证。

---

## 2. 变更文件清单

| 文件路径 | 改动说明 | 变更原因 |
|---|---|---|
| `docs/plans/t03_data_model_and_migration_plan.md` | 编写 T03 单元实施计划方案 | 规范统一数据模型与数据库增量平滑迁移步骤 |
| `app/schemas/com.nonen.Bookkeeping.data.db.AppDatabase/4.json` | 导出 Room 数据库版本 4 结构文件 | 固化包含 13 个实体的数据库架构定义 |
| `app/src/main/java/com/nonen/Bookkeeping/core/DefaultCategories.kt` | 导出一木 76 项默认分类定义 | 留存分类目录种子资产（10 一级 + 66 二级） |
| `app/src/main/java/com/nonen/Bookkeeping/data/db/CategoryEntity.kt` | 建立分类持久化实体与选择值对象 | 支持标准分类树结构与分类选择 |
| `app/src/main/java/com/nonen/Bookkeeping/data/db/CategoryDao.kt` | 建立分类数据访问接口 | 提供分类树查询、按收支与父级获取接口 |
| `app/src/main/java/com/nonen/Bookkeeping/data/db/FinancialEntities.kt` | 建立资产、转账、退款、报销、借贷、分期、账本与标签实体 | 承载原应用全部核心资金流转业务数据 |
| `app/src/main/java/com/nonen/Bookkeeping/data/db/FinancialDaos.kt` | 建立金融业务各领域数据访问接口 | 提供各实体增删改查与余额变动操作 |
| `app/src/main/java/com/nonen/Bookkeeping/data/db/Entities.kt` | 扩充 `TransactionEntity` 关联字段 | 增加 `assetId`、`bookId`、`parentCategoryId`、`childCategoryId` 并建索引 |
| `app/src/main/java/com/nonen/Bookkeeping/data/db/AppDatabase.kt` | 注册 9 个新实体并实现 `MIGRATION_3_4` | 实现从版本 3 到版本 4 平滑升级与默认数据播种 |
| `app/src/main/java/com/nonen/Bookkeeping/BookkeepingApp.kt` | 注册迁移与 onCreate 播种回调 | 暴露新增 DAO 并在创建时预置默认数据 |
| `app/src/test/java/com/nonen/Bookkeeping/data/db/Migration3To4Test.kt` | 编写分类与迁移单元测试 | 自动化检验分类数量、DDL 语句与数据回填 |
| `work/scripts/yimu-offline-rebuild/t03/verify_T03_deliverables.js` | 编写 T03 独立自动化综合验收脚本 | 通过真实 SQLite 验证 v1 至 v4 迁移链与数据完整性 |
| `work/logs/yimu-offline-rebuild/T03-20261018-01/verification_result.json` | 保存独立核验结果文件 | 留存自动化验收通过记录 |
| `work/tasks/yimu-offline-T03-state.md` | 记录 T03 任务进度、验证结果与下一项操作 | 维护单元独立生命周期状态 |
| `work/tasks/yimu-offline-rebuild.md` | 更新总任务状态并推进至 T04 | 维护全工程统一进度视图 |
| `HANDOFF.md` | 更新阶段交接文档 | 供新会话 AI 冷启动衔接 |

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
   - **理由**：完全对齐原应用 `v4.r` 核心代码的实际行为。
8. **金额预处理毛角转点与动词剥离规则【长期】**
   - **决策**：前置执行正则替换 `(块|元|点)([1-9一二三四五六七八九])[毛角]` 为 `点$2`，剥离“花了”、“用了”等动词，并将首尾单字单位消除后再行提取数值。
   - **理由**：保证中文数字与阿拉伯数字混合场景均可精确抽取金额并还原干净备注。
9. **统一分类实体唯一约束与收支方向定义【本阶段】**
   - **决策**：在 `CategoryEntity` 中建立 `(categoryId, parentId, categoryType)` 复合唯一索引，将原应用一级分类 ID 9 及其下属 11 项子分类明确定义为收入（`categoryType = 1`），其余 64 项分类明确定义为支出（`categoryType = 0`）。
   - **理由**：消除了原应用中收入仅靠魔法数值 9 区分的隐患，提供清晰的收支目录筛选能力。
10. **数据库版本 3 至版本 4 增量平滑迁移与默认资产自愈回填【本阶段】**
    - **决策**：在 `MIGRATION_3_4` 中自动注入默认账本（`bookId = 1`）与默认现金资产（`assetId = 1`），历史所有未关联的 transactions 自动关联至默认资产与默认账本。
    - **理由**：彻底消除外键断裂风险，保证历史数据可无缝过渡到多账本与资产管理体系。

---

## 4. 已知坑与失败尝试

- **现象**：`CategoryEntity` 在写入收入分类时，若直接沿用原 APK `categoryType = 0` 会导致收支目录无法通过标准字段过滤。
  - **原因**：原应用内部通过 `parentCategoryId == 9` 判定收入，表内 `categoryType` 均为 0。
  - **正确做法**：在导入种子和持久化时，将一级 ID 9 及其子分类的 `categoryType` 明确置为 1，支出置为 0。
- **现象**：全新安装时若仅依靠 Migration 注入分类，新用户数据库不会触发 Migration。
  - **原因**：Room 对新创建数据库直接执行当前版本的完整 DDL，跳过历史 Migration。
  - **正确做法**：在 `BookkeepingApp.kt` 的 `Room.databaseBuilder` 中同时添加 `RoomDatabase.Callback().onCreate`，实现全新安装与版本升级双重播种保证。

---

## 5. 环境与命令

- **系统平台**：Windows 11，PowerShell 7.6.7，Node.js 24.16.0，JDK 21，Android SDK 37（路径 `D:\Android\Sdk`）。
- **成果核验命令**：
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

---

## 6. 验证状态

### 6.1 已完成验证
- [x] **APK 真实身份与资产提取**：SHA-256 校验通过，提取出 `classification-model.ser`（3.63 MB）与 `TimeExp.m`（3031 字节）。
- [x] **证据链物理核验**：12 个关键反编译类与方法 JSON 证据文件均物理存在且包含唯一 `evidence_id`。
- [x] **分类模型与类定义解构**：提取序列化流中 10 个核心类与 SUID，确认 LibLinear 与 HanLP 底层架构，导出 70 个二级分类标准标签。
- [x] **时间自然语言解析模式**：解压反序列化提取出 7,327 字符高精度时间匹配正则，6 组典型时间句子匹配测试 100% 通过。
- [x] **金额提取与文本清洗规则**：实现毛角转点与数字提取逻辑，6 组典型记账句子金额与备注测试 100% 通过。
- [x] **餐饮时段状态机**：4 组边界时刻状态转换测试 100% 通过。
- [x] **统一 Room 数据库实体与增量迁移构建**：建立 9 大新增业务领域实体，导出 Room `4.json` schema（13 张表），实现 `MIGRATION_3_4`，官方 SQLite 演练 v1 至 v4 全迁移链验证通过，`Migration3To4Test` 单元测试通过，`.\gradlew.bat :app:testDebugUnitTest :app:lintDebug` 100% 通过。

### 6.2 未验证与待后续阶段执行
- [ ] 专用模拟器断网参考界面层级与操作核查（归属 T01-C）。
- [ ] 完成历史分类映射与分类管理迁移（归属 T04）。

---

## 7. 下一阶段交接

- **第一步**：新会话读取本交接文档与实施计划 [t03_data_model_and_migration_plan.md](file:///D:/github-repository/smart-record/docs/plans/t03_data_model_and_migration_plan.md)，基于已构建的 Room 实体与分类目录，开启 **T04（完成历史分类映射与分类管理迁移）**。
- **验收标准**：
  - 运行 `node work/scripts/yimu-offline-rebuild/t03/verify_T03_deliverables.js` 退出状态为 0；
  - 运行 `node work/scripts/yimu-offline-rebuild/t02/verify_T02_deliverables.js` 退出状态为 0；
  - 运行 `node work/scripts/yimu-offline-rebuild/financial/verify_T01_B_deliverables.js` 退出状态为 0；
  - 制定明确的旧分类至一木 76 项分类的映射策略与双向兼容逻辑；
  - 运行 `./gradlew.bat :app:testDebugUnitTest` 全部测试通过。
- **禁止事项**：
  - 严禁擅自使用 Git 进行回滚或重置操作；
  - 严禁直接改动当前工作区未暂存的 11 个历史有效修改；
  - 严禁直接抹除原有数据库表或破坏版本 4 现有数据；
  - 严禁违背已共识的长期架构决策。
