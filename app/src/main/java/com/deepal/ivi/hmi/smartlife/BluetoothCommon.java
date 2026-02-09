package com.deepal.ivi.hmi.smartlife;
//
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.core.content.ContextCompat;

import com.mine.baselibrary.permission.PermissionUtil;


public class BluetoothCommon {
    private static final String TAG = "BluetoothCommon";
    private final Context mContext;
    private final BluetoothAdapter mBluetoothAdapter;

    public BluetoothCommon(Context context) {
        mContext = context.getApplicationContext();
        BluetoothManager manager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager != null ? manager.getAdapter() : null;
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported");
        }
    }

    /**
     * 尝试打开蓝牙（如未授权，则返回 false，由外部 Activity 申请权限后再重试）
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public boolean openBt() {
        if (mBluetoothAdapter == null) return false;
  
        if(!PermissionUtil.hasBluetoothEnablePermission(mContext)){
            Log.w(TAG, "权限BLUETOOTH_CONNECT | BLUETOOTH_ADMIN没权限");
            return false;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
            Log.i(TAG, "尝试打开蓝牙成功");
        }
        return true;
    }
    public static boolean isBluetoothEnabled() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter != null && adapter.isEnabled();
    }
}