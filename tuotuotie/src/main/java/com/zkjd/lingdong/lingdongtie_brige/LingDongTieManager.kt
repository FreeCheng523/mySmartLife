package com.zkjd.lingdong.lingdongtie_brige

import android.Manifest
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.zkjd.lingdong.model.Device
import com.zkjd.lingdong.service.BleService
import com.zkjd.lingdong.ui.home.HomeViewModel
import com.zkjd.lingdong.ui.pairing.PairingAndConnectViewModel
import com.zkjd.lingdong.ui.scanning.ScannningViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * 灵动贴设备管理器
 * 负责统一管理灵动贴设备的扫描、连接、状态监听等功能
 * 
 * 注意：每个 Activity/Fragment 应该创建自己的实例，生命周期与 Activity/Fragment 绑定
 *
 * 使用示例：
 * ```
 * private var lingDongTieManager: LingDongTieManager? = null
 * 
 * // 在 onCreate 中初始化
 * lingDongTieManager = LingDongTieManager.Builder()
 *     .setLifecycle(lifecycle)
 *     .setScanningViewModel(scanningViewModel)
 *     .setPairingViewModel(pairingViewModel)
 *     .setHomeViewModel(homeViewModel)
 *     .setContext(context)
 *     .setOnDeviceAddedCallback { devices -> handleDevices(devices) }
 *     .setDeviceChangedCallback { devices -> handleChanges(devices) }
 *     .setConnectedResultCallback { success, mac -> handleResult(success, mac) }
 *     .setNeedOpenBluetoothCallback { openBluetoothSettings() }
 *     .setStartConnectCallback { showConnectingDialog() }
 *     .build()
 * 
 * // 使用
 * lingDongTieManager?.startScan()
 * ```
 */
