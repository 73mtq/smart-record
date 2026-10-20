# 一木记账 1:1 界面复刻实施计划（修订版）

> 本文件是一木记账界面复刻的正式实施计划，已覆盖旧版计划。当前进入执行阶段，按阶段 0 至 9 推进；真机参考基线未完成时暂停界面代码修改。

## 交付物

- 本文件包含审查意见、已核实证据、范围、架构、阶段与验证方案。
- 执行从阶段 0 开始，先完成静态取证、真机参考与数据基线，再进入主题、外壳与各页面改造。
- 静态证据记录在 `work/ui-evidence/`，截图与对比结果记录在 `work/ui-capture/`。

## 1. 背景

目标是把 `smart-record` 的界面替换为一木记账 `com.wangc.bill` 6.1.8（versionCode 378）的视觉与交互，同时保留当前项目的本地数据层、Room 数据库、仓库与 ViewModel。参考安装包位于 `D:\github-repository\smart-record\reference_apk\base.apk`，SHA-256 为 `e5ec945aa290c6f66fcd43c3f4307eebc5c7796d188e41895dcdf7fed599f8fd`。

已确认的范围：五个主界面、记账页，以及当前已实现的全部二级页面。原版中的会员、云同步、语音记账、股票、借贷、自动记账、桌面微件等功能不在本次范围。用户将连接小米 15 作为原版运行参考。当前本机的 `Medium_Phone_API_36.1` 模拟器为 x86_64，安装包只有 arm64-v8a 原生库，不能作为可靠的原版运行环境。

附件中的旧计划存在多处与安装包资源不一致的断言。本计划以安装包资源、反编译代码、原版真机截图三类证据为准，未取证的数值不写入实现。

## 2. 旧计划审查与已核实修正

### 2.1 审查意见

- 旧计划把未取证的视觉数值写成“严格提取自参考安装包”，其中支出色、浅背景、次要文字色、分割线等与资源表不一致。
- 旧计划的底栏结构与安装包不一致：实际为“首页、日历、资产、统计、我的”五项，没有居中的凸起记账键。
- 旧计划的键盘结构与 `layout_number_input` 不一致：实际包含退格、加、减、乘、除、再记、小数点与取消，没有动态“完成/=”键。
- 旧计划遗漏首页侧栏、真实工具栏、下拉刷新与空状态中提到的右下方快速记账入口。
- 旧计划的统计周期、图表类型与资产文案不准确；实际为“日常、月统计、年统计、自定义”，使用 MPAndroidChart 的柱状图、折线图与饼图。
- 旧计划的日历忽略了 CalendarView 3.7.1、农历文字与自定义月视图、周视图。
- 旧计划没有原版运行参考、数据一致性检查、截图对比、证据文件与停止条件。
- 旧计划没有区分离线不可用功能，会复制会员、云同步、语音等当前无法工作的入口；用户已明确不需要这些功能。
- 旧计划没有处理安装包只有 arm64-v8a 原生库、现有 x86_64 模拟器无法可靠运行原版的问题。
- 旧计划的键盘示意图使用了 ASCII 表格，阅读困难；本计划改用资源表、布局证据与标准表格。

### 2.2 已核实的修正

| 旧计划断言 | 安装包证据 | 修订后的做法 |
| --- | --- | --- |
| 底栏五项为“明细、日历、资产、统计、我的”，中间有凸起记账键 | `activity_main` 使用 `BottomNavigationView`，菜单 `menu_navigation_selector` 五项为“首页、日历、资产、统计、我的”，布局中没有中间凸起键 | 底栏五项等宽排列；悬浮记账球与工具栏 `btn_add_bill` 按原版显示条件实现 |
| 4×4 键盘只有加减和动态“完成/=”键 | `layout_number_input` 高 220dp，四行四列：`1 2 3 退格`、`4 5 6 [+|-]`、`7 8 9 [×|÷]`、`再记 0 . 取消`；`NumberKeyboardView.e()` 使取消键在空输入时为“取消”、有效金额时为“完成”、表达式含运算符时为“=” | 按真实键位与动态状态实现；再记保存后保留分类与账户，清空金额、备注与标签 |
| 主色 `#F55C5C`、背景 `#F7F8FA`、次要文字 `#9CA3AF`、分割线 `#E5E7EB` | 资源表中支出色为 `moneyPay #FFFB5655`，浅背景为 `backgroundLight #FFF4F4F4`，主色与收入色为 `#FF48AB93`，深色主色为 `#FF118C7B` | 全部改用资源表中的真实色值；旧计划中的未取证数值删除 |
| 统计周期为“周、月、年、自定义” | `fragment_statistics` 顶部标签为“日常、月统计、年统计、自定义”，四个子页面使用 `BarChart`、`LineChart`、`PieChart` | 按四个真实标签重构；图表使用原版同款 MPAndroidChart |
| 资产页展示“总资产、总负债” | `fragment_asset` 的文字为“净资产、总资产、负资产” | 按真实文案与分组重构，数据从现有 `AssetRepository` 计算 |
| 日历为手绘 7 列网格 | `fragment_calendar` 使用 `com.haibin.calendarview.CalendarView`，带农历文字颜色属性，并指定自定义 `ActivityMonthView`、`CustomWeekView` | 使用同版本 CalendarView 3.7.1，并移植两个自定义视图；保留农历显示 |
| 首页为月度看板加流水，无侧栏 | `fragment_home` 包含 280dp 的模板抽屉、`JellyRefreshLayout`、工具栏与空状态；工具栏有 `btn_add_bill`、`btn_search`、`btn_more`；账本切换由工具栏账本区域弹出 | 首页增加模板抽屉、账本选择弹窗、下拉刷新、真实工具栏与列表项布局 |
| 我的页面为功能网格加设置列表 | `fragment_my` 包含头像、记账天数、会员卡片与本地入口；当前账号为会员，真机截图不显示会员卡片 | 保留头像、记账天数与本地功能入口；不实现会员购买流程 |
| 记账页是普通表单 | `activity_add_bill` 使用 `TabLayout`（支出/收入/转账）、`ViewPager2`、分类 `RecyclerView`、`MyEditText`、属性胶囊行和 `NumberKeyboardView` | 按 `activity_add_bill` 重写记账页；当前应用没有的报销、优惠、手续费、还款日期、附件等胶囊隐藏 |

