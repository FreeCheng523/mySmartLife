package com.mine.baselibrary.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

/**
 * 设备状态事件
 */
sealed class DeviceEvent {
    data class Connected(val device: BluetoothDevice) : DeviceEvent()
    data class Disconnected(val device: BluetoothDevice) : DeviceEvent()
    data class BatteryLevelChanged(val device: BluetoothDevice, val level: Int) : DeviceEvent()
    data class ConnectionFailed(val device: BluetoothDevice, val reason: String) : DeviceEvent()
    data class DeviceReady(val device: BluetoothDevice) : DeviceEvent() // 设备完全准备好（通知已启用）
    data class AuthSuccess(val device: BluetoothDevice) : DeviceEvent() // 设备鉴权成功
    data class AuthFailed(val device: BluetoothDevice, val reason: String) : DeviceEvent() // 设备鉴权失败
}

/**
 * 断开连接请求事件
 */
data class DisconnectRequestEvent(
    val deviceAddress: String,
    val reason: String
)

/**
 * 蓝牙扫描状态
 */
sealed class ScanState {
    object NotScanning : ScanState()
    object Scanning : ScanState()
    data class ScanResult(val devices: List<BluetoothDevice>) : ScanState()
    data class ScanFailed(val reason: String) : ScanState()
}

/**
 * 蓝牙管理接口
 */
interface IBleManager {
    /**
     * 初始化蓝牙管理器
     */
    fun initialize(context: Context)

    /**
     * 开始扫描蓝牙设备
     */
    fun startScan()

    /**
     * 停止扫描蓝牙设备
     */
    fun stopScan()

    /**
     * 获取扫描状态Flow
     */
    fun getScanState(): Flow<ScanState>

    /**
     * 连接设备
     */
    suspend fun connect(device: BluetoothDevice): Boolean

    /**
     * 断开设备连接
     */
    suspend fun disconnect(device: BluetoothDevice): Boolean

    /**
     * 获取设备事件Flow
     */
    fun getDeviceEvents(): SharedFlow<DeviceEvent>

    /**
     * 获取断开连接请求事件Flow
     */
    fun getDisconnectRequestEvents(): SharedFlow<DisconnectRequestEvent>


    /**
     * 关闭蓝牙管理器
     */
    fun close()


    suspend fun getBleDevices(): List<BluetoothDevice>

}