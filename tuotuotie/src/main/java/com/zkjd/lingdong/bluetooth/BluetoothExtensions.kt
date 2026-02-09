package com.zkjd.lingdong.bluetooth

import android.bluetooth.BluetoothDevice
import java.util.concurrent.ConcurrentHashMap

/**
 * 存储设备RSSI值的映射
 */
private val deviceRssiMap = ConcurrentHashMap<String, Int>()

/**
 * 设置设备的RSSI值
 */
fun BluetoothDevice.setRssiValue(rssi: Int) {
    deviceRssiMap[address] = rssi
}

/**
 * 获取设备的RSSI值
 */
fun BluetoothDevice.getRssiValue(): Int {
    return deviceRssiMap[address] ?: -100
}

/**
 * 判断设备是否在指定的RSSI范围内（距离范围内）
 */
fun BluetoothDevice.isWithinRange(minRssi: Int): Boolean {
    return getRssiValue() >= minRssi
}

/**
 * 清除设备RSSI值
 */
fun clearAllDeviceRssi() {
    deviceRssiMap.clear()
} 