# 香氛设备Room数据库实体表文档

## 概述

本文档描述了香氛设备Room数据库的设计和实现，包括实体类、DAO接口、数据库配置以及AutoMigration自动升级机制。

## 数据库信息

- **数据库名称**: `fragrance_database`
- **当前版本**: 1
- **表名**: `fragrance_device`
- **主键**: `macAddress` (String)

## 实体类：FragranceDevice

### 文件位置
`fragrance/src/main/java/com/smartlife/fragrance/data/model/FragranceDevice.kt`

### 字段说明

#### 基础字段

| 字段名 | 类型 | 说明 | 默认值 |
|--------|------|------|--------|
| `macAddress` | String | 设备MAC地址（主键） | - |
| `deviceName` | String | 设备名称 | - |
| `createdAt` | Long | 创建时间戳 | System.currentTimeMillis() |
| `updatedAt` | Long | 更新时间戳 | System.currentTimeMillis() |

#### 功能字段（12个）

| 字段名 | 类型 | 说明 | 默认值 | 地址 | 可能值 |
|--------|------|------|--------|------|--------|
| `powerState` | PowerState | 开关状态 | PowerState.OFF | 0x00 | PowerState.ON(0x01), PowerState.OFF(0x02) |
| `mode` | Mode | 模式切换 | Mode.STANDBY | 0x01 | Mode.STANDBY(0x01), Mode.FRAGRANCE_A(0x02), Mode.FRAGRANCE_B(0x03) |
| `gear` | Gear | 档位切换 | Gear.LOW | 0x02 | Gear.LOW(0x01), Gear.MEDIUM(0x02), Gear.HIGH(0x03) |
| `carStartStopEnabled` | Boolean | 随车启停开关 | false | 0x04 | false(关), true(开) |
| `carStartStopCycle` | CarStartStopCycle | 随车启停周期 | CarStartStopCycle.CYCLE_1 | 0x05 | CYCLE_1(0x01), CYCLE_2(0x02), CYCLE_3(0x03) |
| `lightMode` | LightMode | 灯光模式 | LightMode.OFF | 0x06 | OFF(0x00), BREATH(0x01), RHYTHM(0x02), FLOW(0x03), ALWAYS_ON(0x04) |
| `lightColor` | String | 灯光颜色 | "000000" | 0x07 | RGB十六进制格式 "GGRRBB" |
| `lightBrightness` | Int | 灯光亮度 | 50 | 0x08 | 0-100 |
| `programVersion` | Int | 程序版本 | 0 | 0x09 | 0-100 |
| `deviceStatus` | Int | 设备状态 | 0x01 | 0x0a | 0x01 |
| `timingDuration` | Int | 定时时长 | 0 | 0x0b | 5-120分钟 |

**档位切换说明**：
- 低档 (0x01): 风扇运行10秒，停30秒
- 中档 (0x02): 风扇运行20秒，停30秒
- 高档 (0x03): 风扇运行30秒，停30秒

**定时功能说明**：
- 数据范围为5-120分钟
- 具体包含值：5分钟、10分钟、30分钟、40分钟、50分钟、60分钟、90分钟、120分钟

#### 预留字段（10个）

| 字段名 | 类型 | 说明 | 默认值 |
|--------|------|------|--------|
| `reservedString1` | String? | 预留String字段1 | null |
| `reservedString2` | String? | 预留String字段2 | null |
| `reservedString3` | String? | 预留String字段3 | null |
| `reservedString4` | String? | 预留String字段4 | null |
| `reservedString5` | String? | 预留String字段5 | null |
| `reservedInt1` | Int? | 预留Int字段1 | null |
| `reservedInt2` | Int? | 预留Int字段2 | null |
| `reservedInt3` | Int? | 预留Int字段3 | null |
| `reservedInt4` | Int? | 预留Int字段4 | null |
| `reservedInt5` | Int? | 预留Int字段5 | null |

