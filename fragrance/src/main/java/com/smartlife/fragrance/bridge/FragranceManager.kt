package com.smartlife.fragrance.bridge

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.mine.baselibrary.bluetooth.ScanState
import com.mine.baselibrary.constants.VehicleTypeConstants
import com.smartlife.fragrance.data.model.FragranceDevice
import com.smartlife.fragrance.data.model.PowerState
import com.smartlife.fragrance.ui.connect.ConnectViewModel
import com.smartlife.fragrance.ui.scanning.FragranceScanningViewModel
import com.smartlife.fragrance.ui.status.DeviceStatusViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * 香薰设备管理器
 * 负责统一管理香薰设备的扫描、连接、状态监听等功能
 * 
 * 注意：每个 Activity/Fragment 应该创建自己的实例，生命周期与 Activity/Fragment 绑定
 *
 * 使用示例：
 * ```
 * private var fragranceManager: FragranceManager? = null
 * 
 * // 在 onCreate 中初始化
 * fragranceManager = FragranceManager.Builder()
 *     .setLifecycle(lifecycle)
 *     .setScanningViewModel(scanningViewModel)
 *     .setConnectViewModel(connectViewModel)
 *     .setDeviceStatusViewModel(deviceStatusViewModel)
 *     .setContext(context)
 *     .setOnDeviceAddedCallback { devices -> handleDevices(devices) }
 *     .setConnectedResultCallback { success, mac -> handleResult(success, mac) }
 *     .setNeedOpenBluetoothCallback { openBluetoothSettings() }
 *     .build()
 * 
 * // 使用
 * fragranceManager?.startScan()
 * ```
 */
