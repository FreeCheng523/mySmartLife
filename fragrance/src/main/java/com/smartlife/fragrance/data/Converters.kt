package com.smartlife.fragrance.data

import androidx.room.TypeConverter
import com.smartlife.fragrance.data.model.CarStartStopCycle
import com.smartlife.fragrance.data.model.Gear
import com.smartlife.fragrance.data.model.LightMode
import com.smartlife.fragrance.data.model.Mode
import com.smartlife.fragrance.data.model.PowerState

/**
 * Room数据库类型转换器
 * 用于将枚举类型转换为数据库存储的Int类型
 */
class Converters {
    
    // PowerState 转换器
    @TypeConverter
    fun fromPowerState(powerState: PowerState): Int {
        return powerState.value
    }
    
    @TypeConverter
    fun toPowerState(value: Int): PowerState {
        return PowerState.fromValue(value)
    }
    
    // Mode 转换器
    @TypeConverter
    fun fromMode(mode: Mode): Int {
        return mode.value
    }
    
    @TypeConverter
    fun toMode(value: Int): Mode {
        return Mode.fromValue(value)
    }
    
    // Gear 转换器
    @TypeConverter
    fun fromGear(gear: Gear): Int {
        return gear.value
    }
    
    @TypeConverter
    fun toGear(value: Int): Gear {
        return Gear.fromValue(value)
    }
    
    // CarStartStopCycle 转换器
    @TypeConverter
    fun fromCarStartStopCycle(cycle: CarStartStopCycle): Int {
        return cycle.value
    }
    
    @TypeConverter
    fun toCarStartStopCycle(value: Int): CarStartStopCycle {
        return CarStartStopCycle.fromValue(value)
    }
    
    // LightMode 转换器
    @TypeConverter
    fun fromLightMode(lightMode: LightMode): Int {
        return lightMode.value
    }
    
    @TypeConverter
    fun toLightMode(value: Int): LightMode {
        return LightMode.fromValue(value)
    }
}

