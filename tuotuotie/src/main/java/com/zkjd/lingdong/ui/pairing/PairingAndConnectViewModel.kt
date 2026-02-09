package com.zkjd.lingdong.ui.pairing

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zkjd.lingdong.repository.DeviceEvent
import com.zkjd.lingdong.repository.DeviceRepository
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
class PairingAndConnectViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object{
        const val TAG = "PairingAndConnectViewModel"
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
    private val _pairingDevice = MutableStateFlow<BluetoothDevice?>(null)
    val pairingDevice: StateFlow<BluetoothDevice?> = _pairingDevice.asStateFlow()

    // 是否正在执行连接操作（线程安全）
    private val isConnecting = AtomicBoolean(false)


    /**
     * 重置状态量
     */
    private fun reset(){
        Timber.tag(TAG).i("重置连接状态")
        isConnecting.set(false)
        _isBluetoothEnabled.value = null
        _pairingDevice.value = null
    }


    init {
        Timber.tag(TAG).i("初始化 PairingAndConnectViewModel，开始监听设备事件")
        viewModelScope.launch {
            // 等待设备完全就绪的事件，只有启用通知成功，妥妥帖才真正可用
            // 用于跟踪上一个事件，防止处理连续相同的事件
            deviceRepository.getDeviceEvents()
                .flowOn(singleThreadDispatcher)
                .map { event ->

                    if(!isConnecting.get()){
                        Timber.tag(TAG).e("not connecting")
                        return@map null
                    }

                    when (event) {
                        is DeviceEvent.ConnectionFailed -> {
                            if (event.macAddress == _pairingDevice.value?.address) {
                                Timber.tag(TAG).e("连接失败: ${event.macAddress} ${event.reason}")
                                return@map ConnectionState.ConnectionFailed(
                                    macAddress = event.macAddress,
                                    reason = event.reason
                                )
                            }
                        }

                        is DeviceEvent.DeviceConnected -> {
                            if (event.macAddress == _pairingDevice.value?.address) {
                                Timber.tag(TAG).i("设备已连接: ${_pairingDevice.value?.address}")
                                return@map ConnectionState.DeviceConnected(
                                    macAddress = event.macAddress
                                )
                            }
                        }

                        is DeviceEvent.AuthSuccess -> {
                            if (event.macAddress == _pairingDevice.value?.address) {
                                Timber.tag(TAG).i("鉴权成功: ${event.macAddress}")
                                return@map ConnectionState.AuthSuccess(
                                    macAddress = event.macAddress
                                )
                            }
                        }

                        is DeviceEvent.AuthFailed -> {
                            if (event.macAddress == _pairingDevice.value?.address) {
                                Timber.tag(TAG).e("鉴权失败: ${event.macAddress} ${event.reason}")
                                return@map ConnectionState.AuthFailed(
                                    macAddress = event.macAddress,
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
                }.filter { status->
                    status!=null
                } .collect { status ->

                    when (status){
                        is ConnectionState.DeviceConnected ->{
                            //只要设置连接成功，就设置为自动连接
                            Timber.tag(TAG).i("设备连接成功，设置自动连接: ${status.macAddress}")
                            setAutoConnected(status.macAddress,true)
                            createBond()
                            _connectionStatus.emit(status)
                        }
                        is ConnectionState.AuthSuccess,
                        is ConnectionState.AuthFailed,
                        is ConnectionState.ConnectionFailed -> {
                            _connectionStatus.emit(status)
                            reset()
                        }
                        else->{
                            Timber.tag(TAG).e("收到未知的status $status")
                        }
                    }

                }
        }
    }

    private suspend fun setAutoConnected(macAddress: String, needAutoConnected: Boolean) {
        deviceRepository.setAutoConnected(macAddress, needAutoConnected)
    }


    /**
     * 检查蓝牙状态
     */
    private suspend fun checkBluetoothStatus():Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val enable = bluetoothAdapter?.isEnabled == true
        Timber.tag(TAG).i("检查蓝牙状态: ${if (enable) "已启用" else "未启用"}")
        _isBluetoothEnabled.emit(enable)
        return enable
    }


    fun connectDevice(macAddress: String){
        Timber.tag(TAG).i("开始连接设备: $macAddress")
        reset()
        viewModelScope.launch {
            if(!checkBluetoothStatus()){
                Timber.tag(TAG).i("蓝牙未启用，取消连接: $macAddress")
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
            Timber.tag(TAG).i("正在连接设备: ${device.address}, 名称: ${device.name}")
            isConnecting.set(true)
            val connected = deviceRepository.connectDevice(device)
            _pairingDevice.value = device
            if (!connected) {
                // 连接失败
                Timber.tag(TAG).i("连接设备失败: ${device.address}")
                isConnecting.set(false)
                _pairingDevice.value = null
                _connectionStatus.emit(ConnectionState.ConnectionFailed(
                    macAddress = device.address,
                    reason = "连接设备失败"
                ))
                Timber.tag(TAG).d("连接设备失败")
            } else {
                Timber.tag(TAG).i("设备连接请求已发送: ${device.address}")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).i("连接过程中发生异常: ${device.address}, 错误: ${e.message}")
            isConnecting.set(false)
            _connectionStatus.emit(ConnectionState.ConnectionFailed(
                macAddress = device.address,
                reason = e.message ?: "连接过程中发生错误"
            ))
            Timber.tag(TAG).d("连接过程中发生错误")
            _pairingDevice.value = null
            // 重新开启蓝牙扫描模式
        }
    }

    /**
     *这是之前的逻辑，保留
     */
    fun createBond(){
        viewModelScope.launch {
            _pairingDevice.value?.let {
                // 如果设备未配对，则进行配对
                //不再配对，好像多个同名设备地址不同配对会产生问题
                val bondState = when(it.bondState) {
                    BluetoothDevice.BOND_BONDED -> "已配对"
                    BluetoothDevice.BOND_BONDING -> "配对中"
                    else -> "未配对"
                }
                Timber.tag(TAG).i("设备配对状态: ${it.address}, 状态: $bondState")
                if (it.bondState != BluetoothDevice.BOND_BONDED) {
                    Timber.tag(TAG).i("开始配对设备: ${it.address}")
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


    suspend fun disconnect(macAddress:String){
        //断开设备时不自动连接
        setAutoConnected(macAddress, false)
        deviceRepository.disconnectDevice(macAddress)
    }
    fun disconnectDevice(macAddress: String) {
        viewModelScope.launch {
            disconnect(macAddress)
        }
    }
    /**
     * 清理资源
     */
    override fun onCleared() {
        super.onCleared()
        Timber.tag(TAG).i("清理资源，关闭单线程执行器")
        // 关闭单线程执行器，避免线程泄漏
        singleThreadExecutor.shutdown()
    }

} 