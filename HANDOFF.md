# 阶段交接文档（HANDOFF.md）

## 1. 状态

- **阶段编号**：T01-B（本地资金业务参考调查）
- **当前状态**：完成
- **阶段目标**：针对原应用 `com.wangc.bill 6.1.8 (378)` 本地资金流转业务开展只读逆向分析，解构账单、资产账户、转账、退款、报销、借还款及分期业务的底层实体、主外键、前置校验、余额数学公式与级联规则。

---

## 2. 变更文件清单

| 文件路径 | 改动说明 | 变更原因 |
|---|---|---|
| `financial-reference.json` | 导出 12 个金融实体与 7 大业务规则配置 | 供后续数据建模与重构读取完整业务规范 |
| `work/evidence/yimu-offline-rebuild/T01/financial/financial-reference.json` | 归档完整资金业务数据规范 | 留存证据备份与版本受控 |
| `work/reports/yimu-offline-rebuild/T01-B-financial.md` | 编写资金业务技术核查报告 | 详尽记录业务状态机矩阵与数学公式 |
| `work/tasks/yimu-offline-T01-B-state.md` | 更新 T01-B 任务进度与验证日志 | 记录任务完成状态与下一项操作 |
| `work/scripts/yimu-offline-rebuild/financial/verify_T01_B_deliverables.js` | 编写成果独立核验脚本 | 验证 APK 校验和、实体完整性与证据编号 |
| `work/logs/yimu-offline-rebuild/T01-B-20261018-01/verification_result.json` | 保存独立核验结果文件 | 留存核验通过记录 |
| `HANDOFF.md` | 编写阶段交接文档 | 供新会话 AI 冷启动衔接 |

---

## 3. 关键决策与理由

1. **采用独立领域实体表架构（Dedicated Room Tables）**
   - **决策**：在 Room 中为 `Asset`、`Transfer`、`Refund`、`Reimbursement`、`Lend`、`Instalment` 建立独立的 Entity 表，通过 Room 事务统一保证资金联动与原子性。
   - **否决备选**：将所有资金流折叠进单一多态 `transactions` 宽表。
   - **理由**：原应用业务逻辑解耦充分，各业务实体拥有独立的自增 ID 体系与生命周期，独立表设计能清晰表达外键约束并防止字段污染。
2. **金额精度存储严格对标原应用使用 Double 类型**
   - **决策**：在 Room 实体中直接使用 `Double` 存储金额数值，配合统一的格式化与保留两位小数运算工具函数（对标原应用 `g2.q()` 与 `g2.R()`）。
   - **否决备选**：升级为长整型“分”（`Long`）或 `BigDecimal` 存储。
   - **理由**：保持与原应用物理存储结构完全一致，降低数据转换与历史数据迁移复杂度。
3. **历史数据采用增量平滑迁移（In-place Smooth Migration）**
   - **决策**：编写严格的数据库升级逻辑，在新建独立表的同时，根据现存记录的 `account` 字段自动初始化默认资产并回填关联 `assetId`。
   - **否决备选**：将原有表设为只读历史表或清空重建。
   - **理由**：必须确保用户现存版本 3 的所有账单数据完整可用且无损过渡。
4. **业务状态机规范建立 5 维标准矩阵**
   - **决策**：对 7 大资金业务逐一梳理【输入校验】、【持久化对象与主外键】、【资产余额计算表达式】、【修改删除对冲级联规则】以及【统计计入口径】。
   - **理由**：为后续业务实现提供严谨、无模糊空间的参考规范，杜绝凭借常识臆造。
5. **排除房贷车贷（Loan）业务**
   - **决策**：根据用户在方案沟通中的明确指示，将房贷、车贷等复杂中长期借贷业务移出重建范围，聚焦核心记账与资产流转。

---

## 4. 已知坑与失败尝试

- **现象**：命令行调用 REA 工具时偶发 `exit status 0xc0000142`（`STATUS_DLL_INIT_FAILED`）。
  - **原因**：Windows 系统中 Oracle Java 路径存在动态链接库加载竞争。
  - **正确做法**：通过 Node.js 直接调用公共 `rea.mjs`，并显式注入环境变量 `REA_JADX_MCP_JAR = 'C:/Users/29602/.local/share/rea/android/jadx-headless-mcp-0.7.1-all.jar'`。
