package com.deepal.ivi.hmi.ipvehiclecommon.service;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.deepal.ivi.hmi.ipvehiclecommon.R;
import com.deepal.ivi.hmi.ipcommon.data.DispatchData;
import com.deepal.ivi.hmi.ipcommon.iInterface.DataCallBack;
import com.deepal.ivi.hmi.ipcommon.util.AndroidUtil;

public class MonitorService extends Service {
    private static final String TAG = "MonitorService";
    private static DataCallBack lightDataCallBack;
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "BrightnessMonitorChannel";
    private ContentObserver brightnessObserver;
    private  Context mContext;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MonitorService onCreate");
        createNotificationChannel();
        startForeground();
        registerBrightnessObserver();
    }

    public static void setBrightnessChangeListener(DataCallBack callBack) {
        lightDataCallBack = callBack;
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Service Monitor", NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Monitors screen brightness changes");
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);
    }

    private void startForeground() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("亮度监听中")
                .setContentText("正在监听屏幕亮度变化")
                .setSmallIcon(R.drawable.logo) // 使用您的应用图标
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    private void registerBrightnessObserver() {
        brightnessObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                try {
                    int brightness = Settings.System.getInt(
                            getContentResolver(),
                            "main_control_brightness"
                    );
                    int lightRatio = AndroidUtil.getBrightnessLevel();
                    Log.d(TAG, "亮度更新: " + brightness+", lightRatio:" + lightRatio);
                    if (lightDataCallBack != null){
                        lightDataCallBack.onDataChange(new DispatchData(lightRatio, "light"));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "获取亮度失败", e);
                }
            }
        };
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor("main_control_brightness"),
                true,
                brightnessObserver
        );
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (brightnessObserver != null) {
            getContentResolver().unregisterContentObserver(brightnessObserver);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}