package com.smartlife.fragrance.data.model

/**
 * 模式枚举
 */
enum class Mode(val value: Int) {
    STANDBY(0x01),    // 待机
    FRAGRANCE_A(0x02), // 香型A
    FRAGRANCE_B(0x03); // 香型B
    
    companion object {
        fun fromValue(value: Int): Mode {
            return values().find { it.value == value } ?: STANDBY
        }
    }
}

