package com.zkjd.lingdong.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zkjd.lingdong.data.FunctionsConfig
import com.zkjd.lingdong.model.ButtonType
import com.zkjd.lingdong.model.Device
import com.zkjd.lingdong.repository.DeviceEvent
import com.zkjd.lingdong.repository.DeviceRepository
import com.zkjd.lingdong.repository.SettingsRepository
import com.zkjd.lingdong.event.ButtonFunctionEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * 主页ViewModel
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val buttonFunctionEvent: ButtonFunctionEvent,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    fun getContext(): Context = context
    internal val TAG = "HomeViewModel"

    // 添加一个中间状态流以便我们可以处理从数据库获取的设备，并填充按键功能
    private val _devicesWithFunctions = MutableStateFlow<List<Device>>(emptyList())

    // 所有设备 - 使用中间状态流作为源
    val devices: StateFlow<List<Device>> = _devicesWithFunctions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 设备事件
    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents
    
    // 是否使用镁佳车机执行器设置
    val useMeijiaCar: StateFlow<Boolean> = settingsRepository.useMeijiaCar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        
    // 是否使用车机音频设置
    val useCarAudio: StateFlow<Boolean> = settingsRepository.useCarAudio
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)   
        
    // 是否启用设备鉴权设置
    val enableAuthentication: StateFlow<Boolean> = settingsRepository.enableAuthentication
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // 是否启用设备鉴权设置
    val initialSetup: StateFlow<Boolean> = settingsRepository.initialSetup
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // 是否启用设备鉴权设置
    val useMusic: StateFlow<Boolean> = settingsRepository.useMusic
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

        
    // 跟踪已成功连接的设备MAC地址
    private val connectedDevices = mutableSetOf<String>()

    init {
        Timber.tag(TAG).i("HomeViewModel 初始化")
        // 监听设备事件
        viewModelScope.launch {
            deviceRepository.getDeviceEvents().collect { event ->
                when (event) {
                    is DeviceEvent.DeviceConnected -> {
                        Timber.tag(TAG).i("设备已连接: ${event.macAddress}")
                        _uiEvents.emit(UiEvent.ShowToast("设备已连接"))
                        // 记录设备已连接
                        connectedDevices.add(event.macAddress)
                    }
                    is DeviceEvent.DeviceDisconnected -> {
                        // 只有设备曾经成功连接过，才显示断开提示
                        if (connectedDevices.contains(event.macAddress)) {
                            Timber.tag(TAG).i("设备已断开连接: ${event.macAddress}")
                            _uiEvents.emit(UiEvent.ShowToast("设备已断开连接"))
                            // 从已连接设备列表中移除
                            connectedDevices.remove(event.macAddress)
                        }
                    }
                    is DeviceEvent.ConnectionFailed -> {
                        Timber.tag(TAG).i("设备连接失败: ${event.macAddress}, 原因: ${event.reason}")
                        _uiEvents.emit(UiEvent.ShowToast("连接失败: ${event.reason}"))
                    }
                    is DeviceEvent.ButtonPressed -> {
                        // 显示按键功能名称或按键类型
                        handleButtonPressed(event)
                    }
                    is DeviceEvent.BatteryLevelChanged -> {
                        // 电量变化事件不需要UI反馈
                        if(event.level<=10) {
                            Timber.tag(TAG).i("设备电量低: ${event.macAddress}, 电量: ${event.level}%")
                            _uiEvents.emit(UiEvent.Navigate(event.macAddress))
                        }
                    }
                    is DeviceEvent.DeviceReady->{
                        Timber.tag(TAG).i("设备已准备就绪: ${event.macAddress}")
                        // 连接已准备好
                    }
                    is DeviceEvent.AuthSuccess -> {
                        Timber.tag(TAG).i("设备鉴权成功: ${event.macAddress}")
                        _uiEvents.emit(UiEvent.ShowToast("设备鉴权成功"))
                    }
                    is DeviceEvent.AuthFailed -> {
                        Timber.tag(TAG).i("设备鉴权失败: ${event.macAddress}, 原因: ${event.reason}")
                        //_uiEvents.emit(UiEvent.ShowToast("设备鉴权失败: ${event.reason}"))
                    }

                    else -> {}
                }
            }
        }

//        // 监听Function事件
//        viewModelScope.launch {
//            carFunctionExecutor.getDeviceEvents().collect { event ->
//                when(event){
//                    is DeviceEvent.SetFunction -> {
//                        // 如果有配置功能，显示功能名称；否则显示按键类型
//                        val message ="${event.keyString}"
//                        // 从当前内存中的设备列表获取设备信息，避免额外的数据库查询
////                        val device = _devicesWithFunctions.value.find { it.macAddress == event.macAddress }
////                        if (device == null) {
////                            _uiEvents.emit(UiEvent.ShowToast("未知设备: ${event.macAddress}"))
////                            return@launch
////                        }
//                        _uiEvents.emit(UiEvent.ShowToast(message))
//                    }
//                    else -> {}
//                }
//            }
//        }
//
//        // 监听Function事件
//        viewModelScope.launch {
//            carMeiJiaFunctionExecutor.getDeviceEvents().collect { event ->
//                when(event){
//                    is DeviceEvent.SetFunction -> {
//                        // 如果有配置功能，显示功能名称；否则显示按键类型
//                        val message ="${event.keyString}"
//                        // 从当前内存中的设备列表获取设备信息，避免额外的数据库查询
////                        val device = _devicesWithFunctions.value.find { it.macAddress == event.macAddress }
////                        if (device == null) {
////                            _uiEvents.emit(UiEvent.ShowToast("未知设备: ${event.macAddress}"))
////                            return@launch
////                        }
//                        _uiEvents.emit(UiEvent.ShowToast(message))
//                    }
//                    else -> {}
//                }
//            }
//        }
        
        // 监听设备列表变化
        viewModelScope.launch {
            deviceRepository.getAllDevices().collect { deviceList ->
                Timber.tag(TAG).i("设备列表更新: 设备数量=${deviceList.size}, MAC地址=${deviceList.map { it.macAddress }}")
                _devicesWithFunctions.value = deviceList
            }
        }
        
        // 监听按键功能变更事件
        viewModelScope.launch {
            buttonFunctionEvent.functionChangedEvent.collect { macAddress ->
                Timber.tag(TAG).i("按键功能变更事件: ${macAddress}")
                refreshDeviceFunctions(macAddress)
            }
        }
    //模拟鉴权测试使用
//        val car=BleAuthUtils.hexStringToByteArray("1122334455667788")
//        val lingdo=BleAuthUtils.hexStringToByteArray("1122334455667788")
//        val encryptedAddr=BleAuthUtils.hexStringToByteArray("53F778B74BB277A3751DE2919BDEFC47")
//        val authData=BleAuthUtils.hexStringToByteArray("EDEC92B40B780E54414A2D3BE8CD86E6")
//
//        val device="DE:3A:19:16:18:39"
//
//        Timber.tag(com.lucas.lingdong2.bluetooth.TAG).e("车机随机=1122334455667788")
//        Timber.tag(com.lucas.lingdong2.bluetooth.TAG).e("灵动随机=1122334455667788")
//        Timber.tag(com.lucas.lingdong2.bluetooth.TAG).e("需解地址=53F778B74BB277A3751DE2919BDEFC47")
//        Timber.tag(com.lucas.lingdong2.bluetooth.TAG).e("认证数据=EDEC92B40B780E54414A2D3BE8CD86E6")
//        // 生成密钥材料
//        val (iv, key) = BleAuthUtils.generateKeyMaterial(
//            car,
//            lingdo,
//            BleConstants.Auth.AUTH_LABEL
//        )
//
//        Timber.tag(com.lucas.lingdong2.bluetooth.TAG).e("iv=${BleAuthUtils.byteArrayToHexString(iv)}")
//        Timber.tag(com.lucas.lingdong2.bluetooth.TAG).e("key=${BleAuthUtils.byteArrayToHexString(key)}")

//        val expectedAddr = BleAuthUtils.padDeviceName2(device.replace(":",""), BleConstants.Auth.PUBLIC_ADDR_LENGTH)
//        val decryptedAddr = BleAuthUtils.aesDecrypt(encryptedAddr, key, iv)
//        Timber.tag(com.lucas.lingdong2.bluetooth.TAG).e("解密=${BleAuthUtils.byteArrayToHexString(decryptedAddr)}")
//        Timber.tag(com.lucas.lingdong2.bluetooth.TAG).e("我的=${BleAuthUtils.byteArrayToHexString(expectedAddr.reversedArray())}")
//
//        // 检查PublicAddr是否匹配
//        val addrMatched = decryptedAddr.contentEquals(expectedAddr.reversedArray())
//        if (!addrMatched) {
//            Timber.tag(com.lucas.lingdong2.bluetooth.TAG).e("PublicAddr不匹配: 期望=${BleAuthUtils.byteArrayToHexString(expectedAddr)}, 实际=${BleAuthUtils.byteArrayToHexString(decryptedAddr)}")
//        }

//        val expectedRawData = BleAuthUtils.generateRawAuthData(car, lingdo).copyOfRange(0,BleConstants.Auth.PUBLIC_ADDR_LENGTH).reversedArray()
//        val decryptedAuthData = BleAuthUtils.aesDecrypt(authData, key, iv)
//
//        Timber.tag(com.lucas.lingdong2.bluetooth.TAG).e("解密认证=${BleAuthUtils.byteArrayToHexString(decryptedAuthData)}")
//        Timber.tag(com.lucas.lingdong2.bluetooth.TAG).e("我的认证=${BleAuthUtils.byteArrayToHexString(expectedRawData)}")
    }

    /**
     * 保存选择的车辆类型名称
     */
    fun toSaveChoseCar(id: String) {
        viewModelScope.launch {
            Timber.tag(TAG).i("保存音效类型: $id")
            //settingsRepository.setUseCarTypeName(id)
            settingsRepository.setUseMusicTypeName(id)
        }
    }

    /**
     * 切换车机执行器类型
     */
    fun toggleCarExecutor() {
        viewModelScope.launch {
            val currentValue = useMeijiaCar.value
            if(!currentValue) {
                val newValue = !currentValue
                Timber.tag(TAG).i("切换车机执行器: ${if (currentValue) "CarService" else "镁佳Car"} -> ${if (newValue) "镁佳Car" else "CarService"}")
                settingsRepository.setUseMeijiaCar(newValue)
                val currentValue2 = useCarAudio.value
                settingsRepository.setUseCarAudio(!currentValue2)
                _uiEvents.emit(UiEvent.ShowToast("已切换到${if (!currentValue) "镁佳Car" else "CarService"}"))
            }
        }
    }


    /**
     * 切换车机音频类型
     */
    fun toggleCarAudio() {
        viewModelScope.launch {
            val currentValue = useCarAudio.value
            if(!currentValue) {
                val newValue = !currentValue
                Timber.tag(TAG).i("切换车机音频: ${if (currentValue) "AudioManager" else "CarAudio"} -> ${if (newValue) "CarAudio" else "AudioManager"}")
                settingsRepository.setUseCarAudio(newValue)
                val currentValue2 = useMeijiaCar.value
                settingsRepository.setUseMeijiaCar(!currentValue2)
                _uiEvents.emit(UiEvent.ShowToast("已切换到${if (!currentValue) "CarAudio" else "AudioManager"}"))
            }
        }
    }
    
    /**
     * 切换设备鉴权开关
     */
    fun toggleAuthentication() {
        viewModelScope.launch {
            val currentValue = enableAuthentication.value
            val newValue = !currentValue
            Timber.tag(TAG).i("切换设备鉴权开关: ${if (currentValue) "开启" else "关闭"} -> ${if (newValue) "开启" else "关闭"}")
            settingsRepository.setEnableAuthentication(newValue)
            _uiEvents.emit(UiEvent.ShowToast("设备鉴权已${if (!currentValue) "开启" else "关闭"}"))
        }
    }

    /**
     * 切换设备鉴权开关
     */
    fun toggleMusic() {
        viewModelScope.launch {
            val currentValue = useMusic.value
            val newValue = !currentValue
            Timber.tag(TAG).i("切换音效开关: ${if (currentValue) "开启" else "关闭"} -> ${if (newValue) "开启" else "关闭"}")
            settingsRepository.setUseMusic(newValue)
            _uiEvents.emit(UiEvent.ShowToast("音效已${if (!currentValue) "开启" else "关闭"}"))
        }
    }

    /**
     * 首次弹出已完成，后续无需再弹出
     */
    fun toInitialSetupExecutor() {
        viewModelScope.launch {
            Timber.tag(TAG).i("完成初始设置")
            settingsRepository.setInitialSetup(true)
        }
    }


    /**
     * 刷新指定设备的按键功能
     */
    private suspend fun refreshDeviceFunctions(macAddress: String) {
        val currentDevices = _devicesWithFunctions.value
        val deviceIndex = currentDevices.indexOfFirst { it.macAddress == macAddress }
        
        if (deviceIndex >= 0) {
            val device = currentDevices[deviceIndex]
            Timber.tag(TAG).i("刷新设备按键功能: ${device.macAddress}, 设备名称: ${device.name}")
            
            // 为设备加载所有类型的按键功能
            val shortPressFunction = deviceRepository.getButtonFunction(device.macAddress, ButtonType.SHORT_PRESS)
            val longPressFunction = deviceRepository.getButtonFunction(device.macAddress, ButtonType.LONG_PRESS)
            val doubleClickFunction = deviceRepository.getButtonFunction(device.macAddress, ButtonType.DOUBLE_CLICK)
            val leftRotateFunction = deviceRepository.getButtonFunction(device.macAddress, ButtonType.LEFT_ROTATE)
            val rightRotateFunction = deviceRepository.getButtonFunction(device.macAddress, ButtonType.RIGHT_ROTATE)
            
            // 创建包含更新后按键功能的新设备对象
            val updatedDevice = device.copy(
                shortPressFunction = shortPressFunction,
                longPressFunction = longPressFunction,
                doubleClickFunction = doubleClickFunction,
                leftRotateFunction = leftRotateFunction,
                rightRotateFunction = rightRotateFunction
            )
            
            // 更新设备列表
            val updatedDevices = currentDevices.toMutableList()
            updatedDevices[deviceIndex] = updatedDevice
            _devicesWithFunctions.value = updatedDevices
            
            Timber.tag(TAG).i("设备按键功能更新完成: ${device.macAddress}, 短按=${shortPressFunction?.name ?: "未配置"}, 长按=${longPressFunction?.name ?: "未配置"}, 双击=${doubleClickFunction?.name ?: "未配置"}, 左旋=${leftRotateFunction?.name ?: "未配置"}, 右旋=${rightRotateFunction?.name ?: "未配置"}")
            Timber.d("已更新设备按键功能: ${device.macAddress}")
        } else {
            Timber.tag(TAG).i("刷新设备按键功能失败: 未找到设备 ${macAddress}")
        }
    }
    
    /**
     * 加载设备的所有按键功能并更新设备列表
     */
    private fun loadDeviceFunctions(deviceList: List<Device>) {
        viewModelScope.launch {
            val updatedDevices = deviceList.map { device ->
                // 为每个设备加载所有类型的按键功能
                val shortPressFunction = deviceRepository.getButtonFunction(device.macAddress, ButtonType.SHORT_PRESS)
                val longPressFunction = deviceRepository.getButtonFunction(device.macAddress, ButtonType.LONG_PRESS)
                val doubleClickFunction = deviceRepository.getButtonFunction(device.macAddress, ButtonType.DOUBLE_CLICK)
                val leftRotateFunction = deviceRepository.getButtonFunction(device.macAddress, ButtonType.LEFT_ROTATE)
                val rightRotateFunction = deviceRepository.getButtonFunction(device.macAddress, ButtonType.RIGHT_ROTATE)
                
                // 创建包含按键功能的新设备对象
                device.copy(
                    shortPressFunction = shortPressFunction,
                    longPressFunction = longPressFunction,
                    doubleClickFunction = doubleClickFunction,
                    leftRotateFunction = leftRotateFunction,
                    rightRotateFunction = rightRotateFunction
                )
            }
            
            Timber.d("已加载设备按键功能: ${updatedDevices.map { it.macAddress to listOf(
                it.shortPressFunction != null,
                it.longPressFunction != null,
                it.doubleClickFunction != null,
                it.leftRotateFunction != null,
                it.rightRotateFunction != null
            ) }}")
            
            _devicesWithFunctions.value = updatedDevices
        }
    }


    /**
     * 删除设备
     */
    fun deleteDevice(macAddress: String,result: (delete: Boolean)->Unit) {
        Timber.tag(TAG).i("开始删除设备: begin")
        viewModelScope.launch {
            try {
                Timber.tag(TAG).i("开始删除设备: $macAddress")
                withContext(Dispatchers.IO) {

                    deviceRepository.disconnectDevice(macAddress)

                    // 删除设备同时会尝试取消配对
                   val deleteCount = deviceRepository.deleteDevice(macAddress){ success ->
                       result(success)
                   }
                    Timber.tag(TAG).i( "设备删除成功 $macAddress $deleteCount")
                }
                // 发送成功提示
                _uiEvents.emit(UiEvent.ShowToast("设备已删除：${macAddress}"))
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "删除设备失败: ${macAddress}")
                _uiEvents.emit(UiEvent.ShowToast("删除设备失败：${e.message}"))
            }
        }
    }

    fun getAllDevice():List<Device>{
        return deviceRepository.getAllDevicesAsList()
    }

    /**
     * 重命名设备
     */
    fun renameDevice(macAddress: String, name: String) {
        viewModelScope.launch {
            Timber.tag(TAG).i("重命名设备: $macAddress, 新名称: $name")
            withContext(Dispatchers.IO) {
                deviceRepository.renameDevice(macAddress, name)
            }
            Timber.tag(TAG).i("设备重命名完成: $macAddress, 新名称: $name")
        }
    }

    /**
     * 刷新设备信息
     */
    fun refreshDevices() {
        viewModelScope.launch {
            Timber.tag(TAG).i("开始刷新设备信息")
            withContext(Dispatchers.IO) {
                deviceRepository.refreshDevices();
            }
            Timber.tag(TAG).i("设备信息刷新完成")
        }
    }

    /**
     * 处理按钮按下事件
     * 显示功能名称或按键类型
     */
    private fun handleButtonPressed(event: DeviceEvent.ButtonPressed) {
        viewModelScope.launch {
            try {
                // 从当前内存中的设备列表获取设备信息，避免额外的数据库查询
                val device = _devicesWithFunctions.value.find { it.macAddress == event.macAddress }
                if (device == null) {
                    Timber.tag(TAG).i("按键事件: 未知设备 ${event.macAddress}, 按键类型: ${event.buttonType}")
                    _uiEvents.emit(UiEvent.ShowToast("未知设备: ${event.macAddress}"))
                    return@launch
                }
                
                // 根据按键类型获取对应的功能
                val buttonFunction = when (event.buttonType) {
                    ButtonType.SHORT_PRESS -> device.shortPressFunction
                    ButtonType.LONG_PRESS -> device.longPressFunction
                    ButtonType.DOUBLE_CLICK -> device.doubleClickFunction
                    ButtonType.LEFT_ROTATE -> device.leftRotateFunction
                    ButtonType.RIGHT_ROTATE -> device.leftRotateFunction //right是空的，旋转就存一个leftRotateFunction
                    ButtonType.FONE_PESS ->  null
                    ButtonType.FTWO_PESS ->  null
                }
                
                // 如果有配置功能，显示功能名称；否则显示按键类型
                val message = if (buttonFunction != null) {
                    "执行功能: ${buttonFunction.name}"
                } else {

                    when(event.buttonType)
                    {
                        ButtonType.FONE_PESS -> "进入返控模式"
                        ButtonType.FTWO_PESS -> "退出返控模式"
                        else -> "按键事件: ${getButtonTypeDisplayName(event.buttonType)}"
                    }

                }

                if (buttonFunction == null) {
                    _uiEvents.emit(UiEvent.ShowToast(device.name+message))
                }
                
                Timber.tag(TAG).i("按键事件: 设备=${device.name}(${event.macAddress}), 按键类型=${event.buttonType}, 功能=${buttonFunction?.name ?: "未配置功能"}")
                Timber.d("按键事件: ${device.name}, ${event.buttonType}, ${buttonFunction?.name ?: "未配置功能"}")
            } catch (e: Exception) {
                Timber.e(e, "处理按键事件失败: ${event.macAddress}, ${event.buttonType}")
            }
        }
    }

    /**
     * 获取按键类型的显示名称
     */
    private fun getButtonTypeDisplayName(buttonType: ButtonType): String {
        return when (buttonType) {
            ButtonType.SHORT_PRESS -> "短按"
            ButtonType.LONG_PRESS -> "长按"
            ButtonType.DOUBLE_CLICK -> "双击"
            ButtonType.LEFT_ROTATE -> "左旋转"
            ButtonType.RIGHT_ROTATE -> "右旋转"
            else -> ""
        }
    }



    /**
     * UI事件
     */
    sealed class UiEvent {
        data class ShowToast(val message: String) : UiEvent()
        data class Navigate(val route: String) : UiEvent()
    }
} 