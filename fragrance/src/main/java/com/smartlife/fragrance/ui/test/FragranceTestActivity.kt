package com.smartlife.fragrance.ui.test

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mine.baselibrary.bluetooth.DeviceEvent
import com.mine.baselibrary.bluetooth.ScanState
import com.smartlife.fragrance.R
import com.smartlife.fragrance.bluetooth.FragranceBleManager
import com.smartlife.fragrance.data.model.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FragranceTestActivity : ComponentActivity() {

    private lateinit var bleManager: FragranceBleManager
    private lateinit var tvStatus: TextView
    private lateinit var tvLog: TextView
    private lateinit var etMacAddress: EditText
    private lateinit var rvDevices: RecyclerView
    private lateinit var deviceAdapter: DeviceAdapter

    private var connectedDevice: BluetoothDevice? = null
    private val devices = mutableListOf<BluetoothDevice>()

    // 权限请求
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            log("所有权限已授予")
            initializeBleManager()
        } else {
            log("权限被拒绝")
            Toast.makeText(this, "需要蓝牙权限才能使用", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        initViews()
        requestPermissions()
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvLog = findViewById(R.id.tvLog)
        etMacAddress = findViewById(R.id.etMacAddress)
        rvDevices = findViewById(R.id.rvDevices)

        // 设置RecyclerView
        deviceAdapter = DeviceAdapter(devices) { device ->
            etMacAddress.setText(device.address)
            connectedDevice = device
            log("选择了设备: ${device.name} (${device.address})")
        }
        rvDevices.layoutManager = LinearLayoutManager(this)
        rvDevices.adapter = deviceAdapter

        // 扫描按钮
        findViewById<Button>(R.id.btnStartScan).setOnClickListener {
            startScan()
        }
        findViewById<Button>(R.id.btnStopScan).setOnClickListener {
            stopScan()
        }

        // 连接按钮
        findViewById<Button>(R.id.btnConnect).setOnClickListener {
            connectDevice()
        }
        findViewById<Button>(R.id.btnDisconnect).setOnClickListener {
            disconnectDevice()
        }

        // 电源控制
        findViewById<Button>(R.id.btnPowerOn).setOnClickListener {
            sendCommand("设置电源: 开") {
                bleManager.setPowerState(getMacAddress(), PowerState.ON)
            }
        }
        findViewById<Button>(R.id.btnPowerOff).setOnClickListener {
            sendCommand("设置电源: 关") {
                bleManager.setPowerState(getMacAddress(), PowerState.OFF)
            }
        }

        // 模式控制
        findViewById<Button>(R.id.btnModeStandby).setOnClickListener {
            sendCommand("设置模式: 待机") {
                bleManager.setMode(getMacAddress(), Mode.STANDBY)
            }
        }
        findViewById<Button>(R.id.btnModeA).setOnClickListener {
            sendCommand("设置模式: 香型A") {
                bleManager.setMode(getMacAddress(), Mode.FRAGRANCE_A)
            }
        }
        findViewById<Button>(R.id.btnModeB).setOnClickListener {
            sendCommand("设置模式: 香型B") {
                bleManager.setMode(getMacAddress(), Mode.FRAGRANCE_B)
            }
        }

        // 档位控制
        findViewById<Button>(R.id.btnGearLow).setOnClickListener {
            sendCommand("设置档位: 低") {
                bleManager.setGear(getMacAddress(), Gear.LOW)
            }
        }
        findViewById<Button>(R.id.btnGearMedium).setOnClickListener {
            sendCommand("设置档位: 中") {
                bleManager.setGear(getMacAddress(), Gear.MEDIUM)
            }
        }
        findViewById<Button>(R.id.btnGearHigh).setOnClickListener {
            sendCommand("设置档位: 高") {
                bleManager.setGear(getMacAddress(), Gear.HIGH)
            }
        }

        // 灯光控制
        findViewById<Button>(R.id.btnLightOff).setOnClickListener {
            sendCommand("设置灯光: 关闭") {
                bleManager.setLightMode(getMacAddress(), LightMode.OFF)
            }
        }
        findViewById<Button>(R.id.btnLightBreath).setOnClickListener {
            sendCommand("设置灯光: 呼吸") {
                bleManager.setLightMode(getMacAddress(), LightMode.BREATH)
            }
        }
        findViewById<Button>(R.id.btnLightRhythm).setOnClickListener {
            sendCommand("设置灯光: 律动") {
                bleManager.setLightMode(getMacAddress(), LightMode.RHYTHM)
            }
        }
        findViewById<Button>(R.id.btnLightFlow).setOnClickListener {
            sendCommand("设置灯光: 流动") {
                bleManager.setLightMode(getMacAddress(), LightMode.FLOW)
            }
        }
        findViewById<Button>(R.id.btnLightAlwaysOn).setOnClickListener {
            sendCommand("设置灯光: 常亮") {
                bleManager.setLightMode(getMacAddress(), LightMode.ALWAYS_ON)
            }
        }

        findViewById<Button>(R.id.btnSetLightColor).setOnClickListener {
            val r = findViewById<EditText>(R.id.etLightColorR).text.toString().toIntOrNull() ?: 0
            val g = findViewById<EditText>(R.id.etLightColorG).text.toString().toIntOrNull() ?: 0
            val b = findViewById<EditText>(R.id.etLightColorB).text.toString().toIntOrNull() ?: 0
            sendCommand("设置灯光颜色: R=$r, G=$g, B=$b") {
                bleManager.setLightColor(getMacAddress(), r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
            }
        }

        findViewById<Button>(R.id.btnSetLightBrightness).setOnClickListener {
            val brightness = findViewById<EditText>(R.id.etLightBrightness).text.toString().toIntOrNull() ?: 50
            sendCommand("设置亮度: $brightness") {
                bleManager.setLightBrightness(getMacAddress(), brightness.coerceIn(0, 100))
            }
        }

        // 其他控制
        findViewById<Button>(R.id.btnRestoreFactory).setOnClickListener {
            sendCommand("恢复出厂设置") {
                bleManager.restoreFactorySettings(getMacAddress())
            }
        }

        findViewById<Button>(R.id.btnSetCarStartStop).setOnClickListener {
            // 先启用随车启停，然后设置周期为周期1
            lifecycleScope.launch {
                val macAddress = getMacAddress()
                if (macAddress.isEmpty()) {
                    Toast.makeText(this@FragranceTestActivity, "请输入MAC地址", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                log("设置随车启停: 启用")
                val success1 = bleManager.setCarStartStopEnabled(macAddress, true)
                if (success1) {
                    log("设置随车启停周期: 周期1 (5分钟工作/10分钟停止)")
                    bleManager.setCarStartStopCycle(macAddress, CarStartStopCycle.CYCLE_1)
                }
            }
        }

        findViewById<Button>(R.id.btnSetTiming).setOnClickListener {
            val minutes = findViewById<EditText>(R.id.etTimingDuration).text.toString().toIntOrNull() ?: 30
            sendCommand("设置定时: $minutes 分钟") {
                bleManager.setTimingDuration(getMacAddress(), minutes)
            }
        }

        // 查询命令
        findViewById<Button>(R.id.btnGetVersion).setOnClickListener {
            sendCommand("获取程序版本") {
                bleManager.getProgramVersion(getMacAddress())
            }
        }
        findViewById<Button>(R.id.btnGetStatus).setOnClickListener {
            sendCommand("获取设备状态") {
                bleManager.getDeviceStatus(getMacAddress())
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.BLUETOOTH)
                add(Manifest.permission.BLUETOOTH_ADMIN)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        val needRequest = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needRequest) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            initializeBleManager()
        }
    }

    private fun initializeBleManager() {
        bleManager = FragranceBleManager(applicationContext)
        bleManager.initialize(applicationContext)

        // 监听扫描状态
        bleManager.getScanState()
            .onEach { state ->
                when (state) {
                    is ScanState.NotScanning -> {
                        updateStatus("未扫描")
                        log("扫描已停止")
                    }
                    is ScanState.Scanning -> {
                        updateStatus("扫描中...")
                        log("开始扫描")
                    }
                    is ScanState.ScanResult -> {
                        updateStatus("扫描到 ${state.devices.size} 个设备")
                        devices.clear()
                        devices.addAll(state.devices)
                        deviceAdapter.notifyDataSetChanged()
                        log("扫描到 ${state.devices.size} 个设备")
                    }
                    is ScanState.ScanFailed -> {
                        updateStatus("${state.reason}")
                        log("扫描失败: ${state.reason}")
                    }
                }
            }
            .launchIn(lifecycleScope)

        // 监听设备事件
        bleManager.getDeviceEvents()
            .onEach { event ->
                when (event) {
                    is DeviceEvent.Connected -> {
                        updateStatus("已连接: ${event.device.address}")
                        connectedDevice = event.device
                        etMacAddress.setText(event.device.address)
                        log("设备已连接: ${event.device.name} (${event.device.address})")
                    }
                    is DeviceEvent.Disconnected -> {
                        updateStatus("已断开: ${event.device.address}")
                        if (connectedDevice?.address == event.device.address) {
                            connectedDevice = null
                        }
                        log("设备已断开: ${event.device.name} (${event.device.address})")
                    }
                    is DeviceEvent.DeviceReady -> {
                        updateStatus("设备就绪: ${event.device.address}")
                        log("设备就绪: ${event.device.address}")
                    }
                    is DeviceEvent.ConnectionFailed -> {
                        updateStatus("连接失败: ${event.reason}")
                        log("连接失败: ${event.device.address} - ${event.reason}")
                    }
                    is DeviceEvent.BatteryLevelChanged -> {
                        log("电池电量变化: ${event.device.address} - ${event.level}%")
                    }
                    is DeviceEvent.AuthSuccess -> {
                        log("鉴权成功: ${event.device.address}")
                    }
                    is DeviceEvent.AuthFailed -> {
                        log("鉴权失败: ${event.device.address} - ${event.reason}")
                    }
                }
            }
            .launchIn(lifecycleScope)

        log("BLE管理器已初始化")
        updateStatus("已初始化")
    }

    private fun startScan() {
        if (!::bleManager.isInitialized) {
            Toast.makeText(this, "BLE管理器未初始化", Toast.LENGTH_SHORT).show()
            return
        }
        devices.clear()
        deviceAdapter.notifyDataSetChanged()
        bleManager.startScan()
        log("开始扫描设备...")
    }

    private fun stopScan() {
        if (!::bleManager.isInitialized) return
        bleManager.stopScan()
        log("停止扫描")
    }

    private fun connectDevice() {
        val macAddress = getMacAddress()
        if (macAddress.isEmpty()) {
            Toast.makeText(this, "请输入MAC地址", Toast.LENGTH_SHORT).show()
            return
        }

        val device = devices.find { it.address == macAddress }
            ?: run {
                // 如果列表中没有，尝试从已连接的设备创建
                if (connectedDevice?.address == macAddress) {
                    Toast.makeText(this, "设备已连接", Toast.LENGTH_SHORT).show()
                    return
                }
                Toast.makeText(this, "未找到设备，请先扫描", Toast.LENGTH_SHORT).show()
                return
            }

        lifecycleScope.launch {
            log("正在连接设备: $macAddress")
            val success = bleManager.connect(device)
            if (!success) {
                log("连接失败: $macAddress")
                Toast.makeText(this@FragranceTestActivity, "连接失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun disconnectDevice() {
        val device = connectedDevice ?: run {
            Toast.makeText(this, "没有已连接的设备", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            log("正在断开设备: ${device.address}")
            bleManager.disconnect(device)
            connectedDevice = null
        }
    }

    private fun getMacAddress(): String {
        return etMacAddress.text.toString().trim()
    }

    private fun sendCommand(action: String, command: suspend () -> Boolean) {
        val macAddress = getMacAddress()
        if (macAddress.isEmpty()) {
            Toast.makeText(this, "请输入MAC地址", Toast.LENGTH_SHORT).show()
            return
        }

        if (connectedDevice?.address != macAddress) {
            Toast.makeText(this, "设备未连接", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            log("发送命令: $action")
            val success = command()
            if (success) {
                log("命令发送成功: $action")
            } else {
                log("命令发送失败: $action")
                Toast.makeText(this@FragranceTestActivity, "命令发送失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStatus(status: String) {
        runOnUiThread {
            tvStatus.text = "状态: $status"
        }
    }

    private fun log(message: String) {
        runOnUiThread {
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val logMessage = "[$timestamp] $message\n"
            tvLog.append(logMessage)
            // 自动滚动到底部
            tvLog.post {
                val scrollView = tvLog.parent as? android.widget.ScrollView
                scrollView?.fullScroll(android.view.View.FOCUS_DOWN)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bleManager.isInitialized) {
            lifecycleScope.launch {
                connectedDevice?.let { bleManager.disconnect(it) }
                bleManager.close()
            }
        }
    }
}

