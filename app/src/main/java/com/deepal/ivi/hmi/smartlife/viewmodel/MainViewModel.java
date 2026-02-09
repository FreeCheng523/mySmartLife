package com.deepal.ivi.hmi.smartlife.viewmodel;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.adayo.service.utils.MKDisplayStatus;
import com.deepal.ivi.hmi.smartlife.app.SmartLifeApp;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class MainViewModel extends ViewModel  {
    private static final String TAG = "MainViewModel";
    public MutableLiveData<Integer> displayMKStatusLiveData = new MutableLiveData<>();
    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduledFuture;
    private int lastDisplayMKStatus = Integer.MIN_VALUE;
    public MainViewModel() {

    }

    public void init() {
        Log.i(TAG, "MainViewModel init");
    }

    public void startObserveDisplayMKStatus() {
        if (executorService == null) {
            executorService = Executors.newSingleThreadScheduledExecutor();
        }

        scheduledFuture = executorService.scheduleWithFixedDelay(() -> {
            int currentDisplayMKStatus = MKDisplayStatus.INSTANCE.displayMKStatus(SmartLifeApp.getInstance());
            //仅当状态改变时，才更新LiveData
            if (currentDisplayMKStatus != lastDisplayMKStatus) {
                displayMKStatusLiveData.postValue(currentDisplayMKStatus);
                lastDisplayMKStatus = currentDisplayMKStatus;
            }
        }, 0, 1, TimeUnit.SECONDS); // 初始延迟0秒，之后每隔1秒执行
    }

    public void stopObserveDisplayMKStatus() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopObserveDisplayMKStatus();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}