package com.deepal.ivi.hmi.smartlife.instrumentPanel;

import android.util.Log;

import androidx.lifecycle.Observer;

import com.deepal.ivi.hmi.ipvehiclecommon.IApplication;
import com.deepal.ivi.hmi.ipvehiclecommon.viewmode.InstrumentPanelViewModel;
import com.deepal.ivi.hmi.smartlife.bean.SmartDevice;
import com.deepal.ivi.hmi.smartlife.utils.LocalStoreManager;
import com.deepal.ivi.hmi.smartlife.utils.UsbConnectionUtil;

import java.util.List;

public class BootAutoConnectIp {
    private final static String TAG = "BootAutoConnectIp";
    private static List<SmartDevice> devices;

    public static void init( ) {
        Log.i(TAG, "小仪表连接前日志输出");
        updateIpCardStatus(0);
        autoConnectInstrmengPanel();

    }

    private static void autoConnectInstrmengPanel() {
        boolean ipExists = UsbConnectionUtil.checkIpDeviceExist(devices);
        Log.d(TAG, ipExists?"准备启动小仪表连接socket":"小仪表不存在");
        if (ipExists){
            Log.i(TAG, "启动小仪表服务并连接socket");
            InstrumentPanelViewModel viewModel = IApplication.getInstrumentPanelViewModel();
            Log.i(TAG, "BootCompleteReceiver 的viewModel == "+viewModel);
            viewModel.init();
            viewModel.isClientConnected.observeForever(observerIpConnected);
            viewModel.startServer();
        }
    }

    public static void updateIpCardStatus(int operation) {
        Log.d(TAG, "更新小仪表状态");
        // 获取当前设备列表
        devices = LocalStoreManager.getInstance().getStoreData("key.store.device", SmartDevice.class);
        Log.i(TAG, "当前设备列表: " + devices);
        if (devices != null && !devices.isEmpty()) {
            boolean updated = false;
            // 遍历并更新小仪表状态为未连接（假设小仪表的 deviceType 是 1）
            for (SmartDevice device : devices) {
                if (device.getDeviceType() == 1 && device.getConnectStatus() == 3 && operation == 0) {
                    device.setConnectStatus(1); // 设置为未连接
                    updated = true;
                }else if (device.getDeviceType() == 1 && device.getConnectStatus() == 1 && operation == 1){
                    device.setConnectStatus(3); //设置为连接
                    updated = true;
                }
            }
            // 如果有更新，则保存回本地存储
            if (updated) {
                LocalStoreManager.getInstance().storeData("key.store.device", devices);
                Log.d(TAG, "更新本地存储状态 Updated meter status to disconnected and saved.");
            }
        }
    }

    private static Observer<Integer> observerIpConnected = (connecteStatus)-> {
        Log.i(TAG, "小仪表连接状态:" + (connecteStatus==1?"已连接":"未连接"));
        if (connecteStatus==1){
            updateIpCardStatus(1);
        }
    };
}
