package com.smarlife.tuotiecarimpllibrary

import android.car.Car
import android.car.VehicleAreaSeat
import android.car.VehicleAreaType
import android.car.VehicleAreaWindow
import android.car.hardware.CarSensorManager
import android.car.hardware.cabin.CarCabinManager
import android.car.hardware.cluster.CarClusterInteractionManager
import android.car.hardware.hvac.CarHvacManager
import android.content.Context
import android.util.Log
import com.android.car.internal.util.TextUtils
import com.example.tuotuotie_car_interface_library.ICarFunctionExecutor
import com.mine.baselibrary.ActionNameProvider
import com.mine.baselibrary.BidirectionalAction
import com.mine.baselibrary.BoundaryCheckResult
import com.mine.baselibrary.BoundaryChecker
import com.mine.baselibrary.Executor
import com.mine.baselibrary.ReactiveFlowHandler
import com.mine.baselibrary.ResourceChecker
import com.mine.baselibrary.window.ToastUtilOverApplication
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread


@Singleton
class TinnoveCarFunctionExecutorImp @Inject constructor(@ApplicationContext private val context: Context) : ICarFunctionExecutor {
    private var mCarHvacManager: CarHvacManager? = null
    private var mCarCabinManager: CarCabinManager? = null

    private var mCarClusterInteractionManager: CarClusterInteractionManager? = null

    private var mCarSensorManager: CarSensorManager?=null

    private val TAG = "TinnoveCar"

    // 频率限制相关变量
    private var lastInitTime: Long = 0L
    private val INIT_INTERVAL_MS = 6000L // 6秒限制

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Flow 延迟响应相关
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
    
    // 座椅位置控制 Flow 处理器
    private val passengerPositionHandler: ReactiveFlowHandler<PassengerPositionAction> by lazy {
        ReactiveFlowHandler(
            context = context,
            tag = TAG,
            boundaryChecker = object : BoundaryChecker<PassengerPositionAction> {
                override suspend fun checkBoundary(
                    action: PassengerPositionAction,
                    currentValue: Int
                ): BoundaryCheckResult {
                    return when (action) {
                        PassengerPositionAction.DECREASE -> {
                            if (currentValue <= 2) {
                                BoundaryCheckResult(false, "副驾座椅已在最前，无法再向前移动")
                            } else {
                                BoundaryCheckResult(    true)
                            }
                        }

                        PassengerPositionAction.INCREASE -> {
                            if (currentValue >= 200) {
                                BoundaryCheckResult(false, "副驾座椅已在最后，无法再向后移动")
                            } else {
                                BoundaryCheckResult(true)
                            }
                        }
                    }
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
                    return checkAndReconnectCabinManager()
                }

                override fun getResourceName(): String = "CarCabinManager"
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
                mCarCabinManager?.getIntProperty(
                    CarCabinManager.ID_BODY_SEAT_PSM_SET,
                    0x00
                ).also {
                    Log.i(TAG,"当前座椅位置:$it")
                }
            }
        )
    }

