# ADB 查看 CPU 使用率命令汇总

> 专门用于查看 `com.deepal.ivi.hmi.smartlife` 应用的 CPU 使用情况

---

## 🎯 快速命令（推荐）

### 1. 查看应用 CPU 详细信息（最推荐）

```bash
adb shell dumpsys cpuinfo | grep smartlife
```

**输出示例**：
```
0.4% 28056/com.deepal.ivi.hmi.smartlife: 0.3% user + 0.1% kernel / faults: 4376 minor 6 major
```

**说明**：
- `0.4%`：总 CPU 使用率
- `0.3% user`：用户态 CPU（应用代码）
- `0.1% kernel`：内核态 CPU（系统调用）
- `faults`：页错误次数

---

### 2. 查看系统整体 CPU 使用率

```bash
adb shell top -n 1 | grep "%cpu"
```

**输出示例**：
```
800%cpu  22%user   0%nice  26%sys 748%idle   0%iow   4%irq   0%sirq   0%host
```

**说明**：
- `800%cpu`：8核 CPU（8 × 100%）
- `22%user`：用户态总使用率
- `26%sys`：系统调用总使用率
- `748%idle`：空闲率（74.8%）

---

### 3. 查看应用实时 CPU（包含完整信息）

```bash
adb shell "top -n 1 | grep com.deepal.ivi.hmi.smartlife"
```

**输出示例**：
```
28056 u0_a95  10 -10  195G  501M  301M  S  3.7  2.9  0:19.25  com.deepal.ivi.hmi.smartlife
```

**CPU 相关列**：
- 第9列 `3.7`：**%CPU**（CPU 使用率）
- 第8列 `S`：进程状态
- 第11列 `0:19.25`：累计 CPU 时间

---

## 📊 详细命令选项

### 4. 按 CPU 使用率排序查看所有进程

```bash
adb shell top -n 1 -o %CPU
```

**说明**：显示所有进程，按 CPU 使用率从高到低排序

---

### 5. 持续监控 CPU（实时更新）

```bash
adb shell top | grep smartlife
```

**说明**：
- 实时更新，每秒刷新
- 按 `Ctrl+C` 退出

---

### 6. 查看 CPU 使用率最高的前 10 个进程

```bash
adb shell top -n 1 -m 10
```

**说明**：显示 CPU 使用率最高的 10 个进程

---

## 🔧 PowerShell 专用命令

### 7. 在 PowerShell 中查看应用 CPU

```powershell
adb shell dumpsys cpuinfo | Select-String -Pattern "smartlife"
```

### 8. 查看系统 CPU 并高亮显示

```powershell
adb shell "top -n 1" | Select-String -Pattern "%cpu|smartlife"
```

---

## 📈 持续监控脚本

### 方法1：简单循环监控

```bash
# Windows PowerShell
while ($true) {
    Write-Host "=== $(Get-Date -Format 'HH:mm:ss') ===" -ForegroundColor Green
    adb shell dumpsys cpuinfo | Select-String -Pattern "smartlife"
    Start-Sleep -Seconds 2
}
```

### 方法2：只显示 CPU 百分比

```bash
# Windows PowerShell
while ($true) {
    $result = adb shell dumpsys cpuinfo | Select-String -Pattern "smartlife"
    if ($result) {
        $cpu = ($result -split '\s+')[0]
        Write-Host "$(Get-Date -Format 'HH:mm:ss') - CPU: $cpu" -ForegroundColor Yellow
    }
    Start-Sleep -Seconds 1
}
```

---

## 🎯 命令对比表

| 命令 | 输出内容 | 更新频率 | 推荐度 |
|------|---------|---------|--------|
| `dumpsys cpuinfo \| grep smartlife` | 详细CPU信息 | 快照 | ⭐⭐⭐⭐⭐ |
| `top -n 1 \| grep smartlife` | 完整进程信息 | 快照 | ⭐⭐⭐⭐ |
| `top \| grep smartlife` | 完整进程信息 | 实时 | ⭐⭐⭐ |
| `top -n 1 \| grep %cpu` | 系统整体CPU | 快照 | ⭐⭐⭐ |

---

## 💡 使用建议

### 快速检查
```bash
adb shell dumpsys cpuinfo | grep smartlife
```

### 详细分析
```bash
adb shell top -n 1 | grep smartlife
```

### 持续监控
```bash
adb shell top | grep smartlife
```

---

## 📝 输出字段说明

### dumpsys cpuinfo 输出格式

```
0.4% 28056/com.deepal.ivi.hmi.smartlife: 0.3% user + 0.1% kernel / faults: 4376 minor 6 major
│    │                                        │              │              │
│    │                                        │              │              └─ 页错误
│    │                                        │              └─ 内核态CPU
│    │                                        └─ 用户态CPU
│    └─ 进程ID/包名
└─ 总CPU使用率
```

### top 输出格式（CPU相关列）

```
28056 u0_a95  10 -10  195G  501M  301M  S  3.7  2.9  0:19.25  com.deepal.ivi.hmi.smartlife
│     │       │  │    │     │     │    │  │    │    │
│     │       │  │    │     │     │    │  │    │    └─ 累计CPU时间
│     │       │  │    │     │     │    │  │    └─ 内存占比
│     │       │  │    │     │     │    │  └─ CPU使用率 ⭐
│     │       │  │    │     │     │    └─ 进程状态
│     │       │  │    │     │     └─ 共享内存
│     │       │  │    │     └─ 物理内存
│     │       │  │    └─ 虚拟内存
│     │       │  └─ Nice值
│     │       └─ 优先级
│     └─ 用户
└─ PID
```

---

## 🚀 快速参考

**最常用命令**：
```bash
# 查看应用CPU
adb shell dumpsys cpuinfo | grep smartlife

# 查看系统CPU
adb shell top -n 1 | grep "%cpu"
```



