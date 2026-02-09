package com.mine.baselibrary.window

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.WindowManager


fun getWindowSize(context: Context): Array<Int> {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // 使用现代 WindowMetrics API (Android 11+)
        val windowMetrics = windowManager.currentWindowMetrics
        val bounds = windowMetrics.bounds
        arrayOf(bounds.width(), bounds.height())
    } else {
        // 兼容旧版本 Android
        val display =
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        val size = Point()
        @Suppress("DEPRECATION")
        display?.getSize(size)
        arrayOf(size.x, size.y)
    }
}

fun getRealWindowSize(context: Context): Array<Int> {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // 使用现代 WindowMetrics API (Android 11+)
        val windowMetrics = windowManager.currentWindowMetrics
        val bounds = windowMetrics.bounds
        arrayOf(bounds.width(), bounds.height())
    } else {
        // 兼容旧版本 Android
        val display =
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        val size = Point()
        @Suppress("DEPRECATION")
        display?.getRealSize(size)
        arrayOf(size.x, size.y)
    }
}