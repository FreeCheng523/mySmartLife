package com.zkjd.lingdong.ui.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.zkjd.lingdong.databinding.ActivityCarFunctionTestBinding
import com.zkjd.lingdong.model.ButtonFunction
import com.zkjd.lingdong.model.ButtonType
import com.zkjd.lingdong.model.FunctionCategory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 美佳车型功能测试Activity
 * 测试MeiJiaCarFunctionExecutor中所有的CAN信号功能
 */
@AndroidEntryPoint
class MeijiaCarFunctionTestActivity : ComponentActivity() {


    private val activityScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var binding: ActivityCarFunctionTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarFunctionTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    /**
     * 触发车辆功能
     * @param code 功能代码
     * @param buttonType 按键类型
     */
    private fun trigger(code: String, buttonType: ButtonType = ButtonType.SHORT_PRESS) {
        binding.tvResult.text = "执行中..."
        activityScope.launch {
            try {
                val function = ButtonFunction(
                    category = FunctionCategory.CAR,
                    name = code,
                    actionCode = code,
                    useType = when (buttonType) {
                        ButtonType.LEFT_ROTATE, ButtonType.RIGHT_ROTATE -> 2
                        else -> 1
                    },
                    configWords = ""
                )
                //carExecutor.executeCarFunction(function, buttonType, "TEST:MAC")
                
                val typeDesc = when (buttonType) {
                    ButtonType.SHORT_PRESS -> ""
                    ButtonType.LEFT_ROTATE -> " (LEFT_ROTATE)"
                    ButtonType.RIGHT_ROTATE -> " (RIGHT_ROTATE)"
                    else -> " (${buttonType.name})"
                }
                binding.tvResult.text = "已触发：$code$typeDesc"
            } catch (e: Exception) {
                binding.tvResult.text = "执行失败：${e.message}"
            }
        }
    }

    /**
     * 设置所有按钮的点击监听器
     */
    private fun setupClickListeners() {
        // 基础功能
        binding.btnTrunk.setOnClickListener { trigger("CAR_TRUNK_TOGGLE") }
        
        // 儿童锁功能
        binding.btnChildLockLeft.setOnClickListener { trigger("CAR_CHILD_LOCK_TOGGLE1") }
        binding.btnChildLockRight.setOnClickListener { trigger("CAR_CHILD_LOCK_TOGGLE2") }
        
        // 座椅功能
        binding.btnSteeringHeat.setOnClickListener { trigger("CAR_STEERING_WHEEL_HEAT_TOGGLE") }
        binding.btnMainSeatHeat.setOnClickListener { trigger("CAR_MAIN_SEAT_HEAT_TOGGLE") }
        binding.btnCopilotSeatHeat.setOnClickListener { trigger("CAR_COPILOT_SEAT_HEAT_TOGGLE") }
        binding.btnLeftVentToggle.setOnClickListener { trigger("CAR_LEFT_REAR_VENT_TOGGLE") }
        binding.btnRightVentToggle.setOnClickListener { trigger("CAR_RIGHT_REAR_VENT_TOGGLE") }
        
        // 按摩功能
        binding.btnMassageIntensity.setOnClickListener { trigger("CAR_MASSAGE_INTENSITY") }
        binding.btnMassageMode.setOnClickListener { trigger("CAR_MASSAGE_MODE") }
        
        // 空调功能
        binding.btnAirConditioning.setOnClickListener { trigger("CAR_AIR_CONDITIONING") }
        binding.btnTempUp.setOnClickListener { trigger("CAR_AC_TEMPERATURE_UP") }
        binding.btnTempDown.setOnClickListener { trigger("CAR_AC_TEMPERATURE_DOWN") }
        binding.btnFanUp.setOnClickListener { trigger("CAR_FAN_SPEED_UP") }
        binding.btnFanDown.setOnClickListener { trigger("CAR_FAN_SPEED_DOWN") }
        binding.btnWindDirection.setOnClickListener { trigger("CAR_AC_WIND_DIRECTION") }
        
        // 除霜功能
        binding.btnDefrostToggle.setOnClickListener { trigger("CAR_DEFROST_TOGGLE") }
        binding.btnDefrost1.setOnClickListener { trigger("CAR_DEFROST1") }
        binding.btnDefrost2.setOnClickListener { trigger("CAR_DEFROST2") }
        
        // 驾驶功能
        binding.btnDrivingMode.setOnClickListener { trigger("CAR_DRIVING_MODE") }
        binding.btnEnergyManage.setOnClickListener { trigger("CAR_ENERGY_MANAGEMENT") }
        
        // 车身功能
        binding.btnTailWingPos.setOnClickListener { trigger("CAR_TRUNK_POSITION") }
        binding.btnMirrorFold.setOnClickListener { trigger("CAR_FOLDING_REARVIEW_MIRROR") }
        binding.btnWiperMode.setOnClickListener { trigger("CAR_WIPER_MODE") }
        
        // 屏幕功能
        binding.btnCopilotScreen.setOnClickListener { trigger("CAR_COPILOT_SCREEN_TOGGLE") }
        
        // 小憩模式
        binding.btnRestLeft.setOnClickListener { trigger("CAR_RESTING_MODE_LEFT") }
        binding.btnRestRight.setOnClickListener { trigger("CAR_RESTING_MODE_RIGHT") }
        
        // 安全功能
        binding.btnPedestrianAlert.setOnClickListener { trigger("CAR_LOW_SPEED_PEDESTRIAN") }
        binding.btnSeatBeltRear.setOnClickListener { trigger("CAR_SEAT_BELT_NOTFASTENED") }
        
        // 老板键功能
        binding.btnBossKeyLeft.setOnClickListener { trigger("CAR_BOSS_KEY", ButtonType.LEFT_ROTATE) }
        binding.btnBossKeyRight.setOnClickListener { trigger("CAR_BOSS_KEY", ButtonType.RIGHT_ROTATE) }
        binding.btnBossKeyPress.setOnClickListener { trigger("CAR_BOSS_KEY", ButtonType.SHORT_PRESS) }
        
        // 旋转调节功能
        binding.btnTemp1Left.setOnClickListener { trigger("CAR_AC_TEMPERATURE1", ButtonType.LEFT_ROTATE) }
        binding.btnTemp1Right.setOnClickListener { trigger("CAR_AC_TEMPERATURE1", ButtonType.RIGHT_ROTATE) }
        binding.btnTemp2Left.setOnClickListener { trigger("CAR_AC_TEMPERATURE2", ButtonType.LEFT_ROTATE) }
        binding.btnTemp2Right.setOnClickListener { trigger("CAR_AC_TEMPERATURE2", ButtonType.RIGHT_ROTATE) }
        binding.btnFanSpeedLeft.setOnClickListener { trigger("CAR_AC_FAN_SPEED", ButtonType.LEFT_ROTATE) }
        binding.btnFanSpeedRight.setOnClickListener { trigger("CAR_AC_FAN_SPEED", ButtonType.RIGHT_ROTATE) }
    }
}


