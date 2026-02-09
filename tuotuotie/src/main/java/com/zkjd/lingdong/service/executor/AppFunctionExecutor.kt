package com.zkjd.lingdong.service.executor

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.zkjd.lingdong.model.ButtonFunction
import com.zkjd.lingdong.model.ButtonType
import com.zkjd.lingdong.repository.DeviceRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AppFunctionExecutor"

/**
 * 应用功能执行器
 * 负责处理与应用启动相关的功能
 */
@Singleton
class AppFunctionExecutor @Inject constructor(
    private val context: Context,
    private val deviceRepository: DeviceRepository,
) {
    /**
     * 执行应用相关功能
     * @param function 要执行的功能
     * @param buttonType 触发的按键类型，用于保持接口一致性
     */
    suspend fun executeAppFunction(function: ButtonFunction, buttonType: ButtonType = ButtonType.SHORT_PRESS,macAddress: String) {
        Timber.tag(TAG)
            .d("执行应用功能: ${function.name}, 代码: ${function.actionCode}, 按键类型: $buttonType")

        when {
            function.actionCode.startsWith("APP_") -> {

                val deviceNow=deviceRepository.getDeviceByMacAddress(macAddress)
                if(deviceNow!=null) {
                    val packageName = function.actionCode.substringAfter("APP_")

//                    if (buttonType == ButtonType.SHORT_PRESS) {
//                        if (!deviceNow.isAppOpen1) {

//                            deviceRepository.setIsAppOpenOne(macAddress,true)
                            launchApp(packageName)

//                        } else {
//                            deviceRepository.setIsAppOpenOne(macAddress,false)
//                            // 创建返回主界面的Intent
//                            val intent = Intent(Intent.ACTION_MAIN)
//                            intent.addCategory(Intent.CATEGORY_HOME)
//                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                            // 启动主界面
//                            context.startActivity(intent)
//                        }
//                    } else if (buttonType == ButtonType.DOUBLE_CLICK) {
//
//                        if (!deviceNow.isAppOpen2) {
//                            deviceRepository.setIsAppOpenTwo(macAddress,true)
//                            launchApp(packageName)
//
//                        } else {
//                            deviceRepository.setIsAppOpenTwo(macAddress,false)
//                            // 创建返回主界面的Intent
//                            val intent = Intent(Intent.ACTION_MAIN)
//                            intent.addCategory(Intent.CATEGORY_HOME)
//                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                            // 启动主界面
//                            context.startActivity(intent)
//                        }
//                    }
                }
            }

            else -> {
                Timber.tag(TAG).w("未知应用功能代码: ${function.actionCode}")
            }
        }
    }
    
    /**
     * 启动指定包名的应用
     * @param packageName 应用包名
     */
    private fun launchApp(packageName: String) {
        try {
            Timber.tag(TAG).w("尝试启动应用: $packageName")
            
            // 1. 获取启动Intent
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            
            if (launchIntent != null) {
                // 2. 添加关键标志，确保应用切换到前台
                launchIntent.apply {
                    // 设置启动新任务
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    // 带历史使用记录（防止创建多个实例）
                    addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    // 将已运行实例带到前台
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    // 如果在后台，则清除顶部Activity
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                
                // 3. 直接启动（让系统处理)
                context.startActivity(launchIntent)
                Timber.tag(TAG).w("成功启动应用: $packageName (直接方式)")
            } else {
                // 尝试使用替代方式打开应用 - 使用ACTION_MAIN方式
                val alternativeIntent = Intent(Intent.ACTION_MAIN)
                alternativeIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                alternativeIntent.setPackage(packageName)
                alternativeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                alternativeIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                alternativeIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                alternativeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                
                try {
                    context.startActivity(alternativeIntent)
                    Timber.tag(TAG).w("成功启动应用: $packageName (替代方式)")
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "替代方式启动应用失败: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "启动应用失败: ${e.message}")
        }
    }
} 