**预留字段使用说明**：
- 预留字段使用可空类型（String?、Int?），默认值为null
- 可直接使用预留字段存储扩展数据，无需修改数据库结构
- 建议在代码注释中说明每个预留字段的实际用途

## DAO接口：FragranceDeviceDao

### 文件位置
`fragrance/src/main/java/com/smartlife/fragrance/data/dao/FragranceDeviceDao.kt`

### 方法说明

#### 插入和更新操作

| 方法名 | 返回类型 | 说明 |
|--------|----------|------|
| `insertDevice(device: FragranceDevice)` | suspend Long | 插入设备，冲突时替换 |
| `updateDevice(device: FragranceDevice)` | suspend Int | 更新设备，返回受影响行数 |
| `deleteDevice(device: FragranceDevice)` | suspend Int | 删除设备，返回受影响行数 |
| `deleteDeviceByMacAddress(macAddress: String)` | suspend Int | 根据MAC地址删除设备 |

#### 查询操作（Flow方式）

| 方法名 | 返回类型 | 说明 |
|--------|----------|------|
| `getDevice(macAddress: String)` | Flow<FragranceDevice?> | 根据MAC地址查询设备（响应式） |
| `getAllDevices()` | Flow<List<FragranceDevice>> | 获取所有设备列表（响应式，按创建时间排序） |

#### 查询操作（非Flow方式）

| 方法名 | 返回类型 | 说明 |
|--------|----------|------|
| `getDeviceByMac(macAddress: String)` | suspend FragranceDevice? | 根据MAC地址查询设备（一次性） |
| `getAllDevicesAsList()` | suspend List<FragranceDevice> | 获取所有设备列表（一次性，按创建时间排序） |
| `getDeviceCount()` | suspend Int | 获取设备数量 |

## TypeConverter 类型转换器

### 概述

TypeConverter 是 Room 数据库提供的一个机制，用于将 Room 不支持的自定义类型（如枚举类）转换为数据库支持的基本类型（如 Int、String 等）。通过 TypeConverter，我们可以在实体类中使用枚举类型，提高代码的类型安全性和可读性。

### 作用

1. **类型安全**：在代码中使用枚举类型，避免使用魔法数字（magic numbers），减少类型错误
2. **自动转换**：Room 在存储和读取数据时自动调用转换器，无需手动转换
3. **代码可读性**：使用枚举值（如 `PowerState.ON`）比使用数字（如 `0x01`）更直观
4. **维护性**：集中管理类型转换逻辑，便于维护和扩展

### 文件位置

`fragrance/src/main/java/com/smartlife/fragrance/data/Converters.kt`

### 转换器说明

本项目中的 `Converters` 类包含以下5个枚举类型的转换器：

| 枚举类型 | 转换方法 | 存储类型 | 说明 |
|---------|---------|---------|------|
| `PowerState` | `fromPowerState()` / `toPowerState()` | Int | 电源状态枚举（开/关） |
| `Mode` | `fromMode()` / `toMode()` | Int | 模式枚举（待机/香型A/香型B） |
| `Gear` | `fromGear()` / `toGear()` | Int | 档位枚举（低/中/高） |
| `CarStartStopCycle` | `fromCarStartStopCycle()` / `toCarStartStopCycle()` | Int | 随车启停周期枚举 |
| `LightMode` | `fromLightMode()` / `toLightMode()` | Int | 灯光模式枚举（关闭/呼吸/律动/流动/常亮） |

### 转换器实现示例

每个转换器都包含两个方法：

```kotlin
// 将枚举转换为数据库存储的 Int 值
@TypeConverter
fun fromPowerState(powerState: PowerState): Int {
    return powerState.value
}

// 将数据库的 Int 值转换为枚举
@TypeConverter
fun toPowerState(value: Int): PowerState {
    return PowerState.fromValue(value)
}
```

### 枚举类定义

所有枚举类都位于 `fragrance/src/main/java/com/smartlife/fragrance/data/model/` 目录下：

