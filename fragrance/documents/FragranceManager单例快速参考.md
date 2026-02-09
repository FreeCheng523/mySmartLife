# FragranceManager 单例快速参考

## 🚀 快速开始

### 初始化（仅一次）

```java
// 在 MainActivity.fragranceSetting() 中
FragranceManager.Builder()
    .setLifecycle(getLifecycle())
    .setScanningViewModel(scanningViewModel)
    .setConnectViewModel(connectViewModel)
    .setDeviceStatusViewModel(deviceStatusViewModel)
    .setContext(this)
    .setOnDeviceAddedCallback(devices -> { /* ... */ return null; })
    .setConnectedResultCallback((success, mac) -> { /* ... */ return null; })
    .setNeedOpenBluetoothCallback(() -> { /* ... */ return null; })
    .build();
```

### 使用单例

```java
// 方式1：成员变量（初始化的类中）
mFragranceManager.startScan();

// 方式2：getInstance（其他任何地方）
FragranceManager manager = FragranceManager.getInstance();
if (manager != null) {
    manager.startScan();
}
```

## 📋 Builder 方法

### 必须设置 ✅
- `setLifecycle(lifecycle)` - Activity 生命周期
- `setScanningViewModel(vm)` - 扫描 VM
- `setConnectViewModel(vm)` - 连接 VM
- `setDeviceStatusViewModel(vm)` - 状态 VM
- `setContext(context)` - 应用上下文

### 可选设置 ⭕
- `setOnDeviceAddedCallback(callback)` - 设备添加回调
- `setConnectedResultCallback(callback)` - 连接结果回调
- `setNeedOpenBluetoothCallback(callback)` - 蓝牙回调

### 构建
- `build()` - 创建单例（⚠️ 只调用一次）

## 🔧 公共方法

```java
// 获取单例
FragranceManager.getInstance()

// 扫描
manager.startScan()
manager.stopScan()

// 连接/删除
manager.connectToDevice(macAddress)
manager.deleteDevice(macAddress)

// 获取状态
manager.getScanState()
manager.getDevices()

// 重置（测试用）
FragranceManager.reset()
```

## ⚠️ 注意事项

### 1. 始终检查 null
```java
❌ FragranceManager.getInstance().startScan(); // 可能 NPE

✅ FragranceManager manager = FragranceManager.getInstance();
   if (manager != null) {
       manager.startScan();
   }
```

### 2. 只初始化一次
```java
❌ // 多次 build
   new FragranceManager.Builder().build();
   new FragranceManager.Builder().build(); // 不会创建新实例

✅ // 只在 onCreate 中 build 一次
   @Override
   protected void onCreate(Bundle savedInstanceState) {
       fragranceSetting(); // 调用一次
   }
```

### 3. 初始化时机
```java
✅ // 在使用前初始化
   onCreate() -> fragranceSetting() -> build()
   
❌ // 未初始化就使用
   getInstance() // 返回 null
```

## 🎯 使用场景

| 场景 | 代码 |
|-----|------|
| **初始化** | `fragranceSetting()` 中 `build()` |
| **扫描设备** | `FragranceManager.getInstance()?.startScan()` |
| **停止扫描** | `FragranceManager.getInstance()?.stopScan()` |
| **连接设备** | `FragranceManager.getInstance()?.connectToDevice(mac)` |
| **删除设备** | `FragranceManager.getInstance()?.deleteDevice(mac)` |

## 🆚 对比：成员变量 vs 单例

### 使用成员变量（初始化的类中）
```java
// MainActivity.java
private FragranceManager mFragranceManager;

void fragranceSetting() {
    mFragranceManager = new FragranceManager.Builder().build();
}

void someMethod() {
    mFragranceManager.startScan(); // 直接使用
}
```

**优点：** 代码简洁，无需 null 检查（初始化后）  
**缺点：** 只能在当前类使用

### 使用单例（任何地方）
```java
// 任何类中
void someMethod() {
    FragranceManager manager = FragranceManager.getInstance();
    if (manager != null) {
        manager.startScan();
    }
}
```

**优点：** 全局访问，任何地方都能用  
**缺点：** 需要 null 检查

## 📍 MainActivity 中的用法

```java
// 1. 成员变量
private FragranceManager mFragranceManager;

// 2. 初始化（onCreate 调用）
private void fragranceSetting() {
    mFragranceManager = new FragranceManager.Builder()
        // ... 设置参数
        .build();
}

// 3. 在 MainActivity 内使用成员变量
void internalMethod() {
    mFragranceManager.startScan();
}

// 4. 在其他地方使用单例
void otherMethod() {
    FragranceManager manager = FragranceManager.getInstance();
    if (manager != null) {
        manager.startScan();
    }
}
```

## 🔄 生命周期

```
┌─────────────────────────────────────────┐
│  Application 启动                        │
├─────────────────────────────────────────┤
│  MainActivity.onCreate()                 │
│    └─> fragranceSetting()               │
│         └─> FragranceManager.build()    │
│              └─> 单例创建 ✅             │
├─────────────────────────────────────────┤
│  任何地方使用                            │
│    └─> FragranceManager.getInstance()  │
│         └─> 返回单例实例                │
├─────────────────────────────────────────┤
│  Application 关闭                        │
│    └─> 单例销毁                         │
└─────────────────────────────────────────┘
```

## 🐛 常见错误

### 错误1: 忘记初始化
```java
❌ // onCreate 中没有调用 fragranceSetting()
   FragranceManager.getInstance() // 返回 null
```

### 错误2: 不检查 null
```java
❌ FragranceManager.getInstance().startScan() // NPE
```

### 错误3: 多次 build
```java
❌ new FragranceManager.Builder().build() // 第1次
   new FragranceManager.Builder().build() // 第2次（无效）
```

## ✅ 最佳实践

```java
// ✅ 正确的完整流程
public class MainActivity extends BaseActivity {
    private FragranceManager mFragranceManager; // 1. 声明
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragranceSetting(); // 2. 初始化
    }
    
    private void fragranceSetting() {
        mFragranceManager = new FragranceManager.Builder()
            .setLifecycle(getLifecycle())
            .setScanningViewModel(scanningViewModel)
            .setConnectViewModel(connectViewModel)
            .setDeviceStatusViewModel(deviceStatusViewModel)
            .setContext(this)
            .build(); // 3. 创建单例
    }
    
    private void useInMainActivity() {
        mFragranceManager.startScan(); // 4a. 内部使用成员变量
    }
    
    private void useInOtherPlace() {
        FragranceManager manager = FragranceManager.getInstance();
        if (manager != null) {
            manager.startScan(); // 4b. 外部使用单例
        }
    }
}
```

## 📚 相关文档

- [详细说明](./FragranceManager单例模式说明.md)
- [集成指南](../../app/docs/FragranceManager集成说明.md)

---
**版本：** 2.0 (单例)  
**更新：** 2025-11-11

