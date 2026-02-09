# Service 前台服务关键点流程图

## 完整流程图

```mermaid
flowchart TD
    Start([开始启动服务]) --> CheckVersion{检查Android版本}

    CheckVersion -->|Android 8.0+| CheckForeground{应用是否在前台?}
    CheckVersion -->|Android 7.1-| StartService1[调用 startService]

    CheckForeground -->|是| StartForegroundService[调用 startForegroundService]
    CheckForeground -->|否| CheckPermission{是否有后台启动权限?}

    CheckPermission -->|有权限| StartForegroundService
    CheckPermission -->|无权限| IllegalStateException[抛出 IllegalStateException<br/>Not allowed to start service]

    StartService1 --> ServiceCreated[系统创建Service实例]
    StartForegroundService --> ServiceCreated

    ServiceCreated --> OnCreate[Service.onCreate<br/>初始化通知系统<br/>initNotification]

    OnCreate --> OnStartCommand[Service.onStartCommand]

    OnStartCommand --> CheckTime{是否在5秒内?}

    CheckTime -->|是| StartForeground[调用 startForeground<br/>显示通知]
    CheckTime -->|否| TimeoutException[抛出 ForegroundServiceDidNotStartInTimeException<br/>服务被系统停止]

    StartForeground --> CheckNotification{通知是否有效?}

    CheckNotification -->|无效| NotificationError[通知创建失败<br/>可能原因:<br/>1. 未创建通知渠道<br/>2. 缺少小图标<br/>3. 渠道被禁用]
    CheckNotification -->|有效| Success[✅ 前台服务运行成功<br/>通知显示<br/>服务保活]

    IllegalStateException --> ErrorHandler[异常处理<br/>记录日志<br/>可尝试WorkManager]
    TimeoutException --> ErrorHandler
    NotificationError --> ErrorHandler

    ErrorHandler --> End([结束])
    Success --> End

    style Start fill:#e1f5ff
    style Success fill:#d4edda
    style IllegalStateException fill:#f8d7da
    style TimeoutException fill:#f8d7da
    style NotificationError fill:#fff3cd
    style ErrorHandler fill:#fff3cd
    style StartForegroundService fill:#cfe2ff
    style StartForeground fill:#cfe2ff
```

## 权限要求图

```mermaid
mindmap
  root((前台服务权限))
    基础权限
      FOREGROUND_SERVICE
        所有前台服务必需
      FOREGROUND_SERVICE_CONNECTED_DEVICE
        Android 14+ 连接设备类型
    后台启动权限
      START_FOREGROUND_SERVICES_FROM_BACKGROUND
        从后台启动前台服务
        普通权限，自动授予
    服务声明
      AndroidManifest.xml
        service标签
        foregroundServiceType属性
```

## 版本差异对比图

```mermaid
graph LR
    subgraph Android7["Android 7.1 及以下"]
        A1[startService] --> A2[Service运行]
    end

    subgraph Android8["Android 8.0 - 13"]
        B1[startForegroundService] --> B2[5秒内必须调用]
        B2 --> B3[startForeground]
        B3 --> B4[前台服务运行]
    end

    subgraph Android14["Android 14+"]
        C1[startForegroundService] --> C2[需要具体类型权限]
        C2 --> C3[5秒内必须调用]
        C3 --> C4[startForeground]
        C4 --> C5[前台服务运行]
    end
```

## 时间线图

```mermaid
flowchart LR
    subgraph Timeline["⏱️ 前台服务启动时间线（5秒限制）"]
        direction TB
        T0["0秒<br/>调用startForegroundService"] --> T1["0.1秒<br/>系统创建Service实例"]
        T1 --> T2["0.3秒<br/>调用onCreate<br/>初始化通知系统"]
        T2 --> T3["0.5秒<br/>调用onStartCommand"]
        T3 --> T4["0.7秒<br/>⚠️ 调用startForeground<br/>必须在5秒内完成"]
        T4 --> T5["0.8秒<br/>✅ 前台服务运行<br/>通知显示"]
        T5 --> T6["持续运行..."]

        T3 -.->|"如果超过5秒未调用"| T7["5秒<br/>❌ 服务被系统停止<br/>ForegroundServiceDidNotStartInTimeException"]

        style T0 fill:#e1f5ff
        style T4 fill:#fff3cd
        style T5 fill:#d4edda
        style T7 fill:#f8d7da
    end
```