已核实的关键资源：

| 内容 | 资源名 | 文件 |
| --- | --- | --- |
| 主界面 | `layout/activity_main` | `res/v9.xml` |
| 底栏菜单 | `menu/menu_navigation_selector` | `res/MV.xml` |
| 首页 | `layout/fragment_home` | `res/Br.xml` |
| 日历 | `layout/fragment_calendar` | `res/rz.xml` |
| 资产 | `layout/fragment_asset` | `res/Gu.xml` |
| 统计 | `layout/fragment_statistics` | `res/2x.xml` |
| 统计日常 | `layout/fragment_statistics_normal` | `res/w6.xml` |
| 统计月 | `layout/fragment_statistics_month` | `res/On.xml` |
| 统计年 | `layout/fragment_statistics_year` | `res/MG.xml` |
| 统计自定义 | `layout/fragment_statistics_self` | `res/Kt.xml` |
| 我的 | `layout/fragment_my` | `res/gh.xml` |
| 记账页 | `layout/activity_add_bill` | `res/k-.xml` |
| 数字键盘 | `layout/layout_number_input` | `res/aj.xml` |
| 数字字体 | `font/roboto_medium_numbers` | `res/RV.ttf` |
| Roboto Medium 字体 | `font/robotomedium` | `res/wA.ttf` |

其他已核实项：

- 底栏默认不显示标签文字，图标 26dp，底栏背景为纯白色，五项不包含记账项；开启“导航文字”设置后标签为 11sp。
- 主色 `#FF48AB93`，深色主色 `#FF118C7B`，收入 `#FF48AB93`，支出 `#FFFB5655`，浅背景 `#FFF4F4F4`，白色 `#FFFFFFFF`，夜间背景 `#FF151C24`。
- 原版 Material Components 版本为 1.10.0，AppCompat 1.6.1，RecyclerView 1.3.0。
- 原版日历库 `com.haibin:calendarview:3.7.1` 可从现有阿里云 `public` 仓库获取；本地已验证该构件的类包名为 `com.haibin.calendarview`。
- 原版统计图表为 MPAndroidChart，包含 `BarChart`、`LineChart`、`PieChart`；安装包没有版本常量，反编译后的类结构与 JitPack `v3.1.0` 相符，使用该版本。
- 当前应用已有 `BookEntity`、`BookDao`、`TagEntity`、`TagDao`，交易表已有 `bookId`、`assetId`、`tag` 字段；具备账本筛选、标签统计和资产统计的数据基础。

## 3. 范围

### 3.1 本次范围

- 五个主界面：首页、日历、资产、统计、我的。
- 记账页：支出、收入、转账、债务四个标签，前三项完整实现，债务为占位页；四列分类网格、二级分类、属性胶囊、数字键盘。
- 首页模板抽屉、账本选择弹窗、下拉刷新、日期分组流水、空状态、悬浮记账球与快速记账入口。
- 当前已有的二级页面：搜索、分类管理、预算、周期记账、记账模板、存钱愿望、资产管理弹窗、分类规则、设置、导入导出、全量备份。
- 浅色与深色主题、系统栏样式、应用锁界面。
- 原版同款字体、图标、菜单、颜色、尺寸与动效中能够从安装包取证的部分。

### 3.2 不纳入本次范围

- 会员、登录、云同步、共享账本、成员收支。
- 语音记账、语音浮层、语音长按。

上述功能不接入网络、账号与业务逻辑；原版界面中的对应入口保留视觉占位，点击时提示本地版本不支持。
- 股票、基金、借贷、报销、分期、信用卡账单等当前数据层未实现的业务。
- 自动记账、无障碍服务、通知监听、桌面微件。
- 多币种与外币折算、图片附件、优惠、手续费。
- 自定义主题市场、会员主题背景。

“我的”页面不实现会员购买流程；当前账号下原版也不显示会员卡片。记账页保留属性胶囊视觉，未接入项点击提示本地版本不支持。参考截图中分类管理的分享中心与图标推广条需要单独屏蔽，并在报告中写明原因。凡因范围排除造成的可见差异，不使用掩膜掩盖布局或样式错误。

## 4. 执行原则

- 先完成静态取证与真机参考采集，再改动界面代码。未取证的尺寸、颜色、文案、交互不写入实现。
- 不使用 mock、伪造截图或只求通过测试的替代数据。界面验证使用真实安装包、真实 Room 数据库与同一台真机。
- 所有中间文件放在 `work/` 下，`work/` 已在 `.gitignore` 中。禁止读写系统 Temp 目录；为 Java、Node、adb、apktool 设置工作区临时目录。
- 不使用 Git 回滚、`git reset`、`git checkout`、`git stash`。当前工作区存在未提交改动，编辑前先看 `git status`，只修改本计划涉及的文件。
- 任何工具或设备阻塞都停止并报告原因，不使用猜测值继续。