class FragranceManager private constructor(
    private val lifecycle: Lifecycle,
    private val fragranceScanningViewModel: FragranceScanningViewModel,
    private val onDeviceAdded: ((List<FragranceDevice>) -> Unit)?,
    private val connectViewModel: ConnectViewModel,
    private val connectedResult: ((success: Boolean, macAddress: String) -> Unit)?,
    private val deviceStatusViewModel: DeviceStatusViewModel,
    private val onDeviceChange:((List<FragranceDevice>) -> Unit)?,
    private val context: Context,
    private val needOpenBluetooth: (() -> Unit)?
) {
    companion object {
        const val TAG = "FragranceManager"
    }
    
    /**
     * Builder 类用于构建 FragranceManager 实例
     */
    class Builder {
        private var lifecycle: Lifecycle? = null
        private var fragranceScanningViewModel: FragranceScanningViewModel? = null
        private var onDeviceAdded: ((List<FragranceDevice>) -> Unit)? = null
        private var connectViewModel: ConnectViewModel? = null
        private var connectedResult: ((success: Boolean, macAddress: String) -> Unit)? = null
        private var deviceStatusViewModel: DeviceStatusViewModel? = null
        private var onDeviceChange:((List<FragranceDevice>) -> Unit)?=null
        private var context: Context? = null
        private var needOpenBluetooth: (() -> Unit)? = null
        
        fun setLifecycle(lifecycle: Lifecycle) = apply {
            this.lifecycle = lifecycle
        }
        
        fun setScanningViewModel(viewModel: FragranceScanningViewModel) = apply {
            this.fragranceScanningViewModel = viewModel
        }
        
        fun setOnDeviceAddedCallback(callback: (List<FragranceDevice>) -> Unit) = apply {
            this.onDeviceAdded = callback
        }
        
        fun setConnectViewModel(viewModel: ConnectViewModel) = apply {
            this.connectViewModel = viewModel
        }
        
        fun setConnectedResultCallback(callback: (success: Boolean, macAddress: String) -> Unit) = apply {
            this.connectedResult = callback
        }
        
        fun setDeviceStatusViewModel(viewModel: DeviceStatusViewModel) = apply {
            this.deviceStatusViewModel = viewModel
        }

        fun setonDeviceChange(onDeviceChange:((List<FragranceDevice>) -> Unit)?)=apply{
            this.onDeviceChange = onDeviceChange
        }
        
        fun setContext(context: Context) = apply {
            this.context = context
        }
        
        fun setNeedOpenBluetoothCallback(callback: () -> Unit) = apply {
            this.needOpenBluetooth = callback
        }
        
        fun build(): FragranceManager {
            requireNotNull(lifecycle) { "Lifecycle must be set" }
            requireNotNull(fragranceScanningViewModel) { "ScanningViewModel must be set" }
            requireNotNull(connectViewModel) { "ConnectViewModel must be set" }
            requireNotNull(deviceStatusViewModel) { "DeviceStatusViewModel must be set" }
            requireNotNull(context) { "Context must be set" }
            
            return FragranceManager(
                lifecycle = lifecycle!!,
                fragranceScanningViewModel = fragranceScanningViewModel!!,
                onDeviceAdded = onDeviceAdded,
                connectViewModel = connectViewModel!!,
                connectedResult = connectedResult,
                deviceStatusViewModel = deviceStatusViewModel!!,
                onDeviceChange = onDeviceChange,
                context = context!!,
                needOpenBluetooth = needOpenBluetooth
            ).also { 
                Timber.tag(TAG).i("FragranceManager 实例创建成功")
            }
        }
    }

    init {
        // 监听设备状态变化
        deviceStatusViewModel.viewModelScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                deviceStatusViewModel.devices
                    .sample(500) // 控制频率，最多每500ms一次
                    .collect { devices ->
                        // 在子线程中处理数据
                        withContext(Dispatchers.IO) {
                            // 更新 UI 以反映设备状态变化
                            onDeviceChange?.invoke(devices)
                            // 在子线程中回调

                        }
                    }
            }
        }

        // 监听扫描状态变化
        fragranceScanningViewModel.viewModelScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                fragranceScanningViewModel.scanState.collect { state ->
                    when (state) {
                        is ScanState.ScanFailed -> {
                            Timber.Forest.tag(TAG).e("扫描失败: ${state.reason}")
                            Toast.makeText(context, " ${state.reason}", Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }

        // 监听新设备发现
        fragranceScanningViewModel.viewModelScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                fragranceScanningViewModel.newDeviceFound.collect { devices ->
                    Timber.Forest.tag(TAG).i("发现新设备: ${devices.size}个")
                    onDeviceAdded?.invoke(devices)
                }
            }
        }

        // 监听连接状态
        connectViewModel.viewModelScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                connectViewModel.connectionStatus.collect { event ->
                    when (event) {
                        is ConnectViewModel.ConnectionState.DeviceConnected -> {
                            Timber.Forest.tag(TAG).i("设备已连接: ${event.macAddress}")
                            updateDeviceAutoConnect(event.macAddress,true)
                        }

                        is ConnectViewModel.ConnectionState.AuthSuccess -> {
                            Timber.Forest.tag(TAG).i("设备认证成功: ${event.macAddress}")
                            Toast.makeText(context, "连接成功", Toast.LENGTH_SHORT).show()
                        }

                        is ConnectViewModel.ConnectionState.ConnectionFailed -> {
                            Timber.Forest.tag(TAG).e("连接失败: ${event.macAddress}, 原因: ${event.reason}")
                            Toast.makeText(context, "连接失败: ${event.reason}", Toast.LENGTH_SHORT).show()
                        }

                        is ConnectViewModel.ConnectionState.AuthFailed -> {
                            Timber.Forest.tag(TAG).e("认证失败: ${event.macAddress}, 原因: ${event.reason}")
                            connectedResult?.invoke(false, event.macAddress)
                            Toast.makeText(context, "认证失败: ${event.reason}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        // 监听蓝牙是否开启
        connectViewModel.viewModelScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                connectViewModel.isBluetoothEnabled.collect { enabled ->
                    if (enabled == false) {
                        needOpenBluetooth?.invoke()
                        Toast.makeText(context, "请打开蓝牙", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * 连接指定设备
     * @param macAddress 设备MAC地址
     */
    fun connectToDevice(macAddress: String) {
        if (VehicleTypeConstants.enableFragrance) {
            connectViewModel.connectDevice(macAddress)
        }
    }

    /**
     * 开始扫描设备
     */
    fun startScan() {
        if (VehicleTypeConstants.enableFragrance) {
            fragranceScanningViewModel.startScan()
        }
    }

    /**
     * 停止扫描设备
     */
    fun stopScan() {
        if (VehicleTypeConstants.enableFragrance) {
            fragranceScanningViewModel.stopScan()
        }
    }

    /**
     * 删除设备
     * @param macAddress 设备MAC地址
     */
     fun deleteDevice(macAddress: String,result:(success: Boolean)-> Unit) {
       deviceStatusViewModel.deleteDevice(macAddress,result)
    }

    /**
     * 重命名设备
     * @param macAddress 设备MAC地址
     */

    fun renameDevice(macAddress: String,name:String) {
        deviceStatusViewModel.renameDevice(macAddress,name)
    }



    /**
     * 更新设备自动连接状态
     * @param macAddress 设备MAC地址
     * @param needAutoConnect 是否需要自动连接
     */
    fun updateDeviceAutoConnect(macAddress: String, needAutoConnect: Boolean) {
        deviceStatusViewModel.updateDeviceAutoConnect(macAddress, needAutoConnect)
    }

    /**
     * 获取当前扫描状态
     */
    fun getScanState() = fragranceScanningViewModel.scanState

    /**
     * 获取所有设备列表
     */
    fun getDevices() = deviceStatusViewModel.devices

    fun getDeviceList() = deviceStatusViewModel.getDeviceList()

    fun setPowerOn(macAddress:String,powerState: Boolean){
        deviceStatusViewModel.setPowerState(
            macAddress, if (powerState) {
                PowerState.ON
            } else {
                PowerState.OFF
            }
        )
    }

    fun disconnected(macAddress: String) {
        connectViewModel.viewModelScope.launch {
            updateDeviceAutoConnect(macAddress,false)
            connectViewModel.disconnected(macAddress)
        }
    }
}