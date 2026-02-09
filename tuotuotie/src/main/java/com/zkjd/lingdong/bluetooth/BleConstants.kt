package com.zkjd.lingdong.bluetooth

import java.util.UUID

/**
 * 蓝牙通信常量
 * 
 * 根据妥妥贴设备通信协议文档定义的常量：
 * - Service UUID: 0xffd0
 * - Characteristic #1: 0xffd1 (用于控制命令，支持notify和read操作)
 * - Characteristic #2: 0xffd2 (用于返控命令，支持write操作)
 */
object BleConstants {
    /** 设备名称前缀
     * 按键 DeepalTag_L0
     * 旋钮 DeepalTag_L1
     * 歌尔 DeepalTag_E1
     * */
    var DEVICE_NAME_HEAD = "DeepalTag"

    var LYT="DeepalTag_L"
    var GR="DeepalTag_E"

    /** 扫描时候最小信号强度（用于距离判断） */
    val MIN_RSSI_THRESHOLD = -70

    /** 主服务UUID */
    val SERVICE_UUID: UUID = UUID.fromString("0000ffd0-0000-1000-8000-00805f9b34fb")
    
    /** 特征UUID - 通知特征（设备->手机） */
    val CHARACTERISTIC_NOTIFY_UUID: UUID = UUID.fromString("0000ffd2-0000-1000-8000-00805f9b34fb")
    
    /** 特征UUID - 写入特征（手机->设备） */
    val CHARACTERISTIC_WRITE_UUID: UUID = UUID.fromString("0000ffd1-0000-1000-8000-00805f9b34fb")
    
    /** 电池服务UUID */
    val BATTERY_SERVICE_UUID: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    
    /** 电池电量特征UUID */
    val BATTERY_LEVEL_UUID: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
    

    
    // 数据包头
    const val HEADER_CONTROL: Byte = 0x01    // 返控指令头（手机->设备）
    const val HEADER_NOTIFICATION: Byte = 0x02   // 控制指令头（设备->手机）
    const val HEADER_AUTH: Byte = 0x04       // 鉴权指令头
    
    // 指令定义
    object Instruction {
        const val BACKLIGHT_IN_WORKING: Byte = 0x01               // 工作模式下背光效果
        const val BACKLIGHT_IN_WAITING_RECONNECTION: Byte = 0x02  // 待命回连模式下背光效果
        const val KNOB_ANTI_MISOPERATION: Byte = 0x03             // 旋钮防误触
    }
    
    // 鉴权指令定义
    object AuthInstruction {
        const val AUTH_REQUEST: Byte = 0x01          // 车机发起鉴权请求
        const val AUTH_RESPONSE: Byte = 0x02         // 妥妥贴应答鉴权请求
        const val AUTH_STATUS_RESPONSE: Byte = 0x03  // 车机返回鉴权结果
    }

    // 鉴权状态码
    object AuthStatus {
        const val AUTH_SUCCESS: Short = 0x0001       // 鉴权成功
        const val AUTH_FAILED: Short = 0x0002        // 鉴权失败
    }
    
    // 鉴权常量
    object Auth {
        const val SALT_LENGTH: Int = 8               // 随机数长度
        const val PUBLIC_ADDR_LENGTH: Int = 16       // 设备地址长度
        const val AUTH_DATA_LENGTH: Int = 16         // 认证数据长度
        const val AUTH_LABEL: String = "0x112233445566"  // 固定标签值(测试用，实际环境需替换)
    }
    
    // 按键代码
    object KeyCode {
        const val KEY_SHORT_PRESS: Byte = 0x01        // 短按按钮键
        const val KEY_LONG_PRESS: Byte = 0x41        // 长按按钮键
        const val KEY_DOUBLE_CLICK: Byte = 0x81.toByte()  // 双击按钮键
        const val KNOB_CLOCKWISE: Byte = 0x02         // 旋钮正旋(右旋)
        const val KNOB_ANTICLOCKWISE: Byte = 0x03     // 旋钮反旋(左旋)
        const val REVERSE_CONTROL_ENTER: Byte = 0xF1.toByte()  // 进入反向控制
        const val REVERSE_CONTROL_EXIT: Byte = 0xF2.toByte()   // 退出反向控制
        const val KEY_NONE: Byte = 0x00                // 无效值，代表按键未使能
    }
    
    // 数据包长度
    const val PACKET_LENGTH = 6
} 