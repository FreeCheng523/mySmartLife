package com.zkjd.lingdong.service.executor


import android.content.Context
import com.example.tuotuotie_car_interface_library.ICarFunctionExecutor
import com.zkjd.lingdong.model.ButtonFunction
import com.zkjd.lingdong.model.ButtonType
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CarFunctionExecutorImp@Inject constructor(
    private val context: Context,
    private val carFunctionExecutor:ICarFunctionExecutor,
): ICarFunctionExecutor  by carFunctionExecutor {

    // 旋转功能执行频率限制
    private var lastRotateExecutionTime: Long = 0L
    private val ROTATE_INTERVAL_MS = 0L // 1秒限制
    
    // 后备箱功能执行频率限制
    private var lastTrunkExecutionTime: Long = 0L
    private val TRUNK_INTERVAL_MS = 7000L // 7秒限制

    companion object{
       const val TAG = "TinnoveCarFunctionExecutor"
    }

    suspend fun executeCarFunction(
        function: ButtonFunction,
        buttonType: ButtonType,
        macAddress: String
    ) {
        try {
            Timber.tag(TAG)
                .d("开始执行车辆功能: ${function.name}, 代码: ${function.actionCode}, 按键类型: $buttonType, MAC地址: $macAddress")

            val array = function.useType.toString().split("")
            Timber.tag(TAG).d("功能使用类型: ${function.useType}, 解析后数组: ${array}")
            
            // 处理旋转类功能
            if (array[1].toInt() == 2 &&
                (buttonType == ButtonType.LEFT_ROTATE || buttonType == ButtonType.RIGHT_ROTATE)
            ) {
                // 检查旋转功能执行频率限制
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastRotateExecutionTime < ROTATE_INTERVAL_MS) {
                    Timber.tag(TAG).w("旋转功能调用过于频繁，跳过本次调用")
                    return
                }
                lastRotateExecutionTime = currentTime
                
                Timber.tag(TAG).d("检测到旋转类功能，开始处理旋转操作")
            when (function.actionCode) {
                //单温区
                "CAR_AC_TEMPERATURE"->{
                    Timber.tag(TAG).d("执行主驾空调温度调节，旋转方向: $buttonType")
                    if (buttonType == ButtonType.RIGHT_ROTATE) {
                        // 顺时针旋转，增加温度
                        Timber.tag(TAG).d("顺时针旋转，增加主驾空调温度")
                        increaseACTemperature()
                    } else {
                        // 逆时针旋转，降低温度
                        Timber.tag(TAG).d("逆时针旋转，降低主驾空调温度")
                        decreaseACTemperature()
                    }
                    return
                }
                //主驾
                "CAR_AC_TEMPERATURE1" -> {
                    // 空调温度调节
                    Timber.tag(TAG).d("执行主驾空调温度调节，旋转方向: $buttonType")
                    if (buttonType == ButtonType.RIGHT_ROTATE) {
                        // 顺时针旋转，增加温度
                        Timber.tag(TAG).d("顺时针旋转，增加主驾空调温度")
                        increaseACTemperature()
                    } else {
                        // 逆时针旋转，降低温度
                        Timber.tag(TAG).d("逆时针旋转，降低主驾空调温度")
                        decreaseACTemperature()
                    }
                    return
                }

                //副驾 l06空调调节没有主副驾之分
                "CAR_AC_TEMPERATURE2" -> {
                    // 空调温度调节
                    Timber.tag(TAG).d("执行副驾空调温度调节，旋转方向: $buttonType")
                    if (buttonType == ButtonType.RIGHT_ROTATE) {
                        // 顺时针旋转，增加温度
                        Timber.tag(TAG).d("顺时针旋转，增加副驾空调温度")
                        increaseACTemperature2()
                    } else {
                        // 逆时针旋转，降低温度
                        Timber.tag(TAG).d("逆时针旋转，降低副驾空调温度")
                        decreaseACTemperature2()
                    }
                    return
                }

                "CAR_AC_FAN_SPEED" -> {
                    // 空调风量调节
                    Timber.tag(TAG).d("执行空调风量调节，旋转方向: $buttonType")
                    if (buttonType == ButtonType.RIGHT_ROTATE) {
                        // 顺时针旋转，增加风量
                        Timber.tag(TAG).d("顺时针旋转，增加空调风量")
                        increaseFanSpeed()
                    } else {
                        // 逆时针旋转，降低风量
                        Timber.tag(TAG).d("逆时针旋转，降低空调风量")
                        decreaseFanSpeed()
                    }
                    return
                }

                //老板键
                "CAR_BOSS_KEY" -> {
                    Timber.tag(TAG).d("执行老板键功能，旋转方向: $buttonType")
                    if (buttonType == ButtonType.RIGHT_ROTATE) {
                        // 顺时针旋转，
                        Timber.tag(TAG).d("顺时针旋转老板键")
                        increasePassengerPoistion()
                    } else {
                        // 逆时针旋转
                        Timber.tag(TAG).d("逆时针旋转老板键")
                        decreasePassengerPoistion()
                    }
                    return

                }
            }
        }

        // 处理普通功能
        Timber.tag(TAG).d("处理普通功能，功能代码: ${function.actionCode}")
        when (function.actionCode) {
            // 后备箱相关功能 - 7秒内只能调用一次
            "CAR_TRUNK_TOGGLE" -> {
                // 检查后备箱功能执行频率限制
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTrunkExecutionTime < TRUNK_INTERVAL_MS) {
                    Timber.tag(TAG).w("后备箱功能调用过于频繁，跳过本次调用")
                    return
                }
                lastTrunkExecutionTime = currentTime
                Timber.tag(TAG).d("执行后备箱开关功能")
                toggleTrunk()
            }

            // 车门相关功能
            "CAR_CHILD_LOCK_TOGGLE1" -> toggleChildLock1()
            "CAR_CHILD_LOCK_TOGGLE2" -> toggleChildLock2()

            // 座椅相关功能
            "CAR_STEERING_WHEEL_HEAT_TOGGLE" -> toggleSteeringWheelHeat()
            "CAR_MAIN_SEAT_HEAT_TOGGLE" -> {
                Timber.tag(TAG).d("执行主驾座椅加热切换")
                toggleMainSeatHeat()
            }
            "CAR_COPILOT_SEAT_HEAT_TOGGLE" -> {
                Timber.tag(TAG).d("执行副驾座椅加热切换")
                toggleCopilotSeatHeat()
            }

            // 按摩相关功能
            "CAR_MASSAGE_INTENSITY" -> adjustMassageIntensity()
            "CAR_MASSAGE_MODE" -> adjustMassageMode()


            // 空调相关功能
            "CAR_AC_TEMPERATURE_UP" -> {
                Timber.tag(TAG).d("执行空调温度升高")
                increaseACTemperature()
            }
            "CAR_AC_TEMPERATURE_DOWN" -> {
                Timber.tag(TAG).d("执行空调温度降低")
                decreaseACTemperature()
            }
            "CAR_FAN_SPEED_UP" -> {
                Timber.tag(TAG).d("执行风量增加")
                increaseFanSpeed()
            }
            "CAR_FAN_SPEED_DOWN" -> {
                Timber.tag(TAG).d("执行风量减少")
                decreaseFanSpeed()
            }
            "CAR_AC_WIND_DIRECTION" -> {
                Timber.tag(TAG).d("执行空调风向调节")
                adjustAcWindDirection()
            }

            "CAR_DEFROST_TOGGLE" -> toggleDefrost()

            // 驾驶模式相关功能
            "CAR_DRIVING_MODE" -> {
                adjustDrivingMode()
            }

            "CAR_ENERGY_MANAGEMENT" -> adjustEnergyManagement()

            // 尾翼控制
            "CAR_TRUNK_POSITION" -> adjustTailWingPosition()

            // 屏幕相关功能
            "CAR_COPILOT_SCREEN_TOGGLE" -> adjustCopilotScreen()

            //除雾除霜
            "CAR_DEFROST1" -> {
                Timber.tag(TAG).d("执行前挡风玻璃除雾除霜")
                toggleDefrost1()
            }
            "CAR_DEFROST2" -> {
                Timber.tag(TAG).d("执行后挡风玻璃除雾除霜")
                toggleDefrost2()
            }
            //空调开关
            "CAR_AIR_CONDITIONING" -> setAirCdFront()

            //主驾小憩模式
            "CAR_RESTING_MODE_LEFT" -> setCdcRespiteSetSts()
            //副驾小憩模式
            "CAR_RESTING_MODE_RIGHT" -> setCdcRespiteSetSts()
            //后视镜折叠
            "CAR_FOLDING_REARVIEW_MIRROR" -> toRearViewMirrorFold()
            //低速行人报警音
            "CAR_LOW_SPEED_PEDESTRIAN" -> toLowSpeedPedestrianAlarm()

            "CAR_SEAT_BELT_NOTFASTENED" -> toSeatBeltCheck()

            "CAR_WIPER_MODE" -> toWiper()

            //主驾座椅通风开/关
            "CAR_LEFT_REAR_VENT_TOGGLE" -> {
                Timber.tag(TAG).d("执行主驾座椅通风切换")
                toSeatVentilationLeft()
            }

            //副驾座椅通风开/关
            "CAR_RIGHT_REAR_VENT_TOGGLE" -> {
                Timber.tag(TAG).d("执行副驾座椅通风切换")
                toSeatVentilationRight()
            }
            else -> {
                Timber.tag(TAG)
                    .w("未知车辆功能代码: ${function.actionCode}")
            }
        }
        
        Timber.tag(TAG).d("车辆功能执行完成: ${function.name}")
        } catch (e: Exception) {
            Timber.tag(TAG).e("执行车辆功能时发生异常: ${function.name}, 错误: ${e.message}", e)
        }
    }
}