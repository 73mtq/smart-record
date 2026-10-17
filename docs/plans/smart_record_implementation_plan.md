# 智能记账应用实施方案与设计计划

## 1. 目标与背景说明

以开源项目 `Lite_Bookkeeping`（Apache-2.0 协议）为技术基底，融合一木记账的视觉与交互语言，构建一款纯本地运行、无网络权限、具备智能自动分类与科学统计分析的个人记账 Android 应用。

### 核心需求对应
1. **输入信息智能选择分类**：在记账表单中实现五级优先判定链（商户记忆 → 自定义规则 → 平台分类 → 内置词库 → 兜底），输入商户或备注时实时自动联动选择分类，并支持复合短语（如“妈妈转账生活费2000”）快速提取填入。
2. **分类体系扩充与财务口径建模**：新增“生活费”与“奖学金”分类，在财务统计模型上设立 `IncomeGroup`（家庭支持、奖助资助、劳动报酬、往来理财），防止生活费等非劳动供养混入常规收入导致月收入虚高与支出占比分母失真。
3. **多维度统计与分析视图**：支持日/周/月/年统计，提供趋势柱状图与占比环形图，支持支出占比排行榜与“收入来源构成（钱从哪来）”分析。
4. **Excel 备份导出与导入**：生成标准多工作表 `.xlsx` 文件（工作表 1 保持流水明细用于无损回灌，工作表 2 生成分类透视与 AI 财务诊断汇总）。
5. **小而美与界面风格**：采用一木记账经典的生机翠绿与珊瑚暖红配色，搭配圆角卡片质感，维持零网络权限与纯本地存储。

---

## 2. 需要用户评审的关键设计决定

> [!IMPORTANT]
> **财务建模口径确定**：
> 生活费与奖学金在分类底层虽然属于资金流入，但在个人财务分析中属于外部供养或奖助。方案将其划归为独立的 `IncomeGroup.SUPPORT` 与 `IncomeGroup.MERIT`，在统计页与 Excel 导出中单独呈现占比，保证日常劳动报酬（工资、兼职）与受资助资金边界清晰。

> [!NOTE]
> **输入自动分类交互优先级策略**：
> 1. 新建记录时输入商户名或备注，触发实时分类推断，并在界面呈现高亮标签。
> 2. 用户若在分类九宫格中人工主动点击了其他分类，系统进入“用户手动锁定”状态，后续键入不再自动覆盖，并提供“重置智能匹配”按钮以恢复自动状态。

---

## 3. 待确认的设计要点

1. **分类九宫格列数**：默认采用 3 列大卡片圆角样式（易点触），也可调整为 4 列微卡片样式（单屏容纳更多分类）。
2. **Excel 导出结构**：首工作表固定为原始流水列以确保导入回灌兼容性，第二工作表为汇总报表。

---

## 4. 详细实施变更方案

### 4.1 分类与规则引擎模块
- [`Categories.kt`](file:///D:/github-repository/smart-record/app/src/main/java/com/nonen/Bookkeeping/core/Categories.kt)
  - 增加“生活费”（🏡）与“奖学金”（🎓）至收入分类首列。
  - 扩充默认规则：包含“生活费转账”、“妈妈转账”、“爸爸转账”、“生活费”、“生活补助”、“家庭支持”、“国家奖学金”、“助学金”等。
  - 新增 `IncomeGroup` 枚举及映射函数 `incomeGroupOf(category)`。

### 4.2 记账表单与智能联动模块
- [`AddEditViewModel.kt`](file:///D:/github-repository/smart-record/app/src/main/java/com/nonen/Bookkeeping/ui/screens/AddEditViewModel.kt)
  - 引入 `RuleEngine` 依据快照，增加 `triggerAutoClassify()`、`applyQuickText()` 及 `selectCategoryManually()` 方法。
  - 增加智能建议状态 `autoSuggestedCategory` 与手动选择标记 `isCategoryManuallyChosen`。
- [`AddEditScreen.kt`](file:///D:/github-repository/smart-record/app/src/main/java/com/nonen/Bookkeeping/ui/screens/AddEditScreen.kt)
  - 新增智能速记自然语言输入栏。
  - 在分类标题栏增加“✨ 智能匹配: [分类名称]”动态徽章与重置入口。
  - 优化商户与备注输入字段的实时联动监听。

### 4.3 统计与可视化模块
- [`StatisticsScreen.kt`](file:///D:/github-repository/smart-record/app/src/main/java/com/nonen/Bookkeeping/ui/screens/StatisticsScreen.kt)
  - 调整排行榜标题：支出方向显示“支出分类占比排行”，收入方向显示“收入来源构成（钱从哪来）”。
- [`StatsComponents.kt`](file:///D:/github-repository/smart-record/app/src/main/java/com/nonen/Bookkeeping/ui/screens/StatsComponents.kt)
  - 在排行卡片 `RankCard` 中为收入项目动态显示收入性质徽章（如“家庭支持”、“奖助资助”）。

### 4.4 Excel 导出模块
- [`MiniXlsx.kt`](file:///D:/github-repository/smart-record/app/src/main/java/com/nonen/Bookkeeping/core/MiniXlsx.kt)
  - 增加 `writeSheets(sheets: List<Sheet>)` 支持多工作表生成。
- [`BackupExporter.kt`](file:///D:/github-repository/smart-record/app/src/main/java/com/nonen/Bookkeeping/export/BackupExporter.kt)
  - 生成“账单明细”与“统计汇总”双工作表，自动计算收支总额、净结余、分类占比排行以及大模型分析提示。

### 4.5 主题与视觉定制
- [`Color.kt`](file:///D:/github-repository/smart-record/app/src/main/java/com/nonen/Bookkeeping/ui/theme/Color.kt)
  - 采用一木生机森绿（`#24B064`）作为主色，明媚珊瑚暖红（`#FF5A5F`）作为支出色，温润浅纸灰（`#F7F9F8`）作为背景色。

---

## 5. 验证方案

### 5.1 自动化测试
1. **单元测试集**：
   - 命令：`.\gradlew testDebugUnitTest`
   - 覆盖范围：规则引擎优先级测试（`RuleEngineTest`）、分类链测试（`ClassifyChainTest`）、多工作表导出回环测试（`BackupRoundTripTest`）及日期跟随计算测试（`HomePeriodTest`）。
2. **编译打包验证**：
   - 命令：`.\gradlew assembleDebug`
   - 目标产物：`app/build/outputs/apk/debug/app-debug.apk`。

### 5.2 手动核验场景
1. 打开记账界面输入“妈妈转账2000”，验证金额是否自动变为 2000，方向是否自动转为收入，分类是否选中“生活费”。
2. 查看统计页面收入方向，验证是否呈现“收入来源构成”及“家庭支持”标签。
3. 执行 Excel 导出，验证生成的 `.xlsx` 文件是否包含“账单明细”和“统计汇总”两个工作表。
