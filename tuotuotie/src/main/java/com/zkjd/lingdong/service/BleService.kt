package com.zkjd.lingdong.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.zkjd.lingdong.R
import com.mine.baselibrary.bluetooth.ScanState
import com.zkjd.lingdong.model.ButtonType
import com.zkjd.lingdong.model.ConnectionState
import com.zkjd.lingdong.model.Device
import com.zkjd.lingdong.repository.DeviceEvent
import com.zkjd.lingdong.repository.DeviceRepository
import com.zkjd.lingdong.repository.SettingsRepository
import com.mine.baselibrary.bluetooth.BluetoothConnectionBroadcastReceiver
import com.mine.baselibrary.window.ToastUtilOverApplication
import com.smartlife.fragrance.data.model.FragranceDevice
import com.smartlife.fragrance.repository.FragranceRepository
import com.smartlife.fragrance.service.FragranceCarExecutor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

private const val TAG = "BleService"
private const val NOTIFICATION_ID = 1001
private const val CHANNEL_ID = "ble_service_channel"

/**
 * 蓝牙后台服务，维持与设备的连接并处理按键事件
 */
@AndroidEntryPoint
class BleService : Service() {

    companion object{
        fun startService(context: Context){
            try {
                val serviceIntent = Intent(context, BleService::class.java)
                // 根据Android版本选择合适的启动方式
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }catch (e: Throwable){
                Timber.tag(TAG).e(e,"start service fail")
            }
        }
    }
    
    @Inject
    lateinit var deviceRepository: DeviceRepository
    
    @Inject
    lateinit var functionExecutor: FunctionExecutor

    @Inject
    lateinit var settingsRepository: SettingsRepository

    //
    @Inject
    lateinit var fragranceRepository : FragranceRepository

    @Inject
    lateinit var fragranceCarExecutor: FragranceCarExecutor

    // 协程作用域
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 本地绑定器
    private val binder = LocalBinder()
    
    // 通知管理器
    private lateinit var notificationManager: NotificationManager
    
    // 连接的设备数量
    private var connectedDevicesCount = 0
    
    // 定期唤醒任务
    private var periodicWakeupJob: Job? = null
    private val wakeupIntervalMs = 1 * 5 * 1000L // 每10秒唤醒一次
    
    // 唤醒锁
    private lateinit var wakeLock: PowerManager.WakeLock

    private lateinit var soundManager: SoundManager

    // 蓝牙连接状态广播接收器
    private lateinit var bluetoothConnectionReceiver: BluetoothConnectionBroadcastReceiver

    // 扫描状态
    private val _scanState = MutableStateFlow<ScanState>(ScanState.NotScanning)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    /**
     * 本地绑定器类
     */
    inner class LocalBinder : Binder() {
        fun getService(): BleService = this@BleService
    }


    private var isCan: Boolean=true

    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).w("服务创建")
        
        // 初始化唤醒锁
        initWakeLock()
        
        // 初始化通知
        initNotification()
        
        // 监听设备状态和事件
        monitorDeviceStates()

        // 服务启动时连接所有保存的设备
        serviceScope.launch {
            acquireWakeLock()
            try {
                connectAllSavedDevices(true)
            } finally {
                releaseWakeLock()
            }
        }

        // 启动定期唤醒任务
        startPeriodicWakeup()

        soundManager = SoundManager(applicationContext)

        soundManager.loadSound(R.raw.click_sound)
        soundManager.loadSound(R.raw.dong)
        soundManager.loadSound(R.raw.dack)

        // 初始化蓝牙连接状态广播接收器
        initBluetoothConnectionReceiver()



        settingsRepository.useMusic
            .onEach { theme ->
                isCan =theme
            }
            .launchIn(serviceScope)

        fragranceCarExecutor.listenIsIgnition{
            isIgnition ->
            serviceScope.launch {
                if(!isIgnition){
                    Log.i(TAG,"下点主动断开所有设备")
                    deviceRepository.getAllDevicesAsList().forEach {
                        try {
                            deviceRepository.disconnectDevice(it.macAddress)
                        } catch (e: Exception) {
                            Log.e(TAG,"deviceRepository",e)
                        }
                    }
                    fragranceRepository.getAllDevicesList().forEach {
                        try {
                            fragranceRepository.disconnectDevice(it.macAddress)
                        }catch (e:Exception){
                            Log.e(TAG,"deviceRepository",e)
                        }
                    }
                }
            }
        }

