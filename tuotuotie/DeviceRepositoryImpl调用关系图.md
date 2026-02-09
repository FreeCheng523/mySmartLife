# DeviceRepositoryImpl 调用关系图

## 📋 概述

`DeviceRepositoryImpl` 是设备存储库的实现类，负责管理设备数据、蓝牙连接和事件流转。

## 🏗️ 类结构

### 实现的接口
```kotlin
DeviceRepositoryImpl : DeviceRepository
```

### 依赖注入（通过构造函数）
```kotlin
@Inject constructor(
    private val deviceDao: DeviceDao,                    // 设备数据访问对象
    private val buttonFunctionMappingDao: ButtonFunctionMappingDao,  // 按键功能映射DAO
    private val bleManager: TuoTuoTieAbsBleManager,      // 蓝牙管理器
    @ApplicationContext private val context: Context     // Android上下文
)
```

## 📊 调用关系图

```
┌─────────────────────────────────────────────────────────────┐
│                    DeviceRepositoryImpl                      │
│  (设备存储库实现类 - 数据层与业务层的桥梁)                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ 实现
                              ▼
                    ┌─────────────────────┐
                    │  DeviceRepository   │
                    │    (接口定义)        │
                    └─────────────────────┘
                              │
                              │ 被注入到
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      UI层/ViewModel层                        │
│  (通过 Hilt 注入 DeviceRepository，实际使用 DeviceRepositoryImpl) │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│              DeviceRepositoryImpl 依赖关系                    │
└─────────────────────────────────────────────────────────────┘

    ┌──────────────────┐
    │   DeviceDao      │◄──────────── 数据库操作
    │   (设备DAO)      │   - getDevice()
    └──────────────────┘   - getAllDevices()
           ▲                - insertDevice()
           │                - updateDevice()
           │                - deleteDevice()
           │                - updateConnectionState()
           │                - updateBatteryLevel()
           │                - updateDeviceName()
           │                - 等等...
           │
           │ 注入
           │
    ┌──────────────────────────────────────┐
    │  DeviceRepositoryImpl                │
    │                                      │
    │  ┌──────────────────────────────┐   │
    │  │  ButtonFunctionMappingDao    │   │◄── 按键功能映射
    │  │  (按键功能映射DAO)            │   │   - getMappingForDeviceAndButton()
    │  └──────────────────────────────┘   │   - setMapping()
    │                                      │   - deleteMappingForDeviceAndButton()
    │  ┌──────────────────────────────┐   │
    │  │  TuoTuoTieAbsBleManager      │   │◄── 蓝牙管理
    │  │  (蓝牙管理器)                 │   │   - initialize()
    │  └──────────────────────────────┘   │   - startScan()
    │                                      │   - stopScan()
    │  ┌──────────────────────────────┐   │   - connect()
    │  │  Context                     │   │   - disconnect()
    │  │  (Android上下文)              │   │   - getDeviceEvents()
    │  └──────────────────────────────┘   │   - getDisconnectRequestEvents()
    │                                      │   - getTuoTuoTieDeviceEvents()
    └──────────────────────────────────────┘
```

## 🔄 事件流

### 输入事件流（监听）

#### 1. 断开连接请求事件
```kotlin
bleManager.getDisconnectRequestEvents().collect { event ->
    disconnectDevice(event.deviceAddress)
}
```

#### 2. 蓝牙设备事件（来自 baselibrary）
```kotlin
bleManager.getDeviceEvents().collect { bleEvent ->
    when (bleEvent) {
        is BleDeviceEvent.Connected -> {
            updateConnectionState(...)
            deviceEventsFlow.emit(DeviceEvent.DeviceConnected(...))
        }
        is BleDeviceEvent.Disconnected -> { ... }
        is BleDeviceEvent.DeviceReady -> { ... }
        is BleDeviceEvent.BatteryLevelChanged -> { ... }
        is BleDeviceEvent.ConnectionFailed -> { ... }
        is BleDeviceEvent.AuthSuccess -> { ... }
        is BleDeviceEvent.AuthFailed -> { ... }
    }
}
```

#### 3. TuoTuoTie 设备特定事件
```kotlin
bleManager.getTuoTuoTieDeviceEvents().collect { event ->
    when (event) {
        is TuoTuoTieDeviceEvent.ButtonPressed -> {
            deviceEventsFlow.emit(DeviceEvent.ButtonPressed(...))
        }
        is TuoTuoTieDeviceEvent.SetReadVules -> {
            // 更新设备颜色和防误触状态
        }
    }
}
```

### 输出事件流（发送）