## 5. 证据与参考采集

### 5.1 环境准备

工作目录：

```text
work/deps/                  apktool 与包装脚本
work/tmp/                   工具临时目录
work/ui-evidence/           aapt2 / JADX / apktool 输出
work/ui-fixtures/           原版导出的 Excel 与校验记录
work/ui-capture/ref/        原版截图与 UI 层级
work/ui-capture/impl/       复刻版截图与 UI 层级
work/ui-capture/diff/       差异图与报告
work/ui-harness/            截图、导航、对比脚本与掩膜
```

环境变量：

```bash
export JAVA_HOME="D:\Program Files\Java\jdk-24"
export REA_JADX_MCP_JAR="C:\Users\29602\.local\share\rea\android\jadx-headless-mcp-0.7.1-all.jar"
export REA_ADB_PATH="D:\Android\Sdk\platform-tools\adb.exe"
export REA_APKTOOL_COMMAND="D:\github-repository\smart-record\work\deps\apktool.cmd"
export TEMP="D:\github-repository\smart-record\work\tmp"
export TMP="D:\github-repository\smart-record\work\tmp"
export TMPDIR="D:\github-repository\smart-record\work\tmp"
export JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=D:/github-repository/smart-record/work/tmp"
```

工具准备：

- Apktool 3.0.3：从 `https://github.com/iBotPeaches/Apktool/releases/download/v3.0.3/apktool_3.0.3.jar` 下载到 `work/deps/apktool.jar`，用 Write 工具创建 `work/deps/apktool.cmd`，内容为 `@java -jar "%~dp0apktool.jar" %*`。
- REA 使用固定版本 6.7.0：`npx -y rea-agents@6.7.0 ...`，避免版本漂移。
- Android SDK 的 `aapt2.exe` 使用 `D:\Android\Sdk\build-tools\37.0.0\aapt2.exe`。
- Python 3.12.2 与 Pillow 12.2.0 已安装，用于截图对比。

### 5.2 静态取证

资源表与布局：

```bash
AAPT="D:/Android/Sdk/build-tools/37.0.0/aapt2.exe"
"$AAPT" dump badging reference_apk/base.apk
"$AAPT" dump resources reference_apk/base.apk > work/ui-evidence/resources.txt
"$AAPT" dump xmltree --file res/v9.xml reference_apk/base.apk
"$AAPT" dump xmltree --file res/Br.xml reference_apk/base.apk
"$AAPT" dump xmltree --file res/rz.xml reference_apk/base.apk
"$AAPT" dump xmltree --file res/Gu.xml reference_apk/base.apk
"$AAPT" dump xmltree --file res/2x.xml reference_apk/base.apk
"$AAPT" dump xmltree --file res/gh.xml reference_apk/base.apk
"$AAPT" dump xmltree --file res/k-.xml reference_apk/base.apk
"$AAPT" dump xmltree --file res/aj.xml reference_apk/base.apk
```

用 Apktool 解码完整资源与清单：

```bash
"$JAVA_HOME/bin/java" -jar work/deps/apktool.jar d -f -s reference_apk/base.apk -o work/ui-evidence/apktool
```

只复制本次范围内的资源。解码后的资源名与文件名的对应关系以资源表为准，不按 `res/-0.xml`、`res/Br.xml` 这类混淆名猜测用途。

代码反编译：

```bash
npx -y rea-agents@6.7.0 inspect-android-package reference_apk/base.apk
npx -y rea-agents@6.7.0 search-android-classes reference_apk/base.apk NumberKeyboardView
npx -y rea-agents@6.7.0 inspect-android-class reference_apk/base.apk com.wangc.bill.view.NumberKeyboardView
npx -y rea-agents@6.7.0 inspect-android-method reference_apk/base.apk com.wangc.bill.view.NumberKeyboardView <method> --overload-index 0
npx -y rea-agents@6.7.0 trace-android-references reference_apk/base.apk com.wangc.bill.view.NumberKeyboardView
```

优先提取以下类的行为：

- `com.wangc.bill.activity.MainActivity`
- 五个 Fragment：`HomeFragment`、`CalendarFragment`、`AssetFragment`、`StatisticsFragment`、`MyFragment`
- `AddBillActivity`、`AddBillActivity` 使用的分类适配器、金额显示与保存逻辑
- `com.wangc.bill.view.NumberKeyboardView`、`MyEditText`、`CategoryCoverView`
- `com.wangc.bill.calendar.ActivityMonthView`、`CustomWeekView`
- 统计子页面与图表配置代码
- 一级、二级分类选择弹窗、账户选择、日期时间选择、标签与备注交互
- 首页工具栏、侧栏、空状态、`btn_add_bill` 的点击与长按行为
- `BookDao` 与账本切换相关的原版行为

证据记录要求：

- 每条进入实现的数值都要记录来源：资源名、布局文件、类名与方法、或截图坐标。
- 未确认的行为写成“待确认”，不先填入实现。
- 用一个 `work/ui-evidence/spec.md` 记录屏幕清单、组件尺寸、颜色、字体、文案、交互与未知项。该文件只保存证据与结论，不保存推测。

### 5.3 真机参考与数据

