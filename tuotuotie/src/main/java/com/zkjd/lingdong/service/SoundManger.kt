package com.zkjd.lingdong.service

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.util.Log
import androidx.annotation.RawRes

class SoundManager(private val context: Context) {
    private var soundPool: SoundPool
    private val soundMap = mutableMapOf<Int, Int>()

    init {
        soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes = AudioAttributes.Builder()
//                .setUsage(AudioAttributes.USAGE_GAME)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build()
        } else {
            @Suppress("DEPRECATION")
            SoundPool(5, android.media.AudioManager.STREAM_MUSIC, 0)
        }
    }

    /**
     * 加载声音资源
     * @param resId 声音资源ID（R.raw.*）
     * @return 声音ID，用于后续播放
     */
    fun loadSound(@RawRes resId: Int): Int {
        val soundId = soundPool.load(context, resId, 1)
        soundMap[resId] = soundId
        return soundId
    }

    /**
     * 播放已加载的声音
     * @param resId 声音资源ID（R.raw.*）
     * @param volume 音量（0.0f - 1.0f）
     * @param loop 是否循环播放
     */
    fun playSound(@RawRes resId: Int, volume: Float = 1.0f, loop: Int = 0) {
        Log.i("SoundManager","playSound")
        val soundId = soundMap[resId] ?: loadSound(resId)
        soundPool.play(soundId, volume, volume, 1, loop, 1.0f)
    }

    /**
     * 释放资源
     */
    fun release() {
        soundPool.release()
    }
}