class LingDongTieManager private constructor(
    private val lifecycle: Lifecycle,
    private val scanningViewModel: ScannningViewModel,
    private val pairingViewModel: PairingAndConnectViewModel,
    private val homeViewModel: HomeViewModel,
    private val context: Context,
    private val deviceChanged: ((List<Device>) -> Unit)?,
    private val needOpenBluetooth: (() -> Unit)?,
    private val startConnect: (() -> Unit)?,
    private val connectedResult: ((success: Boolean, macAddress: String) -> Unit)?,
    private val onDeviceAdded: ((List<Device>) -> Unit)?
) {
    companion object {
        const val TAG = "LingDongTieManager"
        
        fun initLog() {
            //Log.plant(Log.DebugTree())
        }
    }
    
    /**
     * Builder 模式构建 LingDongTieManager 实例
     */
    class Builder {
        private var lifecycle: Lifecycle? = null
        private var scanningViewModel: ScannningViewModel? = null
        private var pairingViewModel: PairingAndConnectViewModel? = null
        private var homeViewModel: HomeViewModel? = null
        private var context: Context? = null
        private var deviceChanged: ((List<Device>) -> Unit)? = null
        private var needOpenBluetooth: (() -> Unit)? = null
        private var startConnect: (() -> Unit)? = null
        private var connectedResult: ((success: Boolean, macAddress: String) -> Unit)? = null
        private var onDeviceAdded: ((List<Device>) -> Unit)? = null

        fun setLifecycle(lifecycle: Lifecycle) = apply {
            this.lifecycle = lifecycle
        }

        fun setScanningViewModel(scanningViewModel: ScannningViewModel) = apply {
            this.scanningViewModel = scanningViewModel
        }

        fun setPairingViewModel(pairingViewModel: PairingAndConnectViewModel) = apply {
            this.pairingViewModel = pairingViewModel
        }

        fun setHomeViewModel(homeViewModel: HomeViewModel) = apply {
            this.homeViewModel = homeViewModel
        }

        fun setContext(context: Context) = apply {
            this.context = context
        }

        fun setDeviceChangedCallback(callback: (List<Device>) -> Unit) = apply {
            this.deviceChanged = callback
        }

        fun setNeedOpenBluetoothCallback(callback: () -> Unit) = apply {
            this.needOpenBluetooth = callback
        }

        fun setStartConnectCallback(callback: () -> Unit) = apply {
            this.startConnect = callback
        }

        fun setConnectedResultCallback(callback: (success: Boolean, macAddress: String) -> Unit) = apply {
            this.connectedResult = callback
        }

        fun setOnDeviceAddedCallback(callback: (List<Device>) -> Unit) = apply {
            this.onDeviceAdded = callback
        }

        fun build(): LingDongTieManager {
            requireNotNull(lifecycle) { "Lifecycle must be set" }
            requireNotNull(scanningViewModel) { "ScanningViewModel must be set" }
            requireNotNull(pairingViewModel) { "PairingViewModel must be set" }
            requireNotNull(homeViewModel) { "HomeViewModel must be set" }
            requireNotNull(context) { "Context must be set" }

            return LingDongTieManager(
                lifecycle = lifecycle!!,
                scanningViewModel = scanningViewModel!!,
                pairingViewModel = pairingViewModel!!,
                homeViewModel = homeViewModel!!,
                context = context!!,
                deviceChanged = deviceChanged,
                needOpenBluetooth = needOpenBluetooth,
                startConnect = startConnect,
                connectedResult = connectedResult,
                onDeviceAdded = onDeviceAdded
            ).also {
                Timber.tag(TAG).i("LingDongTieManager 实例创建成功")
            }
        }
    }



    init {
        // 监听设备状态变化
        homeViewModel.viewModelScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.devices
                    .sample(500) // 控制频率，最多每500ms一次
                    .collect { devices ->
                        // 在子线程中处理数据
                        withContext(Dispatchers.IO) {
                            // 更新 UI 以反映设备状态变化
                            devices.forEach { device ->
                                Log.i(
                                    TAG,
                                    "device ${device.name} ${device.macAddress} ${device.batteryLevel} ${device.lastConnectionState} ${device.bleName}"
                                )
                            }
                            // 在子线程中回调
                            deviceChanged?.invoke(devices)
                        }
                    }
            }
        }

        // 监听 HomeViewModel UI 事件
        homeViewModel.viewModelScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.uiEvents.collect { event ->
                    when (event) {
                        is HomeViewModel.UiEvent.ShowToast -> {
                            // 可以在这里处理 Toast 事件
                        }

                        is HomeViewModel.UiEvent.Navigate -> {
                            // 可以在这里处理导航事件
                        }
                    }
                }
            }
        }

        // 监听新设备发现
        scanningViewModel.viewModelScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                scanningViewModel.newDeviceFound.collect { devices ->
                    Timber.tag(TAG).i("接收到devices $devices")
                    // 在子线程中处理数据
                    withContext(Dispatchers.IO) {
                        onDeviceAdded?.invoke(devices)
                    }
                }
            }
        }

        // 监听连接状态
        pairingViewModel.viewModelScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                pairingViewModel.connectionStatus.collect @RequiresPermission(
                    Manifest.permission.BLUETOOTH_CONNECT
                ) { event ->
                    when (event) {
                        //MainActivity里面是通过循环判断设备是否连接成功的，这里把true发出去其实也没使用
                        is PairingAndConnectViewModel.ConnectionState.DeviceConnected -> {
                            Timber.tag(TAG).i("连接成功！没反馈给MainActivity")
                        }

                        is PairingAndConnectViewModel.ConnectionState.AuthSuccess -> {
                            Timber.tag(TAG).i("配对成功！没反馈给MainActivity")
                        }

                        is PairingAndConnectViewModel.ConnectionState.ConnectionFailed,
                        is PairingAndConnectViewModel.ConnectionState.AuthFailed -> {
                            Timber.tag(TAG).i("设备结束 $event")
                            connectedResult?.invoke(false, event.macAddress)
                            Toast.makeText(context, event.reason, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        // 监听蓝牙是否开启
        pairingViewModel.viewModelScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                pairingViewModel.isBluetoothEnabled.collect {
                    if (it == false) {
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
        pairingViewModel.connectDevice(macAddress)
        startConnect?.invoke()
    }

    /**
     * 开始扫描设备
     */
    fun startScan() {
        scanningViewModel.startScan()
    }

    /**
     * 停止扫描设备
     */
    fun stopScan() {
        scanningViewModel.stopScan()
    }

    /**
     * 删除设备
     * @param macAddress 设备MAC地址
     */
    fun deleteDevice(macAddress: String,result:(success: Boolean)-> Unit) {
        homeViewModel.deleteDevice(macAddress,result)
    }


    /**
     * 重命名
     * @param macAddress
     */
    fun renameDevice(macAddress: String,name:String) {
        homeViewModel.renameDevice(macAddress,name)
    }

    /**
     * 获取所有设备列表
     */
    fun getDevices() = homeViewModel.getAllDevice()
     fun disconnected(macAddress: String) {
         pairingViewModel.viewModelScope.launch {
             pairingViewModel.disconnect(macAddress)
         }
    }
}