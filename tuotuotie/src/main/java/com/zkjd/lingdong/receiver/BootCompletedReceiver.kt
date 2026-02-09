package com.zkjd.lingdong.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.zkjd.lingdong.data.FunctionsConfig
import com.zkjd.lingdong.service.BleService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 开机启动接收器，用于开机时自动启动后台服务
 */
class BootCompletedReceiver : BroadcastReceiver() {
    
    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onReceive(context: Context, intent: Intent) {
        Timber.tag("BootCompletedReceiver").e("收到广播")
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Timber.tag("BootCompletedReceiver").e("收到开机广播")
            
            receiverScope.launch {
                // 启动服务
                Timber.tag("BootCompletedReceiver").e("启动后台服务")
                startBleService(context)
            }
        }
        else{
            Timber.tag("BootCompletedReceiver").e("收到其他广播: ${intent.action}")
            if (FunctionsConfig.getInstance(context).getIs8295()) {
                val action = intent.action
                if (action == "mega.intent.action.VEHICLE_IGNITION_ON") {
                    Timber.tag("BootCompletedReceiver").d("收到车载点火广播")
                    startBleService(context)
                }
            }
        }
    }
    

    /**
     * 启动蓝牙服务
     */
    private fun startBleService(context: Context) {
        BleService.startService(context)
    }
} 