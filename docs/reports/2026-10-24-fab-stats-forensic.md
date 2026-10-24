# 法证级复刻报告：首页 FAB 与统计图表（2026-10-24）

任务：以首页 FAB 与统计图表两个缺陷为探针，先修测量仪、再修 UI。日志 1148–1182（Run-S11Command.ps1）。
状态：**测量仪体检完成；两缺陷根因取证闭环并已修复；四重验证完成数字验证与落点功能验证，状态扫描/盲评/29 页回归待下次执行。**

---

## 第 0 步 — 体检测量仪（全部完成）

### 0.1 噪声底（日志 1156/1157 连拍，1159 分析）

参考侧（原版 1146 号数据态）home_top 与 stats_month 各连拍两次互 diff：

| 页面 | 全页 ratio | worst_tile | FAB/pie_dome/柱图区域 maxΔ |
| --- | --- | --- | --- |
| home_top | 0.000000 | 0.0000 | FAB 区域 0 |
| stats_month | 0.000000 | 0.0000 | 三个图表区域全部 0 |

**管线噪声底 = 0**（容差 8 下逐像素一致）。结论：门禁读数中任何残差都是确定性真实差异，"低于噪声底的改善"在本管线中不存在——一切改善均可作证据，收敛目标即 0。

### 0.2 掩膜审计（日志 1159/1160/1161）

- 目标页掩膜清单：home_top / home_scrolled / home_drawer / account_book_popup / stats_month / stats_normal / stats_year / stats_self **仅有全局掩膜**，无任何页面级掩膜：
  - `status_bar [0,0,1200,140]`：两次参考连拍内部变化 10229/168000 px（maxΔ226）——系统时钟/状态图标动态内容，实证正当；
  - `navigation_bar [0,2618,1200,2670]`：连拍变化 132 px（maxΔ150）——系统手势区，实证正当。
- 无掩膜重 diff（1161，ref vs impl）：两页全部 80 个超阈 tile 均位于 y<140 状态栏区；**无任何内容区差异被掩膜遮蔽**。
- `asset_banner_texture [49,189,1151,485]` 属资产页（非目标页），本次不动，遗留清单已记录。

### 0.3 金丝雀（日志 1162–1173）

| 金丝雀 | 改动 | 结果 |
| --- | --- | --- |
| A（FAB 尺寸） | 首页 FAB 56dp→58dp | 门禁**正确报警**：home_top pass→fail，worst_tile 3.47%→55.59%@(1015,2241)（FAB 区域内），fab 区域 0.65%→9.35%、maxΔ 29→183 |
| B（轴密度） | 月柱图 Y 轴 labelCount 5→6 | month_bar_chart 区域 0.0/maxΔ0 → 0.157%/maxΔ153（**区域级可见**）；但 0.157% 低于 6% tile 阈值，**tile 级不报警——门禁第二层盲区，已记录** |

两金丝雀已还原（1167 重建），基线读数复原验证：home_top 0.019%/3.47% pass、stats_month 0.378%/38.06% fail，与改前完全一致。

### 0.4 指标改造（diff.py 升级，已通过 1182 号运行验证）

- 新增 `hot_tiles`：全部超阈值 tile 清单（此前只报最差一个）；
- 新增 `diff_heatmap`：像素级热力图（暗红灰度，掩膜区压暗）；
- 新增 `probe_regions`（screens.yaml 可配置）：FAB 区域 [900,2140,1180,2420]、stats 页三个图表区域等的分区域 diff_ratio 与 max_channel_delta；
- 报告头固化通过定义：**"无任何 64px tile 超阈值 AND 全页 ratio≤1.5% AND bounds/solid 干净"**；
- 兼容性：升级后对既有采集完全复现旧数字。

---

## 第 1 步 — 发现差异（全部完成，证据链见下）

### 缺陷一：首页 FAB —— 病根不是像素，是落点

用户纠正确认：图标相同、圆边缘仅细微差（量化：rim 1px 环 508px maxΔ29，几何 bbox 两侧一致 182px=56dp）；**真正缺陷是点击后功能页面完全不同**。

运行时实测（日志 1148/1149/1174 + 复点验证）：

