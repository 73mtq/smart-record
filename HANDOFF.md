# 阶段交接文档（HANDOFF.md）

## 1. 状态

- **阶段编号**：S02（T02：本地文字分类与日期解析离线验证）
- **当前状态**：完成
- **阶段目标**：针对原应用 `com.wangc.bill 6.1.8 (378)` 纯离线文字记账中的核心算法、模型与规则进行独立解构与闭环验证，提取分类模型与时间正则资源，核实分词、金额归一化、分类置信度判定与餐饮时段映射状态机。

---

## 2. 变更文件清单

| 文件路径 | 改动说明 | 变更原因 |
|---|---|---|
| `docs/plans/t02_text_classification_plan.md` | 编写 T02 单元实施计划方案 | 规范离线模型提取与验证步骤 |
| `work/evidence/yimu-offline-rebuild/T02/resources/classification-model.ser` | 提取线性 SVM 分类模型物理文件 | 留存纯离线模型核心资产（3.63 MB） |
| `work/evidence/yimu-offline-rebuild/T02/resources/TimeExp.m` | 提取时间表达式正则规则物理文件 | 留存时间自然语言解析核心资产（3031 字节） |
| `work/evidence/yimu-offline-rebuild/T02/time_exp_regex.txt` | 导出完整时间匹配正则表达式 | 保存从 `TimeExp.m` 反序列化生成的 7,327 字符规则串 |
| `work/evidence/yimu-offline-rebuild/T02/extracted_categories_from_ser.json` | 导出模型包含的全量分类标签目录 | 提取并归档模型预测支持的全部 70 个业务分类 |
| `work/evidence/yimu-offline-rebuild/T02/model_classes_in_ser.json` | 导出序列化流包含的 10 个核心类与 SUID | 固化模型底层依赖架构与类版本号定义 |
| `work/evidence/yimu-offline-rebuild/T02/*.json` | 归档 12 项 REA 反编译类与方法证据文件 | 留存包含全局唯一 `evidence_id` 的反编译证据链 |
| `work/reports/yimu-offline-rebuild/T02-classification-time-nlp.md` | 编写 T02 本地文字分类与日期解析技术核查报告 | 详尽记录算法原理、调用链路、数学公式与测试矩阵 |
| `work/tasks/yimu-offline-T02-state.md` | 记录 T02 任务进度、验证结果与下一项操作 | 维护单元独立生命周期状态 |
| `work/tasks/yimu-offline-rebuild.md` | 更新总任务状态并推进至 T03 | 维护全工程统一进度视图 |
| `work/scripts/yimu-offline-rebuild/t02/verify_T02_deliverables.js` | 编写 T02 独立自动化综合验收脚本 | 验证 APK 校验和、资源一致性与全部测试用例 |
| `work/logs/yimu-offline-rebuild/T02-20261018-01/verification_result.json` | 保存独立核验结果文件 | 留存自动化验收通过记录 |
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
7. **分类置信度阈值与餐饮时段动态判定规则【本阶段】**
   - **决策**：分类预测置信度严格执行原应用 `0.015` 门槛值，低于该值统一归入 `其他-其他`；对于命中 `食品餐饮-其他` 的单据，依据 24 小时制进行时段修正：4~10 归为早餐、10~16 归为午餐、其余归为晚餐。
   - **理由**：完全对齐原应用 `v4.r` 核心代码的实际行为。
8. **金额预处理毛角转点与动词剥离规则【本阶段】**
   - **决策**：前置执行正则替换 `(块|元|点)([1-9一二三四五六七八九])[毛角]` 为 `点$2`，剥离“花了”、“用了”等动词，并将首尾单字单位消除后再行提取数值。
   - **理由**：保证中文数字与阿拉伯数字混合场景均可精确抽取金额并还原干净备注。

---

## 4. 已知坑与失败尝试

- **现象**：命令行调用 REA 工具偶发 `exit status 0xc0000142`（`STATUS_DLL_INIT_FAILED`）。
  - **原因**：Windows 环境中 Oracle Java 路径存在动态链接库加载竞争。
  - **正确做法**：通过 Node.js 直接调用公共 `rea.mjs`，并显式注入环境变量 `REA_JADX_MCP_JAR = 'C:/Users/29602/.local/share/rea/android/jadx-headless-mcp-0.7.1-all.jar'`。