设备要求：

- `adb devices` 必须显示小米 15；未连接时停止，不使用 x86_64 模拟器运行原版。
- 不清理、不覆盖原版应用数据。原版已安装时保持原样；未安装时先征得用户同意再安装 `base.apk`。
- 原版与复刻版可以同时安装，包名不同。

数据准备：

- 由用户在原版中导出一份 Excel（选择单个账本、固定日期范围），放到 `work/ui-fixtures/`。不修改原版数据。
- 用当前项目的 Yimu Excel 导入功能把同一份数据导入复刻版；核对原版与复刻版在首页、日历、资产、统计中的总额、笔数、分类名称与账户名称。
- 数据不一致时先修正导入映射或分类映射，再进入截图对比。禁止用掩膜掩盖数据错误。

环境一致：

- 记录并统一屏幕分辨率、密度、字体缩放（1.0）、语言（简体中文）、时区、深浅模式、动画缩放（关闭）、自动旋转（关闭）、状态栏与导航栏模式。
- 原版与复刻版使用同一个启动入口、同一时间、同一账本和同一批数据。
- 截图前等待页面稳定；连续两次截图在屏蔽动态区域后一致才保存。

导航与截图：

- 使用 `uiautomator dump` 读取控件文本与 bounds，按文本或 resource-id 定位点击坐标，再执行 `adb shell input tap` 或 `swipe`。
- 需要交互的页面包括：五个主标签、模板抽屉、账本弹窗、首页列表滚动、记账页三种类型、分类与二级分类、键盘各状态、日历月/周切换与日期选择、资产分组与隐藏、统计四个标签与各图表、我的页面与设置、全部二级页面。
- 截图命令：

```bash
adb -s <serial> exec-out screencap -p > work/ui-capture/ref/<screen>.png
adb -s <serial> shell uiautomator dump /sdcard/window.xml
adb -s <serial> pull /sdcard/window.xml work/ui-capture/ref/<screen>.xml
```

- 每个截图记录设备序列号、分辨率、密度、字体缩放、应用版本、APK 哈希、数据夹具哈希与采集时间。任一条件变化时拒绝比较。
- 原版中的分享中心、图标推广条等排除区域单独记录掩膜与原因；掩膜只覆盖范围排除项与系统栏，不覆盖正常界面。

## 6. 架构与依赖

### 6.1 总体方案

界面外壳与大多数屏幕使用 Compose 重构；原版已有同款库的复杂组件用 `AndroidView` 承载，保证绘制、字体、点击反馈与图表外观一致。

| 组件 | 实现方式 | 原因 |
| --- | --- | --- |
| 底栏 | `BottomNavigationView` + `AndroidView` | 原版使用 Material Components 1.10.0；直接复用菜单、图标与样式 |
| 日历 | `com.haibin.calendarview.CalendarView` 3.7.1 + `AndroidView` | 原版同款库，包含农历与月/周模式；移植自定义月视图与周视图 |
| 统计图表 | MPAndroidChart `BarChart`、`LineChart`、`PieChart` + `AndroidView` | 原版同款图表库；复用图表配置代码 |
| 首页工具栏、侧栏、列表 | Compose | 布局可控，数据来自现有 ViewModel |
| 记账页、分类网格、键盘 | Compose + 纯 Kotlin 键盘状态机 | 便于测试输入逻辑，视觉按 `activity_add_bill` 与 `layout_number_input` 还原 |
| 其他二级页面 | Compose | 复用现有 ViewModel 与路由，按原版布局替换样式 |

`AndroidView` 只承担绘制与手势，不把业务逻辑放进 View 层。底栏选中状态、日历日期、图表数据都由 Compose 状态与 ViewModel 提供。

### 6.2 依赖修改

`settings.gradle.kts` 的 `dependencyResolutionManagement` 增加 JitPack：

```kotlin
maven("https://jitpack.io")
```

现有阿里云 `public` 仓库已包含 `com.haibin:calendarview:3.7.1`，无需新增仓库。

`app/build.gradle.kts` 增加：

```kotlin
implementation("com.google.android.material:material:1.10.0")
implementation("com.haibin:calendarview:3.7.1")
implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
```

`gradle/libs.versions.toml` 增加对应版本声明。原版 `BuildConfig` 没有版本字段，反编译类结构与 JitPack `v3.1.0` 相符，使用 `v3.1.0`。Material 1.10.0、AppCompat 1.6.1、RecyclerView 1.3.0 以 APK 的 META-INF 版本标记为准。

### 6.3 主题与资源

- 新增 `ui/theme/YimuTokens.kt`，保存已核实的颜色、字体、字号、尺寸与圆角；未取证的数值从布局与资源中读取后再加入。
- `Color.kt` 的主色改为 `#48AB93`，支出色改为 `#FB5655`，浅背景改为 `#F4F4F4`，夜间背景改为 `#151C24`。
- 关闭动态取色，删除或隐藏动态取色开关；界面颜色固定为一木主题，避免 Android 12 以上的系统取色覆盖。
- 复制 `robotomedium`、`roboto_medium_numbers` 字体到 `res/font/`，金额、键盘与统计数字使用对应字体。
- 字体样式、行高、字重从原版布局的 `textSize`、`textStyle`、`fontFamily` 与样式资源读取。
- 从安装包复制本次范围内的 PNG、WebP、矢量图、菜单图标、切换图标与动画资源。原版资源只用于本地个人复刻，不对外分发原版资源文件。
- 系统栏颜色、状态栏图标明暗、导航栏颜色与边到边行为从原版主题、清单和真机截图取证后设置。原版与复刻版使用相同的系统栏模式后再比较内容区域。

