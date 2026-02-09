package com.deepal.ivi.hmi.smartlife.app;


import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.deepal.ivi.hmi.ipvehiclecommon.IApplication;
import com.deepal.ivi.hmi.smartlife.base.BaseApplication;
import com.deepal.ivi.hmi.smartlife.instrumentPanel.BootAutoConnectIp;
import com.deepal.ivi.hmi.smartlife.utils.ShellUtils;
import com.zkjd.lingdong.TuoTuoTieApplication;
import com.zkjd.lingdong.lingdongtie_brige.LingDongTieManager;
import com.deepal.ivi.hmi.smartlife.utils.LocalStoreManager;
import com.mine.baselibrary.constants.CarPlatformConstants;
import com.zkjd.lingdong.service.BleService;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dagger.hilt.android.HiltAndroidApp;
import timber.log.Timber;

@HiltAndroidApp
public class SmartLifeApp extends BaseApplication implements Application.ActivityLifecycleCallbacks {
    private static Application sInstance;
    private static final String TAG = "SmartLifeApp";
    private Activity currActivity;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "smartlife version:1.0.0");

        sInstance = this;
        Timber.plant(new Timber.DebugTree());
        LocalStoreManager.getInstance().init(this);
        // 注册Activity生命周期回调
        registerActivityLifecycleCallbacks(this);
        if(CarPlatformConstants.INSTANCE.isIncludeInstrumentPanel()) {
            String[] cmd = {"/system/bin/sh", "-c", "ifconfig"};
            String output = ShellUtils.exec(cmd);
            IApplication.init(this);
            BootAutoConnectIp.init();
        }else{
            Log.i(TAG,"NO InstrumentPanel");
        }
        TuoTuoTieApplication.INSTANCE.init(this);
        LingDongTieManager.Companion.initLog();
        BleService.Companion.startService(this);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

    /**
     * 获得当前app运行的Application
     */
    public static Application getInstance() {
        if (sInstance == null) {
            throw new NullPointerException("please call setApplication.");
        }
        return sInstance;
    }


    public Activity getCurrActivity() {
        return currActivity;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
        this.currActivity = activity;
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        this.currActivity = activity;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }
}
