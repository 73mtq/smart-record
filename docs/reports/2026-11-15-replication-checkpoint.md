# 复刻计划执行记录（2026-11-15）

## 1. 本次完成的功能

### 1.1 标签体系

- `TagEntity` 补齐 `color`、`hide`、`parentTagId`、`updateTime`，数据库升到 v7（`MIGRATION_6_7`）。
- `TagDao` 增加按 ID 查询、按父级查询、更新、删除、清理关联。
- 新增 `TagRepository`：创建、重命名、删除（级联删除二级标签与账单关联）、账单标签写入与读取、隐藏、排序。
- 新增 `TagManageScreen`：一级标签卡片、二级标签胶囊、空态「没有任何标签」、顶部「新增」。
- 新增 `TagEditScreen`：名称、颜色（12 色盘与跟随主题）、一级标签选择、删除确认。
- 记账页标签按钮改为多选弹窗，保存时写入 `transaction_tag_refs`，编辑时回填；标签文本继续写入 `transactions.tags` 保持导出兼容。
- 新增导航路由 `tag_manage`、`tag_edit`。

### 1.2 关于我们与使用指南

- 新增 `AboutScreen`，按原版 `activity_ablout_us.xml` 还原：应用图标、小红书、邮箱、QQ群、官网、公众号、软件版本、备案号、用户协议、隐私政策、个人信息收集清单、第三方共享个人信息清单、开源协议、应用推荐。
- 新增 `GuideScreen`，WebView 加载本地静态文档；新增 6 份本地 HTML（使用指南、用户协议、隐私政策、个人信息收集清单、第三方共享个人信息清单、开源协议）。
- 协议与指南条目跳转内置文档页，复制类条目写入剪贴板并提示。
- 新增导航路由 `about`、`guide`。

### 1.3 主题市场

- 新增 `ThemeCatalog`，录入原版 `j5.c` 的主题表与颜色常量：夜间（萤火）、默认（一木）、四季（春樱、夏浪、秋风、冬雪）、12 个纯色主题。
- 新增 `ThemeTint`：把原生视图中等于默认主色的文字、图标与纯色背景替换为当前主题主色；默认主题时为空操作。
- `BookkeepingTheme` 支持动态主色；`MainActivity` 从同步缓存读取 `themeKey` 注入。
- `SettingsStore` 增加 `themeKey`、`themeLogo` 与同步缓存。
- 主题页主题项接入选中态与点击切换；夜间与默认主题同时切换日/夜模式；四季主题切换首页汇总卡背景。
- 切换后重建 Activity 以刷新原生页面。

### 1.4 资产账户字段

- `AssetEntity` 增加 `icon`、`remark`，数据库升到 v8（`MIGRATION_7_8`）。
- `AssetRepository`、`AssetManageViewModel`、新增/编辑对话框支持备注。
- 资产行与账户选择弹窗显示备注与账户图标。

## 2. 验证结果

| 项目 | 结果 |
| --- | --- |
| `:app:compileDebugKotlin` | BUILD SUCCESSFUL |
| `:app:testDebugUnitTest` | 通过，新增 `ThemeCatalogTest` 5 项 |
| `:app:lintDebug` | BUILD SUCCESSFUL |
| `:app:connectedDebugAndroidTest`（`YimuCompletionInstrumentationTest`） | 1 项通过，0 失败 |
| 真机标签管理 | 一级标签 `work`、二级标签 `sub` 创建成功并正确展示父子关系 |
| 真机主题切换 | 点击「蓝色」后 `settings_theme_cache.xml` 写入 `theme_key=simpleblue`，界面蓝色像素 29648，Activity 重建后停留在主题页 |
| 真机关于我们 | 页面完整渲染，用户协议页 WebView 加载本地文档成功 |

数据库迁移 v7、v8 的 schema 已导出到 `app/schemas/com.nonen.Bookkeeping.data.db.AppDatabase/`。

## 3. 本次未完成

- 分类、预算、周期、愿望、模板的编辑流程仍是对话框或简化交互，尚未按原版独立编辑页还原。
- 阶段 1 数据同步（参考侧实机在 11-14 之后新增了账单）与阶段 7 的 31 页全量回归未执行。
- 规则页图标与分类管理「收入」标签字高的残差未在真机复核。
- 主题的 LOGO 切换与自选背景仍为占位（主题主色、四季背景、选中态已可用）。

## 4. 设备测试注意事项

测试机上的「李跳跳」（`hello.litiaotiao.app`）无障碍服务会抢占 uiautomator 的节点树，导致 `uiautomator dump` 只返回少量节点甚至空白。采集与自动化脚本在每次 dump 前应先执行 `adb shell am force-stop hello.litiaotiao.app`。
