package com.zkjd.lingdong.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.zkjd.lingdong.data.converter.ButtonFunctionConverter
import com.zkjd.lingdong.data.converter.ConnectionStateConverter
import java.util.UUID


/**
 * 按键功能分类枚举
 */
enum class FunctionCategory {
    MEDIA,      // 媒体控制
    APP,        // 应用启动
    CAR,        // 车机控制
    CARTYPE     // 车辆型号
}

/**
 * 设备连接状态枚举
 */
enum class ConnectionState {
    CONNECTED,
    DISCONNECTED,
    CONNECTING
}

/**
 * 按键功能配置数据类
 */
data class ButtonFunction(
    val id: String = UUID.randomUUID().toString(),
    val category: FunctionCategory,
    val name: String,
    val actionCode: String,  // 执行的操作代码
    val iconResId: Int = 0,  // 默认图标
    val iconSelectedResId: Int = 0,  // 新增：选中图标
    val useType: Int = 0,  //  1:按钮 2：旋钮 3：车型
    val configWords: String  //配置字
)

/**
 * 设备实体类，用于Room数据库存储
 */
@Entity(tableName = "devices")
@TypeConverters(ConnectionStateConverter::class, ButtonFunctionConverter::class)
data class Device(
    @PrimaryKey val macAddress: String,
    val name: String,
    val bleName: String,
    val isAntiMisoperationEnabled: Boolean = false,
    var returnControl : Int=0,//返控状态，0关闭，1开启
    var preventAccidental : Int=0,//防误触状态，0关闭，1开启
    var musicCan : Int=1,//音效状态，0关闭，1开启
    var musicName : String,
    var isAppOpen1: Boolean=false,//单击app选项
    var isAppOpen2: Boolean=false,//双击app选项
    var connectedLedColor: Int = 0xFF4CAF50.toInt(),  // 默认绿色
    var reconnectingLedColor: Int = 0xFFF44336.toInt(),  // 默认红色
    var lastConnectionState: ConnectionState = ConnectionState.CONNECTED,
    var batteryLevel: Int? = null,  // 设备电量百分比，初始为null表示未知
    val createdAt: Long = System.currentTimeMillis(),
    val shortPressFunction: ButtonFunction? = null,
    val longPressFunction: ButtonFunction? = null,
    val doubleClickFunction: ButtonFunction? = null,
    val leftRotateFunction: ButtonFunction? = null,
    val rightRotateFunction: ButtonFunction? = null,
    val needAutoConnect:Boolean = false
) {
    // 辅助方法生成默认设备名
    companion object {
        fun generateDefaultName(count: Int): String {
            return "妥妥贴$count"
        }
    }
    
    // 计算属性，判断设备是否已连接
    val isConnected: Boolean
        get() = lastConnectionState == ConnectionState.CONNECTED
    
    // 当前LED颜色
    val ledColor: Int
        get() = if (isConnected) connectedLedColor else reconnectingLedColor
} 