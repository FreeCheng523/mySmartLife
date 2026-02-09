# FragranceManager 快速参考卡片

## 🚀 快速开始

### 1️⃣ 基本集成（已完成）
```java
// 在 onCreate() 中调用
fragranceSetting();
```

### 2️⃣ 扫描设备
```java
// 点击"添加设备"按钮时自动启动
mFragranceManager.startScan();
```

### 3️⃣ 连接设备
```java
// 点击设备卡片时
mFragranceManager.connectToDevice(macAddress);
```

### 4️⃣ 删除设备
```java
// 删除设备时
mFragranceManager.deleteDevice(macAddress);
```

## 📋 设备类型

| 设备 | deviceType | 说明 |
|-----|-----------|------|
| 小仪表 | 1 | USB 设备 |
| 中控按键 | 2 | 车载设备 |
| 妥妥贴 | 3 | 蓝牙设备 |
| **香薰** | **4** | **蓝牙设备**（新增） |

## 🔍 关键方法

### MainActivity 中的方法

| 方法名 | 说明 | 调用时机 |
|-------|-----|---------|
| `fragranceSetting()` | 初始化香薰管理器 | onCreate() |
| `showFragranceConnectedWindowView()` | 显示香薰设备界面 | 点击设备卡片 |
| `onDeviceRemoved()` | 删除设备 | 长按删除 |

### FragranceManager 方法

| 方法名 | 说明 | 参数 |
|-------|-----|-----|
| `startScan()` | 开始扫描 | 无 |
| `stopScan()` | 停止扫描 | 无 |
| `connectToDevice()` | 连接设备 | macAddress |
| `deleteDevice()` | 删除设备 | macAddress |

## 📊 数据流

```
扫描 → FragranceManager.onDeviceAdded 
    → 检查去重 
    → 转换为 AgileSmartDevice 
    → addNewDevice() 
    → 保存到 SP + 更新 UI
```

## 🐛 调试技巧

### 查看日志
```bash
adb logcat | grep -E "MainActivity|FragranceManager"
```

### 关键日志
```
扫描到香薰设备: [...]
香薰设备已存在，跳过: XX:XX:XX:XX:XX:XX
添加香薰设备: AgileSmartDevice(...)
showFragranceConnectedWindowView: 连接中
```

## ⚠️ 注意事项

1. **deviceType 必须是 4**
2. **MAC 地址存储在 deviceId 字段**
3. **UI 更新使用 runOnUiThread()**
4. **检查 Activity 生命周期状态**

## 📝 TODO

- [ ] 实现香薰设备设置对话框
- [ ] 添加设备状态实时同步
- [ ] 优化连接失败处理
- [ ] 添加设备图标

## 📚 完整文档

- [集成说明](./FragranceManager集成说明.md)
- [测试清单](./FragranceManager集成测试清单.md)
- [完成总结](./FragranceManager集成完成总结.md)

---
**最后更新：** 2025-11-11

