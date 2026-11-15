package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.data.prefs.ThemeMode

/**
 * 一木记账「主题外观」页原生宿主：原版 activity_theme.xml + ThemeActivity（用户 2026-11-12 立项）。
 * 数据顺序取自原版 j5.c 静态表：f53154g（LOGO）、f53153f（主题分组）、f53155h 未在本页展开。
 * 主题/背景/LOGO 的切换属于会员与主题市场能力，本地版本保留视觉与点击占位。
 */
class YimuThemeNativeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : RelativeLayout(context, attrs) {

    interface Listener {
        fun onBack()

        /** 未接入本地的入口：主题分享中心、更多LOGO、LOGO 切换、背景新增、主题预览、右上菜单。 */
        fun onUnsupported(action: String)

        /** 夜间主题（萤火）与默认主题（一木）可切换到对应日/夜模式 */
        fun onThemeMode(mode: ThemeMode) = Unit

        /** 选择主题外观：四季或纯色主题只切换主色，夜间与默认会同时切换日/夜模式 */
        fun onThemeSelected(themeKey: String) = Unit
    }

    private data class ThemeChildItem(
        val name: String,
        val coverRes: Int?,
        val vip: Boolean,
        val themeKey: String,
        val color: Int? = null,
    )

    private data class ThemeParentItem(
        val title: String,
        val children: List<ThemeChildItem>,
        val solid: Boolean = false,
    )

    private val toolBar: View
    private val scrollLayout: View
    private val logoList: RecyclerView
    private val backgroundList: RecyclerView
    private val themeParentList: RecyclerView
    private var listener: Listener? = null
    private var currentThemeKey: String = "default"

    init {
        LayoutInflater.from(context).inflate(R.layout.yimu_theme, this, true)
        toolBar = findViewById(R.id.tool_bar)
        scrollLayout = findViewById(R.id.scroll_layout)
        logoList = findViewById(R.id.logo_list)
        backgroundList = findViewById(R.id.theme_background_list)
        themeParentList = findViewById(R.id.theme_parent_list)

        findViewById<View>(R.id.btn_back).setOnClickListener { listener?.onBack() }
        findViewById<View>(R.id.right_icon).setOnClickListener { showMenu() }
        findViewById<View>(R.id.theme_share).setOnClickListener { listener?.onUnsupported(getString(R.string.yimu_theme_share_center)) }
        findViewById<View>(R.id.more_logo).setOnClickListener { listener?.onUnsupported(getString(R.string.yimu_theme_more_logo)) }

        bindLogoList()
        bindBackgroundList()
        bindThemeParents()
    }

    fun bind(listener: Listener, currentThemeKey: String = "default") {
        this.listener = listener
        if (this.currentThemeKey != currentThemeKey) {
            this.currentThemeKey = currentThemeKey
            themeParentList.adapter?.notifyDataSetChanged()
        }
    }

    fun applyToolbarInset(statusBarPx: Int) {
        val spacer = findViewById<View>(R.id.status_bar_spacer)
        if (spacer.layoutParams.height != statusBarPx) {
            spacer.layoutParams = spacer.layoutParams.apply { height = statusBarPx }
            spacer.requestLayout()
        }
    }

    /** 原版 NestedScrollView 底端 padding 50dp；本页额外扣除导航条高度，避免内容绘制到导航条下。 */
    fun applyNavigationInset(navigationPx: Int) {
        val current = scrollLayout.paddingBottom
        if (navigationPx > 0 && current != navigationPx) {
            scrollLayout.setPadding(scrollLayout.paddingLeft, scrollLayout.paddingTop, scrollLayout.paddingRight, navigationPx)
        }
    }

    private fun getString(id: Int): String = context.getString(id)

    private fun dp(value: Int): Int = Math.round(value * resources.displayMetrics.density)

    private fun showMenu() {
        val menu = PopupMenu(context, findViewById(R.id.right_icon))
        menu.menu.add(0, 1, 0, getString(R.string.yimu_theme_menu_import))
        menu.menu.add(0, 2, 1, getString(R.string.yimu_theme_menu_settings))
        menu.setOnMenuItemClickListener { item ->
            listener?.onUnsupported(item.title.toString())
            true
        }
        menu.show()
    }

