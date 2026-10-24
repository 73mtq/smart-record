# HANDOFF — S13 一木记账法证级复刻（2026-10-24 用户要求收工）

## 1. 状态

- **阶段：S13（法证级：先修测量仪、再修 UI）。本文件为收工交接，源码已提交（见 §6），HEAD 前进。**
- **本阶段已完成：**
  - **测量仪体检**：噪声底=0（参考侧连拍逐像素一致）；掩膜审计通过（目标页无页面级掩膜、全局两掩膜有连拍动态实证、无掩膜重 diff 全部超阈 tile 均在时钟区）；金丝雀 A（FAB+2dp）报警成功 55.59%、金丝雀 B（labelCount 5→6）暴露 tile 级盲区（0.157%<6%，区域级可见）；diff.py 升级（hot_tiles+heatmap+probe_regions），通过定义固化为"无任何 tile 超阈值"。阈值未改。
  - **缺陷一根因**：首页 FAB 缺陷在**落点**不在像素——原版首页/日历 add_btn 打开 **AddBillDialog AI 记账面板**，资产页打开 **ThemeActivity 主题外观**；我们全部打开记一笔页。门禁从未发现因为采集点的是 btn_add_bill（工具栏"+"）而非 add_btn。
  - **缺陷二根因**：stats_month 饼图扇区接缝旋转 ~6°——**原版 x4.java:424-427 有 2.5% 钳制**（小于总额 2.5% 的分类钳到 2.5% 参与角度、标签仍用原始百分比），我们缺失。数值闭环：预测 257.64° vs 实测 257.2°，误差<0.5°。
  - **修复已落码**（见 §2）并通过编译；AI 面板落点页 UI 树与参考逐节点一致（parent_layout/add_btn/word_edit bounds 完全相同）；饼图钳制使 pie_dome 12.12%→5.63%、全页 0.378%→0.294%（未归零，残余在标注区 (799,2336)）。
- **交付文档**：`docs/reports/2026-10-24-fab-stats-forensic.md`（两缺陷档案+测量仪记录+掩膜清单+限制说明，自包含）；取证明细追加在 `work/logs/yimu-ui-reconstruction-s11/groupE-known-differences.md` S13 节。

## 2. 本阶段源码变更（已提交）

- 新增：`ui/components/YimuFastAddDialog.kt`、`res/layout/yimu_fast_add.xml`、`res/drawable/selectable_item_expand.xml`（如缺）、mipmap-xxhdpi 9 个原版图标（ic_account_book_more/ic_expand/ic_fast_add/ic_fast_add_bg/ic_no_asset/ic_data_no_baoxiao/ic_calendar/ic_add_asset）。
- 修改：`YimuHomeScreen.kt`（add_btn→AI 面板）、`YimuCalendarNativeView.kt`+`YimuCalendarScreen.kt`（Listener.onFastAdd→AI 面板）、`view_yimu_asset.xml`（FAB 图标→ic_add_asset）、`YimuStatisticsNativeView.kt`（renderMonthCategory 2.5% 钳制，注释标 x4.java:424-427）、`colors.xml`（blackAlpha25）、`yimu_strings.xml`（面板文案）。
- 工具（work/，不入库）：`ui-harness/diff.py` 升级+`ui-harness/diff.py.s12-backup` 备份、`screens.yaml` probe_regions、`tmp/probe/` 全部取证物（布局解码、res_map、提取图标、探针脚本与截图、噪声/金丝雀报告）。
- 全部证据出处（反编译文件:行号/资源名/实测数据）见交付文档；无一处盲调参数。

## 3. 关键取证结论（防重查）

