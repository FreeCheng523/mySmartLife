# FragranceManager 集成测试清单

## 集成完成情况

### ✅ 已完成的集成

1. **导入依赖** ✅
   - 已添加 FragranceManager 相关的导入
   - 包括 FragranceDevice、ConnectViewModel、ScanningViewModel、DeviceStatusViewModel

2. **成员变量** ✅
   - 添加 `mFragranceManager` 成员变量

3. **初始化方法** ✅
   - 实现 `fragranceSetting()` 方法
   - 初始化三个 ViewModel
   - 创建 FragranceManager 实例并配置所有回调

4. **设备添加回调** ✅
   - 实现 `onDeviceAdded` 回调
   - 从 SharedPreferences 读取设备列表
   - 检查设备是否已存在（双重检查）
   - 将新设备添加到列表

5. **连接结果回调** ✅
   - 实现 `connectedResult` 回调
   - 处理连接失败情况

6. **蓝牙打开回调** ✅
   - 实现 `needOpenBluetooth` 回调
   - 调用蓝牙对话框

7. **设备点击事件** ✅
   - 在 `onDeviceItemClick()` 中添加 case 4
   - 实现 `showFragranceConnectedWindowView()` 方法

8. **设备删除** ✅
   - 在 `onDeviceRemoved()` 中添加香薰设备处理

9. **扫描管理** ✅
   - 在 `showAddDeviceDialog()` 中同时启动香薰扫描
   - 在对话框关闭时停止香薰扫描

## 测试步骤

### 1. 扫描测试

**步骤：**
1. 打开应用
2. 确保蓝牙已打开
3. 点击"添加设备"按钮
4. 等待扫描（最多 4 分钟）

**预期结果：**
- 应该能够扫描到附近的香薰设备
- 扫描到的设备会自动添加到设备列表
- 设备类型显示为香薰设备（deviceType=4）
- 初始连接状态为未连接（connectStatus=1）

**日志关键字：**
```
扫描到香薰设备
添加香薰设备
```

### 2. 设备去重测试

**步骤：**
1. 扫描并添加一个香薰设备
2. 关闭添加设备对话框
3. 再次点击"添加设备"进行扫描

**预期结果：**
- 已存在的设备不会重复添加
- 日志中会显示"香薰设备已存在，跳过"

**日志关键字：**
```
香薰设备已存在，跳过: XX:XX:XX:XX:XX:XX (SP中存在: true, 内存中存在: true)
```

### 3. 连接测试

**步骤：**
1. 点击未连接的香薰设备卡片（action=1）
2. 等待连接过程

**预期结果：**
- 调用 `mFragranceManager.connectToDevice()`
- 如果连接成功，设备状态会更新为已连接
- 如果连接失败，会显示 Toast 提示

**日志关键字：**
```
showFragranceConnectedWindowView: 连接中
```

### 4. 已连接设备点击测试

**步骤：**
1. 点击已连接的香薰设备卡片（action=3）

**预期结果：**
- 显示 Toast："香薰设备已连接，待实现设置界面"

**日志关键字：**
```
showFragranceConnectedWindowView: 已连接，显示设置对话框
```

### 5. 设备删除测试

**步骤：**
1. 长按或点击删除按钮删除香薰设备

**预期结果：**
- 设备从列表中移除
- 设备从 SharedPreferences 中移除
- 设备从 Room 数据库中删除
- 调用 `mFragranceManager.deleteDevice()`

### 6. 蓝牙关闭测试

**步骤：**
1. 关闭蓝牙
2. 尝试扫描设备

**预期结果：**
- 显示蓝牙确认对话框
- 提示用户打开蓝牙

### 7. 多设备并发扫描测试

**步骤：**
1. 同时放置多个妥妥贴设备和香薰设备
2. 点击"添加设备"进行扫描

**预期结果：**
- 能够同时扫描到妥妥贴和香薰设备
- 不同类型的设备被正确识别和添加
- 妥妥贴设备 deviceType=3
- 香薰设备 deviceType=4

## 调试信息

### 关键日志点

1. **扫描到设备：**
   ```
   TAG: MainActivity
   扫描到香薰设备: [FragranceDevice(...)] | Thread[...]
   ```

2. **设备已存在：**
   ```
   TAG: MainActivity
   香薰设备已存在，跳过: XX:XX:XX:XX:XX:XX (SP中存在: true/false, 内存中存在: true/false)
   ```

3. **添加设备：**
   ```
   TAG: MainActivity
   添加香薰设备: AgileSmartDevice(...)
   ```

4. **连接设备：**
   ```
   TAG: MainActivity
   showFragranceConnectedWindowView: XX:XX:XX:XX:XX:XX
   showFragranceConnectedWindowView: 连接中
   ```

5. **连接失败：**
   ```
   TAG: FragranceManager
   连接失败: XX:XX:XX:XX:XX:XX, 原因: [reason]
   ```

### 检查点

- [ ] 香薰设备在 SharedPreferences 中正确保存（deviceType=4）
- [ ] 设备 MAC 地址正确存储在 AgileSmartDevice.macAddress
- [ ] 设备连接状态正确更新（1=未连接, 3=已连接）
- [ ] FragranceManager 的扫描和连接方法被正确调用
- [ ] Room 数据库中的设备记录正确创建和删除

## 已知限制

1. **设置界面未实现**
   - 当前点击已连接的香薰设备只显示 Toast
   - 需要后续实现香薰设备设置对话框

2. **设备状态同步待优化**
   - 当前主要依赖 FragranceManager 内部的状态管理
   - 建议添加对 DeviceStatusViewModel.devices Flow 的监听
   - 实现设备状态实时同步到 UI

3. **连接失败处理**
   - 当前只显示简单的 Toast
   - 建议参考妥妥贴的连接失败弹窗
   - 添加重试功能

4. **电池电量**
   - 当前香薰设备电池电量固定为 0
   - 如果设备支持电池电量上报，需要更新此字段

## 后续优化建议

1. **实现设备状态监听**
   ```java
   // 在 fragranceSetting() 中添加
   fragranceDeviceStatusViewModel.getDevices().observe(this, devices -> {
       // 遍历 devices，更新 smartDeviceList 中对应的设备状态
       // 调用 smartDeviceAdapter.notifyDataSetChanged()
   });
   ```

2. **创建香薰设备设置对话框**
   - 参考 `LightSetDialog` 和 `LinkSetDialog`
   - 实现功能控制界面

3. **优化连接失败处理**
   - 创建自定义对话框
   - 添加重试和取消按钮

4. **添加设备图标**
   - 为香薰设备添加专属图标
   - 在 Adapter 中根据 deviceType 显示对应图标

## 参考文件

- **集成说明：** `app/docs/FragranceManager集成说明.md`
- **MainActivity：** `app/src/main/java/com/deepal/ivi/hmi/smartlife/MainActivity.java`
- **FragranceManager：** `fragrance/src/main/java/com/smartlife/fragrance/bridge/FragranceManager.kt`
- **LingDongTieManager 参考：** MainActivity 第 211-446 行


