package com.mine.baselibrary.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat


/**
 * 权限管理工具类
 */
class PermissionUtil {

    companion object {
        private const val TAG = "PermissionUtil"

        // 常用权限定义
        @JvmField
        val CAMERA_PERMISSION = Manifest.permission.CAMERA
        @JvmField
        val READ_STORAGE_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
        @JvmField
        val WRITE_STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
        @JvmField
        val FINE_LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
        @JvmField
        val COARSE_LOCATION_PERMISSION = Manifest.permission.ACCESS_COARSE_LOCATION
        @JvmField
        val RECORD_AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO
        @JvmField
        val POST_NOTIFICATIONS_PERMISSION = Manifest.permission.POST_NOTIFICATIONS

        // 蓝牙权限定义（区分版本）
        // Android 12 (API 31) 之前的蓝牙权限
        /**
         * 蓝牙基础权限 - 允许应用访问蓝牙功能
         * 适用于 Android 11 及以下版本
         * 注意：从 Android 12 开始，此权限已被细分为更具体的权限
         */
        @JvmField
        val BLUETOOTH_PERMISSION = Manifest.permission.BLUETOOTH
        
        /**
         * 蓝牙管理权限 - 允许应用发现和配对蓝牙设备
         * 适用于 Android 11 及以下版本
         * 注意：从 Android 12 开始，此权限已被细分为更具体的权限
         */
        @JvmField
        val BLUETOOTH_ADMIN_PERMISSION = Manifest.permission.BLUETOOTH_ADMIN
        
        // Android 12 (API 31) 及之后的蓝牙权限
        /**
         * 蓝牙扫描权限 - 允许应用扫描附近的蓝牙设备
         * 适用于 Android 12 (API 31) 及以上版本
         * 替代了旧版本的 BLUETOOTH 和 BLUETOOTH_ADMIN 权限
         */
        @JvmField
        val BLUETOOTH_SCAN_PERMISSION = Manifest.permission.BLUETOOTH_SCAN
        
        /**
         * 蓝牙连接权限 - 允许应用连接到已配对的蓝牙设备
         * 适用于 Android 12 (API 31) 及以上版本
         * 替代了旧版本的 BLUETOOTH 和 BLUETOOTH_ADMIN 权限
         */
        @JvmField
        val BLUETOOTH_CONNECT_PERMISSION = Manifest.permission.BLUETOOTH_CONNECT
        
        /**
         * 蓝牙广播权限 - 允许应用作为蓝牙外围设备进行广播
         * 适用于 Android 12 (API 31) 及以上版本
         * 用于 BLE 广播功能，如 iBeacon 等
         */
        @JvmField
        val BLUETOOTH_ADVERTISE_PERMISSION = Manifest.permission.BLUETOOTH_ADVERTISE

        // 权限组定义
        @JvmField
        val STORAGE_PERMISSIONS = arrayOf(READ_STORAGE_PERMISSION, WRITE_STORAGE_PERMISSION)
        @JvmField
        val CAMERA_PERMISSIONS = arrayOf(CAMERA_PERMISSION)
        @JvmField
        val LOCATION_PERMISSIONS = arrayOf(FINE_LOCATION_PERMISSION, COARSE_LOCATION_PERMISSION)
        
        // 蓝牙权限组定义（区分版本）
        // Android 12 (API 31) 之前的蓝牙权限组
        /**
         * 旧版蓝牙权限组 - 适用于 Android 11 及以下版本
         * 包含：
         * - BLUETOOTH: 蓝牙基础权限
         * - BLUETOOTH_ADMIN: 蓝牙管理权限
         * - ACCESS_FINE_LOCATION: 精确位置权限（蓝牙扫描需要）
         */
        @JvmField
        val BLUETOOTH_SCAN_CONNECT_LEGACY_PERMISSIONS = arrayOf(BLUETOOTH_PERMISSION, BLUETOOTH_ADMIN_PERMISSION, FINE_LOCATION_PERMISSION)
        
        // Android 12 (API 31) 及之后的蓝牙权限组
        /**
         * 新版蓝牙权限组 - 适用于 Android 12 (API 31) 及以上版本
         * 包含：
         * - BLUETOOTH_SCAN: 蓝牙扫描权限
         * - BLUETOOTH_CONNECT: 蓝牙连接权限
         * 注意：新版蓝牙权限不再需要位置权限
         */
        @JvmField
        val BLUETOOTH_SCAN_CONNECT_NEW_PERMISSIONS = arrayOf(BLUETOOTH_SCAN_PERMISSION, BLUETOOTH_CONNECT_PERMISSION)
        
        @JvmField
        val ALL_COMMON_PERMISSIONS = arrayOf(
            CAMERA_PERMISSION,
            READ_STORAGE_PERMISSION,
            WRITE_STORAGE_PERMISSION,
            FINE_LOCATION_PERMISSION,
            RECORD_AUDIO_PERMISSION,
            POST_NOTIFICATIONS_PERMISSION
        )

        /**
         * 根据Android版本获取蓝牙权限
         * 
         * Android 12 (API 31) 引入了新的蓝牙权限模型，将原来的 BLUETOOTH 和 BLUETOOTH_ADMIN 权限
         * 细分为更具体的 BLUETOOTH_SCAN、BLUETOOTH_CONNECT 和 BLUETOOTH_ADVERTISE 权限。
         * 
         * 此方法会根据当前设备的 Android 版本自动返回对应的权限数组：
         * - Android 12 及以上：返回新版蓝牙权限（BLUETOOTH_SCAN, BLUETOOTH_CONNECT）
         * - Android 11 及以下：返回旧版蓝牙权限（BLUETOOTH, BLUETOOTH_ADMIN, ACCESS_FINE_LOCATION）
         * 
         * @return 对应Android版本的蓝牙权限数组
         */
        @JvmStatic
        fun getBluetoothScanAndConnectPermissions(): Array<String> {
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                // Android 12 (API 31) 及以上 - 使用新版蓝牙权限
                BLUETOOTH_SCAN_CONNECT_NEW_PERMISSIONS
            } else {
                // Android 11 及以下 - 使用旧版蓝牙权限
                BLUETOOTH_SCAN_CONNECT_LEGACY_PERMISSIONS
            }
        }

