package com.deepal.ivi.hmi.smartlife.utils;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

public class BlurBackgroundHelper {

    public static void applyBlurBehind(@NonNull Dialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) return;

        // 5.0+ 支持模糊背景
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            window.getAttributes().setBlurBehindRadius(50); // 模糊半径
            window.setDimAmount(0f)              ;    // 去掉暗淡


        } else {
            // 低版本用透明+毛玻璃背景图兼容
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.6f); // 暗淡程度
        }
    }
}