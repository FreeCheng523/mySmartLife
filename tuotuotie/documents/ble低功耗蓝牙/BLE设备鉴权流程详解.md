# BLE设备鉴权流程详解

本文档详细说明了妥妥贴蓝牙设备的连接和鉴权完整流程。

## 目录

1. [整体流程概览](#整体流程概览)
2. [扫描阶段](#扫描阶段)
3. [连接阶段](#连接阶段)
4. [服务发现阶段](#服务发现阶段)
5. [鉴权阶段](#鉴权阶段)
6. [完整流程图](#完整流程图)
7. [关键机制](#关键机制)

---

## 整体流程概览

BLE设备连接和鉴权流程分为以下几个主要阶段：

```
扫描设备 → 建立连接 → 发现服务 → 设备鉴权 → 连接就绪
```

---

## 扫描阶段

### 流程

```
startScan() 
  ↓
检查蓝牙状态和权限
  ↓
配置扫描参数 (低延迟模式)
  ↓
scanCallback.onScanResult()
  ↓
过滤设备名称 (以DEVICE_NAME_HEAD开头)
  ↓
解析广播数据判断类型:
  - 0x01: 配对广播 → 添加到扫描结果
  - 0x02: 重连广播
  ↓
发送 ScanState.ScanResult
```

### 关键代码位置

- **扫描启动**: `startScan()` (第138-183行)
- **扫描回调**: `scanCallback.onScanResult()` (第423-477行)
- **广播过滤**: 第426行 - 设备名称前缀匹配
- **广播解析**: 第452行 - 判断广播类型

---

## 连接阶段

### 流程

```kotlin
connect(device: BluetoothDevice)
  ↓
检查权限
  ↓
断开现有连接 disconnectExistingConnection()
  ↓
创建 CompletableDeferred<Boolean> 等待连接结果
  ↓
device.connectGatt() 创建GATT连接
  ↓
gattCallback.onConnectionStateChange()
```

### 连接状态处理

```
STATE_CONNECTED
  ↓
设置连接优先级为 HIGH
  ↓
检查是否为已知设备 (discoveredServicesCache)
  ↓
已知设备? 
  - YES → 尝试使用缓存服务
  - NO  → 执行服务发现 gatt.discoverServices()
```

### 关键代码位置

- **连接方法**: `connect()` (第319-357行)
- **连接回调**: `onConnectionStateChange()` (第603-664行)
- **连接优先级**: `setConnectionPriority()` (第1417-1430行)

---

## 服务发现阶段

### 流程

```kotlin
onServicesDiscovered(gatt, status)
  ↓
防抖处理 (根据设备类型设置不同时间限制)
  - LYT设备: 500ms
  - GR设备: 800ms
  - 其他: 1000ms
  ↓
validateRequiredServices() 验证必要服务
  ↓
验证内容:
  ├─ 主服务 (SERVICE_UUID)
  ├─ 写入特征 (CHARACTERISTIC_WRITE_UUID)
  ├─ 通知特征 (CHARACTERISTIC_NOTIFY_UUID)
  └─ 电池服务 (可选)
  ↓
启用通知特征
  ↓
缓存设备特征 cacheDeviceCharacteristics()
  ↓
检查是否启用鉴权 (settingsRepository.enableAuthentication)
```

### 关键代码位置

- **服务发现回调**: `onServicesDiscovered()` (第670-780行)
- **服务验证**: `validateRequiredServices()` (第975-1027行)
- **通知启用**: `enableCharacteristicNotification()` (第1035-1057行)
- **防抖机制**: 第698行 - `getServiceDiscoveryTimeLimit()`

---

## 鉴权阶段

### authenticateDevice 完整流程

#### 第一步：前置检查与准备

```kotlin
// 第1460-1480行
1. 获取设备GATT连接 (deviceGattMap[device.address])
   失败 → 返回 false
2. 获取主服务 (SERVICE_UUID)
   失败 → 返回 false
3. 获取写入特征 (CHARACTERISTIC_WRITE_UUID)
   失败 → 返回 false
4. 创建异步结果容器
   authResult = CompletableDeferred<Boolean>()
   authenticationResults[device.address] = authResult
```

#### 第二步：生成随机盐值

```kotlin
// 第1487行
hostSalt = BleAuthUtils.generateRandomSalt(SALT_LENGTH)
// SALT_LENGTH = 8 字节
```

#### 第三步：存储鉴权信息

```kotlin
// 第1490行
authenticationData[device.address] = AuthData(hostSalt = hostSalt)
```

#### 第四步：构建鉴权请求数据包

```kotlin
// 第1494-1497行
authRequest = ByteArray(2 + 8) // 总共10字节

结构:
[0] = HEADER_AUTH (0x04)        // 鉴权头部标识
[1] = AUTH_REQUEST (0x01)       // 鉴权请求指令
[2-9] = hostSalt (8字节)        // 车机生成的随机盐值
```

#### 第五步：延迟与发送

```kotlin
// 第1503行
Thread.sleep(DELAY_SEND)  // 延迟1000ms
// 原因: 首次连接蓝牙，需要延迟发送数据才能成功

// 第1506行
writeSuccess = writeCharacteristicWithTimeout(
    gatt, 
    writeCharacteristic, 
    authRequest
)

失败? 
  - 清理 authenticationResults
  - 返回 false
```

#### 第六步：等待响应（超时保护）

```kotlin
// 第1514-1522行
try {
    authenticated = withTimeout(WAIT_TO) {  // 5000ms超时
        authResult.await()  // 等待设备响应
    }
} catch (TimeoutCancellationException) {
    authenticated = false  // 超时失败
}
```

#### 第七步：清理与返回

```kotlin
// 第1526-1534行
authenticationResults.remove(device.address)  // 清理鉴权结果

记录日志:
  - 成功: "设备鉴权成功"
  - 失败: "设备鉴权失败"

return authenticated
```

---

### handleAuthResponse 响应处理流程

当设备返回鉴权响应时，会触发 `onCharacteristicChanged` → `handleAuthResponse`：

#### 响应数据包结构

```
// 第1559行开始验证
总长度: 2 + 8 + 6 + 6 = 22 字节

[0]      = HEADER_AUTH (0x04)
[1]      = AUTH_RESPONSE (0x02)
[2-9]    = deviceSalt (8字节)      // 妥妥贴生成的随机盐值
[10-15]  = encryptedAddr (6字节)   // 加密的设备MAC地址
[16-21]  = authData (6字节)        // 认证数据
```

#### 验证步骤

**1. 提取数据（第1566-1579行）**

```kotlin
deviceSalt = data.copyOfRange(2, 10)
encryptedAddr = data.copyOfRange(10, 16)
authData = data.copyOfRange(16, 22)
```

**2. 生成密钥材料（第1589-1593行）**

```kotlin
(iv, key) = BleAuthUtils.generateKeyMaterial(
    hostSalt,      // 车机的盐值
    deviceSalt,    // 妥妥贴的盐值
    AUTH_LABEL     // 标签常量
)
```

**3. 验证PublicAddr（第1598-1620行）**

```kotlin
// 解密设备地址
decryptedAddr = BleAuthUtils.aesDecrypt(encryptedAddr, key, iv)

// 生成期望值
expectedAddr = BleAuthUtils.padDeviceName2(
    deviceName.replace(":", ""),  // 去掉冒号的MAC地址
    PUBLIC_ADDR_LENGTH            // 6字节
)

// 比对（注意：需要反转字节序）
addrMatched = decryptedAddr.contentEquals(expectedAddr.reversedArray())

失败? → sendAuthStatus(gatt, false) → 返回
```

**4. 验证认证数据（第1623-1638行）**

```kotlin
// 生成期望的原始认证数据
expectedRawData = BleAuthUtils.generateRawAuthData(hostSalt, deviceSalt)
                    .copyOfRange(0, 6)
                    .reversedArray()

// 解密收到的认证数据
decryptedAuthData = BleAuthUtils.aesDecrypt(authData, key, iv)

// 比对
dataMatched = decryptedAuthData.contentEquals(expectedRawData)

失败? → sendAuthStatus(gatt, false) → 返回
```

**5. 鉴权成功处理（第1641-1655行）**

```kotlin
// 更新鉴权信息（包含完整的密钥材料）
authenticationData[device.address] = AuthData(
    hostSalt = hostSalt,
    deviceSalt = deviceSalt,
    iv = iv,
    key = key
)

// 发送成功状态给设备
sendAuthStatus(gatt, true)

// 完成异步结果
completeAuthentication(device.address, true)
```

---

### sendAuthStatus 发送鉴权状态

#### 状态数据包结构

```kotlin
// 第1687-1691行
statusResponse = ByteArray(4)

[0] = HEADER_AUTH (0x04)
[1] = AUTH_STATUS_RESPONSE (0x03)
[2] = statusCode 低8位
[3] = statusCode 高8位

statusCode = 
  - 成功: AUTH_SUCCESS
  - 失败: AUTH_FAILED
```

---

## 完整流程图

### 主流程图（包含双层异步）

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              车机端（Host）                                        │
└─────────────────────────────────────────────────────────────────────────────────┘

    主线程                        写入回调线程                     通知回调线程
 (authenticateDevice)        (onCharacteristicWrite)      (onCharacteristicChanged)
                    
┌──────────────────┐
│ 1-3. 准备数据     │
│  生成hostSalt    │
│  构建请求包      │
└────────┬─────────┘
         ↓
┌──────────────────┐
│ 延迟1000ms       │
└────────┬─────────┘
         ↓
┌──────────────────────────────┐
│ 4. writeCharacteristicWith   │
│    Timeout() 内部:           │
├──────────────────────────────┤
│ 4.1 创建写入结果容器          │
│   writeResult = Deferred()   │
│                              │
│ 4.2 保存到Map               │
│   writeCallbacks[uuid]       │
│   = writeResult              │
│                              │
│ 4.3 启动超时任务 ────────────┼────┐ 3秒超时监控
│   launch { delay(3000) }     │    │ (并行执行)
│                              │    └──→ ⟲ 等待3秒
│ 4.4 写入特征 ────────────────┼───────────────────────→ 发送到蓝牙设备
│   gatt.writeCharacteristic() │        (异步，立即返回)
│   (立即返回)                 │
│                              │
│ 4.5 等待写入完成 ⟲           │
│   writeResult.await()        │
│   (阻塞主线程)               │
└────────┬─────────────────────┘
         │ 等待中...                    ↓
         ⋮                       设备收到写入
         ↓                              ↓
      等待中...                   写入完成回调
         ⋮                              ↓
         ↓                    ┌─────────────────────┐
      等待中...               │ onCharacteristicWrite│
         ⋮                    │ (在回调线程中执行)    │
         ↓                    ├─────────────────────┤
      等待中...               │ writeResult         │
         ⋮                    │  .complete(true)    │
         ↓                    └──────────┬──────────┘
         │ ◄──── 唤醒主线程 ──────────────┘
         ↓
┌────────────────────┐
│ writeResult.await()│
│ 返回 true          │
└────────┬───────────┘
         ↓
  写入请求发送成功
         ↓
┌──────────────────────────────┐
│  创建鉴权结果容器             │
│  authResult = Deferred()     │
│  authenticationResults       │
│    [address] = authResult    │
└────────┬─────────────────────┘
         ↓
┌──────────────────────────────┐
│ 等待鉴权响应 ⟲               │
│ withTimeout(5000) {          │
│   authResult.await()         │
│ }                            │
│ (阻塞主线程)                 │
└────────┬─────────────────────┘
         │ 等待中...                                            ↓
         ⋮                                              妥妥贴处理请求
         ↓                                                      ↓
      等待中...                                           生成响应数据
         ⋮                                                      ↓
         ↓                                              返回鉴权响应
      等待中...                                                 ↓
         ⋮                                    ┌──────────────────────────────┐
         ↓                                    │ onCharacteristicChanged      │
      等待中...                               │ (在通知回调线程中执行)        │
         ⋮                                    └──────────┬───────────────────┘
         ↓                                               ↓
      等待中...                               ┌──────────────────────────────┐
         ⋮                                    │ 5. handleAuthResponse        │
         ↓                                    │    synchronized(lock) {      │
      等待中...                               └──────────┬───────────────────┘
         ⋮                                               ↓
         ↓                                    ┌──────────────────────────────┐
      等待中...                               │ 6. 提取数据                  │
         ⋮                                    │  - deviceSalt (8字节)        │
         ↓                                    │  - encryptedAddr (6字节)     │
      等待中...                               │  - authData (6字节)          │
         ⋮                                    └──────────┬───────────────────┘
         ↓                                               ↓
      等待中...                               ┌──────────────────────────────┐
         ⋮                                    │ 7. 生成密钥材料              │
         ↓                                    │  generateKeyMaterial()       │
      等待中...                               │  → (iv, key)                 │
         ⋮                                    └──────────┬───────────────────┘
         ↓                                               ↓
      等待中...                               ┌──────────────────────────────┐
         ⋮                                    │ 8. 验证PublicAddr           │
         ↓                                    │  aesDecrypt(encryptedAddr)  │
      等待中...                               │  → 比对MAC地址               │
         ⋮                                    └──────────┬───────────────────┘
         ↓                                               ↓
      等待中...                                       匹配? ──NO──┐
         ⋮                                                │        │
         ↓                                               YES       │
      等待中...                                           ↓        ↓
         ⋮                                    ┌──────────────────┐ │
         ↓                                    │ 9. 验证认证数据  │ │
      等待中...                               │  aesDecrypt()   │ │
         ⋮                                    │  → 比对哈希值    │ │
         ↓                                    └────────┬─────────┘ │
      等待中...                                        ↓           │
         ⋮                                         匹配? ──NO──────┤
         ↓                                            │            │
      等待中...                                      YES           │
         ⋮                                            ↓            ↓
         ↓                                  ┌────────────┐ ┌────────────┐
      等待中...                             │sendAuthStatus│sendAuthStatus│
         ⋮                                  │  (success)  │  (failed)   │
         ↓                                  └──────┬─────┘ └──────┬─────┘
      等待中...                                    │              │
         ⋮                                         ↓              ↓
         ↓                                  ┌────────────────────────────┐
      等待中...                             │ completeAuthentication()   │
         ⋮                                  │ authResult.complete(result)│
         ↓                                  └──────────┬─────────────────┘
         │ ◄────── 唤醒主线程 ───────────────────────────┘
         ↓
┌────────────────────┐
│ authResult.await() │
│ 返回结果            │
└────────┬───────────┘
         ↓
    success? ──NO──┐
         │         │
        YES        │
         ↓         ↓
   ┌─────────┐ ┌─────────┐
   │连接成功 │ │断开连接 │
   └─────────┘ └─────────┘
```

---

### 双层异步机制详解

```
┌─────────────────────────────────────────────────────────────────────┐
│                     第一层异步：写入特征                              │
└─────────────────────────────────────────────────────────────────────┘

writeCharacteristicWithTimeout() {
    
    ① 创建 writeResult = CompletableDeferred()
    ② 保存 writeCallbacks[uuid] = writeResult
    ③ 启动超时任务 (3秒)
    ④ 调用 gatt.writeCharacteristic() ────→ 异步写入，立即返回
    ⑤ writeResult.await() ⟲ 阻塞等待
                             ↑
                             │
    onCharacteristicWrite() {│
        writeResult.complete()───┘ 唤醒
    }
}

┌─────────────────────────────────────────────────────────────────────┐
│                   第二层异步：等待鉴权响应                            │
└─────────────────────────────────────────────────────────────────────┘

authenticateDevice() {
    
    writeSuccess = writeCharacteristicWithTimeout() ← 第一层异步完成
    
    ① 创建 authResult = CompletableDeferred()
    ② 保存 authenticationResults[address] = authResult
    ③ withTimeout(5秒) {
         authResult.await() ⟲ 阻塞等待
       }                      ↑
                              │
    handleAuthResponse() {    │
        验证数据...           │
        completeAuthentication()
        authResult.complete()──┘ 唤醒
    }
}
```

---

### 完整时间轴

```
时间轴    主线程                    写入回调线程              通知回调线程
─────────┼─────────────────────────┼────────────────────────┼──────────────────
T=0ms    │ 1-3步：准备数据         │                        │
         │                         │                        │
T=0ms    │ 延迟1000ms...           │                        │
         │                         │                        │
T=1000ms │ ┌─ 第一层异步开始 ─┐    │                        │
         │ │ writeCharacteristic   │                        │
         │ │ WithTimeout()         │                        │
         │ │   ↓                   │                        │
T=1001ms │ │ 创建writeResult       │                        │
         │ │   ↓                   │                        │
T=1002ms │ │ gatt.write() ─────────┼──→ 发送              │
         │ │   ↓                   │                        │
T=1003ms │ │ writeResult.await()   │                        │
         │ │   ⟲ 等待...           │                        │
         │ │                       │                        │
T=1010ms │ │          ←────────────┼─ onCharacteristicWrite│
         │ │                       │  complete(true)        │
         │ │   ↓                   │                        │
T=1011ms │ │ 返回true              │                        │
         │ └─ 第一层异步结束 ─┘    │                        │
         │                         │                        │
T=1012ms │ ┌─ 第二层异步开始 ─┐    │                        │
         │ │ 创建authResult        │                        │
         │ │   ↓                   │                        │
T=1013ms │ │ authResult.await()    │                        │
         │ │   ⟲ 等待...           │                        │
         │ │     ⋮                 │                        │
         │ │   等待中...            │                        │
         │ │     ⋮                 │                        │
T=1500ms │ │   等待中...            │              ←─ 妥妥贴响应
         │ │     ⋮                 │                        │
T=1501ms │ │   等待中...            │              onCharacteristicChanged
         │ │     ⋮                 │              handleAuthResponse
         │ │   等待中...            │                   ↓
T=1520ms │ │   等待中...            │              验证数据...
         │ │     ⋮                 │                   ↓
T=1530ms │ │          ←────────────┼───────────── completeAuth()
         │ │                       │              authResult.complete(true)
         │ │   ↓                   │
T=1531ms │ │ 返回true              │
         │ └─ 第二层异步结束 ─┘    │
         │   ↓                     │
T=1532ms │ 连接成功                │
```

---

## 关键机制

### 1. 超时保护机制

| 项目 | 超时时间 | 常量 | 说明 |
|------|---------|------|------|
| **连接超时** | 10秒 | `CONNECTION_TIMEOUT_MS` | 设备连接最大等待时间 |
| **鉴权超时** | 5秒 | `WAIT_TO` | 等待鉴权响应最大时间 |
| **写入超时** | 3秒 | `DELAY_TIMEOUT` | 单次写入操作最大时间 |
| **首次延迟** | 1秒 | `DELAY_SEND` | 首次连接必需的延迟 |

### 2. 防抖机制

#### 服务发现防抖

使用 `lastServiceDiscoveryTime` 记录每个设备最后一次服务发现时间，根据设备类型限制频率：

```kotlin
private fun getServiceDiscoveryTimeLimit(device: BluetoothDevice): Long {
    val deviceName = device.name ?: ""
    
    return when {
        deviceName.startsWith(BleConstants.LYT) -> 500L   // 妥妥贴设备
        deviceName.startsWith(BleConstants.GR) -> 800L    // GR设备
        else -> 1000L                                     // 默认设备
    }
}
```

#### 通知防抖

使用 `lastProcessedNotification` 防止100ms内重复处理相同数据。

### 3. 缓存机制

| 缓存类型 | 数据结构 | 说明 |
|---------|---------|------|
| **GATT连接缓存** | `deviceGattMap: ConcurrentHashMap<String, BluetoothGatt>` | 设备地址 → GATT连接 |
| **服务发现缓存** | `discoveredServicesCache: ConcurrentHashMap<String, Boolean>` | 设备是否已发现服务 |
| **特征缓存** | `deviceCharacteristics: ConcurrentHashMap<String, Map<UUID, BluetoothGattCharacteristic>>` | 设备特征映射 |
| **鉴权数据缓存** | `authenticationData: ConcurrentHashMap<String, AuthData>` | 包含 hostSalt, deviceSalt, iv, key |

### 4. 异步结果管理

| 类型 | 数据结构 | 用途 |
|------|---------|------|
| **连接结果** | `connectionResults: ConcurrentHashMap<String, CompletableDeferred<Boolean>>` | 等待连接完成 |
| **写入回调** | `writeCallbacks: ConcurrentHashMap<String, CompletableDeferred<Boolean>>` | 等待写入完成 |
| **鉴权结果** | `authenticationResults: ConcurrentHashMap<String, CompletableDeferred<Boolean>>` | 等待鉴权完成 |

### 5. 线程同步机制

```kotlin
// 特征变化回调使用同步锁保护
private val characteristicChangedLock = Any()

override fun onCharacteristicChanged(...) {
    synchronized(characteristicChangedLock) {
        // 处理通知数据
    }
}
```

### 6. 状态流

| 流类型 | 数据结构 | 说明 |
|--------|---------|------|
| **扫描状态流** | `scanStateFlow: MutableStateFlow<ScanState>` | 扫描状态 (NotScanning/Scanning/ScanResult/ScanFailed) |
| **设备事件流** | `deviceEventsFlow: MutableSharedFlow<DeviceEvent>` | 设备事件 (Connected/Disconnected/ButtonPressed/BatteryLevelChanged等) |

---

## 核心数据包格式

### 1. 鉴权请求数据包 (10字节)

```
┌──────┬──────┬─────────────────────────────────────────┐
│ 0x04 │ 0x01 │        hostSalt (8字节)                 │
└──────┴──────┴─────────────────────────────────────────┘
  头部   请求      车机随机盐值
```

### 2. 鉴权响应数据包 (22字节)

```
┌──────┬──────┬─────────┬──────────┬────────────────────┐
│ 0x04 │ 0x02 │deviceSalt│encrypted │    authData       │
│      │      │ (8字节)  │  Addr    │    (6字节)        │
│      │      │          │ (6字节)  │                   │
└──────┴──────┴─────────┴──────────┴────────────────────┘
  头部   响应   设备盐值   加密地址    认证数据
```

### 3. 鉴权状态数据包 (4字节)

```
┌──────┬──────┬──────────┬──────────────────────────────┐
│ 0x04 │ 0x03 │status低位│ status高位                   │
└──────┴──────┴──────────┴──────────────────────────────┘
  头部   状态     结果码
```

---

## 失败场景

### 连接失败场景

1. 蓝牙未启用
2. 缺少蓝牙权限
3. 设备不在范围内
4. 连接超时（10秒）
5. 设备已被其他应用连接

### 鉴权失败场景

1. GATT连接不存在
2. 找不到主服务/写入特征
3. 写入鉴权请求失败
4. 等待响应超时（5秒）
5. PublicAddr验证失败
6. 认证数据验证失败
7. 异常捕获

---

## 关键代码位置索引

| 功能 | 方法名 | 行号 |
|------|--------|------|
| 初始化 | `initialize()` | 129-132 |
| 开始扫描 | `startScan()` | 138-183 |
| 停止扫描 | `stopScan()` | 283-291 |
| 扫描回调 | `scanCallback` | 418-500 |
| 连接设备 | `connect()` | 319-357 |
| 断开连接 | `disconnect()` | 364-383 |
| GATT回调 | `gattCallback` | 599-954 |
| 连接状态变化 | `onConnectionStateChange()` | 603-664 |
| 服务发现 | `onServicesDiscovered()` | 670-780 |
| 特征变化 | `onCharacteristicChanged()` | 804-884 |
| 特征读取 | `onCharacteristicRead()` | 890-927 |
| 特征写入 | `onCharacteristicWrite()` | 933-948 |
| 验证服务 | `validateRequiredServices()` | 975-1027 |
| 启用通知 | `enableCharacteristicNotification()` | 1035-1057 |
| 带超时写入 | `writeCharacteristicWithTimeout()` | 1114-1157 |
| 带超时读取 | `readCharacteristicWithTimeout()` | 1167-1204 |
| 发送命令 | `sendCommand()` | 1216-1245 |
| 设备鉴权 | `authenticateDevice()` | 1460-1540 |
| 处理鉴权响应 | `handleAuthResponse()` | 1547-1666 |
| 发送鉴权状态 | `sendAuthStatus()` | 1673-1699 |
| 完成鉴权 | `completeAuthentication()` | 1706-1713 |
| 读取电池 | `readBatteryLevel()` | 1092-1104 |

---

## 总结

BLE设备鉴权流程采用了**双层异步机制**：

1. **第一层异步**：写入特征 (`writeCharacteristicWithTimeout`)
   - 主线程发送请求后阻塞等待
   - 写入回调线程完成后唤醒主线程

2. **第二层异步**：等待鉴权响应 (`authenticateDevice`)
   - 主线程等待设备响应
   - 通知回调线程处理响应后唤醒主线程

整个过程通过 `CompletableDeferred` 实现线程间同步，并配合多种超时保护、防抖机制和缓存优化，确保连接和鉴权的可靠性和效率。

---

*文档生成时间: 2025-01-XX*  
*基于代码文件: `BleManagerImpl.kt` (1796行)*