//        val activityManager: ActivityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
//
//        val runningTasks = activityManager.getRunningTasks(1)
//
//        val packageName = runningTasks[0].topActivity!!.packageName
    }

    private fun processDevices(devices: List<BluetoothDevice>) {
        serviceScope.launch {
            for (device in devices) {
                // 检查设备是否存在
                val deviceNow=deviceRepository.getDeviceByMacAddress(device.address)
                if (deviceNow!=null && deviceNow.needAutoConnect ) {
                    // 不存在的设备，停止扫描并尝试连接

                    if (!isBluetoothDeviceConnected(this@BleService, device.address)) {
                        Timber.tag(TAG).e("重新连接中："+device.name)
                        deviceRepository.connectDevice(device)
                        Thread.sleep(2000)
                    }
                }
            }
        }
    }

    /**
     * 初始化唤醒锁
     */
    private fun initWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "LingDong:BleServiceWakeLock"
        )
    }

    /**
     * 初始化蓝牙连接状态广播接收器
     */
    private fun initBluetoothConnectionReceiver() {
        bluetoothConnectionReceiver = BluetoothConnectionBroadcastReceiver()
        
        // 配置要监听的事件类型 - 只监听蓝牙适配器状态变化
        bluetoothConnectionReceiver.configureEvents(
            setOf(
                BluetoothConnectionBroadcastReceiver.Companion.BroadcastEventType.ADAPTER_STATE_CHANGED
            )
        )
        
        // 添加回调监听器（立即生效，无延迟）
        bluetoothConnectionReceiver.addListener { intent ->
            Timber.tag(TAG).w("收到蓝牙适配器广播事件: ${intent.action}")
            handleBluetoothConnectionEvent(intent)
        }
        
        // 注册广播接收器
        bluetoothConnectionReceiver.register(this)
        
        Timber.tag(TAG).w("蓝牙连接状态广播接收器已初始化并注册")
    }

    /**
     * 处理蓝牙连接状态广播事件 - 只监听蓝牙适配器状态变化
     */
    private fun handleBluetoothConnectionEvent(intent: Intent) {
        when (intent.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
                Timber.tag(TAG).w("系统广播：蓝牙适配器状态变化: $state")
                
                when (state) {
                    BluetoothAdapter.STATE_OFF -> {
                        Timber.tag(TAG).w("蓝牙已关闭")
                        // 蓝牙关闭时，更新所有设备状态为断开
                        serviceScope.launch {
                            val devices = deviceRepository.getAllDevices().map { it.toList() }.first()
                            val devicesFragrances = fragranceRepository.getAllDevices().map { it.toList() }.first()
                            for (device in devices) {
                                deviceRepository.updateConnectionState(device.macAddress, ConnectionState.DISCONNECTED)
                            }
                            for(fragrance in devicesFragrances){
                                fragranceRepository.updateConnectionState(fragrance.macAddress,
                                    com.smartlife.fragrance.data.model.ConnectionState.DISCONNECTED)
                            }
                            Timber.tag(TAG).w("已更新所有设备状态为断开")
                        }
                    }
                    else -> {
                        // 其他状态不处理
                        Timber.tag(TAG).d("蓝牙适配器状态: $state (不处理)")
                    }
                }
            }
        }
    }
    
    /**
     * 初始化通知
     */
    private fun initNotification() {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }
    
    /**
     * 监听设备状态和事件
     */
    private fun monitorDeviceStates() {
        // 监听设备连接状态
        serviceScope.launch {
            deviceRepository.getAllDevices()
                .map { devices -> devices.count { it.lastConnectionState == ConnectionState.CONNECTED } }
                .distinctUntilChanged()
                .collect { count ->
                    connectedDevicesCount = count
                    updateNotification()
                }
        }
        
        // 监听设备事件
        serviceScope.launch {
            deviceRepository.getDeviceEvents().collect { event ->
                handleDeviceEvent(event)
            }
        }
        
        // 监听设备配置变更
        serviceScope.launch {
            deviceRepository.getAllDevices().collect { devices ->
                for (device in devices) {
//                    if (device.lastConnectionState == ConnectionState.DISCONNECTED) {
//                        // 检测到设备断开，立即尝试重连
//                        tryConnectDevice(device)
//                    }
                }
            }
        }
    }
    //协助测试
    @SuppressLint("WrongConstant", "ShowToast")
    private fun showToast(message: String){
        Thread {
            // 子线程中的操作...

            // 切换到主线程显示 Toast
            Handler(Looper.getMainLooper()).post {
                CustomToast.show(this@BleService,message,500)
                //Toast.makeText(this@BleService, message,500).show()
            }
        }.start()
    }

    object CustomToast {
        private var toast: Toast? = null
        private val handler = Handler(Looper.getMainLooper())

        fun show(context: Context, message: String, durationMillis: Long) {
            // 取消之前的 Toast
            cancel()

            // 创建新的 Toast
            toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)

            // 显示 Toast
            toast?.show()

            // 设置自定义时长
            handler.postDelayed({
                cancel()
            }, durationMillis)
        }

        fun cancel() {
            toast?.cancel()
            toast = null
        }
    }
    /**
     * 处理设备事件
     */
    private suspend fun handleDeviceEvent(event: DeviceEvent) {
        when (event) {
            is DeviceEvent.ButtonPressed -> {
                Timber.tag(TAG).w("按键事件：${event.buttonType}")
                if(event.buttonType==ButtonType.LEFT_ROTATE ) {
                    handleButtonPressed2(event)
                } else if(event.buttonType==ButtonType.RIGHT_ROTATE) {
                    handleButtonPressed2(event)
                } else{
                    handleButtonPressed(event)
                }

//                handleButtonPressed(event)
            }
            is DeviceEvent.DeviceConnected -> {
                Timber.tag(TAG).w("设备已连接: ${event.macAddress}")
                setupDeviceAfterConnect(event.macAddress)
            }
            is DeviceEvent.DeviceDisconnected -> {
                Timber.tag(TAG).w("设备已断开: ${event.macAddress}")
                // 检测到断开连接，快速尝试重连
                serviceScope.launch {
                    delay(500) // 短暂延迟，避免立即重连
                    val device = deviceRepository.getDeviceByMacAddress(event.macAddress)
                    device?.let {
                        if(device.needAutoConnect) {
                            Timber.tag(TAG).w("尝试快速重连设备: ${event.macAddress}")
                            tryConnectDevice(it)
                        }
                    }
                }

            }
            is DeviceEvent.ConnectionFailed -> {
                Timber.tag(TAG).e("设备连接失败: ${event.macAddress}, 原因: ${event.reason}")
            }
            is DeviceEvent.BatteryLevelChanged -> {
                Timber.tag(TAG).w("设备电量变化: ${event.macAddress}, 电量: ${event.level}%")
            }
            is DeviceEvent.DeviceReady -> {
                Timber.tag(TAG).w("设备已准备好: ${event.macAddress}")
                // 设备完全准备就绪，设置LED颜色和防误触模式
                setupDeviceConfiguration(event.macAddress)
            }

            is DeviceEvent.AuthFailed -> {
                //
            }
            is DeviceEvent.AuthSuccess -> {
                //
            }

            else -> {}
        }
    }
    
    /**
     * 设备连接后的初始化设置
     */
    private fun setupDeviceAfterConnect(macAddress: String) {
        // 连接后的基础设置，不包括特征配置
        Timber.tag(TAG).w("设备 $macAddress 已连接，正在进行初始化...")
    }
    
    /**
     * 设备完全准备就绪后的配置（通知已启用）
     */
    private fun setupDeviceConfiguration(macAddress: String) {
        serviceScope.launch {
            try {
                // 设置LED颜色
                deviceRepository.setDeviceLedColor(macAddress, true)
                deviceRepository.setDeviceLedColor(macAddress, false)
                
                // 设置防误触模式
                deviceRepository.setDeviceAntiMisoperation(macAddress)
                
                Timber.tag(TAG).w("设备 $macAddress 配置已完成")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "设置设备配置时出错: $macAddress")
            }
        }
    }
    
    /**
     * 启动定期唤醒任务
     */
    private fun startPeriodicWakeup() {
        periodicWakeupJob?.cancel()
        periodicWakeupJob = serviceScope.launch {
            while (isActive) {
                wakeupAndConnectAllDevices()
                delay(wakeupIntervalMs)
            }
        }
    }
    
    /**
     * 唤醒并连接所有已保存的设备
     */
    private suspend fun wakeupAndConnectAllDevices() {
        Timber.tag(TAG).w("执行定期唤醒和连接...")

        //showToast("执行后台服务打开1")

        try {
            acquireWakeLock()
            connectAllSavedDevices(false)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "唤醒和连接过程中出错")
        } finally {
            releaseWakeLock()
        }
    }
    
    /**
     * 获取唤醒锁
     */
    private fun acquireWakeLock() {
        try {
            if (checkSelfPermission(android.Manifest.permission.WAKE_LOCK) 
                != PackageManager.PERMISSION_GRANTED) {
                Timber.tag(TAG).e("缺少WAKE_LOCK权限")
                return
            }
            
            wakeLock.acquire(30*60*1000L) // 设置30分钟超时
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "获取唤醒锁失败")
        }
    }
    
    /**
     * 释放唤醒锁
     */
    private fun releaseWakeLock() {
        try {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "释放唤醒锁时出错")
        }
    }
    
    /**
     * 尝试连接设备
     */
    private fun tryConnectDevice(device: Device) {
        if(!device.needAutoConnect){
            android.util.Log.e(TAG,"不需要自动重连")
            return
        }
        // 避免重复连接已连接或正在连接的设备
        if (device.lastConnectionState != ConnectionState.DISCONNECTED) {
            android.util.Log.e(TAG,"not DISCONNECTED ${device.lastConnectionState}")
            return
        }

        val macAddress = device.macAddress
        try {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            
            if (!bluetoothAdapter.isEnabled) {
                Timber.tag(TAG).e("蓝牙已禁用，无法连接设备")
                //showToast("蓝牙未打开，无法连接设备")
                return
            }
            
            val bleDevice = bluetoothAdapter.getRemoteDevice(macAddress)
            
            if (bleDevice != null) {
                Timber.tag(TAG).w("正在连接设备: ${device.name} (${macAddress})")
//                showToast("正在连接设备: ${device.name} (${macAddress})2")

                serviceScope.launch {
                    try {
                        deviceRepository.connectDevice(bleDevice)
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "连接设备时出错: $macAddress")
//                        showToast("连接设备时出错:（ $macAddress）2")
                                            }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "尝试连接设备时出错: $macAddress")
//            showToast("尝试连接设备时出错:（ $macAddress）2")

        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).w("服务启动")
        //showToast("服务启动")

        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification())
        

        
        return START_STICKY
    }
    
    /**
     * 连接所有已断开的设备（包括妥妥贴设备和香氛设备）
     */
    private suspend fun connectAllSavedDevices(force:Boolean) {
        try {

            Log.i(TAG,"connectAllSavedDevices $force")

            if(!fragranceCarExecutor.isIgnition()){
                Log.i(TAG,"未点火")
                return
            }

            // 获取蓝牙适配器
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter

            if (!bluetoothAdapter.isEnabled) {
                Timber.tag(TAG).e("蓝牙已禁用，无法连接设备")
               // showToast("蓝牙未打开，无法连接设备")

                return
            }

            // ========== 处理妥妥贴设备 ==========
            // 获取所有已保存的妥妥贴设备
            val devices = deviceRepository.getAllDevices().map { it.toList() }.first()
//            Timber.tag(TAG).w("找到 ${devices.size} 个已保存的设备")
            if (!force) {
                if (devices.size > 0) {
                    for (item in devices) {
                        if (!isBluetoothDeviceConnected(this@BleService, item.macAddress)) {
                            deviceRepository.updateConnectionState(
                                item.macAddress,
                                ConnectionState.DISCONNECTED
                            )
                        }
                    }
                }
            }
            // 过滤出未连接的设备
            var disconnectedDevices = deviceRepository.getAllDevices().map { it.toList() }.first()
            if (!force) {
                disconnectedDevices =
                    disconnectedDevices.filter { it.lastConnectionState == ConnectionState.DISCONNECTED }
            }

            if (disconnectedDevices.isNotEmpty()) {
                Timber.tag(TAG).w("发现 ${disconnectedDevices.size} 个断开的妥妥贴设备需要连接")

                // 尝试连接每个断开的设备
                for (device in disconnectedDevices) {
                    Timber.tag(TAG).i("device info ${device.macAddress} , needAutoConnect: ${device.needAutoConnect}")
                    if(!device.needAutoConnect){
                        Timber.tag(TAG).i("不需要自动重连")
                        continue
                    }
                   // Timber.tag(TAG).w("正在连接断开的设备: ${device.name} (${device.macAddress})")
                    //showToast("正在连接断开的设备: ${device.name} (${device.macAddress})")

                    var blueDevice: BluetoothDevice? = null
                    blueDevice = bluetoothAdapter.getRemoteDevice(device.macAddress)
                    if (blueDevice != null) {
                        deviceRepository.connectDevice(blueDevice)
                        // 给每个设备连接请求留出一些时间
                        delay(500)
                    } else {
                        Timber.tag(TAG).w("设备：%s,%s 连接失败", device.name, device.macAddress)
                        //showToast("设备：${device.name},${device.macAddress} 连接失败")

                    }

                }
            } else {
                Timber.tag(TAG).w("所有妥妥贴设备都已连接，无需再次连接")
            }
            
            // ========== 处理香氛设备 ==========
            // 获取所有香氛设备
            val allFragranceDevicesList = fragranceRepository.getAllDevicesList()
            var connectedFragranceDevice: List<FragranceDevice>
            var disconnectedFragranceDevices :List<FragranceDevice>
            if (force) {
                //如果是强制再次连接，那么这里我们认为所有香氛都是断开的
                connectedFragranceDevice = emptyList<FragranceDevice>()
                disconnectedFragranceDevices = allFragranceDevicesList
            } else {
                //如果不是强制连接，那么过滤出连接/未连接的香氛
                connectedFragranceDevice = allFragranceDevicesList.filter {
                    it.connectionState == com.smartlife.fragrance.data.model.ConnectionState.CONNECTED
                }
                disconnectedFragranceDevices =
                    allFragranceDevicesList.filter {
                        it.connectionState == com.smartlife.fragrance.data.model.ConnectionState.DISCONNECTED
                    }
            }

            if(connectedFragranceDevice.isNotEmpty()){
                Timber.tag(TAG).w("有连接上的香氛,同步氛围灯")
                serviceScope.launch {
                    fragranceCarExecutor.syncAtmosphereLight(connectedFragranceDevice)
                }
            }else{
                Timber.tag(TAG).w("没有连接上的香氛")
            }
            
            if (disconnectedFragranceDevices.isNotEmpty()) {
                Timber.tag(TAG).w("发现 ${disconnectedFragranceDevices.size} 个断开的香氛设备需要连接")
                
                // 尝试连接每个断开的香氛设备
                for (device in disconnectedFragranceDevices) {
                    Timber.tag(TAG).i("fragrance device info ${device.macAddress} , needAutoConnect: ${device.needAutoConnect}, device.syncLightBrightness ${device.syncLightBrightness},force $force")

                    //手动断开不重连
                    if(!device.needAutoConnect){
                        continue
                    }

                    //香氛没有设置同步氛围灯且force为false，不重连
                    if(!device.syncLightBrightness && !force){
                        Timber.tag(TAG).i("香氛设备不需要自动重连")
                        continue
                    }

                    var blueDevice: BluetoothDevice? = null
                    blueDevice = bluetoothAdapter.getRemoteDevice(device.macAddress)
                    if (blueDevice != null) {
                        fragranceRepository.connectDevice(blueDevice)
                        // 给每个设备连接请求留出一些时间
                        delay(500)
                    } else {
                        Timber.tag(TAG).w("香氛设备：%s,%s bluetoothAdapter获取device失败", device.deviceName, device.macAddress)
                    }
                }
            } else {
                Timber.tag(TAG).w("所有香氛设备都已连接，无需再次连接")
            }


        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "连接已保存设备时出错")
//            showToast("连接已保存设备时出错")

        }
    }

    //判断当前妥妥贴设备mac地址是否已经连接
    suspend fun isBluetoothDeviceConnected(context: Context, macAddress: String): Boolean {
        // 获取 BluetoothManager
//        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//
//        // 获取当前已连接的 GATT 设备（包括 BLE 设备）
//        val connectedDevices = bluetoothManager.getConnectedDevices(android.bluetooth.BluetoothProfile.GATT)
        val connectedDevices=deviceRepository.getAllBluetoothDevice()

        // 检查目标 MAC 地址的设备是否在已连接列表中
        return connectedDevices.any { it.address.equals(macAddress, ignoreCase = true) }
    }
    
    //判断当前香氛设备mac地址是否已经连接
    suspend fun isFragranceDeviceConnected(context: Context, macAddress: String): Boolean {
        val connectedDevices = fragranceRepository.getAllBluetoothDevice()
        // 检查目标 MAC 地址的设备是否在已连接列表中
        return connectedDevices.any { it.address.equals(macAddress, ignoreCase = true) }
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onDestroy() {
        Timber.tag(TAG).w("服务销毁")
        
        // 注销蓝牙连接状态广播接收器
        if (::bluetoothConnectionReceiver.isInitialized) {
            bluetoothConnectionReceiver.clearListeners()
            bluetoothConnectionReceiver.unregister(this)
            Timber.tag(TAG).w("蓝牙连接状态广播接收器已注销")
        }
        
        // 取消定期唤醒任务
        periodicWakeupJob?.cancel()
        
        // 释放唤醒锁
        releaseWakeLock()
        
        // 取消所有协程
        serviceScope.cancel()
        
        super.onDestroy()

        soundManager.release()
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.service_notification_title),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "妥妥贴后台服务通知"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 创建通知
     */
    private fun createNotification(): Notification {
    /*    val pendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }*/
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text))
            .setSmallIcon(R.drawable.ic_bluetooth)
            //.setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
            
        notification.flags = notification.flags or Notification.FLAG_FOREGROUND_SERVICE
        
        return notification
    }
    private fun showNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bluetooth)
            .setContentTitle("应用通知")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
    /**
     * 更新通知
     */
    private fun updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }
    
    /**
     * 处理按键事件
     */
    private suspend fun handleButtonPressed(event: DeviceEvent.ButtonPressed) {
        Timber.tag(TAG).w("收到按键事件: ${event.macAddress}, 按键: ${event.buttonType}")

        // 获取按键对应的功能
        var function = deviceRepository.getButtonFunction(event.macAddress, event.buttonType)

        // 右旋和左旋使用同样的功能定义
        if (event.buttonType == ButtonType.RIGHT_ROTATE) {
            function = deviceRepository.getButtonFunction(event.macAddress, ButtonType.LEFT_ROTATE)
        }

        val device=deviceRepository.getDeviceByMacAddress(event.macAddress)

        if(device!=null)
        {
            if(device.musicCan==1) {
                when (device.musicName) {
                    "1" -> soundManager.playSound(R.raw.click_sound)
                    "2" -> soundManager.playSound(R.raw.dong)
                    "3" -> soundManager.playSound(R.raw.dack)
                }
            }
        }
        // 执行功能
        function?.let {
            // 传入功能和按键类型，特别是对于旋转类型的功能需要知道旋转方向
            functionExecutor.executeFunction(it, event.buttonType, event.macAddress)
        } ?: run {
            Timber.tag(TAG).w("按键未配置功能: ${event.buttonType}")
            toastNoFunction(event)
        }
    }

    private fun toastNoFunction(event: DeviceEvent.ButtonPressed) {
        if (event.buttonType in listOf(
                ButtonType.SHORT_PRESS,
                ButtonType.DOUBLE_CLICK,
                ButtonType.LEFT_ROTATE,
                ButtonType.RIGHT_ROTATE
            )
        ) {
            ToastUtilOverApplication().showToast(
                applicationContext, when (event.buttonType) {
                    ButtonType.SHORT_PRESS -> "单击功能尚未设置"
                    ButtonType.DOUBLE_CLICK -> "双击功能尚未设置"
                    ButtonType.LEFT_ROTATE, ButtonType.RIGHT_ROTATE -> "旋钮功能尚未设置"
                    else -> "功能尚未设置"
                }
            )
        }
    }

    // 旋转次数
    private var leftRotate = 0

    /**
     * 处理按键事件
     */
    private suspend fun handleButtonPressed2(event: DeviceEvent.ButtonPressed) {
        Timber.tag(TAG).w("收到按键事件: ${event.macAddress}, 按键: ${event.buttonType}")

        // 获取按键对应的功能
        var function = deviceRepository.getButtonFunction(event.macAddress, event.buttonType)

        // 右旋和左旋使用同样的功能定义
        if (event.buttonType == ButtonType.RIGHT_ROTATE) {
            function = deviceRepository.getButtonFunction(event.macAddress, ButtonType.LEFT_ROTATE)
        }

        val device=deviceRepository.getDeviceByMacAddress(event.macAddress)

        if(device!=null)
        {
            leftRotate++


            val targetTime = LocalDateTime.now().plusNanos(250)
            println("将在 ${targetTime} 发送消息...")

            // 2. 转换为Date类型（Timer需要Date参数）
            val targetDate = Date.from(targetTime.atZone(java.time.ZoneId.systemDefault()).toInstant())

            // 3. 创建定时器，到达目标时间执行发送
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    for (i in 1..leftRotate) {
                       Thread.sleep(50)
                        if(i<2)
                        {
                            if(device.musicCan==1) {
                                when (device.musicName) {
                                    "1" -> soundManager.playSound(R.raw.click_sound)
                                    "2" -> soundManager.playSound(R.raw.dong)
                                    "3" -> soundManager.playSound(R.raw.dack)
                                }
                            }
                        }else{
                            if(i==2){
                                leftRotate=0
                                break
                            }
                        }
                    }
                }
            }, targetDate)


        }
        // 执行功能
        function?.let {
            // 传入功能和按键类型，特别是对于旋转类型的功能需要知道旋转方向
            functionExecutor.executeFunction(it, event.buttonType, event.macAddress)
        } ?: run {
            toastNoFunction(event = event)
            Timber.tag(TAG).w("按键未配置功能: ${event.buttonType}")
        }
    }
}