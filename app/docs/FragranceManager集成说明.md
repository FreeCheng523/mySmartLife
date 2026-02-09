# FragranceManager 集成说明

## 概述
本文档说明了如何在 `MainActivity` 中集成 `FragranceManager`，参考了 `LingDongTieManager` 的实现方式。

## 集成步骤

### 1. 添加依赖和导入

在 `MainActivity.java` 的顶部添加以下导入：

```java
import com.smartlife.fragrance.bridge.FragranceManager;
import com.smartlife.fragrance.data.model.FragranceDevice;
import com.smartlife.fragrance.ui.connect.ConnectViewModel;
import com.smartlife.fragrance.ui.scanning.ScanningViewModel;
import com.smartlife.fragrance.ui.status.DeviceStatusViewModel;
```

### 2. 添加成员变量

在 `MainActivity` 类中添加香薰管理器成员变量：

```java
private FragranceManager mFragranceManager;
```

### 3. 初始化 FragranceManager

在 `onCreate()` 方法中调用 `fragranceSetting()` 方法进行初始化：

```java
lingDongtieSetting();
fragranceSetting();  // 新增
```

### 4. 实现 fragranceSetting() 方法

参考 `lingDongtieSetting()` 的实现方式，创建 `fragranceSetting()` 方法：

**主要功能包括：**

#### 4.1 初始化 ViewModel
```java
com.smartlife.fragrance.ui.scanning.ScanningViewModel fragranceScanningViewModel = 
    new ViewModelProvider(this).get(com.smartlife.fragrance.ui.scanning.ScanningViewModel.class);

ConnectViewModel fragranceConnectViewModel = 
    new ViewModelProvider(this).get(ConnectViewModel.class);

DeviceStatusViewModel fragranceDeviceStatusViewModel = 
    new ViewModelProvider(this).get(DeviceStatusViewModel.class);
```

#### 4.2 创建 FragranceManager 实例

使用构造函数创建 `FragranceManager`，需要提供以下回调：

- **onDeviceAdded**: 处理扫描到的香薰设备
  - 从 SharedPreferences 读取已保存的设备列表
  - 检查设备是否已存在（防止重复添加）
  - 将新设备转换为 `AgileSmartDevice` 并添加到列表
  - 设备类型设置为 `4`（表示香薰设备）

- **connectedResult**: 处理连接结果
  - 主要处理连接失败的情况
  - 显示失败提示

- **needOpenBluetooth**: 需要打开蓝牙时的回调
  - 显示蓝牙对话框

### 5. 在设备点击事件中处理香薰设备

在 `onDeviceItemClick()` 方法的 switch 语句中添加：

```java
case 4: // 香薰设备
    showFragranceConnectedWindowView(device, action);
    break;
```

### 6. 实现设备交互方法

#### showFragranceConnectedWindowView()
```java
public void showFragranceConnectedWindowView(SmartDevice device, int action) {
    String deviceMac = device.getDeviceId();
    if (action == 1 && device.getConnectStatus() == 1) { // 未连接
        mFragranceManager.connectToDevice(device.getDeviceId());
    } else if (action == 3 && device.getConnectStatus() == 3) { // 已连接
        // TODO: 显示香薰设备设置对话框
    }
}
```

### 7. 在设备删除时处理

在 `onDeviceRemoved()` 方法中添加：

```java
else if(device.getDeviceType()==4) {//香薰设备
    mFragranceManager.deleteDevice(device.getDeviceId());
}
```

### 8. 在扫描对话框中启动香薰扫描

在 `showAddDeviceDialog()` 方法中：

**启动扫描：**
```java
mLingDongTieManager.startScan();
if (mFragranceManager != null) {
    mFragranceManager.startScan();
}
```

**停止扫描：**
```java
mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
    @Override
    public void onDismiss(DialogInterface dialog) {
        mLingDongTieManager.stopScan();
        if (mFragranceManager != null) {
            mFragranceManager.stopScan();
        }
    }
});
```

## 设备类型定义

在系统中，各种设备类型的定义如下：

| 设备类型 | deviceType 值 | 说明 |
|---------|---------------|------|
| 小仪表 | 1 | USB 连接的小仪表设备 |
| 中控物理按键 | 2 | 车辆中控物理按键 |
| 妥妥贴（灵动帖） | 3 | 蓝牙连接的妥妥贴设备 |
| 香薰设备 | 4 | 蓝牙连接的香薰设备 |

## 数据流程

### 扫描流程
1. 用户点击"添加设备"按钮
2. 同时启动妥妥贴和香薰设备扫描
3. `FragranceManager` 内部监听扫描结果
4. 扫描到设备后通过 `onDeviceAdded` 回调通知 MainActivity
5. MainActivity 检查设备是否已存在
6. 将新设备添加到列表并保存到 SharedPreferences

### 连接流程
1. 用户点击未连接的香薰设备卡片
2. 调用 `mFragranceManager.connectToDevice(macAddress)`
3. `FragranceManager` 内部处理连接逻辑
4. 连接成功后，设备状态会自动更新（通过 `DeviceStatusViewModel`）
5. 连接失败时，通过 `connectedResult` 回调通知 MainActivity

### 状态同步
- 香薰设备状态由 `FragranceManager` 内部的 Room 数据库管理
- `DeviceStatusViewModel.devices` Flow 提供实时状态更新
- MainActivity 需要监听状态变化并同步更新 UI（待实现）

## 待实现功能

1. **设备状态同步监听**
   - 监听 `DeviceStatusViewModel.devices` Flow
   - 将香薰设备状态同步到 `smartDeviceList`
   - 更新 Adapter 显示

2. **设备设置对话框**
   - 参考 `LightSetDialog` 和 `LinkSetDialog`
   - 创建香薰设备的设置界面
   - 实现功能控制（开关、模式、档位、灯光等）

3. **连接失败处理优化**
   - 参考妥妥贴的连接失败弹窗
   - 提供重试功能

## 注意事项

1. **线程安全**：
   - 扫描结果回调在子线程中执行，UI 操作需要使用 `runOnUiThread()`
   - 访问共享数据（如 `smartDeviceList`）时需要同步

2. **设备去重**：
   - 使用 MAC 地址作为唯一标识
   - 同时检查 SharedPreferences 和内存列表

3. **生命周期管理**：
   - `FragranceManager` 内部使用 `lifecycle.repeatOnLifecycle()` 管理协程
   - 在 Activity 销毁时会自动清理资源

4. **权限处理**：
   - 香薰设备扫描需要蓝牙权限
   - 使用与妥妥贴相同的权限申请流程

## 对比 LingDongTieManager

| 特性 | LingDongTieManager | FragranceManager |
|-----|-------------------|------------------|
| 实现语言 | Java (Builder 模式) | Kotlin (构造函数) |
| 设备类型 | 妥妥贴 (deviceType=3) | 香薰 (deviceType=4) |
| 连接方式 | 蓝牙 BLE | 蓝牙 BLE |
| 数据存储 | Room 数据库 | Room 数据库 |
| 回调方式 | Builder 链式调用 | 构造函数参数 |
| 设备状态监听 | setDeviceChangedCallback | DeviceStatusViewModel.devices Flow |

## 示例代码位置

完整的实现代码请参考：
- `app/src/main/java/com/deepal/ivi/hmi/smartlife/MainActivity.java` (第 448-593 行)
- `fragrance/src/main/java/com/smartlife/fragrance/bridge/FragranceManager.kt`


