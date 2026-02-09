package com.smarlife.tuotiecarimpllibrary

import com.example.tuotuotie_car_interface_library.ICarMediaExecutor
import android.content.Context
import android.view.KeyEvent
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import android.car.Car
import android.car.media.CarAudioManager
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class TinnoveCarMediaExecutor@Inject constructor(
    @ApplicationContext private val context: Context
) : ICarMediaExecutor {

    companion object{
        const val TAG = "TinnoveCarMediaExecutor"
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
        } catch (e: Throwable) {
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


    /**
     * 确保CarAudioManager可用，使用同步初始化以便在需要时可用
     */
    override fun ensureCarAudioManager() {
        if (carAudioManager == null) {
            Timber.tag(TAG).w("懒加载方式初始化车载音频管理器")
            try {
                car = Car.createCar(context)
                carAudioManager = car?.getCarManager(Car.AUDIO_SERVICE) as? CarAudioManager

                if (carAudioManager != null) {
                    audioZoneId = CarAudioManager.PRIMARY_AUDIO_ZONE
                    Timber.tag(TAG).w("车载音频管理器懒加载初始化成功")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "懒加载初始化车载音频管理器失败: ${e.message}")
            }
        }
    }

    override fun ensureCarAudioManagerOnChildThread() {
        // 如果管理器未初始化，尝试懒加载方式初始化
        if (carAudioManager == null) {
            Thread {
                ensureCarAudioManager()
            }.start()
            // 继续执行功能，即使Car服务不可用也不阻塞UI
        }
    }

    /**
     * 增加音量
     */
    override fun volumeUp() {
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
                carAudio.setGroupVolume(this.audioZoneId, mediaGroupId, currentVolume + 1, 4097)
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
    override fun volumeDown() {
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
                carAudio.setGroupVolume(this.audioZoneId, mediaGroupId, currentVolume - 1, 4097)
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
            // 使用媒体按键发送事件
            sendMediaKeyEvent2(KeyEvent.KEYCODE_MEDIA_NEXT)
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
            // 使用媒体按键发送事件
            sendMediaKeyEvent2(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            Timber.tag(TAG).w("已发送播放上一首命令")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "播放上一首失败: ${e.message}")
        }
    }

    /**
     * 播放/暂停
     */
    override fun playPause() {
        Timber.tag(TAG).w("播放/暂停222")
        try {
            // 使用媒体按键发送事件
            sendMediaKeyEvent2(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            Timber.tag(TAG).w("已发送播放/暂停命令")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "播放/暂停失败: ${e.message}")
        }
    }

    /**
     * 静音/取消静音
     */
    override fun toggleMute() {
        Timber.tag(TAG).w("静音/取消静音")
        try {
            val carAudio = carAudioManager ?: return
//            val megacarAudio = megaCarAudioManagerProxy?: return

            // 获取媒体音频组
            val mediaGroupId = carAudio.getVolumeGroupIdForUsage(audioZoneId, 1)

            // 获取当前音量
            val currentVolume = carAudio.getGroupVolume(this.audioZoneId, mediaGroupId)

            // 获取最大音量
            val maxVolume = carAudio.getGroupMaxVolume(this.audioZoneId, mediaGroupId)

            // 切换静音状态
            if (currentVolume>0) {
                this.lastVolumn = currentVolume
                carAudio.setGroupVolume(this.audioZoneId, mediaGroupId, 0, 4097)
                //megacarAudio.setOneKeyMute(carAudio,true)
            }else{
                if(this.lastVolumn==0){
                    this.lastVolumn = maxVolume / 2
                }
                carAudio.setGroupVolume(this.audioZoneId, mediaGroupId, this.lastVolumn, 4097)

                //megacarAudio.setOneKeyMute(carAudio,false)
            }

            Timber.tag(TAG).w("已${if (currentVolume>0) "设置" else "取消"}静音")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "静音/取消静音失败: ${e.message}")
        }
    }

    /**
     * 发送媒体按键事件
     */
    private fun sendMediaKeyEvent2(keyCode: Int) {
        // 模拟按下事件

        val now = SystemClock.uptimeMillis()
        val keyEventDown = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0)
        val keyEventUp = KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0)

        try {
            // 尝试通过CarAudioManager的扩展方法发送
            carAudioManager?.let { carAudio ->
                // 注意：CarAudioManager没有直接分发媒体按键的方法
                // 我们需要退回到使用Android标准方式发送媒体按键事件
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                audioManager.dispatchMediaKeyEvent(keyEventDown)
                audioManager.dispatchMediaKeyEvent(keyEventUp)

            } ?: run {
                // 如果CarAudioManager不可用，使用标准AudioManager
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                audioManager.dispatchMediaKeyEvent(keyEventDown)
                audioManager.dispatchMediaKeyEvent(keyEventUp)

            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "发送媒体按键事件失败: ${e.message}")
        }
    }

    private fun sendMediaKeyEvent(keyCode: Int) {
        // 模拟按下事件
        val keyEventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val keyEventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)

        try {
            // 尝试通过CarAudioManager的扩展方法发送
            carAudioManager?.let { carAudio ->
                // 注意：CarAudioManager没有直接分发媒体按键的方法
                // 我们需要退回到使用Android标准方式发送媒体按键事件
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                audioManager.dispatchMediaKeyEvent(keyEventDown)
                audioManager.dispatchMediaKeyEvent(keyEventUp)

            } ?: run {
                // 如果CarAudioManager不可用，使用标准AudioManager
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                audioManager.dispatchMediaKeyEvent(keyEventDown)
                audioManager.dispatchMediaKeyEvent(keyEventUp)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "发送媒体按键事件失败: ${e.message}")
        }
    }

    /**
     * 释放资源
     */
    override fun release() {
        try {
            car?.disconnect()
            car = null
            carAudioManager = null
            Timber.tag(TAG).w("车载音频管理器已释放")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "释放车载音频管理器失败: ${e.message}")
        }
    }
}