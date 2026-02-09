package com.zkjd.lingdong.ui.theme

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

// 浅色主题颜色方案
private val LightColorScheme = lightColorScheme(
    primary = Green500,
    onPrimary = Color.White,
    primaryContainer = Green100,
    onPrimaryContainer = Green900,
    secondary = Blue500,
    onSecondary = Color.White,
    secondaryContainer = Blue100,
    onSecondaryContainer = Blue900,
    tertiary = Orange500,
    onTertiary = Color.White,
    tertiaryContainer = Orange100,
    onTertiaryContainer = Orange900,
    error = Red500,
    onError = Color.White,
    errorContainer = Red100,
    onErrorContainer = Red900,
    background = Color.White,
    onBackground = Gray900,
    surface = Color.White,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray700
)

// 深色主题颜色方案
private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = Secondary,
    background = DarkBackground,
    surface = CardBackground,
    error = Error,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onError = TextPrimary
)

@Composable
fun LingDong2Theme(
    darkTheme: Boolean = true, // 默认使用暗色主题
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> DarkColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackground.toArgb()
            window.decorView.systemUiVisibility= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    //屏幕适配
    val fontScale = LocalDensity.current.fontScale
    val appDensity = Density(density = dynamicDensity(460F, 740F), fontScale = fontScale)


    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            CompositionLocalProvider(LocalDensity provides appDensity ) {
                content()
            }
        }

        //content = content
    )
}

/**
 * 根据UI设计图得出动态密度适配不同屏幕
 * @param designWidth 填入UI设计图的屏幕短边dp值（绝对宽度）
 * @param designHeight 填入UI设计图的屏幕长边dp值（绝对高度）
 */
@Composable
private fun dynamicDensity(designWidth: Float, designHeight: Float): Float {
    val displayMetrics = LocalContext.current.resources.displayMetrics
    val widthPixels = displayMetrics.widthPixels    //屏幕短边像素（绝对宽度）
    val heightPixels = displayMetrics.heightPixels  //屏幕长边像素（绝对高度）
    val isPortrait = LocalContext.current.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT  //判断横竖屏
    return if (isPortrait) widthPixels / designWidth else heightPixels / designHeight //计算密度
}