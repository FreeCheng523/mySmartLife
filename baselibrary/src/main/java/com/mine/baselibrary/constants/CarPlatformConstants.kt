package com.mine.baselibrary.constants

import com.mine.baselibrary.BuildConfig

/**
 * 车机平台类型常量
 */
object CarPlatformConstants {

    //从BuildConfig 获取当前是否包含小仪表
    val isIncludeInstrumentPanel: Boolean get() = BuildConfig.INCLUDE_INSTRUMENT_PANEL
}

