package com.mine.baselibrary.window;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.mine.baselibrary.R;


/**
 * todo width方向上 WindowManager.LayoutParams.WRAP_CONTENT失效
 * <p>
 * note:
 * 使用windowmanger添加窗口时
 * context为aplication context 需要
 * <p>
 * WindowManager.LayoutParams.TYPE_APPLICATION 普通应用使用，只能在应用内显示，context必须为Activity
 * <p>
 * WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 普通应用使用，需要SYSTEM_ALERT_WINDOW权限，能在应用外显示
 * <p>
 * WindowManager.LayoutParams.TYPE_TOAST  系统应用能用，能在应用外显示
 * <p>
 * <p>
 * 申请了全局悬浮窗权限(<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />)，才可以添加type为WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY的窗口
 */
public class ToastUtilOverApplication {

    private static String TAG = "ToastUtilOverApplication";

    private static View lastToast = null;
    private WindowManager windowManager;
    private View toastView;


    /**
     * 如果overlayApplication为false，则type = WindowManager.LayoutParams.TYPE_APPLICATION，此时Context必须为Activity的context
     *
     * @param context
     * @param message
     */
    public void showToast(Context context, String message) {

        if (!Settings.canDrawOverlays(context)) {
            Log.e("toast","can't canDrawOverlays");
            return;
        }

        if (windowManager == null) {
            windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        }

        if (toastView == null) {
            toastView = LayoutInflater.from(context).inflate(R.layout.toast, null);
        }

        if (lastToast != null && lastToast.isAttachedToWindow()) {
            try {
                lastToast.removeCallbacks(null);
                windowManager.removeView(lastToast);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        lastToast = toastView;


        View lastToast = LayoutInflater.from(context).inflate(R.layout.toast, null);
        TextView textView = lastToast.findViewById(R.id.toast_message);
        textView.setText(message);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        // | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);

        params.width = 978;
        params.height = 238;
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.x = 0;
        params.y = 900; // 你可以根据需要调整 Y 坐标

        try {
            new android.os.Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    windowManager.addView(lastToast, params);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 定义显示时间
        lastToast.postDelayed(() -> {
            try {
                if (lastToast.isAttachedToWindow()) {
                    windowManager.removeView(lastToast);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }, 3000);
    }
}
