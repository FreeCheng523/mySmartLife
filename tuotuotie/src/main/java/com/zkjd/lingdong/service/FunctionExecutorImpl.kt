package com.zkjd.lingdong.service

import android.content.Context
import android.content.Intent
import com.zkjd.lingdong.data.FunctionsConfig
import com.zkjd.lingdong.model.ButtonFunction
import com.zkjd.lingdong.model.ButtonType
import com.zkjd.lingdong.model.FunctionCategory
import com.zkjd.lingdong.service.executor.AppFunctionExecutor
import com.zkjd.lingdong.service.executor.CarMediaExecutorImp
import com.zkjd.lingdong.service.executor.CarFunctionExecutorImp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FunctionExecutor"

/**
 * 功能执行器实现类
 */
@Singleton
class FunctionExecutorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appExecutor: AppFunctionExecutor,
    private val carMediaFunctionExecutor: CarMediaExecutorImp,
    private val carFunctionExecutorImp: CarFunctionExecutorImp,
) : FunctionExecutor {

    /**
     * 执行特定功能
     * @param function 需要执行的按键功能
     * @param buttonType 触发的按键类型，用于处理旋转功能
     */
    override suspend fun executeFunction(
        function: ButtonFunction,
        buttonType: ButtonType,
        macAddress: String
    ) {
        Timber.tag(TAG)
            .d("执行功能: ${function.name}, 代码: ${function.actionCode}, 按键类型: $buttonType")

        try {
            withContext(Dispatchers.IO) {
                // 获取当前设置的执行器类型

                // 将功能传递给相应的执行器，包括按键类型信息
                when (function.category) {
                    FunctionCategory.APP -> appExecutor.executeAppFunction(function, buttonType,macAddress)
                    FunctionCategory.CAR -> {
                        carFunctionExecutorImp.executeCarFunction(function,buttonType,macAddress)
                    }
                    FunctionCategory.MEDIA -> {
                       // newFunctionExecutor.executeNewFunction(function,buttonType)
                        carMediaFunctionExecutor.executeMediaFunction(function, buttonType)
                    }

                    FunctionCategory.CARTYPE -> TODO()
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e("执行功能失败: ${e.message}")
        }
    }

    override suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        try {
            // 使用更可靠的方式获取应用列表
            val installedApps = mutableListOf<AppInfo>()

            // 查询所有带启动器图标的应用
            val resolveInfos = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(intent, android.content.pm.PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }

            Timber.tag(TAG).w("总共找到 ${resolveInfos.size} 个应用")

            for (resolveInfo in resolveInfos) {
                try {
                    val appName = resolveInfo.loadLabel(pm).toString()
                    val packageName = resolveInfo.activityInfo.packageName

                    // 不加载实际图标，只使用固定图标ID
                    val iconResId = android.R.drawable.sym_def_app_icon
                    installedApps.add(AppInfo(packageName, appName, iconResId))
                    // 跳过系统应用
//                    if (!packageName.startsWith("com.android") &&
//                        !packageName.startsWith("com.google.android")) {
//                        installedApps.add(AppInfo(packageName, appName, iconResId))
//                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).e("处理应用信息时出错: ${e.message}")
                }
            }

            // 按应用名称排序
            val sortedApps = installedApps.sortedBy { it.appName }
            Timber.tag(TAG).w("过滤后获取到 ${sortedApps.size} 个应用")
            sortedApps
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "获取应用列表失败: ${e.message}")
            emptyList()
        }
    }
}