- **现象**：Plan 模式下执行工作区文件写入被 safety-guard 拦截阻止。
  - **原因**：Antigravity 环境处于计划保护只读状态。
  - **正确做法**：先生成 plan artifact 请求用户核准，通过 Proceed 确认后再写入工作区。
- **现象**：命令行调用 `inspect-android-class` 时传入 `--class-name` 参数报错终止。
  - **原因**：`rea.mjs` 底层接收的是位置参数 `apkPath className` 而非选项标志。
  - **正确做法**：统一使用工作区封装的 [rea_utils.js](file:///D:/github-repository/smart-record/work/scripts/yimu-offline-rebuild/rea_utils.js) 提供的标准函数 `inspectClass(cls, outFile)`。
- **现象**：保存反编译文件时触发 `ENOENT` 文件系统异常。
  - **原因**：方法名包含 `<init>` 等字符，Windows 系统文件名禁止使用尖括号。
  - **正确做法**：在拼接输出文件名时对特殊符号执行安全正则替换（如 `replace(/[<>]/g, '_')`）。
- **现象**：反编译内部类时报错 `Exact class not found: SpeechManager$b`。
  - **原因**：JADX 内部类索引采用点号语法（`SpeechManager.b`）而非 JVM 字节码的美元符号语法。
  - **正确做法**：通过 `get_class_summary` 的 `inner_classes` 清单确认名称，使用点号连接。

---

## 5. 环境与命令

- **系统平台**：Windows 11，PowerShell 7.6.7，Node.js 24.16.0，JDK 21，Android SDK 37（路径 `D:\Android\Sdk`）。
- **逆向分析工具**：
  - REA 6.5.0（路径 `D:/dev-cache/npm-cache/_npx/ad9a3fdbcad992c6/node_modules/rea-agents/scripts/rea.mjs`）
  - JADX Headless MCP 0.7.1（路径 `C:/Users/29602/.local/share/rea/android/jadx-headless-mcp-0.7.1-all.jar`）
  - 辅助工具：[rea_utils.js](file:///D:/github-repository/smart-record/work/scripts/yimu-offline-rebuild/rea_utils.js)
- **参考 APK 基准**：
  - 路径：[base.apk](file:///D:/github-repository/smart-record/reference_apk/base.apk)
  - 校验和：`e5ec945aa290c6f66fcd43c3f4307eebc5c7796d188e41895dcdf7fed599f8fd`
- **成果核验命令**：
  - T02 独立验收：
    ```powershell
    node work/scripts/yimu-offline-rebuild/t02/verify_T02_deliverables.js
    ```
  - T01-B 独立验收：
    ```powershell
    node work/scripts/yimu-offline-rebuild/financial/verify_T01_B_deliverables.js
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

### 6.2 未验证与待后续阶段执行
- [ ] 专用模拟器断网参考界面层级与操作核查（归属 T01-C）。
- [ ] 统一 Room 数据库实体与增量迁移构建（归属 T03）。

---

## 7. 下一阶段交接

- **第一步**：新会话读取本交接文档与技术报告 [T02-classification-time-nlp.md](file:///D:/github-repository/smart-record/work/reports/yimu-offline-rebuild/T02-classification-time-nlp.md)，汇总 T01（分类与资金业务）与 T02（离线模型规则）成果，开启 **T03（统一数据模型与数据库增量平滑迁移构建）**。
- **验收标准**：
  - 运行 `node work/scripts/yimu-offline-rebuild/t02/verify_T02_deliverables.js` 退出状态为 0；
  - 运行 `node work/scripts/yimu-offline-rebuild/financial/verify_T01_B_deliverables.js` 退出状态为 0；
  - 在 Room 中建立完整的资金业务实体（`Asset`、`Transfer`、`Refund` 等），实现 `MIGRATION_3_4`；
  - 运行 `./gradlew.bat :app:testDebugUnitTest` 全部测试通过。
- **禁止事项**：
  - 严禁擅自使用 Git 进行回滚或重置操作；
  - 严禁直接改动当前工作区未暂存的 14 个历史有效修改；
  - 严禁直接抹除原有数据库表或破坏版本 3 现有数据；
  - 严禁违背已共识的 5 大长期架构决策。