### 6.4 数据与 ViewModel 复用

直接复用：

- `TransactionRepository`、`ReportRepository`、`AssetRepository`、`BudgetRepository`、`PeriodicRepository`、`TemplateRepository`、`SavingsGoalRepository`、`RuleRepository`。
- `HomeViewModel`、`CalendarViewModel`、`AssetManageViewModel`、`StatsViewModel`、`SettingsViewModel`、`AddEditViewModel` 与各二级页面 ViewModel。
- `StatsCalculator`、`Categories`、`RuleEngine`、`Format.kt`。
- `AppContainer` 中的 `bookDao`、`tagDao`、`assetDao`、`budgetDao`、`cycleDao`、`moduleBillDao`、`dreamDao`。

需要补充的能力：

- 首页按 `bookId` 过滤交易。DAO 已有 `observeFilteredRange` 的账本参数；为首页增加按账本与月份读取的查询或在仓库层包装，配套 JVM 测试。
- 记账页转账模式。复用 `AssetRepository.executeTransfer` 与 `TransferEntity`，在 `AddEditViewModel` 增加支出、收入、转账三种状态与保存路径。
- 统计页面需要但当前尚未聚合的数据：标签统计、预算统计、资产走势的日/月粒度、日历日汇总。优先复用现有 `ReportRepository` 与 `StatsCalculator`；缺少的查询在仓库层增加，不修改 Room 表结构。
- 账本切换状态：`HomeViewModel` 增加当前账本 `StateFlow`，侧栏切换后首页、日历、资产、统计的阅读范围随之更新。当前应用已有 `BookEntity` 与 `BookDao`，不新建账本表。

## 7. 页面映射

### 7.1 主界面

| 原版 | 当前文件 | 改造内容 |
| --- | --- | --- |
| `activity_main` + `menu_navigation_selector` | `ui/screens/MainScreen.kt` | 三页 Pager 改为五页 Pager；自定义胶囊底栏替换为原版 `BottomNavigationView`，默认只显示图标；恢复原版悬浮记账球的显示与单击记账 |
| `fragment_home` | `HomeScreen.kt`、`HomeComponents.kt`，新增 `HomeToolbar.kt`、`HomeDrawer.kt`、`TransactionItem.kt` | 工具栏、280dp 模板抽屉、账本选择弹窗、日期分组列表、空状态、下拉刷新、悬浮记账球；列表项按 `item_bill_*` 布局还原 |
| `fragment_calendar` | `CalendarScreen.kt`，新增 `ui/calendar/YimuCalendarView.kt`、`ActivityMonthView.kt`、`CustomWeekView.kt` | `CalendarView` 3.7.1 承载月/周视图；移植日期金额、圆点、选中态与农历显示；下方单日列表复用现有查询 |
| `fragment_asset` | 新增 `AssetTabScreen.kt`，复用 `AssetManageViewModel` | 净资产、总资产、负资产；资金、信用、投资、借贷分组；隐藏、校准、转账、新增与编辑弹窗 |
| `fragment_statistics` 与四个子布局 | `StatisticsScreen.kt`、`StatsComponents.kt`，新增 `StatsCharts.kt` | 顶部改为日常/月统计/年统计/自定义；移植 BarChart、LineChart、PieChart 配置；按现有数据实现收支、资产走势、分类占比、标签占比、预算与报表区块 |
| `fragment_my` | 新增 `MyScreen.kt`；现有 `SettingsScreen.kt` 改为独立设置页 | 头像、记账天数、累计笔数、本地功能入口；当前账号下原版不显示会员卡片；设置、导入导出、备份从设置页进入 |
| `activity_add_bill` + `layout_number_input` | `AddEditScreen.kt`，新增 `NumberKeyboard.kt`、`TransferEditor.kt` | 四个标签（债务为占位）、四列分类网格、二级分类、属性胶囊、金额显示、四行四列键盘、保存与再记 |

### 7.2 二级页面

| 当前页面 | 原版布局 | 改造内容 |
| --- | --- | --- |
| 搜索 | `activity_bill_search` | 搜索框、筛选栏、结果列表按原版尺寸与字体还原 |
| 分类管理 | `activity_add_category`、`activity_add_child_category`、`item_category_icon_*` | 一级/二级分类列表、图标选择、排序、隐藏与删除 |
| 预算 | `activity_budget_setting`、`activity_budget_manager`、`dialog_choice_month_budget` | 总预算、分类预算、进度与超支状态 |
| 周期记账 | `activity_cycle_manager`、`activity_cycle_set_simple`、`activity_cycle_bill_info` | 周期规则列表、启停、新增与编辑 |
| 记账模板 | `activity_template` | 模板列表、应用、新增与删除 |
| 存钱愿望 | `activity_dream_list`、`activity_add_dream`、`activity_dream_log` | 目标卡片、进度、存入与取出 |
| 资产弹窗 | `activity_add_capital`、`activity_add_credit_card`、`activity_add_instalment` 等 | 按当前资产类型保留可用弹窗；未实现的类型保留卡片视觉，数值为 0，点击提示本地版本不支持 |
| 分类规则 | `activity_ai_type_edit` 或相同语义的规则页面 | 关键词规则、商户记忆、测试与推荐区域 |
| 设置 | `activity_setting` | 主题、显示、安全、导入、导出、备份、关于 |
| 导入导出与备份 | `activity_import_*`、`activity_export_*`、`activity_backup_*` | 按当前已实现的导入导出流程替换界面，保留现有解析器与备份服务 |

