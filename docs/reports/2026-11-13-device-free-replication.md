# 一木记账复刻：设备无关收尾与静态一致性审计

日期：2026-11-13
参考安装包：`reference_apk/base.apk`（`com.wangc.bill` 6.1.8，versionCode 378，SHA-256 `e5ec945aa290c6f66fcd43c3f4307eebc5c7796d188e41895dcdf7fed599f8fd`）
复刻应用：`com.nonen.Bookkeeping`，Room 版本 6，包名与既有用户数据保持不变。

## 1. 本轮边界

用户要求不使用真机与模拟器进行调试。本轮所有结论来自安装包解码资源、JADX 反编译源码、2026-11-13 09:32 暂停快照中的真机参考截图与 UI 层级，以及 JVM 单元测试与构建产物。设备侧像素门禁沿用暂停快照记录：31 个页面中 22 页通过、9 页存在已知残差。

## 2. 证据来源

| 证据 | 路径 | 用途 |
| --- | --- | --- |
| 解码资源 | `work/ui-evidence/apktool/res/` | 布局、颜色、字符串、图标、尺寸 |
| 反编译源码 | `work/ui-evidence/jadx/sources/` | 绘制公式、适配器、图表配置、交互分支 |
| 真机参考截图与层级 | `work/ui-capture/ref/` | 31 页逐页参考与节点 bounds |
| 上次全量回归 | `work/logs/yimu-ui-reconstruction-s11/pause-20261113-0932/README.md` | 22/31 通过、9 页残差与根因 |
| 静态审计 | `work/logs/yimu-device-free/static-audit.md` | 原版布局与复刻布局逐属性比对 |

## 3. 本轮实现

### 3.1 日历页绘制对齐

原版 `ActivityMonthView.java` 与 `CustomWeekView.java` 的绘制公式逐项移植到 `ui/calendar/YimuCalendarView.kt`：

- 日号基线 = `mTextBaseLine + (y - mItemHeight/4)`（`ActivityMonthView.java:95,111`）。
- 无账单农历基线 = `mTextBaseLine + y + mItemHeight/10`（`ActivityMonthView.java:101,118`）。
- 支出行 = `mTextBaseLine + y`，收入行在其后 `mItemHeight/5`（`ActivityMonthView.java:106-107,124-126`）。
- 选中圆角矩形 = `(x+3dp, y+3dp, x+mItemWidth-3dp, y+mItemHeight-3dp)`，3dp 按 `(int)(3×density+0.5)` 取整（`ActivityMonthView.java:217-220`）。
- 删除此前经验补丁：`top = y - 2f` 与 `mItemHeight -= 0.9dp`。原版两个类都没有 `onSizeChanged`，库以 55dp 整数倍计算行高与基线。
- 周末、未来日、当日与选中日颜色按 `ActivityMonthView.java:247-267` 分支重写；日号与金额颜色改用主题资源，夜间模式可以随 values-night 切换。
- 日历高度改为按当月行数计算：参考机 2026-11（周日起算 5 行）实测 1028px = 316.3dp，四行与六行月份分别按 55dp 增减。此前固定 316.3dp 会让六行月份与四行月份错位。

新增 `YimuCalendarGeometryTest`（7 项）与 `YimuCalendarHeightTest`（4 项），锁定上述公式与行数换算。

### 3.2 分类管理页选中态

原版 `CategoryEditActivity` 首屏在 `e0()` 调用 `payBtn.getPaint().setFakeBoldText(true)`（`CategoryEditActivity.java:229`），切换时按选中态设置 16sp 黑色与 13sp 灰色（`440-446`、`604-609`）。复刻删除 XML 中错误的 `textStyle="bold"`（真实粗体与 fakeBold 的字形不同），字号按原版 `16dp/13dp` 写入，并在 `YimuCategoryManageNativeView.bind` 中按选中态设置字号、颜色与 `isFakeBoldText`。

### 3.3 预算页横轴

原版 `BudgetManagerActivity` 以当月天数作为数据点数量与 `setLabelCount` 参数，formatter 隐藏偶数位置并输出 `(int)value + 起始日`，超过当月天数回绕（`BudgetManagerActivity.java:335-360, 887-900`）。复刻此前固定 31 个数据点、`labelCount=15`、`granularity=2`，导致 30 天月份多出一个标签。现改为按当月天数生成数据点与标签，新增 `YimuBudgetNativeViewTest`（5 项）覆盖 28/30/31 天与回绕。

### 3.4 智能规则页原生化

