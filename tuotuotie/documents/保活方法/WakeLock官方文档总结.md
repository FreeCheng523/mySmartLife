# Android 唤醒锁（WakeLock）官方文档总结

## 一、唤醒锁概述

### 1.1 定义
唤醒锁（WakeLock）是 Android 系统提供的一种电源管理机制，允许应用程序在特定情况下防止设备进入休眠状态，确保关键任务能够持续执行。

### 1.2 工作原理
- **正常情况下**：Android 设备在闲置一段时间后会自动进入休眠状态，以节省电量
- **使用唤醒锁后**：应用程序可以保持设备的 CPU 或屏幕处于活动状态，防止系统进入休眠
- **适用场景**：播放音乐、导航、下载大文件、蓝牙通信、后台服务等需要持续运行的任务

## 二、唤醒锁类型

根据官方文档，Android 提供了多种类型的唤醒锁：

### 2.1 PARTIAL_WAKE_LOCK（部分唤醒锁）
- **特点**：保持 CPU 活跃，允许屏幕和键盘关闭
- **适用场景**：后台服务、网络请求、数据处理等不需要屏幕亮起的任务
- **电量影响**：中等（仅保持 CPU 运行）
- **代码示例**：
```kotlin
val wakeLock = powerManager.newWakeLock(
    PowerManager.PARTIAL_WAKE_LOCK,
    "MyApp::MyWakelockTag"
)
```

### 2.2 SCREEN_DIM_WAKE_LOCK（屏幕调暗唤醒锁）
- **特点**：保持屏幕亮起，但允许调暗
- **适用场景**：需要显示信息但不需要全亮屏幕的场景
- **注意**：在 API 27+ 已废弃

### 2.3 SCREEN_BRIGHT_WAKE_LOCK（屏幕全亮唤醒锁）
- **特点**：保持屏幕全亮
- **适用场景**：需要用户持续关注屏幕的应用
- **注意**：在 API 27+ 已废弃

### 2.4 FULL_WAKE_LOCK（完全唤醒锁）
- **特点**：保持屏幕和键盘全亮
- **适用场景**：需要用户交互的应用
- **注意**：在 API 27+ 已废弃

### 2.5 现代替代方案
对于需要保持屏幕亮起的场景，官方推荐使用：
- `Window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, ...)`
- `View.setKeepScreenOn(true)`

## 三、使用方法

### 3.1 基本使用步骤

#### 步骤 1：获取 PowerManager
```kotlin
val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
```

#### 步骤 2：创建唤醒锁
```kotlin
val wakeLock = powerManager.newWakeLock(
    PowerManager.PARTIAL_WAKE_LOCK,
    "MyApp::MyWakelockTag"  // 标签用于调试和日志
)
```

#### 步骤 3：获取唤醒锁
```kotlin
wakeLock.acquire()  // 或 wakeLock.acquire(timeoutMillis) 带超时
```

#### 步骤 4：执行任务
```kotlin
try {
    // 执行需要保持设备唤醒的任务
    performBackgroundTask()
} finally {
    // 确保释放唤醒锁
    wakeLock.release()
}
```

### 3.2 带超时的获取方式
```kotlin
// 设置超时时间（毫秒），超时后自动释放
wakeLock.acquire(30 * 60 * 1000L)  // 30分钟超时
```

### 3.3 检查唤醒锁状态
```kotlin
if (wakeLock.isHeld) {
    // 唤醒锁正在持有中
    wakeLock.release()
}
```

## 四、最佳实践

### 4.1 最小化持有时间
- ✅ **正确做法**：仅在执行关键任务时获取，任务完成后立即释放
- ❌ **错误做法**：长时间持有唤醒锁，即使没有任务在执行

### 4.2 使用 try-finally 确保释放
```kotlin
wakeLock.acquire()
try {
    // 执行任务
    performTask()
} finally {
    // 无论成功或失败都释放
    if (wakeLock.isHeld) {
        wakeLock.release()
    }
}
```

### 4.3 设置合理的超时时间
```kotlin
// 根据任务预计完成时间设置超时
wakeLock.acquire(estimatedTaskDurationMillis)
```

