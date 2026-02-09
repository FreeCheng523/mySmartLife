package com.zkjd.lingdong.data

import com.zkjd.lingdong.R
import com.zkjd.lingdong.model.ButtonFunction
import com.zkjd.lingdong.model.FunctionCategory

/**
 * 预设功能数据类，提供应用支持的所有功能选项
 */
object DefaultFunctions {
    
    // 媒体控制功能
    val MEDIA_FUNCTIONS = listOf(
        ButtonFunction(
            id = "media_play_pause",
            category = FunctionCategory.MEDIA,
            name = "播放/暂停",
            actionCode = "MEDIA_PLAY_PAUSE",
            iconResId = R.drawable.ic_play_pause,
            configWords = ""
        ),
        ButtonFunction(
            id = "media_next",
            category = FunctionCategory.MEDIA,
            name = "下一曲",
            actionCode = "MEDIA_NEXT",
            iconResId = R.drawable.ic_next_track,
            configWords = ""
        ),
        ButtonFunction(
            id = "media_previous",
            category = FunctionCategory.MEDIA,
            name = "上一曲",
            actionCode = "MEDIA_PREVIOUS",
            iconResId = R.drawable.ic_previous_track,
            configWords = ""
        ),
        ButtonFunction(
            id = "media_volume_up",
            category = FunctionCategory.MEDIA,
            name = "增大音量",
            actionCode = "MEDIA_VOLUME_UP",
            iconResId = R.drawable.ic_volume_up,
            configWords = ""
        ),
        ButtonFunction(
            id = "media_volume_down",
            category = FunctionCategory.MEDIA,
            name = "减小音量",
            actionCode = "MEDIA_VOLUME_DOWN",
            iconResId = R.drawable.ic_volume_down,
            configWords = ""
        ),
        ButtonFunction(
            id = "media_mute",
            category = FunctionCategory.MEDIA,
            name = "静音",
            actionCode = "MEDIA_MUTE",
            iconResId = R.drawable.ic_mute,
            configWords = ""
        ),
        ButtonFunction(
            id = "media_switch_source",
            category = FunctionCategory.MEDIA,
            name = "切换音源",
            actionCode = "MEDIA_SWITCH_SOURCE",
            iconResId = R.drawable.ic_switch_source,
            configWords = ""
        )
    )
    
    // 应用启动功能 - 这些将从系统动态获取
    val APP_FUNCTIONS = mutableListOf<ButtonFunction>()
    
    // 获取所有功能列表
    fun getAllFunctions(): List<ButtonFunction> {
        return MEDIA_FUNCTIONS +
               APP_FUNCTIONS
    }
    
    // 根据功能ID查找功能
    fun getFunctionById(id: String): ButtonFunction? {
        return getAllFunctions().find { it.id == id }
    }
    
}