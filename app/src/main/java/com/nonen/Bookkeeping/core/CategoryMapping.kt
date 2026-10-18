package com.nonen.Bookkeeping.core

import com.nonen.Bookkeeping.data.db.CategoryEntity

/**
 * 分类映射解析结果。
 */
data class TargetCategoryMapping(
    val parentId: Int,
    val childId: Int?,
    val parentName: String,
    val childName: String?,
    val categoryType: Int,
    val isCustom: Boolean = false,
    val iconUrl: String = "ic_parent_other",
)

/**
 * 历史分类与一木 76 项默认分类的双向映射规则定义。
 */
object CategoryMapping {

    /**
     * 预定义的迁移自定义分类（保留原意、无法唯一映射时自动播种）。
     */
    val CUSTOM_SEED_CATEGORIES: List<CategoryEntity> = listOf(
        // 软件服务 (一级自定义分类)
        CategoryEntity(categoryId = 88, parentId = 0, categoryName = "软件服务", iconUrl = "ic_shopping_phone", positionWeight = 88, categoryType = 0, isCustom = true),
        CategoryEntity(categoryId = 8801, parentId = 88, categoryName = "AI/API", iconUrl = "ic_shopping_phone", positionWeight = 8801, categoryType = 0, isCustom = true),
        CategoryEntity(categoryId = 8802, parentId = 88, categoryName = "会员订阅", iconUrl = "ic_shopping_recharge", positionWeight = 8802, categoryType = 0, isCustom = true),
        CategoryEntity(categoryId = 8803, parentId = 88, categoryName = "云服务", iconUrl = "ic_shopping_phone", positionWeight = 8803, categoryType = 0, isCustom = true),
        CategoryEntity(categoryId = 8804, parentId = 88, categoryName = "工具软件", iconUrl = "ic_shopping_phone", positionWeight = 8804, categoryType = 0, isCustom = true),

        // 餐饮自定义二级分类
        CategoryEntity(categoryId = 791, parentId = 7, categoryName = "饮品零食", iconUrl = "ic_restaurant_snacks", positionWeight = 791, categoryType = 0, isCustom = true),
        CategoryEntity(categoryId = 792, parentId = 7, categoryName = "聚餐外卖", iconUrl = "ic_restaurant_treat", positionWeight = 792, categoryType = 0, isCustom = true),

        // 交通自定义二级分类
        CategoryEntity(categoryId = 691, parentId = 6, categoryName = "共享单车", iconUrl = "ic_traffic_bus", positionWeight = 691, categoryType = 0, isCustom = true),
        CategoryEntity(categoryId = 692, parentId = 6, categoryName = "火车机票", iconUrl = "ic_traffic_train", positionWeight = 692, categoryType = 0, isCustom = true),

        // 医疗自定义二级分类
        CategoryEntity(categoryId = 191, parentId = 1, categoryName = "视力矫正", iconUrl = "ic_medical_hospital", positionWeight = 191, categoryType = 0, isCustom = true),

        // 教育自定义二级分类
        CategoryEntity(categoryId = 391, parentId = 3, categoryName = "打印资料", iconUrl = "ic_education_book", positionWeight = 391, categoryType = 0, isCustom = true),

        // 人情自定义二级分类
        CategoryEntity(categoryId = 291, parentId = 2, categoryName = "请客", iconUrl = "ic_gift_gift", positionWeight = 291, categoryType = 0, isCustom = true),
        CategoryEntity(categoryId = 292, parentId = 2, categoryName = "份子钱", iconUrl = "ic_gift_red_paper", positionWeight = 292, categoryType = 0, isCustom = true),

        // 购物消费自定义二级分类 (服饰美容包含服饰与美妆，无法唯一映射)
        CategoryEntity(categoryId = 891, parentId = 8, categoryName = "服饰美容", iconUrl = "ic_shopping_motion", positionWeight = 891, categoryType = 0, isCustom = true),

        // 收入自定义二级分类
        CategoryEntity(categoryId = 991, parentId = 9, categoryName = "生活费", iconUrl = "ic_income_subsidy", positionWeight = 991, categoryType = 1, isCustom = true),
        CategoryEntity(categoryId = 992, parentId = 9, categoryName = "平台收益", iconUrl = "ic_income_manager_money", positionWeight = 992, categoryType = 1, isCustom = true),
        CategoryEntity(categoryId = 993, parentId = 9, categoryName = "退税", iconUrl = "ic_income_subsidy", positionWeight = 993, categoryType = 1, isCustom = true),
        CategoryEntity(categoryId = 994, parentId = 9, categoryName = "退款", iconUrl = "ic_income_other", positionWeight = 994, categoryType = 1, isCustom = true),
    )

