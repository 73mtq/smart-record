package com.nonen.Bookkeeping.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YimuTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors? = null,
) {
    Row(modifier.fillMaxWidth().semantics { testTagsAsResourceId = true }.testTag("tool_bar")
        .background(MaterialTheme.colorScheme.surface).statusBarsPadding().height(50.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(50.dp), contentAlignment = Alignment.Center) { navigationIcon() }
        Box(Modifier.weight(1f)) {
            ProvideTextStyle(TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface), content = title)
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}
