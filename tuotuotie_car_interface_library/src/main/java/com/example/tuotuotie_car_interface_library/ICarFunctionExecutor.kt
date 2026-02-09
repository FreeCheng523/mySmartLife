package com.example.tuotuotie_car_interface_library

interface ICarFunctionExecutor {

    /**
     * 初始化功能执行器
     */

    // 空调相关功能
    suspend fun setAirCdFront()
    suspend fun increaseACTemperature()
    suspend fun decreaseACTemperature()

    fun increaseACTemperature2()

    fun decreaseACTemperature2()

    suspend fun increaseFanSpeed()
    suspend fun decreaseFanSpeed()
    fun adjustAcWindDirection()
    suspend fun toggleDefrost1()
    suspend fun toggleDefrost2()

    // 座椅相关功能
    suspend fun toggleMainSeatHeat()
    suspend fun toggleCopilotSeatHeat()
    suspend fun toggleSteeringWheelHeat()
    suspend fun toSeatVentilationLeft()
    suspend fun toSeatVentilationRight()

    // 车门/后视镜相关功能
    suspend fun toggleTrunk()
    suspend fun toRearViewMirrorFold()

    // 儿童锁相关功能
    suspend fun toggleChildLock1()
    suspend fun toggleChildLock2()

    // 驾驶模式相关功能
    suspend fun adjustDrivingMode()

    // 尾翼控制
    suspend fun adjustTailWingPosition()

    // 小憩模式
    suspend fun setCdcRespiteSetSts()

    // 安全带提示
    suspend fun toSeatBeltCheck()

    // 雨刮模式
    suspend fun toWiper()

    // 低速行人报警音
    suspend fun toLowSpeedPedestrianAlarm()

    suspend fun increasePassengerPoistion()
    suspend fun decreasePassengerPoistion()

    // 按摩控制
    suspend fun adjustMassageIntensity()
    suspend fun adjustMassageMode()

    suspend fun toggleDefrost()


    fun adjustEnergyManagement()

    fun adjustCopilotScreen()

    /**
     * 氛围灯颜色
     */
    fun getAmbientLightColor(): Int

    /**
     * 氛围灯亮度
     */
    fun getAtmosphereLightBrightness():Int

    /**
     * 氛围灯开关状态
     */
    fun getAtmosphereLightSwtich():Int


    //是否点火
    fun isIgnition(): Boolean

    fun listenIsIgnition(callBack: (isIgnition: Boolean) -> Unit)
}