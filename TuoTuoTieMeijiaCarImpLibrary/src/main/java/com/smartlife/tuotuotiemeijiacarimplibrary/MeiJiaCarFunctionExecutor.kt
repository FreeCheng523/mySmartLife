package com.smartlife.tuotuotiemeijiacarimplibrary

import android.car.VehicleAreaType
import android.car.hardware.cabin.CarCabinManager
import android.content.Context
import com.android.car.internal.util.TextUtils
import com.example.tuotuotie_car_interface_library.ICarFunctionExecutor
import com.mega.nexus.os.MegaSystemProperties
import com.mine.baselibrary.ActionNameProvider
import com.mine.baselibrary.BidirectionalAction
import com.mine.baselibrary.BoundaryCheckResult
import com.mine.baselibrary.BoundaryChecker
import com.mine.baselibrary.Executor
import com.mine.baselibrary.ReactiveFlowHandler
import com.mine.baselibrary.ResourceChecker
import com.mine.baselibrary.constants.CarPlatformConstants
import com.mine.baselibrary.constants.VehicleTypeConstants
import com.mine.baselibrary.window.ToastUtilOverApplication
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.car.MegaCarProperty
import mega.car.Signal.CDC_2D2_CDCLERECHILDLOCKREQ
import mega.car.Signal.CDC_2D2_CDCRIRECHILDLOCKREQ
import mega.car.Signal.GW_288_BDCLERECHILDLOCKSTS
import mega.car.Signal.GW_288_BDCRIRECHILDLOCKSTS
import mega.car.hardware.CarPropertyValue
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton


/**
 * 美佳车型功能执行器
 * 负责处理车辆控制相关功能，如空调、座椅、车门等控制
 */
