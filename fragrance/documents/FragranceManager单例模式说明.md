# FragranceManager 单例模式说明

## 概述

`FragranceManager` 已改造为**单例模式**，使用 **Builder 模式**进行初始化，确保整个应用中只有一个 `FragranceManager` 实例。

## 设计模式

### 1. 单例模式（Singleton Pattern）

- 保证类只有一个实例
- 提供全局访问点
- 线程安全（使用 `@Volatile` 和 `synchronized`）

### 2. Builder 模式（Builder Pattern）

- 分步骤构建复杂对象
- 参数验证
- 链式调用，代码更清晰

## 使用方式

### 初始化（首次创建）

```kotlin
// 在 MainActivity 的 fragranceSetting() 方法中
FragranceManager.Builder()
    .setLifecycle(getLifecycle())
    .setScanningViewModel(scanningViewModel)
    .setConnectViewModel(connectViewModel)
    .setDeviceStatusViewModel(deviceStatusViewModel)
    .setContext(this)
    .setOnDeviceAddedCallback { devices ->
        // 处理扫描到的设备
    }
    .setConnectedResultCallback { success, macAddress ->
        // 处理连接结果
    }
    .setNeedOpenBluetoothCallback {
        // 需要打开蓝牙
    }
    .build()
```

### 获取实例

```kotlin
// 在任何地方获取单例
val fragranceManager = FragranceManager.getInstance()
if (fragranceManager != null) {
    // 使用 manager
    fragranceManager.startScan()
    fragranceManager.connectToDevice(macAddress)
}
```

### Java 中使用

```java
// 获取单例
FragranceManager fragranceManager = FragranceManager.getInstance();
if (fragranceManager != null) {
    fragranceManager.startScan();
    fragranceManager.connectToDevice(macAddress);
    fragranceManager.stopScan();
    fragranceManager.deleteDevice(macAddress);
}
```

## 核心特性

### 1. 线程安全

```kotlin
companion object {
    @Volatile
    private var instance: FragranceManager? = null
    
    fun getInstance(): FragranceManager? {
        return instance
    }
}
```

- 使用 `@Volatile` 确保多线程可见性
- `synchronized` 同步块保证线程安全
- 双重检查锁定（DCL）模式

### 2. 参数验证

```kotlin
fun build(): FragranceManager {
    requireNotNull(lifecycle) { "Lifecycle must be set" }
    requireNotNull(scanningViewModel) { "ScanningViewModel must be set" }
    requireNotNull(connectViewModel) { "ConnectViewModel must be set" }
    requireNotNull(deviceStatusViewModel) { "DeviceStatusViewModel must be set" }
    requireNotNull(context) { "Context must be set" }
    
    // 创建单例...
}
```

### 3. 可选回调

```kotlin
private val onDeviceAdded: ((List<FragranceDevice>) -> Unit)?
private val connectedResult: ((success: Boolean, macAddress: String) -> Unit)?
private val needOpenBluetooth: (() -> Unit)?

// 使用安全调用
onDeviceAdded?.invoke(devices)
connectedResult?.invoke(false, macAddress)
needOpenBluetooth?.invoke()
```

### 4. 重置功能

```kotlin
companion object {
    /**
     * 重置单例（用于测试或重新初始化）
     */
    fun reset() {
        instance = null
    }
}
```

## Builder 方法列表

| 方法 | 参数 | 是否必须 | 说明 |
|-----|------|---------|------|
| `setLifecycle()` | Lifecycle | ✅ 必须 | Activity 生命周期 |
| `setScanningViewModel()` | ScanningViewModel | ✅ 必须 | 扫描 ViewModel |
| `setConnectViewModel()` | ConnectViewModel | ✅ 必须 | 连接 ViewModel |
| `setDeviceStatusViewModel()` | DeviceStatusViewModel | ✅ 必须 | 状态 ViewModel |
| `setContext()` | Context | ✅ 必须 | 应用上下文 |
| `setOnDeviceAddedCallback()` | (List<FragranceDevice>) -> Unit | ❌ 可选 | 设备添加回调 |
| `setConnectedResultCallback()` | (Boolean, String) -> Unit | ❌ 可选 | 连接结果回调 |
| `setNeedOpenBluetoothCallback()` | () -> Unit | ❌ 可选 | 蓝牙打开回调 |
| `build()` | - | - | 构建单例 |

## 公共方法

| 方法 | 参数 | 返回值 | 说明 |
|-----|------|-------|------|
| `getInstance()` | - | FragranceManager? | 获取单例实例 |
| `reset()` | - | Unit | 重置单例 |
| `startScan()` | - | Unit | 开始扫描设备 |
| `stopScan()` | - | Unit | 停止扫描设备 |
| `connectToDevice()` | macAddress: String | Unit | 连接指定设备 |
| `deleteDevice()` | macAddress: String | Unit | 删除设备 |
| `getScanState()` | - | StateFlow<ScanState> | 获取扫描状态 |
| `getDevices()` | - | StateFlow<List<FragranceDevice>> | 获取设备列表 |

## MainActivity 集成示例

### 初始化

```java
private void fragranceSetting() {
    // 1. 初始化 ViewModel
    ScanningViewModel scanningViewModel = 
        new ViewModelProvider(this).get(ScanningViewModel.class);
    ConnectViewModel connectViewModel = 
        new ViewModelProvider(this).get(ConnectViewModel.class);
    DeviceStatusViewModel deviceStatusViewModel = 
        new ViewModelProvider(this).get(DeviceStatusViewModel.class);

    // 2. 使用 Builder 创建单例
    mFragranceManager = new FragranceManager.Builder()
        .setLifecycle(getLifecycle())
        .setScanningViewModel(scanningViewModel)
        .setConnectViewModel(connectViewModel)
        .setDeviceStatusViewModel(deviceStatusViewModel)
        .setContext(this)
        .setOnDeviceAddedCallback(devices -> {
            // 处理扫描到的设备
            return null;
        })
        .setConnectedResultCallback((success, macAddress) -> {
            // 处理连接结果
            return null;
        })
        .setNeedOpenBluetoothCallback(() -> {
            // 打开蓝牙
            return null;
        })
        .build();
}
```

