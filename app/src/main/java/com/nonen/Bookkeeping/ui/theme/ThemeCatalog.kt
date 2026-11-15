package com.nonen.Bookkeeping.ui.theme

import com.nonen.Bookkeeping.R

/**
 * 主题外观目录：原版 j5.c 静态表（夜间、默认、四季、纯色）与 j5.c.a 颜色映射。
 * 颜色取原版字节码中的主题色常量。
 */
object ThemeCatalog {

    data class Item(
        val key: String,
        val name: String,
        val color: Int,
        val background: Int? = null,
        val vip: Boolean = false,
    )

    const val DEFAULT = "default"
    const val NIGHT = "night"
    const val DEFAULT_PRIMARY = 0xFF48AB93.toInt()

    val items: List<Item> = listOf(
        Item(NIGHT, "萤火", 0xFF151C24.toInt(), R.mipmap.bg_main_night, vip = true),
        Item(DEFAULT, "一木", DEFAULT_PRIMARY, R.mipmap.bg_main),
        Item("spring", "春樱", 0xFFFBA8B2.toInt(), R.mipmap.bg_main_spring, vip = true),
        Item("summer", "夏浪", 0xFF46C3D5.toInt(), R.mipmap.bg_main_summer, vip = true),
        Item("autumn", "秋风", 0xFFECB541.toInt(), R.mipmap.bg_main_autumn, vip = true),
        Item("winter", "冬雪", 0xFF669BB3.toInt(), R.mipmap.bg_main_winter, vip = true),
        Item("simplegreen", "绿色", 0xFF48AB93.toInt()),
        Item("simpleblue", "蓝色", 0xFF5AC5FC.toInt()),
        Item("simpleblack", "黑色", 0xFF000000.toInt()),
        Item("simplered", "红色", 0xFFF04A5E.toInt()),
        Item("simpleyellow", "黄色", 0xFFFCBD1A.toInt()),
        Item("simplegrey", "灰色", 0xFFBDBEC0.toInt()),
        Item("simpleone", "深蓝", 0xFF347497.toInt()),
        Item("simpletwo", "胭脂", 0xFFE6A7A2.toInt()),
        Item("simplethree", "翠绿", 0xFFAFD892.toInt()),
        Item("simplefour", "葡萄红", 0xFFCC528F.toInt()),
        Item("simplefive", "莫兰迪", 0xFFA1BECC.toInt()),
        Item("simplesix", "活力橙", 0xFFFD823F.toInt()),
    )

    fun colorFor(key: String): Int = items.firstOrNull { it.key == key }?.color ?: DEFAULT_PRIMARY

    fun nameFor(key: String): String = items.firstOrNull { it.key == key }?.name ?: "一木"

    fun backgroundFor(key: String): Int? = items.firstOrNull { it.key == key }?.background

    fun isNight(key: String): Boolean = key == NIGHT
}
