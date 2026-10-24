package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
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
import java.util.Locale

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
    private val adapter = AssetAdapter()
    private var hidden = false
    private var net = 0.0
    private var total = 0.0
    private var liability = 0.0

    init {
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter
        eye.setImageResource(R.mipmap.ic_num_not_hide)
        eye.setOnClickListener {
            hidden = !hidden
            eye.setImageResource(if (hidden) R.mipmap.ic_num_hide else R.mipmap.ic_num_not_hide)
            updateAmounts()
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
        adapter.submit(assets, onAssetClick, onAssetLongClick)
        tip.visibility = if (assets.isEmpty()) View.VISIBLE else View.GONE
        list.visibility = if (assets.isEmpty()) View.GONE else View.VISIBLE
        fab.setOnClickListener { onAdd() }
        setting.setOnClickListener { onSettings() }
    }

    private fun updateAmounts() {
        val mask = hidden
        netAssets.text = if (mask) "****" else money(net)
        deposit.text = if (mask) "****" else money(total)
        liabilities.text = if (mask) "****" else money(liability)
    }

    private fun money(value: Double): String = String.format(Locale.US, "%,.2f", value)

    private class AssetAdapter : RecyclerView.Adapter<AssetAdapter.Holder>() {
        private var items: List<AssetEntity> = emptyList()
        private var onClick: (AssetEntity) -> Unit = {}
        private var onLongClick: (AssetEntity) -> Unit = {}

        fun submit(items: List<AssetEntity>, onClick: (AssetEntity) -> Unit, onLongClick: (AssetEntity) -> Unit) {
            this.items = items
            this.onClick = onClick
            this.onLongClick = onLongClick
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_yimu_asset_row, parent, false))

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val asset = items[position]
            holder.name.text = asset.assetName
            holder.cost.text = String.format(Locale.US, "%,.2f", asset.assetNumber)
            holder.itemView.setOnClickListener { onClick(asset) }
            holder.itemView.setOnLongClickListener { onLongClick(asset); true }
        }

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.name)
            val cost: TextView = view.findViewById(R.id.cost)
        }
    }
}