### 7.3 关键组件规格

底栏：

- 使用 `BottomNavigationView`，菜单五项“首页、日历、资产、统计、我的”，选中项与 Pager 双向同步。
- 图标 26dp，默认不显示标签，纯白背景；开启“导航文字”设置后标签 11sp；选中色与未选中色从原版样式与 selector 读取。
- 高度、内边距、导航栏 inset 以真机 UI 层级中的 `bottom_nav_view` bounds 为准。

首页：

- 工具栏包含账本名称、`btn_add_bill`、`btn_search`、`btn_more`；按钮尺寸与间距从 `fragment_home` 读取。
- 280dp 抽屉是模板抽屉，内容为模板列表；账本切换由工具栏账本区域弹出，列表来自 `BookDao`，切换后刷新首页与其它页面的数据范围。
- 列表项按原版 `item_bill_*` 布局还原；分类图标使用安装包内的资源；分类名称、金额、账户、时间、备注的排列与颜色从布局与真机截图读取。
- 下拉刷新使用 Compose 实现，指示器颜色与位置按原版取证；如果需要完全一致的回弹物理效果，再移植 `JellyRefreshLayout` 的绘制逻辑。
- 长按多选工具栏 `layout_bill_edit` 作为首页交互的一部分，在默认截图中不可见；本次保留“统计、导出、编辑、删除”等当前应用支持的动作，隐藏“组合、报销”。

日历：

- `CalendarView` 的 `calendar_padding`、农历文字颜色、选中文字颜色、月视图/周视图类名从 `fragment_calendar` 读取。
- `ActivityMonthView` 与 `CustomWeekView` 负责日期下方的金额、圆点与选中态；行为通过 JADX 读取后移植，不自行设计。
- 月/周切换、左右滑动、年月选择、单日列表联动、右下角快速记账入口按原版实现。

统计：

- 顶部四个标签“日常、月统计、年统计、自定义”，标签指示器与文字样式从 `fragment_statistics` 读取。
- 日常：近 7 日收支柱状图、收支汇总、资产占比、预算占比、标签数据。
- 月统计：月支出、月收入、月结余、收支柱状图、资产走势折线图、分类占比、标签占比、报表统计。
- 年统计：年支出、年收入、年结余、12 个月收支柱状图、资产走势、分类占比、标签占比、报表统计。
- 自定义：起止日期、总支出、总收入、总结余、收支走势、分类占比、标签占比。
- 成员收支、共享账本相关区块不属于本次范围，保留布局位置并显示空状态，报告中记录。

记账页：

- 顶部 `TabLayout` 有“支出、收入、转账、债务”四项；债务为占位页，显示本地版本不支持的提示。下方 `ViewPager2` 承载分类与属性区域。
- 分类网格列数、图标尺寸、文字大小、二级分类浮层从原版布局与适配器读取。
- 金额显示区包含 `¥`、当前金额、计算表达式；原版中的外币近似金额在不支持多币种时隐藏。
- 键盘四行四列，键高、间距、背景、按下态、字体从 `layout_number_input` 与 `NumberKeyboardView` 读取；取消键在空输入时为“取消”，有效金额时为“完成”，表达式含运算符时为“=”；`再记` 保存后保留分类与账户，清空金额、备注与标签。
- 属性胶囊按原版保留日期、账户、报销、附件、优惠、属性、还款日期、手续费；账户、日期、备注、标签可用，未接入项点击提示本地版本不支持。
- 转账模式复用现有资产转账逻辑，包含转出账户、转入账户、金额、日期、备注与手续费（如果数据层支持）。

## 8. 实施阶段

### 阶段 0：证据与参考基线

- 完成 Apktool 解码、REA 反编译、资源表导出、屏幕清单与证据文件。
- 完成原版 Excel 导出、复刻版导入与总额核对。
- 完成截图与 UI 层级基线，建立 `work/ui-harness/screens.yaml` 导航清单与掩膜文件。
- 用本计划替换 `docs/plans/yimu_exact_ui_reconstruction_plan.md` 中的旧内容，删除未取证数值与错误结构，写入已核实的规格与证据索引。
- 核对 Material、CalendarView、MPAndroidChart 版本；核对键盘行为、转账流程、分类列数、列表项间距等未确定项。
- 本阶段不改界面代码。以下任一条件不满足时停止：小米 15 未连接、原版无法运行、原版数据无法导出、JADX/Apktool 失败、关键组件行为无法取证。

验证：证据文件齐全；每个本次范围内的页面都有布局文件、关键尺寸、颜色与文案；截图基线可重复采集。

### 阶段 1：主题、字体与资源

- 修改 `settings.gradle.kts`、`app/build.gradle.kts`、`gradle/libs.versions.toml`。
- 新增 `ui/theme/YimuTokens.kt`；更新 `Color.kt`、`Theme.kt`、`Type.kt`。
- 复制字体、图标、菜单、selector、尺寸与样式资源；新增 `res/menu/menu_navigation_selector.xml`。
- 关闭动态取色；调整系统栏与边到边行为。

验证：`:app:assembleDebug`、`:app:testDebugUnitTest`、`:app:lintDebug` 通过；首页在真机上的背景、主色、字体与参考截图的静态区域一致。

