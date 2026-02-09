package com.zkjd.lingdong.service.executor

import android.content.Context
import com.zkjd.lingdong.model.ButtonFunction
import com.zkjd.lingdong.model.ButtonType
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import com.example.tuotuotie_car_interface_library.ICarMediaExecutor
import dagger.hilt.android.qualifiers.ApplicationContext

private const val TAG = "CarMediaFunctionExecutor"

/**
 * 车载媒体功能执行器
 * 使用 CarAudioManager 处理媒体控制相关功能，专为车载环境设计
 */
@Singleton
class CarMediaExecutorImp @Inject constructor(
    @ApplicationContext private val context: Context,
    private val carMediaExecutor: ICarMediaExecutor
) : ICarMediaExecutor by carMediaExecutor{

    /**
     * 执行媒体相关功能
     * @param function 要执行的功能
     * @param buttonType 触发的按键类型，用于处理旋转功能
     */
    fun executeMediaFunction(function: ButtonFunction, buttonType: ButtonType = ButtonType.SHORT_PRESS) {
        Timber.tag(TAG)
            .d("执行车载媒体功能: ${function.name}, 代码: ${function.actionCode}, 按键类型: $buttonType")
        
        ensureCarAudioManagerOnChildThread()
        
        ensureCarAudioManager()

        val array=function.useType.toString().split("")
        
        // 处理旋转类功能
        if (array[1].toInt()==2 &&
           (buttonType == ButtonType.LEFT_ROTATE || buttonType == ButtonType.RIGHT_ROTATE)) {
            when (function.actionCode) {
                "MEDIA_VOLUME_CONTROL" -> {
                    // 音量控制
                    if (buttonType == ButtonType.RIGHT_ROTATE) {
                        volumeUp() // 顺时针旋转，增大音量
                    } else {
                        volumeDown() // 逆时针旋转，减小音量
                    }
                    return
                }
                "MEDIA_MUSIC_SWITCH" -> {
                    // 音乐切换
                    if (buttonType == ButtonType.RIGHT_ROTATE) {
                        nextTrack() // 顺时针旋转，下一首
                    } else {
                        previousTrack() // 逆时针旋转，上一首
                    }
                    return
                }
            }
        }
        
        // 处理普通功能
        when (function.actionCode) {
            "MEDIA_VOLUME_UP" -> volumeUp()
            "MEDIA_VOLUME_DOWN" -> volumeDown()
            "MEDIA_NEXT_TRACK" -> nextTrack()
            "MEDIA_PREVIOUS_TRACK" -> previousTrack()
            "MEDIA_PLAY_PAUSE" -> playPause()
            "MEDIA_MUTE_TOGGLE" -> toggleMute()
            else -> {
                Timber.tag(TAG).w("未知媒体功能代码: ${function.actionCode}")
            }
        }
    }

} 