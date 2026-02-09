package com.smartlife.fragrance.data.model

/**
 * 是否待机
 */
enum class PowerState(val value: Int) {
    ON(0x01),      // 开
    OFF(0x02);     // 关
    
    companion object {
        fun fromValue(value: Int): PowerState {
            return values().find { it.value == value } ?: OFF
        }
    }
}