```kotlin
private val deviceEventsFlow = MutableSharedFlow<DeviceEvent>(replay = 0)

override fun getDeviceEvents(): Flow<DeviceEvent> = deviceEventsFlow
```

**事件类型**:
- `DeviceEvent.DeviceConnected`
- `DeviceEvent.DeviceDisconnected`
- `DeviceEvent.DeviceReady`
- `DeviceEvent.ButtonPressed`
- `DeviceEvent.BatteryLevelChanged`
- `DeviceEvent.ConnectionFailed`
- `DeviceEvent.AuthSuccess`
- `DeviceEvent.AuthFailed`

## 📦 主要功能模块

### 1. 设备数据管理
- `getDevice()` - 获取单个设备（Flow）
- `getAllDevices()` - 获取所有设备（Flow）
- `getDeviceByMacAddress()` - 根据MAC地址获取设备
- `addDevice()` - 添加设备
- `updateDevice()` - 更新设备
- `deleteDevice()` - 删除设备

### 2. 蓝牙连接管理
- `startScan()` - 开始扫描
- `stopScan()` - 停止扫描
- `getScanState()` - 获取扫描状态
- `connectDevice()` - 连接设备
- `disconnectDevice()` - 断开设备
- `unpairDevice()` - 取消配对

### 3. 设备属性管理
- `updateConnectionState()` - 更新连接状态
- `updateBatteryLevel()` - 更新电量
- `renameDevice()` - 重命名设备
- `setLedColor()` - 设置LED颜色
- `setAntiMisoperation()` - 设置防误触
- `setPreventAccidental()` - 存储防误触状态
- `setReturnControl()` - 存储返控状态
- `setAutoConnected()` - 设置自动连接
- `setMusicCan()` - 设置音效开关
- `renameMusicID()` - 修改音效名称

### 4. 按键功能管理
- `getButtonFunction()` - 获取按键功能
- `setButtonFunction()` - 设置按键功能
- `clearButtonFunction()` - 清除按键功能
- `refreshDevices()` - 刷新设备数据

### 5. 设备控制
- `setDeviceLedColor()` - 设置设备LED颜色
- `setColor()` - 下发设置颜色
- `setPreventAccid()` - 下发防误触
- `setDeviceAntiMisoperation()` - 设置设备防误触模式

## 🔗 依赖注入配置

### Hilt 模块配置 (`AppModule.kt`)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class BindsModule {
    @Binds
    @Singleton
    abstract fun bindDeviceRepository(
        deviceRepositoryImpl: DeviceRepositoryImpl
    ): DeviceRepository
}
```

### 使用示例

```kotlin
@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository  // 实际注入的是 DeviceRepositoryImpl
) : ViewModel() {
    // 使用 deviceRepository
    val devices = deviceRepository.getAllDevices()
    val deviceEvents = deviceRepository.getDeviceEvents()
}
```

## 📝 辅助工具类

### DatabaseHelper
用于在IO线程执行数据库操作：
```kotlin
DatabaseHelper.executeOnIOThread { 
    deviceDao.getDeviceByMacAddress(macAddress) 
}
```

### FunctionsConfig / DefaultFunctions
用于获取按键功能配置：
```kotlin
val functionsConfig = FunctionsConfig.getInstance(context)
functionsConfig.getFunctionById(functionId) 
    ?: DefaultFunctions.getFunctionById(functionId)
```

## 🎯 数据流向

```
蓝牙硬件 → TuoTuoTieAbsBleManager → DeviceRepositoryImpl → UI层
    ↑                                      ↓
    └──────────────────────────────────────┘
              (设备控制指令)

数据库 ← DeviceDao/ButtonFunctionMappingDao ← DeviceRepositoryImpl
    ↑                                              ↓
    └──────────────────────────────────────────────┘
              (数据查询和更新)
```

## 🔍 关键代码位置

- **接口定义**: `tuotuotie/src/main/java/com/zkjd/lingdong/repository/DeviceRepository.kt`
- **实现类**: `tuotuotie/src/main/java/com/zkjd/lingdong/repository/DeviceRepositoryImpl.kt`
- **依赖注入**: `tuotuotie/src/main/java/com/zkjd/lingdong/di/AppModule.kt`
- **事件定义**: `tuotuotie/src/main/java/com/zkjd/lingdong/repository/DeviceEvent.kt`
- **蓝牙管理器**: `tuotuotie/src/main/java/com/zkjd/lingdong/bluetooth/TuoTuoTieAbsBleManager.kt`
- **数据访问**: `tuotuotie/src/main/java/com/zkjd/lingdong/data/dao/DeviceDao.kt`

