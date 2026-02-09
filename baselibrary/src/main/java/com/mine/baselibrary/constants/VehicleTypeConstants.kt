package com.mine.baselibrary.constants

import com.mine.baselibrary.BuildConfig

/**
 * 车型类型常量
 */
object VehicleTypeConstants {
    // 车型类型常量定义，从 BuildConfig 读取，保持单一数据源
    // 518, 673-7, 673-g3, 673-g5-hurry-change, 673-g3-hurry-change
    const val tinnove_8678_d4 = BuildConfig.tinnove_8678_d4
    const val tinnove_8155_d3 = BuildConfig.tinnove_8155_d3
    const val mega_8155_d3 = BuildConfig.mega_8155_d3
    const val mega_8155_g3 = BuildConfig.mega_8155_g3
    const val mega_8295_d4 = BuildConfig.mega_8295_d4

    // 直接从 BuildConfig 获取当前车型类型
    const val CURRENT_VEHICLE_TYPE = BuildConfig.CURRENT_VEHICLE_TYPE

    // 车型类型判断辅助方法
    val is_tinnove_8678_d4: Boolean get() = CURRENT_VEHICLE_TYPE == tinnove_8678_d4
    val is_tinnove_8155_d3: Boolean get() = CURRENT_VEHICLE_TYPE == tinnove_8155_d3
    val is_mega_8155_d3: Boolean get() = CURRENT_VEHICLE_TYPE == mega_8155_d3
    val is_mega_8155_g3: Boolean get() = CURRENT_VEHICLE_TYPE == mega_8155_g3
    val is_mega_8295_d4: Boolean get() = CURRENT_VEHICLE_TYPE == mega_8295_d4

    val isMega = is_mega_8155_d3 || is_mega_8155_g3 || is_mega_8295_d4

    val isMega8155 = is_mega_8155_d3 || is_mega_8155_g3

    val isMega8295 = is_mega_8295_d4

    val isTinnove = is_tinnove_8678_d4 || is_tinnove_8155_d3

    val enableFragrance = BuildConfig.enable_fragrance == "1"
}

