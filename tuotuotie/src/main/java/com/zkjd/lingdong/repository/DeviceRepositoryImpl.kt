package com.zkjd.lingdong.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.graphics.Color
import com.zkjd.lingdong.bluetooth.BleConstants
import com.mine.baselibrary.bluetooth.ScanState
import com.zkjd.lingdong.bluetooth.TuoTuoTieAbsBleManager
import com.zkjd.lingdong.data.DefaultFunctions
import com.zkjd.lingdong.data.FunctionsConfig
import com.zkjd.lingdong.data.dao.ButtonFunctionMappingDao
import com.zkjd.lingdong.data.dao.DeviceDao
import com.zkjd.lingdong.model.ButtonFunction
import com.zkjd.lingdong.model.ButtonType
import com.zkjd.lingdong.model.ConnectionState
import com.zkjd.lingdong.model.Device
import com.zkjd.lingdong.model.FunctionCategory
import com.zkjd.lingdong.utils.DatabaseHelper
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import com.zkjd.lingdong.bluetooth.TuoTuoTieDeviceEvent
import com.mine.baselibrary.bluetooth.DeviceEvent as BleDeviceEvent

private const val TAG = "DeviceRepository"

/**
 * 设备存储库实现类
 */
@Suppress("DEPRECATION")
@Module
@InstallIn(SingletonComponent::class)
class DeviceRepositoryImpl @Inject constructor(
    private val deviceDao: DeviceDao,
    private val buttonFunctionMappingDao: ButtonFunctionMappingDao,
    private val bleManager: TuoTuoTieAbsBleManager,
    @ApplicationContext private val context: Context
) : DeviceRepository {
    
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val deviceEventsFlow = MutableSharedFlow<DeviceEvent>(replay = 0)

    // 监听断开连接请求事件
    private val disconnectRequestEventCollector = coroutineScope.launch {
        bleManager.getDisconnectRequestEvents().collect { event ->
            Timber.tag(TAG).w("收到断开连接请求: ${event.deviceAddress}, 原因: ${event.reason}")
            // 调用 disconnectDevice 方法断开设备连接
            disconnectDevice(event.deviceAddress)
        }
    }
    
    // 将蓝牙设备事件转换为应用设备事件
    @SuppressLint("UseKtx")
    private val bleDeviceEventCollector = coroutineScope.launch {
        bleManager.getDeviceEvents().collect { bleEvent ->
            when (bleEvent) {
                is BleDeviceEvent.Connected -> {
                    val macAddress = bleEvent.device.address
                    updateConnectionState(macAddress, ConnectionState.CONNECTED)
                    setReturnControl(macAddress,0)
                    deviceEventsFlow.emit(DeviceEvent.DeviceConnected(macAddress))
                }
                is BleDeviceEvent.Disconnected -> {
                    val macAddress = bleEvent.device.address
                    updateConnectionState(macAddress, ConnectionState.DISCONNECTED)
                    deviceEventsFlow.emit(DeviceEvent.DeviceDisconnected(macAddress))
                }
                is BleDeviceEvent.DeviceReady -> {
                    val macAddress = bleEvent.device.address
                    Timber.tag("DeviceRepository").d("设备已准备好接收通知: $macAddress")
                    // 确保发送就绪事件
                    deviceEventsFlow.emit(DeviceEvent.DeviceReady(macAddress))
                }
                is BleDeviceEvent.BatteryLevelChanged -> {
                    val macAddress = bleEvent.device.address
                    val level = bleEvent.level
                    val name= getDeviceByMacAddress(macAddress)?.name
                    // 更新数据库中的电量
                    updateBatteryLevel(macAddress, level)
                    // 发送事件通知
                    deviceEventsFlow.emit(DeviceEvent.BatteryLevelChanged(name.toString(), level))
                }
                is BleDeviceEvent.ConnectionFailed -> {
                    val macAddress = bleEvent.device.address
                    updateConnectionState(macAddress, ConnectionState.DISCONNECTED)
                    deviceEventsFlow.emit(DeviceEvent.ConnectionFailed(macAddress, bleEvent.reason))
                }

                is BleDeviceEvent.AuthSuccess -> {
                    val macAddress = bleEvent.device.address
                    deviceEventsFlow.emit(DeviceEvent.AuthSuccess(macAddress))
                }
                is BleDeviceEvent.AuthFailed -> {
                    val macAddress = bleEvent.device.address
                    deviceEventsFlow.emit(DeviceEvent.AuthFailed(macAddress, bleEvent.reason))
                }

            }
        }
    }
    
    // 监听TuoTuoTie设备特定事件
    private val tuoTuoTieDeviceEventCollector = coroutineScope.launch {
        bleManager.getTuoTuoTieDeviceEvents().collect { event ->
            when (event) {
                is TuoTuoTieDeviceEvent.ButtonPressed -> {
                    val macAddress = event.device.address
                    val buttonType = when (event.keyCode) {
                        BleConstants.KeyCode.KEY_SHORT_PRESS -> ButtonType.SHORT_PRESS
                        BleConstants.KeyCode.KEY_LONG_PRESS -> ButtonType.LONG_PRESS
                        BleConstants.KeyCode.KEY_DOUBLE_CLICK -> ButtonType.DOUBLE_CLICK
                        BleConstants.KeyCode.KNOB_CLOCKWISE -> ButtonType.RIGHT_ROTATE
                        BleConstants.KeyCode.KNOB_ANTICLOCKWISE -> ButtonType.LEFT_ROTATE
                        BleConstants.KeyCode.REVERSE_CONTROL_ENTER -> ButtonType.FONE_PESS
                        BleConstants.KeyCode.REVERSE_CONTROL_EXIT -> ButtonType.FTWO_PESS
                        else -> null
                    }
                    //读到返控指令1为进入返控状态，0为退出返控状态
                    when(buttonType)
                    {
                        ButtonType.FONE_PESS -> setReturnControl(macAddress,1)
                        ButtonType.FTWO_PESS -> setReturnControl(macAddress,0)
                        else -> {}
                    }
                    buttonType?.let {
                        Timber.tag("DeviceRepository").d("收到按键事件: $macAddress, 类型: $it")
                        deviceEventsFlow.emit(DeviceEvent.ButtonPressed(macAddress, it))
                    }
                }
                is TuoTuoTieDeviceEvent.SetReadVules -> {
                    val macAddress = event.device.address
                    withContext(Dispatchers.IO) {
                        val device = getDeviceByMacAddress(macAddress)
                        if (device != null) {
                            val colors = event.vules.substring(4)
                            // 更新设备的两种颜色
                            device.connectedLedColor = Color.parseColor("#FF"+colors).toInt()
                            device.lastConnectionState=ConnectionState.CONNECTED
                            setLedColor(macAddress, true, device.connectedLedColor)

                            val pA = event.vules.substring(2,4)
                            device.preventAccidental=pA.toInt()
                            setPreventAccidental(macAddress,pA.toInt())

                            updateDevice(device)
                        }
                    }
                }
            }
        }
    }

    init {
        // 初始化蓝牙管理器
        bleManager.initialize(context)
        

    }

    override fun getDevice(macAddress: String): Flow<Device?> = deviceDao.getDevice(macAddress)

    override fun getAllDevices(): Flow<List<Device>> = deviceDao.getAllDevices()

    override fun getAllDevicesAsList(): List<Device> = deviceDao.getAllDevicesAsList()

    override suspend fun getAllBluetoothDevice(): List<BluetoothDevice> = bleManager.getBleDevices()
    
    override suspend fun getDeviceByMacAddress(macAddress: String): Device? = 
        DatabaseHelper.executeOnIOThread { deviceDao.getDeviceByMacAddress(macAddress) }
    
    override suspend fun addDevice(device: Device) {
        DatabaseHelper.executeOnIOThread { deviceDao.insertDevice(device) }
    }
    
    override suspend fun updateDevice(device: Device) {
        DatabaseHelper.executeOnIOThread { deviceDao.updateDevice(device) }
    }
    

    override suspend fun deleteDevice(address: String,result:(success: Boolean)-> Unit):Int {
        try {
            // 删除数据库中的设备信息
            val deleteCount = DatabaseHelper.executeOnIOThread {
                // 先删除与设备相关的所有按键功能映射
                buttonFunctionMappingDao.deleteAllMappingsForDevice(address)
                // 然后删除设备
                deviceDao.deleteDeviceByMacAddress(address)
            }
            // 取消设备配对（即使失败也不影响删除结果）
            try {
                unpairDevice(address)
            } catch (e: Exception) {
                Timber.e(e, "取消设备配对失败: $address")
            }
            result(deleteCount > 0)
            return deleteCount
        } catch (e: Exception) {
            Timber.e(e, "删除设备失败: $address")
            result(false)
            return 0
        }
    }
    
    /**
     * 取消设备配对
     * @param macAddress 设备MAC地址
     * @return 是否成功取消配对
     */
    override suspend fun unpairDevice(macAddress: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 获取蓝牙适配器
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                Timber.e("蓝牙未启用，无法取消配对")
                return@withContext false
            }
            
            // 获取已配对设备
            val bondedDevices = bluetoothAdapter.bondedDevices
            var device = bondedDevices.find { it.address == macAddress }


            if (device != null) {
                // 使用反射调用removeBond方法取消配对
                val method = device.javaClass.getMethod("removeBond")
                val result = method.invoke(device) as Boolean

                if (result) {
                    Timber.d("设备成功取消配对: $macAddress")
                    // 更新设备连接状态
                    updateConnectionState(macAddress, ConnectionState.DISCONNECTED)
                } else {
                    Timber.e("设备取消配对失败: $macAddress")
                }
            }else{
                device = bluetoothAdapter.getRemoteDevice(macAddress)
            }

            // 断开设备连接
            if (device != null) {
                bleManager.disconnect(device)
            }
            

            return@withContext true
        } catch (e: Exception) {
            Timber.e(e, "取消设备配对时出错: $macAddress")
            return@withContext false
        }
    }
    
    override suspend fun updateConnectionState(macAddress: String, state: ConnectionState) {
        DatabaseHelper.executeOnIOThread {
            deviceDao.updateConnectionState(macAddress, state)
        }
    }
    
    override suspend fun updateBatteryLevel(macAddress: String, level: Int) {
        DatabaseHelper.executeOnIOThread { deviceDao.updateBatteryLevel(macAddress, level) }
    }
    
    override suspend fun renameDevice(macAddress: String, name: String) {
        DatabaseHelper.executeOnIOThread { deviceDao.updateDeviceName(macAddress, name) }
    }
    
    override suspend fun setLedColor(macAddress: String, isConnectedMode: Boolean, colorInt: Int) {
        DatabaseHelper.executeOnIOThread {
            if (isConnectedMode) {
                deviceDao.updateConnectedLedColor(macAddress, colorInt)
            } else {
                deviceDao.updateReconnectingLedColor(macAddress, colorInt)
            }
        }
    }

    override suspend fun setPreventAccidental(macAddress: String,  colorInt: Int) {
        DatabaseHelper.executeOnIOThread {

            deviceDao.updatePreventAccidental(macAddress, colorInt)

        }
    }

    override suspend fun setAutoConnected(macAddress: String, needAutoConnect: Boolean) {
        DatabaseHelper.executeOnIOThread {

            deviceDao.updateDeviceAutoConnect(macAddress, needAutoConnect)

        }
    }

    override suspend fun setReturnControl(macAddress: String,  rC: Int) {
        DatabaseHelper.executeOnIOThread {

            deviceDao.updateReturnControl(macAddress, rC)

        }
    }


    override suspend fun setAntiMisoperation(macAddress: String, enabled: Boolean) {
        DatabaseHelper.executeOnIOThread { deviceDao.updateAntiMisoperation(macAddress, enabled) }
    }

    override suspend fun setIsAppOpenOne(macAddress: String, enabled: Boolean) {
        DatabaseHelper.executeOnIOThread { deviceDao.updateIsAppOpen1(macAddress, enabled) }
    }

    override suspend fun setIsAppOpenTwo(macAddress: String, enabled: Boolean) {
        DatabaseHelper.executeOnIOThread { deviceDao.updateIsAppOpen2(macAddress, enabled) }
    }


    override suspend fun setMusicCan(macAddress: String, enabled: Boolean) {
        DatabaseHelper.executeOnIOThread { deviceDao.updatemusicCan(macAddress, enabled) }
    }

    override suspend fun renameMusicID(macAddress: String, id: String) {
        DatabaseHelper.executeOnIOThread { deviceDao.updatemusicName(macAddress, id) }
    }
    
    override suspend fun getButtonFunction(macAddress: String, buttonType: ButtonType): ButtonFunction? {
        return DatabaseHelper.executeOnIOThread {
            val mapping = buttonFunctionMappingDao.getMappingForDeviceAndButton(macAddress, buttonType)
            if(mapping?.customParams!=null && mapping.customParams.isNotEmpty()){
                val ss = mapping.customParams.split(":")
                mapping.functionId?.let { ButtonFunction( name = ss[0], actionCode = ss[1], id = it, category = FunctionCategory.APP , configWords = "") }
            }else {
                mapping?.functionId?.let { functionId ->
                    val functionsConfig = FunctionsConfig.getInstance(context)
                    functionsConfig.getFunctionById(functionId) ?: DefaultFunctions.getFunctionById(
                        functionId
                    )
                }
            }
        }
    }
    
    override suspend fun setButtonFunction(macAddress: String, buttonType: ButtonType, function: ButtonFunction) {
        DatabaseHelper.executeOnIOThread {
            val customParams = if(function.category==FunctionCategory.APP) function.name+":"+function.actionCode else ""
            buttonFunctionMappingDao.setMapping(macAddress, buttonType, function.id, customParams)
        }
    }
    
    override fun startScan() {
        bleManager.startScan()
    }

    override fun stopScan() {
        bleManager.stopScan()
    }

    override fun getScanState(): Flow<ScanState> = bleManager.getScanState()

    override suspend fun connectDevice(bluetoothDevice: BluetoothDevice) :Boolean{
        updateConnectionState(bluetoothDevice.address, ConnectionState.CONNECTING)
        // 连接设备
        val success = bleManager.connect(bluetoothDevice)

        // 检查设备是否已存在于数据库
        val existingDevice = DatabaseHelper.executeOnIOThread {
            deviceDao.getDeviceByMacAddress(bluetoothDevice.address)
        }

        if (existingDevice == null) {
            return success;
        }

        // 更新连接状态
        if (!success) {
            // 如果连接失败，更新设备状态
            updateConnectionState(bluetoothDevice.address, ConnectionState.DISCONNECTED)
        }/*else{
            updateConnectionState(bluetoothDevice.address, ConnectionState.CONNECTED)
            //bleManager.removeDeviceByAddress(bluetoothDevice.address)
        }*/
        return success
    }
    
    override suspend fun disconnectDevice(macAddress: String) {
        // 更新连接状态
        updateConnectionState(macAddress, ConnectionState.DISCONNECTED)
        
        // 找到所有连接中的蓝牙设备
        val bluetoothDevices = withContext(Dispatchers.IO) {
            BluetoothDevice::class.java.classLoader!!.loadClass("android.bluetooth.BluetoothAdapter")
                .getMethod("getDefaultAdapter").invoke(null)
                .run {
                    javaClass.getMethod("getBondedDevices").invoke(this) as Set<*>
                }
                .filterIsInstance<BluetoothDevice>()
        }
        
        // 查找匹配MAC地址的设备
        var btDevice = bluetoothDevices.find { it.address == macAddress }

        if (btDevice == null) {
            try {
                Timber.tag(TAG).i("没有找到配对的蓝牙 $macAddress")
                val bluetoothManager =
                    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val bluetoothAdapter = bluetoothManager.adapter
                btDevice = bluetoothAdapter.getRemoteDevice(macAddress)
                Timber.tag(TAG).i("find by bluetoothAdapter: $btDevice")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "find by bluetoothAdapter: $btDevice")
            }
        }
        
        // 断开设备连接
        btDevice?.let { bleManager.disconnect(it) }
    }
    
    override fun getDeviceEvents(): Flow<DeviceEvent> = deviceEventsFlow
    
    override suspend fun setDeviceLedColor(macAddress: String, isConnectedState: Boolean) {
        val device = DatabaseHelper.executeOnIOThread {
            deviceDao.getDeviceByMacAddress(macAddress)
        } ?: return
        
        // 找到所有连接中的蓝牙设备
        val bluetoothDevices = withContext(Dispatchers.IO) {
            BluetoothDevice::class.java.classLoader!!.loadClass("android.bluetooth.BluetoothAdapter")
                .getMethod("getDefaultAdapter").invoke(null)
                .run {
                    javaClass.getMethod("getBondedDevices").invoke(this) as Set<*>
                }
                .filterIsInstance<BluetoothDevice>()
        }
        
        // 查找匹配MAC地址的设备
        val btDevice = bluetoothDevices.find { it.address == macAddress } ?: return
        
        // 设置颜色
        val color = if (isConnectedState) device.connectedLedColor else device.reconnectingLedColor
        
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        
        if (isConnectedState) {
            bleManager.setWorkingLedColor(btDevice, r, g, b)
        } else {
            bleManager.setReconnectingLedColor(btDevice, r, g, b)
        }
    }

    @SuppressLint("SuspiciousIndentation")
    override suspend fun setColor(macAddress: String, isConnectedState: Boolean, colors: Int){

        val r = (colors shr 16) and 0xFF
        val g = (colors shr 8) and 0xFF
        val b = colors and 0xFF

        bleManager.setWorkingLedColor2(macAddress, r, g, b)

    }

    override suspend fun setPreventAccid(macAddress: String, pA: Int){

        val message=if(pA==0) "防误触关闭" else "防误触开启"
        Timber.d("下发防误触功能: $message")
        bleManager.setPreventAccid(macAddress, pA)

    }

    override suspend fun setDeviceAntiMisoperation(macAddress: String) {
        val device = DatabaseHelper.executeOnIOThread {
            deviceDao.getDeviceByMacAddress(macAddress)
        } ?: return
        
        // 找到所有连接中的蓝牙设备
        val bluetoothDevices = withContext(Dispatchers.IO) {
            BluetoothDevice::class.java.classLoader!!.loadClass("android.bluetooth.BluetoothAdapter")
                .getMethod("getDefaultAdapter").invoke(null)
                .run {
                    javaClass.getMethod("getBondedDevices").invoke(this) as Set<*>
                }
                .filterIsInstance<BluetoothDevice>()
        }
        
        // 查找匹配MAC地址的设备
        val btDevice = bluetoothDevices.find { it.address == macAddress } ?: return
        
        // 设置防误触模式
        bleManager.setAntiMisoperation(btDevice, device.isAntiMisoperationEnabled)
    }
    
    /**
     * 清除指定设备的按键功能
     */
    override suspend fun clearButtonFunction(macAddress: String, buttonType: ButtonType) {
        DatabaseHelper.executeOnIOThread {
            buttonFunctionMappingDao.deleteMappingForDeviceAndButton(macAddress, buttonType)
        }
        Timber.d("清除按钮功能: $macAddress, $buttonType")
    }
    
    /**
     * 刷新设备数据
     * 
     * 当设备状态在其他地方被修改时（如按键功能被更新），
     * 调用此方法以确保主界面的设备列表能够反映最新的状态。
     */
    override suspend fun refreshDevices() {
        withContext(Dispatchers.IO) {
            // 因为Room的Flow会自动在数据库变化时通知观察者，
            // 所以这里我们主要是确保设备对象中的功能字段被正确更新
            val devices = deviceDao.getAllDevicesAsList()

            // 遍历所有设备并更新其功能字段
            devices.forEach { device ->
                // 重新构建一个包含最新功能的设备对象
                val updatedDevice = device.copy(
                    shortPressFunction = getButtonFunction(
                        device.macAddress,
                        ButtonType.SHORT_PRESS
                    ),
                    longPressFunction = getButtonFunction(device.macAddress, ButtonType.LONG_PRESS),
                    doubleClickFunction = getButtonFunction(
                        device.macAddress,
                        ButtonType.DOUBLE_CLICK
                    ),
                    leftRotateFunction = getButtonFunction(
                        device.macAddress,
                        ButtonType.LEFT_ROTATE
                    ),
                    rightRotateFunction = getButtonFunction(
                        device.macAddress,
                        ButtonType.RIGHT_ROTATE
                    )
                )

                // 更新到数据库
                deviceDao.updateDevice(updatedDevice)
            }
            Timber.d("已刷新 ${devices.size} 个设备的数据")
        }
    }
} 