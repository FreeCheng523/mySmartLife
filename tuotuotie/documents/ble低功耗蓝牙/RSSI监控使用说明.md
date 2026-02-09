# RSSI监控功能使用说明

## 功能概述

RSSI（Received Signal Strength Indicator，接收信号强度指示）监控功能可以帮助您主动判断蓝牙设备是否已经断开连接。当设备信号强度低于设定阈值或无法读取RSSI时，系统会自动断开设备连接。

## 主要特性

1. **自动启动**：设备鉴权成功后自动开启RSSI监控（默认配置：每5秒检查，阈值-85dBm）
2. **定期监控**：以固定间隔读取设备的RSSI值
3. **智能断开**：连续3次RSSI低于阈值才断开，避免因短暂信号波动误判
4. **容错机制**：信号恢复正常后自动重置弱信号计数
5. **自动停止**：设备断开时自动停止RSSI监控
6. **事件通知**：实时通知RSSI变化和断开事件
7. **可配置参数**：支持自定义监控间隔和RSSI阈值

## API说明

### 1. 开始RSSI监控

```kotlin
fun startRssiMonitoring(
    device: BluetoothDevice,      // 要监控的蓝牙设备
    intervalMs: Long = 5000L,      // 监控间隔（毫秒），默认5秒
    rssiThreshold: Int = -85       // RSSI阈值（dBm），默认-85dBm
)
```

**参数说明：**

- `device`：要监控的蓝牙设备对象
- `intervalMs`：监控间隔时间，单位毫秒，建议5000-10000ms
- `rssiThreshold`：RSSI阈值，单位dBm，当RSSI低于此值时断开连接
  - 典型值：-85dBm（较严格）到-100dBm（较宽松）
  - 值越大表示要求信号越强

### 2. 停止RSSI监控

```kotlin
fun stopRssiMonitoring(device: BluetoothDevice)
```

停止对指定设备的RSSI监控。

### 3. 停止所有RSSI监控

```kotlin
fun stopAllRssiMonitoring()
```

停止所有正在进行的RSSI监控任务。

### 4. 手动读取RSSI

```kotlin
suspend fun readRssi(device: BluetoothDevice): Int?
```

手动读取设备的RSSI值，返回RSSI值（dBm）或null（读取失败）。

## 事件监听

监听设备事件流以接收RSSI相关的事件：

```kotlin
bleManager.getDeviceEvents().collect { event ->
    when (event) {
        // RSSI值变化
        is DeviceEvent.RssiChanged -> {
            val device = event.device
            val rssi = event.rssi
            Log.d(TAG, "设备 ${device.address} RSSI: ${rssi}dBm")
        }

        // 因RSSI过弱导致断开
        is DeviceEvent.RssiWeakDisconnected -> {
            val device = event.device
            val rssi = event.rssi
            Log.w(TAG, "设备 ${device.address} 因信号过弱断开，RSSI: ${rssi}dBm")
        }

        else -> { /* 其他事件 */ }
    }
}
```

## 使用示例

### 基本使用（自动启动）

**注意：从当前版本开始，RSSI监控会在设备鉴权成功后自动启动，无需手动调用！**

```kotlin
class DeviceManager @Inject constructor(
    private val bleManager: BleManager
) {
    init {
        // 监听设备事件
        lifecycleScope.launch {
            bleManager.getDeviceEvents().collect { event ->
                handleDeviceEvent(event)
            }
        }
    }

    // 连接设备（RSSI监控会自动启动）
    suspend fun connectDevice(device: BluetoothDevice) {
        val connected = bleManager.connect(device)
        if (connected) {
            Log.d(TAG, "设备连接成功，RSSI监控已自动启动")
            // RSSI监控已自动启动，默认配置：
            // - 监控间隔：5秒
            // - RSSI阈值：-85dBm
        }
    }

    // 断开设备（RSSI监控会自动停止）
    suspend fun disconnectDevice(device: BluetoothDevice) {
        bleManager.disconnect(device)
        // RSSI监控会自动停止，无需手动调用
    }

    private fun handleDeviceEvent(event: DeviceEvent) {
        when (event) {
            is DeviceEvent.RssiChanged -> {
                // 更新UI显示信号强度
                updateSignalStrength(event.device, event.rssi)
            }

            is DeviceEvent.RssiWeakDisconnected -> {
                // 提示用户设备因信号弱断开
                showToast("设备 ${event.device.name} 信号过弱已断开")
            }

            else -> { /* 处理其他事件 */ }
        }
    }
}
```