- FAB 真身：add_btn = Material FAB（res/Fw.xml：56dp/margin20/borderWidth0/maxImageSize32dp/tint white/bgTint colorPrimary）；图标切换 MainActivity.m0 smali：tab_asset→ic_add_asset，其余→ic_add_ai_bill。
- AI 面板 = AddBillDialog（R.layout.dialog_add_bill = res/jx.xml）：顶行账本条（book_icon 空时 GONE，book_name 14sp 粗体从 x49 起）→ 白卡（shape_bg_setting 8dp 圆角，margin 5dp）含 word_edit（14sp/hint #8a8a8a/四行占位/lineSpacingExtra 8dp/padding 20dp）+ analysis_layout（解析预览，GONE）+ add_btn（ic_fast_add_bg tint #4d48ab93 + ic_fast_add 白勾）→ **底部图标行在白卡内部**（45dp 日历+date 7sp 粗体/tag/无账户/不报销）。
- 饼图：原版 rotationAngle 也为 0f（x4.t()）；数据按金额降序（x4.w 比较器）；**2.5% 钳制在 x4.java:424-427**；月柱图/资产走势区域 diff=0 无恙。
- 测量仪：噪声底=0；探针区域 FAB=[900,2140,1180,2420]、pie_dome=[400,2360,830,2425] 等已入 screens.yaml。
- Frida 不可行（无 root/无 frida/生产包），替代证据链见交付文档"限制"节。

## 4. 剩余工作（按优先级）

1. **AI 面板落点页收尾**：yimu_fast_add 结构修正（底部行入卡）后的重采复测；新增 fast_add 采集场景（双侧 tap FAB→同态弹面板）入 screens.yaml，纳入门禁。
2. **饼图残差 5.63% 归零**：热力图定位残余（接缝/标注区），候选：MPChart 角度累加细节、标注线锚点、sliceSpace 交互；禁止盲调。
3. **四重验证**：状态扫描 N≥10（月份/页签/收支×首页滚动，重采重 diff）；盲评（FAB/图表区域无标签成对图交 visual-judge 独立裁判 A/B）；29 页全量回归（impl 全量重采+diff）。
4. **资产 FAB 落点=主题外观页**：ThemeActivity 复刻未立项，**待用户裁决**（超出两缺陷范围）。
5. 金丝雀 B 盲区：轴密度类错误 tile 不报警——靠 probe_regions 人工审查（阈值未改）。

## 5. 环境与命令（不变）

- 设备小米 15 `95f79212`；收工纪律已执行：动画三项 1.0、前台桌面、validation/reference 双 app 已停。
- 构建：`.\gradlew.bat :app:assembleDebug -PyimuUiValidation=true`；artifact 同步 `cp app/build/outputs/apk/debug/app-debug.apk work/artifacts/yimu-ui-validation.apk`；安装 `adb install -r`（app+androidTest 两个 apk）；采集 `python work/ui-harness/capture.py --app reference|validation [--only 页]`；diff `python work/ui-harness/diff.py --only 页 --report …`。全部走 `Run-S11Command.ps1`，下一日志号 **1183**。
- 当前 validation APK 与源码一致（含修复，artifact sha256 f440c9bc…；注意 Run-S11Command 内嵌套调用 Install-S11Validation.ps1 有 Tee 缓冲异常前科，安装用直接 adb）。
- 采集前设动画缩放 0、结束恢复 1.0（本次收工已恢复 1.0）。

## 6. 提交

- 本阶段源码+文档已提交并推送 GitHub（用户 2026-10-24 明确要求"保存进度整理文档提交github"）；提交不含 work/（个人账单数据，不入库）与根目录误建文件 `--help`/`--package`（仍待裁决）。

## 7. 需要用户裁决

- 资产 FAB 落点页（主题外观 ThemeActivity）是否立项复刻（约一页工作量）。
- AI 面板保存行为的本地解析替代方案是否接受（原版解析路径反编译不完整，我们用 app 内既有本地模型 ClassificationModelService.parseQuickRecord，与 AddEditViewModel 同一解析器，保持无网络红线）。
- 根目录误建文件 `--help`、`--package` 是否删除。
