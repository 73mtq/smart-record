package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewOutlineProvider
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.data.db.AssetEntity

class YimuAssetDialog(
    context: Context,
    assets: List<AssetEntity>,
    onSelect: (AssetEntity) -> Unit,
    onNone: () -> Unit,
    onManage: () -> Unit,
    onDismiss: () -> Unit,
) : BottomSheetDialog(context) {
    private lateinit var contentView: View

    init {
        val content = LayoutInflater.from(context).inflate(R.layout.yimu_asset_dialog, null, false)
        contentView = content
        val list = content.findViewById<RecyclerView>(R.id.data_list)
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = AssetAdapter(assets,
            onSelect = { asset -> dismiss(); onSelect(asset) },
            onNone = { dismiss(); onNone() },
        )
        content.findViewById<View>(R.id.asset_add).setOnClickListener {
            dismiss()
            onManage()
        }
        content.findViewById<View>(R.id.asset_sort).setOnClickListener { unsupported(context) }
        content.findViewById<View>(R.id.combined_payment).setOnClickListener { unsupported(context) }
        setContentView(content)
        findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.background = null
        val radius = 12 * context.resources.displayMetrics.density
        content.background = GradientDrawable().apply {
            setColor(android.graphics.Color.WHITE)
            cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
        }
        content.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val path = Path().apply {
                    addRoundRect(RectF(0f, 0f, view.width.toFloat(), view.height.toFloat()),
                        floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f), Path.Direction.CW)
                }
                outline.setPath(path)
            }
        }
        content.clipToOutline = true
        content.elevation = 8 * context.resources.displayMetrics.density
        (content.parent as? View)?.background = null
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
        behavior.isHideable = true
        window?.apply {
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            setDimAmount(0.6f)
        }
        setCanceledOnTouchOutside(true)
        setOnDismissListener { onDismiss() }
    }

    override fun show() {
        super.show()
        val radius = 12 * this.context.resources.displayMetrics.density
        window?.setBackgroundDrawable(GradientDrawable().apply {
            setColor(android.graphics.Color.WHITE)
            cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
        })
        window?.decorView?.background = null
    }

    private fun unsupported(context: Context) {
        Toast.makeText(context, R.string.yimu_local_unsupported, Toast.LENGTH_SHORT).show()
    }

    private class AssetAdapter(
        assets: List<AssetEntity>,
        private val onSelect: (AssetEntity) -> Unit,
        private val onNone: () -> Unit,
    ) : RecyclerView.Adapter<AssetAdapter.Holder>() {
        private val items: List<AssetEntity?> = listOf(null) + assets

        class Holder(view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_yimu_asset_choice, parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val asset = items[position]
            val context = holder.itemView.context
            val name = holder.itemView.findViewById<TextView>(R.id.name)
            val icon = holder.itemView.findViewById<ImageView>(R.id.asset_choice_icon)
            icon.translationY = -1f
            if (asset == null) {
                name.setText(R.string.yimu_drawer_no_asset)
                name.setTextColor(context.getColor(R.color.black))
                icon.setImageResource(R.mipmap.ic_no_asset)
                icon.imageTintList = ColorStateList.valueOf(context.getColor(R.color.black))
            } else {
                name.text = asset.assetName
                name.setTextColor(context.getColor(R.color.black))
                icon.setImageResource(R.mipmap.ic_main_account_book)
                icon.imageTintList = ColorStateList.valueOf(context.getColor(R.color.textColorPrimary))
            }
            holder.itemView.setOnClickListener {
                if (asset == null) onNone() else onSelect(asset)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
