package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.data.db.AssetEntity
import com.nonen.Bookkeeping.data.repo.AssetRepository
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 资产页原生视图：顶部横幅、分类汇总卡片、可折叠的账户分组列表与新增按钮。
 */
class YimuAssetNativeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : RelativeLayout(context, attrs) {

    init {
        LayoutInflater.from(context).inflate(R.layout.view_yimu_asset, this, true)
    }

    private val netAssets = findViewById<TextView>(R.id.net_assets)
    private val deposit = findViewById<TextView>(R.id.deposit)
    private val liabilities = findViewById<TextView>(R.id.liabilities)
    private val tip = findViewById<View>(R.id.asset_tip)
    private val list = findViewById<RecyclerView>(R.id.asset_list)
    private val fab = findViewById<View>(R.id.add_btn)
    private val eye = findViewById<ImageView>(R.id.ic_num_hide)
    private val setting = findViewById<View>(R.id.asset_setting)
    private val adapter = AssetGroupAdapter()
    private var hidden = false
    private var net = 0.0
    private var total = 0.0
    private var liability = 0.0

    /** 折叠的组名集合，默认全部展开（原版 AssetWeight.notExpand 默认 false）。 */
    private val collapsedGroups = mutableSetOf<String>()

    init {
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter
        eye.setImageResource(R.mipmap.ic_num_not_hide)
        eye.setOnClickListener {
            hidden = !hidden
            eye.setImageResource(if (hidden) R.mipmap.ic_num_hide else R.mipmap.ic_num_not_hide)
            updateAmounts()
            adapter.notifyDataSetChanged()
        }
    }

    fun render(
        netAsset: Double,
        totalAsset: Double,
        totalLiability: Double,
        assets: List<AssetEntity>,
        onAdd: () -> Unit,
        onSettings: () -> Unit,
        onAssetClick: (AssetEntity) -> Unit,
        onAssetLongClick: (AssetEntity) -> Unit,
    ) {
        net = netAsset
        total = totalAsset
        liability = totalLiability
        updateAmounts()
        adapter.submit(groupAssets(assets), onAssetClick, onAssetLongClick)
        tip.visibility = if (assets.isEmpty()) View.VISIBLE else View.GONE
        list.visibility = if (assets.isEmpty()) View.GONE else View.VISIBLE
        fab.setOnClickListener { onAdd() }
        setting.setOnClickListener { onSettings() }
    }

