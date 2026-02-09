package com.zkjd.lingdong.ui.function

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zkjd.lingdong.model.ButtonFunction
import com.zkjd.lingdong.model.ButtonType
import com.zkjd.lingdong.model.Device
import com.zkjd.lingdong.repository.DeviceRepository
import com.zkjd.lingdong.event.ButtonFunctionEvent
import com.zkjd.lingdong.repository.DeviceEvent
import com.zkjd.lingdong.repository.SettingsRepository
import com.zkjd.lingdong.ui.home.HomeViewModel
import com.zkjd.lingdong.ui.led.LedColorViewModel.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
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
import timber.log.Timber
import javax.inject.Inject

/**
 * 新的功能配置ViewModel
 */
@HiltViewModel
class FunctionViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    @ApplicationContext private val context: Context,
    val settingsRepository: SettingsRepository,
    private val buttonFunctionEvent: ButtonFunctionEvent,
) : ViewModel() {
    
    // 当前设备
    private val _currentDevice = MutableStateFlow<Device?>(null)
    val currentDevice: StateFlow<Device?> = _currentDevice.asStateFlow()
    
    // 临时存储的按钮功能映射
    private val _tempButtonFunctions = MutableStateFlow<Map<ButtonType, ButtonFunction?>>(emptyMap())
    val tempButtonFunctions: StateFlow<Map<ButtonType, ButtonFunction?>> = _tempButtonFunctions.asStateFlow()
    
    // 当前选中的按钮类型，默认为短按
    private val _selectedButtonType = MutableStateFlow(ButtonType.SHORT_PRESS)
    val selectedButtonType: StateFlow<ButtonType> = _selectedButtonType.asStateFlow()

    // 是否为旋钮
    private var _isRoty = MutableStateFlow(false)
    val isRoty: StateFlow<Boolean> = _isRoty.asStateFlow()

    // 是否保存
    private var _isSave = MutableStateFlow(false)
    val isSave: StateFlow<Boolean> = _isSave.asStateFlow()

    // 设备事件
    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents

    val useCarTypeName: StateFlow<String> = settingsRepository.useCarTypeName
        .onEach { value ->Timber.w("读取DataStore22: $value") }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "s05")

    /**
     * 获取应用上下文
     */
    fun getContext(): Context = context
    
    /**
     * 设置当前选中的按钮类型
     */
    fun setSelectedButtonType(buttonType: ButtonType) {
        _selectedButtonType.value = buttonType
    }

    /**
     * 加载设备功能配置
     */
    fun loadDeviceFunctions(deviceMac: String) {
        if(_currentDevice.value!=null && _currentDevice.value!!.macAddress==deviceMac) return
        viewModelScope.launch {
            try {
                val device = deviceRepository.getDeviceByMacAddress(deviceMac)
                _currentDevice.value = device
                Log.d("bleName", "设备bleName: ${device?.bleName}")
                if(device!!.bleName.contains("DeepalTag_L0"))
                {
                    _isRoty.value=true
                }else{
                    _isRoty.value=true
                }

                // 加载所有按钮功能
                val buttonFunctions = mutableMapOf<ButtonType, ButtonFunction?>()
                ButtonType.values().forEach { buttonType ->
                    buttonFunctions[buttonType] = deviceRepository.getButtonFunction(deviceMac, buttonType)
                }

                _tempButtonFunctions.value = buttonFunctions.toMutableMap()
                Timber.d("已加载设备功能配置: $buttonFunctions")
            } catch (e: Exception) {
                Timber.e(e, "加载设备功能配置失败")
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
//                            ButtonType.FONE_PESS -> _uiEvents.emit(UiEvent.ShowToast("进入返控模式"))
//                            ButtonType.FTWO_PESS -> _uiEvents.emit(UiEvent.ShowToast("退出返控模式"))
//                            else -> {}
//                        }
//                    }
//                    is DeviceEvent.BatteryLevelChanged -> {
//                        // 电量变化事件不需要UI反馈
//                        if(event.level<=10)
//                            _uiEvents.emit(UiEvent.ShowToast("妥妥贴电池电量已过低，请及时更换电池！"))
//                    }
//                    else -> {}
//                }
//            }
//        }
    }

    

    /**
     * 临时保存按钮功能
     */
    fun tempSaveButtonFunction(buttonType: ButtonType, function: ButtonFunction?) {
        val updatedFunctions = _tempButtonFunctions.value.toMutableMap()
//        for(functionx in updatedFunctions)
//        {
//            if(functionx.value?.name?.startsWith(function?.name.toString()) == true)
//            {
//                updatedFunctions[buttonType] = null
//            }
//        }
        updatedFunctions[buttonType] = function
        _tempButtonFunctions.value = updatedFunctions
        _isSave.value=true
    }
    
    /**
     * 保存所有按钮功能
     */
    fun saveAllButtonFunctions(deviceMac: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 保存所有临时存储的按钮功能
                _tempButtonFunctions.value.forEach { (buttonType, function) ->
                    if (function != null) {
                        deviceRepository.setButtonFunction(deviceMac, buttonType, function)
                        Timber.d("批量保存按钮功能: $buttonType -> $function")
                    } else {
                        // 当function为null时，也要清除对应的功能
                        deviceRepository.clearButtonFunction(deviceMac, buttonType)
                        Timber.d("批量清除按钮功能: $buttonType")
                    }
                }
                // 显示保存成功提示
                showSaveSuccessToast()
                
                // 刷新设备数据，确保主界面可以看到最新的功能设置
                deviceRepository.refreshDevices()
                buttonFunctionEvent.sendFunctionChangedEvent(deviceMac)
            } catch (e: Exception) {
                Timber.e(e, "批量保存按钮功能失败")
            }
        }
    }

    /**
     * 重命名设备
     */
    fun renameDevice(macAddress: String, name: String) {
        viewModelScope.launch {

            if(!name.isLongerThan20Chars()){
            withContext(Dispatchers.IO) {
                deviceRepository.renameDevice(macAddress, name)
            }}else{
                _uiEvents.emit(UiEvent.ShowToast("请输入小于20个字的名称！"))
            }
        }
    }

    fun String.isLongerThan20Chars(): Boolean {
        return this.length > 20
    }


    /**
     * 显示保存成功提示
     */
    fun showSaveSuccessToast() {
        // 在实际实现中，这里应该使用一个事件来通知UI显示Toast
        // 你可以使用SharedFlow或其他方式实现
        Timber.d("保存成功")
    }

    /**
     * UI事件
     */
    sealed class UiEvent {
        data class ShowToast(val message: String) : UiEvent()
        data class Navigate(val route: String) : UiEvent()
    }

} 