    // 风量控制 Flow 处理器
    private val fanSpeedHandler: ReactiveFlowHandler<FanSpeedAction> by lazy {
        ReactiveFlowHandler(
            context = context,
            tag = TAG,
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
                            //例如：当空调是3档，但是空调是关闭的，获取的档位为0，不是实际挡位，所以这里不做限制
                            //BoundaryCheckResult(true)
                        }

                        FanSpeedAction.INCREASE -> {
                            if (currentValue >= 8) {
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
                    return checkAndReconnectHvacManager()
                }

                override fun getResourceName(): String = "CarHvacManager"
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
                mCarHvacManager?.getIntProperty(
                    CarHvacManager.ID_HVAC_FAN_SPEED_ACK,
                    VehicleAreaSeat.SEAT_ROW_FRONT
                )
            }
        )
    }
    
    // 温度控制 Flow 处理器
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
                            if (currentTemp <= 17.5f) {
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
                    return checkAndReconnectHvacManager()
                }

                override fun getResourceName(): String = "CarHvacManager"
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
                mCarHvacManager?.getFloatProperty(
                    CarHvacManager.ID_ZONED_TEMP_SETPOINT,
                    VehicleAreaSeat.SEAT_MAIN_DRIVER
                )?.let { (it * 10).toInt() }
            },
            maxRequestsPerPeriod = 5
        )
    }
    
    init {
        init()
        // 初始化 Flow 处理器（通过 lazy 初始化）
        passengerPositionHandler
        fanSpeedHandler
        temperatureHandler
    }

    // Car属性工具类
    private val carPropertyUtils: VirtualCarUtils by lazy {
        VirtualCarUtils(context)
    }

    /**
     * 初始化Car服务
     * 限制：6秒内最多调用一次
     */
    fun init(){
        val currentTime = System.currentTimeMillis()
        //因为createCar内部失败会有5s的重试时间，所以调用频率限制为6s
        if (currentTime - lastInitTime < INIT_INTERVAL_MS) {
            Timber.tag(TAG).w("init调用过于频繁，跳过本次调用")
            return
        }

        lastInitTime = currentTime
        thread {
            Timber.tag(TAG).d("开始初始化TinnoveCarFunctionExecutor")
            try {
                Car.createCar(context, null, 500) { car, ready ->
                    if (ready) {
                        Timber.tag(TAG).d("Car服务连接成功")
                        try {
                            mCarHvacManager = car.getCarManager(Car.HVAC_SERVICE) as? CarHvacManager
                            mCarCabinManager =
                                car.getCarManager(Car.CABIN_SERVICE) as? CarCabinManager
                            mCarClusterInteractionManager =
                                car.getCarManager(Car.INSTRUMENT_PANEL_SERVICE) as CarClusterInteractionManager?

                            mCarSensorManager =
                                car.getCarManager(Car.SENSOR_SERVICE) as CarSensorManager?

                            Timber.tag(TAG)
                                .d("TinnoveCarFunctionExecutor初始化完成 - HVAC: ${mCarHvacManager != null}, Cabin: ${mCarCabinManager != null}")
                        } catch (e: Exception) {
                            Timber.tag(TAG).e("获取TinnoveCarFunctionExecutor失败: ${e.message}")
                        }
                    } else {
                        Timber.tag(TAG).e("Car服务连接失败")
                    }
                }
            } catch (e: Throwable) {
                Timber.tag(TAG).e("初始化Car服务时发生异常: ${e.message}")
            }
        }
    }


    /**
     * 检查CarHvacManager是否可用，如果为空则尝试重新初始化
     */
    private fun checkAndReconnectHvacManager(): Boolean {
        if (mCarHvacManager == null) {
            Timber.tag(TAG).e("CarHvacManager为空，尝试重新连接")
            init()
            return false
        }
        return true
    }

    /**
     * 检查CarCabinManager是否可用，如果为空则尝试重新初始化
     */
    private fun checkAndReconnectCabinManager(): Boolean {
        if (mCarCabinManager == null) {
            Timber.tag(TAG).e("CarCabinManager为空，尝试重新连接")
            init()
            return  false
        }
        return true
    }



    override suspend fun setAirCdFront() {
        Timber.tag(TAG).d("开始执行setAirCdFront切换")
        if (!checkAndReconnectHvacManager()) {
            return
        }
        try {

            val currentState = carPropertyUtils.getAirCdFront()
            Timber.tag(TAG).w("空调开关状态: $currentState")

            // 切换状态 (1→2, 2→1, 其他→2)
            val newState = when (currentState) {
                0x1 -> 0x2 // 如果当前关闭，则开启
                0x2 -> 0x1 // 如果当前开启，则关闭
                else -> 0x2 // 其他状态（未知等）则默认开启
            }

            // 设置新状态
            carPropertyUtils.setAirCdFront(newState)

            // 显示toast提示
            val toastMessage = if (newState == 0x2) "前排空调已开启" else "前排空调已关闭"
            ToastUtilOverApplication().showToast(context, toastMessage)

            Timber.tag(TAG).w("设置空调状态为: ${if (newState == 0x2) "开启" else "关闭"}")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "切换空调状态失败")
        }
    }

    override suspend fun toLowSpeedPedestrianAlarm() {
        Timber.tag(TAG).d("开始执行toLowSpeedPedestrianAlarm切换")
        if (!checkAndReconnectHvacManager()) {
            return
        }
        Timber.tag(TAG).w("低速行人报警音开关")
        try {


            val currentState = mCarClusterInteractionManager?.getIntProperty(
                CarClusterInteractionManager.ID_IP_CLUSTER_SOUND_EFFECT_SWITCH,
                0x3)

            Timber.tag(TAG).w("低速行人报警音开关状态: $currentState")

            // 切换状态 (1→0, 0→1)
            val newState = when (currentState) {
                0 -> 1 // 如果当前关闭，则开启
                1 -> 0 // 如果当前非关闭状态，则关闭
                else -> 1
            }
            mCarClusterInteractionManager?.setIntProperty(
                CarClusterInteractionManager.ID_IP_CLUSTER_SOUND_EFFECT_SWITCH,
                0x3, newState)

            // 显示toast提示
            val toastMessage = if (newState == 1) "低速行人报警音已开启" else "低速行人报警音已关闭"
           // ToastUtilOverApplication().showToast(context, toastMessage)

            Timber.tag(TAG).w("低速行人报警音开关已设置为: $currentState")

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "低速行人报警音开关设置失败")
        }

    }

    override suspend fun decreasePassengerPoistion() {
        Timber.tag(TAG).d("收到decreasePassengerPoistion请求")
        passengerPositionHandler.emit(PassengerPositionAction.DECREASE)
    }

    override suspend fun increasePassengerPoistion() {
        Timber.tag(TAG).d("收到increasePassengerPoistion请求")
        passengerPositionHandler.emit(PassengerPositionAction.INCREASE)
    }


    /**
     * 实际执行副驾座椅前移操作的内部方法
     */
    private suspend fun executeDecreasePassengerPosition() {
        Timber.tag(TAG).d("开始执行decreasePassengerPoistion切换")
        if (!checkAndReconnectCabinManager()) {
            return
        }
        mCarCabinManager?.let { carCabinManager ->
           val currentPosition = carCabinManager.getIntProperty(
                CarCabinManager.ID_BODY_SEAT_PSM_SET,
                0x00
            )

            val nextPoistion =Math.max(currentPosition-20,1)
            if(nextPoistion>=1) {
                //data[0]=0x00, data[1]=HU_HorizontalPositionSts(0x0=Inactive；0x1~0xCB=0%~100%；0xCC~0xFE=Reserved；0xFF=Invalid)
                carCabinManager.setIntProperty(
                    CarCabinManager.ID_BODY_SEAT_PSM_SET,
                    0x00,
                    nextPoistion
                )
                // 显示toast提示
                ToastUtilOverApplication().showToast(context, "已向前移动副驾座椅")
                Timber.tag(TAG).e("decreasePassengerPoistion set to $nextPoistion")
            }else{
                Timber.tag(TAG).e("decreasePassengerPoistion 已经是最前面")
            }
        }?:let {
            Timber.tag(TAG).e("CarHvacManager为空，无法decreasePassengerPoistion功能")
        }
    }

    /**
     * 实际执行副驾座椅后移操作的内部方法
     */
    private suspend fun executeIncreasePassengerPosition() {
        Timber.tag(TAG).d("开始执行increasePassengerPoistion切换")
        if (!checkAndReconnectCabinManager()) {
            return
        }
        mCarCabinManager?.let { carCabinManager ->
            val currentPosition = carCabinManager.getIntProperty(
                CarCabinManager.ID_BODY_SEAT_PSM_SET,
                0x00
            )
            val nextPoistion =Math.min(currentPosition+20,201)//步长为1不行
            if(nextPoistion<=201) {
                //data[0]=0x00, data[1]=HU_HorizontalPositionSts(0x0=Inactive；0x1~0xCB=0%~100%；0xCC~0xFE=Reserved；0xFF=Invalid)
                carCabinManager.setIntProperty(
                    CarCabinManager.ID_BODY_SEAT_PSM_SET,
                    0x00,
                    nextPoistion
                )
                // 显示toast提示
                ToastUtilOverApplication().showToast(context, "已向后移动副驾座椅")
                Timber.tag(TAG).e("increasePassengerPoistion set to $nextPoistion")
            }
        }?:let {
            Timber.tag(TAG).e("CarCabinManager为空，无法执行increasePassengerPoistion功能")
        }
    }



    override suspend fun toSeatVentilationLeft() {
        Timber.tag(TAG).d("开始执行主驾座椅通风切换")
        if (!checkAndReconnectHvacManager()) {
            return
        }

        mCarHvacManager?.let { carHvacManager ->
            val currentLevel = carHvacManager.getIntProperty(
                CarHvacManager.ID_HVAC_SEAT_VENTILATION,
                VehicleAreaSeat.SEAT_MAIN_DRIVER
            )
            Timber.tag(TAG).d("主驾座椅通风当前等级: $currentLevel")

            val newLevel = (currentLevel + 1) % 4
            Timber.tag(TAG).d("主驾座椅通风设置新等级: $newLevel")

            carHvacManager.setIntProperty(
                CarHvacManager.ID_HVAC_SEAT_VENTILATION,
                VehicleAreaSeat.SEAT_MAIN_DRIVER, newLevel
            )

            // 显示toast提示
            val toastMessage = when (newLevel) {
                0 -> "主驾座椅通风已关闭"
                1 -> "主驾座椅通风已启用并设置为1档"
                2 -> "主驾座椅通风已启用并设置为2档"
                3 -> "主驾座椅通风已启用并设置为3档"
                else -> "主驾座椅通风已关闭"
            }
            ToastUtilOverApplication().showToast(context, toastMessage)

            Timber.tag(TAG).d("主驾座椅通风设置完成")
        } ?: run {
            Timber.tag(TAG).e("CarHvacManager为空，无法执行主驾座椅通风功能")
        }
    }

    override suspend fun toSeatVentilationRight() {
        Timber.tag(TAG).d("开始执行副驾座椅通风切换")
        if (!checkAndReconnectHvacManager()) {
            return
        }

        mCarHvacManager?.let { carHvacManager ->
            val currentLevel = carHvacManager.getIntProperty(
                CarHvacManager.ID_HVAC_SEAT_VENTILATION,
                VehicleAreaSeat.SEAT_PASSENGER
            )
            Timber.tag(TAG).d("副驾座椅通风当前等级: $currentLevel")

            val newLevel = (currentLevel + 1) % 4
            Timber.tag(TAG).d("副驾座椅通风设置新等级: $newLevel")

            carHvacManager.setIntProperty(
                CarHvacManager.ID_HVAC_SEAT_VENTILATION,
                VehicleAreaSeat.SEAT_PASSENGER, newLevel
            )

            // 显示toast提示
            val toastMessage = when (newLevel) {
                0 -> "副驾座椅通风已关闭"
                1 -> "副驾座椅通风已启用并设置为1档"
                2 -> "副驾座椅通风已启用并设置为2档"
                3 -> "副驾座椅通风已启用并设置为3档"
                else -> "副驾座椅通风已关闭"
            }
            ToastUtilOverApplication().showToast(context, toastMessage)

            Timber.tag(TAG).d("副驾座椅通风设置完成")
        } ?: run {
            Timber.tag(TAG).e("CarHvacManager为空，无法执行副驾座椅通风功能")
        }
    }




    override suspend fun toggleDefrost1() {
        /**
         * 常量：HvacPropertyValue : int32_t {
         *     关闭
         *     OFF = 0x01,
         *  开启
         *     ON = 0x02,
         *  请求改变状态
         *     AUTO_ON_OFF = 0x03,
         *  可用，可操作状态等等
         *     ACTIVE = 0x04,
         *  不可用，不可操作状态，置灰等等
         *     INACTIVE = 0x05,
         * };
         */
        Timber.tag(TAG).d("开始执行前挡风玻璃除雾除霜")
        if (!checkAndReconnectHvacManager()) {
            return
        }

        mCarHvacManager?.let { carHvacManager ->

            val currentStatus = carHvacManager.getIntProperty(
                CarHvacManager.ID_WINDOW_DEFROSTER_ON,
                VehicleAreaWindow.WINDOW_FRONT_WINDSHIELD
            )

            if(currentStatus == 0x01){//当前为关闭
                carHvacManager.setIntProperty(
                    CarHvacManager.ID_WINDOW_DEFROSTER_ON,
                    VehicleAreaWindow.WINDOW_FRONT_WINDSHIELD, 0x02
                )
                // 显示toast提示
                ToastUtilOverApplication().showToast(context, "已开启前除雾除霜")
            }else{
                carHvacManager.setIntProperty(
                    CarHvacManager.ID_WINDOW_DEFROSTER_ON,
                    VehicleAreaWindow.WINDOW_FRONT_WINDSHIELD, 0x01
                )
                // 显示toast提示
                ToastUtilOverApplication().showToast(context, "已关闭前除雾除霜")
            }

       /*     carHvacManager.setIntProperty(
                CarHvacManager.ID_WINDOW_DEFROSTER_ON,
                VehicleAreaWindow.WINDOW_FRONT_WINDSHIELD, 0x03
            )*/
            Timber.tag(TAG).d("前挡风玻璃除雾除霜设置完成 (AUTO_ON_OFF = 0x03)")
        } ?: run {
            Timber.tag(TAG).e("CarHvacManager为空，无法执行前挡风玻璃除雾除霜功能")
        }
    }

    override suspend fun toggleDefrost2() {
        Timber.tag(TAG).d("开始执行后挡风玻璃除雾除霜")
        if (!checkAndReconnectHvacManager()) {
            return
        }

        mCarHvacManager?.let { carHvacManager ->

            val currentStatus = carHvacManager.getIntProperty(
                CarHvacManager.ID_WINDOW_DEFROSTER_ON,
                VehicleAreaWindow.WINDOW_REAR_WINDSHIELD
            )

            if(currentStatus == 0x01){//当前为关闭
                carHvacManager.setIntProperty(
                    CarHvacManager.ID_WINDOW_DEFROSTER_ON,
                    VehicleAreaWindow.WINDOW_REAR_WINDSHIELD, 0x02
                )
                // 显示toast提示
                ToastUtilOverApplication().showToast(context, "已开启后除雾除霜")
            }else{
                carHvacManager.setIntProperty(
                    CarHvacManager.ID_WINDOW_DEFROSTER_ON,
                    VehicleAreaWindow.WINDOW_REAR_WINDSHIELD, 0x01
                )
                // 显示toast提示
                ToastUtilOverApplication().showToast(context, "已关闭后除雾除霜")
            }

            /*carHvacManager.setIntProperty(
                CarHvacManager.ID_WINDOW_DEFROSTER_ON,
                VehicleAreaWindow.WINDOW_REAR_WINDSHIELD, 0x03
            )*/
            Timber.tag(TAG).d("后挡风玻璃除雾除霜设置完成 (AUTO_ON_OFF = 0x03)")
        } ?: run {
            Timber.tag(TAG).e("CarHvacManager为空，无法执行后挡风玻璃除雾除霜功能")
        }
    }


    //data[0] = 0x7，data[1] = HU_MirrorFoldReq（0x0=inactive；0x1=fold；0x2=unfold；0x3=reserve）

    override suspend fun toRearViewMirrorFold(){
        Timber.tag(TAG).d("开始执行toRearViewMirrorFold")
        if (!checkAndReconnectHvacManager()) {
            return
        }

        mCarCabinManager?.let { carBinManager ->

           val mirrorStatus = carBinManager.getIntProperty(
                CarCabinManager.ID_BODY_COMMON_SET,0x7
            )

           val newMirrorStatus = if (mirrorStatus == 0x01) 0x02 else 0x01

            carBinManager.setIntProperty(
                CarCabinManager.ID_BODY_COMMON_SET,
                0x7,
                newMirrorStatus
            )

            // 显示toast提示
            val toastMessage = if (newMirrorStatus == 0x01) "后视镜折叠" else "后视镜展开"
            ToastUtilOverApplication().showToast(context, toastMessage)

            Timber.tag(TAG).d("toRearViewMirrorFold")
        } ?: run {
            Timber.tag(TAG).e("CarHvacManager为空，无法执行后toRearViewMirrorFold功能")
        }
    }

     override fun adjustAcWindDirection() {
         Timber.tag(TAG).d("开始执行空调风向调节")
         if (!checkAndReconnectHvacManager()) {
             return
         }
        Timber.tag(TAG).w("调节空调吹风方向")
        try {
            // 获取当前吹风方向
            var toastMessage = "空调风向已调节"

            mCarHvacManager?.let { carHvacManager ->
                val currentFanDirection = carHvacManager.getIntProperty(
                    CarHvacManager.ID_ZONED_FAN_DIRECTION,
                    VehicleAreaSeat.SEAT_ROW_FRONT
                )
                when (currentFanDirection) {
                    //当前吹脸，则按下吹脚
                    0x1 -> {
                        carHvacManager.setIntProperty(
                            CarHvacManager.ID_ZONED_FAN_DIRECTION,
                            VehicleAreaSeat.SEAT_ROW_FRONT, 0x9
                        )
                        toastMessage = "空调风向已切换为吹脸吹脚"
                    }
                    //当前是吹脸，吹脚，则按下除霜
                    0x3 -> {
                        carHvacManager.setIntProperty(
                            CarHvacManager.ID_ZONED_FAN_DIRECTION,
                            VehicleAreaSeat.SEAT_ROW_FRONT, 0xa
                        )
                        toastMessage = "空调风向已切换为除霜吹脸吹脚"
                    }
                    //当前是脸，脚，霜,则按下吹脚
                    0x7 -> {
                        carHvacManager.setIntProperty(
                            CarHvacManager.ID_ZONED_FAN_DIRECTION,
                            VehicleAreaSeat.SEAT_ROW_FRONT, 0x09
                        )
                        toastMessage = "空调风向已切换为除霜吹脸"
                    }
                    //当前是脸，霜，则按下脸
                    0x6 -> {
                        carHvacManager.setIntProperty(
                            CarHvacManager.ID_ZONED_FAN_DIRECTION,
                            VehicleAreaSeat.SEAT_ROW_FRONT, 0x08
                        )
                        toastMessage = "空调风向已切换为除霜"
                    }
                    //当前是霜，则按下脚
                    0x4 -> {
                        carHvacManager.setIntProperty(
                            CarHvacManager.ID_ZONED_FAN_DIRECTION, VehicleAreaSeat.SEAT_ROW_FRONT, 0x09
                        )
                        toastMessage = "空调风向已切换为除霜吹脚"
                    }
                    //当前是脚，霜，则按下霜
                    0x5 -> {
                        carHvacManager.setIntProperty(
                            CarHvacManager.ID_ZONED_FAN_DIRECTION,
                            VehicleAreaSeat.SEAT_ROW_FRONT, 0x0a
                        )
                        toastMessage = "空调风向已切换为吹脚"
                    }
                    //当前是脚，则切换为脸
                    0x2 -> {
                        carHvacManager.setIntProperty(
                            CarHvacManager.ID_ZONED_FAN_DIRECTION,
                            VehicleAreaSeat.SEAT_ROW_FRONT, 0x1
                        )
                        toastMessage = "空调风向已切换为吹脸"
                    }
                }

            // 显示toast提示
            ToastUtilOverApplication().showToast(context, toastMessage)
            Timber.tag(TAG).d("空调风向调节完成: $toastMessage")
            }

           /* val currentDirection = carPropertyUtils.getAdjustAcWindDirection()
            Timber.tag(TAG).w("当前吹风方向: $currentDirection")
            // 循环切换吹风方向: 吹脸→吹脚→除霜→吹脸
            val newDirection = when (currentDirection) {
                1 -> 2
                2 -> 3
                3 -> 10
                4 -> 8
                5 -> 10
                6 -> 9
                7 -> 8
                else -> 1 // 其他→吹脸
            }

            // 设置新的吹风方向
            carPropertyUtils.setAdjustAcWindDirection(newDirection)

            Timber.tag(TAG).w("next吹风方向: $newDirection")

            // 输出设置后的吹风方向
            val directionDesc = when (newDirection) {
                1 -> "吹脚"
                2 -> "吹脚"
                3 -> "吹面+吹脚"
                4 -> "吹面+除霜"
                5 -> "吹面"
                6 -> "除霜"
                7 -> "吹脚+除霜"
                8 -> "吹面"
                9 -> "除霜"
                10 -> "吹面"
                else -> "吹脚"
            }

            // 显示toast提示
            ToastUtilOverApplication().showToast(context, "空调吹风已设置为$directionDesc")*/

            return
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "调节空调吹风方向失败")
            return
        }
    }

    private fun adjustAcWindDirection2() {
        /**
         * 常量：VehicleHvacFanDirection:
         *     /**吹脸**/
         *     FACE = 0x1,  y
         *     /**吹脚**/
         *     FOOT = 0x2, y
         *     /**吹脸/吹脚**/
         *     FACE_FOOT = 0x3, y
         *     /**除霜**/
         *     DEFROST = 0x4, y
         *     /**吹脚/除霜(后排无此功能)**/
         *     FOOT_DEFROST = 0x5, y
         *     /**吹脸/除霜**/
         *     blowface/defroster = 0x06,  not
         *    /**吹脸/吹脚/除霜**/
         *    blowface/blowfeet/defroster = 0x07, not
         *     /**点击吹脸区域**/
         *     FACE_PRESS = 0x8,
         *     /**点击吹脚区域**/
         *     FOOT_PRESS = 0x9,
         *     /**点击除霜区域**/
         *     DEFROST_PRESS = 0xa,
         *     /**无效，置灰**/（857项目）
         *     INVALID = 0x7;
         */
        Timber.tag(TAG).d("开始执行空调风向调节")
        if (!checkAndReconnectHvacManager()) {
            return
        }

        mCarHvacManager?.let { carHvacManager ->
            val currentWindowDirection = carHvacManager.getIntProperty(
                CarHvacManager.ID_ZONED_FAN_DIRECTION,
                VehicleAreaSeat.SEAT_ROW_FRONT
            )
            Timber.tag(TAG).d("当前空调风向: $currentWindowDirection")

            val arrays = arrayOf(0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7)
            val next = arrays[(currentWindowDirection + 1) % arrays.size]
            Timber.tag(TAG).d("设置空调风向为: $next")

            carHvacManager.setIntProperty(
                CarHvacManager.ID_ZONED_FAN_DIRECTION,
                VehicleAreaSeat.SEAT_ROW_FRONT, next
            )
            Timber.tag(TAG).d("空调风向调节完成")
        } ?: run {
            Timber.tag(TAG).e("CarHvacManager为空，无法执行空调风向调节功能")
        }
    }

    override suspend fun adjustDrivingMode(){
        Timber.tag(TAG).d("开始执行adjustDrivingMode切换")
        mCarCabinManager?.let { carCabinManager ->
            // data[0]=0x01,data[1]=HU_DrvMod2（0x0=Inactive；
            // 0x1=ECO；
            // 0x2=normal；
            // 0x3=sport；
            // 0x4=自定义 1；
            // 0x5=专属模式 2；0x6=自定义 3；0x7=Reserved）

           val currentMode = carCabinManager.getIntProperty(
                CarCabinManager.ID_PHEV_DRV_MODE_SET,
                0x01
            )
            val nextMode = when (currentMode) {
                0x1 -> 0x2
                0x2 -> 0x3
                0x3 -> 0x5
                0x5-> 0x4
                0x4 -> 0x1
                else -> 0x1
            }
            carCabinManager.setIntProperty(
                CarCabinManager.ID_PHEV_DRV_MODE_SET,
                0x01, nextMode
            )

            // 显示toast提示
            val toastMessage = when (nextMode) {
                0x1 -> "驾驶模式已设置为经济"
                0x2 -> "驾驶模式已设置为舒适"
                0x3 -> "驾驶模式已设置为运动"
                0x4 -> "驾驶模式已设置为自定义"
                0x5 -> "驾驶模式已设置为专属"
                else -> "驾驶模式已设置为舒适"
            }
            ToastUtilOverApplication().showToast(context, toastMessage)
        }?:let {
            Timber.tag(TAG).e("mCarCabinManager为空，无法执行adjustDrivingMode")
        }
    }

    override suspend fun adjustTailWingPosition(){
        Timber.tag(TAG).d("开始执行adjustTailWingPosition切换")
        mCarCabinManager?.let { carCabinManager ->
            val currentTailStatus = carCabinManager.getIntProperty(
                CarCabinManager.ID_BODY_COMMON_SET,0x010
            )
            //date[0] = 0x10，date[1] = THU_SpoilerCtrlReq (0x0=Inactive；0x1=Open；0x2=Open2；0x3=Auto；0x4=Close；0x5=Custom；0x6=Open3)
            //4 Close
            //1 Open
            //2 Open2
            //3 Auto
            //5 Custom
            val nextStatus = when (currentTailStatus) {
                1 -> 2
                2 -> 3
                3 -> 5
                5 -> 4
                4 -> 1
                else -> 1
            }

            carCabinManager.setIntProperty(
                CarCabinManager.ID_BODY_COMMON_SET,
                0x010, nextStatus
            )

            // 显示toast提示
            val toastMessage = when (nextStatus) {
                1 -> "尾翼档位设置为低风阻"
                2 -> "尾翼档位设置为高下压"
                3 -> "尾翼档位设置为自动"
                5 -> "尾翼档位设置为自定义"
                4 -> "尾翼档位已关闭"
                else -> ""
            }
            if (!android.text.TextUtils.isEmpty(toastMessage)) {
                ToastUtilOverApplication().showToast(context, toastMessage)
            }

            Timber.tag(TAG).d("next tail is $nextStatus")
        }
    }

    override suspend fun toggleTrunk(){
        Timber.tag(TAG).d("开始执行toggleTrunk切换")
        mCarCabinManager?.let { carCabinManager ->
            val trunkStatus = carCabinManager.getIntProperty(
                CarCabinManager.ID_BODY_DOOR_TRUNK_DOOR_POS,
                0x00
            )
            if (trunkStatus == 0) {//当前为关闭
                carCabinManager.setIntProperty(
                    CarCabinManager.ID_BODY_DOOR_TRUNK_DOOR_POS,
                    0x00,
                    0x02
                )
                // 显示toast提示
               // ToastUtilOverApplication().showToast(context, "行李箱门打开")
            }else{
                carCabinManager.setIntProperty(
                    CarCabinManager.ID_BODY_DOOR_TRUNK_DOOR_POS,
                    0x00,
                    0x01
                )
                // 显示toast提示
                ToastUtilOverApplication().showToast(context, "行李箱门关闭")
            }
        }?:let {
            Timber.tag(TAG).e("mCarCabinManager为空，无法执行toggleTrunk")
        }
    }

    override suspend fun toggleMainSeatHeat() {
        Timber.tag(TAG).d("开始执行主驾座椅加热切换")
        mCarHvacManager?.let { carHvacManager ->
            val currentLevel = carHvacManager.getIntProperty(
                CarHvacManager.ID_ZONED_SEAT_TEMP,
                VehicleAreaSeat.SEAT_MAIN_DRIVER
            )
            Timber.tag(TAG).d("主驾座椅加热当前等级: $currentLevel")
            // 循环切换档位 (0→1→2→3→0)
            val newLevel = (currentLevel + 1) % 4
            Timber.tag(TAG).d("主驾座椅加热设置新等级: $newLevel")

            carHvacManager.setIntProperty(
                CarHvacManager.ID_ZONED_SEAT_TEMP,
                VehicleAreaSeat.SEAT_MAIN_DRIVER,
                newLevel
            )

            // 显示toast提示
            val toastMessage = when (newLevel) {
                0 -> "主驾座椅加热已关闭"
                1 -> "主驾座椅加热已启用并设置为低档"
                2 -> "主驾座椅加热已启用并设置为中档"
                3 -> "主驾座椅加热已启用并设置为高档"
                else -> "主驾座椅加热已关闭"
            }
            ToastUtilOverApplication().showToast(context, toastMessage)

            Timber.tag(TAG).d("主驾座椅加热设置完成")
        } ?: run {
            Timber.tag(TAG).e("CarHvacManager为空，无法执行主驾座椅加热功能")
        }
    }

    override suspend fun toggleCopilotSeatHeat() {
        Timber.tag(TAG).d("开始执行副驾座椅加热切换")
        mCarHvacManager?.let { carHvacManager ->
            val currentLevel = carHvacManager.getIntProperty(
                CarHvacManager.ID_ZONED_SEAT_TEMP,
                VehicleAreaSeat.SEAT_PASSENGER
            )
            Timber.tag(TAG).d("副驾座椅加热当前等级: $currentLevel")
            // 循环切换档位 (0→1→2→3→0)
            val newLevel = (currentLevel + 1) % 4
            Timber.tag(TAG).d("副驾座椅加热设置新等级: $newLevel")

            carHvacManager.setIntProperty(
                CarHvacManager.ID_ZONED_SEAT_TEMP,
                VehicleAreaSeat.SEAT_PASSENGER,
                newLevel
            )

            // 显示toast提示
            val toastMessage = when (newLevel) {
                0 -> "副驾座椅加热已关闭"
                1 -> "副驾座椅加热已启用并设置为低档"
                2 -> "副驾座椅加热已启用并设置为中档"
                3 -> "副驾座椅加热已启用并设置为高档"
                else -> "副驾座椅加热已关闭"
            }
            ToastUtilOverApplication().showToast(context, toastMessage)

            Timber.tag(TAG).d("副驾座椅加热设置完成")
        } ?: run {
            Timber.tag(TAG).e("CarHvacManager为空，无法执行副驾座椅加热功能")
        }
    }

    override suspend fun toggleSteeringWheelHeat() {

    }

    /**
     * 实际执行风量减少操作的内部方法
     */
    private suspend fun executeDecreaseFanSpeed() {
        Timber.tag(TAG).d("开始执行风量减少")
        if (!checkAndReconnectHvacManager()) {
            return
        }
        mCarHvacManager?.let { carHvacManager ->
            val fanLevel = carHvacManager.getIntProperty(
                CarHvacManager.ID_HVAC_FAN_SPEED_ACK,
                VehicleAreaSeat.SEAT_ROW_FRONT
            )

            //当前空调关闭，fanLevel固定获取为0
            //所以无法获取到关闭时的确切挡位
           /* if(carPropertyUtils.airCdFront==0x1 && fanLevel==0){

            }*/

            // 边界检查：如果风量小于1，直接返回
            if (fanLevel < 1) {
                Timber.tag(TAG).d("风量已是最小值，无法再减少")
                return
            }

            // 特殊业务逻辑：如果风量为1，关闭空调
            if (fanLevel == 1) {
                Timber.tag(TAG).d("风量为1直接关闭空调")
                carPropertyUtils.setAirCdFront(0x01)
                //ToastUtilOverApplication().showToast(context, "空调已关闭")
                return
            }

            carHvacManager.setIntProperty(
                CarHvacManager.ID_HVAC_FAN_SPEED_ADJUST,
                VehicleAreaSeat.SEAT_ROW_FRONT,
                0xAA
            )
            Timber.tag(TAG).d("风量减少设置完成 (0xAA)")




            coroutineScope.launch {
                delay(1000)

                val nextFanLevel = carHvacManager.getIntProperty(
                    CarHvacManager.ID_HVAC_FAN_SPEED_ACK,
                    VehicleAreaSeat.SEAT_ROW_FRONT
                )
                // 显示toast提示
                ToastUtilOverApplication().showToast(context, "空调风量已调低到${nextFanLevel}档")

                Timber.tag(TAG).d("current fan level is $fanLevel")
            }

        } ?: run {
            Timber.tag(TAG).e("CarHvacManager为空，无法执行风量减少功能")
        }
    }

    /**
     * 实际执行风量增加操作的内部方法
     */
    private suspend fun executeIncreaseFanSpeed() {
        Timber.tag(TAG).d("开始执行风量增加")
        if (!checkAndReconnectHvacManager()) {
            return
        }
        mCarHvacManager?.let { carHvacManager ->

            val fanLevel = carHvacManager.getIntProperty(
                CarHvacManager.ID_HVAC_FAN_SPEED_ACK,
                VehicleAreaSeat.SEAT_ROW_FRONT
            )

            // 边界检查：如果已经是最大风量，直接返回
            if (fanLevel >= 8) {
                Timber.tag(TAG).d("风量已是最大值，无法再增加")
                return
            }

            carHvacManager.setIntProperty(
                CarHvacManager.ID_HVAC_FAN_SPEED_ADJUST,
                VehicleAreaSeat.SEAT_ROW_FRONT, 0xBB
            )
            Timber.tag(TAG).d("风量增加设置完成 (0xBB)")

            coroutineScope.launch {
                delay(1000)
                val nextFanLevel = carHvacManager.getIntProperty(
                    CarHvacManager.ID_HVAC_FAN_SPEED_ACK,
                    VehicleAreaSeat.SEAT_ROW_FRONT
                )
                // 显示toast提示
                ToastUtilOverApplication().showToast(context, "空调风量已调高到${nextFanLevel}档")
                Timber.tag(TAG).d("current fan level is $fanLevel")
            }

        } ?: run {
            Timber.tag(TAG).e("CarHvacManager为空，无法执行风量增加功能")
        }
    }

    override suspend fun decreaseFanSpeed() {
        Timber.tag(TAG).d("收到decreaseFanSpeed请求")
        fanSpeedHandler.emit(FanSpeedAction.DECREASE)
    }

    override suspend fun increaseFanSpeed() {
        Timber.tag(TAG).d("收到increaseFanSpeed请求")
        fanSpeedHandler.emit(FanSpeedAction.INCREASE)
    }

    /**
     * 实际执行空调温度降低操作的内部方法
     */
    private suspend fun executeDecreaseACTemperature() {
        Timber.tag(TAG).d("开始执行空调温度降低")
        if (!checkAndReconnectHvacManager()) {
            return
        }
        mCarHvacManager?.let { carHvacManager ->
            carHvacManager.setIntProperty(
                CarHvacManager.ID_HVAC_TEMPERATURE_ADJUST,
                VehicleAreaSeat.SEAT_MAIN_DRIVER,
                0x0
            )
            Timber.tag(TAG).d("空调温度降低设置完成 (0x0)")

            coroutineScope.launch {
                delay(1000)
                val nextTemp = carHvacManager.getFloatProperty(
                    CarHvacManager.ID_ZONED_TEMP_SETPOINT,
                    VehicleAreaSeat.SEAT_MAIN_DRIVER,
                )
                // 显示toast提示
                ToastUtilOverApplication().showToast(
                    context, if (nextTemp <= 17.5) {
                        "空调温度已调到最低"
                    } else {
                        "空调温度已调低到${nextTemp}度"
                    }
                )
                Timber.tag(TAG).d("current tempurature is $nextTemp")
            }
        } ?: run {
            Timber.tag(TAG).e("CarHvacManager为空，无法执行空调温度降低功能")
        }
    }

    /**
     * 实际执行空调温度升高操作的内部方法
     */
    private suspend fun executeIncreaseACTemperature() {
        Timber.tag(TAG).d("开始执行空调温度升高")
        if (!checkAndReconnectHvacManager()) {
            return
        }
        mCarHvacManager?.let { carHvacManager ->
            carHvacManager.setIntProperty(
                CarHvacManager.ID_HVAC_TEMPERATURE_ADJUST,
                VehicleAreaSeat.SEAT_MAIN_DRIVER,
                0x1
            )
            Timber.tag(TAG).d("空调温度升高设置完成 (0x1)")

            coroutineScope.launch {
                delay(1000)
                val nextTemp = carHvacManager.getFloatProperty(
                    CarHvacManager.ID_ZONED_TEMP_SETPOINT,
                    VehicleAreaSeat.SEAT_MAIN_DRIVER,
                )

                // 显示toast提示
                ToastUtilOverApplication().showToast(
                    context, if (nextTemp>=32.5) {
                        "空调温度已调到最高"
                    } else {
                        "空调温度已调高到${nextTemp}度"
                    }
                )

                Timber.tag(TAG).d("current tempurature is $nextTemp")
            }



        } ?: run {
            Timber.tag(TAG).e("CarHvacManager为空，无法执行空调温度升高功能")
        }
    }

    override suspend fun decreaseACTemperature() {
        Timber.tag(TAG).d("收到decreaseACTemperature请求")
        temperatureHandler.emit(TemperatureAction.DECREASE)
    }

    override  fun increaseACTemperature2() {
    }

    override fun decreaseACTemperature2() {
    }

    override suspend  fun increaseACTemperature() {
        Timber.tag(TAG).d("收到increaseACTemperature请求")
        temperatureHandler.emit(TemperatureAction.INCREASE)
    }

    // 实现MeiJiaCarFunctionExecutor中特有的方法

    /**
     * 实现儿童锁控制 - 左侧
     */
    override suspend fun toggleChildLock1() {
        Timber.tag(TAG).d("TinnoveCarFunctionExecutor不支持toggleChildLock1功能")
        ToastUtilOverApplication().showToast(context, "当前车型不支持此功能")
    }

    /**
     * 实现儿童锁控制 - 右侧
     */
    override suspend fun toggleChildLock2() {
        Timber.tag(TAG).d("TinnoveCarFunctionExecutor不支持toggleChildLock2功能")
        ToastUtilOverApplication().showToast(context, "当前车型不支持此功能")
    }

    /**
     * 实现小憩模式设置
     */
    override suspend fun setCdcRespiteSetSts() {
        Timber.tag(TAG).d("TinnoveCarFunctionExecutor不支持setCdcRespiteSetSts功能")
        ToastUtilOverApplication().showToast(context, "当前车型不支持此功能")
    }

    override suspend fun toSeatBeltCheck() {

    }

    override suspend fun toWiper() {

    }


    /**
     * 实现按摩强度调节
     */
    override suspend fun adjustMassageIntensity() {
        Timber.tag(TAG).d("TinnoveCarFunctionExecutor不支持adjustMassageIntensity功能")
        ToastUtilOverApplication().showToast(context, "当前车型不支持此功能")
    }

    /**
     * 实现按摩模式调节
     */
    override suspend fun adjustMassageMode() {
        Timber.tag(TAG).d("TinnoveCarFunctionExecutor不支持adjustMassageMode功能")
        ToastUtilOverApplication().showToast(context, "当前车型不支持此功能")
    }

    override suspend fun toggleDefrost() {
    }

    override fun adjustEnergyManagement() {
    }

    override fun adjustCopilotScreen() {

    }

    override fun getAmbientLightColor(): Int {
        Timber.tag(TAG).d("开始getAmbientLightColor")
        if (!checkAndReconnectCabinManager()) {
            return -1
        }
        try {
            val value = mCarCabinManager!!.getIntProperty(
                CarCabinManager.ID_HU_PRIATMOS_PHERE_LIGHTCOR_SET,
                VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL
            )
            return value
        }catch (e: Exception){
            e.printStackTrace()
            Timber.tag(TAG).e(e,"getAmbientLightColor")
            return  -1
        }

    }

    override fun getAtmosphereLightBrightness(): Int {
        Timber.tag(TAG).d("getAtmosphereLightBrightness")
        if (!checkAndReconnectCabinManager()) {
            return -1
        }
        try {
            val value =
                mCarCabinManager!!.getIntProperty(CarCabinManager.ID_PRI_SUB_AUX_LIGHT_BRIGHT, 0)
            return value
        }catch (e: Exception){
            e.printStackTrace()
            Timber.tag(TAG).e(e,"getAmbientLightColor")
            return  -1
        }
    }

    override fun getAtmosphereLightSwtich(): Int {
    return -1
    }

    override fun isIgnition(): Boolean {
        try {
            val ignitionEvent = mCarSensorManager?.getLatestSensorEvent(CarSensorManager.SENSOR_TYPE_IGNITION_STATE)
            if (ignitionEvent != null) {
                val ignitionValue = ignitionEvent.intValues[0]
                Log.i(TAG,"current ignitionValue $ignitionValue")
                return ignitionValue ==4
            }else{
                Log.i(TAG,"get ignition fail")
                return true
            }
        }catch (e: Exception){
            Log.e(TAG,"isIgnition error",e)
            return true
        }
    }

    override fun listenIsIgnition(callBack: (Boolean) -> Unit) {
        mCarSensorManager?.registerListener(
            { carSensorEvent ->
                try {
                    val prop = carSensorEvent.sensorType
                    if (prop == CarSensorManager.SENSOR_TYPE_IGNITION_STATE) {
                        val ignition = carSensorEvent.intValues[0]
                        Log.i(TAG,"listenIsIgnition $ignition")
                        if (ignition == 4) {
                            callBack(true)
                        }
                    }
                }catch (e: Exception){
                    Log.e(TAG,"listenIsIgnition",e)
                }
            },
            CarSensorManager.SENSOR_TYPE_IGNITION_STATE,
            CarSensorManager.SENSOR_RATE_NORMAL
        )
    }
}