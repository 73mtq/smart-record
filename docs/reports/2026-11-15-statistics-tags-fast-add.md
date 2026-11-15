# 统计页标签占比、图表空态与 AI 记账实时识别（S17）

## 1. 问题与根因

| 问题 | 根因 |
| --- | --- |
| 统计页「标签占比」显示 `No chart data available` | `yimu_statistics_month/year/self.xml` 中的 `tag_pie_chart` 从未在 `YimuStatisticsNativeView` 中初始化或渲染，MPAndroidChart 保持无数据默认状态并绘制文案 |
| 参考应用同样没有标签数据却不显示文案 | 原版 `i4.V1` 在空数据时绘制一个 100% 的 `pieChartNone` 灰色扇区，`setDrawValues(false)`，使图表不再进入无数据分支 |
| 首页右下角快速记账不识别金额与分类 | `YimuFastAddDialog` 只在点击添加时调用一次本地解析，输入过程中没有监听，`analysis_layout`、`category_info`、`num_info` 从未被赋值 |
| 月份、年份、自定义日期切换无反应 | 月份条与年份条只渲染文字与选中色，没有点击回调；自定义页起止日期固定为近一个月 |

## 2. 实现

### 2.1 统计聚合

新增 `stats/StatisticsNativeStats.kt`：

- `aggregateTagStats`：按账单标签聚合笔数、支出、收入。同一标签下同一账单只计一次，跨标签各计全额，结果按笔数降序，与原版 `i4.D0`、`TagParentType.compareTo` 一致。
- `aggregateCategoryRows`：按一级分类聚合金额、笔数、百分比，名称可由调用方归一化，与原版 `qg` 适配器字段一致。
- `aggregateDayForms` / `aggregateMonthForms`：按日、按月生成报表行（日期、收入、支出、结余），与原版 `x7` 适配器字段一致。

### 2.2 统计页

- 三个统计页接入标签饼图与标签数据列表：支出与收入都非空时中心提示可点击切换，只有一侧非空时固定显示该侧并隐藏切换图标，两侧都空时隐藏中心提示并绘制灰色空环。
- 月页接入分类数据列表与报表统计列表；分类占比中心提示接入支出/收入切换，切换时同步更新标题、数据标题与列表。
- 年页接入资产走势折线与分类占比饼图、分类数据列表、月度报表列表与标签区块。
- 自定义页接入分类占比饼图与分类数据列表。
- 所有统计图表统一 `setNoDataText("")`，空态由灰色空环承接。
- 月份条（0 本月、1 上月、向前 10 个月）、年份条（0 所有、1 今年、2 去年、向前 5 年）接入点击回调，查询区间随状态变化。
- 自定义页起止日期改为可选，点击弹出 `YimuDateDialog`，起止顺序自动纠正。
- 报表统计与标签数据的排序按钮接入 `PopupMenu`：报表按日期/收入/支出/结余的正序与降序共 8 项，标签按笔数/支出/收入的正序与降序共 6 项；月、年、自定义三页各自保存排序状态，切换后保持。

### 2.3 AI 记账面板

- 输入后 150ms 防抖调用 `ClassificationModelService.parseQuickRecord`，解析出金额才显示「分类 / 金额」并点亮添加按钮，未识别时隐藏分析行、按钮保持禁用，与原版 `AddBillDialog.E0/p0` 的行为一致。
- 解析在后台线程执行，弹窗关闭时取消任务与作用域。
- 保存使用解析出的金额、分类、备注与日期。

### 2.4 构建配置

`app/build.gradle.kts` 的 lint 配置禁用 `UnsafeOptInUsageError`、`UnsafeOptInUsageWarning`：Lint 的 `ExperimentalDetector` 分析 `YimuStatisticsNativeView` 主构造属性时以 `Unexpected owner function: null` 崩溃（`KaFirKotlinPropertyKtParameterBasedSymbol`），属 Lint 自身缺陷。代价是这两项实验性 API 检查不再执行。

## 3. 验证

- `:app:assembleDebug` BUILD SUCCESSFUL。
- `:app:testDebugUnitTest`：181 用例、0 失败，其中 `StatisticsNativeStatsTest` 覆盖标签聚合、重复关联去重、缺失标签忽略、分类百分比、报表结余。
- `:app:lintDebug` BUILD SUCCESSFUL。
- 真机（95f79212，1200×2670）：
  - 快速记账输入 `12` 后显示「分类：出行交通 公共交通」「金额：12.00」，日历图标日号更新为 15，添加按钮点亮。
  - 统计月页点击「9月」后分类数据列表高度 1432、报表统计列表高度 3900，月份切换与查询生效。
  - 分类占比饼图出现真实扇区颜色（#F9ED69、#95E1D3 等）。
  - 年统计页分类数据列表高度 1743、月度报表列表高度 1300，新增列表有数据。
  - 标签表为空（0 标签、0 关联）时 `tag_chart_check` 为 GONE，饼图绘制灰色空环。

## 4. 未完成

- 分类饼图中心的支出/收入切换、自定义日期范围的点击未做真机操作验证（统计页原生内容在 uiautomator 中不可见，真机滚动定位不稳定）。
- 标签类型筛选（所有标签/一级/二级）未实现：本应用标签没有层级字段。
- 报表列头（日期/收入/支出/结余四个表头）未接入点击排序，排序入口目前只有右侧排序按钮。
- 年页「所有」年份的月度柱图把各年同月合并展示，与原版行为未逐项比对。
