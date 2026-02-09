package com.smartlife.fragrance.data.model

/**
 * 灯光模式枚举
 */
enum class LightMode(val value: Int) {
    OFF(0x00),      // 关闭
    BREATH(0x01),   // 呼吸
    RHYTHM(0x02),   // 律动
    FLOW(0x03),     // 流动
    ALWAYS_ON(0x04); // 常亮
    
    companion object {
        fun fromValue(value: Int): LightMode {
            return values().find { it.value == value } ?: OFF
        }
    }
}

