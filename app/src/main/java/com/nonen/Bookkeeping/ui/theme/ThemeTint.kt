package com.nonen.Bookkeeping.ui.theme

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

/**
 * 原生视图的主题着色：把等于默认主色的文字、图标与纯色背景替换成当前主题主色。
 * 默认主题时是空操作，保证已通过像素回归的界面不受影响。
 */
object ThemeTint {

    fun apply(root: View, color: Int) {
        if (color == ThemeCatalog.DEFAULT_PRIMARY) return
        tint(root, color)
    }

    private fun tint(view: View, color: Int) {
        if (view is TextView) {
            val textColors = view.textColors
            if (textColors != null && textColors.defaultColor == ThemeCatalog.DEFAULT_PRIMARY) {
                view.setTextColor(color)
            }
            val hintColors = view.hintTextColors
            if (hintColors != null && hintColors.defaultColor == ThemeCatalog.DEFAULT_PRIMARY) {
                view.setHintTextColor(color)
            }
        }
        if (view is ImageView) {
            val tintList = view.imageTintList
            if (tintList != null && tintList.defaultColor == ThemeCatalog.DEFAULT_PRIMARY) {
                view.imageTintList = ColorStateList.valueOf(color)
            }
        }
        when (val background = view.background) {
            is GradientDrawable -> {
                if (background.color?.defaultColor == ThemeCatalog.DEFAULT_PRIMARY) background.setColor(color)
            }
            is ColorDrawable -> {
                if (background.color == ThemeCatalog.DEFAULT_PRIMARY) background.color = color
            }
            else -> Unit
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) tint(view.getChildAt(index), color)
        }
    }
}
