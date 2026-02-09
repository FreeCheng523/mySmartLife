package com.zkjd.lingdong.ui.led

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zkjd.lingdong.R
import com.zkjd.lingdong.model.ButtonType
import com.zkjd.lingdong.model.LedColor
import com.zkjd.lingdong.repository.DeviceEvent
import com.zkjd.lingdong.repository.DeviceRepository
import com.zkjd.lingdong.repository.SettingsRepository
import com.zkjd.lingdong.service.SoundManager
import com.zkjd.lingdong.ui.function.FunctionViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * LED灯颜色设置ViewModel
 */
@HiltViewModel
class LedColorViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    fun getContext(): Context = context
    // 工作模式颜色
    private val _connectedLedColor = MutableStateFlow<LedColor>(LedColor.GREEN)
    val connectedLedColor: StateFlow<LedColor> = _connectedLedColor.asStateFlow()
    
    // 回连模式颜色
    private val _reconnectingLedColor = MutableStateFlow<LedColor>(LedColor.RED)
    val reconnectingLedColor: StateFlow<LedColor> = _reconnectingLedColor.asStateFlow()
    
    // 当前选中的模式 (true = 工作模式, false = 回连模式)
    private val _isConnectedMode = MutableStateFlow(true)
    val isConnectedMode: StateFlow<Boolean> = _isConnectedMode.asStateFlow()
    
    // 当前活跃的颜色（根据选中的模式决定）
    val currentActiveColor: StateFlow<LedColor> get() = 
        if (_isConnectedMode.value) connectedLedColor else reconnectingLedColor

    // 是否为旋钮
    private var _isRoty = MutableStateFlow(false)
    val isRoty: StateFlow<Boolean> = _isRoty.asStateFlow()

    // 设备防误触
    private var _isPreventAccidental : MutableStateFlow<Boolean> =MutableStateFlow(false)
    val isPreventAccidental : StateFlow<Boolean> = _isPreventAccidental.asStateFlow()

    // 是否有音效
    private var _isCanMusic = MutableStateFlow(true)
    val isCanMusic: StateFlow<Boolean> = _isCanMusic.asStateFlow()

    // 音效ID
    private var _useMusicTypeName = MutableStateFlow("1")
    val useMusicTypeName: StateFlow<String> = _useMusicTypeName.asStateFlow()

    // 预设颜色列表
    val presetColors = listOf(
        LedColor.RED,
        LedColor.ORANGE,
        LedColor.YELLOW,
        LedColor.GREEN,
        LedColor.BLUE,

        LedColor.MAGENTA,
        LedColor.CYAN,
        LedColor.BLUE2,

    )
    
    // UI事件
    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents
    
    // 当前设备MAC地址
    private var currentDeviceMac: String? = null

    private lateinit var soundManager: SoundManager
    
    /**
     * 加载设备颜色
     */
    fun loadDeviceColor(deviceMac: String) {
        soundManager = SoundManager(context)

        soundManager.loadSound(R.raw.click_sound)
        soundManager.loadSound(R.raw.dong)
        soundManager.loadSound(R.raw.dack)

        // 音效选择
        val useMusicTypeName: StateFlow<String> = settingsRepository.useMusicTypeName
            .onEach { value ->
                when (value) {
                    "1" -> {
                        soundManager.playSound(R.raw.click_sound)
                    }

                    "2" -> {
                        soundManager.playSound(R.raw.dong)
                    }

                    "3" -> {
                        soundManager.playSound(R.raw.dack)
                    }
                } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, "0")

        currentDeviceMac = deviceMac
        viewModelScope.launch {
            try {
                val device = withContext(Dispatchers.IO) {
                    deviceRepository.getDeviceByMacAddress(deviceMac)
                }
                if (device != null) {
                    // 加载设备的两种颜色
                    val connectedColorInt = device.connectedLedColor
                    val reconnectingColorInt = device.reconnectingLedColor

                    if(device!!.bleName.contains("DeepalTag_L0"))
                    {
                        _isRoty.value=true
                    }else{
                        _isRoty.value=true
                    }
                    _useMusicTypeName.value=device.musicName
                    _isCanMusic.value=if(device.musicCan==0)false else true

                    _isPreventAccidental.value=if(device.preventAccidental==0)false else true

                    // 找到最接近的预设颜色或创建新的LedColor对象
                    val connectedColor = findClosestPresetColor(connectedColorInt)
                    val reconnectingColor = findClosestPresetColor(reconnectingColorInt)

                    _connectedLedColor.value = connectedColor
                    _reconnectingLedColor.value = reconnectingColor
                }
            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowError("获取LED颜色失败: ${e.message}"))
            }
        }