    private val logoResources = listOf(
        R.mipmap.yimu_logo_launcher,
        R.mipmap.yimu_logo_color_one,
        R.mipmap.yimu_logo_color_five,
        R.mipmap.yimu_logo_dark_two,
        R.mipmap.yimu_logo_dark_one,
        R.mipmap.yimu_logo_person_text,
        R.mipmap.yimu_logo_pixel_text_one,
        R.mipmap.yimu_logo_pixel_one,
        R.mipmap.yimu_logo_cartoon_text_one,
        R.mipmap.yimu_logo_cartoon_one,
    )

    private fun bindLogoList() {
        val columns = 5
        val cell = (resources.displayMetrics.widthPixels - dp(50)) / columns
        logoList.layoutManager = GridLayoutManager(context, columns)
        logoList.isNestedScrollingEnabled = false
        logoList.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
                object : RecyclerView.ViewHolder(LayoutInflater.from(context).inflate(R.layout.yimu_theme_logo_item, parent, false)) {}

            override fun getItemCount(): Int = logoResources.size

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                holder.itemView.layoutParams = RecyclerView.LayoutParams(cell, cell)
                holder.itemView.findViewById<ImageView>(R.id.logo).setImageResource(logoResources[position])
                holder.itemView.findViewById<View>(R.id.check_icon).visibility = View.GONE
                holder.itemView.setOnClickListener { listener?.onUnsupported(getString(R.string.yimu_theme_logo)) }
            }
        }
    }

    private fun bindBackgroundList() {
        backgroundList.layoutManager = GridLayoutManager(context, 2)
        backgroundList.isNestedScrollingEnabled = false
        val items = listOf(true, false)
        backgroundList.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
                object : RecyclerView.ViewHolder(LayoutInflater.from(context).inflate(R.layout.yimu_theme_background_item, parent, false)) {}

            override fun getItemCount(): Int = items.size

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val blank = items[position]
                val itemView = holder.itemView
                val cover = itemView.findViewById<ImageView>(R.id.cover)
                val name = itemView.findViewById<TextView>(R.id.name)
                val select = itemView.findViewById<View>(R.id.select_layout)
                if (blank) {
                    cover.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.shape_bg_gray))
                    cover.imageTintList = null
                    name.text = getString(R.string.yimu_theme_blank)
                    name.visibility = View.VISIBLE
                    select.visibility = View.VISIBLE
                    itemView.setOnClickListener { listener?.onUnsupported(getString(R.string.yimu_theme_background)) }
                } else {
                    cover.setImageResource(R.mipmap.yimu_theme_custom_add)
                    cover.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.iconTint))
                    name.visibility = View.GONE
                    select.visibility = View.GONE
                    itemView.setOnClickListener { listener?.onUnsupported(getString(R.string.yimu_theme_background)) }
                }
            }
        }
    }

    private fun bindThemeParents() {
        themeParentList.layoutManager = LinearLayoutManager(context)
        themeParentList.isNestedScrollingEnabled = false
        themeParentList.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
                object : RecyclerView.ViewHolder(LayoutInflater.from(context).inflate(R.layout.yimu_theme_parent_item, parent, false)) {}

            override fun getItemCount(): Int = themeParents.size

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val parent = themeParents[position]
                holder.itemView.findViewById<TextView>(R.id.name).text = parent.title
                val list = holder.itemView.findViewById<RecyclerView>(R.id.theme_list)
                list.isNestedScrollingEnabled = false
                list.layoutManager = GridLayoutManager(context, if (parent.solid) 4 else 2)
                list.adapter = if (parent.solid) ColorChildAdapter(parent.children) else ImageChildAdapter(parent.children)
            }
        }
    }

    private val themeParents = listOf(
        ThemeParentItem(
            getString(R.string.yimu_theme_custom),
            listOf(ThemeChildItem(getString(R.string.yimu_theme_add), R.mipmap.yimu_theme_custom_add, true, "self_add")),
        ),
        ThemeParentItem(
            getString(R.string.yimu_theme_night),
            listOf(ThemeChildItem(getString(R.string.yimu_theme_firefly), R.mipmap.bg_main_night, true, "night")),
        ),
        ThemeParentItem(
            getString(R.string.yimu_theme_default),
            listOf(ThemeChildItem(getString(R.string.yimu_theme_yimu), R.mipmap.bg_main, false, "default")),
        ),
        ThemeParentItem(
            getString(R.string.yimu_theme_seasons),
            listOf(
                ThemeChildItem(getString(R.string.yimu_theme_spring), R.mipmap.bg_main_spring, true, "spring"),
                ThemeChildItem(getString(R.string.yimu_theme_summer), R.mipmap.bg_main_summer, true, "summer"),
                ThemeChildItem(getString(R.string.yimu_theme_autumn), R.mipmap.bg_main_autumn, true, "autumn"),
                ThemeChildItem(getString(R.string.yimu_theme_winter), R.mipmap.bg_main_winter, true, "winter"),
            ),
        ),
        ThemeParentItem(
            getString(R.string.yimu_theme_solid),
            listOf(
                ThemeChildItem(getString(R.string.yimu_theme_green), null, false, "simplegreen", 0xFF48AB93.toInt()),
                ThemeChildItem(getString(R.string.yimu_theme_blue), null, false, "simpleblue", 0xFF5AC5FC.toInt()),
                ThemeChildItem(getString(R.string.yimu_theme_black), null, false, "simpleblack", 0xFF000000.toInt()),
                ThemeChildItem(getString(R.string.yimu_theme_red), null, false, "simplered", 0xFFF04A5E.toInt()),
                ThemeChildItem(getString(R.string.yimu_theme_yellow), null, false, "simpleyellow", 0xFFFCBD1A.toInt()),
                ThemeChildItem(getString(R.string.yimu_theme_grey), null, false, "simplegrey", 0xFFBDBEC0.toInt()),
                ThemeChildItem(getString(R.string.yimu_theme_deep_blue), null, false, "simpleone", 0xFF347497.toInt()),
                ThemeChildItem(getString(R.string.yimu_theme_rouge), null, false, "simpletwo", 0xFFE6A7A2.toInt()),
                ThemeChildItem(getString(R.string.yimu_theme_emerald), null, false, "simplethree", 0xFFAFD892.toInt()),
                ThemeChildItem(getString(R.string.yimu_theme_wine), null, false, "simplefour", 0xFFCC528F.toInt()),
                ThemeChildItem(getString(R.string.yimu_theme_morandi), null, false, "simplefive", 0xFFA1BECC.toInt()),
                ThemeChildItem(getString(R.string.yimu_theme_orange), null, false, "simplesix", 0xFFFD823F.toInt()),
            ),
            solid = true,
        ),
    )

    private inner class ImageChildAdapter(
        private val items: List<ThemeChildItem>,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            object : RecyclerView.ViewHolder(LayoutInflater.from(context).inflate(R.layout.yimu_theme_child_item, parent, false)) {}

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = items[position]
            holder.itemView.findViewById<TextView>(R.id.name).text = item.name
            val cover = holder.itemView.findViewById<ImageView>(R.id.cover)
            cover.setImageResource(requireNotNull(item.coverRes))
            cover.imageTintList = if (item.themeKey == "self_add") {
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.iconTint))
            } else {
                null
            }
            holder.itemView.findViewById<View>(R.id.vip_icon).visibility = if (item.vip) View.VISIBLE else View.GONE
            holder.itemView.findViewById<View>(R.id.select_layout).visibility =
                if (item.themeKey == currentThemeKey) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener {
                when (item.themeKey) {
                    "night" -> listener?.onThemeMode(ThemeMode.DARK)
                    "default" -> listener?.onThemeMode(ThemeMode.LIGHT)
                    "self_add" -> {
                        listener?.onUnsupported(item.name)
                        return@setOnClickListener
                    }
                    else -> Unit
                }
                listener?.onThemeSelected(item.themeKey)
            }
        }
    }

    private inner class ColorChildAdapter(
        private val items: List<ThemeChildItem>,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            object : RecyclerView.ViewHolder(LayoutInflater.from(context).inflate(R.layout.yimu_theme_color_item, parent, false)) {}

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = items[position]
            holder.itemView.findViewById<TextView>(R.id.name).text = item.name
            val cover = holder.itemView.findViewById<ImageView>(R.id.cover)
            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.RECTANGLE
            drawable.cornerRadius = dp(10).toFloat()
            drawable.setColor(requireNotNull(item.color))
            cover.setImageDrawable(drawable)
            holder.itemView.findViewById<View>(R.id.vip_icon).visibility = if (item.vip) View.VISIBLE else View.GONE
            holder.itemView.findViewById<View>(R.id.select_layout).visibility =
                if (item.themeKey == currentThemeKey) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener { listener?.onThemeSelected(item.themeKey) }
        }
    }
}
