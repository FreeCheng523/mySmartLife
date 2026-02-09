package com.deepal.ivi.hmi.ipvehiclecommon;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;

import com.deepal.ivi.hmi.ipcommon.IPcApplication;
import com.deepal.ivi.hmi.ipvehiclecommon.model.VehicleDataManager;
import com.deepal.ivi.hmi.ipvehiclecommon.viewmode.InstrumentPanelViewModel;

/*
* tinnovecommon
* */
public class IApplication{
    private static final String TAG = "IApplication";
    private static Application  instance;
    private static InstrumentPanelViewModel viewModel;
    private static Activity currActivity;

    public static Application getInstance() {
        return instance;
    }

    public static void init(Application application) {
        Log.i(TAG, "小仪表的APPlication被调用 开始init");
        instance = application;
        // 在Application中直接持有ViewModel,保证ViewModel唯一
        initializeViewModel();
        IPcApplication.init(application);
        VehicleDataManager.getInstance().init(application.getApplicationContext());
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                currActivity = activity;
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                currActivity = activity;
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                currActivity = activity;
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                // 不做处理
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                // 不做处理
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
                // 不做处理
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                if (currActivity == activity) {
                }
            }
        });
    }

    private static void initializeViewModel() {
        ViewModelStore viewModelStore = new ViewModelStore();
        ViewModelProvider.Factory factory = ViewModelProvider.AndroidViewModelFactory.getInstance(instance);
        ViewModelProvider provider = new ViewModelProvider(viewModelStore, factory);
        viewModel = provider.get(InstrumentPanelViewModel.class);
        Log.i(TAG, "小仪表的APPlication被调用 获取ViewModel == "+viewModel);
    }

    public static InstrumentPanelViewModel getInstrumentPanelViewModel() {
        if (viewModel == null){
            initializeViewModel();
        }
        return viewModel;
    }

    public static Activity getCurrActivity() {
        return currActivity;
    }
}