//        viewModelScope.launch {
//            deviceRepository.getDeviceEvents().collect { event ->
//                when (event) {
//
//                    is DeviceEvent.ButtonPressed -> {
//                        // 显示按键功能名称或按键类型
//                        when(event.buttonType)
//                        {
//                            ButtonType.FONE_PESS -> _uiEvents.emit(UiEvent.ShowError("进入返控模式"))
//                            ButtonType.FTWO_PESS -> _uiEvents.emit(UiEvent.ShowError("退出返控模式"))
//                            else -> {}
//                        }
//
//                    }
//                    is DeviceEvent.BatteryLevelChanged -> {
//                        // 电量变化事件不需要UI反馈
//                        if(event.level<=10)
//                            _uiEvents.emit(UiEvent.ShowError("妥妥贴电池电量已过低，请及时更换电池！"))
//                    }
//                    else -> {}
//                }
//            }
//        }
    }
    
    /**
     * 设置当前模式
     */
    fun setColorMode(isConnected: Boolean) {
        _isConnectedMode.value = isConnected
    }


    /**
     * 设置防误触保存和下发指令
     */
    fun setPreventAccidental(deviceMac: String) {
        viewModelScope.launch {
            try {
                val value=when(_isPreventAccidental.value){
                    false ->1
                    true  ->0
                }

                withContext(Dispatchers.IO) {
                    val device = deviceRepository.getDeviceByMacAddress(deviceMac)
                    if (device != null) {
                        // 更新设备的两种颜色
                        if(device.returnControl==1) {
                            device.preventAccidental = value
                            deviceRepository.setPreventAccidental(deviceMac, value)
                            deviceRepository.updateDevice(device)
                            //添加判断是否在返控模式下
                            _isPreventAccidental.value = if (value == 0) false else true


                            //添加写入
                            deviceRepository.setPreventAccid(deviceMac, value)

                            val message=if(value==0) "防误触关闭" else "防误触开启"
                            _uiEvents.emit(UiEvent.ShowError(message))
                        }else{
                            _uiEvents.emit(UiEvent.ShowError("返控模式未开启，无法进行操作"))
                        }
                    }


                }

            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowError("防误触设置失败"))

            }
        }
    }

    /**
     * 查找最接近的预设颜色
     */
    private fun findClosestPresetColor(colorInt: Int): LedColor {
        // 简单实现：如果颜色与预设颜色完全匹配，则返回该预设颜色
        presetColors.forEach { presetColor ->
            if (presetColor.color.toArgb() == colorInt) {
                return presetColor
            }
        }
        
        // 如果没有匹配的预设颜色，则创建一个新的LedColor对象
        return LedColor(Color(colorInt), "自定义")
    }
    
    /**
     * 选择预设颜色
     */
    fun selectPresetColor(color: LedColor) {
        if (_isConnectedMode.value) {
            _connectedLedColor.value = color
        } else {
            _reconnectingLedColor.value = color
        }
    }
    
    /**
     * 选择自定义颜色（字符串输入）
     */
    fun selectCustomColor(hexColor: String) {
        try {
            val color = LedColor.fromHex(hexColor)
            if (_isConnectedMode.value) {
                _connectedLedColor.value = color
            } else {
                _reconnectingLedColor.value = color
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                _uiEvents.emit(UiEvent.ShowError("无效的颜色格式"))
            }
        }
    }

    /**
     * 切换设备鉴权开关
     */
    fun toggleMusic(mac: String) {
        viewModelScope.launch {
            _isCanMusic.value=!_isCanMusic.value
            deviceRepository.setMusicCan(mac,_isCanMusic.value)
            _uiEvents.emit(UiEvent.ShowError("音效已${if (_isCanMusic.value) "开启" else "关闭"}"))
        }
    }

    /**
     * 保存选择的车辆类型名称
     */
    fun toSaveChoseCar(id: String,mac: String) {
        viewModelScope.launch {
            _useMusicTypeName.value=id
            deviceRepository.renameMusicID(mac,id)
            when (id) {
                "1" -> {
                    soundManager.playSound(R.raw.click_sound)
                }

                "2" -> {
                    soundManager.playSound(R.raw.dong)
                }

                "3" -> {
                    soundManager.playSound(R.raw.dack)
                }
            }
        }
    }
    
    /**
     * 选择自定义颜色（Color对象）
     */
    fun selectCustomColor(color: Color, name: String = "自定义") {
        val ledColor = LedColor(color, name)
        if (_isConnectedMode.value) {
            _connectedLedColor.value = ledColor
        } else {
            _reconnectingLedColor.value = ledColor
        }
    }
    
    /**
     * 应用颜色设置到设备
     */
    fun applyColorSetting() {
        val deviceMac = currentDeviceMac ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val device = deviceRepository.getDeviceByMacAddress(deviceMac)
                    if (device != null) {
                        // 更新设备的两种颜色
                        if(device.returnControl==1) {
                            device.connectedLedColor = _connectedLedColor.value.color.toArgb()
                            device.reconnectingLedColor = _reconnectingLedColor.value.color.toArgb()

                            deviceRepository.setLedColor(deviceMac, true, device.connectedLedColor)
                            deviceRepository.setLedColor(
                                deviceMac,
                                false,
                                device.reconnectingLedColor
                            )
                            deviceRepository.updateDevice(device)

                            //发送指令
                            deviceRepository.setColor(deviceMac, true, device.connectedLedColor)

                            _uiEvents.emit(UiEvent.ColorApplied)
                        }else{
                            _uiEvents.emit(UiEvent.ShowError("返控模式未开启，无法进行操作"))
                        }
                    }
                }

            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowError("保存颜色设置失败: ${e.message}"))
            }
        }
    }
    
    /**
     * UI事件
     */
    sealed class UiEvent {
        data class ShowError(val message: String) : UiEvent()
        object ColorApplied : UiEvent()
    }
} 