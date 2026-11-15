# 真机回归记录：2026-11-13 下午（数据 2108 条）

## 1. 环境与数据

- 设备：小米 15（95f79212），1200×2670，密度 520，动画缩放采集时 0，收工已恢复 1.0，前台为系统桌面，参考包与验收包均已停止。
- 参考数据重新导出：`work/ui-fixtures/yimu_all_20261113_1659.xls`，2108 条，SHA-256 `4c85d92dc3c1566e68cb1a5c3388d86e9cb1a14b8bb3b7ef8c8cc9b1fa53b9d4`，支出 -4531009 分，收入 3559914 分。
- 与上午 2106 条相比新增两笔：午饭 -15.00、奶茶 -16.72。
- 验收包已用 `YimuReferenceDataInstrumentationTest#prepareReferenceFromActualExport` 导入 2108 条，OK (1 test)；screens.yaml 的 dataset 段已同步。
- 验收 APK：`work/artifacts/yimu-ui-validation.apk`，SHA-256 `487d036a9fa8b8ee267629f601abd5a59f4d2c70bf6bd7c1bf53806e090a23b6`。
- 采集：reference 31 页、validation 31 页全部完成；对比报告 `work/ui-capture/diff/full-20261113-1715.json`。

## 2. 结果：24 页通过，7 页未通过

通过页：home_top 0.019%、home_scrolled 0.0038%、home_drawer、account_book_popup、add_bill 六场景、asset 0.0%、stats_month 0.028%、stats_year 0.0038%、stats_self 0.0106%、my 0.0017%、search 0.0%、settings 0.0365%、periodic 0.0、savings_goal 0.0、budget 0.007%、import_export 0.0、backup 0.0、theme_appearance 0.0215%、fast_add 0.0207%。

未通过页：

| 页面 | 本次 | 上一次（上午） | 判定 |
| --- | --- | --- | --- |
| calendar_month | 1.9574% / tile 58.2% | 0.3951% / 16.1% | 回归 |
| calendar_week | 0.385% / tile 42.4% | 0.2763% / 18.6% | 回归 |
| stats_normal | 5.3101% / tile 95.5% | 0.0874% / 11.9% | 回归 |
| category_manage | 0.0193% / tile 11.5% | 0.0345% / 15.6% | 仍有单 tile |
| rules | 29.9358% | 29.9358% | 数据缺失，未验证 |
| add_bill_date_dialog | 0.5522% | 0.5429% | 已知 1px 字体/阴影 |
| add_bill_asset_dialog | 0.1855% | 0.1855% | 已知系统阴影 |

## 3. 回归根因

### 3.1 日历：原版公式移植在本项目几何下不成立

- 选中矩形像素：参考 y 572–799，复刻 y 575–805；参考高度 227px，复刻 230px。
- 本次按原版源码删除了两处经验补偿：`DayRenderer` 的 `top = y - 2f` 与 `ActivityMonthView.onSizeChanged` 的 `mItemHeight -= 0.9dp`。
- 计算结果：把 `mItemHeight` 减 3px 可使第二行单元格顶部从 575 回到 572、选中矩形底部从 805 回到 799，与参考完全一致。说明本项目 CalendarView 的实际单元格高度比原版多约 3px，经验补偿正是为此存在。
- 结论：恢复 `top = y - 2f`（月视图文字与选中矩形）与 `mItemHeight -= round(0.9×density)`；日号颜色保留主题资源替换。周视图选中矩形本次与参考 bbox 一致（422,462–993,694），差异在文字层，恢复农历 `-1f` 与原有颜色分支后重测。

### 3.2 统计日常：AppCompat 改造改变了 CheckBox 测量

- 差异集中在 `过滤占比小于1%的资产` 一行：参考文字区域 [642,1992][1078,2070]（两行，高 78px），复刻 [642,1999][1078,2116]（三行，高 117px），把下方“预算占比/查看详情”整体下推 52px。
- 触发原因：MainActivity 改为 AppCompatActivity 后，XML 里的 `CheckBox` 被 AppCompat 自动替换为带 Material 按钮样式的控件，占用宽度增加导致多换一行。此前 ComponentActivity 下是平台 CheckBox，参考侧同样是平台样式。
- 结论：夜间模式不能以全局 AppCompat 替换为代价。改为在 MainActivity 复写 `attachBaseContext`，按主题缓存构造 `createConfigurationContext` 覆盖 uiMode，Activity 仍保持 ComponentActivity；主题切换时 `recreate()`。这样原生控件膨胀路径与上午通过时一致。

### 3.3 分类管理：单 tile 残差

- 整体 0.0193%，仅右侧 x 1072–1130、y 518–576 一块 64×64 tile 超阈（11.5%），位置对应第一行分类右侧的编辑/菜单图标区域。
- 需要下一次对比该图标的资源、尺寸与点击态；文字 fakeBold 改动已使整体比例从 0.0345% 降到 0.0193%，方向正确。

### 3.4 规则页：验收包没有参考侧那条自定义规则

- 参考侧列表有一条自定义智能分类（类别“文化教育-AI与订阅”，若干关键词），验收包规则表为空，页面处于空态，整页 29.9% 差异属数据缺失。
- 下一次用 instrumentation 向验收包播种一条 `CategoryRuleEntity`（isCustom=true）后重采；行布局本身已按 `item_ai_type.xml` 实现，尚未获得像素证据。

## 4. 本日下午的收获

1. 31 页全量真机链路重新跑通：导出 → 导入 → 双端采集 → 全量 diff，数据身份 2108 条。
2. 上午/本轮新增的资产分组、预算进度与日均、账户选择行、日期弹窗高度、规则页原生化都进入了验收包；asset、budget、my、theme_appearance、settings 等页仍通过。
3. 明确了两个“看似正确、实测回归”的改动：日历原版公式与 AppCompat 全局替换。对应修复方向已经量化，不需要再猜。
4. 明确 rules 页的失败是数据而不是布局，下一轮播种即可验证。

## 5. 收工状态

- 设备动画缩放已恢复 1.0；参考包与验收包已停止；前台系统桌面。
- 无运行中的后台任务，Gradle 守护进程已停止，java 进程数为 0。
- 源码保持本日下午状态，尚未提交；下一次先按 3.1 与 3.2 修复，再重采日历、统计日常、分类管理与规则四页。
