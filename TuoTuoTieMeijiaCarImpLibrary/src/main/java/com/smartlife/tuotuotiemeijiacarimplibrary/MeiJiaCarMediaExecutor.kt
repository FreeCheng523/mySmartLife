package com.smartlife.tuotuotiemeijiacarimplibrary

import android.car.Car
import android.car.media.CarAudioManager
import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent
import com.example.tuotuotie_car_interface_library.ICarMediaExecutor
import com.mine.baselibrary.constants.CarPlatformConstants
import com.mine.baselibrary.constants.VehicleTypeConstants
import timber.log.Timber
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Singleton
class MeiJiaCarMediaExecutor@Inject constructor(
    @ApplicationContext private val context: Context
) : ICarMediaExecutor {

    companion object{
        private const val TAG = "MeiJiaCarMediaExecutor"
    }

    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private var car: Car? = null
    private var carAudioManager: CarAudioManager? = null
    // 音频区域标识符，通常使用主区域
    private var audioZoneId: Int = 0

    // 静音前保存当前音量
    private var lastVolumn: Int = 0
    init {
        // 在后台线程中初始化车载音频管理器
        Thread {
            initCarAudio()
        }.start()
    }

    /**
     * 初始化车载音频管理器
     */
    private fun initCarAudio() {
        try {
            Timber.tag(TAG).w("开始初始化车载音频管理器...")
            car = Car.createCar(context)
            carAudioManager = car?.getCarManager(Car.AUDIO_SERVICE) as? CarAudioManager

            // 获取主音频区域ID
            if (carAudioManager != null) {
                audioZoneId = CarAudioManager.PRIMARY_AUDIO_ZONE//主音区
                Timber.tag(TAG).w("车载音频管理器初始化成功，主音频区域ID: $audioZoneId")
            } else {
                Timber.tag(TAG).e("无法获取CarAudioManager")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "初始化车载音频管理器失败: ${e.message}")
            // 添加重试逻辑
            Timber.tag(TAG).w("3秒后重试初始化车载音频管理器...")
            Thread.sleep(3000)
            retryInitCarAudio()
        }
    }

    /**
     * 重试初始化车载音频管理器
     * 最多重试5次
     */
    private fun retryInitCarAudio(retryCount: Int = 0) {
        if (retryCount >= 5) {
            Timber.tag(TAG).e("初始化车载音频管理器失败，已达到最大重试次数")
            return
        }

        try {
            Timber.tag(TAG).w("重试初始化车载音频管理器 (${retryCount + 1}/5)...")
            car = Car.createCar(context)
            carAudioManager = car?.getCarManager(Car.AUDIO_SERVICE) as? CarAudioManager

            if (carAudioManager != null) {
                audioZoneId = CarAudioManager.PRIMARY_AUDIO_ZONE
                Timber.tag(TAG).w("车载音频管理器初始化成功，主音频区域ID: $audioZoneId")
            } else {
                Timber.tag(TAG).e("重试后仍无法获取CarAudioManager")
                // 继续重试
                Thread.sleep(3000)
                retryInitCarAudio(retryCount + 1)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "重试初始化车载音频管理器失败: ${e.message}")
            // 继续重试
            Thread.sleep(3000)
            retryInitCarAudio(retryCount + 1)
        }
    }

    override fun ensureCarAudioManager() {

    }

    override fun ensureCarAudioManagerOnChildThread() {

    }

    /**
     * 增加音量
     */
    override fun volumeUp() {
        if(VehicleTypeConstants.isMega8295){
            volumeUp8295()
        }else{
            volumeUp8155()
        }
    }

    private fun volumeUp8155() {
        Timber.tag(TAG).w("增加音量")
        try {
            // 获取当前音量
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            // 获取最大音量
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

            if (currentVolume < maxVolume) {
                // 使用AudioManager增加音量
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    0  // 不显示UI
                )
                Timber.tag(TAG).w("音量已增加：${currentVolume + 1}/$maxVolume")
            } else {
                Timber.tag(TAG).w("音量已达最大值：$maxVolume")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "增加音量失败: ${e.message}")
        }
    }

    /**
     * 降低音量
     */
    override fun volumeDown() {
        if(VehicleTypeConstants.isMega8295){
            volumeDown8295()
        }else{
            volumeDown8155()
        }
    }

    private fun volumeDown8155() {
        Timber.tag(TAG).w("降低音量")
        try {
            // 获取当前音量
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            // 获取最大音量（用于日志）
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

            if (currentVolume > 0) {
                // 使用AudioManager降低音量
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    0  // 不显示UI
                )
                Timber.tag(TAG).w("音量已降低：${currentVolume - 1}/$maxVolume")
            } else {
                Timber.tag(TAG).w("音量已达最小值：0")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "降低音量失败: ${e.message}")
        }
    }

    /**
     * 增加音量
     */
    fun volumeUp8295() {
        Timber.tag(TAG).w("增加车载音量")
        try {
            val carAudio = carAudioManager ?: return

            // 获取媒体音频组
            //void setGroupVolume(int zoneId, int groupId, int index, int flags);
            //第一个参数是音区，857项目默认只有1个音区，这里传0
            //第二个参数是设置音量的音量组：传0-9,0是导航，1是媒体，2是电话，3是tts，4是按键音，5是ktv，6是电话铃声，7是车外媒体,8是车外tts，9是avas
            //第三个参数是音量等级，范围为0-20,
            //第四个参数方控传4097，其他应用传0
            //1是媒体
            val mediaGroupId = carAudio.getVolumeGroupIdForUsage(audioZoneId, 1)

            // 获取当前音量
            val currentVolume = carAudio.getGroupVolume(this.audioZoneId, mediaGroupId)
            // 获取最大音量
            val maxVolume = carAudio.getGroupMaxVolume(this.audioZoneId, mediaGroupId)

            if (currentVolume < maxVolume) {
                // 增加音量
                carAudio.setGroupVolume(this.audioZoneId, mediaGroupId, currentVolume + 1, android.media.AudioManager.FLAG_SHOW_UI)
                Timber.tag(TAG).w("车载音量已增加：${currentVolume + 1}/$maxVolume")
            } else {
                Timber.tag(TAG).w("车载音量已达最大值：$maxVolume")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "增加车载音量失败: ${e.message}")
        }
    }

    /**
     * 降低音量
     */
    fun volumeDown8295() {
        Timber.tag(TAG).w("降低车载音量")
        try {
            val carAudio = carAudioManager ?: return

            // 获取媒体音频组
            val mediaGroupId = carAudio.getVolumeGroupIdForUsage(audioZoneId, 1)

            // 获取当前音量
            val currentVolume = carAudio.getGroupVolume(this.audioZoneId, mediaGroupId)
            // 获取最大音量（用于日志）
            val maxVolume = carAudio.getGroupMaxVolume(this.audioZoneId, mediaGroupId)

            if (currentVolume > 0) {
                // 降低音量
                carAudio.setGroupVolume(this.audioZoneId, mediaGroupId, currentVolume - 1, android.media.AudioManager.FLAG_SHOW_UI)
                Timber.tag(TAG).w("车载音量已降低：${currentVolume - 1}/$maxVolume")
            } else {
                Timber.tag(TAG).w("车载音量已达最小值：0")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "降低车载音量失败: ${e.message}")
        }
    }

    /**
     * 播放下一首
     */
    override fun nextTrack() {
        Timber.tag(TAG).w("播放下一首")
        try {
            // 使用mediaButton模拟按下下一首按钮
//            val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT)
//            audioManager.dispatchMediaKeyEvent(keyEvent)
//
//            // 确保按键释放
//            val keyEventUp = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT)
//            audioManager.dispatchMediaKeyEvent(keyEventUp)

            sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
            Timber.tag(TAG).w("已发送播放下一首命令")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "播放下一首失败: ${e.message}")
        }
    }

    /**
     * 播放上一首
     */
    override fun previousTrack() {
        Timber.tag(TAG).w("播放上一首")
        try {
//            // 使用mediaButton模拟按下上一首按钮
//            val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
//            audioManager.dispatchMediaKeyEvent(keyEvent)
//
//            // 确保按键释放
//            val keyEventUp = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
//            audioManager.dispatchMediaKeyEvent(keyEventUp)


            sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            Timber.tag(TAG).w("已发送播放上一首命令")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "播放上一首失败: ${e.message}")
        }
    }

    /**
     * 播放/暂停
     */
    override fun playPause() {
        Timber.tag(TAG).w("播放/暂停")
        try {

            // 使用mediaButton模拟按下播放/暂停按钮

//            val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
//
//            audioManager.dispatchMediaKeyEvent(keyEvent)
//
//            // 确保按键释放
//            val keyEventUp = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
//            audioManager.dispatchMediaKeyEvent(keyEventUp)

            sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)


            Timber.tag(TAG).w("已发送播放/暂停命令")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "播放/暂停失败: ${e.message}")
        }
    }

    private fun sendMediaKeyEvent(keyCode: Int) {
        // 模拟按下事件

        val now = SystemClock.uptimeMillis()
        val keyEventDown = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0)
        val keyEventUp = KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0)

        try {

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.dispatchMediaKeyEvent(keyEventDown)
            audioManager.dispatchMediaKeyEvent(keyEventUp)

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "发送媒体按键事件失败: ${e.message}")
        }
    }


    /**
     * 静音/取消静音
     */
    override fun toggleMute() {
        Timber.tag(TAG).w("静音/取消静音")
        try {

            val carAudio = carAudioManager ?: return

            // 获取媒体音频组
            val mediaGroupId = carAudio.getVolumeGroupIdForUsage(audioZoneId, 1)

            // 获取当前音量
            val currentVolume = carAudio.getGroupVolume(this.audioZoneId, mediaGroupId)

            // 获取最大音量
            val maxVolume = carAudio.getGroupMaxVolume(this.audioZoneId, mediaGroupId)

            // 切换静音状态
            if (currentVolume>0) {
                this.lastVolumn = currentVolume
                carAudio.setGroupVolume(this.audioZoneId, mediaGroupId, 0, android.media.AudioManager.FLAG_SHOW_UI)

            }else{
                if(this.lastVolumn==0){
                    this.lastVolumn = maxVolume / 2
                }
                carAudio.setGroupVolume(this.audioZoneId, mediaGroupId, this.lastVolumn, android.media.AudioManager.FLAG_SHOW_UI)

            }


            Timber.tag(TAG).w("已${if (currentVolume>0) "设置" else "取消"}静音")

//
//            // 获取当前静音状态
//            val isMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
//
//            // 切换静音状态
//            if (isMuted) {
//                audioManager.adjustStreamVolume(
//                    AudioManager.STREAM_MUSIC,
//                    AudioManager.ADJUST_UNMUTE,
//                    0  // 不显示UI
//                )
//                Timber.tag(TAG).w("已取消静音")
//            } else {
//                audioManager.adjustStreamVolume(
//                    AudioManager.STREAM_MUSIC,
//                    AudioManager.ADJUST_MUTE,
//                    0  // 不显示UI
//                )
//                Timber.tag(TAG).w("已设置静音")
//            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "静音/取消静音失败: ${e.message}")
        }
    }

    override fun release() {

    }
}