    /**
     * 将历史分类名和子分类名解析为一木分类或自定义分类映射结果。
     */
    fun resolve(category: String, subCategory: String?, isIncome: Boolean): TargetCategoryMapping {
        if (isIncome) {
            return resolveIncome(category, subCategory)
        } else {
            return resolveExpense(category, subCategory)
        }
    }

    private fun resolveIncome(category: String, subCategory: String?): TargetCategoryMapping {
        val targetName = subCategory?.takeIf { it.isNotBlank() } ?: category
        return when (targetName) {
            "工资" -> TargetCategoryMapping(9, 904, "收入", "工资", 1, isCustom = false, "ic_income_wages")
            "兼职", "兼职外快" -> TargetCategoryMapping(9, 905, "收入", "兼职外快", 1, isCustom = false, "ic_income_part_time_job")
            "理财", "理财盈利" -> TargetCategoryMapping(9, 909, "收入", "理财盈利", 1, isCustom = false, "ic_income_manager_money")
            "闲置出售", "二手闲置" -> TargetCategoryMapping(9, 903, "收入", "二手闲置", 1, isCustom = false, "ic_income_second_hand")
            "红包", "礼金人情" -> TargetCategoryMapping(9, 908, "收入", "礼金人情", 1, isCustom = false, "ic_income_cash_gift")
            "奖学金", "奖金" -> TargetCategoryMapping(9, 906, "收入", "奖金", 1, isCustom = false, "ic_income_bonus")
            "报销" -> TargetCategoryMapping(9, 901, "收入", "报销", 1, isCustom = false, "ic_income_reimbursement")
            "补贴" -> TargetCategoryMapping(9, 902, "收入", "补贴", 1, isCustom = false, "ic_income_subsidy")
            "中奖" -> TargetCategoryMapping(9, 910, "收入", "中奖", 1, isCustom = false, "ic_income_winning")
            "生活费" -> TargetCategoryMapping(9, 991, "收入", "生活费", 1, isCustom = true, "ic_income_subsidy")
            "平台收益" -> TargetCategoryMapping(9, 992, "收入", "平台收益", 1, isCustom = true, "ic_income_manager_money")
            "退税" -> TargetCategoryMapping(9, 993, "收入", "退税", 1, isCustom = true, "ic_income_subsidy")
            "退款" -> TargetCategoryMapping(9, 994, "收入", "退款", 1, isCustom = true, "ic_income_other")
            else -> TargetCategoryMapping(9, 911, "收入", "其他", 1, isCustom = false, "ic_income_other")
        }
    }

    private fun resolveExpense(category: String, subCategory: String?): TargetCategoryMapping {
        if (!subCategory.isNullOrBlank()) {
            val subMapping = resolveExpenseSubCategory(category, subCategory)
            if (subMapping != null) {
                return subMapping
            }
        }

        // 一级分类映射
        return when (category) {
            "餐饮", "食品餐饮" -> TargetCategoryMapping(7, null, "食品餐饮", null, 0, isCustom = false, "ic_parent_restaurant")
            "交通", "出行交通" -> TargetCategoryMapping(6, null, "出行交通", null, 0, isCustom = false, "ic_parent_traffic")
            "购物", "购物消费" -> TargetCategoryMapping(8, null, "购物消费", null, 0, isCustom = false, "ic_parent_shopping")
            "居住", "居家生活" -> TargetCategoryMapping(4, null, "居家生活", null, 0, isCustom = false, "ic_parent_home")
            "娱乐", "休闲娱乐" -> TargetCategoryMapping(5, null, "休闲娱乐", null, 0, isCustom = false, "ic_parent_game")
            "医疗", "健康医疗" -> TargetCategoryMapping(1, null, "健康医疗", null, 0, isCustom = false, "ic_parent_medical")
            "教育", "文化教育" -> TargetCategoryMapping(3, null, "文化教育", null, 0, isCustom = false, "ic_parent_education")
            "人情", "送礼人情" -> TargetCategoryMapping(2, null, "送礼人情", null, 0, isCustom = false, "ic_parent_gift")
            "旅行" -> TargetCategoryMapping(5, 507, "休闲娱乐", "旅游度假", 0, isCustom = false, "ic_game_travel")
            "通讯" -> TargetCategoryMapping(4, 408, "居家生活", "话费宽带", 0, isCustom = false, "ic_home_telephone_bill")
            "宠物" -> TargetCategoryMapping(8, 803, "购物消费", "宠物用品", 0, isCustom = false, "ic_shopping_pets")
            "母婴" -> TargetCategoryMapping(8, 805, "购物消费", "母婴玩具", 0, isCustom = false, "ic_shopping_baby")
            "金融" -> TargetCategoryMapping(99, 9902, "其他", "理财支出", 0, isCustom = false, "ic_r_manager_money")
            "服饰美容" -> TargetCategoryMapping(8, 891, "购物消费", "服饰美容", 0, isCustom = true, "ic_shopping_motion")
            "软件服务" -> TargetCategoryMapping(88, null, "软件服务", null, 0, isCustom = true, "ic_shopping_phone")
            "其他" -> TargetCategoryMapping(99, null, "其他", null, 0, isCustom = false, "ic_parent_other")
            else -> TargetCategoryMapping(99, null, "其他", null, 0, isCustom = false, "ic_parent_other")
        }
    }

