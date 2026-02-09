package com.smartlife.fragrance.repository

import android.bluetooth.BluetoothDevice
import com.mine.baselibrary.bluetooth.DeviceEvent
import com.mine.baselibrary.bluetooth.ScanState
import com.smartlife.fragrance.data.model.ConnectionState
import com.smartlife.fragrance.data.model.CarStartStopCycle
import com.smartlife.fragrance.data.model.FragranceDevice
import com.smartlife.fragrance.data.model.Gear
import com.smartlife.fragrance.data.model.LightMode
import com.smartlife.fragrance.data.model.Mode
import com.smartlife.fragrance.data.model.PowerState
import kotlinx.coroutines.flow.Flow

/**
 * 香氛设备仓库接口
 */
interface FragranceRepository {

    // 获取特定的设备
    fun getDevice(macAddress: String): Flow<FragranceDevice?>

    // 获取所有已配对设备
    fun getAllDevices(): Flow<List<FragranceDevice>>

    fun getAllDevicesList(): List<FragranceDevice>
    
    // 获取特定设备
    suspend fun getDeviceByMacAddress(macAddress: String): FragranceDevice?
    
    // 添加新设备
    suspend fun addDevice(device: FragranceDevice)
    
    // 更新设备信息
    suspend fun updateDevice(device: FragranceDevice)
    
    // 删除设备
    suspend fun deleteDevice(device: FragranceDevice):Int

    suspend fun deleteDevice(address: String):Int
    
    // 取消设备配对
    suspend fun unpairDevice(macAddress: String): Boolean
    
    // 更新设备连接状态
    suspend fun updateConnectionState(macAddress: String, state: ConnectionState)
    
    // 更新设备自动连接状态
    suspend fun updateDeviceAutoConnect(macAddress: String, needAutoConnect: Boolean)
    
    // 修改设备名称
    suspend fun renameDevice(macAddress: String, name: String)
    
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
    
    // 获取设备事件（使用基类 DeviceEvent）
    fun getDeviceEvents(): Flow<DeviceEvent>
    
    // 获取所有蓝牙设备
    suspend fun getAllBluetoothDevice(): List<BluetoothDevice>
    
    // 设备属性更新方法
    suspend fun setPowerState(macAddress: String, powerState: PowerState)
    
    suspend fun setMode(macAddress: String, mode: Mode)
    
    suspend fun setGear(macAddress: String, gear: Gear)
    
    suspend fun setCarStartStopEnabled(macAddress: String, enabled: Boolean)
    
    suspend fun setCarStartStopCycle(macAddress: String, cycle: CarStartStopCycle)
    
    suspend fun setLightMode(macAddress: String, lightMode: LightMode)
    
    suspend fun setLightColor(macAddress: String, color: String)

    abstract suspend fun setLightColor(macAddress: String, r: Int, g: Int, b: Int): Boolean

    abstract suspend fun setLightColor(macAddress: String, color: Int): Boolean
    
    suspend fun setLightBrightness(macAddress: String, brightness: Int)
    
    suspend fun setSyncLightBrightness(macAddress: String, sync: Boolean)
    
    suspend fun setTimingDuration(macAddress: String, duration: Int)
    
    suspend fun setDeviceStatus(macAddress: String, status: Int)
    
    suspend fun setProgramVersion(macAddress: String, version: Int)

}

