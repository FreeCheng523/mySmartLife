package com.deepal.ivi.hmi.smartlife.windowview;

import static android.content.Context.WINDOW_SERVICE;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.deepal.ivi.hmi.smartlife.databinding.ToastDeviceConnectedBinding;

public class DeviceConnectedWindowView extends FrameLayout {
    private static final String TAG = "DeviceConnectedWindowView";
    private ToastDeviceConnectedBinding binding;

    public DeviceConnectedWindowView(Context context) {
        super(context, null);
        init(context);
    }

    public DeviceConnectedWindowView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public DeviceConnectedWindowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void init(Context context) {
        binding = ToastDeviceConnectedBinding.inflate(LayoutInflater.from(context), this, true);

        binding.toastOutBox.setOnClickListener(v -> {
            //dismiss();
        });

        binding.toastContentContainer.setOnClickListener(v -> {

        });

        binding.toastBackImage.setOnClickListener(v -> {
            dismiss();
        });
    }

    public void dismiss() {
        WindowManager windowManager = (WindowManager) getContext().getSystemService(WINDOW_SERVICE);
        windowManager.removeView(this);
    }
}