### 使用单例

```java
// 方式1：使用成员变量（推荐在初始化的 Activity 中）
if (mFragranceManager != null) {
    mFragranceManager.startScan();
}

// 方式2：使用单例访问（推荐在其他地方）
FragranceManager fragranceManager = FragranceManager.getInstance();
if (fragranceManager != null) {
    fragranceManager.startScan();
}
```

## 优势

### 1. 资源节省
- 只创建一个实例，节省内存
- 避免重复初始化 ViewModel

### 2. 状态一致
- 全局共享同一个实例
- 设备状态统一管理

### 3. 易于访问
- 任何地方都可以通过 `getInstance()` 访问
- 无需传递引用

### 4. 生命周期管理
- 与 Activity 生命周期绑定
- 自动管理协程生命周期

### 5. 线程安全
- 多线程环境下安全使用
- 无竞态条件

## 注意事项

### 1. 初始化时机

⚠️ **必须在使用前完成初始化**

```java
// 正确：在 onCreate() 中初始化
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    fragranceSetting(); // 初始化 FragranceManager
}

// 错误：在使用前未初始化
FragranceManager.getInstance().startScan(); // 可能返回 null
```

### 2. 空值检查

⚠️ **始终检查 null**

```java
// 正确：检查 null
FragranceManager manager = FragranceManager.getInstance();
if (manager != null) {
    manager.startScan();
}

// 错误：不检查 null
FragranceManager.getInstance().startScan(); // 可能 NullPointerException
```

### 3. 生命周期

⚠️ **单例会持续存在于整个应用生命周期**

- 如果 Activity 重建，单例不会重新创建
- 如需重新初始化，调用 `FragranceManager.reset()` 后再 `build()`

### 4. 内存泄漏

⚠️ **注意回调中的引用**

```java
// 潜在问题：Lambda 中持有 Activity 引用
.setOnDeviceAddedCallback(devices -> {
    // 这里的 this 引用了 Activity
    this.updateUI(devices);
    return null;
})

// 解决方案：使用 WeakReference 或在 onDestroy 中清理
```

### 5. 测试

⚠️ **测试前重置单例**

```kotlin
@Before
fun setUp() {
    FragranceManager.reset()
}
```

## 与 LingDongTieManager 对比

| 特性 | LingDongTieManager | FragranceManager |
|-----|-------------------|------------------|
| 模式 | Builder 模式（非单例） | Builder + 单例模式 |
| 实例 | 每次创建新实例 | 全局唯一实例 |
| 访问方式 | 成员变量 | getInstance() |
| 内存占用 | 每个 Activity 一个 | 整个应用一个 |
| 适用场景 | 每页独立管理 | 全局统一管理 |

## 迁移指南

### 从旧版本迁移

**旧版本（构造函数）：**
```kotlin
val manager = FragranceManager(
    lifecycle = lifecycle,
    scanningViewModel = scanningViewModel,
    // ...
)
```

**新版本（Builder + 单例）：**
```kotlin
val manager = FragranceManager.Builder()
    .setLifecycle(lifecycle)
    .setScanningViewModel(scanningViewModel)
    // ...
    .build()
```

### 使用单例访问

**旧方式：**
```kotlin
// 需要传递 manager 实例
fun doSomething(manager: FragranceManager) {
    manager.startScan()
}
```

**新方式：**
```kotlin
// 直接获取单例
fun doSomething() {
    FragranceManager.getInstance()?.startScan()
}
```

## 常见问题

### Q1: 为什么 `getInstance()` 返回 null？

**A:** 因为还没有调用 `build()` 初始化。确保在 `onCreate()` 中调用 `fragranceSetting()`。

### Q2: 如何重新初始化单例？

**A:** 调用 `FragranceManager.reset()` 后重新 `build()`。

```java
FragranceManager.reset();
new FragranceManager.Builder()
    // ... 设置参数
    .build();
```

### Q3: 单例会导致内存泄漏吗？

**A:** 如果正确使用生命周期感知组件（如 `repeatOnLifecycle`），不会。但要注意回调中的强引用。

### Q4: 可以在多个 Activity 中使用吗？

**A:** 可以。使用 `getInstance()` 在任何地方访问。但注意 Lifecycle 绑定的是第一次初始化的 Activity。

### Q5: 为什么使用 Builder 模式而不是直接构造？

**A:** Builder 模式提供：
- 参数验证
- 链式调用
- 可选参数
- 更清晰的代码

## 最佳实践

1. ✅ 在 Application 或首个 Activity 的 `onCreate()` 中初始化
2. ✅ 使用 `getInstance()` 访问单例
3. ✅ 始终检查 null
4. ✅ 回调中避免强引用 Activity
5. ✅ 测试前重置单例
6. ❌ 不要在多个地方多次调用 `build()`
7. ❌ 不要假设单例已初始化

## 示例代码

完整示例请参考：
- **FragranceManager.kt**: `fragrance/src/main/java/com/smartlife/fragrance/bridge/FragranceManager.kt`
- **MainActivity.java**: `app/src/main/java/com/deepal/ivi/hmi/smartlife/MainActivity.java` (第 448-594 行)

---

**最后更新：** 2025-11-11  
**版本：** 2.0 (单例模式)


