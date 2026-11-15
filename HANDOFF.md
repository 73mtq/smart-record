# HANDOFF — S17 统计页补齐与 AI 记账实时识别（2026-11-15）

## 1. 状态

- **阶段：S17（统计页缺口补齐、图表空态与 AI 记账实时识别）。** 修复统计页「标签占比」空图表显示图表库无数据文案的问题，补齐标签占比饼图与标签数据列表、分类数据列表、报表统计列表、年页资产走势与分类占比、自定义页分类占比；接入月份条与年份条切换、自定义页起止日期选择；首页 AI 记账面板改为输入过程中本地解析并回填分类、金额与日期。详见 `docs/reports/2026-11-15-statistics-tags-fast-add.md`。
- **HEAD：fbc48c8**，本轮改动已提交（见 git log）。
- **验证：** `assembleDebug`、`testDebugUnitTest`（181 用例、0 失败）、`lintDebug` 全部 BUILD SUCCESSFUL。真机（95f79212）确认：快速记账输入 12 显示「分类：出行交通 公共交通」「金额：12.00」；月页点击「9月」后分类数据列表高度 1432、报表列表高度 3900；分类占比饼图出现真实扇区颜色；年页分类数据列表高度 1743、月度报表高度 1300；自定义页点击起始日期弹出 `YimuDateDialog`。
- **取巧点：** 统计页原生内容在 uiautomator 中不可见（Compose `AndroidView` 子树未暴露），控件定位改用 `work/scripts/view_tree_bounds.py` 解析 `dumpsys activity top`；页面滚动需用 `input touchscreen swipe`。
- **上一阶段：** S16 真机回归 24 页通过、7 页未通过，遗留 64px tile 抗锯齿与两个系统弹窗差异，见第 8、9 节与 `docs/reports/2026-11-14-calendar-night-regression.md`。

## 2. 本轮源码变更

### 2.1 日历

- `ui/calendar/YimuCalendarView.kt`：按 `ActivityMonthView.java:95-126,217-220` 与 `CustomWeekView.java:101-130,194-198` 重写日号、农历、收支两行基线与选中矩形；删除 `top=-2f` 与 `mItemHeight-=0.9dp` 经验补丁；日号与金额颜色改用主题资源；新增按当月行数计算 CalendarView 高度的纯函数 `calendarRows/calendarHeightPx`（5 行=1028px，四/六行按 55dp 增减）。
- `ui/components/YimuCalendarNativeView.kt`：`render` 时按 `state.month` 设置日历高度。
- 测试：`YimuCalendarGeometryTest`（7）、`YimuCalendarHeightTest`（4）。

### 2.2 分类管理

- `res/layout/yimu_category_manage.xml`：删除 `pay_btn` 的 `textStyle="bold"`，字号改为原版 `16dp/13dp`。
- `ui/components/YimuCategoryManageNativeView.kt`：`bind` 调用 `applyTypeSelection`，按 `CategoryEditActivity.java:229,440-446,604-609` 设置字号、颜色与 `paint.isFakeBoldText`（首屏支出标签为 fakeBold）。

### 2.3 预算

- `ui/components/YimuBudgetNativeView.kt`：数据点数量与 `xAxis.setLabelCount` 改为当月天数，移除 `granularity=2f` 与固定 15，formatter 照 `BudgetManagerActivity.java:335-360` 隐藏偶数位置并回绕。
- 新增 `YimuCircleLineView` 实现原版进度条（5dp 圆头线、左右 5dp、按剩余百分比绘制），替换布局中的两个普通 View；日均与日预算改为按已过天数/剩余天数计算（`BudgetManagerActivity.java:620-681`）。
- 测试：`YimuBudgetNativeViewTest`（7）。

### 2.4 智能规则页

- 新增 `ui/components/YimuAiTypeNativeView.kt`、`res/layout/yimu_ai_type_edit.xml`、`res/layout/item_yimu_ai_type.xml`，按 `activity_self_ai_type.xml` 与 `item_ai_type.xml` 原生化列表、关键词换行标签与空态；数据读取 `CategoryRuleEntity.isCustom` 并按类别聚合。
- `ui/screens/RulesScreen.kt` 改为 `AndroidView(YimuAiTypeNativeView)`，保留新增规则对话框。
- 测试：`YimuAiTypeNativeViewTest`（1）。

### 2.5 深浅主题

