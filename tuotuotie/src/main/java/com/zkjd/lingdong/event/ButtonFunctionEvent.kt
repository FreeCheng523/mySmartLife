package com.zkjd.lingdong.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 按键功能事件管理器
 */
@Singleton
class ButtonFunctionEvent @Inject constructor() {
    
    private val _functionChangedEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val functionChangedEvent: SharedFlow<String> = _functionChangedEvent.asSharedFlow()
    
    /**
     * 发送按键功能变更事件
     * @param deviceMacAddress 设备MAC地址
     */
    suspend fun sendFunctionChangedEvent(deviceMacAddress: String) {
        _functionChangedEvent.emit(deviceMacAddress)
    }
} 