| 页面 | 参考 FAB（add_btn）落点 | 我们修复前落点 |
| --- | --- | --- |
| 首页 | **AI 记账面板**（AddBillDialog，MainActivity 内对话框） | 记一笔页（AddEditScreen） |
| 日历 | **AI 记账面板**（同上） | 记一笔页 |
| 资产 | **主题外观页**（ThemeActivity，连续 3 次验证） | 记一笔页 |

**门禁制度性盲区实锤**：screens.yaml 记账页采集点击的是 `btn_add_bill`（滚动后工具栏"+"），**从未点过 `add_btn`（FAB）**——像素门禁 21/29 通过与用户目视矛盾的根源之一。

参考侧证据链（静态反编译，Frida 不可用见 §限制）：
- `layout_float_ball`（res/Fw.xml）：FAB = Material FloatingActionButton，fabSize normal(56dp)、margin 20dp、borderWidth 0、maxImageSize 32dp、scaleType center、tint textColorWhite、backgroundTint colorPrimary；
- 图标切换：MainActivity.m0（smali 反编译 switch）——`tab_asset → setImage(R.mipmap.ic_add_asset)`（纯加号），其余 tab → `ic_add_ai_bill`（麦克风）；
- 首页/日历 FAB 落点：MainActivity.e0()（MainActivity.java:690）`new AddBillDialog(this,...)`；
- 资产 FAB 落点：ThemeActivity（ThemeActivity 实测 3 次）。

**AI 面板（dialog_add_bill，res/jx.xml 经 aapt2 解码）**：顶行账本条（book_icon 无图标时 GONE+book_name 14sp 粗体+ic_account_book_more 18dp）→ 白卡（shape_bg_setting 8dp 圆角、5dp 边距）内含 word_edit（14sp、hint textGray #8a8a8a、四行占位文案、lineSpacingExtra 8dp、padding 20dp）+ analysis_layout 解析结果区（分类/转出/转入/金额，GONE）+ add_btn 提交钮（ic_fast_add_bg 青色四分之一圆 tint colorPrimaryAlpha30 #4d48ab93 + ic_fast_add 白勾 19dp）→ 底部行（日历 45dp+date_text 7sp 粗体、tag 45dp、无账户 ic_no_asset、不报销 ic_data_no_baoxiao）。全部图标从 reference_apk/base.apk 原始 drawable 提取（资源名+路径记录于 work/tmp/probe/res_map.txt）。

### 缺陷二：统计图表（stats_month 饼图）—— 2.5% 钳制缺失

测量（1182 号前后对比）：
- 饼图只露出屏幕底部一条"穹顶"（y2385-2419，卡片被底栏裁切）；圆心/半径两侧一致（~599,2620 r≈233）；
- 青/橙扇区接缝：ref 257.2° vs impl 263.3°（旋转差 ~6°）；两侧标注文字同为 **15.90%**（数据一致，排除数据源）；
- 反编译命中 `x4.java`（com.wangc.bill.manager.chart，原版图表管理类）：数据装配 y() 中 **`d10 = 0.025*d9; if (doubleValue < d10) doubleValue = d10`（x4.java:424-427）**——小于总额 2.5% 的分类钳到 2.5% 参与角度、标签百分比仍用原始值；
- 数值闭环：用 1998 条快照计算 2026-10 支出（总 4359.42，6 个一级分类，3 个低于 2.5% 被钳升）→ 预测原版接缝 257.64° vs 实测 257.2°；无钳制预测 263.46° vs 实测 263.3°，误差 <0.5°，根因唯一；
- 柱状图区域 diff=0.0（原图无恙），资产走势区域 1.2% 为既有 ±1.2 钳制残留。

---

## 第 2 步 — 修复（全部落码，构建通过，1181/1182 号验证）