- **PowerState.kt** - 电源状态
  - `ON(0x01)` - 开
  - `OFF(0x02)` - 关

- **Mode.kt** - 模式
  - `STANDBY(0x01)` - 待机
  - `FRAGRANCE_A(0x02)` - 香型A
  - `FRAGRANCE_B(0x03)` - 香型B

- **Gear.kt** - 档位
  - `LOW(0x01)` - 低档
  - `MEDIUM(0x02)` - 中档
  - `HIGH(0x03)` - 高档

- **CarStartStopCycle.kt** - 随车启停周期
  - `CYCLE_1(0x01)` - 5分钟工作/10分钟停止
  - `CYCLE_2(0x02)` - 5分钟工作/20分钟停止
  - `CYCLE_3(0x03)` - 5分钟工作/30分钟停止

- **LightMode.kt** - 灯光模式
  - `OFF(0x00)` - 关闭
  - `BREATH(0x01)` - 呼吸
  - `RHYTHM(0x02)` - 律动
  - `FLOW(0x03)` - 流动
  - `ALWAYS_ON(0x04)` - 常亮

每个枚举类都包含：
- 对应的 Int 值（与原始协议值保持一致）
- `fromValue(value: Int)` 静态方法，用于从 Int 值创建枚举实例

### 注册转换器

在 `FragranceDatabase` 类中使用 `@TypeConverters` 注解注册转换器：

```kotlin
@TypeConverters(Converters::class)
@Database(
    entities = [FragranceDevice::class],
    version = 1,
    exportSchema = true
)
abstract class FragranceDatabase : RoomDatabase()
```

### 工作原理

1. **存储数据时**：
   - Room 检测到实体类中的枚举字段
   - 自动调用对应的 `fromXxx()` 方法
   - 将枚举转换为 Int 值存储到数据库

2. **读取数据时**：
   - Room 从数据库读取 Int 值
   - 自动调用对应的 `toXxx()` 方法
   - 将 Int 值转换为枚举对象返回

### 优势

- ✅ **类型安全**：编译时检查，避免使用无效的枚举值
- ✅ **代码提示**：IDE 可以提供完整的枚举值自动补全
- ✅ **可读性强**：代码中使用 `PowerState.ON` 比 `0x01` 更清晰
- ✅ **维护方便**：枚举值集中管理，修改时只需更新枚举类
- ✅ **向后兼容**：数据库仍存储 Int 值，与原有协议兼容

## 数据库类：FragranceDatabase

### 文件位置
`fragrance/src/main/java/com/smartlife/fragrance/data/FragranceDatabase.kt`

### 配置说明

```kotlin
@TypeConverters(Converters::class)
@Database(
    entities = [FragranceDevice::class],
    version = 1,
    exportSchema = true,
    autoMigrations = [
        // 当升级到版本2时，取消注释下面这行：
        // AutoMigration(from = 1, to = 2)
    ]
)
```

### 获取数据库实例

```kotlin
val database = FragranceDatabase.getDatabase(context)
val dao = database.fragranceDeviceDao()
```

## AutoMigration自动升级机制

### 概述

Room 2.4.0+ 支持AutoMigration，可以自动处理简单的数据库结构变更，无需手动编写SQL迁移代码。

### 支持的自动迁移操作

- ✅ 自动添加新列（使用默认值）
- ✅ 自动删除列
- ✅ 自动创建/删除索引
- ✅ 自动重命名列（需配合@RenameColumn注解）

### 不支持的自动迁移操作

- ❌ 复杂数据转换
- ❌ 计算字段
- ❌ 表结构重构

### 使用流程

#### 首次创建（version = 1）

直接创建数据库，无需Migration。

#### 后续升级（version = 2）

1. **修改实体类**：添加或删除字段（新增字段使用默认值或可空类型）
2. **更新版本号**：在 `@Database` 注解中将 `version` 改为 `2`
3. **添加AutoMigration**：在 `autoMigrations` 数组中添加 `AutoMigration(from = 1, to = 2)`
4. **编译项目**：Room会自动检测差异并执行迁移，生成 `schemas/2.json`

