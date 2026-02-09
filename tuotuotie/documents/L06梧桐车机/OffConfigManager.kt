package com.wt.vehiclecenter.common.api

import android.car.Car
import android.car.hardware.CarVendorExtensionManager
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.openos.util.and
import com.wt.vehiclecenter.common.util.LogUtils

import com.wt.vehiclecenter.common.util.globalIntSetting
import com.wt.vehiclecenter.common.util.systemIntSetting


/**
 * 管理读取全部的下线配置数据
 */
class OffConfigManager private constructor() {

    companion object {
        const val TAG = "OffConfigManager"
        const val KEY_AR_HUD = "key_ar_hud"
        const val KEY_DLP_LIGHT = "key_dlp_light"

        /**
         * 驾驶员左前摄像头(DMS)
         */
        const val KEY_DMS_LH_CAMERA = "key_dms_lh_camera"
        private const val CAR_TYPE: String = "com.wt.vehiclecenter.cartype"
        const val CAR_LEVEL2 = 2
        const val CAR_LEVEL3 = 3
        const val CAR_LEVEL4 = 4

        const val ONE_SOUND_AREA = 0
        const val TWO_SOUND_AREA = 1
        const val FOUR_SOUND_AREA = 2

        @JvmStatic
        val instance: OffConfigManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            OffConfigManager()
        }
    }

    private var offlineData: ByteArray? = null

    /**
     * "0：无
     * 1：燃油
     * 2：PHEV
     * 3：REEV
     * 4：纯电",EV
     *
     */
    var carType: Int = 0
        private set

    /**
     * 获取车辆销售区域
     * 0：中国大陆
     * 1：日本
     * 2：海湾地区
     * 3：东南亚
     * 4：西欧
     * 5：东欧
     * 6：北美
     * 7：南美
     * 8：非洲
     * 9：澳洲
     * 10：俄罗斯/独联体
     * （可能关联语言、收音机频段、麦加指向针）
     */
    var carArea: Int = 0

    /**
     * wltp和nedc的显示模式 只显示wltp
     * true 只显示wltp
     */
    fun applyOnlyWltp():Boolean{
        val result = when(carArea){
            4,5->
                true
            else ->
                false
        }
        return result
    }

    /**
     * wltp和nedc的显示模式 只显示nedc
     * true 只显示nedc  nedc
     */
    fun applyOnlyNedc():Boolean{

        return false
    }


    /**
     * 0：国内
     * 1：独联体俄罗斯
     * 2：独联体其他国家
     * 3:中东沙特
     * 4:中东其他国家
     * 5:中东伊拉克（劣油）
     * 6:非洲(摩洛哥）
     * 7:美洲墨西哥
     * 8:美洲其余国家
     * 9:亚太左舵
     * 10:英国（右舵）
     * 11:澳大利亚（右舵）
     * 12:欧洲、以色列
     * 13:巴基斯坦（右舵）
     * 14:南非（右舵）
     * 15:泰国、印尼、马来（右舵）
     */
    private var overseasDomain: Int = 0

    fun applyPassengerFrontAirbagMode() = (overseasDomain == 10 || overseasDomain == 12)

    /**
     * 雪地模式
     * 0：无
     * 1：有
     */
    private var snowMode: Int = 0

    /**
     * 是否有雪地模式
     */
    fun applySnowMode() = snowMode == 1

    /**
     * 拖挂车模式
     * 0：无
     * 1：有
     */
    private var towingMode: Int = 0

    /**
     * 是否有拖挂车模式
     */
    fun applyTowingMode() = towingMode == 1

    /**
     * 生命体征探测
     * 0：无
     * 1：国内方案
     * 2：海外方案
     */
    private var vitalSignDetection: Int = 0

    /**
     * 是否有生命体征探测
     */
    fun applyVitalSignDetection() = vitalSignDetection != 0

    /**
     * REEV车型
     */
    fun isCarReev() = carType == 3

    /**
     * 纯电车型,EV
     */
    fun isCarElectric() = carType == 4

    /**
     * 0：无
     * 1：有
     * 云台相机
     */
    private var cloudCamera: Int = 0

    /**
     * 是否有云台相机
     */
    fun applyCloudCamera() = cloudCamera == 1

    /**
     * 0：无
     * 1：有
     * 车外语音
     */
    private var outVoice: Int = 0

    /**
     * 是否有车外语音
     */
    fun applyOutVoice() = outVoice == 1

    /**
     * 0：无
     * 1：有
     * 旋转屏
     */
    private var rotateScreen: Int = 0

    /**
     * 是否有旋转屏
     */
    fun applyRotateScreen() = rotateScreen == 1

    /**
     * 0：无
     * 1：驱动模式（REEV）
     * 2：驱动模式（PHEV）
     * 驱动模式
     */
    private var driveMode: Int = 0

    /**
     * 是否有驱动模式
     */
    fun applyDriveMode() = driveMode != 0

    /**
     * 是否驱动模式（PHEV）
     */
    fun driveModePhev() = driveMode == 2

    /**
     * 0：无
     * 1：有
     * 驱动模式-山地模式
     */
    private var driveModeLand: Int = 0

    /**
     * 是否有驱动模式-山地模式
     */
    fun applyDriveModeLand() = driveModeLand == 1

    /**
     * 0：无
     * 1：驾驶模式（燃油车）
     * 2：驾驶风格（PHEV）
     * 3：驾驶风格（REEV）
     * 4：驾驶风格EV
     * 驾驶模式||驾驶风格
     */
    private var driveStyle: Int = 0

    /**
     * 是否有驾驶风格
     */
    fun applyDriveStyle() = driveStyle != 0

    /**
     * 3：驾驶风格（REEV）
     */
    fun driveStyleReev() = driveStyle == 3

    /**
     * 4：驾驶风格EV
     */
    fun driveStyleEv() = driveStyle == 4

    /**
     * 0：无
     * 1：有
     * 驾驶模式与转向助力联动
     */
    private var driveModeLinkPowerSteer: Int = 0

    /**
     * 是否有驾驶模式与转向助力联动
     */
    fun applyDriveModeLinkPowerSteer() = driveModeLinkPowerSteer == 1

    /**
     * 0：无
     * 1：滑行能量回收（PHEV）
     * 2：滑行能量回收（REEV）
     * 3. 滑行能量回收（EV）
     * 滑行能量回收
     */
    private var energyRecovery: Int = 0

    /**
     * 是否有滑行能量回收
     */
    fun applyEnergyRecovery() = energyRecovery != 0

    /**
     * 2：滑行能量回收（REEV）
     */
    fun energyRecoveryReev() = energyRecovery == 2

    /**
     * 3. 滑行能量回收（EV）
     */
    fun energyRecoveryEv() = driveStyle == 3

    /**
     * 0：无
     * 1：有
     * 自定亮度调节
     */
    private var autoBrightness: Int = 0

    /**
     * 是否有自定亮度调节
     */
    fun applyAutoBrightness() = autoBrightness == 1

    /**
     * 0：非电动
     * 1：电动
     * 后背门
     */
    private var backDoor: Int = 0

    /**21   v
     * 是否有电动后背门
     */
    fun applyElectricBackDoor() = backDoor == 1

    /**
     * 0：无
     * 1：有
     * 无线充电
     */
    private var wirelessCharge: Int = 0

    /**
     * 是否有无线充电
     */
    fun isWirelessCharge() = wirelessCharge == 1

    /**
     * 0：无
     * 1：WHUD
     * 2：ARHUD
     * 3：预留
     * 抬头显示器类型
     */
    private var typeHud: Int = 0

    /**
     * 是否有ARHud
     */
    fun applyArHud() = typeHud == 2

    /**
     * 0：无
     * 1：有
     * HUD自适应调节亮度
     */
    private var hudAutoBrightness: Int = 0

    /**
     * 是否有HUD自适应调节亮度
     */
    fun applyHudAutoBrightness() = hudAutoBrightness == 1

    /**
     * 0：无
     * 1：有
     * HUD辅助驾驶显示
     */
    private var hudAdas: Int = 0

    /**
     * 是否有HUD辅助驾驶显示
     * 1.0版电检表分车型
     * 1.1版默认所有车型都有了，所以true
     */
    fun applyHudAdas() = true//hudAdas == 1

    /**
     * 0：无
     * 1：有
     * DLP大灯相关功能
     */
    private var dlpLight: Int = 0

    /**
     * 是否有DLP大灯相关功能
     */
    fun applyDlpLight() = dlpLight == 1


    /**
     * 左右舵车型判断
     * 0是左舵
     * 1是右舵
     */
    private var carRightMode: Int = 0
    private var rightModeCar: Int = 0

    /**
     * 是否右舵车
     */
    fun applyCarRightMode() = false
    fun applyRightModeCar() = rightModeCar == 1

    /**
     * 注意这里获取的是CarModel的id，不是真实的carLevel
     * 这里是项目的配置排列
     * LV4的id区间是[103,117]
     */
    public var carLevelId: Int = 0

    /**
     * 0：无
     * 1：有
     * 外后视镜自动折叠
     */
    private var mirrorOutAutoFold: Int = 0

    /**
     * 是否有外后视镜自动折叠
     */
    fun applyMirrorOutAutoFold() = mirrorOutAutoFold == 1

    /**
     * 0：无
     * 1：有
     * 外后视镜折叠
     */
    private var mirrorOutFold: Int = 0

    /**
     * 是否有外后视镜折叠
     */
    fun applyMirrorOutFold(): Boolean {
        LogUtils.d(TAG, "applyMirrorOutFold__mirrorOutFold=${mirrorOutFold}")
        return mirrorOutFold == 1
    }

    /**
     *
     * 0,1,4
     * 2,5,7,8,9,10,11,12,13,17,18,19,20,21,22,23
     * 3,6,14,15,16,24
     * 获取carlevel 2-4
     *
     * 0：REEV LEVEL2-120KM
     * 1：REEV LEVEL2-200KM
     *
     * 2：REEV LEVEL3-200KM
     * 3：REEV LEVEL4-200KM
     * 4：EV LEVEL2-510KM
     * 5：EV LEVEL3-510KM
     * 6：EV LEVEL4-510KM
     * 7：REEV LEVEL3-200KM+舒适座椅包
     * 8：REEV LEVEL3-200KM+智能影音包
     * 9：REEV LEVEL3-200KM+旅途伴友包
     * 10：REEV LEVEL3-200KM+舒适座椅包+智能影音包
     * 11：REEV LEVEL3-200KM+舒适座椅包+旅途伴友包
     * 12：REEV LEVEL3-200KM+智能影音包+旅途伴友包
     * 13：REEV LEVEL3-200KM+舒适座椅包+智能影音包+旅途伴友包
     * 14：REEV LEVEL4-200KM+舒适座椅包
     * 15：REEV LEVEL4-200KM+旅途伴友包
     * 16：REEV LEVEL4-200KM+ 舒适座椅包+旅途伴友包
     * 17：EV LEVEL3-510KM+舒适座椅包
     * 18：EV LEVEL3-510KM+智能影音包
     * 19：EV LEVEL3-510KM+旅途伴友包
     * 20：EV LEVEL3-510KM+舒适座椅包+智能影音包
     * 21：EV LEVEL3-510KM+舒适座椅包+旅途伴友包
     * 22：EV LEVEL3-510KM+智能影音包+旅途伴友包
     * 23：EV LEVEL3-510KM+舒适座椅包+智能影音包+旅途伴友包
     * 24：EV LEVEL4-510KM+舒适座椅包
     */
    fun carLevel(): Int {
        val level = when (carLevelId) {
            1, 8, 11, 14, 16, 18, 20, 22 -> 3
            0, 2, 3, 4, 5, 6, 7, 9, 10, 12, 13, 15, 17, 19, 21, 23 -> 4
            else -> -1
        }

        return level
    }

    fun hasDalyEye(): Boolean {
        return carLevelId != 0
    }

    /**
     * 0：无
     * 1：有
     * 自动除雾开关
     */
    private var autoFog: Int = 0

    /**
     * 是否有自动除雾开关
     */
    fun applyAutoFog() = autoFog == 1

    /**
     * 0：无
     * 1：有
     * 遮阳帘控制
     */
    private var sunShade: Int = 0

    /**
     * 是否有遮阳帘控制
     */
    fun applySunShade() = sunShade == 1

    /**
     * 0：无
     * 1：1组记忆
     * 2：2组记忆
     * 3：多组（＞2组）记忆
     * 座椅记忆
     */
    var seatMemory: Int = 0

    /**
     * 是否有座椅记忆
     */
    fun applySeatMemory() = seatMemory != 0

    /**
     * 0：无
     * 1：有
     */
    private var elk: Int = 0

    /**
     * 是否有前碰撞预警
     */
    fun applyElk() = elk != 0

    /**
     * 0：无
     * 1：有
     */
    private var forwardCollisionWarning: Int = 0

    /**
     * 是否有前碰撞预警
     */
    fun applyForwardCollisionWarning() = forwardCollisionWarning != 0

    /**
     * 0：无
     * 1：有
     */
    private var rearCollisionWarning: Int = 0

    /**
     * 是否有后追尾预警
     */
    fun applyRearCollisionWarning() = rearCollisionWarning != 0

    /**
     * 0：无
     * 1：有
     */
    private var sideExitWarning: Int = 0

    /**
     * 是否有开门预警
     */
    fun applySideExitWarning() = sideExitWarning != 0

    /**
     * 0：无
     * 1：有
     */
    private var lcw: Int = 0

    /**
     * 是否有并线辅助
     */
    fun applyLcw() = lcw != 0

    /**
     * 0：无
     * 1：有
     */
    private var lcwSensitivity: Int = 0

    /**
     * 是否有预警时机
     */
    fun applyLcwSensitivity() = lcwSensitivity != 0

    /**
     * 0：无
     * 1：有
     */
    private var aeb: Int = 0

    /**
     * 是否有自动紧急制动
     */
    fun applyAeb() = aeb != 0

    /**
     * 0：无
     * 1：有
     */
    private var rearCrossTrafficWarning: Int = 0

    /**
     * 是否有倒车横向辅助
     */
    fun applyRearCrossTrafficWarning() = rearCrossTrafficWarning != 0

    /**
     * 0：无
     * 1：有
     * 外后视镜倒车辅助
     */
    var mirrorOutBack: Int = 0

    /**
     * 是否有外后视镜倒车辅助
     */
    fun applyMirrorOutBack() = mirrorOutBack == 1

    /**
     * 是否有主驾座椅加热
     */
    fun applyMainDriveSeatAir() = mainDriveSeatAir != 0

    /**
     * 是否有主驾座椅通风
     */
    fun applyMainDriveSeatHot() = mainDriveSeatHot != 0

    /**
     * "0：无
     * 1：自动巡航驾驶辅助-0级
     * 2：自动巡航驾驶辅助-1级
     * 3：自动巡航驾驶辅助-2级
     * 4：自动巡航驾驶辅助-2.5级
     * 5：自动巡航驾驶辅助-3级
     * 6：
     * 7：ACC
     * 8：IACC
     * 9：IACC+LCDA
     * 10：IACC+UDLC（NID1.0）
     * 11：NID3.0
     * "
     * 自动巡航驾驶辅助
     */
    var autoAdasAcc: Int = 0
        private set

    /**
     * 0：无
     * 1：APA2.0
     * 2：APA4.0
     * 3：APA5.0
     * 4：APA6.0
     * 5：APA7.0
     *
     * 自动泊车类型
     */
    var autoPark: Int = 0

    /**
     * 0：无
     * 1：有
     * 驾驶员左前摄像头(DMS)（注5）
     */
    var dmsLHCamera: Int = 0
        private set

    /**
     * 是否有DMS
     */
    fun applyDmsLHCamera() = dmsLHCamera == 1

    /**
     * 0：无
     * 1：有
     *
     * 自定义按键-车外喊话
     * */
    private var hasOutShouting: Int = 0

    /**
     * 是否有车外喊话
     */
    fun applyOutShouting() = hasOutShouting == 1

    /**
     * 0：无
     * 1：单色
     * 2：多色
     * 3-7：预留
     * 氛围灯功能
     */
    private var atmoLightFun: Int = 0

    /**
     * 是否有氛围灯功能
     */
    fun applyAtmoLightFun() = atmoLightFun != 0

    /**
     * 0：无
     * 1：有
     * 自动远光灯
     */
    private var autoHighLight: Int = 0

    /**
     * 是否有自动远光灯
     */
    fun applyAutoHighLight() = autoHighLight != 0

    /**
     * 0：无
     * 1：有
     * 媒体音效
     */
    private var mediaEffect: Int = 0

    /**
     * 是否有媒体音效
     */
    fun applyMediaEffect() = mediaEffect != 0

    /**
     * 0：单音区
     * 1：双音区
     * 2：四音区
     * 3：预留
     * 音区
     */
    var soundArea: Int = 0
        private set

    /**
     * 0：无
     * 1：两档
     * 2：三挡
     * 3：预留
     * 主驾座椅通风
     */
    var mainDriveSeatAir: Int = 2
        private set

    /**
     * 0：无
     * 1：两档
     * 2：三挡
     * 3：预留
     * 副驾座椅通风
     */
    var passDriveSeatAir: Int = 0
        private set

    /**
     * 0：无
     * 1：两档
     * 2：三挡
     * 3：预留
     * 主驾座椅加热
     */
    var mainDriveSeatHot: Int = 0
        private set

    /**
     * 0：无
     * 1：两档
     * 2：三挡
     * 3：预留
     * 副驾座椅加热
     */
    var passDriveSeatHot: Int = 0
        private set

    /**
     * 0：无
     * 1：有
     * C柱环形灯
     */
    var cRoundLight: Int = 0
        private set

    /**
     * 0：无
     * 1:有
     * 自动雨刮
     */
    var autoRain: Int = 0
        private set


    /**
     * 雨刮档位开关
     * 0 无
     * 1 自动雨刮
     * 2 间歇雨刮
     */
    var rainWiper: Int = 0
        private set

    /**
     * 后排安全带
     */
    var rearBeltWaring: Int = 0;

    //    全部都有的配置
    fun hasRearBeltWaring(): Boolean = rearBeltWaring == 1 || true

    /**
     * 是否有C柱环形灯
     */
    fun applyCRoundLight() = cRoundLight == 1

    /**
     * 后排安全带未系提醒
     */
    var seatSeatBeltWarning: Int = 0

    /**
     * 是否有后排安全带未系提醒
     */
    fun applySeatBeltWarning() = seatSeatBeltWarning == 1

    /**
     * 一键管家（道路救援）
     */
    var bcall: Int = 0

    /**
     * 是否有一键管家（道路救援）
     * 0:无
     * 1:有
     */
    fun applyBcall() = bcall == 1

    /**
     * 紧急救援
     */
    var ecall: Int = 0

    /**
     * 是否有紧急救援
     * 0:无
     * 1:有
     */
