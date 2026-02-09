package com.smartlife.fragrance.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.mine.baselibrary.bluetooth.DeviceEvent
import com.mine.baselibrary.bluetooth.ScanState
import com.smartlife.fragrance.bluetooth.FragranceAbsBleManager
import com.smartlife.fragrance.data.dao.FragranceDeviceDao
import com.smartlife.fragrance.data.model.CarStartStopCycle
import com.smartlife.fragrance.data.model.ConnectionState
import com.smartlife.fragrance.data.model.FragranceDevice
import com.smartlife.fragrance.data.model.Gear
import com.smartlife.fragrance.data.model.LightMode
import com.smartlife.fragrance.data.model.Mode
import com.smartlife.fragrance.data.model.PowerState
import com.smartlife.fragrance.utils.DatabaseHelper
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

private const val TAG = "FragranceRepository"

/**
 * 香氛设备存储库实现类
 */
@Suppress("DEPRECATION")
@Module
@InstallIn(SingletonComponent::class)
class FragranceRepositoryImpl @Inject constructor(
    private val deviceDao: FragranceDeviceDao,
    private val bleManager: FragranceAbsBleManager,
    @ApplicationContext private val context: Context
) : FragranceRepository {
    
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val deviceEventsFlow = MutableSharedFlow<DeviceEvent>(replay = 0)

    /**
     * 如果powerState改变，则发送这个事件，供首页监听powerState使用
     */
    private val powerStateChangeEventFlow = MutableSharedFlow<FragranceDevice>(replay = 0)

    //监听设备信息变化
    private val fragranceDeviceInfoColletor = coroutineScope.launch {
        bleManager.deviceInfoFlow.collect { fragranceDevice ->
            Timber.tag(TAG).w(" ${fragranceDevice.macAddress},${fragranceDevice.deviceName} 信息改变")
            // 根据fragranceDevice中的信息，更新数据库中相同macAddress的设备信息
            DatabaseHelper.executeOnIOThread {
                val existingDevice = deviceDao.getDeviceByMac(fragranceDevice.macAddress)
                if (existingDevice != null) {
                    // 更新所有12个功能字段，保留其他字段不变
                    val updatedDevice = existingDevice.copy(
                        powerState = fragranceDevice.powerState,
                        mode = fragranceDevice.mode,
                        gear = fragranceDevice.gear,
                        carStartStopEnabled = fragranceDevice.carStartStopEnabled,
                        carStartStopCycle = fragranceDevice.carStartStopCycle,
                        lightMode = fragranceDevice.lightMode,
                        lightColor = fragranceDevice.lightColor,
                        lightBrightness = fragranceDevice.lightBrightness,
                        programVersion = fragranceDevice.programVersion,
                        deviceStatus = fragranceDevice.deviceStatus,
                        timingDuration = fragranceDevice.timingDuration,
                        updatedAt = System.currentTimeMillis()
                    )
                    deviceDao.updateDevice(updatedDevice)
                    if(existingDevice.powerState!=fragranceDevice.powerState){
                        powerStateChangeEventFlow.emit(fragranceDevice)
                    }
                    Timber.tag(TAG).d("设备信息已更新: ${fragranceDevice.macAddress}")
                } else {
                    Timber.tag(TAG).w("设备不存在于数据库中，无法更新: ${fragranceDevice.macAddress}")
                }
            }
        }
    }

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
                is DeviceEvent.Connected -> {
                    val macAddress = bleEvent.device.address
                    updateConnectionState(macAddress, ConnectionState.CONNECTED)
                    deviceEventsFlow.emit(bleEvent)
                }
                is DeviceEvent.Disconnected -> {
                    val macAddress = bleEvent.device.address
                    updateConnectionState(macAddress, ConnectionState.DISCONNECTED)
                    deviceEventsFlow.emit(bleEvent)
                }
                is DeviceEvent.DeviceReady -> {
                    val macAddress = bleEvent.device.address
                    Timber.tag(TAG).d("设备已准备好接收通知: $macAddress")
                    deviceEventsFlow.emit(bleEvent)
                }
                is DeviceEvent.BatteryLevelChanged -> {
                    val macAddress = bleEvent.device.address
                    Timber.tag(TAG).d("设备电量变化: $macAddress, 电量: ${bleEvent.level}")
                    deviceEventsFlow.emit(bleEvent)
                }
                is DeviceEvent.ConnectionFailed -> {
                    val macAddress = bleEvent.device.address
                    updateConnectionState(macAddress, ConnectionState.DISCONNECTED)
                    deviceEventsFlow.emit(bleEvent)
                }
                is DeviceEvent.AuthSuccess -> {
                    deviceEventsFlow.emit(bleEvent)
                }
                is DeviceEvent.AuthFailed -> {
                    deviceEventsFlow.emit(bleEvent)
                }
            }
        }
    }

    init {
        Timber.tag(TAG).i("init")
        // 初始化蓝牙管理器
        bleManager.initialize(context)
    }

    override fun getDevice(macAddress: String): Flow<FragranceDevice?> = deviceDao.getDevice(macAddress)

    override fun getAllDevices(): Flow<List<FragranceDevice>> = deviceDao.getAllDevices()

    override fun getAllDevicesList(): List<FragranceDevice>  = deviceDao.getAllDevicesAsList()

    override suspend fun getAllBluetoothDevice(): List<BluetoothDevice> = bleManager.getBleDevices()
    
    override suspend fun getDeviceByMacAddress(macAddress: String): FragranceDevice? = 
        DatabaseHelper.executeOnIOThread { deviceDao.getDeviceByMac(macAddress) }
    
    override suspend fun addDevice(device: FragranceDevice) {
        DatabaseHelper.executeOnIOThread { deviceDao.insertDevice(device) }
    }
    
    override suspend fun updateDevice(device: FragranceDevice) {
        DatabaseHelper.executeOnIOThread { deviceDao.updateDevice(device) }
    }
    
    override suspend fun deleteDevice(device: FragranceDevice):Int {
        // 删除数据库中的设备信息

        val deleteCount = deviceDao.deleteDevice(device)

        // 取消设备配对
        unpairDevice(device.macAddress)

        return deleteCount
    }

    override suspend fun deleteDevice(address: String): Int {

        val deleteCount = deviceDao.deleteDeviceByMacAddress(address)
        Timber.tag(TAG).i("删除设备 address $deleteCount")
        // 取消设备配对
        unpairDevice(address)

        return deleteCount;
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
            } else {
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
    
    override suspend fun updateDeviceAutoConnect(macAddress: String, needAutoConnect: Boolean) {
        DatabaseHelper.executeOnIOThread {
            deviceDao.updateDeviceAutoConnect(macAddress, needAutoConnect)
        }
    }
    
    override suspend fun renameDevice(macAddress: String, name: String) {
        DatabaseHelper.executeOnIOThread {
            val device = deviceDao.getDeviceByMac(macAddress)
            if (device != null) {
                val updatedDevice = device.copy(displayName = name, updatedAt = System.currentTimeMillis())
                deviceDao.updateDevice(updatedDevice)
            }
        }
    }
    
    override fun startScan() {
        bleManager.startScan()
    }

    override fun stopScan() {
        bleManager.stopScan()
    }

    override fun getScanState(): Flow<ScanState> = bleManager.getScanState()

    override suspend fun connectDevice(bluetoothDevice: BluetoothDevice): Boolean {
        updateConnectionState(bluetoothDevice.address, ConnectionState.CONNECTING)
        // 连接设备
        val success = bleManager.connect(bluetoothDevice)

        // 检查设备是否已存在于数据库
        val existingDevice = DatabaseHelper.executeOnIOThread {
            deviceDao.getDeviceByMac(bluetoothDevice.address)
        }

        if (existingDevice == null) {
            return success
        }

        // 更新连接状态
        if (!success) {
            // 如果连接失败，更新设备状态
            updateConnectionState(bluetoothDevice.address, ConnectionState.DISCONNECTED)
        }
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

        if(btDevice==null){
            try {
                Timber.tag(TAG).i("没有找到配对的蓝牙 $macAddress")
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val bluetoothAdapter = bluetoothManager.adapter
                btDevice = bluetoothAdapter.getRemoteDevice(macAddress)
                Timber.tag(TAG).i("find by bluetoothAdapter: $btDevice")
            }catch (e: Exception){
                Timber.tag(TAG).e(e,"find by bluetoothAdapter: $btDevice")
            }
        }
        // 断开设备连接
        btDevice?.let {             bleManager.disconnect(it) }
    }
    
    override fun getDeviceEvents(): Flow<DeviceEvent> = deviceEventsFlow
    
    // 设备属性更新方法
    override suspend fun setPowerState(macAddress: String, powerState: PowerState) {
        DatabaseHelper.executeOnIOThread {
            val device = deviceDao.getDeviceByMac(macAddress)
            if (device != null) {
                val updatedDevice = device.copy(powerState = powerState, updatedAt = System.currentTimeMillis())
                deviceDao.updateDevice(updatedDevice)
                bleManager.setPowerState(macAddress,powerState)
            }
        }
    }
    
    override suspend fun setMode(macAddress: String, mode: Mode) {
        DatabaseHelper.executeOnIOThread {
            val device = deviceDao.getDeviceByMac(macAddress)
            if (device != null) {
                val updatedDevice = device.copy(mode = mode, updatedAt = System.currentTimeMillis())
                deviceDao.updateDevice(updatedDevice)
            }
        }
    }
    
    override suspend fun setGear(macAddress: String, gear: Gear) {
        DatabaseHelper.executeOnIOThread {
            val device = deviceDao.getDeviceByMac(macAddress)
            if (device != null) {
                val updatedDevice = device.copy(gear = gear, updatedAt = System.currentTimeMillis())
                deviceDao.updateDevice(updatedDevice)
            }
        }
    }
    
    override suspend fun setCarStartStopEnabled(macAddress: String, enabled: Boolean) {
        DatabaseHelper.executeOnIOThread {
            val device = deviceDao.getDeviceByMac(macAddress)
            if (device != null) {
                val updatedDevice = device.copy(carStartStopEnabled = enabled, updatedAt = System.currentTimeMillis())
                deviceDao.updateDevice(updatedDevice)
            }
        }
    }
    
    override suspend fun setCarStartStopCycle(macAddress: String, cycle: CarStartStopCycle) {
        DatabaseHelper.executeOnIOThread {
            val device = deviceDao.getDeviceByMac(macAddress)
            if (device != null) {
                val updatedDevice = device.copy(carStartStopCycle = cycle, updatedAt = System.currentTimeMillis())
                deviceDao.updateDevice(updatedDevice)
            }
        }
    }
    
    override suspend fun setLightMode(macAddress: String, lightMode: LightMode) {
        DatabaseHelper.executeOnIOThread {
            val device = deviceDao.getDeviceByMac(macAddress)
            if (device != null) {
                val updatedDevice = device.copy(lightMode = lightMode, updatedAt = System.currentTimeMillis())
                deviceDao.updateDevice(updatedDevice)
            }
        }
    }
    
    override suspend fun setLightColor(macAddress: String, color: String) {
        DatabaseHelper.executeOnIOThread {
            val device = deviceDao.getDeviceByMac(macAddress)
            if (device != null) {
                val updatedDevice = device.copy(lightColor = color, updatedAt = System.currentTimeMillis())
                deviceDao.updateDevice(updatedDevice)
                bleManager.setLightColor(macAddress,color)
            }
        }
    }

    override suspend fun setLightColor(
        macAddress: String,
        r: Int,
        g: Int,
        b: Int
    ): Boolean {
        // 将 RGB 值转换为 RRGGBB 格式的字符串（数据库存储格式）
        val colorString = String.format("%02X%02X%02X", 
            r.coerceIn(0, 255), 
            g.coerceIn(0, 255), 
            b.coerceIn(0, 255)
        )
        
        // 更新数据库
        DatabaseHelper.executeOnIOThread {
            val device = deviceDao.getDeviceByMac(macAddress)
            if (device != null) {
                val updatedDevice = device.copy(lightColor = colorString, updatedAt = System.currentTimeMillis())
                deviceDao.updateDevice(updatedDevice)
            }
        }
        
        // 调用蓝牙管理器设置颜色
        return bleManager.setLightColor(macAddress, r, g, b)
    }

    override suspend fun setLightColor(
        macAddress: String,
        color: Int
    ): Boolean {
        // 将 Int 颜色值转换为 RGB 分量（格式：0xAARRGGBB）
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        
        // 将 RGB 值转换为 RRGGBB 格式的字符串（数据库存储格式）
        val colorString = String.format("%02X%02X%02X", r, g, b)
        
        // 更新数据库
        DatabaseHelper.executeOnIOThread {
            val device = deviceDao.getDeviceByMac(macAddress)
            if (device != null) {
                val updatedDevice = device.copy(lightColor = colorString, updatedAt = System.currentTimeMillis())
                deviceDao.updateDevice(updatedDevice)
            }
        }
        
        // 调用蓝牙管理器设置颜色
        return bleManager.setLightColor(macAddress, color)
    }

    override suspend fun setLightBrightness(macAddress: String, brightness: Int) {
        DatabaseHelper.executeOnIOThread {
            val device = deviceDao.getDeviceByMac(macAddress)
            if (device != null) {
                val updatedDevice = device.copy(lightBrightness = brightness, updatedAt = System.currentTimeMillis())
                deviceDao.updateDevice(updatedDevice)
            }
        }
    }
    
    override suspend fun setSyncLightBrightness(macAddress: String, sync: Boolean) {
        DatabaseHelper.executeOnIOThread {
            val device = deviceDao.getDeviceByMac(macAddress)
            if (device != null) {
                val updatedDevice = device.copy(syncLightBrightness = sync, updatedAt = System.currentTimeMillis())
                val updateResult = deviceDao.updateDevice(updatedDevice)
                Timber.tag(TAG).i("setSyncLightBrightness $macAddress, sync $sync , updateResult $updateResult")
            }
        }
    }
    
    override suspend fun setTimingDuration(macAddress: String, duration: Int) {
        DatabaseHelper.executeOnIOThread {
            val device = deviceDao.getDeviceByMac(macAddress)
            if (device != null) {
                val updatedDevice = device.copy(timingDuration = duration, updatedAt = System.currentTimeMillis())
                deviceDao.updateDevice(updatedDevice)
            }
        }
    }
    
    override suspend fun setDeviceStatus(macAddress: String, status: Int) {
        DatabaseHelper.executeOnIOThread {
            val device = deviceDao.getDeviceByMac(macAddress)
            if (device != null) {
                val updatedDevice = device.copy(deviceStatus = status, updatedAt = System.currentTimeMillis())
                deviceDao.updateDevice(updatedDevice)
            }
        }
    }
    
    override suspend fun setProgramVersion(macAddress: String, version: Int) {
        DatabaseHelper.executeOnIOThread {
            val device = deviceDao.getDeviceByMac(macAddress)
            if (device != null) {
                val updatedDevice = device.copy(programVersion = version, updatedAt = System.currentTimeMillis())
                deviceDao.updateDevice(updatedDevice)
            }
        }
    }
}

