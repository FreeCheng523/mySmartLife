package com.zkjd.lingdong.data.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zkjd.lingdong.model.ButtonFunction

/**
 * ButtonFunction类型转换器，用于Room数据库
 */
class ButtonFunctionConverter {
    private val gson = Gson()
    
    @TypeConverter
    fun fromButtonFunction(buttonFunction: ButtonFunction?): String? {
        return if (buttonFunction == null) null else gson.toJson(buttonFunction)
    }
    
    @TypeConverter
    fun toButtonFunction(json: String?): ButtonFunction? {
        if (json == null) return null
        val type = object : TypeToken<ButtonFunction>() {}.type
        return gson.fromJson(json, type)
    }
} 