页面按 `activity_self_ai_type.xml`（`tip_layout` + `ai_type_list`）与工具栏 `activity_base_tool_bar.xml` 重建，行布局按 `item_ai_type.xml` 实现类别图标、类别名与关键词换行标签。数据读取 `CategoryRuleEntity` 中 `isCustom=true` 的规则，按类别聚合；空态沿用原版提示文案。新增 `YimuAiTypeNativeViewTest`（1 项）验证过滤与聚合。

### 3.5 深浅主题切换

此前 `MainActivity` 只切换 Compose 配色，原生 View 页面仍使用日间资源，且主题页的“夜间主题/一木”项目点击提示不支持。本轮补齐：

- `SettingsStore` 增加主题模式同步缓存，`setThemeMode` 同时写入 DataStore 与 SharedPreferences。
- `BookkeepingApp` 在进程启动时把缓存值写入 `AppCompatDelegate.setDefaultNightMode`。
- `MainActivity` 改为 `AppCompatActivity`，主题模式变化时同步 AppCompat 夜间模式，Activity 重建后原生页面取到 values-night 资源。
- 主题外观页的“萤火”（night）切换为深色模式，“一木”（default）切换为浅色模式；其余主题市场项目保持本地版本提示。

### 3.6 资产页分组列表

原版资产页使用分组适配器 `t0`：组头 `item_asset_info.xml` 显示组名、账户数、组总额与展开箭头，组内 `item_asset_info_child.xml` 显示账户图标、名称、备注与 15sp 金额（`adapter/t0.java:100-102,143-169,178-229`、`adapter/v0.java:17-88`）。分组按 `Asset.getGroupName()`，组总额只统计 `intoTotalAsset=true` 的账户（`manager/c.java:1158-1169,651-689`）。

复刻此前把账户渲染成扁平列表，行内金额 14sp、无组头。本轮新增 `item_yimu_asset_header.xml`，重写 `item_yimu_asset_row.xml` 与 `YimuAssetNativeView`：组头可展开/折叠、显示 `(N)` 与组总额、箭头旋转 90°（与缺失的 `ic_right_small` 逐像素一致），组内行显示 15sp 金额并按原版规则着色；隐藏金额时组头与行内均显示 `****`。默认现金账户的分组名由自造的“现金账户”改为原版“资金账户”，旧数据库在打开时统一更正。

新增 `AssetGroupingTest`（8 项）覆盖分组、类型回退、组顺序、组内排序、组总额与金额掩码。

### 3.7 预算进度与日均、账户选择行、日期弹窗高度

- 预算进度条：原版 `CircleLineView` 是 5dp 圆头线段，左右各留 5dp，按百分比绘制（`view/CircleLineView.java:17-84`）；`BudgetManagerActivity.s0` 传入的是剩余比例 `(预算-支出)/预算×100`，预算减支出小于等于 0 时不绘制（`BudgetManagerActivity.java:679-681`）。复刻新增 `YimuCircleLineView`，替换布局中两个普通 View，轨迹色 `colorPrimaryAlpha10`、填充色 `textColorPrimary`，`budgetRemainingPercent` 取整并钳制到 0..100。
- 日均与日预算：原版用已过天数与剩余天数计算（`BudgetManagerActivity.java:620-675`）。复刻新增 `budgetDays`：日均 = 支出 / 已过天数，日预算 = 剩余 / 剩余天数（含当天），过去月份日预算为 0、未来月份日均为 0；原版用“当天支出”修正日预算的分支未实现，按剩余/剩余天数简化。
- 账户选择弹窗行：原版 `item_asset_choice.xml` 为 55dp 行，包含 20dp 图标、名称/备注竖排与右侧 14sp 金额（备注 12sp，信用额度行默认隐藏）。复刻此前只有名称，现按原版重建 `item_yimu_asset_choice.xml`，在 `YimuAssetDialog` 中绑定账户金额，无账户行隐藏金额。
- 日期弹窗日历高度：`dialog_date_choice.xml` 的 CalendarView 为 `wrap_content`，原版高度随当月行数变化；复刻改为与日历页共用 `calendarHeightPx`，按当前月份设置高度，五行的采集月份仍为 1028px。

## 4. 验证结果

### 4.1 构建、单元测试与 Lint

| 项目 | 命令 | 结果 |
| --- | --- | --- |
| 构建 | `gradlew.bat :app:assembleDebug` | BUILD SUCCESSFUL |
| JVM 测试 | `gradlew.bat :app:testDebugUnitTest` | 29 个 suite、176 个用例、0 失败、0 错误（本轮新增日历几何 11、预算 7、规则 1、资产分组 8） |
| Lint | `gradlew.bat :app:lintDebug` | BUILD SUCCESSFUL，0 个 error，警告以 HardcodedText/ContentDescription 为主（来自原版布局文案） |

