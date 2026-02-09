package com.zkjd.lingdong.data

import androidx.room.TypeConverter
import com.zkjd.lingdong.model.ButtonType
import com.zkjd.lingdong.model.ConnectionState
import com.zkjd.lingdong.model.FunctionCategory

/**
 * Room数据库类型转换器
 */
class Converters {
    
    @TypeConverter
    fun fromButtonType(value: ButtonType): String {
        return value.name
    }
    
    @TypeConverter
    fun toButtonType(value: String): ButtonType {
        return ButtonType.valueOf(value)
    }
    
    @TypeConverter
    fun fromFunctionCategory(value: FunctionCategory): String {
        return value.name
    }
    
    @TypeConverter
    fun toFunctionCategory(value: String): FunctionCategory {
        return FunctionCategory.valueOf(value)
    }
    
    @TypeConverter
    fun fromConnectionState(value: ConnectionState): String {
        return value.name
    }
    
    @TypeConverter
    fun toConnectionState(value: String): ConnectionState {
        return ConnectionState.valueOf(value)
    }
} 