- **现象**：Plan 模式下执行工作区文件写入被 safety-guard 拦截阻止。
  - **原因**：Antigravity 环境处于计划保护只读状态。
  - **正确做法**：在指定运行路径生成方案文件并请求用户核准，通过 Proceed 确认后退出只读模式。
- **现象**：检查 `static final int` 类型字面量常量时缺少 `<clinit>` 反编译代码。
  - **原因**：Java 编译期已将基本数值内联，不生成静态初始化器字节码。
  - **正确做法**：从调用方的反编译逻辑和字段声明中直接提取数值。

---

## 5. 环境与命令

- **系统平台**：Windows 11，PowerShell 7.6.7，Node.js，JDK 21，Android SDK 37（路径 `D:\Android\Sdk`）。
- **逆向分析工具**：
  - REA 6.5.0（路径 `D:/dev-cache/npm-cache/_npx/ad9a3fdbcad992c6/node_modules/rea-agents/scripts/rea.mjs`）
  - JADX Headless MCP 0.7.1（路径 `C:/Users/29602/.local/share/rea/android/jadx-headless-mcp-0.7.1-all.jar`）
  - 依赖库：`work/deps/smali`
- **成果核验命令**：
  ```powershell
  node work/scripts/yimu-offline-rebuild/financial/verify_T01_B_deliverables.js
  ```
- **参考 APK 基准**：
  - 路径：`reference_apk/base.apk`
  - 校验和：`e5ec945aa290c6f66fcd43c3f4307eebc5c7796d188e41895dcdf7fed599f8fd`

---

## 6. 验证状态

### 6.1 已完成验证
- [x] **APK 真实身份**：SHA-256 匹配一致，包名与版本确认无误。
- [x] **数据库架构与映射**：提取 `assets/litepal.xml` 证实版本 2707 与 82 项实体映射。
- [x] **实体字段完备性**：12 个金融实体通过 REA 提取完整字段、类型及主外键。
- [x] **业务逻辑闭环性**：
  - 收支记账与资产增减：`a0.h` -> `f.s1`（新增/修改）与 `f.r`（删除）；
  - 转账汇率与手续费：`TransferAddActivity.complete` -> `u2.g`（跨币种公式与手续费独立单据）；
  - 退款冲正与差额单：`AddRefundActivity.complete` -> `y1.g` 与 `y1.i`（溢出差额单与级联反向清算）；
  - 报销回款与冲销：`z1.g` 与 `z1.k`（明细结构 `"{assetId}:{number}"` 资金反向追回）；
  - 借贷双账户流转：`LendAddActivity.complete` -> `h1.d`（借出与借入双向记账）；
  - 分期零头分配算法：`f1.z`（四种分配规则完整数学表达与原单关联联动）。
- [x] **证据链物理核验**：报告内引用的 25 项证据编号均与本地磁盘 JSON 文件一致。

### 6.2 未验证与待后续阶段执行
- [ ] 实际断网环境下与 UI 交互联动的完整执行（由 T01-C 界面调查会话独立核验，无冲突风险）。
- [ ] 定期存款（`AssetFixedDeposit`）与手动投资（`StockAsset`）的深层收益算式（后续阶段独立核验）。

---

## 7. 下一阶段交接

- **第一步**：启动者读取本交接文档与 `work/reports/yimu-offline-rebuild/T01-B-financial.md`，汇总 T01-A、T01-B、T01-C 全部成果，开启 T02（本地分类与分词验证）或 T03（统一数据模型与迁移构建）。
- **验收标准**：
  - 运行 `node work/scripts/yimu-offline-rebuild/financial/verify_T01_B_deliverables.js` 退出状态为 0；
  - `financial-reference.json` 正确反映 7 大核心业务规范。
- **禁止事项**：
  - 严禁擅自修改 `app/` 源码、测试用例或 Gradle 构建文件；
  - 严禁擅自使用 Git 回滚或重置代码；
  - 严禁违背已达成共识的五项核心架构决策。