### 4.2 静态一致性审计

`work/scripts/static_audit.py` 把 screens.yaml 的 31 个页面归并为 24 个布局比对组，按 `android:id` 比对尺寸、内外边距、文字大小、颜色、背景、可见性、图标与文案，共记录 576 条差异（高 52、中 67、低 261、人工复核 196）。

与已有真机结论互相印证的部分：

- `import_export`（13 个 id）与 `backup`（33 个 id）0 条差异，与“逐像素通过”一致。
- `account_book_popup` 6/6 同名 id、0 差异。
- `theme_appearance` 仅 3 条低差异。
- 复刻映射布局引用的 23 个颜色键与原版同名且数值全部一致。

审计给出的 12 条“真实缺陷候选”逐项处置：

| 候选 | 处置 |
| --- | --- |
| `home:month_pay_title` 文案“本月支出(元)” | 参考真机层级同样显示“本月支出(元)”，原版 XML 静态文案在运行时被改写，非缺陷 |
| `add_bill_date_dialog:calendarView` 高度 | 日期弹窗使用独立 `DateMonthView`，参考弹窗已按用户决定记录 1px 字体差异；本轮未改 |
| `calendar:data_list`、`calendar:month` 高度 | 页面在同状态真机截图中仅 FAB 与选中日残差，属于 XML 与运行时代码设置差异，非可见缺陷 |
| `asset:asset_background` 高度 | 横幅由 `YimuAssetBannerLayout` 按 980:264 比例绘制，资产页曾逐像素通过，非缺陷 |
| `asset:cost` 14sp | 已按原版 `item_asset_info_child.xml` 改为 15sp |
| `stats_month/stats_year:month_list/year_list` 尺寸 | 月份/年份选择层在参考截图状态不可见，未做可见性验证 |
| `my:data_layout` 高度 | 背景容器，无可见内容差异 |
| `budget:progress_view_two` 宽度 0dp | 已按原版 `CircleLineView` 实现百分比进度，轨迹与填充颜色、线宽、左右留白一致 |

审计同时确认了设置页动态行、记账页键盘、账户选择弹窗行、周期/愿望空态等“无法按 id 对照”的结构差异，均已记录在 `static-audit.md` 第六节。

## 5. 不能由静态证据证明的部分

- 本轮修改后的页面像素差异：日历、分类管理、预算、规则、资产分组、主题切换都需要重新采集真机截图才能确认。
- 夜间主题 29 页的灯箱截图与颜色差异。
- 字体栅格化、系统窗口阴影、FAB 图标边缘、图表抗锯齿等 1–3px 级残差。
- 用户个人内容（我的页头像与主题背景、规则页自定义规则、账户备注与图标）在参考侧与复刻侧的差异。
- 运行时交互：主题切换重建、资产分组展开、规则新增、预算进度条。

## 6. 交付物

- APK：`app/build/outputs/apk/debug/app-debug.apk`（包名 `com.nonen.Bookkeeping`，versionName 0.2.3，32,065,767 字节，SHA-256 `b06515658f13c8e3ce4a8f46710fc5090fb4c05c0714ae5d02f4700a9f58e9c1`）。
- 单元测试报告：`app/build/test-results/testDebugUnitTest/`。
- Lint 报告：`app/build/reports/lint-results-debug.html`。
- 静态审计：`work/logs/yimu-device-free/static-audit.md`、`static-audit.json`、`work/scripts/static_audit.py`。
- 任务状态：`work/tasks/yimu-ui-reconstruction.md`。

## 7. 剩余差距

- 统计日常页首个柱顶数值右缘 1px 抗锯齿差，未找到静态成因。
- 日期弹窗、账户选择弹窗的系统阴影与 1px 字体差异（用户 2026-10-20 决定记录）。
- 我的页头像与主题背景属于用户个人内容；标签管理、使用指南、关于我们仍是本地提示。
- 主题市场除“萤火/一木”外的主题、LOGO 与背景切换仍是占位。
- 预算日预算按“剩余/剩余天数”简化，原版用当天支出修正的分支未实现；日均与日预算只能在有预算数据的真机上核对。
- 资产行图标与备注缺少数据字段，使用占位图标；分组重命名、拖拽排序、折叠动画与状态持久化未实现。
- 会员、云同步、语音记账、股票、借贷、自动记账、桌面小组件按既定范围不接入。
- 应用名保持“轻记账”，与参考安装包“一木记账”不同，便于同一台设备上两个应用共存区分。
