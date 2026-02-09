package com.zkjd.lingdong.service

import com.zkjd.lingdong.model.ButtonFunction
import com.zkjd.lingdong.model.ButtonType

/**
 * 功能执行器接口，负责执行设备按键对应的功能
 */
interface FunctionExecutor {
    
    /**
     * 执行特定功能
     * @param function 要执行的功能
     * @param buttonType 触发的按键类型，对于旋转类功能尤为重要
     */
    suspend fun executeFunction(
        function: ButtonFunction,
        buttonType: ButtonType,
        macAddress: String
    )
    
    /**
     * 获取设备上所有已安装的应用
     */
    suspend fun getInstalledApps(): List<AppInfo>
}

/**
 * 应用信息数据类
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val iconResId: Int
) 