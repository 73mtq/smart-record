package com.nonen.Bookkeeping.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.text.PlatformTextStyle
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = YimuTokens.Primary,
    onPrimary = Color.White,
    primaryContainer = YimuTokens.PrimaryAlpha10,
    onPrimaryContainer = YimuTokens.PrimaryDark,
    secondary = YimuTokens.PrimaryDark,
    onSecondary = Color.White,
    secondaryContainer = YimuTokens.PrimaryAlpha10,
    onSecondaryContainer = YimuTokens.PrimaryDark,
    tertiary = YimuTokens.PrimaryLight,
    background = YimuTokens.Background,
    onBackground = YimuTokens.TextPrimary,
    surface = YimuTokens.Surface,
    onSurface = YimuTokens.TextPrimary,
    surfaceVariant = YimuTokens.Background,
    onSurfaceVariant = YimuTokens.TextSecondary,
    outline = YimuTokens.Divider,
    error = YimuTokens.Pay,
)

private val DarkColors = darkColorScheme(
    primary = YimuTokens.Primary,
    onPrimary = Color.White,
    primaryContainer = YimuTokens.NightPrimary,
    onPrimaryContainer = YimuTokens.Primary,
    secondary = YimuTokens.PrimaryLight,
    onSecondary = YimuTokens.NightBackground,
    secondaryContainer = YimuTokens.NightPrimary,
    onSecondaryContainer = YimuTokens.Primary,
    tertiary = YimuTokens.PrimaryLight,
    background = YimuTokens.NightBackground,
    onBackground = Color.White,
    surface = YimuTokens.NightSurface,
    onSurface = Color.White,
    surfaceVariant = YimuTokens.NightSurface,
    onSurfaceVariant = YimuTokens.NightText,
    outline = YimuTokens.NightDivider,
    error = YimuTokens.Pay,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(YimuTokens.CommonRadius),
    medium = RoundedCornerShape(YimuTokens.CommonRadius),
    large = RoundedCornerShape(YimuTokens.CommonRadius),
    extraLarge = RoundedCornerShape(12.dp),
)

@Composable
fun BookkeepingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor =
                (if (darkTheme) YimuTokens.NightBackground else Color.White).toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = {
            ProvideTextStyle(MaterialTheme.typography.bodyLarge.copy(
                lineHeight = TextUnit.Unspecified,
                letterSpacing = 0.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = true),
            ), content = content)
        },
    )
}