### 时间线说明

| 时间点      | 操作                            | 说明           |
| -------- | ----------------------------- | ------------ |
| **0秒**   | 调用 `startForegroundService()` | 启动前台服务       |
| **0.1秒** | 系统创建 Service 实例               | 系统创建服务对象     |
| **0.3秒** | 调用 `onCreate()`               | 初始化通知系统、监听器等 |
| **0.5秒** | 调用 `onStartCommand()`         | 服务启动命令       |
| **0.7秒** | ⚠️ **调用 `startForeground()`** | **必须在5秒内完成** |
| **0.8秒** | ✅ 前台服务运行                      | 通知显示，服务保活    |
| **5秒**   | ❌ 如果未调用 `startForeground()`   | 服务被系统强制停止    |

## 异常处理流程图

```mermaid
flowchart TD
    TryStart[尝试启动服务] --> Catch{捕获异常}

    Catch -->|IllegalStateException| IllegalState[后台启动失败]
    Catch -->|ForegroundServiceDidNotStartInTimeException| Timeout[启动超时]
    Catch -->|SecurityException| Security[权限不足]
    Catch -->|其他异常| Other[其他错误]
    Catch -->|无异常| Success2[启动成功]

    IllegalState --> CheckPerm{检查权限声明}
    CheckPerm -->|已声明| LogError1[记录日志<br/>检查权限是否正确]
    CheckPerm -->|未声明| AddPerm[添加权限声明<br/>START_FOREGROUND_SERVICES_FROM_BACKGROUND]

    Timeout --> CheckCode[检查代码]
    CheckCode -->|延迟调用| FixCode1[立即在onStartCommand中调用]
    CheckCode -->|异步调用| FixCode2[改为同步调用]

    Security --> CheckManifest[检查AndroidManifest]
    CheckManifest -->|缺少权限| AddPerm2[添加相应权限]

    Other --> LogError2[记录详细错误信息]

    LogError1 --> Retry[可尝试WorkManager]
    AddPerm --> Retry
    FixCode1 --> Retry
    FixCode2 --> Retry
    AddPerm2 --> Retry
    LogError2 --> Retry

    Retry --> End2([结束])
    Success2 --> End2

    style IllegalState fill:#f8d7da
    style Timeout fill:#f8d7da
    style Security fill:#f8d7da
    style Other fill:#fff3cd
    style Success2 fill:#d4edda
```

## 关键要点总结

### 1. 启动流程

- **Android 8.0+**: `startForegroundService()` → `onStartCommand()` → `startForeground()`（5秒内）
- **Android 7.1-**: `startService()` → `onStartCommand()`

### 2. 权限要求

- ✅ `FOREGROUND_SERVICE` - 基础权限
- ✅ `START_FOREGROUND_SERVICES_FROM_BACKGROUND` - 后台启动权限
- ✅ `FOREGROUND_SERVICE_CONNECTED_DEVICE` - Android 14+ 特定类型权限

### 3. 时间限制

- ⚠️ **5秒规则**: `startForegroundService()` 调用后必须在 5 秒内调用 `startForeground()`
- ⚠️ **超时后果**: 服务会被系统强制停止

### 4. 异常处理

- `IllegalStateException`: 后台启动无权限
- `ForegroundServiceDidNotStartInTimeException`: 超时未调用 `startForeground()`
- `SecurityException`: 权限不足

### 5. 通知要求

- ✅ 必须设置小图标
- ✅ Android 8.0+ 必须使用通知渠道
- ✅ 通知必须持续显示

### 6. 最佳实践

- ✅ 在 `onCreate()` 中初始化通知系统
- ✅ 在 `onStartCommand()` 中立即调用 `startForeground()`
- ✅ 区分不同类型的异常并处理
- ✅ 版本兼容处理

---

*基于《开启前台服务_startForegroundService和startForeground说明.md》和《在后台开启服务_.md》整理*