### 阶段 2：五标签外壳与路由

- 重写 `MainScreen.kt`：五页 Pager、原版底栏、底栏与 Pager 同步。
- 更新 `AppNavHost.kt`：主页面不再承载三个旧标签；日历、资产保留快捷入口并切换到对应标签。
- 更新 `MainActivity.kt`：快捷方式目标改为标签选择；应用锁界面按原版样式调整。
- 新增 `HomeDrawer.kt` 与账本选择弹窗状态；更新 `HomeViewModel` 的账本筛选。

验证：五个标签可点击与滑动切换；模板抽屉打开与账本弹窗切换生效；快捷方式进入正确标签；底栏 bounds、图标、文字与参考截图一致。

### 阶段 3：首页

- 新增 `HomeToolbar.kt`、`TransactionItem.kt`；重构 `HomeScreen.kt` 与 `HomeComponents.kt`。
- 还原工具栏、日期分组、空状态、下拉刷新、悬浮记账球与工具栏记账按钮。
- 移植 `item_bill_*` 布局与分类图标映射；实现多选工具栏中当前支持的动作。

验证：首页顶部视图、列表项、空状态与参考截图一致；账本切换、搜索、记账入口、长按多选可用；列表滚动稳定。

### 阶段 4：记账页与键盘

- 重写 `AddEditScreen.kt`，新增 `NumberKeyboard.kt`、`TransferEditor.kt`。
- 扩展 `AddEditViewModel`：支出、收入、转账、债务四个标签，债务为占位；金额表达式状态机；分类、账户、日期、备注、标签。
- 实现二级分类浮层、保存、再记、取消与编辑既有账单。

验证：JVM 键盘状态测试通过；真机完成支出、收入、转账保存；键盘键位、金额格式、按钮按下态与参考截图一致；再记后状态保留正确。

### 阶段 5：日历

- 新增 `YimuCalendarView.kt`、`ActivityMonthView.kt`、`CustomWeekView.kt`；重构 `CalendarScreen.kt`。
- 接入 `CalendarViewModel` 的月份与单日查询；还原农历、金额、圆点、选中态、月/周切换。
- 还原年月选择、单日列表与右下角快速记账。

验证：月/周切换、跨月滑动、日期选择、单日列表、农历显示与参考截图一致；空日期状态一致。

### 阶段 6：资产

- 新增 `AssetTabScreen.kt`，复用 `AssetManageViewModel` 与现有弹窗逻辑。
- 还原净资产、总资产、负资产、分组、隐藏模式、校准、转账、新增与编辑。
- 资产分组与原版类型不一致时，按当前数据能力映射；报销、债务、理财卡保留视觉占位，数值为 0，点击提示本地版本不支持。

验证：分组、金额、隐藏、校准、转账与参考截图一致；数据总额与夹具一致；相关单元测试通过。

### 阶段 7：统计

- 新增 `StatsCharts.kt`；重构 `StatisticsScreen.kt`。
- 接入 MPAndroidChart 的 BarChart、LineChart、PieChart；移植原版颜色、坐标轴、图例、点击与空数据状态。
- 实现四个标签的数据聚合；补充标签、预算、资产走势所需的仓库查询。
- 成员与共享账本区块按范围隐藏或显示空状态。

验证：四个标签可切换；图表数据与夹具总额一致；分类下钻、标签切换、收支切换可用；图表静态外观与参考截图一致。

### 阶段 8：我的与二级页面

- 新增 `MyScreen.kt`；把现有 `SettingsScreen.kt` 改为独立设置页；更新 `AppNavHost.kt` 的路由。
- 按原版布局重做搜索、分类管理、预算、周期、模板、存钱愿望、分类规则、设置、导入导出与备份页面。
- 每个页面复用现有 ViewModel 与仓库；只修改界面与必要的交互状态。

验证：全部路由可达；每个页面的主状态与参考截图一致；导入、导出、备份、预算、周期、模板、储蓄与分类管理的行为测试通过。

### 阶段 9：深色主题与整体回归

- 补齐夜间颜色、夜间资源与系统栏样式。
- 检查边到边、键盘与导航栏 inset、字体缩放、旋转锁定与低电量等状态。
- 运行完整截图清单、全部 JVM 测试、Lint、构建与真机交互测试。

验证：深浅两种模式的截图清单全部通过；构建、测试、Lint 通过；没有未记录的界面差异。

## 9. 验证与验收

### 9.1 构建与测试

```bash
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:installDebug
```

新增测试：

- JVM：键盘状态机、金额格式、账本筛选、统计区间映射、主题 token 常量。
- 仪器测试：五标签切换、侧栏、记账保存（支出/收入/转账）、日历选择、二级页面基本流程。测试使用真机与真实 Room 数据库，不使用 mock。

### 9.2 截图对比

工具：`work/ui-harness/capture.py`、`work/ui-harness/diff.py`，使用 Pillow 12.2.0。输出目录为 `work/ui-capture/`。

对比流程：

1. 按 `screens.yaml` 在原版与复刻版上执行同一组点击与滚动，截图并保存 UI 层级。
2. 校验设备序列号、分辨率、密度、字体缩放、语言、主题、APK 哈希与数据夹具哈希。
3. 使用掩膜去除系统状态栏、导航栏、时钟、电量与范围排除区域。
4. 以相同尺寸叠加比较；输出差异图、差异比例、最差 64×64 区块与每个页面的报告。
5. 复刻版安装包变化后重新采集，不复用旧截图。

