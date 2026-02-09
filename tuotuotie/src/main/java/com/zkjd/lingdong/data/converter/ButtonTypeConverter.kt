package com.zkjd.lingdong.data.converter

import androidx.room.TypeConverter
import com.zkjd.lingdong.model.ButtonType

class ButtonTypeConverter {
    @TypeConverter
    fun fromButtonType(buttonType: ButtonType): String {
        return buttonType.name
    }

    @TypeConverter
    fun toButtonType(value: String): ButtonType {
        return ButtonType.valueOf(value)
    }
} 