    private fun updateAmounts() {
        netAssets.text = assetAmountText(net, hidden)
        deposit.text = assetAmountText(total, hidden)
        liabilities.text = assetAmountText(liability, hidden)
    }

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        resources.displayMetrics,
    ).roundToInt()

    /** 金额颜色：原版 v0.w2 按是否计入总资产与正负号着色。 */
    private fun assetCostColor(asset: AssetEntity): Int {
        val colorRes = when {
            !asset.intoTotalAsset || asset.assetNumber == 0.0 -> R.color.textColorBlack
            asset.assetNumber > 0 -> R.color.moneyIncome
            else -> R.color.moneyPay
        }
        return ContextCompat.getColor(context, colorRes)
    }

    private inner class AssetGroupAdapter : RecyclerView.Adapter<AssetGroupAdapter.GroupHolder>() {
        private var groups: List<AssetGroup> = emptyList()
        private var onClick: (AssetEntity) -> Unit = {}
        private var onLongClick: (AssetEntity) -> Unit = {}

        fun submit(
            groups: List<AssetGroup>,
            onClick: (AssetEntity) -> Unit,
            onLongClick: (AssetEntity) -> Unit,
        ) {
            this.groups = groups
            this.onClick = onClick
            this.onLongClick = onLongClick
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = groups.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupHolder =
            GroupHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_yimu_asset_header, parent, false),
            )

        override fun onBindViewHolder(holder: GroupHolder, position: Int) {
            holder.bind(groups[position])
        }

        inner class GroupHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val totalLayout: View = view.findViewById(R.id.total_layout)
            private val topLayout: View = view.findViewById(R.id.top_layout)
            private val title: TextView = view.findViewById(R.id.title)
            private val assetNum: TextView = view.findViewById(R.id.asset_num)
            private val number: TextView = view.findViewById(R.id.number)
            private val arrow: ImageView = view.findViewById(R.id.arrow)
            private val childList: RecyclerView = view.findViewById(R.id.data_list)
            private val childAdapter = AssetChildAdapter()

            init {
                childList.layoutManager = LinearLayoutManager(view.context)
                childList.isNestedScrollingEnabled = false
                childList.adapter = childAdapter
            }

            fun bind(group: AssetGroup) {
                val expanded = !collapsedGroups.contains(group.name)
                title.text = group.name
                assetNum.text = "(" + group.assets.size + ")"
                assetNum.visibility = if (expanded) View.GONE else View.VISIBLE
                number.text = assetAmountText(group.total, hidden)
                // 原版折叠箭头 ic_right_small 与 ic_bottom_small 旋转 -90 度逐像素相同
                arrow.rotation = if (expanded) 0f else -90f
                childList.visibility = if (expanded) View.VISIBLE else View.GONE
                val params = totalLayout.layoutParams
                val targetHeight = if (expanded) ViewGroup.LayoutParams.WRAP_CONTENT else dp(45)
                if (params.height != targetHeight) {
                    params.height = targetHeight
                    totalLayout.layoutParams = params
                }
                childAdapter.submit(group.assets, onClick, onLongClick, hidden)
                topLayout.setOnClickListener {
                    if (expanded) collapsedGroups.add(group.name) else collapsedGroups.remove(group.name)
                    val index = groups.indexOfFirst { it.name == group.name }
                    if (index >= 0) notifyItemChanged(index)
                }
            }
        }
    }

    private inner class AssetChildAdapter : RecyclerView.Adapter<AssetChildAdapter.AssetHolder>() {
        private var items: List<AssetEntity> = emptyList()
        private var onClick: (AssetEntity) -> Unit = {}
        private var onLongClick: (AssetEntity) -> Unit = {}
        private var masked = false

        fun submit(
            items: List<AssetEntity>,
            onClick: (AssetEntity) -> Unit,
            onLongClick: (AssetEntity) -> Unit,
            masked: Boolean,
        ) {
            this.items = items
            this.onClick = onClick
            this.onLongClick = onLongClick
            this.masked = masked
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetHolder =
            AssetHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_yimu_asset_row, parent, false),
            )

        override fun onBindViewHolder(holder: AssetHolder, position: Int) {
            holder.bind(items[position])
        }

        inner class AssetHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val icon: ImageView = view.findViewById(R.id.icon)
            private val name: TextView = view.findViewById(R.id.name)
            private val remark: TextView = view.findViewById(R.id.remark)
            private val cost: TextView = view.findViewById(R.id.cost)

            fun bind(asset: AssetEntity) {
                icon.setImageResource(assetIconRes(asset))
                name.text = asset.assetName
                val remarkText = asset.remark.orEmpty().trim()
                remark.text = remarkText
                remark.visibility = if (remarkText.isEmpty()) View.GONE else View.VISIBLE
                cost.text = assetAmountText(asset.assetNumber, masked)
                cost.setTextColor(assetCostColor(asset))
                itemView.setOnClickListener { onClick(asset) }
                itemView.setOnLongClickListener { onLongClick(asset); true }
            }

            /** 账户图标：优先使用账户自带的图标资源名，缺失时按账户类型回退。 */
            private fun assetIconRes(asset: AssetEntity): Int {
                val custom = asset.icon?.takeIf { it.isNotBlank() }?.let { name ->
                    itemView.resources.getIdentifier(name, "mipmap", itemView.context.packageName)
                }
                if (custom != null && custom != 0) return custom
                return when (asset.assetType) {
                    AssetRepository.TYPE_FUND, AssetRepository.TYPE_CREDIT -> R.mipmap.ic_asset_choice_one
                    else -> R.mipmap.ic_asset_not_check
                }
            }
        }
    }
}

/** 分组后的账户集合：组名与组内账户。 */
internal data class AssetGroup(
    val name: String,
    val assets: List<AssetEntity>,
) {
    /** 组总额：仅统计计入总资产的账户（原版 manager/c.java:677-679）。 */
    val total: Double get() = assets.filter { it.intoTotalAsset }.sumOf { it.assetNumber }
}

/** 原版默认分组名（database/action/f.java:805-825）。 */
private val defaultGroupNames = mapOf(
    1 to "资金账户",
    2 to "信贷账户",
    3 to "充值账户",
    4 to "投资理财",
    5 to "二手货物",
    6 to "应收款/借出",
    7 to "应付款/借入",
    9 to "报销",
)

/** 组间默认顺序；原版按 AssetWeight 权重排序，未拖动过的组权重相同。 */
private val defaultGroupOrder = listOf(
    "资金账户",
    "信贷账户",
    "充值账户",
    "应收款/借出",
    "应付款/借入",
    "报销",
)

/** 组名：优先使用账户分组名，为空时按账户类型回退。 */
internal fun assetGroupName(asset: AssetEntity): String =
    asset.groupName?.trim()?.takeIf { it.isNotEmpty() }
        ?: defaultGroupNames[asset.assetType]
        ?: "其他账户"

/** 把账户按组名分组：组内按 positionWeight、assetId 升序，组间按默认顺序、名称升序。 */
internal fun groupAssets(assets: List<AssetEntity>): List<AssetGroup> {
    val grouped = LinkedHashMap<String, MutableList<AssetEntity>>()
    assets.sortedWith(compareBy({ it.positionWeight }, { it.assetId })).forEach { asset ->
        grouped.getOrPut(assetGroupName(asset)) { mutableListOf() }.add(asset)
    }
    return grouped.map { AssetGroup(it.key, it.value) }
        .sortedWith(compareBy({ defaultGroupRank(it.name) }, { it.name }))
}

private fun defaultGroupRank(name: String): Int = defaultGroupOrder.indexOf(name).let {
    if (it < 0) defaultGroupOrder.size else it
}

/** 金额文本；隐藏金额时与原版一致显示 ****（t0.w2 组头、v0.w2 行内）。 */
internal fun assetAmountText(value: Double, hidden: Boolean): String =
    if (hidden) "****" else String.format(Locale.US, "%,.2f", value)
