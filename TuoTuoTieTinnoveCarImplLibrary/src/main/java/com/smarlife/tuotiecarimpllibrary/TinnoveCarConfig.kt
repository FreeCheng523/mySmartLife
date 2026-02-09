package com.smarlife.tuotiecarimpllibrary

import android.car.Car
import android.car.hardware.CarVendorExtensionManager
import android.content.Context
import com.mine.baselibrary.util.bytesUtil.getBitsRange
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread

/**
 * 梧桐车机配置类
 * 负责处理车辆配置相关功能，如电动尾翼、驾驶风格等
 */
@Singleton
class TinnoveCarConfig @Inject constructor(
    @ApplicationContext private val context: Context
) {


    private var mCarVendorExtensionManager: CarVendorExtensionManager? = null
    private var lastInitTime: Long = 0L
    private val INIT_INTERVAL_MS = 6000L // 6秒限制

    init {
        // 在后台线程中初始化CarVendorExtensionManager器
        intCarVendorExtensionManager()
    }


    private fun getConfigData(): ByteArray? {
        try {
            val offlineData = mCarVendorExtensionManager?.getBytesProperty(
                CarVendorExtensionManager.ID_VENDOR_OFF_LINE_STATUS,
                0
            )
            if (offlineData?.isNotEmpty() == true) {
                return offlineData
            }
            Timber.Forest.tag(TAG).e("offlineData is null or empty")
            return null
        } catch (e: Throwable) {
            Timber.Forest.tag(TAG).e(e, "getConfigData异常: ${e.message}")
            return null
        }
    }

    /**
     * 检查CarVendorExtensionManager是否可用，如果为空则尝试重新初始化
     * @return true 如果Manager可用，false 如果Manager为空且已尝试重连
     */
    private fun checkAndReconnectVendorExtensionManager(): Boolean {
        if (mCarVendorExtensionManager == null) {
            intCarVendorExtensionManager()
            Timber.Forest.tag(TAG).e("mCarVendorExtensionManager is null, reconnect")
            return false
        }
        return true
    }

    //是否有电动尾翼
    //如果没读取到配置字，默认设置为true
    fun hasElectricTailSpoiler(): Boolean{
        if (!checkAndReconnectVendorExtensionManager()) {
            return true // 默认返回true
        }
        val configData = getConfigData()
        configData?.let {
            if(configData.size<9){
                Timber.Forest.tag(TAG).e("size is error")
                return false
            }
            val electricTailSpoiler = configData.getBitsRange(8,0,0)
            return electricTailSpoiler == 1
        }
        return false
    }

    //是否有氛围灯
    //如果没读取到配置字，默认设置为false
    fun hasAmbientLight(): Boolean{
        if (!checkAndReconnectVendorExtensionManager()) {
            return false // 默认返回false
        }
        val configData = getConfigData()
        configData?.let {
            if(configData.size<36){
                Timber.Forest.tag(TAG).e("size is error")
                return false
            }
            val electricTailSpoiler = configData.getBitsRange(35,0,3)
            Timber.Forest.tag(TAG).d("electricTailSpoiler bits range (35,0,3) result: $electricTailSpoiler")

            val result = electricTailSpoiler == 3
            Timber.Forest.tag(TAG).d("hasAmbientLight returning: $result")

            return result
        }
        return false
    }


    /**
     * 0：无
     * 1：驾驶模式（燃油车）
     * 2：驾驶风格（PHEV）
     * 3：驾驶风格（REEV）
     * 4：驾驶风格EV
     * 5：四驱全地形
     */
    fun driveStyle(): Int{
        val defaultStyle = 3

        if (!checkAndReconnectVendorExtensionManager()) {
            return defaultStyle // 默认返回true
        }
        val configData = getConfigData()
        configData?.let {
            if(configData.size<4){
                return defaultStyle
            }
            val driveStyle = configData.getBitsRange (3,3,5)
            return driveStyle
        }
        return defaultStyle
    }

    /**
     * 初始化CarVendorExtensionManager器
     * 限制：6秒内最多调用一次
     */
    private fun intCarVendorExtensionManager() {
        val currentTime = System.currentTimeMillis()
        //因为createCar内部失败会有5s的重试时间，所以调用频率限制为6s
        Timber.Forest.tag(TAG).i("intCarVendorExtensionManager,currentTime:$currentTime,lastInitTime:$lastInitTime")
        if (currentTime - lastInitTime < INIT_INTERVAL_MS) {
            Timber.Forest.tag(TAG).w("intCarVendorExtensionManager调用过于频繁，跳过本次调用")
            return
        }

        lastInitTime = currentTime

        thread {
            try {
                Timber.Forest.tag(TAG).w("初始化mCarVendorExtensionManager...")
                Car.createCar(context, null, 500) { car, ready ->
                    if (ready) {
                        mCarVendorExtensionManager =
                            car?.getCarManager(Car.VENDOR_EXTENSION_SERVICE) as? CarVendorExtensionManager
                        Timber.Forest.tag(TAG).w("初始化mCarVendorExtensionManager成功")
                    } else {
                        Timber.Forest.tag(TAG).w("初始化mCarVendorExtensionManager not ready")
                    }
                }
            } catch (e: Exception) {
                Timber.Forest.tag(TAG).e(e, "初始化CarVendorExtensionManager失败: ${e.message}")
            }
        }
    }



    companion object {
        @Volatile
        private var INSTANCE: TinnoveCarConfig? = null

        const val TAG = "TinnoveCarConfig"

        /**
         * 获取 TinnoveCarConfig 单例实例
         * @param context 应用上下文
         * @return TinnoveCarConfig 单例实例
         */
        fun getInstance(context: Context): TinnoveCarConfig {
            return INSTANCE ?: synchronized(this) {
                val instance = TinnoveCarConfig(context)
                INSTANCE = instance
                instance
            }
        }
    }
}