### 4.4 使用有意义的标签
```kotlin
// ✅ 好的标签：包含包名、类名或方法名
"com.example.app:MyService::ConnectTask"

// ❌ 不好的标签：过于简单
"wakelock"
```

### 4.5 优先使用替代方案
在以下场景，优先考虑不使用唤醒锁：
- **定期任务**：使用 `WorkManager` 或 `JobScheduler`
- **延迟任务**：使用 `AlarmManager`
- **保持屏幕亮起**：使用 `FLAG_KEEP_SCREEN_ON`
- **网络请求**：使用 `WorkManager` 的网络约束

### 4.6 在服务中使用
对于后台服务，建议：
- 在 `onCreate()` 中初始化唤醒锁对象
- 在需要时获取，任务完成后释放
- 在 `onDestroy()` 中确保释放所有唤醒锁

## 五、权限要求

### 5.1 声明权限
在 `AndroidManifest.xml` 中声明：
```xml
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

### 5.2 运行时检查（Android 6.0+）
```kotlin
if (checkSelfPermission(android.Manifest.permission.WAKE_LOCK) 
    != PackageManager.PERMISSION_GRANTED) {
    // 处理权限缺失的情况
    return
}
```

## 六、注意事项

### 6.1 电量消耗
- ⚠️ **警告**：唤醒锁会显著增加电池消耗
- 持有时间越长，电量消耗越大
- 应谨慎使用，避免滥用

### 6.2 必须成对使用
- 每次 `acquire()` 必须对应一次 `release()`
- 避免重复释放（可能导致异常）
- 使用 `isHeld` 检查状态

### 6.3 生命周期管理
- 在 Activity/Service 销毁时确保释放
- 避免在静态变量中持有唤醒锁
- 考虑使用超时机制作为安全网

### 6.4 线程安全
- 唤醒锁是线程安全的
- 可以在不同线程中获取和释放
- 但建议在同一上下文中管理

### 6.5 API 级别兼容性
- `PARTIAL_WAKE_LOCK`：所有 API 级别都支持
- 屏幕相关的唤醒锁在 API 27+ 已废弃
- 使用 `Window.setFlags()` 作为替代

## 七、实际应用示例

### 7.1 在 BleService 中的应用
```kotlin
class BleService : Service() {
    private lateinit var wakeLock: PowerManager.WakeLock
    
    override fun onCreate() {
        super.onCreate()
        // 初始化唤醒锁
        initWakeLock()
    }
    
    private fun initWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "LingDong:BleServiceWakeLock"
        )
    }
    
    private fun acquireWakeLock() {
        try {
            if (checkSelfPermission(android.Manifest.permission.WAKE_LOCK) 
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
            wakeLock.acquire(30 * 60 * 1000L) // 30分钟超时
        } catch (e: Exception) {
            // 处理异常
        }
    }
    
    private fun releaseWakeLock() {
        try {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        } catch (e: Exception) {
            // 处理异常
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock() // 确保释放
    }
}
```

### 7.2 使用场景
1. **蓝牙连接任务**：确保连接过程不被中断
2. **定期唤醒任务**：定期检查设备连接状态
3. **后台数据处理**：处理设备事件和数据同步

## 八、总结

### 8.1 关键要点
1. ✅ 唤醒锁用于防止设备休眠，确保关键任务完成
2. ✅ `PARTIAL_WAKE_LOCK` 是最常用的类型，适合后台服务
3. ✅ 必须成对使用 `acquire()` 和 `release()`
4. ✅ 使用 try-finally 确保释放
5. ✅ 设置合理的超时时间
6. ✅ 最小化持有时间，减少电量消耗

### 8.2 官方建议
- 优先考虑使用 `WorkManager`、`JobScheduler` 等现代 API
- 仅在必要时使用唤醒锁
- 遵循最佳实践，减少对电池的影响
- 使用有意义的标签便于调试

### 8.3 相关文档链接
- **PowerManager 类参考**：https://developer.android.com/reference/android/os/PowerManager
- **WakeLock 使用指南**：https://developer.android.com/training/scheduling/wakelock
- **后台工作文档**：https://developer.android.com/develop/background-work

---

*本文档基于 Android 官方文档整理，适用于 Android 开发最佳实践。*

