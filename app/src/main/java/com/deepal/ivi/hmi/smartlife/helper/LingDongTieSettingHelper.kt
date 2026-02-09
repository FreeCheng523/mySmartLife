package com.deepal.ivi.hmi.smartlife.helper

import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.deepal.ivi.hmi.smartlife.MainActivity
import com.deepal.ivi.hmi.smartlife.bean.AgileSmartDevice
import com.zkjd.lingdong.lingdongtie_brige.LingDongTieManager
import com.zkjd.lingdong.model.ConnectionState
import com.zkjd.lingdong.model.Device
import com.zkjd.lingdong.ui.home.HomeViewModel
import com.zkjd.lingdong.ui.pairing.PairingAndConnectViewModel
import com.zkjd.lingdong.ui.scanning.ScannningViewModel
import timber.log.Timber

/**
 * 灵动贴设备设置辅助类
 * 用于隔离 MainActivity 中的灵动贴设备相关逻辑
 */
object LingDongTieSettingHelper {
    private const val TAG = "MainActivity"

    /**
     * 初始化灵动贴设备相关功能
     *
     * @param activity MainActivity 实例
     * @return LingDongTieManager 实例
     */
    @JvmStatic
    fun setupLingDongTie(activity: MainActivity): LingDongTieManager {
        val pairingViewModel = ViewModelProvider(activity)
            .get<PairingAndConnectViewModel>(PairingAndConnectViewModel::class.java)

        val scanningViewModel = ViewModelProvider(activity)
            .get<ScannningViewModel>(ScannningViewModel::class.java)

        val homeViewModel = ViewModelProvider(activity)
            .get<HomeViewModel>(HomeViewModel::class.java)

        return LingDongTieManager.Builder()
            .setLifecycle(activity.lifecycle)
            .setScanningViewModel(scanningViewModel)
            .setPairingViewModel(pairingViewModel)
            .setHomeViewModel(homeViewModel)
            .setContext(activity)
            .setOnDeviceAddedCallback { dataBaseDevices: List<Device>? ->
                handleDevicesAdded(activity, dataBaseDevices!!)
            }
            .setConnectedResultCallback { connected: Boolean?, address: String? ->
                handleConnectedResult(activity, connected!!, address)
            }
            .setDeviceChangedCallback { dataBaseDevices: List<Device?>? ->
                handleDeviceChanged(activity, dataBaseDevices)
            }
            .setNeedOpenBluetoothCallback { Unit }
            .setStartConnectCallback { Unit }
            .build()
    }

    /**
     * 处理新增设备
     */
    private fun handleDevicesAdded(
        activity: MainActivity,
        dataBaseTuoTuoTieDevices: List<Device>
    ) {
        Timber.tag(TAG)
            .i("scanned device " + dataBaseTuoTuoTieDevices.toString() + "|" + Thread.currentThread())

        // ✅ 移除：不再从SP读取设备，只检查内存列表
        
        // 检查内存列表中是否有数据库中的设备（防止重复添加）
        val addedDevices: MutableList<Device> = ArrayList<Device>()
        for (dataBaseDevice in dataBaseTuoTuoTieDevices) {
            val dpCheckedAddress = dataBaseDevice.macAddress

            // 只检查内存列表中的设备
            val memoryHasDevice = activity.isDeviceInMemory(dpCheckedAddress)

            // 只有当内存列表中不存在该设备时，才添加到待添加列表
            if (!memoryHasDevice) {
                addedDevices.add(dataBaseDevice)
            } else {
                Log.i(
                    TAG, "设备已存在，跳过: " + dpCheckedAddress +
                            " (内存中存在: " + memoryHasDevice + ")"
                )
            }
        }

        if (!addedDevices.isEmpty()) {
            // 新的妥妥贴设备
            for (addedDevice in addedDevices) {
                var connectStatus = 1
                if (addedDevice.lastConnectionState == ConnectionState.CONNECTED) {
                    connectStatus = 3
                }

                val device = AgileSmartDevice(addedDevice.name, 3, 1, connectStatus,addedDevice.createdAt,addedDevice.bleName)

                // 设置基本属性
                device.setMacAddress(addedDevice.macAddress) // 设置MAC地址
                if (addedDevice.batteryLevel == null) {
                    device.setBatteryLevel(0) // 设置电池电量
                } else {
                    device.setBatteryLevel(addedDevice.batteryLevel!!) // 设置电池电量
                }

                Timber.tag(TAG).i("添加妥妥贴设备: " + device)

                activity.runOnUiThread(Runnable {
                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        activity.addNewDevicePublic(device)
                    }
                })
            }

            activity.dismissSearchDialogIfShowing()
            Log.i(TAG, "设备" + addedDevices)
        }
    }

    /**
     * 处理连接结果
     */
    private fun handleConnectedResult(
        activity: MainActivity,
        connected: Boolean,
        address: String?
    ) {
        // 连接成功的弹框，是通过轮询查询的，所以这里不处理连接成功，弹出弹框
        if (!connected) {
            activity.showTuoTuoTieConnectFailDialog(address)
        }
    }

    /**
     * 处理设备状态变化
     */
    private fun handleDeviceChanged(activity: MainActivity, dataBaseDevices: List<Device?>?) {
        Log.i(TAG, "数据变动: " + dataBaseDevices + " thread " + Thread.currentThread())
        if(dataBaseDevices==null){
            return
        }
        // 都有的设备，检查设备信息是否有变化，如果有更新状态
        for (dataBaseDevice in dataBaseDevices) {
            activity.updateLingDongTieStatus(dataBaseDevice)
        }
    }
}

