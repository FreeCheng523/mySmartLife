package com.deepal.ivi.hmi.smartlife.utils;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.deepal.ivi.hmi.smartlife.bean.SmartDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class UsbConnectionUtil {

    private static final String TAG = "UsbConnectionUtil";

    public static boolean checkIpDeviceExist(List<SmartDevice>smartDeviceList){
        try {
            for (SmartDevice smartDevice : smartDeviceList ) {
                Log.i(TAG, "已添加设备："+smartDevice.toString());
                if (smartDevice.getDeviceType()==1){
                    return true;
                }
            }
        }
        catch (RuntimeException e){
            Log.e(TAG, "已添加的设备为空: "+e.getMessage());
        }
        Log.w(TAG, "已添加设备中没有小仪表盘");
        return false;
    }

    public static UsbDevice getIpDevice(Context context,int vendorId,int productId){
        UsbDevice usbResultDevice = null;
        HashMap<String, UsbDevice> deviceList = findDevice(context);
        if (deviceList == null) {
            Log.i(TAG, "车机能检测到的USB是空的，Device list is null");
            return null;
        }
        Log.i(TAG, "已检测到USB设备数: " + deviceList.size());
        for (Map.Entry<String, UsbDevice> entry : deviceList.entrySet()) {
            UsbDevice usbDevice = entry.getValue();
            Log.i(TAG, "Found USB device: " + usbDevice.toString());

            // 检查 Vendor ID 和 Product ID
            if (vendorId == usbDevice.getVendorId() && productId == usbDevice.getProductId()) {
                Log.i(TAG, "找到小仪表设备 Found matching USB device: " + usbDevice);
                usbResultDevice = usbDevice;
            }
        }
        return usbResultDevice;
    }

    public static HashMap<String, UsbDevice> findDevice(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            throw new IllegalStateException("UsbManager is null");
        }
        return  usbManager.getDeviceList();
    }
}