        /**
         * 检查蓝牙权限是否已授予
         * 
         * 根据当前设备的 Android 版本自动检查对应的蓝牙权限：
         * - Android 12 及以上：检查 BLUETOOTH_SCAN 和 BLUETOOTH_CONNECT 权限
         * - Android 11 及以下：检查 BLUETOOTH、BLUETOOTH_ADMIN 和 ACCESS_FINE_LOCATION 权限
         * 
         * @param context 上下文
         * @return true 如果所有必要的蓝牙权限都已授予，false 否则
         */
        @JvmStatic
        fun checkBluetoothScanAndConnectPermission(context: Context): Boolean {
            val bluetoothPermissions = getBluetoothScanAndConnectPermissions()
            return areAllPermissionsGranted(context, bluetoothPermissions)
        }

        //检查是否能调用BluetoothAdapter.enable()
        @JvmStatic
        fun hasBluetoothEnablePermission(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ 需要 BLUETOOTH_CONNECT
                return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                // Android 11及以下需要 BLUETOOTH_ADMIN
                return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_ADMIN
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * 检查是否有蓝牙扫描权限
         * 
         * 根据当前设备的 Android 版本自动检查对应的蓝牙扫描权限：
         * - Android 12 (API 31) 及以上：检查 BLUETOOTH_SCAN 权限
         * - Android 11 及以下：检查 BLUETOOTH 和 ACCESS_FINE_LOCATION 权限
         * 
         * 注意：在 Android 11 及以下版本中，蓝牙扫描需要位置权限，因为蓝牙扫描
         * 可能被用于推断用户位置信息。
         * 
         * @param context 上下文
         * @return true 如果具有蓝牙扫描权限，false 否则
         */
        @JvmStatic
        fun hasBluetoothScanPermission(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ 只需要 BLUETOOTH_SCAN 权限
                return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                // Android 11及以下需要 BLUETOOTH 和 ACCESS_FINE_LOCATION 权限
                val hasBluetoothPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH
                ) == PackageManager.PERMISSION_GRANTED
                
                val hasLocationPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                
                return hasBluetoothPermission && hasLocationPermission
            }
        }

        /**
         * 检查是否有蓝牙连接权限
         * 
         * 根据当前设备的 Android 版本自动检查对应的蓝牙连接权限：
         * - Android 12 (API 31) 及以上：检查 BLUETOOTH_CONNECT 权限
         * - Android 11 及以下：检查 BLUETOOTH 权限
         * 
         * 蓝牙连接权限用于：
         * - 连接到已配对的蓝牙设备
         * - 与蓝牙设备进行数据传输
         * - 控制蓝牙设备的开关状态
         * 
         * @param context 上下文
         * @return true 如果具有蓝牙连接权限，false 否则
         */
        @JvmStatic
        fun hasBluetoothConnectPermission(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ 需要 BLUETOOTH_CONNECT 权限
                return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                // Android 11及以下需要 BLUETOOTH 权限
                return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        


        /**
         * 检查权限是否已授予
         */
        @JvmStatic
        fun isPermissionGranted(context: Context, permission: String): Boolean {
            return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * 检查多个权限是否都已授予
         */
        @JvmStatic
        fun areAllPermissionsGranted(context: Context, permissions: Array<String>): Boolean {
            return permissions.all { isPermissionGranted(context, it) }
        }

        /**
         * 获取已授予的权限列表
         */
        @JvmStatic
        fun getGrantedPermissions(context: Context, permissions: Array<String>): List<String> {
            return permissions.filter { isPermissionGranted(context, it) }
        }

        /**
         * 获取被拒绝的权限列表
         */
        @JvmStatic
        fun getDeniedPermissions(context: Context, permissions: Array<String>): List<String> {
            return permissions.filter { !isPermissionGranted(context, it) }
        }

        /**
         * 获取权限的显示名称
         */
        @JvmStatic
        fun getPermissionDisplayName(permission: String): String {
            return when (permission) {
                CAMERA_PERMISSION -> "相机权限"
                READ_STORAGE_PERMISSION -> "读取存储权限"
                WRITE_STORAGE_PERMISSION -> "写入存储权限"
                FINE_LOCATION_PERMISSION -> "精确位置权限"
                COARSE_LOCATION_PERMISSION -> "粗略位置权限"
                RECORD_AUDIO_PERMISSION -> "录音权限"
                POST_NOTIFICATIONS_PERMISSION -> "通知权限"
                
                // 蓝牙权限显示名称
                BLUETOOTH_PERMISSION -> "蓝牙权限"                    // 旧版蓝牙基础权限
                BLUETOOTH_ADMIN_PERMISSION -> "蓝牙管理权限"           // 旧版蓝牙管理权限
                BLUETOOTH_SCAN_PERMISSION -> "蓝牙扫描权限"            // 新版蓝牙扫描权限
                BLUETOOTH_CONNECT_PERMISSION -> "蓝牙连接权限"         // 新版蓝牙连接权限
                BLUETOOTH_ADVERTISE_PERMISSION -> "蓝牙广播权限"       // 新版蓝牙广播权限
                
                else -> permission
            }
        }

        /**
         * 获取权限数组的显示名称拼接字符串
         * 
         * @param permissions 权限数组
         * @param separator 分隔符，默认为 "、"
         * @return 拼接后的权限显示名称字符串
         */
        @JvmStatic
        @JvmOverloads
        fun getPermissionDisplayNames(permissions: Array<String>, separator: String = "、"): String {
            return permissions.map { getPermissionDisplayName(it) }.joinToString(separator)
        }

        /**
         * 获取权限列表的显示名称拼接字符串
         * 
         * @param permissions 权限列表
         * @param separator 分隔符，默认为 "、"
         * @return 拼接后的权限显示名称字符串
         */
        @JvmStatic
        @JvmOverloads
        fun getPermissionDisplayNames(permissions: List<String>, separator: String = "、"): String {
            return permissions.map { getPermissionDisplayName(it) }.joinToString(separator)
        }

        /**
         * 打开应用设置页面
         */
        @JvmStatic
        fun openAppSettings(context: Context) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }

    /**
     * 权限请求回调接口
     */
    interface PermissionCallback {
        /**
         * 单个权限被授予时的回调（可选实现）
         */
        fun onPermissionGranted(permission: String) {
            // 默认空实现，子类可以选择性重写
        }

        /**
         * 单个权限被拒绝时的回调（可选实现）
         */
        fun onPermissionDenied(permission: String) {
            // 默认空实现，子类可以选择性重写
        }

        /**
         * 单个权限被永久拒绝时的回调（可选实现）
         */
        fun onPermissionPermanentlyDenied(permission: String) {
            // 默认空实现，子类可以选择性重写
        }

        /**
         * 所有权限请求完成后的回调（必须实现）
         */
        fun onAllPermissionsResult(granted: List<String>, denied: List<String>, permanentlyDenied: List<String>)
    }

    private var callback: PermissionCallback? = null
    private var activity: ComponentActivity? = null
    private var multiplePermissionsLauncher: ActivityResultLauncher<Array<String>>? = null
    private var singlePermissionLauncher: ActivityResultLauncher<String>? = null

    /**
     * 初始化权限请求器
     */
    fun init(activity: ComponentActivity, callback: PermissionCallback) {
        this.activity = activity
        this.callback = callback

        // 创建多权限请求启动器
        multiplePermissionsLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handleMultiplePermissionsResult(permissions)
        }

        // 创建单权限请求启动器
        singlePermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            // 单权限请求的回调处理
            Log.d(TAG, "单权限请求结果: $isGranted")
        }
    }


    /**
     * 请求单个权限
     */
    fun requestPermission(permission: String) {
        singlePermissionLauncher?.launch(permission)
    }

    /**
     * 请求多个权限
     */
    fun requestPermissions(permissions: Array<String>) {
        multiplePermissionsLauncher?.launch(permissions)
    }

    /**
     * 请求多个权限（从列表）
     */
    @JvmOverloads
    fun requestPermissions(permissions: List<String>) {
        requestPermissions(permissions.toTypedArray())
    }

    /**
     * 智能请求权限（只请求未授予的权限）
     */
    @JvmOverloads
    fun requestPermissionsIfNeeded(context: Context, permissions: Array<String>) {
        val deniedPermissions = getDeniedPermissions(context, permissions)
        if (deniedPermissions.isNotEmpty()) {
            requestPermissions(deniedPermissions)
        } else {
            Log.i(TAG, "所有权限都已授予")
            callback?.onAllPermissionsResult(permissions.toList(), emptyList(), emptyList())
        }
    }

    /**
     * 处理多权限请求结果
     */
    private fun handleMultiplePermissionsResult(permissions: Map<String, Boolean>) {
        val grantedPermissions = mutableListOf<String>()
        val deniedPermissions = mutableListOf<String>()
        val permanentlyDeniedPermissions = mutableListOf<String>()
        val rationalePermissions = mutableListOf<String>()

        permissions.entries.forEach { (permission, isGranted) ->
            if (isGranted) {
                grantedPermissions.add(permission)
                callback?.onPermissionGranted(permission)
            } else {
                deniedPermissions.add(permission)
                callback?.onPermissionDenied(permission)

                // 检查是否需要显示权限说明
                if (shouldShowRequestPermissionRationale(permission)) {
                    rationalePermissions.add(permission)
                } else {
                    // 永久拒绝的权限
                    permanentlyDeniedPermissions.add(permission)
                    callback?.onPermissionPermanentlyDenied(permission)
                }
            }
        }

        // 打印权限结果日志
        logPermissionResults(grantedPermissions, rationalePermissions, permanentlyDeniedPermissions)

        // 回调总体结果
        callback?.onAllPermissionsResult(grantedPermissions, rationalePermissions, permanentlyDeniedPermissions)

    }

    /**
     * 检查是否需要显示权限说明
     */
    private fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        return activity?.shouldShowRequestPermissionRationale(permission) ?: false
    }

    /**
     * 显示权限说明对话框
     */
    private fun showPermissionRationaleDialog(permissions: List<String>) {
        activity?.let { act ->
            showPermissionRationaleDialog(act, permissions)
        } ?: run {
            Log.w(TAG, "需要显示权限说明: ${permissions.joinToString(", ")}")
        }
    }

    /**
     * 显示设置对话框
     */
    private fun showSettingsDialog(permissions: List<String>) {
        activity?.let { act ->
            showSettingsDialog(act, permissions)
        } ?: run {
            Log.e(TAG, "需要引导到设置页面: ${permissions.joinToString(", ")}")
        }
    }

    /**
     * 打印权限请求结果日志
     */
    private fun logPermissionResults(
        granted: List<String>,
        rationalePermissions: List<String>,
        permanentlyDenied: List<String>
    ) {
        if (granted.isNotEmpty()) {
            Log.i(TAG, "已授予权限：")
            granted.forEach { permission ->
                Log.i(TAG, "✓ ${getPermissionDisplayName(permission)} ($permission)")
            }
        }

        if (rationalePermissions.isNotEmpty()) {
            Log.w(TAG, "被拒绝权限：")
            rationalePermissions.forEach { permission ->
                Log.w(TAG, "✗ ${getPermissionDisplayName(permission)} ($permission)")
            }
        }

        if (permanentlyDenied.isNotEmpty()) {
            Log.e(TAG, "永久拒绝权限：")
            permanentlyDenied.forEach { permission ->
                Log.e(TAG, "⚠ ${getPermissionDisplayName(permission)} ($permission)")
            }
        }

        // 打印总体结果
        val totalGranted = granted.size
        val totalDenied = rationalePermissions.size
        val totalPermanentlyDenied = permanentlyDenied.size
        Log.i(TAG, "权限请求结果统计 - 已授予: $totalGranted, 被拒绝: $totalDenied, 永久拒绝: $totalPermanentlyDenied")
    }


    /**
     * 显示设置对话框（需要Activity实例）
     */
    @JvmOverloads
    fun showSettingsDialog(activity: ComponentActivity, permissions: List<String>) {
        val permissionNames = permissions.map { getPermissionDisplayName(it) }

        AlertDialog.Builder(activity)
            .setTitle("需要手动授予权限")
            .setMessage("以下权限被永久拒绝，请在设置中手动授予：\n${permissionNames.joinToString("\n• ", "• ")}")
            .setPositiveButton("去设置") { _, _ ->
                openAppSettings(activity)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示权限说明对话框（需要Activity实例）
     */
    @JvmOverloads
    fun showPermissionRationaleDialog(activity: ComponentActivity, permissions: List<String>) {
        val permissionNames = permissions.map { getPermissionDisplayName(it) }

        AlertDialog.Builder(activity)
            .setTitle("需要权限")
            .setMessage("应用需要以下权限才能正常工作：\n${permissionNames.joinToString("\n• ", "• ")}")
            .setPositiveButton("授予权限") { _, _ ->
                requestPermissions(permissions)
            }
            .setNegativeButton("取消", null)
            .show()
    }



}