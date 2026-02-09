package com.zkjd.lingdong.repository

import android.bluetooth.BluetoothDevice
import com.mine.baselibrary.bluetooth.ScanState
import com.zkjd.lingdong.model.ButtonFunction
import com.zkjd.lingdong.model.ButtonType
import com.zkjd.lingdong.model.ConnectionState
import com.zkjd.lingdong.model.Device
import kotlinx.coroutines.flow.Flow

/**
 * 设备仓库接口
 */
interface DeviceRepository {

    //获取特定的设备
    fun getDevice(macAddress: String): Flow<Device?>

    // 获取所有已配对设备
    fun getAllDevices(): Flow<List<Device>>
    
    // 获取特定设备
    suspend fun getDeviceByMacAddress(macAddress: String): Device?
    
    // 添加新设备
    suspend fun addDevice(device: Device)
    
    // 更新设备信息
    suspend fun updateDevice(device: Device)


    suspend fun deleteDevice(address: String,result:(success: Boolean)-> Unit):Int
    
    // 取消设备配对
    suspend fun unpairDevice(macAddress: String): Boolean
    
    // 更新设备连接状态
    suspend fun updateConnectionState(macAddress: String, state: ConnectionState)
    
    // 更新设备电量
    suspend fun updateBatteryLevel(macAddress: String, level: Int)
    
    // 修改设备名称
    suspend fun renameDevice(macAddress: String, name: String)
    
    // 设置设备LED颜色
    suspend fun setLedColor(macAddress: String, isConnectedMode: Boolean, colorInt: Int)
    
    // 设置设备防误触模式
    suspend fun setAntiMisoperation(macAddress: String, enabled: Boolean)

    // 设置设备音效开关
    suspend fun setMusicCan(macAddress: String, enabled: Boolean)

    // 修改设备音效名称
    suspend fun renameMusicID(macAddress: String, id: String)
    // 获取设备的按键功能
    suspend fun getButtonFunction(macAddress: String, buttonType: ButtonType): ButtonFunction?
    
    // 设置设备的按键功能
    suspend fun setButtonFunction(macAddress: String, buttonType: ButtonType, function: ButtonFunction)
    
    // 开始蓝牙扫描
    fun startScan()

    // 停止蓝牙扫描
    fun stopScan()

    // 获取扫描状态
    fun getScanState(): Flow<ScanState>
    
    // 连接蓝牙设备
    suspend fun connectDevice(bluetoothDevice: BluetoothDevice): Boolean
    
    // 断开蓝牙设备连接
    suspend fun disconnectDevice(macAddress: String)
    
    // 获取设备事件
    fun getDeviceEvents(): Flow<DeviceEvent>
    
    // 设置蓝牙设备的LED颜色
    suspend fun setDeviceLedColor(macAddress: String, isConnectedState: Boolean)
    
    // 设置蓝牙设备的防误触模式
    suspend fun setDeviceAntiMisoperation(macAddress: String)
    
    // 清除设备的按键功能
    suspend fun clearButtonFunction(macAddress: String, buttonType: ButtonType)
    
    // 刷新设备数据
    suspend fun refreshDevices()

    // 下发设置颜色
    suspend fun setColor(macAddress: String, isConnectedState: Boolean, colors: Int)

    // 存储防误触状态
    suspend fun setPreventAccidental(macAddress: String, colorInt: Int)


    suspend fun setAutoConnected(macAddress: String, needAutoConnect: Boolean)

    // 下发防误触
    suspend fun setPreventAccid(macAddress: String, pA: Int)
    // 存储返控状态
    suspend fun setReturnControl(macAddress: String, rC: Int)

    suspend fun getAllBluetoothDevice(): List<BluetoothDevice>

    suspend fun setIsAppOpenOne(macAddress: String, enabled: Boolean)
    suspend fun setIsAppOpenTwo(macAddress: String, enabled: Boolean)
    fun getAllDevicesAsList(): List<Device>
}