### 示例：从版本1升级到版本2

**步骤1：修改实体类**
```kotlin
data class FragranceDevice(
    // ... 现有字段 ...
    val newField: String = "default" // 新增字段，使用默认值
)
```

**步骤2：更新数据库类**
```kotlin
@Database(
    entities = [FragranceDevice::class],
    version = 2, // 更新版本号
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2) // 添加自动迁移
    ]
)
```

**步骤3：编译项目**
- Room会自动检测字段变更
- 自动执行迁移SQL
- 生成 `schemas/2.json` 文件

### 注意事项

1. **Schema导出必须启用**：
   - `exportSchema = true`（必需）
   - 在 `build.gradle.kts` 中配置 `room.schemaLocation`

2. **新增字段默认值**：
   - 新增非空字段必须有默认值，否则AutoMigration会失败
   - 建议使用可空类型，默认值为null

3. **版本控制**：
   - schema JSON文件（`schemas/1.json`、`schemas/2.json`等）必须提交到Git
   - 便于团队协作和版本追踪

4. **测试验证**：
   - 升级后务必测试，确保数据完整性
   - 建议在测试环境中先验证迁移逻辑

## 配置说明

### build.gradle.kts配置

在 `android` -> `defaultConfig` 中添加：

```kotlin
defaultConfig {
    // ... 其他配置 ...
    
    // Room schema导出配置（AutoMigration必需）
    kapt {
        arguments {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }
}
```

### schemas目录

位置：`fragrance/schemas/`

- Room编译时会自动生成版本对应的schema文件（如 `1.json`、`2.json` 等）
- 这些文件需要提交到版本控制
- 用于AutoMigration比较版本差异

## 使用示例

### 插入设备

```kotlin
val device = FragranceDevice(
    macAddress = "AA:BB:CC:DD:EE:FF",
    deviceName = "香氛设备1",
    powerState = PowerState.ON, // 使用枚举类型
    mode = Mode.FRAGRANCE_A, // 香型A模式
    gear = Gear.MEDIUM // 中档
)

dao.insertDevice(device)
```

**注意**：Room 会自动通过 TypeConverter 将枚举值转换为 Int 存储到数据库。

### 查询设备（Flow方式）

```kotlin
dao.getDevice("AA:BB:CC:DD:EE:FF")
    .collect { device ->
        device?.let {
            // 处理设备数据
        }
    }
```

### 更新设备

```kotlin
val device = dao.getDeviceByMac("AA:BB:CC:DD:EE:FF")
device?.let {
    val updated = it.copy(
        powerState = PowerState.OFF, // 使用枚举类型
        lightMode = LightMode.BREATH // 呼吸模式
    )
    dao.updateDevice(updated)
}
```

**注意**：查询返回的设备对象中，枚举字段已经是枚举类型，可以直接使用。

### 使用预留字段

```kotlin
val device = FragranceDevice(
    macAddress = "AA:BB:CC:DD:EE:FF",
    deviceName = "香氛设备1",
    reservedString1 = "扩展数据1", // 使用预留字段
    reservedInt1 = 100 // 使用预留字段
)
```

## 版本历史

### Version 1 (初始版本)
- 创建基础实体类结构
- 包含12个功能字段
- 包含10个预留字段（5个String + 5个Int）
- 配置AutoMigration支持
- 使用 TypeConverter 将功能字段改为枚举类型（PowerState、Mode、Gear、CarStartStopCycle、LightMode）

## 参考文档

- [Room官方文档](https://developer.android.com/training/data-storage/room)
- [Room TypeConverter指南](https://developer.android.com/training/data-storage/room/referencing-data#type-converters)
- [Room AutoMigration指南](https://developer.android.com/training/data-storage/room/migrating-db-versions#automigrations)
- 香氛设备功能规格表（见同目录下的Excel文档）

## 更新日期

2025-01-27