| 修复 | 文件 | 证据出处 |
| --- | --- | --- |
| AI 面板布局 1:1 复刻 | `app/src/main/res/layout/yimu_fast_add.xml`（新建） | res/jx.xml（dialog_add_bill）aapt2 解码；层级实测修正：底部图标行在白卡内部、book_icon 空时 GONE |
| AI 面板对话框 | `YimuFastAddDialog.kt`（新建）：底部 Dialog、宽度满屏、IME 自弹、保存走本地解析 | AddBillDialog.java:1030-1103；本地解析 = ClassificationModelService.parseQuickRecord（与 AddEditViewModel.applyQuickText 同一模型），**替代原版解析路径，保持无网络权限红线** |
| 首页 FAB 接线 | `YimuHomeScreen.kt`（add_btn onClick） | 探针 1149 实测落点 |
| 日历 FAB 接线 | `YimuCalendarNativeView.kt`（Listener+onFastAdd）、`YimuCalendarScreen.kt` | 探针 1174 实测落点 |
| 资产 FAB 图标 | `view_yimu_asset.xml` ic_add_ai_bill → **ic_add_asset**（从 base.apk 提取的原始 PNG，mipmap-xxhdpi） | MainActivity.m0 smali switch + 图标 bbox 实测 |
| 饼图 2.5% 钳制 | `YimuStatisticsNativeView.kt` renderMonthCategory | x4.java:424-427 |
| 资源 | 新增 blackAlpha25 #40000000；9 个原版图标入 mipmap-xxhdpi | res_map.txt 资源表 |

面板落点页 UI 树逐节点比对（impl3 vs ref probe）：`parent_layout [0,920][1200,1541]`、`add_btn [1021,1083][1184,1246]`、`word_edit [16,1083][1021,1362]` 与参考**完全一致**；content_layout/底部行按白卡结构修正后待复测（被收尾打断，未重采）。

钳制效果（1182 号）：pie_dome 区域 12.12%→**5.63%**（maxΔ 255→153），全页 0.378%→**0.294%**，worst_tile 38.06%@(519,2372)→31.93%@(799,2336)。**方向正确（与 257.64° 预测一致），未完全归零——残余在接缝/标注区，下次按热力图继续取证（候选：MPChart 角度累加细节、标注线锚点、sliceSpace 交互）。**

---

## 第 3 步 — 四重验证（部分完成）

| 验证 | 状态 |
| --- | --- |
| 数字验证（轴/占比序列数字相等） | **部分完成**：接缝角预测-实测闭环（257.64°/257.2°）；两侧标注 15.90% 一致；柱图轴区域 diff=0 |
| 落点功能验证（新门禁） | **完成**：UI 树节点级比对 word_edit/add_btn/parent_layout 完全一致；双侧 AI 面板打开路径实证 |
| 状态扫描（N≥10 随机状态重采） | **未开始**（下会话首项） |
| 盲评（无标签成对图交独立裁判） | **未开始** |
| 回归（29 页全量） | **未开始**；注意 impl 侧 stats_month/home_top 已是修复后采集，全量重采时一并刷新 |

## 限制与红线遵守记录

- **Frida 不可行**（如实记录）：设备无 root（su 不存在）、host 无 frida、参考为生产签名包——动态插桩不可用。替代证据链：uiautomator 双侧逐节点几何 + 参考截图数字提取 + aapt2 资源表/二进制 XML 解码 + jadx 静态源码（行号均已在档案标注）。静态反编译按协议仅作"解读辅助"，所有关键参数均经像素/数值独立闭环验证。
- 同源材料：FAB/面板 9 个图标全部取自 base.apk 原始 drawable（未手绘），布局属性取自二进制 XML 而非截图反推。
- 像素仅回答"哪里不同"：接缝角度等修复参数均来自 x4.java 代码证据 + 1998 条数据复算，像素只做验证。
- 门禁阈值未改（6% tile / 1.5% 全页 / bounds 3px）；通过定义文字固化进报告。
- 全程采集走 Run-S11Command.ps1（日志 1148–1182；一次 Tee 缓冲异常导致 1163 安装日志不完整，已用 1168c/d 直接 adb 安装补录）。

## 遗留（下次会话，按优先级）

1. AI 面板白卡结构修正后的落点页重采 + 像素门禁（新建 fast_add 采集场景入 screens.yaml，双侧弹面板同态采集）；
2. 饼图残差 5.63% 继续取证归零（热力图定位 → 候选根因逐一排除）；
3. 四重验证：状态扫描 N≥10（月份/页签/收支切换×首页滚动）、盲评（visual-judge）、29 页回归；
4. 资产 FAB 落点 = 主题外观页复刻（ThemeActivity，超出本次两缺陷范围，**待用户裁决**是否立项）；
5. 金丝雀 B 暴露的 tile 盲区：轴密度类错误 0.157% < 6% tile 阈值——靠 probe_regions 人工审查补位（阈值未改，按用户红线）。
