package com.zkjd.lingdong.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

data class LedColor(
    val color: Color,
    val name: String
) {
    fun toHex(): String = String.format("#%06X", (0xFFFFFF and color.toArgb()))

    companion object {
        val RED = LedColor(Color(0xFFFF0000), "红色")
        val GREEN = LedColor(Color(0xFF00FF00), "绿色")
        val BLUE = LedColor(Color(0xFF0000FF), "蓝色")
        val YELLOW = LedColor(Color(0xFFFFFF00), "黄色")
        val CYAN = LedColor(Color(0xFF00FFFF), "青色")
        val MAGENTA = LedColor(Color(0xFFFF00FF), "品红")
        val BLUE2 = LedColor(Color(0xFFC400FF), "紫色")
        val ORANGE = LedColor(Color(0xFFFF6A00), "橙色")

        val DEFAULT_COLORS = listOf(RED, GREEN, BLUE, YELLOW, CYAN, MAGENTA, BLUE2, ORANGE)
        
        /**
         * 从十六进制字符串创建LedColor对象
         */
        fun fromHex(hexColor: String): LedColor {
            val colorString = if (hexColor.startsWith("#")) hexColor else "#$hexColor"
            try {
                val colorInt = android.graphics.Color.parseColor(colorString)
                return LedColor(Color(colorInt), "自定义")
            } catch (e: Exception) {
                throw IllegalArgumentException("无效的颜色格式: $hexColor")
            }
        }
    }
} 