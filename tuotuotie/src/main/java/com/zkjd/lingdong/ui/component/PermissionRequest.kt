package com.zkjd.lingdong.ui.component

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * 蓝牙权限检查和请求组件
 */
@Composable
fun BluetoothPermissionRequest(
    content: @Composable () -> Unit,
    onPermissionResult: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }
    
    // 定义需要的蓝牙权限列表
    val bluetoothPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+需要BLUETOOTH_SCAN和BLUETOOTH_CONNECT权限
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            // 低版本Android需要BLUETOOTH和BLUETOOTH_ADMIN权限
            // 以及位置权限(用于蓝牙扫描)
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }
    
    // 检查是否已经授予了所有所需权限
    val allPermissionsGranted = remember(context) {
        bluetoothPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // 所有权限都已授予
            onPermissionResult(true)
        } else {
            // 至少有一个权限被拒绝
            showRationale = true
            onPermissionResult(false)
        }
    }
    
    LaunchedEffect(allPermissionsGranted) {
        if (allPermissionsGranted) {
            onPermissionResult(true)
        } else {
            // 请求所有需要的权限
            permissionLauncher.launch(bluetoothPermissions)
        }
    }
    
    if (showRationale) {
        PermissionRationaleDialog(
            onDismiss = { showRationale = false },
            onGoToSettings = {
                showRationale = false
                openAppSettings(context)
            }
        )
    }
    
    if (allPermissionsGranted) {
        content()
    } else {
        PermissionRequiredScreen()
    }
}

/**
 * 需要权限时显示的屏幕
 */
@Composable
private fun PermissionRequiredScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "需要蓝牙权限",
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "请授予蓝牙权限以允许应用扫描和连接蓝牙设备。",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            CircularProgressIndicator()
        }
    }
}

/**
 * 权限说明对话框
 */
@Composable
private fun PermissionRationaleDialog(
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("需要权限") },
        text = { 
            Text("没有蓝牙权限，应用将无法扫描和连接蓝牙设备。请在设置中手动授予权限。") 
        },
        confirmButton = {
            TextButton(onClick = onGoToSettings) {
                Text("前往设置")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 打开应用设置页面
 */
private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

/**
 * 检查是否有蓝牙权限
 */
fun hasBluetoothPermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+需要BLUETOOTH_SCAN和BLUETOOTH_CONNECT权限
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == 
                PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == 
                PackageManager.PERMISSION_GRANTED
    } else {
        // 低版本Android需要BLUETOOTH和BLUETOOTH_ADMIN权限以及位置权限
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == 
                PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == 
                PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == 
                PackageManager.PERMISSION_GRANTED
    }
} 