package com.deepal.ivi.hmi.smartlife.helper;

import android.util.Log;

import androidx.lifecycle.ViewModelProvider;
import com.deepal.ivi.hmi.smartlife.MainActivity;
import com.deepal.ivi.hmi.smartlife.bean.SmartDevice;
import com.smartlife.fragrance.bridge.FragranceManager;
import com.smartlife.fragrance.data.model.FragranceDevice;
import com.smartlife.fragrance.ui.connect.ConnectViewModel;
import com.smartlife.fragrance.ui.status.DeviceStatusViewModel;

import kotlin.Unit;
import timber.log.Timber;

import java.util.ArrayList;
import java.util.List;

/**
 * 香薰设备设置辅助类
 * 用于隔离 MainActivity 中的香薰设备相关逻辑
 */
public class FragranceSettingHelper {
    private static final String TAG = "MainActivity";
    
    /**
     * 初始化香薰设备相关功能
     * @param activity MainActivity 实例
     * @return FragranceManager 实例
     */
    public static FragranceManager setupFragrance(MainActivity activity) {
        // 初始化香薰相关的 ViewModel
        com.smartlife.fragrance.ui.scanning.FragranceScanningViewModel fragranceScanningViewModel =
                new ViewModelProvider(activity).get(com.smartlife.fragrance.ui.scanning.FragranceScanningViewModel.class);

        ConnectViewModel fragranceConnectViewModel = 
                new ViewModelProvider(activity).get(ConnectViewModel.class);

        DeviceStatusViewModel fragranceDeviceStatusViewModel = 
                new ViewModelProvider(activity).get(DeviceStatusViewModel.class);

        // 使用 Builder 模式创建实例
        return new FragranceManager.Builder()
                .setLifecycle(activity.getLifecycle())
                .setScanningViewModel(fragranceScanningViewModel)
                .setConnectViewModel(fragranceConnectViewModel)
                .setDeviceStatusViewModel(fragranceDeviceStatusViewModel)
                .setonDeviceChange(fragranceDevices -> {
                    handleFragranceDeviceChanged(activity, fragranceDevices);
                    return Unit.INSTANCE;
                })
                .setContext(activity)
                .setOnDeviceAddedCallback(fragranceDevices -> {
                    handleFragranceDevicesAdded(activity, fragranceDevices);
                    return  Unit.INSTANCE;
                })
                .setConnectedResultCallback((success, macAddress) -> {
                    handleFragranceConnectResult(activity, success, macAddress);
                    return  Unit.INSTANCE;
                })
                .setNeedOpenBluetoothCallback(() -> {
                    activity.runOnUiThread(activity::showBluetoothDialog);
                    return  Unit.INSTANCE;
                })
                .build();
    }
    
    /**
     * 处理扫描到的香薰设备
     */
    private static void handleFragranceDevicesAdded(MainActivity activity, List<FragranceDevice> fragranceDevices) {
        // 处理扫描到的香薰设备
        Timber.tag(TAG).i("扫描到香薰设备: " + fragranceDevices.toString() + " | " + Thread.currentThread());

        // ✅ 移除：不再从SP读取设备，只检查内存列表

        // 检查内存列表中是否有数据库中的设备（防止重复添加）
        List<FragranceDevice> addedDevices = new ArrayList<>();
        for (FragranceDevice fragranceDevice : fragranceDevices) {
            String checkedAddress = fragranceDevice.getMacAddress();

            // 只检查内存列表中的设备
            boolean memoryHasDevice = activity.isDeviceInMemory(checkedAddress);

            // 只有当内存列表中不存在该设备时，才添加到待添加列表
            if (!memoryHasDevice) {
                addedDevices.add(fragranceDevice);
            } else {
                Log.i(TAG, "香薰设备已存在，跳过: " + checkedAddress + 
                        " (内存中存在: " + memoryHasDevice + ")");
            }
        }

        if (!addedDevices.isEmpty()) {
            // 新的香薰设备
            for (FragranceDevice addedDevice : addedDevices) {
                int connectStatus = 1;
                if (addedDevice.getConnectionState() == com.smartlife.fragrance.data.model.ConnectionState.CONNECTED) {
                    connectStatus = 3;
                }

                SmartDevice device = new SmartDevice(
                        addedDevice.getDisplayName(),
                        4,  // deviceType: 4 表示香薰设备
                        1,  // deviceCategory
                        connectStatus,
                        addedDevice.getCreatedAt(),
                        ""
                );

                // 设置基本属性
                device.setDeviceId(addedDevice.getMacAddress());
                device.setDeviceBattery(0); // 香薰设备可能没有电池电量

                Log.i(TAG, "添加香薰设备: " + device);

                activity.runOnUiThread(() -> {
                    if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                        activity.addNewDevicePublic(device);
                    }
                });
            }

            activity.dismissSearchDialogIfShowing();
            Log.i(TAG, "添加香薰设备: " + addedDevices);
        }
    }
    
    /**
     * 处理香薰设备连接结果
     */
    private static void handleFragranceConnectResult(MainActivity activity, Boolean success, String macAddress) {
        // 处理连接结果（主要处理失败情况）
        if (!success) {
            activity.runOnUiThread(() -> {
                activity.showFragranceConnectFailDialog(macAddress);
            });
        }
    }
    
    /**
     * 处理香薰设备状态变化
     */
    private static void handleFragranceDeviceChanged(MainActivity activity, List<FragranceDevice> fragranceDevices) {
        Log.i(TAG, "香薰设备数据变动: " + fragranceDevices + " thread " + Thread.currentThread());

        // 都有的设备，检查设备信息是否有变化，如果有更新状态
        for (FragranceDevice fragranceDevice : fragranceDevices) {
            activity.updateFragranceDeviceStatus(fragranceDevice);
        }
    }
}

