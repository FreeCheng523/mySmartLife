package com.deepal.ivi.hmi.smartlife;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.deepal.ivi.hmi.smartlife.bean.SmartDevice;

public class DeviceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "DeviceBroadcastReceiver";
    public DeviceBroadcastReceiver instance;
    private Activity activity;
    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";


    public DeviceBroadcastReceiver() {
    }
    public DeviceBroadcastReceiver(Activity activity) {
        this.activity = activity;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "收到广播（receive action):" + action);
        switch (action){
            //开机广播
            case Intent.ACTION_BOOT_COMPLETED:
                Log.i(TAG, "开机广播");
                break;

            case Intent.ACTION_SHUTDOWN:

                break;

            //USB插入广播
            case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                // 处理设备连接
                Log.d("InstrumentPanelDialog", "小仪表设备插入: " + device.getDeviceName());
                break;

            //USB拔出广播
            case UsbManager.ACTION_USB_DEVICE_DETACHED:
                break;
        }



        if (ACTION_USB_PERMISSION.equals(action)) {
            // 获取权限结果的广播
            synchronized (this) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    //call method to set up device communication
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.e("USBReceiver", "获取权限成功：" + device.getDeviceName());
                    } else {
                        Log.e("USBReceiver", "获取权限失败：" + device.getDeviceName());
                    }
                }
            }
        } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            // 有新的设备插入了，在这里一般会判断这个设备是不是我们想要的，是的话就去请求权限
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            // 有设备拔出了
        }
    }

    public DeviceBroadcastReceiver getInstance() {
        if (instance == null){
            synchronized (DeviceBroadcastReceiver.class){
                if (instance == null) {
                    instance = new DeviceBroadcastReceiver();
                }
            }
        }
        return instance;
    }
}