- `data/prefs/SettingsStore.kt`：增加主题模式同步缓存 `cachedThemeMode()`，`setThemeMode` 同时写 DataStore 与 SharedPreferences。
- `BookkeepingApp.kt`：进程启动时把缓存值写入 `AppCompatDelegate.setDefaultNightMode`。
- `MainActivity.kt`：改为 `AppCompatActivity`，主题模式变化时同步 AppCompat 夜间模式（原生 View 取到 values-night）。
- `ui/screens/YimuThemeScreen.kt`、`ui/components/YimuThemeNativeView.kt`：主题页“萤火”切换深色、“一木”切换浅色；其余主题入口保持本地提示。
- `ui/AppNavHost.kt`：入口路由只在 intent 改变时导航，主题切换重建后保留原返回栈，不会从主题页跳回首页。

### 2.6 资产页账户分组

- `ui/components/YimuAssetNativeView.kt`：重构为分组适配器，组头显示组名、`(N)`、组总额与展开箭头，组内行显示图标、名称、备注与 15sp 金额；隐藏金额时组头与行内均为 `****`。
- 新增 `res/layout/item_yimu_asset_header.xml`；重写 `res/layout/item_yimu_asset_row.xml`。
- `res/layout/view_yimu_asset.xml`：仅调整 `asset_list` 的上下边距，banner、汇总卡片、tip、FAB 未改。
- 分组与组总额照 `adapter/t0.java:100-229`、`adapter/v0.java:17-88`、`manager/c.java:1158-1169,651-689`；默认现金账户分组名由“现金账户”改为“资金账户”，旧数据库在 `RoomDatabase.Callback.onOpen` 统一更正。
- 测试：`AssetGroupingTest`（8）。

### 2.7 账户选择弹窗与日期弹窗

- `res/layout/item_yimu_asset_choice.xml` 按 `item_asset_choice.xml` 重建：55dp 行、20dp 图标、名称/备注竖排、右侧 14sp 金额与信用额度行；`YimuAssetDialog` 绑定账户金额，无账户行隐藏金额。
- `YimuDateDialog` 按当前月份设置 CalendarView 高度，与日历页共用 `calendarHeightPx`，四/六行月份不再固定 316.3dp。

### 2.8 统计日常

- 仅补充 `i4.java` 与 fork `BarChartRenderer` 的取证注释，配置未改；首个柱顶数值右缘 1px 残差仍存在，未找到静态成因。

## 3. 设备无关验证

- `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain` → BUILD SUCCESSFUL in 1m 13s。
- 测试报告 `app/build/test-results/testDebugUnitTest/`：29 suite、174 tests、0 failures、0 errors。
- Lint `app/build/reports/lint-results-debug.html`：0 error；1039 条 warning（以 HardcodedText、ContentDescription、UnusedResources 为主）。
- 静态审计：`work/logs/yimu-device-free/static-audit.md`（31 页归并 24 组、576 条差异）、`static-audit.json`、`work/scripts/static_audit.py`。
- 报告：`docs/reports/2026-11-13-device-free-replication.md`。

## 4. 恢复设备后的顺序

1. 重新导出参考账单并导入 validation（数据每天变化），执行 31 页 reference + validation 全量采集与全量 diff。
2. 重点复测：home_top、calendar_month/week、category_manage、budget、rules、asset（含真实账户分组）、theme_appearance（萤火/一木切换）。
3. 夜间主题 29 页采集：主题页切到“萤火”，逐页确认 values-night 与原生页面颜色。
4. 交互复核：主题切换重建是否保留路由、资产分组展开/折叠、规则新增、预算轴标签、日历四/六行月份高度。
5. 用户包构建与安装前备份数据；仪器测试只对 `com.nonen.Bookkeeping.uivalidation` 执行。

## 5. 已知差异与未完成项

- 统计日常首个柱顶数值右缘 1px 抗锯齿差。
- 日期弹窗、账户选择弹窗的系统窗口阴影与 1px 字体差异（2026-10-20 用户决定记录）。
- 我的页头像与主题背景属于用户个人内容；标签管理、使用指南、关于我们仍为本地提示。
- 主题市场除“萤火/一木”外的主题、LOGO 与背景切换仍是占位。
- 预算日预算按“剩余/剩余天数”简化，原版用当天支出修正的分支未实现。
- 资产行图标与备注缺少数据字段（占位图标、remark 恒 GONE）；分组重命名、拖拽排序、折叠动画与展开状态持久化未实现；资产页包含原版排除的 6/7/8/9 类型。
- 会员、云同步、语音记账、股票、借贷、自动记账、桌面小组件按既定范围不接入。

## 6. 环境与命令

- JDK 21 toolchain；Android SDK `D:\Android\Sdk`；Gradle 9.5.0。
- 构建用户包：`.gradlew.bat :app:assembleDebug`（不传 `-PyimuUiValidation=true`）。
- 构建验收包：`.gradlew.bat :app:assembleDebug -PyimuUiValidation=true`。
- 单元测试：`.gradlew.bat :app:testDebugUnitTest`；Lint：`.gradlew.bat :app:lintDebug`。
- 静态审计：`python work/scripts/static_audit.py`。
- 既有截图与对比工具仍在 `work/ui-harness/`，参考截图在 `work/ui-capture/ref/`，validation 截图在 `work/ui-capture/impl/`。

