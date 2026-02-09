package com.deepal.ivi.hmi.ipcommon;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class IPcApplication {
    private static final String TAG = "IPcApplication";
    private static Application  instance;
    private static Activity currActivity;

    public static void init(Application application) {
        instance = application;
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
                    // currActivity = null;
                }
            }
        });
    }

    public static Application  getInstance() {
        return instance;
    }

    public static Activity getCurrActivity() {
        return currActivity;
    }
}
