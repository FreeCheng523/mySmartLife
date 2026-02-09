package com.zkjd.lingdong.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.zkjd.lingdong.data.converter.ButtonTypeConverter

/**
 * 按键功能映射实体，关联设备和对应的按键功能
 */
@Entity(
    tableName = "button_function_mappings",
    foreignKeys = [
        ForeignKey(
            entity = Device::class,
            parentColumns = ["macAddress"],
            childColumns = ["deviceMacAddress"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("deviceMacAddress")]
)
@TypeConverters(ButtonTypeConverter::class)
data class ButtonFunctionMapping(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceMacAddress: String,  // 设备MAC地址
    val buttonType: ButtonType,    // 按键类型
    val functionId: String?,       // 功能ID，null表示未设置
    val customParams: String? = null  // 自定义参数，JSON格式存储
) 