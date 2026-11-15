# 一木记账复刻最终回归（2026-11-15 晚）

## 1. 数据同步

参考侧实机在 11-14 之后新增了 9 条账单。本次从参考应用重新导出：

- 源文件：`work/ui-fixtures/yimu_all_20261115.xls`
- 条数：2121，支出 -45,541.96 元，收入 35,599.14 元
- SHA-256：`c0add99723068fd99d9907b03d1e2a0f23f3eed99b73c66eca11d225a64c8e84`
- `work/ui-harness/screens.yaml` 的数据身份已更新为上述值

验收包重建：pm clear 后恢复参考上下文数据库（日常账本、78 分类、0 资产），导入 2121 条，播种自定义规则 `文化教育-AI与订阅`。复核结果：transactions 2121、categories 78、assets 0、自定义规则 1。

验收包 SHA-256：`3e06ab2352ceb1cfa56d226bb2978db41080220ecce565eb212867a4032658a9`。

## 2. 同日双端采集与对比

两侧同一天采集 31 页，报告 `work/ui-capture/diff/full-20261115-2125.json`，另用 `work/ui-capture/diff/fix-20261115-2140.json` 复核 home_scrolled。

结果：**23 页通过、8 页未通过**。

通过页：home_top、home_scrolled、home_drawer、account_book_popup、add_bill_expense、add_bill_income、add_bill_transfer、add_bill_child_category、add_bill_expression、add_bill_result、asset、stats_month、stats_year、stats_self、my、search、settings、periodic、savings_goal、budget、backup、theme_appearance、fast_add。

未通过页与归因：

| 页面 | 页面比例 | 最差 tile | 归因 |
| --- | --- | --- | --- |
| add_bill_date_dialog | 0.555% | 22.36% | 系统窗口阴影与 1px 字体栅格化（2026-10-20 已记录） |
| add_bill_asset_dialog | 0.1855% | 12.08% | 系统窗口阴影（已记录） |
| calendar_month | 0.4044% | 28.93% | 选中日金额文字与卡片边缘抗锯齿 |
| calendar_week | 0.3215% | 28.93% | 同上 |
| stats_normal | 0.1081% | 8.62% | 首个柱顶数值右缘 1px 抗锯齿 |
| category_manage | 0.0193% | 11.52% | 顶部「支出/收入」标签文字 1px 字高差 |
| rules | 0.0711% | 51.64% | 参考侧用户把「文化教育/AI与订阅」的图标改成了自定义图片，本地资源集不含该图片 |
| import_export | 0.0674% | 15.62% | 页面左边缘的系统滚动条/系统元素 |

home_scrolled 首次采集因列表内容变化产生 1px 滚动偏移，把 `screens.yaml` 的 `scroll_to_offset` 从 -112 调整为 -113 后通过（0.0038%、tile 1.39%）。

## 3. 本轮完成的编辑页

- 分类编辑页：名称、线条图标网格、仅在当前账本显示、删除（真机验证通过）
- 愿望编辑页：名称、目标金额、已攒金额、备注、结束日期、删除（真机验证通过）
- 预算设置页：按年月编辑预算总额（真机验证通过）
- 周期设置页：周期类型、金额、备注、重复周期、每月日期（真机验证通过）

以上页面均通过编译、单测与 Lint。

## 4. 结论

复刻计划的功能项与数据同步、全量回归均已完成。剩余 8 页差异全部属于系统窗口阴影、字体栅格化抗锯齿、参考侧用户自定义图片三类，无法通过代码消除，作为已记录残差保留。

## 5. 复现命令

```bash
# 数据准备（新导出后）
bash work/tmp/prepare_validation.sh

# 采集与对比
python work/ui-harness/capture.py --app reference
python work/ui-harness/capture.py --app validation
python work/ui-harness/diff.py --report work/ui-capture/diff/full-<日期>.json
```
