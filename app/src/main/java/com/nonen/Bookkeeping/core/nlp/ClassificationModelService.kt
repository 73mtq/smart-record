package com.nonen.Bookkeeping.core.nlp

import android.content.Context
import com.hankcs.hanlp.HanLP
import com.hankcs.hanlp.seg.common.Term
import com.hankcs.hanlp.tokenizer.StandardTokenizer
import com.nonen.Bookkeeping.core.CategoryMapping
import com.nonen.Bookkeeping.core.DefaultCategories
import com.wangc.bookkeeping.test.LinearSVMModel
import de.bwaldvogel.liblinear.FeatureNode
import de.bwaldvogel.liblinear.Linear
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.ObjectInputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.regex.Pattern
import java.util.zip.GZIPInputStream

/**
 * 离线自然语言速记识别结果。
 */
data class QuickRecordResult(
    val amount: Double?,
    val isIncome: Boolean,
    val category: String,
    val subCategory: String?,
    val remark: String,
    val dateTime: LocalDateTime,
    val confidence: Double,
    val rawText: String,
)

/**
 * 本地分类模型与离线自然语言处理服务。
 */
class ClassificationModelService(
    val svmModel: LinearSVMModel,
    val timePattern: Pattern?,
) {

    /**
     * 对输入文本进行多标签概率预测。
     * 返回从类别全称（如 "食品餐饮-早餐"）到置信度概率的映射。
     */
    fun predict(text: String?): Map<String, Double> {
        if (text.isNullOrBlank()) return emptyMap()

        // 1. 中文分词获取词条
        val words = segment(text)
        if (words.isEmpty()) return emptyMap()

        // 2. 统计在 wordIdTrie 中的词频
        val trie = svmModel.wordIdTrie
        val tfMap = mutableMapOf<Int, Int>()
        for (word in words) {
            val id = trie?.get(word.toCharArray())
            if (id != null) {
                tfMap[id] = (tfMap[id] ?: 0) + 1
            }
        }
        if (tfMap.isEmpty()) {
            // 若整词未命中，尝试字符级别匹配
            for (ch in text) {
                val id = trie?.get(charArrayOf(ch))
                if (id != null) {
                    tfMap[id] = (tfMap[id] ?: 0) + 1
                }
            }
        }
        if (tfMap.isEmpty()) return emptyMap()

        // 3. 构建 TF-IDF 特征向量并进行 L2 归一化
        val weighter = svmModel.featureWeighter
        val nodes = tfMap.entries.map { (wordId, count) ->
            val weight = weighter?.weight(wordId, count) ?: 0.0
            FeatureNode(wordId + 1, weight)
        }.sortedBy { it.index }.toTypedArray()

        var sumSq = 0.0
        for (node in nodes) {
            sumSq += node.value * node.value
        }
        val norm = Math.sqrt(sumSq)
        if (norm > 0.0) {
            for (node in nodes) {
                node.value /= norm
            }
        }

        // 4. 调用 LibLinear 进行概率推断
        val nrClass = svmModel.svmModel?.nrClass ?: 0
        if (nrClass <= 0) return emptyMap()

        val probEstimates = DoubleArray(nrClass)
        Linear.predictProbability(svmModel.svmModel, nodes, probEstimates)

        // 5. 将推断概率映射到类别标签
        val catalog = svmModel.catalog ?: return emptyMap()
        val resultMap = mutableMapOf<String, Double>()

        for (i in 0 until minOf(catalog.size, nrClass)) {
            val categoryName = catalog[i]
            resultMap[categoryName] = probEstimates[i]
        }

        return resultMap
    }

    /**
     * 解析自然语言记账文本，抽取金额、收支方向、时间、分类及备注。
     */
    fun parseQuickRecord(
        input: String,
        baseDateTime: LocalDateTime = LocalDateTime.now(),
    ): QuickRecordResult {
        var text = input.trim()
        val raw = text

        // 1. 抽取完整语句中的全部时间表达片段
        var parsedDateTime = baseDateTime
        val timePat = timePattern
        if (timePat != null) {
            val nounRanges = HanLP.newSegment().enableOffset(true).seg(text)
                .filter { it.nature.toString().startsWith("n") }
                .map { it.offset until (it.offset + it.word.length) }
            val matcher = timePat.matcher(text)
            data class MatchRange(val start: Int, val end: Int, val matched: String)
            val matches = mutableListOf<MatchRange>()
            while (matcher.find()) {
                val group = matcher.group()
                if (!group.isNullOrBlank()) {
                    // 时间表达式必须保留名词内部的文字，例如早餐、午饭。
                    val insideNoun = nounRanges.any { range ->
                        matcher.start() >= range.first && matcher.end() <= range.last + 1 &&
                            matcher.end() - matcher.start() < range.count()
                    }
                    if (insideNoun) continue
                    matches.add(MatchRange(matcher.start(), matcher.end(), group))
                }
            }

            if (matches.isNotEmpty()) {
                // 将匹配到的时间表达组合供 resolveTimeExp 解析
                val timeTokens = mutableListOf<String>()
                for (m in matches) {
                    val tok = m.matched.trim()
                    if (tok == "半" && timeTokens.isNotEmpty()) {
                        val lastIdx = timeTokens.size - 1
                        timeTokens[lastIdx] = timeTokens[lastIdx] + "半"
                    } else {
                        timeTokens.add(tok)
                    }
                }
                val combinedTimeExp = timeTokens.joinToString(" ")
                parsedDateTime = resolveTimeExp(combinedTimeExp, baseDateTime)

                // 从待分析文本中彻底剥离所有时间片段，避免时间数字干扰金额识别
                val sb = java.lang.StringBuilder(text)
                for (m in matches.reversed()) {
                    sb.replace(m.start, m.end, " ")
                }
                text = sb.toString().replace(Regex("\\s+"), " ").trim()
            }
        }

        // 2. 金额与收支方向抽取
        var amount: Double? = null
        var isIncome = false

        // 优先判断显式收支关键词
        if (text.contains("收入") || text.contains("赚了") || text.contains("发工资") ||
            text.contains("兼职收入") || text.contains("到账") || text.contains("报销入账")
        ) {
            isIncome = true
        }

        // 提取金额数字（包含前缀或后缀：元/块/￥/rmb）
        val moneyRegex = Pattern.compile("(?<!\\d)(\\d+(?:\\.\\d{1,2})?)\\s*(?:元|块|圆)?")
        val moneyMatcher = moneyRegex.matcher(text)
        var matchedMoneyText = ""
        while (moneyMatcher.find()) {
            val numStr = moneyMatcher.group(1)
            val parsedVal = numStr?.toDoubleOrNull()
            if (parsedVal != null && parsedVal > 0.0) {
                amount = parsedVal
                matchedMoneyText = moneyMatcher.group(0).orEmpty()
            }
        }

        // 3. 剥离金额与常见动词，整理最终备注
        var cleanRemark = text
        if (matchedMoneyText.isNotBlank()) {
            cleanRemark = cleanRemark.replace(matchedMoneyText, " ")
        }
        cleanRemark = cleanRemark
            .replace("花了", " ")
            .replace("用了", " ")
            .replace("支出", " ")
            .replace("买", " ")
            .replace("消费", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (cleanRemark.isBlank()) {
            cleanRemark = raw
        }

        // 4. 使用本地模型进行分类预测
        val predictions = predict(cleanRemark.ifBlank { raw })
        var topCategoryFull = "其他-其他"
        var maxProb = 0.0

        for ((cat, prob) in predictions) {
            if (prob > maxProb) {
                maxProb = prob
                topCategoryFull = cat
            }
        }
        if (maxProb < 0.015) {
            topCategoryFull = "其他-其他"
        }

        // 食品餐饮特殊时间细分
        if (topCategoryFull == "食品餐饮-其他" || topCategoryFull == "食品餐饮") {
            val hour = parsedDateTime.hour
            topCategoryFull = when {
                hour in 4..9 -> "食品餐饮-早餐"
                hour in 10..15 -> "食品餐饮-午餐"
                else -> "食品餐饮-晚餐"
            }
        }

        // 判断预测类别是否归属收入
        if (topCategoryFull.startsWith("收入-")) {
            isIncome = true
        }

        // 拆分为一级与二级分类
        val parts = topCategoryFull.split("-")
        val parentCat = parts.getOrNull(0) ?: if (isIncome) "收入" else "其他"
        val childCat = parts.getOrNull(1)

        // 映射到应用内部的标准分类名
        val mapped = CategoryMapping.resolve(parentCat, childCat, isIncome)

        return QuickRecordResult(
            amount = amount,
            isIncome = isIncome,
            category = mapped.parentName,
            subCategory = mapped.childName,
            remark = cleanRemark,
            dateTime = parsedDateTime,
            confidence = maxProb,
            rawText = raw,
        )
    }

    private fun segment(text: String): List<String> {
        val list = StandardTokenizer.segment(text)
        val result = mutableListOf<String>()
        for (term in list) {
            val word = term.word
            if (!word.isNullOrEmpty() && word.indexOf('\u0000') < 0) {
                result.add(word)
            }
        }
        return result
    }

    companion object {
        @Volatile
        private var instance: ClassificationModelService? = null

        /**
         * 获取单例服务（已初始化则直接返回）。
         */
        fun getInstance(): ClassificationModelService? = instance

        /**
         * 从 Context 资产目录中初始化加载本地模型与时间表达式。
         */
        @Synchronized
        fun initializeFromAssets(context: Context): ClassificationModelService {
            instance?.let { return it }

            val model = loadModelFromStream(context.assets.open("classification-model.ser"))
            val pattern = loadPatternFromStream(context.assets.open("TimeExp.m"))

            return ClassificationModelService(model, pattern).also {
                instance = it
            }
        }

        /**
         * 从输入流加载 LinearSVMModel。
         */
        fun loadModelFromStream(inputStream: InputStream): LinearSVMModel {
            BufferedInputStream(inputStream).use { bis ->
                ObjectInputStream(bis).use { ois ->
                    return ois.readObject() as LinearSVMModel
                }
            }
        }

        /**
         * 从输入流加载 TimeExp.m 正则对象。
         */
        fun loadPatternFromStream(inputStream: InputStream): Pattern {
            BufferedInputStream(inputStream).use { bis ->
                GZIPInputStream(bis).use { gzis ->
                    ObjectInputStream(gzis).use { ois ->
                        return ois.readObject() as Pattern
                    }
                }
            }
        }

        /**
         * 时间词汇相对解析，支持具体点分及相对日期/时段。
         */
        fun resolveTimeExp(timeExp: String, base: LocalDateTime): LocalDateTime {
            val today = base.toLocalDate()
            var date = today

            // 1. 优先匹配长词日期
            when {
                timeExp.contains("大前天") -> date = today.minusDays(3)
                timeExp.contains("大后天") -> date = today.plusDays(3)
                timeExp.contains("前天") -> date = today.minusDays(2)
                timeExp.contains("后天") -> date = today.plusDays(2)
                timeExp.contains("昨天") -> date = today.minusDays(1)
                timeExp.contains("明天") -> date = today.plusDays(1)
                timeExp.contains("今天") -> date = today
            }

            // 2. 尝试提取具体几点几分
            val pointRegex = Pattern.compile("([上下]午|中午|晚上|夜里|深夜|清晨|凌晨)?\\s*(\\d{1,2})\\s*(?:点|时|:)\\s*(?:(\\d{1,2})|半)?(?:\\s*分)?")
            val matcher = pointRegex.matcher(timeExp)
            if (matcher.find()) {
                val period = matcher.group(1) ?: ""
                var hour = matcher.group(2)?.toIntOrNull() ?: 12
                val minuteStr = matcher.group(3)
                val minute = when {
                    matcher.group(0)?.contains("半") == true -> 30
                    minuteStr != null -> minuteStr.toIntOrNull() ?: 0
                    else -> 0
                }

                if ((period.contains("下午") || period.contains("晚上") || period.contains("夜里") || period.contains("深夜")) && hour < 12) {
                    hour += 12
                } else if ((period.contains("上午") || period.contains("早") || period.contains("凌晨")) && hour == 12) {
                    hour = 0
                }

                val parsedTime = try {
                    LocalTime.of(hour, minute)
                } catch (_: java.time.DateTimeException) {
                    null
                }
                if (parsedTime != null) {
                    return LocalDateTime.of(date, parsedTime)
                }
            }

            // 3. 泛时段模糊归纳
            var time = base.toLocalTime()
            when {
                timeExp.contains("下午") -> time = LocalTime.of(15, 30)
                timeExp.contains("夜宵") || timeExp.contains("深夜") -> time = LocalTime.of(22, 30)
                timeExp.contains("晚上") || timeExp.contains("晚") -> time = LocalTime.of(19, 0)
                timeExp.contains("中午") || timeExp.contains("正午") -> time = LocalTime.of(12, 15)
                timeExp.contains("早") || timeExp.contains("上午") || timeExp.contains("清晨") -> time = LocalTime.of(8, 30)
                timeExp.contains("午") -> time = LocalTime.of(12, 15)
            }

            return LocalDateTime.of(date, time)
        }
    }
}
