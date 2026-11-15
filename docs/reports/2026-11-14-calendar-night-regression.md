# 真机回归：2026-11-14 上午（数据 2112 条）

## 1. 环境与数据

- 设备 95f79212，1200×2670，密度 520；采集时动画缩放 0，收工恢复 1.0，前台系统桌面，参考包与验收包已停止。
- 参考数据重新导出：`work/ui-fixtures/yimu_all_20261114_0914.xls`，2112 条，SHA-256 `c333eed468ccd142b4ad854373734f123173d981b3c500538183e7433986c524`，支出 -4534116 分，收入 3559914 分。
- 验收包导入 2112 条 OK；播种一条自定义规则（文化教育-AI与订阅，关键词 token）。
- 验收 APK：`work/artifacts/yimu-ui-validation.apk`，SHA-256 `da62c6b78dcb9ba3f0ff5308e61e42c88539c85a19adf75dbae72c2b7cf99497`。
- 全量报告：`work/ui-capture/diff/full-20261114-1022.json`。

## 2. 结果：24 页通过，7 页未通过

通过页：home 四场景、记账六场景、asset、stats_month、stats_year、stats_self、my、search、settings、periodic、savings_goal、budget、import_export、backup、theme_appearance、fast_add。

未通过页与前一天对比：

| 页面 | 今天 | 昨天 | 变化 |
| --- | --- | --- | --- |
| calendar_month | 0.2731% / tile 10.03% | 1.9574% / 58.2% | 大幅收敛 |
| calendar_week | 0.1941% / tile 18.55% | 0.385% / 42.4% | 收敛 |
| stats_normal | 0.0703% / tile 8.91% | 5.3101% / 95.5% | 回归修复 |
| rules | 0.0711% / tile 51.64% | 29.9358% | 回归修复（图标仍差） |
| category_manage | 0.0193% / tile 11.52% | 0.0193% / 11.52% | 内容和关闭按钮已对齐，仅文字抗锯齿 tile |
| add_bill_date_dialog | 0.5533% / tile 22.36% | 0.5522% | 已知 1px/阴影 |
| add_bill_asset_dialog | 0.1855% / tile 12.08% | 0.1855% | 已知阴影 |

## 3. 本日修复

1. **日历回归**：恢复 `top = y - 2f` 与 `mItemHeight -= 0.9dp` 两处经验补偿，保留主题颜色与动态高度。选中矩形恢复到参考的 y572–799，月视图从 1.96% 降到 0.273%，周视图从 0.385% 降到 0.194%。剩余差异是选中日金额文字的抗锯齿边缘，页面级已远低于阈值。
2. **夜间模式**：撤销 MainActivity 的 AppCompatActivity 改造，改为 `attachBaseContext` 读取主题同步缓存并覆盖 uiMode，模式变化时 `recreate()`。真机验证：“萤火”切换后背景 #151C24、卡片 #212A33；“一木”切回白色；Activity 重建后仍停留在主题外观页，返回栈保留。
3. **统计日常回归**：随 AppCompat 撤销恢复平台 CheckBox，`过滤占比小于1%的资产` 回到两行，页面从 5.31% 降到 0.070%。
4. **分类管理提示条**：按 `activity_type_edit.xml:22-27` 补上 `category_type_close` 关闭图标与点击隐藏，分类页内容区逐像素一致，仅“收入”标签文字 530 px 抗锯齿差。
5. **规则页图标**：按“一级/二级”拆分类别查询图标，并从原版数据库确认 `文化教育/AItoken` 使用 `lineIcon/ic_campus_education.png`，补充 `文化教育/AI与订阅` 映射。规则页从 29.94% 降到 0.071%，但参考侧图标与当前资源仍不一致（用户后来改过图标），剩一个 64px tile。
6. **规则页数据**：新增 `seedCustomRule` instrumentation，向验收包播种参考侧自定义规则后复测。

## 4. 剩余差异性质

- calendar_month / calendar_week：选中日金额文字的亚像素抗锯齿，页面比例 0.27%、0.19%，仅各一个/多个 64px tile 超 6%。
- stats_normal：柱顶数值文字边缘抗锯齿，页面比例 0.07%。
- category_manage：未选中“收入”标签 1px 字高差，页面比例 0.019%。
- rules：参考侧自定义类别图标不在现有资源集内，页面比例 0.071%。
- 两个弹窗：系统窗口阴影与 1px 字体差，用户 2026-10-20 已决定记录。

## 5. 收工状态

- 动画缩放 1.0；参考包与验收包停止；前台系统桌面。
- 无后台任务；Gradle 守护进程已停止。
- 验收包的夜间主题缓存当前为“一木/浅色”（验证后未改回 SYSTEM，仅影响验收包）。
- 源码未提交，HEAD 仍为 fbc48c8。
