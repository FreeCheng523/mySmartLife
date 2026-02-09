package com.mine.baselibrary.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

/**
 * 蓝牙连接状态监听广播接收器
 * 用于监听蓝牙设备的连接和断开状态
 * 
 * 权限要求：
 * Android 12 (API 31) 及以上版本：
 * - android.permission.BLUETOOTH_CONNECT - 用于接收连接状态相关广播
 * - android.permission.BLUETOOTH_SCAN - 用于接收扫描相关广播（可选）
 * 
 * Android 11 及以下版本：
 * - android.permission.BLUETOOTH - 基础蓝牙权限
 * - android.permission.BLUETOOTH_ADMIN - 蓝牙管理权限
 * - android.permission.ACCESS_FINE_LOCATION - 位置权限（Android 6-11 需要）
 * 
 * 监听的广播事件：
 * - BluetoothDevice.ACTION_ACL_CONNECTED - ACL连接事件
 * - BluetoothDevice.ACTION_ACL_DISCONNECTED - ACL断开事件
 * - BluetoothDevice.ACTION_BOND_STATE_CHANGED - 绑定状态变化事件
 * - BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED - 连接状态变化事件
 * - BluetoothAdapter.ACTION_STATE_CHANGED - 适配器状态变化事件
 */
class BluetoothConnectionBroadcastReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BluetoothConnectionBR"
        
        // 可监听的广播事件类型
        enum class BroadcastEventType {
            ACL_CONNECTED,           // ACL连接事件
            ACL_DISCONNECTED,        // ACL断开事件
            BOND_STATE_CHANGED,      // 绑定状态变化事件
            CONNECTION_STATE_CHANGED, // 连接状态变化事件
            ADAPTER_STATE_CHANGED    // 适配器状态变化事件
        }
    }
    
    /**
     * 蓝牙事件监听器接口
     */
    fun interface OnBluetoothEventListener {
        /**
         * 当收到蓝牙事件时回调
         * @param intent 广播 Intent
         */
        fun onBluetoothEvent(intent: Intent)
    }
    
    // 监听配置
    private var enabledEvents = mutableSetOf<BroadcastEventType>()
    
    // 事件监听器列表（线程安全）
    private val listeners = mutableSetOf<OnBluetoothEventListener>()
    
    // 已注册标志
    private var isRegistered = false
    
    /**
     * 配置要监听的广播事件类型
     * @param events 要监听的事件类型集合，如果为空则监听所有事件
     */
    fun configureEvents(events: Set<BroadcastEventType> = emptySet()) {
        enabledEvents.clear()
        if (events.isEmpty()) {
            // 如果未指定事件，默认监听所有事件
            enabledEvents.addAll(BroadcastEventType.values())
            Log.d(TAG, "配置为监听所有蓝牙事件")
        } else {
            enabledEvents.addAll(events)
            Log.d(TAG, "配置监听事件: ${events.joinToString(", ")}")
        }
    }
    
    /**
     * 启用特定事件监听
     * @param event 要启用的事件类型
     */
    fun enableEvent(event: BroadcastEventType) {
        enabledEvents.add(event)
        Log.d(TAG, "启用事件监听: $event")
    }
    
    /**
     * 禁用特定事件监听
     * @param event 要禁用的事件类型
     */
    fun disableEvent(event: BroadcastEventType) {
        enabledEvents.remove(event)
        Log.d(TAG, "禁用事件监听: $event")
    }
    
    /**
     * 检查事件是否已启用
     * @param event 要检查的事件类型
     * @return 是否已启用
     */
    fun isEventEnabled(event: BroadcastEventType): Boolean {
        return enabledEvents.contains(event)
    }
    
    /**
     * 获取当前启用的所有事件
     * @return 已启用的事件集合
     */
    fun getEnabledEvents(): Set<BroadcastEventType> {
        return enabledEvents.toSet()
    }
    
    /**
     * 添加事件监听器
     * @param listener 监听器
     */
    @Synchronized
    fun addListener(listener: OnBluetoothEventListener) {
        listeners.add(listener)
        Log.d(TAG, "添加监听器，当前监听器数量: ${listeners.size}")
    }
    
    /**
     * 移除事件监听器
     * @param listener 监听器
     */
    @Synchronized
    fun removeListener(listener: OnBluetoothEventListener) {
        listeners.remove(listener)
        Log.d(TAG, "移除监听器，当前监听器数量: ${listeners.size}")
    }
    
    /**
     * 清除所有监听器
     */
    @Synchronized
    fun clearListeners() {
        listeners.clear()
        Log.d(TAG, "清除所有监听器")
    }
    
    /**
     * 获取当前监听器数量
     * @return 监听器数量
     */
    @Synchronized
    fun getListenerCount(): Int {
        return listeners.size
    }
    
    /**
     * 注册广播接收器
     * @param context 上下文
     */
    fun register(context: Context) {
        if (!isRegistered) {
            // 如果未配置事件，默认监听所有事件
            if (enabledEvents.isEmpty()) {
                configureEvents()
            }
            
            val filter = IntentFilter().apply {
                // 根据配置动态添加要监听的广播事件
                if (enabledEvents.contains(BroadcastEventType.ACL_CONNECTED)) {
                    addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                }
                if (enabledEvents.contains(BroadcastEventType.ACL_DISCONNECTED)) {
                    addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                }
                if (enabledEvents.contains(BroadcastEventType.BOND_STATE_CHANGED)) {
                    addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                }
                if (enabledEvents.contains(BroadcastEventType.CONNECTION_STATE_CHANGED)) {
                    addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                }
                if (enabledEvents.contains(BroadcastEventType.ADAPTER_STATE_CHANGED)) {
                    addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                }
            }
            context.registerReceiver(this, filter)
            isRegistered = true
            Log.d(TAG, "蓝牙连接状态广播接收器已注册，监听事件: ${enabledEvents.joinToString(", ")}")
        }
    }
    
    /**
     * 注销广播接收器
     * @param context 上下文
     */
    fun unregister(context: Context) {
        if (isRegistered) {
            try {
                context.unregisterReceiver(this)
                isRegistered = false
                Log.d(TAG, "蓝牙连接状态广播接收器已注销")
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "注销广播接收器时发生异常: ${e.message}")
            }
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                if (enabledEvents.contains(BroadcastEventType.ACL_CONNECTED)) {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        Log.d(TAG, "蓝牙设备已连接: ${it.name ?: "未知设备"} (${it.address})")
                    }
                    notifyListeners(intent)
                }
            }
            
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                if (enabledEvents.contains(BroadcastEventType.ACL_DISCONNECTED)) {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        Log.d(TAG, "蓝牙设备已断开: ${it.name ?: "未知设备"} (${it.address})")
                    }
                    notifyListeners(intent)
                }
            }
            
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                if (enabledEvents.contains(BroadcastEventType.BOND_STATE_CHANGED)) {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                    device?.let {
                        Log.d(TAG, "蓝牙设备绑定状态变化: ${it.name ?: "未知设备"} (${it.address}) - 状态: $bondState")
                    }
                    notifyListeners(intent)
                }
            }
            
            BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
                if (enabledEvents.contains(BroadcastEventType.CONNECTION_STATE_CHANGED)) {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.STATE_DISCONNECTED)
                    device?.let {
                        Log.d(TAG, "蓝牙适配器连接状态变化: ${it.name ?: "未知设备"} (${it.address}) - 状态: $state")
                    }
                    notifyListeners(intent)
                }
            }
            
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                if (enabledEvents.contains(BroadcastEventType.ADAPTER_STATE_CHANGED)) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
                    Log.d(TAG, "蓝牙适配器状态变化: $state")
                    
                    when (state) {
                        BluetoothAdapter.STATE_ON -> {
                            Log.d(TAG, "蓝牙已开启")
                        }
                        BluetoothAdapter.STATE_OFF -> {
                            Log.d(TAG, "蓝牙已关闭")
                        }
                        BluetoothAdapter.STATE_TURNING_ON -> {
                            Log.d(TAG, "蓝牙正在开启")
                        }
                        BluetoothAdapter.STATE_TURNING_OFF -> {
                            Log.d(TAG, "蓝牙正在关闭")
                        }
                    }
                    
                    notifyListeners(intent)
                }
            }
        }
    }
    
    /**
     * 通知所有监听器
     * @param intent 广播 Intent
     */
    @Synchronized
    private fun notifyListeners(intent: Intent) {
        if (listeners.isEmpty()) {
            Log.w(TAG, "没有监听器，事件: ${intent.action}")
            return
        }
        
        // 复制列表，避免在回调中修改导致 ConcurrentModificationException
        val listenersCopy = listeners.toList()
        
        listenersCopy.forEach { listener ->
            try {
                listener.onBluetoothEvent(intent)
            } catch (e: Exception) {
                Log.e(TAG, "监听器回调异常: ${intent.action}", e)
            }
        }
        
        Log.d(TAG, "已通知 ${listenersCopy.size} 个监听器: ${intent.action}")
    }
}