## 8. 真机回归记录（2026-11-13 下午）

- 数据：`work/ui-fixtures/yimu_all_20261113_1659.xls`，2108 条，SHA-256 `4c85d92dc3c1566e68cb1a5c3388d86e9cb1a14b8bb3b7ef8c8cc9b1fa53b9d4`；验收包导入 OK (1 test)。
- 验收 APK：`work/artifacts/yimu-ui-validation.apk`，SHA-256 `487d036a9fa8b8ee267629f601abd5a59f4d2c70bf6bd7c1bf53806e090a23b6`。
- 全量报告：`work/ui-capture/diff/full-20261113-1715.json`。
- 通过 24 页：home_top、home_scrolled、home_drawer、account_book_popup、记账六场景、asset、stats_month、stats_year、stats_self、my、search、settings、periodic、savings_goal、budget、import_export、backup、theme_appearance、fast_add。
- 未通过 7 页：calendar_month 1.9574%（回归）、calendar_week 0.385%（回归）、stats_normal 5.3101%（回归）、category_manage 0.0193%/tile 11.5%、rules 29.94%（数据缺失）、add_bill_date_dialog 0.55%、add_bill_asset_dialog 0.19%。
- 日历回归根因：删除 `top=y-2f` 与 `mItemHeight-=0.9dp` 后，选中矩形从参考的 y572–799 变成 y575–805；把 mItemHeight 减 3px 正好回到参考位置。恢复这两处补偿，保留主题颜色替换。
- 统计日常回归根因：MainActivity 改为 AppCompatActivity 后，XML 的 CheckBox 被替换为 Material 样式，`过滤占比小于1%的资产` 从两行变三行，下方内容整体下移 52px。应改为 ComponentActivity + `attachBaseContext` 覆盖 uiMode，主题切换时 `recreate()`。
- 分类管理唯一超阈 tile 在 x1072–1130、y518–576（首行右侧编辑/菜单图标），待核对图标资源与尺寸。
- 规则页失败为数据缺失：验收包没有参考侧的自定义规则，需播种一条 `CategoryRuleEntity` 后复测。
- 收工：动画缩放 1.0、前台系统桌面、参考包与验收包停止、无后台任务、java 进程 0。

## 9. 2026-11-14 上午回归修复

- 数据：`work/ui-fixtures/yimu_all_20261114_0914.xls`，2112 条，SHA-256 `c333eed468ccd142b4ad854373734f123173d981b3c500538183e7433986c524`；验收包导入 OK，并播种一条自定义规则（文化教育-AI与订阅 / token）。
- 验收 APK：`work/artifacts/yimu-ui-validation.apk`，SHA-256 `da62c6b78dcb9ba3f0ff5308e61e42c88539c85a19adf75dbae72c2b7cf99497`。
- 全量报告：`work/ui-capture/diff/full-20261114-1022.json`；24 页通过、7 页未通过。
- 修复一：日历恢复 `top=y-2f` 与 `mItemHeight-=0.9dp` 经验补偿（选中矩形回到参考 y572–799），月/周分别降到 0.273%/0.194%。
- 修复二：撤销 AppCompatActivity，改用 `attachBaseContext` 覆盖 uiMode；夜间主题真机验证通过，Activity 重建后仍停留在主题外观页。
- 修复三：统计日常随 AppCompat 撤销恢复平台 CheckBox，降到 0.070%。
- 修复四：分类管理补 `category_type_close` 关闭图标，内容区逐像素一致，仅“收入”标签 1px 字高差（页面 0.019%）。
- 修复五：规则页按一级/二级拆分图标查询，补充 `文化教育/AI与订阅 → lineIcon/ic_campus_education.png`，页面 29.94%→0.071%；参考侧用户后来改过图标，剩一个 64px tile。
- 仍失败 7 页：calendar_month 0.273%/tile 10.03%、calendar_week 0.194%/18.55%、stats_normal 0.070%/8.91%、category_manage 0.019%/11.52%、rules 0.071%/51.64%、add_bill_date_dialog 0.553%/22.36%、add_bill_asset_dialog 0.186%/12.08%。
- 剩余差异性质：前五页均为 1px 级文字抗锯齿或未知用户图标；两个弹窗为系统窗口阴影与字体栅格化，用户已决定记录。
- 收工：动画 1.0、双端应用停止、前台桌面、无后台任务与 java 进程。

## 7. 纪律

- 本轮未执行任何 git 回滚、reset、checkout、stash；未修改用户包数据；未连接设备或模拟器。
