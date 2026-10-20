package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import com.nonen.Bookkeeping.core.CategoryMapping

class CategoryIconCatalog(context: Context) {
    val entries = context.assets.open("yimu-category-icons.json").bufferedReader().use { JSONObject(it.readText()) }
    fun names(parent: String, child: String?, isIncome: Boolean): Pair<String, String?> {
        val key = if (child.isNullOrBlank()) parent else "$parent/$child"
        if (entries.has(key)) return parent to child
        val mapped = CategoryMapping.resolve(parent, child, isIncome)
        if (!child.isNullOrBlank()) {
            val candidates = entries.keys().asSequence().filter {
                it.endsWith("/$child") && it.startsWith("收入/") == isIncome
            }.toList()
            if (candidates.size == 1) return candidates.single().substringBefore('/') to child
        }
        return mapped.parentName to (child?.takeIf { entries.has("${mapped.parentName}/$it") } ?: mapped.childName)
    }

    fun get(parent: String, child: String?, isIncome: Boolean, iconUrl: String?): String {
        val (parentName, childName) = names(parent, child, isIncome)
        val key = if (childName.isNullOrBlank()) parentName else "$parentName/$childName"
        return if (entries.has(key)) entries.getString(key) else requireNotNull(iconUrl) { "category icon is required: $key" }
    }
}

@Composable
fun YimuCategoryIcon(parent: String, child: String? = null, modifier: Modifier = Modifier, isIncome: Boolean = false, iconUrl: String? = null) {
    val context = LocalContext.current
    val catalog = remember(context) { CategoryIconCatalog(context) }
    val name = catalog.get(parent, child, isIncome || parent == "收入", iconUrl)
    val tint = MaterialTheme.colorScheme.onSurface
    when {
        name.startsWith("font:") -> Text(name.removePrefix("font:"), modifier, fontSize = 18.sp)
        name.startsWith("lineIcon/") -> {
            val bitmap = remember(name) { context.assets.open("yimu/$name").use { requireNotNull(BitmapFactory.decodeStream(it)).asImageBitmap() } }
            Image(bitmap, null, modifier, colorFilter = ColorFilter.tint(tint))
        }
        else -> {
            val id = remember(name) { context.resources.getIdentifier(name, "mipmap", context.packageName).also { require(it != 0) } }
            Image(painterResource(id), null, modifier, colorFilter = ColorFilter.tint(tint))
        }
    }
}
