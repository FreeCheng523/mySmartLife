package com.smartlife.fragrance.data.model

/**
 * 随车启停周期枚举
 */
enum class CarStartStopCycle(val value: Int) {
    CYCLE_1(0x01),  // 5分钟工作/10分钟停止
    CYCLE_2(0x02),  // 5分钟工作/20分钟停止
    CYCLE_3(0x03);  // 5分钟工作/30分钟停止
    
    companion object {
        fun fromValue(value: Int): CarStartStopCycle {
            return values().find { it.value == value } ?: CYCLE_1
        }
    }
}

