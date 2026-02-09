package com.mine.baselibrary.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.DeadObjectException
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import com.mine.baselibrary.permission.PermissionUtil
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.experimental.and

const val TAG = "BleManagerImpl"
private const val CONNECTION_TIMEOUT_MS = 10000L // 连接超时时间，10秒

/**
 * 延迟发送，首次连接蓝牙，需要延迟发送数据才能成功
 */
const val DELAY_SEND = 1000L

/**
 * 带超时保护,保护写入和下发
 */
private const val DELAY_TIMEOUT = 3000L

/**
 * 蓝牙管理器实现类，提供BLE设备的扫描、连接、通信等功能
 */

abstract class BaseBleManagerIml(
    open val appContext: Context,
) : IBleManager {

    /** 主服务UUID */
    abstract val mainServiceUUID: UUID

    /** 特征UUID - 通知特征（设备->手机） */
    abstract val characteristicNotifyUuid: UUID

    /** 特征UUID - 写入特征（手机->设备） */
    abstract val characteristicWriteUuid:UUID

    /** 电池服务UUID */
    abstract val batteryServiceUuid: UUID

    /** 电池电量特征UUID */
    abstract val batteryLevelUuid: UUID

    /**
     * 是否启用RSSI监控
     * @return 启用返回true，否则返回false，默认返回false
     */
    abstract fun isRssiMonitoringEnabled(): Boolean

    /**
     * 扫描结果
     */
    abstract fun onAcceptScanResult(result: ScanResult)

    /**
     * gatt 连上后验证设备是否具有所有必需的服务和特征
     * @param gatt 蓝牙GATT连接
     * @return 验证成功返回true，否则返回false
     */
    abstract fun validateRequiredServices(gatt: BluetoothGatt): Boolean

    /**
     * 是否鉴权
     */
    abstract fun authIsEnable(): Boolean



    /**
     * 对设备进行鉴权
     * @param device 蓝牙设备
     * @return 鉴权成功返回true，否则返回false
     */
    abstract suspend fun authenticateDevice(device: BluetoothDevice): Boolean
    /**
     * 处理设备的鉴权响应
     * @param gatt 设备GATT连接
     * @param data 响应数据
     */
    abstract fun handleAuthResponse(gatt: BluetoothGatt, data: ByteArray)


    /**
     * 特征值变化回调（用于接收通知）
     */
    abstract fun onAcceptCharacteristicChanged(
        characteristic: BluetoothGattCharacteristic, gatt: BluetoothGatt
    )

    /**
     * 特征读取完成回调
     */
    abstract fun onAcceptCharacteristicRead(
        status: Int,
        characteristic: BluetoothGattCharacteristic,
        gatt: BluetoothGatt

    )

    /** 客户端特征配置描述符UUID */
    val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /** 用于后台任务的协程作用域 */
    val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 蓝牙管理器和适配器 */
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    /** 存储设备地址到GATT连接的映射 */
    val deviceGattMap = ConcurrentHashMap<String, BluetoothGatt>()

    /** 扫描状态流 */
    val scanStateFlow = MutableStateFlow<ScanState>(ScanState.NotScanning)

    /** 设备事件流 */
    val deviceEventsFlow = MutableSharedFlow<DeviceEvent>(replay = 0)


    /** 断开连接请求事件流 */
    private val disconnectRequestEventsFlow = MutableSharedFlow<DisconnectRequestEvent>(replay = 0)

    /** 扫描结果列表 */
    val scanResults = mutableSetOf<BluetoothDevice>()


    /** 存储连接操作的异步结果 */
    private val connectionResults = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    /** 存储写入特征回调结果 */
    val writeCallbacks = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    /** 存储已成功连接并发现服务的设备信息 */
    private val discoveredServicesCache = ConcurrentHashMap<String, Boolean>()

    /** 存储设备特征信息 */
    private val deviceCharacteristics =
        ConcurrentHashMap<String, Map<UUID, BluetoothGattCharacteristic>>()

    /** 鉴权结果回调 */
    val authenticationResults = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    /** 用于同步处理蓝牙特性变化事件的互斥锁 */
    val characteristicChangedLock = Any()

    /** 记录最近处理的通知特征值哈希，用于防止重复处理 */
    val lastProcessedNotification = ConcurrentHashMap<String, Long>()

    /** 记录每个设备最后一次服务发现的时间，用于防止重复处理 */
    private val lastServiceDiscoveryTime = ConcurrentHashMap<String, Long>()

    /** 存储RSSI监控任务 */
    private val rssiMonitoringJobs = ConcurrentHashMap<String, Job>()

    /** 存储RSSI读取结果回调 */
    private val rssiReadCallbacks = ConcurrentHashMap<String, CompletableDeferred<Int>>()

    /**
     * 初始化蓝牙管理器
     * @param context 应用上下文
     */
    override fun initialize(context: Context) {
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    /**
     * 开始扫描BLE设备
     * 扫描结果通过 scanStateFlow 流发布
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun startScan() {
        // 检查蓝牙是否启用
        if (!bluetoothAdapter.isEnabled) {
            Log.e(TAG, "蓝牙未开启")
            coroutineScope.launch {
                scanStateFlow.emit(ScanState.ScanFailed("蓝牙未启用"))
            }
            return
        }

        // 检查蓝牙扫描权限
        if (!hasBluetoothPermission("scan")) {
            coroutineScope.launch{
                scanStateFlow.emit(ScanState.ScanFailed("缺少蓝牙扫描权限"))
            }
            Log.e(TAG, "缺少蓝牙扫描权限")
            return
        }


        // 清空之前的扫描结果
        scanResults.clear()
        coroutineScope.launch{
            scanStateFlow.emit(ScanState.Scanning)
        }

        // 设置扫描参数 - 低延迟模式提高扫描速度
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val filters = mutableListOf<ScanFilter>()
        // 创建MAC地址过滤器
        val scanFilter: ScanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid.fromString(mainServiceUUID.toString())) // 替换为目标设备的MAC地址
            .build()
        // 将过滤器添加到列表
        filters.add(scanFilter)

        // 开始扫描
        try {
            bluetoothAdapter.bluetoothLeScanner.startScan(
                null, // 不使用过滤器，在回调中根据设备名称前缀筛选
                scanSettings,
                scanCallback
            )
            Timber.tag(TAG).w("开始扫描BLE设备")
        } catch (e: Exception) {
            coroutineScope.launch{
                scanStateFlow.emit(ScanState.ScanFailed("扫描启动失败: ${e.message}"))
                Timber.tag(TAG).e(e, "启动扫描失败")
            }
        }
    }


    /**
     * 停止BLE设备扫描
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun stopScan() {
        try {
            bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
            coroutineScope.launch {
                scanStateFlow.emit(ScanState.NotScanning)
                Timber.tag(TAG).w("停止扫描BLE设备")
            }

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "停止扫描失败")
        }
    }

    /**
     * 获取当前的扫描状态流
     * @return 扫描状态流
     */
    override fun getScanState(): Flow<ScanState> = scanStateFlow.asStateFlow()

    /**
     * 连接到BLE设备
     * @param device 要连接的蓝牙设备
     * @return 连接成功返回true，否则返回false
     */
    override suspend fun connect(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        // 检查蓝牙连接权限
        if (!hasBluetoothPermission()) {
            deviceEventsFlow.emit(DeviceEvent.ConnectionFailed(device, "缺少蓝牙连接权限"))
            Timber.tag(TAG).e("连接失败: 缺少蓝牙连接权限")
            return@withContext false
        }

        try {
            //这里影响到鉴权
            //if (connectionResults[device.address]!=null){
            //    return@withContext true
            //}
            // 如果已存在连接，先断开
            disconnectExistingConnection(device.address)

            // 创建连接结果的异步通知器
            val connectResult = CompletableDeferred<Boolean>()
            connectionResults[device.address] = connectResult

            // 判断是否是重连
            val isReconnecting = discoveredServicesCache[device.address] == true

            // 重连场景使用autoConnect=true可能导致连接延迟，这里统一使用false以保证低延迟
            val autoConnect = false

            // 创建新连接
            Timber.tag(TAG)
                .w("尝试${if (isReconnecting) "重新" else ""}连接设备: ${device.address}, autoConnect=$autoConnect")
            val gatt = device.connectGatt(appContext, autoConnect, gattCallback)
            deviceGattMap[device.address] = gatt

            true

        } catch (e: Exception) {
            deviceEventsFlow.emit(DeviceEvent.ConnectionFailed(device, "连接失败"))
            Timber.tag(TAG).e(e, "连接过程中发生异常: ${device.address}")
            return@withContext false
        }
    }

    /**
     * 断开与BLE设备的连接
     * @param device 要断开的蓝牙设备
     * @return 断开成功返回true，否则返回false
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun disconnect(device: BluetoothDevice) = withContext(Dispatchers.IO) {
        if (!hasBluetoothPermission()) {
            Timber.tag(TAG).e("断开连接失败: 缺少蓝牙连接权限")
            return@withContext false
        }

        val gatt = deviceGattMap[device.address] ?: run {
            Timber.tag(TAG).w("找不到设备的GATT连接: ${device.address}")
            return@withContext false
        }

        try {
            Timber.tag(TAG).w("断开设备连接: ${device.address}")

            // 主动断开前停止RSSI监控
            stopRssiMonitoring(device)

            gatt.disconnect()
            return@withContext true
        } catch (e: DeadObjectException) {
            // DeadObjectException 表示底层连接已经失效，可以认为已经断开
            Timber.tag(TAG).w("设备连接已失效（DeadObjectException）: ${device.address}")

            // 清理资源
            deviceGattMap.remove(device.address)
            try {
                gatt.close()
            } catch (closeException: Exception) {
                Timber.tag(TAG).e(closeException, "关闭GATT失败")
            }

            return@withContext false
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "断开连接失败: ${device.address}")
            return@withContext false
        }
    }

    /**
     * 获取设备事件流
     * @return 设备事件共享流
     */
    override fun getDeviceEvents(): SharedFlow<DeviceEvent> = deviceEventsFlow


    /**
     * 获取断开连接请求事件流
     * @return 断开连接请求事件共享流
     */
    override fun getDisconnectRequestEvents(): SharedFlow<DisconnectRequestEvent> =
        disconnectRequestEventsFlow

    /**
     * 关闭蓝牙管理器，释放资源
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun close() {
        // 停止扫描
        stopScan()

        // 停止所有RSSI监控
        stopAllRssiMonitoring()

        // 断开所有设备连接
        for (gatt in deviceGattMap.values) {
            closeGattConnection(gatt)
        }

        deviceGattMap.clear()
        connectionResults.clear()
        writeCallbacks.clear()
        discoveredServicesCache.clear()
        deviceCharacteristics.clear()

        authenticationResults.clear()
        lastProcessedNotification.clear()
        lastServiceDiscoveryTime.clear()
        rssiReadCallbacks.clear()
        Timber.tag(TAG).w("蓝牙管理器已关闭")
    }

    /**
     * BLE设备扫描回调
     */
    private val scanCallback = object : ScanCallback() {
        /**
         * 收到单个扫描结果
         */
        @SuppressLint("NewApi")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            onAcceptScanResult(result)
        }

        /**
         * 收到批量扫描结果
         */
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            Timber.tag(TAG).w("收到批量扫描结果: ${results.size}个")
            for (result in results) {
                onScanResult(0, result)
            }
        }

        /**
         * 扫描失败
         */
        @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
        override fun onScanFailed(errorCode: Int) {
            Timber.tag(TAG).e("扫描失败, 错误码: $errorCode")

            stopScan()

            scanStateFlow.tryEmit(ScanState.ScanFailed("扫描失败，错误码: $errorCode"))
        }

    }


    /**
     * GATT回调
     */
    private val gattCallback = object : BluetoothGattCallback() {
        /**
         * 连接状态变化回调
         */
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val device = gatt.device
            //gatt.requestMtu(158)
            Timber.tag(TAG).i("onConnectionStateChange - device: ${device.address}, status: $status, newState: $newState")
            // Log equivalent to onClientRegistered callback
            if (status == 0 && newState == BluetoothProfile.STATE_CONNECTING) {
                Timber.tag(TAG).d("onClientRegistered() - status=$status clientIf=${gatt.hashCode()}")
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Timber.tag(TAG).w("设备已连接: ${device.address}")

                    // 延迟设置连接参数，等待连接稳定
                    coroutineScope.launch {
                        delay(500) // 等待500ms让连接稳定
                        setConnectionPriority(gatt, BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                    }
                    // 设置连接参数，请求更低延迟
                    //setConnectionPriority(gatt, BluetoothGatt.CONNECTION_PRIORITY_HIGH)

                    // 检查是否为已知设备（已发现过服务）
                    val isKnownDevice = discoveredServicesCache[device.address] == true

                    if (isKnownDevice) {
                        // 对于已知设备，尝试直接使用缓存的服务
                        Timber.tag(TAG).w("设备已连接过，尝试直接验证服务: ${device.address}")

                        try {
                            // 尝试获取服务，如果服务已缓存，不会触发服务发现
                            val services = gatt.services
                            if (services.isNotEmpty()) {
                                // 有服务缓存，验证必要服务和特征
                                if (validateRequiredServices(gatt)) {
                                    // 快速读取电池电量
                                    readBatteryLevel(gatt)

                                    // 发送连接成功事件
                                    coroutineScope.launch {
                                        Timber.tag(TAG).i("发送连接成功")
                                        deviceEventsFlow.emit(DeviceEvent.Connected(device))
                                        // 直接标记连接成功，不等待服务发现回调
                                        completeConnectionWithSuccess(device.address)
                                    }
                                    return
                                }
                            }
                        } catch (e: Exception) {
                            Timber.tag(TAG).w("使用缓存服务失败: ${e.message}")
                        }
                    }

                    // 如果没有缓存或缓存无效，执行传统的服务发现
                    Timber.tag(TAG).w("开始发现服务: ${device.address}")
                    gatt.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    // 检查状态码，区分正常断开和错误断开
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        val errorMsg = when (status) {
                            22 -> "连接超时 (GATT_CONN_TIMEOUT)"
                            8 -> "连接失败 (GATT_INSUFFICIENT_AUTHORIZATION)"
                            19 -> "连接失败 (GATT_CONNECTION_CONGESTED)"
                            else -> "连接失败 (状态码: $status)"
                        }
                        Timber.tag(TAG).e("设备断开连接，错误: $errorMsg, 设备: ${device.address}")
                        completeConnectionWithFailure(device.address, errorMsg)
                    } else {
                        Timber.tag(TAG).w("设备正常断开: ${device.address}")
                    }

                    // 停止RSSI监控
                    stopRssiMonitoring(device)

                    // 从管理映射中移除，关闭GATT连接
                    deviceGattMap.remove(device.address)
                    gatt.close()

                    // 如果断开时正在等待连接结果，则标记为失败
                    completeConnectionWithFailure(device.address, "设备断开连接")

                    // 发送断开连接事件
                    coroutineScope.launch {
                        deviceEventsFlow.emit(DeviceEvent.Disconnected(device))
                    }
                }
            }
        }


        /**
         * 服务发现完成回调
         */
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val device = gatt.device
            val deviceAddress = device.address

            val service = gatt.getService(mainServiceUUID) ?: run {
                Timber.tag(TAG).e("发送鉴权状态失败: 找不到主服务")
                return
            }

            val readCharacteristic =
                service.getCharacteristic(characteristicNotifyUuid) ?: run {
                    Timber.tag(TAG).e("发送鉴权状态失败: 找不到写入特征")
                    return
                }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // 获取当前时间
                val currentTime = System.currentTimeMillis()

                // 获取该设备上次服务发现的时间
                val lastDiscoveryTime = lastServiceDiscoveryTime[deviceAddress] ?: 0L
                val diffTime = currentTime - lastDiscoveryTime
                // 更新该设备的服务发现时间
                lastServiceDiscoveryTime[deviceAddress] = currentTime

                Timber.tag(TAG).e("鉴权:设备${deviceAddress}服务发现间隔时间: ${diffTime}ms")

                // 针对不同设备设置不同的时间限制
                val timeLimit = getServiceDiscoveryTimeLimit(device)

                if (diffTime <= timeLimit) {
                    Timber.tag(TAG)
                        .w("设备${deviceAddress}服务发现过于频繁，跳过处理，间隔: ${diffTime}ms, 限制: ${timeLimit}ms")
                    return
                }

                // 验证设备服务和特征
                val isValidDevice = validateRequiredServices(gatt)
                Timber.tag(TAG).w("服务发现完成: ${device.address}, 验证结果: $isValidDevice")

                if (isValidDevice) {
                    // 标记设备已发现服务，用于优化重连
                    discoveredServicesCache[device.address] = true

                    // 缓存服务特征映射
                    cacheDeviceCharacteristics(gatt)


                    // 执行设备鉴权
                    coroutineScope.launch {
                        // 检查是否需要进行鉴权
                        val enableAuth = authIsEnable()

                        if (enableAuth) {
                            // 开启了鉴权，进行设备鉴权
                            Timber.tag(TAG).w("鉴权:已启用，开始设备鉴权: ${device.address}")
                            val authSuccess = authenticateDevice(device)

                            if (authSuccess) {
                                // 鉴权成功，发送连接成功事件
                                deviceEventsFlow.emit(DeviceEvent.Connected(device))
                                deviceEventsFlow.emit(DeviceEvent.AuthSuccess(device))
                                // 标记连接成功
                                completeConnectionWithSuccess(device.address)
                                //延迟发送，首次连接蓝牙，需要延迟发送数据才能成功
                                Thread.sleep(DELAY_SEND)
                                readDeviceInfo(gatt, readCharacteristic)
                                // 读取电池电量
                                readBatteryLevel(gatt)

                                // 延迟启动RSSI监控，避免连接过载
                                if (isRssiMonitoringEnabled()) {
                                    coroutineScope.launch {
                                        delay(2000) // 等待2秒让连接完全稳定
                                        Timber.tag(TAG).w("鉴权成功，开启RSSI监控: ${device.address}")
                                        startRssiMonitoring(device)
                                    }
                                }
                            } else {
                                // 鉴权失败，断开连接
                                Timber.tag(TAG).e("设备鉴权失败，断开连接: ${device.address}")
                                deviceEventsFlow.emit(
                                    DeviceEvent.AuthFailed(
                                        device,
                                        "设备鉴权失败"
                                    )
                                )

                                try {
                                    gatt.disconnect()
                                } catch (e: DeadObjectException) {
                                    Timber.tag(TAG).w("鉴权失败时发现连接已失效: ${device.address}")
                                }
                                completeConnectionWithFailure(device.address, "设备鉴权失败")
                            }
                        } else {
                            // 未开启鉴权，直接标记连接成功
                            Timber.tag(TAG).w("鉴权已禁用，跳过设备鉴权: ${device.address}")
                            deviceEventsFlow.emit(DeviceEvent.Connected(device))
                            completeConnectionWithSuccess(device.address)
                            //延迟发送，首次连接蓝牙，需要延迟发送数据才能成功
                            Thread.sleep(DELAY_SEND)
                            readDeviceInfo(gatt, readCharacteristic)
                            // 读取电池电量
                            readBatteryLevel(gatt)

                             // 延迟启动RSSI监控，避免连接过载
                                if (isRssiMonitoringEnabled()) {
                                    coroutineScope.launch {
                                        delay(2000) // 等待2秒让连接完全稳定
                                        Timber.tag(TAG).w("鉴权成功，开启RSSI监控: ${device.address}")
                                        startRssiMonitoring(device)
                                    }
                                }
                        }
                    }
                } else {
                    // 设备缺少必要服务，断开连接
                    Timber.tag(TAG).e("设备缺少必要的服务或特征: ${device.address}")
                    coroutineScope.launch {
                        deviceEventsFlow.emit(
                            DeviceEvent.ConnectionFailed(
                                device,
                                "设备缺少必要的服务或特征"
                            )
                        )
                    }

                    try {
                        gatt.disconnect()
                    } catch (e: DeadObjectException) {
                        Timber.tag(TAG).w("服务验证失败时发现连接已失效: ${device.address}")
                    }
                    completeConnectionWithFailure(device.address, "设备缺少必要的服务或特征")
                }
            } else {
                Timber.tag(TAG).e("服务发现失败: $status")
                coroutineScope.launch {
                    deviceEventsFlow.emit(DeviceEvent.ConnectionFailed(device, "服务发现失败"))
                }

                completeConnectionWithFailure(device.address, "服务发现失败")
            }
        }


        /**
         * 描述符写入完成回调
         */
        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (descriptor.uuid == CLIENT_CHARACTERISTIC_CONFIG_UUID) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Timber.tag(TAG).w("通知描述符写入成功: ${descriptor.characteristic.uuid}")
                    // 通知配置成功，设备准备就绪
                    coroutineScope.launch {
                        deviceEventsFlow.emit(DeviceEvent.DeviceReady(gatt.device))
                    }
                } else {
                    Timber.tag(TAG)
                        .e("通知描述符写入失败: ${descriptor.characteristic.uuid}, 状态码: $status")
                }
            }
        }


        /**
         * 特征值变化回调（用于接收通知）
         */
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            Timber.tag(TAG).e("onCharacteristicChanged")
            // 使用同步锁，确保多线程环境下的数据一致性
            onAcceptCharacteristicChanged(characteristic, gatt)
        }

        /**
         * 特征读取完成回调
         */
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            onAcceptCharacteristicRead(status, characteristic, gatt)
        }

        /**
         * 特征写入完成回调
         */
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val uuid = characteristic.uuid.toString()
            Timber.tag(TAG).w("特征写入完成: $uuid, 状态: $status，uuid:${writeCallbacks[uuid]}")

            writeCallbacks[uuid]?.let { deferred ->
                if (!deferred.isCompleted) {
                    Timber.tag(TAG).w("特征写入完成: writeCallbacks to complete")
                    deferred.complete(status == BluetoothGatt.GATT_SUCCESS)
                }
                writeCallbacks.remove(uuid)
            }
        }

        /**
         * RSSI读取完成回调
         */
        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            val device = gatt.device
            val deviceAddress = device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Timber.tag(TAG).d("读取RSSI成功: ${deviceAddress}, RSSI=$rssi dBm")


                // 完成RSSI读取回调
                rssiReadCallbacks[deviceAddress]?.let { deferred ->
                    if (!deferred.isCompleted) {
                        deferred.complete(rssi)
                    }
                    rssiReadCallbacks.remove(deviceAddress)
                }
            } else {
                Timber.tag(TAG).e("读取RSSI失败: ${deviceAddress}, 状态码: $status")

                // 读取失败，返回错误值
                rssiReadCallbacks[deviceAddress]?.let { deferred ->
                    if (!deferred.isCompleted) {
                        deferred.completeExceptionally(Exception("读取RSSI失败: status=$status"))
                    }
                    rssiReadCallbacks.remove(deviceAddress)
                }
            }
        }

