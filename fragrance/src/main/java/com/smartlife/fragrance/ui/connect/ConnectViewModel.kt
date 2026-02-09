package com.smartlife.fragrance.ui.connect

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mine.baselibrary.bluetooth.DeviceEvent
import com.smartlife.fragrance.repository.FragranceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import timber.log.Timber

/**
 * 配对步骤枚举
 */
enum class PairStepOnlyConnect {
    STEP_ONE_CHECK_BLUETOOTH, // 第一步：检查蓝牙权限和开启状态
    STEP_SECOND_PAIRED         // 第二步：配对完成
}

/**
 * 处理配对和连接
 */
@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val fragranceRepository: FragranceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        const val TAG = "PairingAndConnect"
    }

    /**
     * 单线程执行器，确保事件处理不会并发
     */
    private val singleThreadExecutor = Executors.newSingleThreadExecutor()
    
    /**
     * 单线程调度器
     */
    private val singleThreadDispatcher: CoroutineDispatcher = 
        singleThreadExecutor.asCoroutineDispatcher()

    private val _connectionStatus = MutableSharedFlow<ConnectionState>()
    val connectionStatus: SharedFlow<ConnectionState> = _connectionStatus

    // 蓝牙是否可用
    private val _isBluetoothEnabled = MutableStateFlow<Boolean?>(null)
    val isBluetoothEnabled: StateFlow<Boolean?> = _isBluetoothEnabled.asStateFlow()

    // 当前正在配对的设备
    private val _connectingDevice = MutableStateFlow<BluetoothDevice?>(null)
    val pairingDevice: StateFlow<BluetoothDevice?> = _connectingDevice.asStateFlow()

    // 是否正在执行连接操作（线程安全）
    private val isConnecting = AtomicBoolean(false)

    /**
     * 重置状态量
     */
    private fun reset() {
        isConnecting.set(false)
        _isBluetoothEnabled.value = null
        _connectingDevice.value = null
    }

    init {
        viewModelScope.launch {
            // 等待设备完全就绪的事件，只有启用通知成功，香氛设备才真正可用
            // 用于跟踪上一个事件，防止处理连续相同的事件
            fragranceRepository.getDeviceEvents()
                .flowOn(singleThreadDispatcher)
                .map { event ->

                    if (!isConnecting.get()) {
                        Timber.tag(TAG).e("not connecting")
                        return@map null
                    }

                    when (event) {
                        is DeviceEvent.ConnectionFailed -> {
                            val macAddress = event.device.address
                            if (macAddress == _connectingDevice.value?.address) {
                                Timber.tag(TAG).e("连接失败: $macAddress ${event.reason}")
                                return@map ConnectionState.ConnectionFailed(
                                    macAddress = macAddress,
                                    reason = event.reason
                                )
                            }
                        }

                        is DeviceEvent.Connected -> {
                            val macAddress = event.device.address
                            if (macAddress == _connectingDevice.value?.address) {
                                Timber.tag(TAG).d("设备已连接: ${_connectingDevice.value?.address}")
                                return@map ConnectionState.DeviceConnected(
                                    macAddress = macAddress
                                )
                            }
                        }

                        is DeviceEvent.AuthSuccess -> {
                            val macAddress = event.device.address
                            if (macAddress == _connectingDevice.value?.address) {
                                Timber.tag(TAG).e("鉴权成功: $macAddress")
                                return@map ConnectionState.AuthSuccess(
                                    macAddress = macAddress
                                )
                            }
                        }

                        is DeviceEvent.AuthFailed -> {
                            val macAddress = event.device.address
                            if (macAddress == _connectingDevice.value?.address) {
                                Timber.tag(TAG).e("鉴权失败: $macAddress ${event.reason}")
                                return@map ConnectionState.AuthFailed(
                                    macAddress = macAddress,
                                    reason = event.reason
                                )
                            }
                        }

                        else -> {
                            Timber.tag(TAG).d("收到其他事件，线程: ${Thread.currentThread().name}")
                            return@map null
                        }
                    }
                    return@map null
                }.filter { status ->
                    status != null
                }.collect { status ->

                    when (status) {
                        is ConnectionState.DeviceConnected -> {
                            // 连接成功后的处理
                            // 注意：fragrance 模块没有 setAutoConnected 方法，如果需要可以添加
                            //加这句话会断开连接
                            //createBond()
                            _connectionStatus.emit(status)
                        }
                        is ConnectionState.AuthSuccess,
                        is ConnectionState.AuthFailed,
                        is ConnectionState.ConnectionFailed -> {
                            _connectionStatus.emit(status)
                            reset()
                        }
                        else -> {
                            Timber.tag(TAG).e("收到未知的status $status")
                        }
                    }
                }
        }
    }

    /**
     * 检查蓝牙状态
     */
    private suspend fun checkBluetoothStatus(): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val enable = bluetoothAdapter?.isEnabled == true
        _isBluetoothEnabled.emit(enable)
        return enable
    }

    fun connectDevice(macAddress: String) {
        reset()
        viewModelScope.launch {
            if (!checkBluetoothStatus()) {
                return@launch
            }
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            val device = bluetoothAdapter.getRemoteDevice(macAddress)
            connectToDevice(device)
        }
    }

    /**
     * 连接到设备并检查服务
     */
    suspend fun connectToDevice(device: BluetoothDevice) {
        try {
            isConnecting.set(true)
            val connected = fragranceRepository.connectDevice(device)
            _connectingDevice.value = device
            if (!connected) {
                // 连接失败
                isConnecting.set(false)
                _connectingDevice.value = null
                _connectionStatus.emit(ConnectionState.ConnectionFailed(
                    macAddress = device.address,
                    reason = "连接设备失败"
                ))
                Timber.tag(TAG).d("连接设备失败")
            }
        } catch (e: Exception) {
            isConnecting.set(false)
            _connectionStatus.emit(ConnectionState.ConnectionFailed(
                macAddress = device.address,
                reason = e.message ?: "连接过程中发生错误"
            ))
            Timber.tag(TAG).d("连接过程中发生错误")
            _connectingDevice.value = null
            // 重新开启蓝牙扫描模式
        }
    }

    /**
     * 这是之前的逻辑，保留
     */
    fun createBond() {
        viewModelScope.launch {
            _connectingDevice.value?.let {
                // 如果设备未配对，则进行配对
                // 不再配对，好像多个同名设备地址不同配对会产生问题
                if (it.bondState != BluetoothDevice.BOND_BONDED) {
                    it.createBond()
                }
            }
        }
    }

    /**
     * 连接状态密封类
     * @param macAddress 设备MAC地址
     * @param reason 原因说明
     */
    sealed class ConnectionState(open val macAddress: String, open val reason: String) {
        data class DeviceConnected(override val macAddress: String, override val reason: String = "") : ConnectionState(macAddress, reason)
        data class ConnectionFailed(override val macAddress: String, override val reason: String) : ConnectionState(macAddress, reason)
        data class AuthFailed(override val macAddress: String, override val reason: String) : ConnectionState(macAddress, reason)
        data class AuthSuccess(override val macAddress: String, override val reason: String = "") : ConnectionState(macAddress, reason)
    }

    /**
     * 清理资源
     */
    override fun onCleared() {
        super.onCleared()
        // 关闭单线程执行器，避免线程泄漏
        singleThreadExecutor.shutdown()
    }

    suspend fun disconnected(macAddress: String) {
        fragranceRepository.disconnectDevice(macAddress)
    }
}