验收阈值：

- 每个页面的未屏蔽区域差异像素比例不超过 1.5%，单通道容差 8。
- 任意 64×64 区块的差异比例不超过 6%。
- 底栏、工具栏、键盘、分类单元格等关键 bounds 与参考 UI 层级的差异不超过 3px。
- 纯色区域取色与资源值的单通道差异不超过 2。
- 数据型文本在夹具一致时逐字核对；夹具无法一致时，只屏蔽文本区域，并保留布局与颜色比较。
- 任何截图缺失、哈希不一致、分辨率不一致、重复图片或夹具总额不一致都视为失败。

### 9.3 行为验收

- 五个标签可点击与滑动切换，底栏选中态同步。
- 模板抽屉可打开，账本弹窗切换后首页、日历、资产、统计的数据范围一致。
- 记账页支出、收入、转账均可保存；债务占位页显示提示；再记保留分类与账户；编辑与删除可用。
- 键盘的数字、小数点、退格、加减乘除、再记、取消行为与原版一致。
- 日历月/周切换、跨月滑动、日期选择、单日列表、农历显示正常。
- 统计四个标签与图表、分类下钻、收支切换、标签切换正常。
- 我的页面入口与全部二级页面可达；导入、导出、备份与原有功能正常。
- 深浅主题、字体缩放、系统栏 inset 无遮挡或错位。

验证过程中发现的差异必须记录在 `work/ui-capture/report.json` 与页面清单中；修复后重新运行对比，不以人工口头确认代替报告。

## 10. 风险与待核实项

- Compose 文本度量与 TextView 存在差异：使用原版字体、字号、行高与字距，按 UI 层级与截图测量后调整；差异无法消除时记录具体数值并报告。
- CalendarView 的自定义月视图与周视图需要反编译后移植：先确认 3.7.1 的 API 与原版一致，再实现日期金额、圆点与选中态。
- MPAndroidChart 版本需要反编译核对：JitPack 上使用匹配版本；图表配置与颜色从原版代码提取。
- `BottomNavigationView` 需要 Material 主题环境：用 `ContextThemeWrapper` 或调整 Activity 主题，保证图标、文字与点击反馈与原版一致。
- 小米 15 的 MIUI 可能限制截图、安装或辅助功能：遇到限制时停止并报告，不通过清空数据或修改系统设置绕过。
- 原版数据导出可能要求会员或特定版本：无法导出时使用用户认可的夹具，并明确哪些动态文本被屏蔽。
- 原版资源文件名经过混淆：所有资源通过资源表映射，不按文件名猜测。
- 二级页面数量较多：按阶段 8 逐个完成，每个页面先取证再实现，不使用统一模板替代原版布局。
- 范围排除项会产生可见差异：在报告中逐项列出，屏蔽区域只覆盖排除项。
- 当前工作区存在未提交改动：编辑前确认 `git status`，只改本计划涉及的文件，不执行任何 Git 回滚操作。

## 11. 关键文件

新增与修改：

- `app/build.gradle.kts`
- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- `app/src/main/res/menu/menu_navigation_selector.xml`
- `app/src/main/res/font/`
- `app/src/main/res/drawable/`
- `app/src/main/res/values/themes.xml`、`app/src/main/res/values-night/themes.xml`
- `app/src/main/java/com/nonen/Bookkeeping/ui/theme/YimuTokens.kt`
- `app/src/main/java/com/nonen/Bookkeeping/ui/theme/Color.kt`
- `app/src/main/java/com/nonen/Bookkeeping/ui/theme/Theme.kt`
- `app/src/main/java/com/nonen/Bookkeeping/ui/theme/Type.kt`
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/MainScreen.kt`
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/HomeScreen.kt`
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/HomeComponents.kt`
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/HomeToolbar.kt`（新增）
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/HomeDrawer.kt`（新增）
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/TransactionItem.kt`（新增）
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/CalendarScreen.kt`
- `app/src/main/java/com/nonen/Bookkeeping/ui/calendar/YimuCalendarView.kt`（新增）
- `app/src/main/java/com/nonen/Bookkeeping/ui/calendar/ActivityMonthView.kt`（新增）
- `app/src/main/java/com/nonen/Bookkeeping/ui/calendar/CustomWeekView.kt`（新增）
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/AssetTabScreen.kt`（新增）
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/StatisticsScreen.kt`
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/StatsCharts.kt`（新增）
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/MyScreen.kt`（新增）
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/SettingsScreen.kt`
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/AddEditScreen.kt`
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/NumberKeyboard.kt`（新增）
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/TransferEditor.kt`（新增）
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/AddEditViewModel.kt`
- `app/src/main/java/com/nonen/Bookkeeping/ui/screens/HomeScreen.kt` 中的 `HomeViewModel`
- `app/src/main/java/com/nonen/Bookkeeping/ui/AppNavHost.kt`
- `app/src/main/java/com/nonen/Bookkeeping/MainActivity.kt`
- `app/src/main/java/com/nonen/Bookkeeping/data/repo/TransactionRepository.kt`（账本过滤）
- `app/src/test/java/com/nonen/Bookkeeping/...`（键盘、筛选、统计映射测试）
- `app/src/androidTest/java/com/nonen/Bookkeeping/...`（真机导航、记账、日历流程）
- `work/ui-harness/`、`work/ui-evidence/`、`work/ui-fixtures/`、`work/ui-capture/`（均被 `work/` 忽略）
