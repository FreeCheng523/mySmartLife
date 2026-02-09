package com.zkjd.lingdong.repository

import com.zkjd.lingdong.model.ButtonType

/**
 * 设备事件，用于通知UI层设备状态变化
 */
sealed class DeviceEvent {
    // 连接事件
    data class DeviceConnected(val macAddress: String) : DeviceEvent()
    data class DeviceDisconnected(val macAddress: String) : DeviceEvent()
    data class ConnectionFailed(val macAddress: String, val reason: String) : DeviceEvent()
    data class DeviceReady(val macAddress: String) : DeviceEvent()
    
    // 按键事件
    data class ButtonPressed(val macAddress: String, val buttonType: ButtonType) : DeviceEvent()
    
    // 电池电量事件
    data class BatteryLevelChanged(val macAddress: String, val level: Int) : DeviceEvent()

    data class AuthSuccess(val macAddress: String) : DeviceEvent()
    data class AuthFailed(val macAddress: String, val reason: String) : DeviceEvent()
    data class SetFunction(val macAddress: String,val keyString: String) : DeviceEvent() //车机反馈
} 