### 手动控制（可选）

如果需要自定义监控参数或手动控制，仍可以使用API：

```kotlin
// 手动启动，自定义参数
suspend fun connectAndCustomMonitor(device: BluetoothDevice) {
    val connected = bleManager.connect(device)
    if (connected) {
        // 先停止自动启动的监控
        bleManager.stopRssiMonitoring(device)

        // 使用自定义参数重新启动
        bleManager.startRssiMonitoring(
            device = device,
            intervalMs = 3000L,  // 自定义间隔
            rssiThreshold = -75   // 自定义阈值
        )
    }
}
```

### 车载场景示例

在车载环境中，默认的自动监控已经足够使用。如需根据车辆状态调整监控策略：

```kotlin
// 车辆熄火或用户离开时，加强RSSI监控
fun onVehicleParked() {
    connectedDevices.forEach { device ->
        // 停止默认监控，使用更严格的配置
        bleManager.stopRssiMonitoring(device)

        // 使用较短间隔和较严格阈值
        bleManager.startRssiMonitoring(
            device = device,
            intervalMs = 3000L,      // 3秒检查一次
            rssiThreshold = -75      // 信号强度要求更高
        )
    }
}

// 车辆启动时恢复默认监控
fun onVehicleStarted() {
    connectedDevices.forEach { device ->
        // 停止严格监控
        bleManager.stopRssiMonitoring(device)

        // 恢复默认配置
        bleManager.startRssiMonitoring(
            device = device,
            intervalMs = 5000L,
            rssiThreshold = -85
        )
    }
}
```

### 自适应RSSI监控

根据设备类型和场景调整监控参数：

```kotlin
fun startAdaptiveRssiMonitoring(device: BluetoothDevice) {
    val (interval, threshold) = when {
        // 重要设备：频繁检查，严格阈值
        isImportantDevice(device) -> Pair(3000L, -75)

        // 普通设备：标准检查
        else -> Pair(5000L, -85)
    }

    bleManager.startRssiMonitoring(
        device = device,
        intervalMs = interval,
        rssiThreshold = threshold
    )
}
```

## 连续检测机制（防抖逻辑）

为了避免因短暂的信号波动而误判断开，系统采用**连续检测机制**：

### 工作原理

```
时间轴：  T1      T2      T3      T4      T5
RSSI:    -90     -88     -92     -80     -90
阈值:    -85     -85     -85     -85     -85
计数:     1       2       3    [断开]   (重置)

示例1：连续3次信号弱 → 断开
T1: -90dBm < -85 → 计数=1 (继续监控)
T2: -88dBm < -85 → 计数=2 (继续监控)
T3: -92dBm < -85 → 计数=3 (触发断开)

示例2：中间恢复正常 → 重置计数
T1: -90dBm < -85 → 计数=1 (继续监控)
T2: -80dBm > -85 → 计数重置为0 (信号恢复)
T3: -88dBm < -85 → 计数=1 (重新开始计数)
```

### 优势

- ✅ **避免误判**：短暂的信号波动不会导致断开
- ✅ **快速响应**：持续弱信号能在15秒内（3次×5秒）检测到
- ✅ **自动恢复**：信号改善后自动重置，无需人工干预
- ✅ **稳定性高**：适合车载等可能有短暂干扰的环境

## RSSI值参考

| RSSI范围         | 信号质量 | 说明     |
| -------------- | ---- | ------ |
| -30 ~ -50 dBm  | 优秀   | 设备非常接近 |
| -50 ~ -70 dBm  | 良好   | 正常工作距离 |
| -70 ~ -85 dBm  | 一般   | 边缘区域   |
| -85 ~ -100 dBm | 较弱   | 即将断开   |
| < -100 dBm     | 极弱   | 无法稳定连接 |

## 注意事项

1. **自动启动机制**：
   
   - ✅ 设备鉴权成功后自动启动RSSI监控
   - ✅ 设备断开连接时自动停止RSSI监控
   - ✅ 默认配置：5秒间隔，-85dBm阈值
   - ℹ️ 如需自定义参数，可以手动停止后重新启动

2. **监控间隔**：
   
   - 不建议设置过短的间隔（< 2秒），避免频繁读取影响性能
   - 建议范围：3000ms - 10000ms
   - 默认值：5000ms

