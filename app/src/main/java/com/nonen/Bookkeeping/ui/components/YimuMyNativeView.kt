package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.nonen.Bookkeeping.R

/**
 * 原版 fragment_my.xml 的原生宿主：头图高度按 481/1080 宽比（MyFragment.a0()），
 * 资料卡按 980:376 约束比例，昵称/天数文案与各入口点击由 bind 注入。
 */
class YimuMyNativeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : RelativeLayout(context, attrs) {

    interface Listener {
        fun onAuto() = Unit
        fun onCycle() = Unit
        fun onDream() = Unit
        fun onRules() = Unit
        fun onBook() = Unit
        fun onTag() = Unit
        fun onCategory() = Unit
        fun onSettings() = Unit
        fun onCloudSync() = Unit
        fun onDataBackup() = Unit
        fun onImportExport() = Unit
        fun onTheme() = Unit
        fun onWidget() = Unit
        fun onGuide() = Unit
        fun onInvitation() = Unit
        fun onFeedback() = Unit
        fun onEvaluate() = Unit
        fun onAbout() = Unit
    }

    private val secondBackground: ImageView
    private val myBackground: ImageView
    private val userInfoLayout: View
    private val photoLayout: CardView
    private val vipIcon: ImageView
    private val nickName: TextView
    private val recordDayNum: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.yimu_my, this, true)
        secondBackground = findViewById(R.id.second_background)
        myBackground = findViewById(R.id.my_background)
        userInfoLayout = findViewById(R.id.user_info_layout)
        photoLayout = findViewById(R.id.photo_image_layout)
        vipIcon = findViewById(R.id.vip_icon)
        nickName = findViewById(R.id.nick_name)
        recordDayNum = findViewById(R.id.record_day_num)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || w == oldw) return
        // MyFragment.a0(): second_background 高 = 宽 × 481 / 1080
        secondBackground.layoutParams = secondBackground.layoutParams.apply {
            height = w * 481 / 1080
        }
        secondBackground.requestLayout()
        // user_info_layout 内卡片约束 dimensionRatio 980:376（内容宽 = w - 2×15dp）
        val density = resources.displayMetrics.density
        val cardWidth = w - Math.round(30 * density)
        val card = myBackground.parent as View
        card.layoutParams = card.layoutParams.apply { height = cardWidth * 376 / 980 }
        card.requestLayout()
    }

    fun bind(nickname: String, recordText: String, showVip: Boolean = false, listener: Listener) {
        // 原版 XML 无 textColor，运行时主题：nick_name=colorPrimaryText(绿)、record=darkGrey(#666666，实测)
        nickName.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.colorPrimaryText))
        recordDayNum.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.darkGrey))
        nickName.text = nickname
        recordDayNum.text = recordText
        vipIcon.visibility = if (showVip) VISIBLE else GONE
        findViewById<View>(R.id.auto_manager).setOnClickListener { listener.onAuto() }
        findViewById<View>(R.id.cycle_manager).setOnClickListener { listener.onCycle() }
        findViewById<View>(R.id.dream_list).setOnClickListener { listener.onDream() }
        findViewById<View>(R.id.ai_type_self).setOnClickListener { listener.onRules() }
        findViewById<View>(R.id.book_manager).setOnClickListener { listener.onBook() }
        findViewById<View>(R.id.tag_manager).setOnClickListener { listener.onTag() }
        findViewById<View>(R.id.category_manager).setOnClickListener { listener.onCategory() }
        findViewById<View>(R.id.setting).setOnClickListener { listener.onSettings() }
        findViewById<View>(R.id.remote_backup_layout).setOnClickListener { listener.onCloudSync() }
        findViewById<View>(R.id.data_backup).setOnClickListener { listener.onDataBackup() }
        findViewById<View>(R.id.import_manager).setOnClickListener { listener.onImportExport() }
        findViewById<View>(R.id.theme_setting).setOnClickListener { listener.onTheme() }
        findViewById<View>(R.id.widget_setting).setOnClickListener { listener.onWidget() }
        findViewById<View>(R.id.user_guide).setOnClickListener { listener.onGuide() }
        findViewById<View>(R.id.invitation).setOnClickListener { listener.onInvitation() }
        findViewById<View>(R.id.feed_back).setOnClickListener { listener.onFeedback() }
        findViewById<View>(R.id.evaluate).setOnClickListener { listener.onEvaluate() }
        findViewById<View>(R.id.about_us).setOnClickListener { listener.onAbout() }
    }
}