@Singleton
class MeiJiaCarFunctionExecutor @Inject constructor(
    @ApplicationContext private val context: Context
) : ICarFunctionExecutor {



    // Car属性工具类
    private val mCarProperty: MegaCarProperty by lazy {
        MegaCarProperty.getInstance()
    }
    
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // 温度控制 Flow 延迟响应相关
    private enum class TemperatureAction : BidirectionalAction {
        DECREASE, // 降低温度
        INCREASE; // 升高温度
        
        override fun isOpposite(other: BidirectionalAction): Boolean {
            return when (this) {
                DECREASE -> other == INCREASE
                INCREASE -> other == DECREASE
            }
        }
    }
    
    // 温度控制 Flow 延迟响应相关（副驾）
    private enum class TemperatureAction2 : BidirectionalAction {
        DECREASE, // 降低温度
        INCREASE; // 升高温度
        
        override fun isOpposite(other: BidirectionalAction): Boolean {
            return when (this) {
                DECREASE -> other == INCREASE
                INCREASE -> other == DECREASE
            }
        }
    }
    
    // 风量控制 Flow 延迟响应相关
    private enum class FanSpeedAction : BidirectionalAction {
        DECREASE, // 减少风量
        INCREASE; // 增加风量
        
        override fun isOpposite(other: BidirectionalAction): Boolean {
            return when (this) {
                DECREASE -> other == INCREASE
                INCREASE -> other == DECREASE
            }
        }
    }
    
    // 座椅位置控制 Flow 延迟响应相关
    private enum class PassengerPositionAction : BidirectionalAction {
        DECREASE, // 前移
        INCREASE; // 后移
        
        override fun isOpposite(other: BidirectionalAction): Boolean {
            return when (this) {
                DECREASE -> other == INCREASE
                INCREASE -> other == DECREASE
            }
        }
    }
    
    // 温度控制 Flow 处理器（主驾）
    private val temperatureHandler: ReactiveFlowHandler<TemperatureAction> by lazy {
        ReactiveFlowHandler(
            context = context,
            tag = TAG,
            boundaryChecker = object : BoundaryChecker<TemperatureAction> {
                override suspend fun checkBoundary(
                    action: TemperatureAction,
                    currentValue: Int
                ): BoundaryCheckResult {
                    // 将 Int 值（温度 * 10）转换回 Float 进行比较
                    val currentTemp = currentValue / 10.0f
                    return when (action) {
                        TemperatureAction.DECREASE -> {
                            if (currentTemp <= minTemperature) {
                                BoundaryCheckResult(false, "空调温度已调到最低")
                            } else {
                                BoundaryCheckResult(true)
                            }
                        }

                        TemperatureAction.INCREASE -> {
                            if (currentTemp >= 32.5f) {
                                BoundaryCheckResult(false, "空调温度已调到最高")
                            } else {
                                BoundaryCheckResult(true)
                            }
                        }
                    }
                }
            },
            executor = object : Executor<TemperatureAction> {
                override suspend fun execute(action: TemperatureAction) {
                    when (action) {
                        TemperatureAction.DECREASE -> executeDecreaseACTemperature()
                        TemperatureAction.INCREASE -> executeIncreaseACTemperature()
                    }
                }
            },
            resourceChecker = object : ResourceChecker {
                override suspend fun check(): Boolean {
                    // MegaCarProperty 是单例，不需要检查
                    return true
                }

                override fun getResourceName(): String = "MegaCarProperty"
            },
            actionNameProvider = object : ActionNameProvider<TemperatureAction> {
                override fun getActionName(action: TemperatureAction): String {
                    return when (action) {
                        TemperatureAction.DECREASE -> "降低温度"
                        TemperatureAction.INCREASE -> "升高温度"
                    }
                }
            },
            getCurrentValue = {
                // 将 Float 温度值乘以 10 转换为 Int
                mCarProperty.getFloatProp(ID_TEMPERATURE_FRONTLEFT)?.let { (it * 10).toInt() }
            },
            maxRequestsPerPeriod = 5
        )
    }
    
    // 温度控制 Flow 处理器（副驾）
    private val temperatureHandler2: ReactiveFlowHandler<TemperatureAction2> by lazy {
        ReactiveFlowHandler(
            context = context,
            tag = TAG,
            boundaryChecker = object : BoundaryChecker<TemperatureAction2> {
                override suspend fun checkBoundary(
                    action: TemperatureAction2,
                    currentValue: Int
                ): BoundaryCheckResult {
                    // 将 Int 值（温度 * 10）转换回 Float 进行比较
                    val currentTemp = currentValue / 10.0f
                    return when (action) {
                        TemperatureAction2.DECREASE -> {
                            if (currentTemp <= minTemperature) {
                                BoundaryCheckResult(false, "空调温度已调到最低")
                            } else {
                                BoundaryCheckResult(true)
                            }
                        }

                        TemperatureAction2.INCREASE -> {
                            if (currentTemp >= 32.5f) {
                                BoundaryCheckResult(false, "空调温度已调到最高")
                            } else {
                                BoundaryCheckResult(true)
                            }
                        }
                    }
                }
            },
            executor = object : Executor<TemperatureAction2> {
                override suspend fun execute(action: TemperatureAction2) {
                    when (action) {
                        TemperatureAction2.DECREASE -> executeDecreaseACTemperature2()
                        TemperatureAction2.INCREASE -> executeIncreaseACTemperature2()
                    }
                }
            },
            resourceChecker = object : ResourceChecker {
                override suspend fun check(): Boolean {
                    // MegaCarProperty 是单例，不需要检查
                    return true
                }

                override fun getResourceName(): String = "MegaCarProperty"
            },
            actionNameProvider = object : ActionNameProvider<TemperatureAction2> {
                override fun getActionName(action: TemperatureAction2): String {
                    return when (action) {
                        TemperatureAction2.DECREASE -> "降低温度"
                        TemperatureAction2.INCREASE -> "升高温度"
                    }
                }
            },
            getCurrentValue = {
                // 将 Float 温度值乘以 10 转换为 Int
                mCarProperty.getFloatProp(ID_TEMPERATURE_FRONTRIGHT)?.let { (it * 10).toInt() }
            },
            maxRequestsPerPeriod = 5
        )
    }
    
    // 风量控制 Flow 处理器
    private val fanSpeedHandler: ReactiveFlowHandler<FanSpeedAction> by lazy {
        ReactiveFlowHandler(
            context = context,
            tag = TAG,
            enableBoundaryCheck = true,
            boundaryChecker = object : BoundaryChecker<FanSpeedAction> {
                override suspend fun checkBoundary(
                    action: FanSpeedAction,
                    currentValue: Int
                ): BoundaryCheckResult {
                    return when (action) {
                        FanSpeedAction.DECREASE -> {
                            // 注意：风量为1时允许通过，因为会执行关闭空调的特殊逻辑
                            if (currentValue < 1) {
                                BoundaryCheckResult(false, "空调已关闭")
                            } else {
                                BoundaryCheckResult(true)
                            }
                        }

                        FanSpeedAction.INCREASE -> {
                            /**
                             * 8295上的对应关系
                             * 8-8
                             * 9-10
                             * 10-11
                             */
                            if (currentValue >= 11) {
                                BoundaryCheckResult(false, "空调风量已调到最大，无法再增加")
                            } else {
                                BoundaryCheckResult(true)
                            }
                        }
                    }
                }
            },
            executor = object : Executor<FanSpeedAction> {
                override suspend fun execute(action: FanSpeedAction) {
                    when (action) {
                        FanSpeedAction.DECREASE -> executeDecreaseFanSpeed()
                        FanSpeedAction.INCREASE -> executeIncreaseFanSpeed()
                    }
                }
            },
            resourceChecker = object : ResourceChecker {
                override suspend fun check(): Boolean {
                    // MegaCarProperty 是单例，不需要检查
                    return true
                }

                override fun getResourceName(): String = "MegaCarProperty"
            },
            actionNameProvider = object : ActionNameProvider<FanSpeedAction> {
                override fun getActionName(action: FanSpeedAction): String {
                    return when (action) {
                        FanSpeedAction.DECREASE -> "减少风量"
                        FanSpeedAction.INCREASE -> "增加风量"
                    }
                }
            },
            getCurrentValue = {
                mCarProperty.getIntProp(ID_BLW_LEVEL_FRONT)
            }
        )
    }
    
    // 座椅位置控制 Flow 处理器
    private val passengerPositionHandler: ReactiveFlowHandler<PassengerPositionAction> by lazy {
        ReactiveFlowHandler(
            context = context,
            tag = TAG,
            minIntervalMs = 0,
            enableBoundaryCheck = false, // 美佳车型无法获取当前位置，禁用边界检查
            boundaryChecker = object : BoundaryChecker<PassengerPositionAction> {
                override suspend fun checkBoundary(
                    action: PassengerPositionAction,
                    currentValue: Int
                ): BoundaryCheckResult {
                    // 由于无法获取当前位置，边界检查被禁用
                    return BoundaryCheckResult(true)
                }
            },
            executor = object : Executor<PassengerPositionAction> {
                override suspend fun execute(action: PassengerPositionAction) {
                    when (action) {
                        PassengerPositionAction.DECREASE -> executeDecreasePassengerPosition()
                        PassengerPositionAction.INCREASE -> executeIncreasePassengerPosition()
                    }
                }
            },
            resourceChecker = object : ResourceChecker {
                override suspend fun check(): Boolean {
                    // MegaCarProperty 是单例，不需要检查
                    return true
                }

                override fun getResourceName(): String = "MegaCarProperty"
            },
            actionNameProvider = object : ActionNameProvider<PassengerPositionAction> {
                override fun getActionName(action: PassengerPositionAction): String {
                    return when (action) {
                        PassengerPositionAction.DECREASE -> "前移"
                        PassengerPositionAction.INCREASE -> "后移"
                    }
                }
            },
            getCurrentValue = {
                // 美佳车型无法获取当前位置，返回null
                null
            }
        )
    }
    
    // 车辆功能相关CAN信号ID常量
    companion object {

        private const val TAG = "MeiJiaCarFunction"

        // 后备箱控制
        private const val ID_DOOR_BACK = 0x6000031
        // 儿童锁控制
        private const val ID_CHILD_LOCK_LEFT = 0x64000164
        private const val ID_CHILD_LOCK_RIGHT = 0x64000165
        // 方向盘加热
        private const val ID_STEERING_WHEEL_HEATING = 0x3000001
        // 座椅加热（主驾、副驾）
        private const val ID_SEAT_HEAT_FRONTLEFT = 0x2000088
        private const val ID_SEAT_HEAT_FRONTRIGHT = 0x2000089
        // 空调温度调节
        private const val ID_TEMPERATURE_FRONTLEFT = 0x2000077
        private const val ID_TEMPERATURE_FRONTRIGHT = 0x2000078
        // 空调风量调节
        private const val ID_BLW_LEVEL_FRONT = 0x200007e
        // 空调吹风方向
        private const val ID_BLW_DIRECT_FRONT = 0x2000084
        // 驾驶模式
        private const val ID_DRV_MODE = 0x4000006
        // 能量管理模式
        private const val ID_ENERGY_MANAGEMENT_MODE = 0x4000031
        private const val ID_DRV_ENERGY_MODE = 0x4000015
        // 前后除霜除雾
        private const val ID_REQ_FRONT_DEFROST = 0x2000036
        private const val ID_REAR_DEFROST = 0x200001b
        // 按摩控制
        private const val ID_DSM_PARK_MASSAGE_STR = 0x64000180
        private const val ID_PSM_PARK_MASSAGE_STR = 0x64000184
        private const val ID_DSM_PARK_MASSAGE_MOD = 0x6400017f
        private const val ID_PSM_PARK_MASSAGE_MOD = 0x64000183
        // 尾翼档位
        private const val ID_PTS_ELE_TAIL_FUN_SET = 0x64000221
        // 副驾屏展开度数
        private const val ID_CDC_TURN_VOICE_CTRL = 0x6400011e
        //前排空调开关 关闭
        private const val ID_WHOLE_CABIN_AIR_CD_FRONT=0x200006a
        //前排空调开关 运行
        private const val ID_AC_RUN_REQ=0x2000042

        //小憩模式 运行
        private const val ID_DC_3BD_CDCRESPITESETSTS=0x6400016a
        //小憩模式 运行
        private const val ID_RESPITE_MODE=0x7000024
        //后视镜开关
        private const val ID_REAR_VIEW_MIRROR_FOLD_CONTROL=0x1000007
        //二排安全带未系报警提示音
        private const val ID_SEAT_BELT_CHECK_REAR=0x3000078
        //雨刮模式
        private const val ID_WIPER_FRONT=0x9000081
        //低速行人报警音
        private const val ID_ACOUSTIC_VEH_ALERT=0xa000007

        //低速行人报警音
        private const val ID_ACOUSTIC_VEH_ALERT2=0xa000009

        private const val ID_CHIME_AVAS_PREVIEW=0x7000032
        //主驾座椅通风
        private const val ID_SEAT_VENT_FRONTLEFT=0x200008d
        //副驾座椅通风
        private const val ID_SEAT_VENT_FRONTRIGHT=0x200008e
        //老板键 副驾座椅移动控制
        private const val ID_SEAT_POSITION_FRONTRIGHT=0x300005d
        //座舱氛围灯颜色
        private const val ID_Ambient_Light_Color=0x800004f
        //座舱氛围灯开关
        private const val ID_Ambient_Light_Switch=0x8000008
    }

    init {
        // 初始化 Flow 处理器（通过 lazy 初始化）
        temperatureHandler
        temperatureHandler2
        fanSpeedHandler
        passengerPositionHandler
    }

    /**
     * 实际执行空调温度降低操作的内部方法（主驾）
     */
    private suspend fun executeDecreaseACTemperature() {
        Timber.tag(TAG).d("开始执行空调温度降低（主驾）")
        try {
            // 获取当前主驾温度
            val currentTemp = mCarProperty.getFloatProp(ID_TEMPERATURE_FRONTLEFT)
            Timber.tag(TAG).w("当前空调温度: $currentTemp°C")
            
            // 计算新温度，不低于最小值
            val newTemp = maxOf((currentTemp ?: minTemperature) - temperatureStep, minTemperature)
            
            // 设置新温度
            mCarProperty.setFloatProp(ID_TEMPERATURE_FRONTLEFT, newTemp)
            
            coroutineScope.launch {
                delay(1000)
                val nextTemp = mCarProperty.getFloatProp(ID_TEMPERATURE_FRONTLEFT)
                // 显示toast提示
                ToastUtilOverApplication().showToast(
                    context, if (nextTemp != null && nextTemp <= minTemperature) {
                        "主驾空调温度已调到最低"
                    } else {
                        "主驾空调温度已调低到${nextTemp ?: newTemp}度"
                    }
                )
                Timber.tag(TAG).d("current temperature is ${nextTemp ?: newTemp}")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "降低空调温度失败")
        }
    }

    /**
     * 实际执行空调温度升高操作的内部方法（主驾）
     */
    private suspend fun executeIncreaseACTemperature() {
        Timber.tag(TAG).d("开始执行空调温度升高（主驾）")
        try {
            // 获取当前主驾温度
            val currentTemp = mCarProperty.getFloatProp(ID_TEMPERATURE_FRONTLEFT)
            Timber.tag(TAG).w("当前空调温度: $currentTemp°C")

            // 计算新温度，不超过最大值
            val newTemp = minOf((currentTemp ?: minTemperature) + temperatureStep, maxTemperature)

            // 设置新温度
            mCarProperty.setFloatProp(ID_TEMPERATURE_FRONTLEFT, newTemp)

            coroutineScope.launch {
                delay(1000)
                val nextTemp = mCarProperty.getFloatProp(ID_TEMPERATURE_FRONTLEFT)
                // 显示toast提示
                ToastUtilOverApplication().showToast(
                    context, if (nextTemp != null && nextTemp >= 32.5) {
                        "主驾空调温度已调到最高"
                    } else {
                        "主驾空调温度已调高到${nextTemp ?: newTemp}度"
                    }
                )
                Timber.tag(TAG).d("current temperature is ${nextTemp ?: newTemp}")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "提高空调温度失败")
        }
    }

    /**
     * 实际执行空调温度降低操作的内部方法（副驾）
     */
    private suspend fun executeDecreaseACTemperature2() {
        Timber.tag(TAG).d("开始执行空调温度降低（副驾）")
        try {
            // 获取当前副驾温度
            val currentTemp = mCarProperty.getFloatProp(ID_TEMPERATURE_FRONTRIGHT)
            Timber.tag(TAG).w("当前空调温度: $currentTemp°C")
            
            // 计算新温度，不低于最小值
            val newTemp = maxOf((currentTemp ?: minTemperature) - temperatureStep, minTemperature)
            
            // 设置新温度
            mCarProperty.setFloatProp(ID_TEMPERATURE_FRONTRIGHT, newTemp)
            
            coroutineScope.launch {
                delay(1000)
                val nextTemp = mCarProperty.getFloatProp(ID_TEMPERATURE_FRONTRIGHT)
                // 显示toast提示
                ToastUtilOverApplication().showToast(
                    context, if (nextTemp != null && nextTemp <= minTemperature) {
                        "副驾空调温度已调到最低"
                    } else {
                        "副驾空调温度已调低到${nextTemp ?: newTemp}度"
                    }
                )
                Timber.tag(TAG).d("current temperature is ${nextTemp ?: newTemp}")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "降低空调温度失败")
        }
    }

    /**
     * 实际执行空调温度升高操作的内部方法（副驾）
     */
    private suspend fun executeIncreaseACTemperature2() {
        Timber.tag(TAG).d("开始执行空调温度升高（副驾）")
        try {
            // 获取当前副驾温度
            val currentTemp = mCarProperty.getFloatProp(ID_TEMPERATURE_FRONTRIGHT)
            Timber.tag(TAG).w("当前空调温度: $currentTemp°C")

            // 计算新温度，不超过最大值
            val newTemp = minOf((currentTemp ?: minTemperature) + temperatureStep, maxTemperature)

            // 设置新温度
            mCarProperty.setFloatProp(ID_TEMPERATURE_FRONTRIGHT, newTemp)

            coroutineScope.launch {
                delay(1000)
                val nextTemp = mCarProperty.getFloatProp(ID_TEMPERATURE_FRONTRIGHT)
                // 显示toast提示
                ToastUtilOverApplication().showToast(
                    context, if (nextTemp != null && nextTemp >= 32.5) {
                        "副驾空调温度已调到最高"
                    } else {
                        "副驾空调温度已调高到${nextTemp ?: newTemp}度"
                    }
                )
                Timber.tag(TAG).d("current temperature is ${nextTemp ?: newTemp}")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "提高空调温度失败")
        }
    }

    /**
     * 实际执行风量减少操作的内部方法
     */
    private suspend fun executeDecreaseFanSpeed() {
        Timber.tag(TAG).d("开始执行风量减少")
        try {
            val currentSpeed = mCarProperty.getIntProp(ID_BLW_LEVEL_FRONT)
            Timber.tag(TAG).w("当前空调风量: $currentSpeed")

            // 边界检查：如果风量小于1，直接返回
            if (currentSpeed < 1) {
                Timber.tag(TAG).d("风量已是最小值，无法再减少")
                return
            }

            // 特殊业务逻辑：如果风量为1，关闭空调
            if (currentSpeed == 1) {
                Timber.tag(TAG).d("风量为1直接关闭空调")
                mCarProperty.setIntProp(ID_WHOLE_CABIN_AIR_CD_FRONT, 0)
                return
            }

            val newSpeed = when (currentSpeed) {
                0x1 -> 0x0
                0x2 -> 0x1
                0x3 -> 0x2
                0x4 -> 0x3
                0x5 -> 0x4
                0x6 -> 0x5
                0x7 -> 0x6
                0x8 -> 0x9
                0xA -> 0x9
                0xB -> 0xC
                else -> 0x00
            }

            if (newSpeed != 0) {
                if (newSpeed != 0x9) {
                    // 计算新风量，不低于最小值
                    val newSpeed2 = maxOf(currentSpeed - 1, minFanSpeed)
                    mCarProperty.setIntProp(ID_BLW_LEVEL_FRONT, newSpeed2)
                } else {
                    val newSpeed2 = maxOf(currentSpeed - 2, minFanSpeed)
                    mCarProperty.setIntProp(ID_BLW_LEVEL_FRONT, newSpeed2)
                }
            }

            coroutineScope.launch {
                delay(1000)
                val nextFanLevel = mCarProperty.getIntProp(ID_BLW_LEVEL_FRONT)
                // 显示toast提示
                val mode1Desc = when (nextFanLevel) {
                    0x0 -> "已关闭"
                    0x1 -> "1"
                    0x2 -> "2"
                    0x3 -> "3"
                    0x4 -> "4"
                    0x5 -> "5"
                    0x6 -> "6"
                    0x7 -> "7"
                    0x8 -> "8"
                    0xA -> "9"
                    0xB -> "10"
                    else -> "未知"
                }
                ToastUtilOverApplication().showToast(context, "空调风量已调低到$mode1Desc")
                Timber.tag(TAG).d("current fan level is $nextFanLevel")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "降低空调风量失败")
        }
    }

    /**
     * 实际执行风量增加操作的内部方法
     */
    private suspend fun executeIncreaseFanSpeed() {
        Timber.tag(TAG).d("开始执行风量增加")
        try {
            val currentSpeed = mCarProperty.getIntProp(ID_BLW_LEVEL_FRONT)
            Timber.tag(TAG).w("当前空调风量: $currentSpeed")

            // 边界检查：如果已经是最大风量，直接返回
            if (currentSpeed >= 0xB) {
                Timber.tag(TAG).d("风量已是最大值，无法再增加")
                return
            }

            val newSpeed = when (currentSpeed) {
                0x1 -> 0x2
                0x2 -> 0x3
                0x3 -> 0x4
                0x4 -> 0x5
                0x5 -> 0x6
                0x6 -> 0x7
                0x7 -> 0x8
                0x8 -> 0x9
                0xA -> 0x9
                0xB -> 0x9
                else -> 0x01
            }

            if (newSpeed != 0x9) {
                // 计算新风量，不超过最大值
                val newSpeed2 = minOf(currentSpeed + 1, newSpeed)
                mCarProperty.setIntProp(ID_BLW_LEVEL_FRONT, newSpeed2)
            } else {
                val newSpeed2 = minOf(currentSpeed + 2, newSpeed)
                mCarProperty.setIntProp(ID_BLW_LEVEL_FRONT, newSpeed2)
            }

            coroutineScope.launch {
                delay(1000)
                val nextFanLevel = mCarProperty.getIntProp(ID_BLW_LEVEL_FRONT)
                // 显示toast提示
                val mode1Desc = when (nextFanLevel) {
                    0x1 -> "1"
                    0x2 -> "2"
                    0x3 -> "3"
                    0x4 -> "4"
                    0x5 -> "5"
                    0x6 -> "6"
                    0x7 -> "7"
                    0x8 -> "8"
                    0xA -> "9"
                    0xB -> "10"
                    else -> "未知"
                }
                ToastUtilOverApplication().showToast(context, "空调风量已调高到$mode1Desc")
                Timber.tag(TAG).d("current fan level is $nextFanLevel")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "提高空调风量失败")
        }
    }

    /**
     * 实际执行副驾座椅前移操作的内部方法
     */
    private suspend fun executeDecreasePassengerPosition() {
        Timber.tag(TAG).d("开始执行decreasePassengerPoistion")
        try {
            val carPropertyValue = CarPropertyValue(ID_SEAT_POSITION_FRONTRIGHT, 1)
            carPropertyValue.setExtension(2) // extension=2 表示前移
            mCarProperty.setRawProp(carPropertyValue)

            delay(250) // 使用协程的delay替代Thread.sleep

            val carPropertyValue2 = CarPropertyValue(ID_SEAT_POSITION_FRONTRIGHT, 0)
            carPropertyValue2.setExtension(2)
            mCarProperty.setRawProp(carPropertyValue2)

            ToastUtilOverApplication().showToast(context, "已向前移动副驾座椅")
            Timber.tag(TAG).d("decreasePassengerPoistion 执行完成")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "老板键减少状态失败")
        }
    }

    /**
     * 实际执行副驾座椅后移操作的内部方法
     */
    private suspend fun executeIncreasePassengerPosition() {
        Timber.tag(TAG).d("开始执行increasePassengerPoistion")
        try {
            val carPropertyValue = CarPropertyValue(ID_SEAT_POSITION_FRONTRIGHT, 1)
            carPropertyValue.setExtension(3) // extension=3 表示后移
            mCarProperty.setRawProp(carPropertyValue)

            delay(250) // 使用协程的delay替代Thread.sleep

            val carPropertyValue2 = CarPropertyValue(ID_SEAT_POSITION_FRONTRIGHT, 0)
            carPropertyValue2.setExtension(3)
            mCarProperty.setRawProp(carPropertyValue2)

            ToastUtilOverApplication().showToast(context, "已向后移动副驾座椅")
            Timber.tag(TAG).d("increasePassengerPoistion 执行完成")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "老板键增加状态失败")
        }
    }

    /**
     * 空调开关
     * 前排空调开关
     *
     * ID: 0x200006a (ID_WHOLE_CABIN_AIR_CD_FRONT)
     * ID: 0x2000042 (ID_AC_RUN_REQ)
     * 值类型: int
     */
    override suspend fun setAirCdFront(){
        Timber.tag(TAG).w("前排空调开关")
        try {

            val currentSpeed = mCarProperty.getIntProp(ID_BLW_LEVEL_FRONT)

            Timber.tag(TAG).w("空调风扇速度: $currentSpeed")

            if(currentSpeed==0) {
                mCarProperty.setIntProp(ID_AC_RUN_REQ, 1)
                //deviceEventsFlow.emit(DeviceEvent.SetFunction("", "前排空调 开启"))
                ToastUtilOverApplication().showToast(context, "前排空调已开启")
                Timber.tag(TAG).w("空调开关已设置为: 1")
            }else {
                mCarProperty.setIntProp(ID_WHOLE_CABIN_AIR_CD_FRONT, 0)

                //deviceEventsFlow.emit(DeviceEvent.SetFunction("", "前排空调 关闭"))
                ToastUtilOverApplication().showToast(context, "前排空调已关闭")
                Timber.tag(TAG).w("空调开关已设置为: 0")
            }

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "空调开关设置失败")
        }
    }

    /**
     * 小憩模式
     * 情景模式
     *
     * ID: 0x6400016a (ID_DC_3BD_CDCRESPITESETSTS) 小憩模式座椅信号
     * ID: 0x7000024 (ID_RESPITE_MODE) 小憩模式开关信号
     * 值类型: int
     * 值说明:
     *   0关1开。
     * 小憩模式座椅信号发送同时
     * 需要发小憩模式开关信号
     * 座椅1\2\3发 1
     * 座椅0发 0
     *小憩模式座椅信号
     * 0x0:关闭
     * 0x1:主驾
     * 0x2:副驾
     * 0x3:前排
     */
    override suspend fun setCdcRespiteSetSts(){
        Timber.tag(TAG).w("小憩模式")
        try {
            val currentState = mCarProperty.getIntProp(ID_DC_3BD_CDCRESPITESETSTS)
            Timber.tag(TAG).w("当前小憩模式座椅信号: $currentState")

            // 切换状态 (1→0, 0→1)
            val newState = when (currentState) {
                0 -> 1 // 如果当前关闭，则开启
                1 -> 2 // 如果当前非关闭状态，则关闭
                2 -> 3
                3 -> 0
                else -> 1
            }

            when(newState)
            {
                1-> mCarProperty.setIntProp(ID_RESPITE_MODE, 1)
                2-> mCarProperty.setIntProp(ID_RESPITE_MODE, 1)
                3-> mCarProperty.setIntProp(ID_RESPITE_MODE, 1)
                0-> mCarProperty.setIntProp(ID_RESPITE_MODE, 0)
            }
            mCarProperty.setIntProp(ID_DC_3BD_CDCRESPITESETSTS, newState)

            Timber.tag(TAG).w("小憩模式开关已设置为: $newState")

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "小憩模式开关设置失败")
        }
    }

    /**
     * 后视镜折叠
     * ID: 0x1000007 (ID_REAR_VIEW_MIRROR_FOLD_CONTROL)
     * 值类型: int
     */
    override suspend fun toRearViewMirrorFold(){
        Timber.tag(TAG).w("后视镜折叠开关")
        try {

            val currentState = mCarProperty.getIntProp(ID_REAR_VIEW_MIRROR_FOLD_CONTROL)
            Timber.tag(TAG).w("当前后视镜折叠状态: $currentState")

            // 切换状态 (1→0, 0→1)
            val newState = when (currentState) {
                0 -> 1 // 如果当前关闭，则开启
                1 -> 0 // 如果当前非关闭状态，则关闭
                else -> 1
            }

            mCarProperty.setIntProp(ID_REAR_VIEW_MIRROR_FOLD_CONTROL, newState)


            //deviceEventsFlow.emit(DeviceEvent.SetFunction("", "后视镜折叠${if (newState == 0) "关闭" else "开启"}"))
            val toastMessage = if (newState == 1) "后视镜已折叠" else "后视镜已展开"
            ToastUtilOverApplication().showToast(context, toastMessage)
            Timber.tag(TAG).w("后视镜折叠开关已设置为: $newState")

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "后视镜折叠开关设置失败")
        }
    }

    /**
     * 二排安全带未系报警提示音
     * ID: 0x3000078 (ID_SEAT_BELT_CHECK_REAR)
     * 值类型: int
     */
    override suspend fun toSeatBeltCheck(){
        Timber.tag(TAG).w("二排安全带未系报警提示音开关")
        try {

            val currentState = mCarProperty.getIntProp(ID_SEAT_BELT_CHECK_REAR)
            Timber.tag(TAG).w("当前二排安全带未系报警提示音开关状态: $currentState")

            // 切换状态 (1→0, 0→1)
            val newState = when (currentState) {
                0 -> 1 // 如果当前关闭，则开启
                1 -> 0 // 如果当前非关闭状态，则关闭
                else -> 1
            }

            mCarProperty.setIntProp(ID_SEAT_BELT_CHECK_REAR, newState)

            Timber.tag(TAG).w("二排安全带未系报警提示音开关已设置为: $newState")

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "二排安全带未系报警提示音开关设置失败")
        }
    }

    /**
     * 雨刮模式
     * ID: 0x9000081 (ID_WIPER_FRONT)
     * 值类型: int
     */
    override suspend fun toWiper(){
        Timber.tag(TAG).w("雨刮模式开关")
        try {

            val currentState = mCarProperty.getIntProp(ID_WIPER_FRONT)
            Timber.tag(TAG).w("雨刮模式开关状态: $currentState")

            // 切换状态 (1→0, 0→1)
            val newState = when (currentState) {
                0 -> 1 // 如果当前关闭，则开启
                1 -> 2 // 如果当前非关闭状态，则关闭
                2 -> 3
                3 -> 0
                else -> 1
            }

            mCarProperty.setIntProp(ID_WIPER_FRONT, newState)
            Timber.tag(TAG).w("雨刮模式开关已设置为: $newState")

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "雨刮模式开关设置失败")
        }
    }


    /**
     * 低速行人报警音
     * ID: 0xa000007 (ID_ACOUSTIC_VEH_ALERT)
     * 值类型: int
     */
    override suspend fun toLowSpeedPedestrianAlarm(){
        Timber.tag(TAG).w("低速行人报警音开关")
        try {

            val currentState = mCarProperty.getIntProp(ID_ACOUSTIC_VEH_ALERT)
            val newState2 = mCarProperty.getIntProp(ID_ACOUSTIC_VEH_ALERT2)
            Timber.tag(TAG).w("低速行人报警音开关状态: $currentState")

            // 切换状态 (1→0, 0→1)
            val newState = when (currentState) {
                0 -> 1 // 如果当前关闭，则开启
                1 -> 0 // 如果当前非关闭状态，则关闭
                else -> 1
            }
            
            mCarProperty.setIntProp(ID_ACOUSTIC_VEH_ALERT, newState)
            if(newState==1) {
                mCarProperty.setIntProp(ID_CHIME_AVAS_PREVIEW, newState2)

                //android.provider.Settings.System.putInt(context.contentResolver, "key_sound_alarm_switch", 1)
            }
//            else{
//                android.provider.Settings.System.putInt(context.contentResolver, "key_sound_alarm_switch", 0)
//            }

            val toastMessage = if (newState == 1) "低速行人报警音已开启" else "低速行人报警音已关闭"
            ToastUtilOverApplication().showToast(context, toastMessage)
            Timber.tag(TAG).w("低速行人报警音开关已设置为: $newState")

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "低速行人报警音开关设置失败")
        }
    }

    /**
     * 主驾通风
     * ID: 0x200008d (ID_SEAT_VENT_FRONTLEFT)
     * 值类型: int
     */
    override suspend fun toSeatVentilationLeft(){
        Timber.tag(TAG).w("主驾通风开关")
        try {

            val currentState = mCarProperty.getIntProp(ID_SEAT_VENT_FRONTLEFT)
            Timber.tag(TAG).w("主驾通风开关状态: $currentState")

            // 循环切换档位 (0→1→2→3→0)
            val newLevel = (currentState + 1) % 4

            val nameStirng = when (newLevel) {
                0 -> "关闭"  // 舒适→运动
                1 -> "低"  // 运动→经济
                2 -> "中"  // 经济→专属
                3 -> "高"  // 专属→自定义
                else -> "关闭" // 其他→舒适
            }

            mCarProperty.setIntProp(ID_SEAT_VENT_FRONTLEFT, newLevel)


            //deviceEventsFlow.emit(DeviceEvent.SetFunction("", "主驾座椅通风 $nameStirng"))
            val toastMessage = when (newLevel) {
                0 -> "主驾座椅通风已关闭"
                1 -> "主驾座椅通风已启用并设置为1档"
                2 -> "主驾座椅通风已启用并设置为2档"
                3 -> "主驾座椅通风已启用并设置为3档"
                else -> "主驾座椅通风已关闭"
            }
            ToastUtilOverApplication().showToast(context, toastMessage)
            Timber.tag(TAG).w("主驾通风开关已设置为: $newLevel")

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "主驾通风开关设置失败")
        }
    }

    /**
     * 副驾通风
     * ID: 0x200008e (ID_SEAT_VENT_FRONTRIGHT)
     * 值类型: int
     */
    override suspend fun toSeatVentilationRight(){
        Timber.tag(TAG).w("副驾通风开关")
        try {

            val currentState = mCarProperty.getIntProp(ID_SEAT_VENT_FRONTRIGHT)
            Timber.tag(TAG).w("副驾通风开关状态: $currentState")

            // 循环切换档位 (0→1→2→3→0)
            val newLevel = (currentState + 1) % 4

            val nameStirng = when (newLevel) {
                0 -> "关闭"  // 舒适→运动
                1 -> "低"  // 运动→经济
                2 -> "中"  // 经济→专属
                3 -> "高"  // 专属→自定义
                else -> "关闭" // 其他→舒适
            }

            mCarProperty.setIntProp(ID_SEAT_VENT_FRONTRIGHT, newLevel)


            //deviceEventsFlow.emit(DeviceEvent.SetFunction("", "副驾座椅通风 $nameStirng"))
            val toastMessage = when (newLevel) {
                0 -> "副驾座椅通风已关闭"
                1 -> "副驾座椅通风已启用并设置为1档"
                2 -> "副驾座椅通风已启用并设置为2档"
                3 -> "副驾座椅通风已启用并设置为3档"
                else -> "副驾座椅通风已关闭"
            }
            ToastUtilOverApplication().showToast(context, toastMessage)
            Timber.tag(TAG).w("副驾通风开关已设置为: $newLevel")

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "副驾通风开关设置失败")
        }
    }

    /**
     * 老板键 - 副驾座椅后移
     * ID: 0x300005d (ID_SEAT_POSITION_FRONTRIGHT)
     * 值类型: int
     */
    override suspend fun increasePassengerPoistion(){
        Timber.tag(TAG).d("收到increasePassengerPoistion请求")
        passengerPositionHandler.emit(PassengerPositionAction.INCREASE)
    }

    /**
     * 老板键 - 副驾座椅前移
     * ID: 0x300005d (ID_SEAT_POSITION_FRONTRIGHT)
     * 值类型: int
     */
    override suspend fun decreasePassengerPoistion(){
        Timber.tag(TAG).d("收到decreasePassengerPoistion请求")
        passengerPositionHandler.emit(PassengerPositionAction.DECREASE)
    }

    /**
     * 切换后备箱开关状态
     * 
     * ID: 0x6000031 (ID_DOOR_BACK)
     * 值类型: int
     * 值说明: 
     *   0=关闭
     *   1=开启
     *   2=打开中
     *   3=关闭中
     *   4=停止
     */
    override suspend fun toggleTrunk() {
        Timber.tag(TAG).w("切换后备箱开关状态")
        try {
            // 获取当前后备箱状态
            val currentState = mCarProperty.getIntProp(ID_DOOR_BACK)
            Timber.tag(TAG).w("当前后备箱状态: $currentState")

            // 切换状态 (0→1, 其他→0)
            val newState = when (currentState) {
                0 -> 1 // 如果当前关闭，则开启
                else -> 0 // 如果当前非关闭状态，则关闭
            }

            // 设置新状态
            mCarProperty.setIntProp(ID_DOOR_BACK, newState)
            //deviceEventsFlow.emit(DeviceEvent.SetFunction("", "后备箱${if (newState == 1) "开启" else "关闭"}"))

            val string = if (newState == 1) "后备箱已打开" else "后备箱已关闭"
            Timber.tag(TAG).w("设置后备箱状态为: $string")
            ToastUtilOverApplication().showToast(context, string)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "切换后备箱状态失败")
        }
    }
    
    /**
     * 切换儿童锁开关状态
     * 同时控制左右后儿童锁
     * 
     * ID: 0x64000164 (ID_CHILD_LOCK_LEFT)
     * ID: 0x64000165 (ID_CHILD_LOCK_RIGHT)
     * 值类型: int
     * 值说明:
     *   0x0=关闭
     *   0x1=开启
     */
    override suspend fun toggleChildLock1() {
        Timber.tag(TAG).w("切换左儿童锁开关状态")
        try {
            // 获取当前左侧儿童锁状态
            val currentStateLeft = mCarProperty.getIntProp(GW_288_BDCLERECHILDLOCKSTS)
            Timber.tag(TAG).w("当前儿童锁状态: 左=$currentStateLeft")
            
            // 切换状态 (0→1, 1→0)
            val newState = if (currentStateLeft == 0 ) 2 else 1
            
            // 设置新状态
            mCarProperty.setIntProp(CDC_2D2_CDCLERECHILDLOCKREQ, newState)

            val keyString = "左儿童锁${if (newState == 2) "已打开" else "已关闭"}"
            Timber.tag(TAG).w("设置左儿童锁状态为: ${if (newState == 2) "开启" else "关闭"}")
            ToastUtilOverApplication().showToast(context, keyString)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "切换左儿童锁状态失败")
        }
    }

    /**
     * 切换儿童锁开关状态
     * 同时控制左右后儿童锁
     *
     * ID: 0x64000164 (ID_CHILD_LOCK_LEFT)
     * ID: 0x64000165 (ID_CHILD_LOCK_RIGHT)
     * 值类型: int
     * 值说明:
     *   0x0=关闭
     *   0x1=开启
     */
    override suspend fun toggleChildLock2() {
        Timber.tag(TAG).w("切换右儿童锁开关状态")
        try {
            // 获取当前左侧儿童锁状态
            val currentStateRight = mCarProperty.getIntProp(GW_288_BDCRIRECHILDLOCKSTS)
            Timber.tag(TAG).w("当前儿童锁状态:右=$currentStateRight")

            // 切换状态 (0→1, 1→0)
            val newState = if ( currentStateRight == 0) 2 else 1

            // 设置新状态
            mCarProperty.setIntProp(CDC_2D2_CDCRIRECHILDLOCKREQ, newState)

            val keyString = "右儿童锁${if (newState == 2) "已打开" else "已关闭"}"
            Timber.tag(TAG).w("设置右儿童锁状态为: ${if (newState == 2) "开启" else "关闭"}")
            ToastUtilOverApplication().showToast(context, keyString)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "切换右儿童锁状态失败")
        }
    }

    /**
     * 切换方向盘加热开关状态
     * 
     * ID: 0x3000001 (ID_STEERING_WHEEL_HEATING)
     * 值类型: int
     * 值说明:
     *   0x0=关闭
     *   0x1=开启
     */
    override suspend fun toggleSteeringWheelHeat() {
        Timber.tag(TAG).w("切换方向盘加热开关状态")
        try {
            // 获取当前方向盘加热状态
            val currentState = mCarProperty.getIntProp(ID_STEERING_WHEEL_HEATING)
            Timber.tag(TAG).w("当前方向盘加热状态: $currentState")
            
            // 切换状态 (0→1, 1→0)
            val newState = if (currentState == 0) 1 else 0
            
            // 设置新状态
            mCarProperty.setIntProp(ID_STEERING_WHEEL_HEATING, newState)

            val toastMessage = if (newState == 1) "方向盘已开启加热" else "方向盘已取消加热"
            ToastUtilOverApplication().showToast(context, toastMessage)
            Timber.tag(TAG).w("设置方向盘加热状态为: ${if (newState == 1) "开启" else "关闭"}")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "切换方向盘加热状态失败")
        }
    }
    
    /**
     * 切换主驾座椅加热档位
     * 
     * ID: 0x2000088 (ID_SEAT_HEAT_FRONTLEFT)
     * 值类型: int
     * 值说明:
     *   0=关闭
     *   1=低档
     *   2=中档
     *   3=高档
     */
    override suspend fun toggleMainSeatHeat() {
        Timber.tag(TAG).w("切换主驾座椅加热档位")
        try {
            // 获取当前主驾座椅加热状态
            val currentLevel = mCarProperty.getIntProp(ID_SEAT_HEAT_FRONTLEFT)
            Timber.tag(TAG).w("当前主驾座椅加热等级: $currentLevel")
            
            // 循环切换档位 (0→1→2→3→0)
            val newLevel = (currentLevel + 1) % 4

            val nameStirng = when (newLevel) {
                0 -> "关闭"  // 舒适→运动
                1 -> "低"  // 运动→经济
                2 -> "中"  // 经济→专属
                3 -> "高"  // 专属→自定义
                else -> "关闭" // 其他→舒适
            }
            
            // 设置新状态
            mCarProperty.setIntProp(ID_SEAT_HEAT_FRONTLEFT, newLevel)

            //deviceEventsFlow.emit(DeviceEvent.SetFunction("", "主驾座椅加热 $nameStirng "))
            val toastMessage = when (newLevel) {
                0 -> "主驾座椅加热已关闭"
                1 -> "主驾座椅加热已启用并设置为低档"
                2 -> "主驾座椅加热已启用并设置为中档"
                3 -> "主驾座椅加热已启用并设置为高档"
                else -> "主驾座椅加热已关闭"
            }
            ToastUtilOverApplication().showToast(context, toastMessage)
            Timber.tag(TAG).w("设置主驾座椅加热为: $newLevel")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "设置主驾座椅加热失败")
        }
    }
    
    /**
     * 切换副驾座椅加热档位
     * 
     * ID: 0x2000089 (ID_SEAT_HEAT_FRONTRIGHT)
     * 值类型: int
     * 值说明:
     *   0=关闭
     *   1=低档
     *   2=中档
     *   3=高档
     */
    override suspend fun toggleCopilotSeatHeat() {
        Timber.tag(TAG).w("切换副驾座椅加热档位")
        try {
            // 获取当前副驾座椅加热状态
            val currentLevel = mCarProperty.getIntProp(ID_SEAT_HEAT_FRONTRIGHT)
            Timber.tag(TAG).w("当前副驾座椅加热等级: $currentLevel")
            
            // 循环切换档位 (0→1→2→3→0)
            val newLevel = (currentLevel + 1) % 4

            val nameStirng = when (newLevel) {
                0 -> "关闭"  // 舒适→运动
                1 -> "低"  // 运动→经济
                2 -> "中"  // 经济→专属
                3 -> "高"  // 专属→自定义
                else -> "关闭" // 其他→舒适
            }
            
            // 设置新状态
            mCarProperty.setIntProp(ID_SEAT_HEAT_FRONTRIGHT, newLevel)

            //deviceEventsFlow.emit(DeviceEvent.SetFunction("", "副驾座椅加热 $nameStirng "))
            val toastMessage = when (newLevel) {
                0 -> "副驾座椅加热已关闭"
                1 -> "副驾座椅加热已启用并设置为低档"
                2 -> "副驾座椅加热已启用并设置为中档"
                3 -> "副驾座椅加热已启用并设置为高档"
                else -> "副驾座椅加热已关闭"
            }
            ToastUtilOverApplication().showToast(context, toastMessage)
            Timber.tag(TAG).w("设置副驾座椅加热为: $newLevel")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "设置副驾座椅加热失败")
        }
    }

    /**
     * 切换驾驶模式
     * 
     * ID: 0x4000006 (ID_DRV_MODE)
     * 值类型: int
     * 值说明:
     *   1=舒适模式
     *   2=运动模式
     *   3=经济模式
     *   4=专属模式
     *   5=自定义模式
     * 
     * @return 当前模式描述
     */
    override suspend fun adjustDrivingMode() {
        if(VehicleTypeConstants.isMega8295){
            adjustDrivingMode8295()
        }else{
            adjustDrivingMode8155()
        }
    }

    private fun adjustDrivingMode8155() {
        Timber.tag(TAG).w("切换驾驶模式")
        try {
            // 获取当前驾驶模式
            val currentMode = mCarProperty.getIntProp(ID_DRV_MODE)
            Timber.tag(TAG).w("当前驾驶模式: $currentMode")
            
            // 需要的extension参数(385车型)
            val extensionParams: IntArray
            
            // 循环切换驾驶模式: 1舒适→2运动→4专属→0自定义→3经济→1舒适
            val newMode = when (currentMode) {
                1 -> 2  // 舒适→运动
                2 -> 4  // 运动→专属
                3 -> 1  // 经济→舒适
                4 -> 0  // 专属→自定义
                0 -> 3
                else -> 1 // 其他→舒适
            }

            Timber.tag(TAG).w("走到这")
            // 设置新的驾驶模式
//            val carPropertyValue = CarPropertyValue(ID_DRV_MODE,newMode)
//            carPropertyValue.setExtension(extensionParams)
//            mCarProperty.setRawProp(carPropertyValue)

            DriveModelManager.setDriveMode(newMode,context)

            // 获取模式描述
            val modeDesc = when (newMode) {
                1 -> "舒适模式"
                2 -> "运动模式"
                3 -> "经济模式"
                4 -> "专属模式"
                0 -> "自定义模式"
                else -> "未知模式"
            }

            val deeee = when (newMode) {
                1 -> "{1, 1, 1, 1}" // 舒适
                2 -> "{1, 2, 2, 2}" // 运动
                3 -> "{1, 2, 2, 2}" // 经济
                4 -> "{1, 1, 1, 1}"// 专属(与舒适相同，实际应弹窗选择)
//                5 -> intArrayOf(1, 1, 1, 1) // 自定义(实际应使用保存的自定义值)
                else -> "默认{1, 1, 1, 1}"
            }

            val toastMessage = when (newMode) {
                1 -> "驾驶模式已设置为舒适"
                2 -> "驾驶模式已设置为运动"
                3 -> "驾驶模式已设置为经济"
                4 -> "驾驶模式已设置为专属"
                0 -> "驾驶模式已设置为自定义"
                else -> "驾驶模式已设置为舒适"
            }
            ToastUtilOverApplication().showToast(context, toastMessage)
            Timber.tag(TAG).w("设置驾驶模式为: $modeDesc ($deeee)")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "切换驾驶模式失败")
        }
    }


    /**
     * 切换驾驶模式
     *
     * ID: 0x4000006 (ID_DRV_MODE)
     * 值类型: int
     * 值说明:
     *   1=舒适模式
     *   2=运动模式
     *   3=经济模式
     *   4=专属模式
     *   5=自定义模式
     *
     * @return 当前模式描述
     */
 fun adjustDrivingMode8295() {
        Timber.tag(TAG).w("切换驾驶模式")
        try {
            // 获取当前驾驶模式
            val currentMode = mCarProperty.getIntProp(ID_DRV_MODE)
            Timber.tag(TAG).w("当前驾驶模式: $currentMode")

            // 需要的extension参数(385车型)
            val extensionParams: IntArray

            // 循环切换驾驶模式: 1舒适→2运动→3经济→4专属→0自定义→1舒适
            val newMode = when (currentMode) {
                1 -> 2  // 舒适→运动
                2 -> 3  // 运动→经济
                3 -> 4  // 经济→专属
                4 -> 0  // 专属→自定义
                0 -> 1
                else -> 1 // 其他→舒适
            }

//            Timber.tag(TAG).w("走到这")
            // 设置新的驾驶模式
//            val carPropertyValue = CarPropertyValue(ID_DRV_MODE,newMode)
//            carPropertyValue.setExtension(extensionParams)
//            mCarProperty.setRawProp(carPropertyValue)

            DriveModelManager.setDriveMode(newMode,context)

            // 获取模式描述
            val modeDesc = when (newMode) {
                1 -> "舒适模式"
                2 -> "运动模式"
                3 -> "经济模式"
                4 -> "专属模式"
                0 -> "自定义模式"
                else -> "未知模式"
            }

            val deeee = when (newMode) {
                1 -> "{1, 1, 1, 1}" // 舒适
                2 -> "{1, 2, 2, 2}" // 运动
                3 -> "{1, 2, 2, 2}" // 经济
                4 -> "{1, 1, 1, 1}"// 专属(与舒适相同，实际应弹窗选择)
//                5 -> intArrayOf(1, 1, 1, 1) // 自定义(实际应使用保存的自定义值)
                else -> "默认{1, 1, 1, 1}"
            }

            val toastMessage = when (newMode) {
                1 -> "驾驶模式已设置为舒适"
                2 -> "驾驶模式已设置为运动"
                3 -> "驾驶模式已设置为经济"
                4 -> "驾驶模式已设置为专属"
                0 -> "驾驶模式已设置为自定义"
                else -> "驾驶模式已设置为舒适"
            }
            ToastUtilOverApplication().showToast(context, toastMessage)
            Timber.tag(TAG).w("设置驾驶模式为: $modeDesc ($deeee)")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "切换驾驶模式失败")
        }
    }
    
    /**
     * 切换能量管理模式
     * 
     * ID: 0x4000015 (ID_DRV_ENERGY_MODE)
     * 值类型: int
     * 值说明:
     *   0=关闭
     *   1=开启
     * 
     * @return 当前模式描述
     */
   override fun adjustEnergyManagement() {
        Timber.tag(TAG).w("切换能量管理模式")
        try {
            // 获取当前能量管理模式
            val currentMode = mCarProperty.getIntProp(ID_DRV_ENERGY_MODE)
            Timber.tag(TAG).w("当前能量管理模式: $currentMode")
            
            // 切换状态 (0→1, 1→0)
            val newMode = when (currentMode) {
                0 -> 1  // 市区
                1 -> 2  // 高速
                2 -> 0  // 山地
                else -> 1 // 市区
            }
            
            // 设置新的能量管理模式
            mCarProperty.setIntProp(ID_DRV_ENERGY_MODE, newMode)


            val newModeString = when (newMode) {
                0 -> "市区模式"  // 舒适→运动
                1 -> "高速模式"  // 运动→经济
                2 -> "山地模式"
                else -> ""
            }
            val toastMessage = when (newMode) {
                0 -> "能量管理模式已设置为市区"
                1 -> "能量管理模式已设置为高速"
                2 -> "能量管理模式已设置为山地"
                else -> "能量管理模式已设置为市区"
            }
            ToastUtilOverApplication().showToast(context, toastMessage)
            Timber.tag(TAG).w("设置能量管理模式为: $newMode")

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "切换能量管理模式失败")

        }
    }

    /**
     * 切换除雾除霜状态
     * 前后除霜除雾
     * ID_REQ_FRONT_DEFROST = 0x2000036
     * 前除霜：0x1
     * 后除霜：0x2
     * 关闭：OFF = 0x01,
     * 开启：ON = 0x02,
     * 循环模式为前开->后开->前关->后关->前开
     */
    override suspend fun toggleDefrost()  {
        Timber.tag(TAG).w("切换除雾除霜状态")
        try {
            // 获取当前除雾除霜状态
            val currentMode1 = mCarProperty.getIntProp(ID_REQ_FRONT_DEFROST)
            val currentMode2 = mCarProperty.getIntProp(ID_REAR_DEFROST)
            Timber.tag(TAG).w("切换除雾除霜状态: 前：$currentMode1，后：$currentMode2")

            // 循环除雾除霜模式: 前开->后开->前关->后关->前开
            val newMode1 = when (currentMode1) {
                0 -> 0x01  //前关后关->前开后关
                1 -> 0x00  //前开后关->前开后开
                else -> 0x01
            }

            val newMode2 = when (currentMode2) {
                0 -> 0x01  //前关后关->前开后关
                1 -> 0x00  //前开后关->前开后开
                else -> 0x01
            }


            // 设置新的除雾除霜模式
            mCarProperty.setIntProp(ID_REQ_FRONT_DEFROST, newMode1)
            mCarProperty.setIntProp(ID_REAR_DEFROST, newMode2)

            // 输出设置后的除雾除霜模式
            val mode1Desc = when (newMode1) {
                0x00 -> "前关"
                0x01 -> "前开"
                else -> "未知"
            }
            val mode2Desc = when (newMode2) {
                0x00 -> "后关"
                0x01 -> "后开"
                else -> "未知"
            }
            //deviceEventsFlow.emit(DeviceEvent.SetFunction("", "除雾除霜 $mode1Desc ，$mode2Desc"))
            val toastMessage = when {
                newMode1 == 0x01 && newMode2 == 0x01 -> "已开启前除霜和后除霜"
                newMode1 == 0x00 && newMode2 == 0x00 -> "已关闭前除霜和后除霜"
                newMode1 == 0x01 && newMode2 == 0x00 -> "已开启前除霜"
                newMode1 == 0x00 && newMode2 == 0x01 -> "已开启后除霜"
                else -> "除雾除霜状态已更新"
            }
            ToastUtilOverApplication().showToast(context, toastMessage)
            Timber.tag(TAG).w("设置除雾除霜模式: $mode1Desc ($newMode1)，$mode2Desc ($newMode2)")
            /*return mode1Desc + mode2Desc*/
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "调节除雾除霜模式失败")
            /*return "操作失败"*/
        }
    }

    /**
     * 切换除雾除霜状态
     * 前后除霜除雾
     * ID_REQ_FRONT_DEFROST = 0x2000036
     * 前除霜：0x1
     * 后除霜：0x2
     * 关闭：OFF = 0x01,
     * 开启：ON = 0x02,
     * 循环模式为前开->后开->前关->后关->前开
     */
    override suspend fun toggleDefrost1() {
        Timber.tag(TAG).w("切换前除雾除霜状态")
        try {
            // 获取当前除雾除霜状态
            val currentMode1 = mCarProperty.getIntProp(ID_REQ_FRONT_DEFROST)

            Timber.tag(TAG).w("切换前除雾除霜状态: $currentMode1")

            // 循环除雾除霜模式: 前开->后开->前关->后关->前开
            val newMode1 = when (currentMode1) {
                0 -> 0x01  //前关后关->前开后关
                1 -> 0x00  //前开后关->前开后开
                else -> 0x01
            }


            // 设置新的除雾除霜模式
            mCarProperty.setIntProp(ID_REQ_FRONT_DEFROST, newMode1)

            // 输出设置后的除雾除霜模式
            val mode1Desc = when (newMode1) {
                0x00 -> "关闭"
                0x01 -> "开启"
                else -> "未知"
            }

            //deviceEventsFlow.emit(DeviceEvent.SetFunction("", "除雾除霜 $mode1Desc ，$mode2Desc"))
            val toastMessage = if (newMode1 == 0x01) "已开启前除霜" else "已关闭前除霜"
            ToastUtilOverApplication().showToast(context, toastMessage)
            Timber.tag(TAG).w("设置前除雾除霜: $mode1Desc ($newMode1))")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "调节前除雾除霜模式失败")
        }
    }


    /**
     * 切换除雾除霜状态
     * 前后除霜除雾
     * ID_REQ_FRONT_DEFROST = 0x2000036
     * 前除霜：0x1
     * 后除霜：0x2
     * 关闭：OFF = 0x01,
     * 开启：ON = 0x02,
     * 循环模式为前开->后开->前关->后关->前开
     */
    override suspend fun toggleDefrost2() {
        Timber.tag(TAG).w("切换后除雾除霜状态")
        try {
            // 获取当前除雾除霜状态

            val currentMode2 = mCarProperty.getIntProp(ID_REAR_DEFROST)
            Timber.tag(TAG).w("切换后除雾除霜状态: $currentMode2")

            val newMode2 = when (currentMode2) {
                0 -> 0x01  //前关后关->前开后关
                1 -> 0x00  //前开后关->前开后开
                else -> 0x01
            }


            // 设置新的除雾除霜模式

            mCarProperty.setIntProp(ID_REAR_DEFROST, newMode2)


            val mode2Desc = when (newMode2) {
                0x00 -> "关闭"
                0x01 -> "开启"
                else -> "未知"
            }
            //deviceEventsFlow.emit(DeviceEvent.SetFunction("", "除雾除霜 $mode1Desc ，$mode2Desc"))
            val toastMessage = if (newMode2 == 0x01) "已开启后除霜" else "已关闭后除霜"
            ToastUtilOverApplication().showToast(context, toastMessage)
            Timber.tag(TAG).w("设置后除雾除霜: $mode2Desc ($newMode2)")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "调节后除雾除霜模式失败")
        }
    }


    /**
     * 切换中央控制状态
     */
    private fun toggleCentralControl() {
        Timber.tag(TAG).w("切换中央控制状态")
        // TODO: 实现中央控制状态切换逻辑
    }


    // 空调温度范围（摄氏度）
    private val minTemperature = 18f
    private val maxTemperature = 32.5f
    private val temperatureStep = 0.5f
    
    // 空调风量范围
    private val minFanSpeed = 1
    private val maxFanSpeed = 10

    /**
     * 提高空调温度（主驾）
     * 
     * ID: 0x2000077 (ID_TEMPERATURE_FRONTLEFT)
     * 值类型: float
     * 值说明: 
     *   17.5-32.5 步进0.5
     */
    override suspend fun increaseACTemperature() {
        Timber.tag(TAG).d("收到increaseACTemperature请求")
        temperatureHandler.emit(TemperatureAction.INCREASE)
    }

    /**
     * 提高空调温度（副驾）
     * 
     * ID: 0x2000078 (ID_TEMPERATURE_FRONTRIGHT)
     * 值类型: float
     * 值说明: 
     *   17.5-32.5 步进0.5
     */
    override fun increaseACTemperature2() {
        Timber.tag(TAG).d("收到increaseACTemperature2请求")
        coroutineScope.launch {
            temperatureHandler2.emit(TemperatureAction2.INCREASE)
        }
    }
    
    /**
     * 降低空调温度（主驾）
     * 
     * ID: 0x2000077 (ID_TEMPERATURE_FRONTLEFT)
     * 值类型: float
     * 值说明: 
     *   17.5-32.5 步进0.5
     */
    override suspend fun decreaseACTemperature() {
        Timber.tag(TAG).d("收到decreaseACTemperature请求")
        temperatureHandler.emit(TemperatureAction.DECREASE)
    }

    /**
     * 降低空调温度（副驾）
     * 
     * ID: 0x2000078 (ID_TEMPERATURE_FRONTRIGHT)
     * 值类型: float
     * 值说明: 
     *   17.5-32.5 步进0.5
     */
    override fun decreaseACTemperature2() {
        Timber.tag(TAG).d("收到decreaseACTemperature2请求")
        coroutineScope.launch {
            temperatureHandler2.emit(TemperatureAction2.DECREASE)
        }
    }

    /**
     * 增加风扇速度
     * 
     * ID: 0x200007e (ID_BLW_LEVEL_FRONT)
     * 值类型: int
     * 值说明: 
     *   1-10 对应1-10档
     */
    override suspend fun increaseFanSpeed() {
        Timber.tag(TAG).d("收到increaseFanSpeed请求")
        fanSpeedHandler.emit(FanSpeedAction.INCREASE)
    }
    
    /**
     * 降低风扇速度
     * 
     * ID: 0x200007e (ID_BLW_LEVEL_FRONT)
     * 值类型: int
     * 值说明: 
     *   1-10 对应1-10档
     */
    override suspend fun decreaseFanSpeed() {
        Timber.tag(TAG).d("收到decreaseFanSpeed请求")
        fanSpeedHandler.emit(FanSpeedAction.DECREASE)
    }

    /**
     * 调节空调吹风方向
     * 
     * ID: 0x2000084 (ID_BLW_DIRECT_FRONT)
     * 值类型: int
     * 值说明:
     *   1=吹脸
     *   2=吹脚
     *   3=吹脸吹脚
     *   4=除霜
     *   5=吹脸除霜
     *   6=吹脚除霜
     *   7=吹脸吹脚除霜
     *   9=自动
     *
     *   7种模式都有的车型：385MCA，D587,673-5,C385-2,C673-6。其余车型都是5种
     */
    override fun adjustAcWindDirection() {
        if (VehicleTypeConstants.isMega8155) {
            adjustAcWindDirection8155()
        } else {
            adjustAcWindDirection8295()
        }
    }

    private fun adjustAcWindDirection8155() {
        Timber.tag(TAG).w("调节空调吹风方向")
        try {
            var isFive:Boolean =true
            var model = MegaSystemProperties.getInt(DriveModelManager.PROP_ECU_CONFIG_C385_VEHICLE_TYPE, 0);
            var model2 = MegaSystemProperties.getInt(DriveModelManager.PROP_ECU_CONFIG_SUB_VEHICLEMODEL, 0);

            if(model2==0)
            {
                if(model==3||model==24||model==21||model==22)
                {
                    isFive=false
                }
            }else{
                if (model2==2)
                {
                    isFive=false
                }
            }
            var newDirection: Int=1
            // 获取当前吹风方向
            val currentDirection = mCarProperty.getIntProp(ID_BLW_DIRECT_FRONT)
            Timber.tag(TAG).w("当前吹风方向: $currentDirection")

            if(isFive){
                newDirection = when (currentDirection) {
                    1 -> 3  // 吹脸→吹脸吹脚
                    2 -> 6  // 吹脚→
                    3 -> 2  // 吹脸吹脚→吹脚 除霜
                    4 -> 1
                    6 -> 4
                    else -> 1 // 其他→吹脸
                }
            }else {
                newDirection = when (currentDirection) {
                    1 -> 3  // 吹脸→吹脸吹脚
                    2 -> 6  // 吹脚→吹脚除霜
                    3 -> 2  // 吹脸吹脚→吹脚
                    4 -> 7  // 除霜→吹脸吹脚除霜
                    7 -> 5  // 吹脸吹脚除霜->吹脸除霜
                    5 -> 1  // 吹脸除霜->吹脸
                    6 -> 4  // 吹脚除霜→除霜
                    else -> 1 // 其他→吹脸
                }
            }
            // 设置新的吹风方向
            mCarProperty.setIntProp(ID_BLW_DIRECT_FRONT, newDirection)
            
            // 获取吹风方向描述
            val directionDesc = when (newDirection) {
                1 -> "吹脸"
                2 -> "吹脚"
                3 -> "吹脸吹脚"
                4 -> "除霜"
                5 -> "吹脸除霜"
                6 -> "吹脚除霜"
                7 -> "吹脸吹脚除霜"
                9 -> "自动"
                else -> "未知"
            }
            //deviceEventsFlow.emit(DeviceEvent.SetFunction("", directionDesc))
            val toastMessage = when (newDirection) {
                1 -> "空调风向已切换为吹面"
                2 -> "空调风向已切换为吹脚"
                3 -> "空调风向已切换为吹面吹脚"
                4 -> "空调风向已切换为除霜"
                5 -> "空调风向已切换为吹脸除霜"
                6 -> "空调风向已切换为吹脚除霜"
                7 -> "空调风向已切换为吹脸吹脚除霜"
                9 -> "空调风向已切换为自动"
                else -> "空调风向已调节"
            }
            ToastUtilOverApplication().showToast(context, toastMessage)
            Timber.tag(TAG).w("设置吹风方向为: $directionDesc ($newDirection)")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "调节空调吹风方向失败")
        }
    }


    /**
     * 调节空调吹风方向
     *
     * ID: 0x2000084 (ID_BLW_DIRECT_FRONT)
     * 值类型: int
     * 值说明:
     *   1=吹脸
     *   2=吹脚
     *   3=吹脸吹脚
     *   4=除霜
     *   5=吹脸除霜
     *   6=吹脚除霜
     *   7=吹脸吹脚除霜
     *   9=自动
     *
     *   默认7种模式
     */
    fun adjustAcWindDirection8295() {
        Timber.tag(TAG).w("调节空调吹风方向")
        try {
            var isFive:Boolean =true
//            var model = MegaSystemProperties.getInt(DriveModelManager.PROP_ECU_CONFIG_C385_VEHICLE_TYPE, 0);
//            var model2 = MegaSystemProperties.getInt(DriveModelManager.PROP_ECU_CONFIG_SUB_VEHICLEMODEL, 0);

//            if(model2==0)
//            {
//                if(model==3||model==24||model==21||model==22)
//                {
//                    isFive=false
//                }
//            }else{
//                if (model2==2)
//                {
//                    isFive=false
//                }
//            }
            var newDirection: Int=1
            // 获取当前吹风方向
            val currentDirection = mCarProperty.getIntProp(ID_BLW_DIRECT_FRONT)
            Timber.tag(TAG).w("当前吹风方向: $currentDirection")

//            if(isFive){
//                newDirection = when (currentDirection) {
//                    1 -> 3  // 吹脸→吹脸吹脚
//                    2 -> 6  // 吹脚→
//                    3 -> 2  // 吹脸吹脚→吹脚 除霜
//                    4 -> 1
//                    6 -> 4
//                    else -> 1 // 其他→吹脸
//                }
//            }else {
                newDirection = when (currentDirection) {
                    1 -> 3  // 吹脸→吹脸吹脚
                    2 -> 4  // 吹脚→除霜
                    3 -> 2  // 吹脸吹脚→吹脚
                    4 -> 6  // 除霜->吹脚除霜
                    5 -> 7  // 吹脸除霜->吹脸吹脚除霜
                    6 -> 5  // 吹脚除霜->吹脸除霜
                    else -> 1 // 其他→吹脸
                }
//            }
            // 设置新的吹风方向
            mCarProperty.setIntProp(ID_BLW_DIRECT_FRONT, newDirection)

            // 获取吹风方向描述
            val directionDesc = when (newDirection) {
                1 -> "吹脸"
                2 -> "吹脚"
                3 -> "吹脸吹脚"
                4 -> "除霜"
                5 -> "吹脸除霜"
                6 -> "吹脚除霜"
                7 -> "吹脸吹脚除霜"
                9 -> "自动"
                else -> "未知"
            }
            //deviceEventsFlow.emit(DeviceEvent.SetFunction("", directionDesc))
            val toastMessage = when (newDirection) {
                1 -> "空调吹风已设置为吹面"
                2 -> "空调吹风已设置为吹脚"
                3 -> "空调吹风已设置为吹面吹脚"
                4 -> "空调吹风已设置为除霜"
                5 -> "空调吹风已设置为吹脸除霜"
                6 -> "空调吹风已设置为吹脚除霜"
                7 -> "空调吹风已设置为吹脸吹脚除霜"
                9 -> "空调吹风已设置为自动"
                else -> "空调吹风方向已更新"
            }
            ToastUtilOverApplication().showToast(context, toastMessage)
            Timber.tag(TAG).w("设置吹风方向为: $directionDesc ($newDirection)")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "调节空调吹风方向失败")
        }
    }

    /**
     * 调节按摩强度
     * 主驾和副驾同时设置
     * 
     * ID: 0x64000180 (ID_DSM_PARK_MASSAGE_STR) - 主驾
     * ID: 0x64000184 (ID_PSM_PARK_MASSAGE_STR) - 副驾
     * 值类型: int
     * 值说明:
     *   0=关闭
     *   1=低档
     *   2=中档
     *   3=高档
     */
    override suspend fun adjustMassageIntensity() {
        Timber.tag(TAG).w("调节按摩强度")
        try {
            // 获取当前主驾按摩强度
            val currentLevel = mCarProperty.getIntProp(ID_DSM_PARK_MASSAGE_STR)
            Timber.tag(TAG).w("当前按摩强度: $currentLevel")
            
            // 循环切换强度 (0→1→2→3→0)
            val newLevel = (currentLevel + 1) % 4
            
            // 设置新强度（主驾和副驾）
            mCarProperty.setIntProp(ID_DSM_PARK_MASSAGE_STR, newLevel)
            mCarProperty.setIntProp(ID_PSM_PARK_MASSAGE_STR, newLevel)
            
            val levelDesc = when (newLevel) {
                0 -> "关闭"
                1 -> "低档"
                2 -> "中档"
                3 -> "高档"
                else -> "未知"
            }
            Timber.tag(TAG).w("设置按摩强度为: $levelDesc ($newLevel)")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "调节按摩强度失败")
        }
    }
    
    /**
     * 调整按摩模式
     * 主驾和副驾同时设置
     * 
     * ID: 0x6400017f (ID_DSM_PARK_MASSAGE_MOD) - 主驾
     * ID: 0x64000183 (ID_PSM_PARK_MASSAGE_MOD) - 副驾
     * 值类型: int
     * 值说明:
     *   0=模式1波浪
     *   1=模式2猫步
     *   2=模式3蛇形
     *   3=模式4肩部
     *   4=模式5腰部
     *   5=模式6脉冲
     *   6=模式7舒适
     *   7=模式8推拿
     */
    override suspend fun adjustMassageMode() {
        Timber.tag(TAG).w("调整按摩模式")
        try {
            // 获取当前主驾按摩模式
            val currentMode = mCarProperty.getIntProp(ID_DSM_PARK_MASSAGE_MOD)
            Timber.tag(TAG).w("当前按摩模式: $currentMode")
            
            // 循环切换模式 (0→1→...→7→0)
            val newMode = (currentMode + 1) % 8
            
            // 设置新模式（主驾和副驾）
            mCarProperty.setIntProp(ID_DSM_PARK_MASSAGE_MOD, newMode)
            mCarProperty.setIntProp(ID_PSM_PARK_MASSAGE_MOD, newMode)
            
            val modeDesc = when (newMode) {
                0 -> "模式1波浪"
                1 -> "模式2猫步"
                2 -> "模式3蛇形"
                3 -> "模式4肩部"
                4 -> "模式5腰部"
                5 -> "模式6脉冲"
                6 -> "模式7舒适"
                7 -> "模式8推拿"
                else -> "未知"
            }
            Timber.tag(TAG).w("设置按摩模式为: $modeDesc ($newMode)")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "调整按摩模式失败")
        }
    }

    /**
     * 调节尾翼位置
     * 
     * ID: 0x64000221 (ID_PTS_ELE_TAIL_FUN_SET)
     * 值类型: int
     * 值说明:
     *   1=关闭
     *   2=开启
     *   3=随车速响应
     *   4=随运动模式开启
     * 
     * @return 当前位置描述
     */
    override suspend fun adjustTailWingPosition() {
        Timber.tag(TAG).w("调节尾翼位置")
        try {
            // 获取当前尾翼状态
            val currentPosition = mCarProperty.getIntProp(ID_PTS_ELE_TAIL_FUN_SET)
            Timber.tag(TAG).w("当前尾翼位置: $currentPosition")
            
            // 循环切换位置: 1→2→3→4→1
            val newPosition = when (currentPosition) {
                0 -> 2  // 关闭→开启
                1 -> 3  // 开启→随车速响应
                2 -> 4  // 随车速响应→随运动模式开启
                else -> 1 // 其他→关闭
            }
            
            // 设置新的尾翼位置
            mCarProperty.setIntProp(ID_PTS_ELE_TAIL_FUN_SET, newPosition)
            
            val positionDesc = when (newPosition) {
                1 -> "关闭"
                2 -> "开启"
                3 -> "随车速响应"
                4 -> "随运动模式开启"
                else -> "未知"
            }
            val toastMessage = when (newPosition) {
                1 -> "尾翼档位已关闭"
                2 -> "尾翼档位已开启"
                3 -> "尾翼档位设置为随车速响应"
                4 -> "尾翼档位设置为随运动模式开启"
                else -> "尾翼档位已关闭"
            }
            //ToastUtilOverApplication().showToast(context, toastMessage)
            Timber.tag(TAG).w("设置尾翼位置为: $positionDesc ($newPosition)")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "调节尾翼位置失败")
        }
    }
    
    /**
     * 调节副驾屏展开度数
     * 
     * ID: 0x6400011e (ID_CDC_TURN_VOICE_CTRL)
     * 值类型: int
     * 值说明:
     *   0x00-0x87: 0-135度
     *   0x88: 无请求
     * 
     * @return 当前角度描述
     */
    override fun adjustCopilotScreen() {
        Timber.tag(TAG).w("调节副驾屏展开度数")
        try {
            // 获取当前副驾屏角度
            val currentAngle = mCarProperty.getIntProp(ID_CDC_TURN_VOICE_CTRL)
            Timber.tag(TAG).w("当前副驾屏角度值: $currentAngle")
            
            // 计算角度(十六进制值对应的角度)
            val currentDegree = if (currentAngle in 0x00..0x87) {
                (currentAngle.toFloat() / 0x87.toFloat() * 135f).toInt()
            } else {
                0
            }
            
            // 循环切换角度: 0→45→90→135→0
            val newDegree = when {
                currentDegree < 30 -> 45
                currentDegree < 70 -> 90
                currentDegree < 110 -> 135
                else -> 0
            }
            
            // 计算对应的十六进制值
            val newAngle = (newDegree.toFloat() / 135f * 0x87.toFloat()).toInt()
            
            // 设置新的副驾屏角度
            mCarProperty.setIntProp(ID_CDC_TURN_VOICE_CTRL, newAngle)
            
            Timber.tag(TAG).w("设置副驾屏角度为: ${newDegree}度 (值=$newAngle)")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "调节副驾屏展开度数失败")
        }
    }
    override fun getAmbientLightColor(): Int {
        Timber.tag(TAG).w("开始获取氛围灯颜色")
        try {
            val value= mCarProperty.getIntProp(ID_Ambient_Light_Color)
            return value
            }   catch (e: Exception){
            e.printStackTrace()
            Timber.tag(TAG).e(e,"氛围灯颜色获取失败")
            return  -1
      }
    }

    override fun getAtmosphereLightSwtich():Int {
        Timber.tag(TAG).w("开始获取座舱氛围灯开关状态")
        try {
            val currentSwitch = mCarProperty.getIntProp(ID_Ambient_Light_Switch)
            return currentSwitch
            Timber.tag(TAG).w("舱氛围灯开关: $currentSwitch")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "氛围灯开关状态获取失败")
            return -1
        }
    }


    override fun getAtmosphereLightBrightness(): Int {
        return -1
    }

    override fun isIgnition(): Boolean {
        return true
    }

    override fun listenIsIgnition(callBack: (Boolean) -> Unit) {

    }
}