    private fun resolveExpenseSubCategory(category: String, subCategory: String): TargetCategoryMapping? {
        return when (subCategory) {
            // 餐饮子分类
            "早餐" -> TargetCategoryMapping(7, 708, "食品餐饮", "早餐", 0, isCustom = false, "ic_restaurant_breakfast")
            "午餐" -> TargetCategoryMapping(7, 707, "食品餐饮", "午餐", 0, isCustom = false, "ic_restaurant_lunch")
            "晚餐" -> TargetCategoryMapping(7, 706, "食品餐饮", "晚餐", 0, isCustom = false, "ic_restaurant_dinner")
            "饮品零食" -> TargetCategoryMapping(7, 791, "食品餐饮", "饮品零食", 0, isCustom = true, "ic_restaurant_snacks")
            "聚餐外卖" -> TargetCategoryMapping(7, 792, "食品餐饮", "聚餐外卖", 0, isCustom = true, "ic_restaurant_treat")

            // 交通子分类
            "打车" -> TargetCategoryMapping(6, 607, "出行交通", "打车", 0, isCustom = false, "ic_traffic_taxi")
            "公交地铁", "公共交通" -> TargetCategoryMapping(6, 606, "出行交通", "公共交通", 0, isCustom = false, "ic_traffic_bus")
            "共享单车" -> TargetCategoryMapping(6, 691, "出行交通", "共享单车", 0, isCustom = true, "ic_traffic_bus")
            "火车机票" -> TargetCategoryMapping(6, 692, "出行交通", "火车机票", 0, isCustom = true, "ic_traffic_train")

            // 医疗子分类
            "日常买药", "买药" -> TargetCategoryMapping(1, 101, "健康医疗", "买药", 0, isCustom = false, "ic_medical_medicine")
            "常规就医", "医院" -> TargetCategoryMapping(1, 102, "健康医疗", "医院", 0, isCustom = false, "ic_medical_hospital")
            "视力矫正" -> TargetCategoryMapping(1, 191, "健康医疗", "视力矫正", 0, isCustom = true, "ic_medical_hospital")

            // 教育子分类
            "培训考试" -> TargetCategoryMapping(3, 301, "文化教育", "培训考试", 0, isCustom = false, "ic_education_train")
            "学杂费", "学费" -> TargetCategoryMapping(3, 303, "文化教育", "学费", 0, isCustom = false, "ic_education_tuition")
            "教材书籍", "书报杂志" -> TargetCategoryMapping(3, 302, "文化教育", "书报杂志", 0, isCustom = false, "ic_education_book")
            "打印资料" -> TargetCategoryMapping(3, 391, "文化教育", "打印资料", 0, isCustom = true, "ic_education_book")

            // 人情子分类
            "红包" -> TargetCategoryMapping(2, 202, "送礼人情", "红包", 0, isCustom = false, "ic_gift_red_paper")
            "礼物" -> TargetCategoryMapping(2, 204, "送礼人情", "礼物", 0, isCustom = false, "ic_gift_gift")
            "请客" -> TargetCategoryMapping(2, 291, "送礼人情", "请客", 0, isCustom = true, "ic_gift_gift")
            "份子钱" -> TargetCategoryMapping(2, 292, "送礼人情", "份子钱", 0, isCustom = true, "ic_gift_red_paper")

            // 软件服务子分类
            "AI/API" -> TargetCategoryMapping(88, 8801, "软件服务", "AI/API", 0, isCustom = true, "ic_shopping_phone")
            "会员订阅" -> TargetCategoryMapping(88, 8802, "软件服务", "会员订阅", 0, isCustom = true, "ic_shopping_recharge")
            "云服务" -> TargetCategoryMapping(88, 8803, "软件服务", "云服务", 0, isCustom = true, "ic_shopping_phone")
            "工具软件" -> TargetCategoryMapping(88, 8804, "软件服务", "工具软件", 0, isCustom = true, "ic_shopping_phone")

            else -> null
        }
    }
}