3. **RSSI阈值**：
   
   - 车载环境建议：-85dBm 到 -75dBm
   - 阈值过高可能导致误判断开
   - 阈值过低可能无法及时检测断开
   - 默认值：-85dBm

4. **自动断开机制（智能防抖）**：
   
   - **信号弱断开**：连续3次RSSI低于阈值才断开（避免短暂波动误判）
   - **自动恢复**：如果中间RSSI恢复正常，计数器会自动重置
   - **读取失败断开**：连续3次读取RSSI失败会自动断开设备（判定设备已离线）

5. **资源管理**：
   
   - ✅ 断开设备时会自动停止监控，无需手动调用
   - ✅ 应用退出时会自动清理所有监控任务
   - ℹ️ 如需临时暂停监控，可调用 `stopRssiMonitoring()`

6. **权限要求**：
   
   - 需要 `BLUETOOTH_CONNECT` 权限（Android 12+）
   - 设备必须处于已连接状态

## 故障排查

### RSSI读取失败

**可能原因：**

- 设备已断开连接
- 缺少蓝牙权限
- 系统蓝牙堆栈繁忙

**解决方案：**

- 检查设备连接状态
- 确认权限已授予
- 增加监控间隔

### 误判断开（已优化）

**系统已内置防抖机制**：

- ✅ 连续3次信号弱才断开，不会因短暂波动误判
- ✅ 信号恢复正常后自动重置计数

**如仍然出现误判：**

- 降低RSSI阈值（如从-75调整为-85，增加容忍度）
- 增加监控间隔（如从3秒调整为5秒或10秒）
- 检查环境是否有强烈电磁干扰（如微波炉、大功率设备）

## 完整集成示例

```kotlin
@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val bleManager: BleManager
) : ViewModel() {

    private val _rssiState = MutableStateFlow<Map<String, Int>>(emptyMap())
    val rssiState: StateFlow<Map<String, Int>> = _rssiState.asStateFlow()

    init {
        // 监听设备事件
        viewModelScope.launch {
            bleManager.getDeviceEvents().collect { event ->
                when (event) {
                    is DeviceEvent.Connected -> {
                        // 连接成功后启动RSSI监控
                        bleManager.startRssiMonitoring(
                            device = event.device,
                            intervalMs = 5000L,
                            rssiThreshold = -85
                        )
                    }

                    is DeviceEvent.RssiChanged -> {
                        // 更新RSSI状态
                        _rssiState.value = _rssiState.value.toMutableMap().apply {
                            put(event.device.address, event.rssi)
                        }
                    }

                    is DeviceEvent.RssiWeakDisconnected -> {
                        // 处理信号弱断开
                        handleWeakSignalDisconnection(event.device, event.rssi)
                    }

                    is DeviceEvent.Disconnected -> {
                        // 设备断开，移除RSSI状态
                        _rssiState.value = _rssiState.value.toMutableMap().apply {
                            remove(event.device.address)
                        }
                    }
                }
            }
        }
    }

    private fun handleWeakSignalDisconnection(device: BluetoothDevice, rssi: Int) {
        Log.w(TAG, "设备 ${device.address} 因RSSI过弱断开: ${rssi}dBm")
        // 可以在这里添加重连逻辑或用户提示
    }

    override fun onCleared() {
        super.onCleared()
        // ViewModel销毁时停止所有监控
        bleManager.stopAllRssiMonitoring()
    }
}
```

## 更新日志

- **2025-10-16 v1.2**：优化版本，添加智能防抖机制
  
  - ✅ **连续检测机制**：连续3次RSSI低于阈值才断开，避免短暂波动误判
  - ✅ **自动恢复计数**：信号恢复正常后自动重置弱信号计数
  - ✅ 提高车载环境下的稳定性
  - 📊 更详细的日志输出，便于问题诊断

- **2025-10-16 v1.1**：增强版本，添加自动启动功能
  
  - ✅ 设备鉴权成功后自动启动RSSI监控
  - ✅ 设备断开时自动停止RSSI监控
  - ✅ 默认配置优化（5秒间隔，-85dBm阈值）
  - ℹ️ 仍支持手动控制和自定义参数

- **2025-10-16 v1.0**：初始版本，添加RSSI监控功能
  
  - 支持自定义监控间隔和阈值
  - 支持连续失败检测
  - 支持自动断开弱信号设备