//        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
//            super.onMtuChanged(gatt, mtu, status)
//            Timber.tag(TAG).w("ATT MTU changed to $mtu, 状态: ${status == BluetoothGatt.GATT_SUCCESS}")
//        }
    }




    /**
     * 将字节数组转换为十六进制字符串（用于日志调试）
     */
    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val result = StringBuilder(bytes.size * 3)
        for (b in bytes) {
            result.append(hexChars[b.toInt() shr 4 and 0xF])
            result.append(hexChars[b.toInt() and 0xF])
            result.append(' ')
        }
        return result.toString().trim()
    }


    /**
     * 启用特征的通知功能
     * @param gatt GATT连接
     * @param characteristic 需要启用通知的特征
     * @return 成功返回true，否则返回false
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun enableCharacteristicNotification(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ): Boolean {
        // 1. 启用本地通知
        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            Timber.tag(TAG).e("无法启用特征通知: ${characteristic.uuid}")
            return false
        }

        // 2. 获取并配置客户端特征配置描述符(CCCD)
        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID) ?: run {
            Timber.tag(TAG).e("找不到通知描述符: ${characteristic.uuid}")
            return false
        }

        // 3. 写入描述符，启用通知
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        if (!gatt.writeDescriptor(descriptor)) {
            Timber.tag(TAG).e("写入通知描述符失败: ${characteristic.uuid}")
            return false
        }

        Timber.tag(TAG).w("已请求启用通知特征: ${characteristic.uuid}")
        return true
    }

    /**
     * 完成连接并标记为成功
     * @param deviceAddress 设备MAC地址
     */
    private fun completeConnectionWithSuccess(deviceAddress: String) {
        connectionResults[deviceAddress]?.let { deferred ->
            if (!deferred.isCompleted) {
                deferred.complete(true)
                Timber.tag(TAG).w("连接完成并标记为成功: $deviceAddress")
            }
            connectionResults.remove(deviceAddress)
        }
    }

    /**
     * 完成连接并标记为失败
     * @param deviceAddress 设备MAC地址
     * @param reason 失败原因
     */
    private fun completeConnectionWithFailure(deviceAddress: String, reason: String) {
        connectionResults[deviceAddress]?.let { deferred ->
            if (!deferred.isCompleted) {
                deferred.complete(false)
                Timber.tag(TAG).e("连接失败: $deviceAddress, 原因: $reason")
            }
            connectionResults.remove(deviceAddress)
        }
    }

    /**
     * 读取设备电池电量
     * @param gatt 蓝牙GATT连接
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun readBatteryLevel(gatt: BluetoothGatt) {
        val batteryService = gatt.getService(batteryServiceUuid) ?: run {
            Timber.tag(TAG).w("无法读取电池电量: 设备缺少电池服务")
            return
        }

        val batteryCharacteristic = batteryService.getCharacteristic(batteryLevelUuid) ?: run {
            Timber.tag(TAG).w("无法读取电池电量: 设备缺少电池电量特征")
            return
        }

        gatt.readCharacteristic(batteryCharacteristic)
    }


    /**
     * 带超时保护的写入特征
     * @param gatt GATT连接
     * @param characteristic 要写入的特征
     * @param value 写入的数据
     * @param timeoutMs 超时时间（毫秒）
     * @return 写入成功返回true，否则返回false
     */
    suspend fun writeCharacteristicWithTimeout(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        timeoutMs: Long = DELAY_TIMEOUT,
        tagMsg: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val uuid = characteristic.uuid.toString()
        val result = CompletableDeferred<Boolean>()

        // 保存回调
        writeCallbacks[uuid] = result

        Timber.tag(TAG).e("$tagMsg 写入特征值: $uuid --$result")

        // 设置超时
        val timeoutJob = coroutineScope.launch {
            delay(timeoutMs)
            if (!result.isCompleted) {
                result.complete(false)
                writeCallbacks.remove(uuid)
                Timber.tag(TAG).e("$tagMsg 写入特征超时: $uuid")
            }
        }
        // 执行写入
        characteristic.value = value
        val writeStarted = gatt.writeCharacteristic(characteristic)

        if (!writeStarted) {
            timeoutJob.cancel()
            writeCallbacks.remove(uuid)
            Timber.tag(TAG).e("$tagMsg 启动写入失败: $uuid")
            return@withContext false
        } else {
            Timber.tag(TAG).e("$tagMsg 启动写入成功: $uuid")
        }

        // 等待写入完成或超时
        val success = result.await()
        Timber.tag(TAG).e("$tagMsg 等待写入完成或超时: $uuid,result:$success")
        timeoutJob.cancel()

        return@withContext success
    }


    /**
     * 带超时保护的读取特征
     * @param gatt GATT连接
     * @param characteristic 要写入的特征
     * @param timeoutMs 超时时间（毫秒）
     * @return 写入成功返回true，否则返回false
     */
    suspend fun readCharacteristicWithTimeout(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        timeoutMs: Long = DELAY_TIMEOUT
    ): Boolean = withContext(Dispatchers.IO) {
        val uuid = characteristic.uuid.toString()
        val result = CompletableDeferred<Boolean>()

        // 保存回调
        writeCallbacks[uuid] = result

        // 设置超时
        val timeoutJob = coroutineScope.launch {
            delay(timeoutMs)
            if (!result.isCompleted) {
                result.complete(false)
                writeCallbacks.remove(uuid)
                Timber.tag(TAG).e("读取特征超时: $uuid")
            }
        }

        val writeStarted = gatt.readCharacteristic(characteristic)

        if (!writeStarted) {
            timeoutJob.cancel()
            writeCallbacks.remove(uuid)
            Timber.tag(TAG).e("启动读取失败: $uuid")
            return@withContext false
        } else {
            Timber.tag(TAG).e("启动读取成功: $uuid")
        }

        // 等待写入完成或超时
        val success = result.await()
        timeoutJob.cancel()

        return@withContext success
    }


    /**
     * 断开现有的设备连接
     * @param deviceAddress 设备MAC地址
     */
    private fun disconnectExistingConnection(deviceAddress: String) {
        val existingGatt = deviceGattMap[deviceAddress]
        if (existingGatt != null) {
            Timber.tag(TAG).w("断开现有连接: $deviceAddress")
            closeGattConnection(existingGatt)
            deviceGattMap.remove(deviceAddress)
        }
    }

    /**
     * 关闭GATT连接并释放资源
     * @param gatt GATT连接
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun closeGattConnection(gatt: BluetoothGatt) {
        try {
            // 先断开连接
            gatt.disconnect()

            // 等待一段时间确保断开
            Thread.sleep(200)

            // 关闭GATT
            gatt.close()
        } catch (e: DeadObjectException) {
            // DeadObjectException 表示底层连接已经失效，直接关闭
            Timber.tag(TAG).e(e,"连接已失效（DeadObjectException），直接关闭: ${gatt.device.address}")
            try {
                gatt.close()
            } catch (closeException: Exception) {
                Timber.tag(TAG).e(closeException, "关闭GATT失败: ${gatt.device.address}")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "关闭GATT连接时出错: ${gatt.device.address}")
        }
    }

    /**
     * 检查是否有蓝牙权限
     * @param permissionType 权限类型，"scan"表示扫描权限，其他值表示连接权限
     * @return 有权限返回true，否则返回false
     */
    private fun hasBluetoothPermission(permissionType: String = "connect"): Boolean {
        return when (permissionType) {
            "scan" -> PermissionUtil.hasBluetoothScanPermission(appContext)
            else -> PermissionUtil.hasBluetoothConnectPermission(appContext)
        }
    }

    /**
     * 设置连接优先级
     * @param gatt GATT连接
     * @param priority 优先级，如CONNECTION_PRIORITY_HIGH
     */
    private fun setConnectionPriority(gatt: BluetoothGatt, priority: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val success = gatt.requestConnectionPriority(priority)
                if (success) {
                    Timber.tag(TAG)
                        .w("设置连接优先级成功: ${gatt.device.address}, 优先级=$priority")
                } else {
                    Timber.tag(TAG).w("设置连接优先级失败: ${gatt.device.address}")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "设置连接优先级时出错: ${gatt.device.address}")
            }
        }
    }

    /**
     * 缓存设备特征映射
     */
    private fun cacheDeviceCharacteristics(gatt: BluetoothGatt) {
        try {
            val characteristicMap = mutableMapOf<UUID, BluetoothGattCharacteristic>()

            // 获取所有服务
            for (service in gatt.services) {
                // 获取服务中的所有特征
                for (characteristic in service.characteristics) {
                    characteristicMap[characteristic.uuid] = characteristic
                }
            }

            // 存储特征映射
            deviceCharacteristics[gatt.device.address] = characteristicMap
            Timber.tag(TAG)
                .w("已缓存设备特征映射: ${gatt.device.address}, 特征数量=${characteristicMap.size}")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "缓存设备特征时出错: ${gatt.device.address}")
        }
    }




    /**
     * 清理过期的通知记录
     */
    fun clearExpiredNotifications(currentTime: Long) {
        // 只保留最近500毫秒内的通知记录
        if (lastProcessedNotification.size > 20) {
            val keysToRemove = lastProcessedNotification.entries
                .filter { (currentTime - it.value) > 500 }
                .map { it.key }

            keysToRemove.forEach { lastProcessedNotification.remove(it) }
        }
    }

    /**
     * 根据设备类型获取服务发现时间限制
     * @param device 蓝牙设备
     * @return 时间限制（毫秒）
     */
    private fun getServiceDiscoveryTimeLimit(device: BluetoothDevice): Long {
        return when {
            // 默认设备 - 标准间隔
            else -> 1000L
        }
    }

    /**
     */
    override suspend fun getBleDevices(): List<BluetoothDevice> = withContext(Dispatchers.IO) {
        return@withContext bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
    }

    /**
     * 开始RSSI监控
     * @param device 要监控的设备
     * @param intervalMs 监控间隔（毫秒），默认10000ms
     * @param rssiThreshold RSSI阈值，低于此值将断开连接，默认-100dBm
     */
    fun startRssiMonitoring(
        device: BluetoothDevice,
        intervalMs: Long = 10000L,
        rssiThreshold: Int = -100
    ) {
        val deviceAddress = device.address

        // 如果已经在监控，先停止
        stopRssiMonitoring(device)

        // 启动新的监控任务
        val monitoringJob = coroutineScope.launch {
            Timber.tag(TAG)
                .w("开始RSSI监控: ${deviceAddress}, 间隔=${intervalMs}ms, 阈值=${rssiThreshold}dBm")

            var consecutiveFailures = 0        // 连续读取失败次数
            val maxFailures = 3                 // 连续失败3次后断开

            var consecutiveWeakSignals = 0      // 连续信号弱的次数
            val maxWeakSignals = 3              // 连续3次信号弱才断开

            while (isActive) {
                try {
                    // 读取RSSI
                    val rssi = readRssi(device)

                    if (rssi != null) {
                        // 重置失败计数
                        consecutiveFailures = 0

                        // 检查RSSI是否低于阈值
                        if (rssi < rssiThreshold) {
                            consecutiveWeakSignals++
                            Timber.tag(TAG)
                                .w("RSSI过弱: ${deviceAddress}, RSSI=${rssi}dBm < 阈值=${rssiThreshold}dBm，连续弱信号次数=${consecutiveWeakSignals}/${maxWeakSignals}")

                            // 连续多次信号弱才断开
                            if (consecutiveWeakSignals >= maxWeakSignals) {
                                Timber.tag(TAG)
                                    .e("连续${consecutiveWeakSignals}次RSSI过弱，断开连接: ${deviceAddress}")

                                // 发送断开连接请求事件
                                disconnectRequestEventsFlow.emit(
                                    DisconnectRequestEvent(
                                        deviceAddress = device.address,
                                        reason = "RSSI信号过弱，连续${consecutiveWeakSignals}次低于阈值${rssiThreshold}dBm"
                                    )
                                )

                                // 停止监控
                                break
                            }
                        } else {
                            // RSSI正常，重置弱信号计数
                            if (consecutiveWeakSignals > 0) {
                                Timber.tag(TAG)
                                    .d("RSSI恢复正常: ${deviceAddress}, RSSI=${rssi}dBm，重置弱信号计数")
                            }
                            consecutiveWeakSignals = 0
                            Timber.tag(TAG).d("RSSI正常: ${deviceAddress}, RSSI=${rssi}dBm")
                        }
                    } else {
                        // 读取失败
                        consecutiveFailures++
                        Timber.tag(TAG)
                            .e("读取RSSI失败: ${deviceAddress}, 连续失败次数=${consecutiveFailures}/${maxFailures}")

                        if (consecutiveFailures >= maxFailures) {
                            Timber.tag(TAG)
                                .e("连续${consecutiveFailures}次读取RSSI失败，判定设备已断开: ${deviceAddress}")

                            // 发送断开连接请求事件
                            disconnectRequestEventsFlow.emit(
                                DisconnectRequestEvent(
                                    deviceAddress = device.address,
                                    reason = "连续${consecutiveFailures}次读取RSSI失败，判定设备已断开"
                                )
                            )

                            // 停止监控
                            break
                        }
                    }

                    // 等待下次监控
                    delay(intervalMs)

                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "RSSI监控过程中发生异常: ${deviceAddress}")
                    consecutiveFailures++

                    if (consecutiveFailures >= maxFailures) {
                        Timber.tag(TAG)
                            .e("连续${consecutiveFailures}次读取RSSI失败，判定设备已断开: ${deviceAddress}")

                        // 发送断开连接请求事件
                        disconnectRequestEventsFlow.emit(
                            DisconnectRequestEvent(
                                deviceAddress = device.address,
                                reason = "RSSI监控过程中连续${consecutiveFailures}次发生异常"
                            )
                        )

                        // 停止监控
                        break
                    }

                    delay(intervalMs)
                }
            }

            // 清理监控任务
            rssiMonitoringJobs.remove(deviceAddress)
            Timber.tag(TAG).w("停止RSSI监控: ${deviceAddress}")
        }

        // 保存监控任务
        rssiMonitoringJobs[deviceAddress] = monitoringJob
    }

    /**
     * 停止RSSI监控
     * @param device 要停止监控的设备
     */
    fun stopRssiMonitoring(device: BluetoothDevice) {
        val deviceAddress = device.address
        rssiMonitoringJobs[deviceAddress]?.let { job ->
            job.cancel()
            rssiMonitoringJobs.remove(deviceAddress)
            Timber.tag(TAG).w("已停止RSSI监控: ${deviceAddress}")
        }
    }

    /**
     * 停止所有RSSI监控
     */
    fun stopAllRssiMonitoring() {
        Timber.tag(TAG).w("停止所有RSSI监控，当前监控数量: ${rssiMonitoringJobs.size}")
        rssiMonitoringJobs.values.forEach { job ->
            job.cancel()
        }
        rssiMonitoringJobs.clear()
    }

    /**
     * 手动读取设备RSSI
     * @param device 要读取的设备
     * @return RSSI值，如果读取失败返回null
     */
    suspend fun readRssi(device: BluetoothDevice): Int? =
        withContext(Dispatchers.IO) @androidx.annotation.RequiresPermission(
            android.Manifest.permission.BLUETOOTH_CONNECT
        ) {
            val deviceAddress = device.address

            // 检查权限
            if (!hasBluetoothPermission()) {
                Timber.tag(TAG).e("读取RSSI失败: 缺少蓝牙连接权限")
                return@withContext null
            }

            // 获取GATT连接
            val gatt = deviceGattMap[deviceAddress] ?: run {
                Timber.tag(TAG).e("读取RSSI失败: 找不到设备的GATT连接 ${deviceAddress}")
                return@withContext null
            }

            try {
                // 创建异步结果
                val rssiResult = CompletableDeferred<Int>()
                rssiReadCallbacks[deviceAddress] = rssiResult

                // 发起RSSI读取请求
                val readStarted = gatt.readRemoteRssi()

                if (!readStarted) {
                    rssiReadCallbacks.remove(deviceAddress)
                    Timber.tag(TAG).e("启动RSSI读取失败: ${deviceAddress}")
                    return@withContext null
                }

                // 等待读取结果，设置5秒超时
                return@withContext try {
                    withTimeout(5000L) {
                        rssiResult.await()
                    }
                } catch (e: TimeoutCancellationException) {
                    rssiReadCallbacks.remove(deviceAddress)
                    Timber.tag(TAG).e("读取RSSI超时: ${deviceAddress}")
                    null
                } catch (e: Exception) {
                    rssiReadCallbacks.remove(deviceAddress)
                    Timber.tag(TAG).e(e, "读取RSSI异常: ${deviceAddress}")
                    null
                }

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "读取RSSI过程中发生异常: ${deviceAddress}")
                return@withContext null
            }
        }

    abstract suspend fun readDeviceInfo(
        gatt: BluetoothGatt,
        readCharacteristic: BluetoothGattCharacteristic
    )

}