//    fun applyEcall() = ecall == 1
    fun applyEcall() = false //外挂ECall方案，车辆中心要去掉ECall的入口 暂设计如此


    /**
     * 4g相关功能配置字
     */
    var mobileNet: Int = 0

    /**
     * 是否有4g相关功能配置字
     * 0:无
     * 1:有
     */
    fun apply4G() = mobileNet == 1

    /**
     * 用户中心功能配置字
     */
    var userCenter: Int = 0

    /**
     * 是否有4g相关功能配置字
     * 0:无
     * 1:有
     */
    fun applyUserCenter() = userCenter != 0



    /**
     * 智能限速
     * 0.没有
     * 1.有
     */
    private var intelligentSpeedLimit:Int  = 0

    /**
     * 获取智能限速的值
     */
    fun applyIntelligentSpeedLimit() = intelligentSpeedLimit != 0

    /**
     * 是否有预热  （这个是判断是否有4g参与，临时用于预热判断）
     * 1 没有
     * 0 有
     */
    private var hasPreHeat:Int = 1


    /**
     * 是否有预热
     */
    fun applyHasPreHeat() = hasPreHeat != 1


    /**
     * 遥控进出
     * 0 无  1有
     */
    private var hasControlOutIn:Int  = 0


    /**
     * 遥控进出
     * 0 无  1有
     */
     fun applyHasControlOutIn() = hasControlOutIn ==1

    /**
     * 0：无
     * 1：有
     * 钥匙关闭背门
     */
    private var keyCloseBackDoor: Int = 0

    /**
     * 是否有钥匙关闭背门
     */
    fun applyKeyCloseBackDoor() = keyCloseBackDoor == 1
    /**
     * 0：无
     * 1：有
     * 背门开度设置
     */
    private var backDoorSet: Int = 0

    /**
     * 是否有背门开度设置
     */
    fun applyBackDoorSet() = backDoorSet == 1

    /**
     * 是否有 超速预警  0.无 1。有
     */
    private var hasOverSpeedWarning:Int = 0

    /**
     *  超速预警  true。有   false 无
     */
    fun applyOverSpeedWarning() = hasOverSpeedWarning==1

    /**
     * 智能限速辅助  0.无  1.有
     */
    private var hasIntelligentSpeedAssistance:Int = 0


    /**
     *  智能限速辅助  true。有   false 无
     */
    fun applyIntelligentSpeedAssistance() = hasIntelligentSpeedAssistance==1


    /**
     * 是否有哨兵模式  0 无   1 有
     */
    private var hasAssistanceSentinel = 0

    /**
     * 是否有智能限速偏差值 0 无 1 有
     * */
    private var hasSpeedOffset = 0
    /**
     * 是否有哨兵模式  false 无   true有
     */
    fun applyHasAssistanceSentinel() = hasAssistanceSentinel==1

    /**
     * 读取全部的配置字
     */
    fun init(context: Context) {
        typeHud = KEY_AR_HUD.globalIntSetting
        dlpLight = KEY_DLP_LIGHT.globalIntSetting
        dmsLHCamera = KEY_DMS_LH_CAMERA.systemIntSetting
        Car.createCar(context, null, 1000) { car, ready ->
            LogUtils.d(TAG, "readOffConfig = $car, ready=$ready")
            if (ready) {
                try {
                    val carVendorExtensionManager =
                        car.getCarManager(Car.VENDOR_EXTENSION_SERVICE) as? CarVendorExtensionManager?
                    offlineData = carVendorExtensionManager?.getBytesProperty(
                        CarVendorExtensionManager.ID_VENDOR_OFF_LINE_STATUS, 0
                    )
                    //offline 读取全部的配置字byte数组,数组下标0对应byte0,自行解析每个byte对应配置字值
                    Log.d(TAG, "readOffConfig = " + offlineData.contentToString())
                    offlineData?.takeIf { it.size > 8 }?.let {
                        //获取车辆销售区域
                        carArea = (it[0] and 0x1E) shr 1
                        LogUtils.i(TAG, "init-carArea = $carArea")
                        //海外域名配置
                        overseasDomain = it[58] and 0x1f
                        LogUtils.i(TAG, "overseasDomain = $overseasDomain")
                        //雪地模式
                        snowMode = (it[57] and 0x20) shr 5
                        LogUtils.i(TAG, "snowMode = $snowMode")
                        //拖挂车模式
                        towingMode = (it[57] and 0x4) shr 2
                        LogUtils.i(TAG, "towingMode = $towingMode")
                        //生命体征探测
                        vitalSignDetection = it[48] and 0x3
                        LogUtils.i(TAG, "vitalSignDetection = $vitalSignDetection")
                        carType = (it[0] and 0xE0) shr 5
                        Settings.Global.putInt(context.contentResolver, CAR_TYPE, carType)
                        Log.d(TAG, "carType = $carType")
                        cloudCamera = (it[2] and 0x10) shr 4
                        Log.d(TAG, "cloudCamera = $cloudCamera")
                        //outVoice = (it[2] and 0x20) shr 5
                        outVoice = (it[39] and 0x80) shr 7
                        Log.d(TAG, "outVoice = $outVoice")
                        userCenter = (it[39] and 0x03)
                        Log.d(TAG, "userCenter === $userCenter")
                        rotateScreen = (it[2] and 0x80) shr 7
                        Log.d(TAG, "rotateScreen = $rotateScreen")
                        driveMode = it[3] and 0x03
                        Log.d(TAG, "driveMode = $driveMode")
                        driveModeLand = (it[3] and 0x04) shr 2
                        Log.d(TAG, "driveModeLand = $driveModeLand")
                        driveStyle = (it[3] and 0x38) shr 3
                        Log.d(TAG, "driveStyle = $driveStyle")
                        driveModeLinkPowerSteer = (it[3] and 0x80) shr 7
                        Log.d(TAG, "driveModeLinkPowerSteer = $driveModeLinkPowerSteer")
                        energyRecovery = it[4] and 0x03
                        Log.d(TAG, "energyRecovery = $energyRecovery")
                        mirrorOutAutoFold = (it[4] and 0x20) shr 5
                        Log.i(TAG, "mirrorOutAutoFold = $mirrorOutAutoFold")
                        mirrorOutFold = (it[4] and 0x40) shr 6
                        Log.i(TAG, "mirrorOutFold = $mirrorOutFold")
                        autoBrightness = (it[5] and 0x80) shr 7
                        Log.d(TAG, "autoBrightness = $autoBrightness")
                        backDoor = (it[7] and 0x40) shr 6
                        Log.d(TAG, "backDoor = $backDoor")
                        wirelessCharge = (it[7] and 0x80) shr 7
                        Log.i(TAG, "wirelessCharge = $wirelessCharge")
                        //arHud = (it[8] and 0x10) shr 4
                        typeHud = it[9] and 0x03
                        KEY_AR_HUD.globalIntSetting = typeHud
                        Log.i(TAG, "typeHud = $typeHud")
                        hudAutoBrightness = (it[8] and 0x20) shr 5
                        Log.i(TAG, "hudAutoBrightness = $hudAutoBrightness")
                        hudAdas = (it[9] and 0x04) shr 2
                        Log.i(TAG, "hudAdas = $hudAdas")
                        dlpLight = (it[9] and 0x08) shr 3
                        KEY_DLP_LIGHT.globalIntSetting = dlpLight
                        Log.d(TAG, "dlpLight = $dlpLight")
                        carRightMode = (it[9] and 0x10) shr 4
                        rightModeCar = carRightMode
                        Log.d(TAG, "carRightMode = $carRightMode")
                        autoFog = (it[12] and 0x40) shr 6
                        Log.d(TAG, "autoFog = $autoFog")
                        seatMemory = (it[14] and 0xF0) shr 4
                        Log.d(TAG, "seatMemory = $seatMemory")
                        mirrorOutBack = (it[4] and 0x80) shr 7
                        Log.d(TAG, "mirrorOutBack = $mirrorOutBack")
                        sunShade = (it[36] and 0x02) shr 1
                        Log.d(TAG, "sunShade = $sunShade")
                        carLevelId = (it[63] and 0xFF) shr 0
                        Log.d(TAG, "carLevelId = $carLevelId")
                        autoAdasAcc = (it[20] and 0xFF) shr 0
                        Log.d(TAG, "autoAdasAcc = $autoAdasAcc")
                        autoPark = (it[24] and 0xFC) shr 2
                        Log.d(TAG, "autoPark = $autoPark")
                        dmsLHCamera = (it[8] and 0x80) shr 7
                        Log.d(TAG, "dmsLHCamera = $dmsLHCamera")
                        KEY_DMS_LH_CAMERA.systemIntSetting = dmsLHCamera
                        atmoLightFun = (it[35] and 0x07) shr 0
                        Log.d(TAG, "atmoLightFun = $atmoLightFun")
                        autoHighLight = (it[35] and 0x20) shr 5
                        Log.d(TAG, "autoHighLight = $autoHighLight")
                        mediaEffect = (it[30] and 0x01) shr 0
                        Log.d(TAG, "mediaEffect = $mediaEffect")
                        soundArea = (it[30] and 0x18) shr 3
                        Log.d(TAG, "soundArea = $soundArea")
                        mainDriveSeatAir = it[13] and 0x03
                        Log.d(TAG, "mainDriveSeatAir = $mainDriveSeatAir")
                        passDriveSeatAir = (it[13] and 0x0C) shr 2
                        Log.d(TAG, "passDriveSeatAir = $passDriveSeatAir")
                        mainDriveSeatHot = (it[13] and 0x30) shr 4
                        Log.d(TAG, "mainDriveSeatHot = $mainDriveSeatHot")
                        passDriveSeatHot = (it[13] and 0xC0) shr 6
                        Log.i(TAG, "passDriveSeatHot = $passDriveSeatHot")
                        cRoundLight = (it[9] and 0x20) shr 5
                        Log.i(TAG, "cRoundLight = $cRoundLight")
                        autoRain = (it[5] and 0x40) shr 6
                        Log.i(TAG, "autoRain = $autoRain")
                        rainWiper = (it[33] and 0xC0) shr 6
                        Log.i(TAG, "rainWiper = $rainWiper")
                        seatSeatBeltWarning = (it[40] and 0x08) shr 3
                        Log.i(TAG, "seatSeatBeltWarning = $seatSeatBeltWarning")
                        bcall = (it[61] and 0x10) shr 4
                        Log.i(TAG, "BCall = $bcall")
                        ecall = (it[7] and 0x6) shr 1
                        Log.i(TAG, "ECall = $ecall")

                        elk = (it[55] and 0x02) shr 1
                        Log.i(TAG, "elk = $elk")
                        forwardCollisionWarning = (it[53] and 0x40) shr 6
                        Log.i(TAG, "forwardCollisionWarning = $forwardCollisionWarning")
                        rearCollisionWarning = (it[54] and 0x04) shr 2
                        Log.i(TAG, "rearCollisionWarning = $rearCollisionWarning")

                        sideExitWarning = (it[54] and 0x20) shr 5
                        Log.i(TAG, "sideExitWarning = $sideExitWarning")

                        lcw = (it[54] and 0x08) shr 3
                        Log.i(TAG, "lcw = $lcw")

                        rearCrossTrafficWarning = (it[48] and 0xC0) shr 6
                        Log.i(TAG, "rearCrossTrafficWarning = $rearCrossTrafficWarning")

                        lcwSensitivity = (it[54] and 0x10) shr 4
                        Log.i(TAG, "lcwSensitivity = $lcwSensitivity")

                        aeb = (it[53] and 0x80) shr 7
                        Log.i(TAG, "aeb = $aeb")

                        mobileNet = (it[5] and 0x20) shr 5
                        Log.i(TAG, "mobileNet = $mobileNet")

                        intelligentSpeedLimit = (it[48] and 0x0C) shr 2
                        Log.d(TAG, "intelligentSpeedLimit = $intelligentSpeedLimit")

                        hasPreHeat = (it[5] and 0x20) shr 5
                        Log.i(TAG, "hasPreHeat = $hasPreHeat")
                        hasControlOutIn = it[52] and 0x01
                        Log.i(TAG, "hasControlOutIn = $hasControlOutIn")

                        hasOutShouting = it[45] and 0x01
                        Log.i(TAG, "hasOutShouting = $hasOutShouting")

                        rearBeltWaring = (it[40] and 0x08) shr 3
                        Log.i(TAG, "rearBeltWaring = $rearBeltWaring")

                        keyCloseBackDoor  = (it[19] and 0x10) shr 4
                        Log.i(TAG, "keyCloseBackDoor = $keyCloseBackDoor")

                        backDoorSet  = (it[17] and 0x10) shr 4
                        Log.i(TAG, "backDoorSet = $backDoorSet")

                        hasOverSpeedWarning= (it[53] and 0x8) shr 3
                        Log.d(TAG, "hasOverSpeedWarning = $hasOverSpeedWarning")

                        hasIntelligentSpeedAssistance= (it[53] and 0x2) shr 1
                        Log.d(TAG, "hasIntelligentSpeedAssistance = $hasIntelligentSpeedAssistance")

                        hasAssistanceSentinel= (it[51] and 0x80) shr 7
                        Log.d(TAG, "hasAssistanceSentinel = $hasAssistanceSentinel")
                        hasSpeedOffset= (it[48] and 0x30) shr 4
                        Log.d(TAG, "hasSpeedOffset = $hasSpeedOffset")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }


    fun isAccLv3OrLv4(): Boolean {
        LogUtils.d(TAG, "isAccLv3OrLv4=${autoAdasAcc}")
        return autoAdasAcc == 8 || autoAdasAcc == 9
    }

    fun isAccLv4(): Boolean {
        LogUtils.d(TAG, "isAccLv4=${autoAdasAcc}")
        return autoAdasAcc == 9
    }

    /**
     * 是否是间歇雨刮
     */
    fun isIntermittentWiper(): Boolean {
        // TODO: 不知道怎么配置，暂时用这个区分
        return (rainWiper == 2).apply {
            LogUtils.d(TAG, "间歇雨刮档位：isIntermittentWiper=${this}")
        }
    }


    /*    0：无    已经不再使用 改为 applyHasControlOutIn
        1：APA2.0
        2：APA4.0
        3：APA5.0
        4：APA6.0
        5：APA7.0*/
    fun hasApa(): Boolean {
        return autoPark > 0
    }

    inline val isLevel3: Boolean
        get() = carLevel() == CAR_LEVEL3

    inline val isLevel4: Boolean
        get() = carLevel() == CAR_LEVEL4

    inline val isLevel3OrLevel4: Boolean
        get() = isLevel3 || isLevel4

    fun isEuropeCarModel(): Boolean {
        return carLevelId in setOf<Int>(
            8, 9, 10, 11, 12, 13
        )
    }

    /**
     * 获取车辆销售区域
     * @return
     */
    private fun getArea(): Byte {
        var area: Byte = 0
        try {
            if (offlineData != null && offlineData!!.size >= 59) {
                area = (offlineData!![58].toInt() and 31).toByte() // 取0-4位
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        LogUtils.d(TAG, "车辆的销售区域 area = $area")
        return area
    }

    /**
     * 销售区域是否是欧盟地区
     * @return
     */
    fun isEurope(): Boolean {
        return 12 == getArea().toInt()
    }

    fun hasSpeedOffset(): Boolean {
        return hasSpeedOffset == 1
    }

    /**
     * 销售区域是独联体
     */
    fun isCis(): Boolean {
        return 1 == getArea().toInt() || 2 == getArea().toInt()
    }

    /**
     * 销售区域是中东沙特
     */
    fun isMiddleEastSaudi(): Boolean {
        return 3 == getArea().toInt()
    }


    /**
     * 销售区域是中东
     */
    fun isMiddleEast(): Boolean {
        return 4 == getArea().toInt()
    }


    /**
     * 销售区域是美洲
     */
    fun isMexico(): Boolean {
        return 8 == getArea().toInt()
    }


    /**
     * 销售区域是东盟
     */
    fun isAsean(): Boolean {
        return 9 == getArea().toInt() || 15 == getArea().toInt()
    }


}