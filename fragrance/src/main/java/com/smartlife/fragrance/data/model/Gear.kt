package com.smartlife.fragrance.data.model

/**
 * 档位枚举
 */
enum class Gear(val value: Int) {
    LOW(0x01),      // 低档
    MEDIUM(0x02),   // 中档
    HIGH(0x03);     // 高档
    
    companion object {
        fun fromValue(value: Int): Gear {
            return values().find { it.value == value } ?: LOW
        }
    }
}

