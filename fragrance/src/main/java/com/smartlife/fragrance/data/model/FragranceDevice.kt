package com.smartlife.fragrance.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.smartlife.fragrance.data.converter.ConnectionStateConverter

/**
 * 香氛设备实体类，用于Room数据库存储
 * 
 * 包含所有12个功能字段，以及5个预留String字段和5个预留Int字段用于未来扩展
 */
@Entity(tableName = "fragrance_device")
@TypeConverters(ConnectionStateConverter::class)
data class FragranceDevice(
    @PrimaryKey val macAddress: String,
    
    // 基础信息

    //名字
    val deviceName: String,

    //显示的名字
    val displayName: String = "",

    // 连接状态
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    
    // 功能字段（12个）
    val powerState: PowerState = PowerState.OFF, // 开关状态 (0x01开, 0x02关)
    val mode: Mode = Mode.STANDBY, // 模式切换 (0x01待机, 0x02香型A, 0x03香型B)
    val gear: Gear = Gear.LOW, // 档位切换 (0x01低档, 0x02中档, 0x03高档)
    val carStartStopEnabled: Boolean = false, // 随车启停开关
    val carStartStopCycle: CarStartStopCycle = CarStartStopCycle.CYCLE_1, // 随车启停周期 (0x01/0x02/0x03)
    val lightMode: LightMode = LightMode.OFF, // 灯光模式 (0x00关闭, 0x01呼吸, 0x02律动, 0x03流动, 0x04常亮)
    val lightColor: String = "000000", // 灯光颜色 (RGB十六进制, 格式: "GGRRBB")
    val lightBrightness: Int = 50, // 灯光亮度 (0-100)
    val programVersion: Int = 0, // 程序版本 (0-100)
    val deviceStatus: Int = 0x01, // 设备状态 (0x01) 1.获取香氛剩余寿命百分比值 2.获取香氛棒检测是否缺失 3.获取香氛机器设备电池剩余值
    val timingDuration: Int = 0, // 定时时长 (5-120分钟)
    val batteryLevel:Int = 0,//电量
    val needAutoConnect:Boolean = false,//是否自动重连
    val syncLightBrightness: Boolean = false, //是否同步氛围灯
    
    // 槽位信息
    val slotAFragranceName: String? = null, // A槽香薰名字 (0x01/0x02/0x03)
    val slotBFragranceName: String? = null, // B槽香薰名字 (0x01/0x02/0x03)

    val lightSwitch: Boolean = false, // 灯光开关 (0x00=关, 0x01=开)
    
    // 时间戳
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // 预留字段（用于未来扩展）
    val reservedString1: String? = null,
    val reservedString2: String? = null,
    val reservedString3: String? = null,
    val reservedString4: String? = null,
    val reservedString5: String? = null,
    val reservedString6: String? = null,
    val reservedString7: String? = null,
    val reservedString8: String? = null,
    val reservedString9: String? = null,
    val reservedString10: String? = null,

    val reservedInt1: Int? = null,
    val reservedInt2: Int? = null,
    val reservedInt3: Int? = null,
    val reservedInt4: Int? = null,
    val reservedInt5: Int? = null,
    val reservedInt6: Int? = null,
    val reservedInt7: Int? = null,
    val reservedInt8: Int? = null,
    val reservedInt9: Int? = null,
    val reservedInt10: Int? = null
)

