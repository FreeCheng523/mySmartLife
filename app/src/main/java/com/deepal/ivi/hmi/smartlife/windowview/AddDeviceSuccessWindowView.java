package com.deepal.ivi.hmi.smartlife.windowview;

import static android.content.Context.WINDOW_SERVICE;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.deepal.ivi.hmi.smartlife.databinding.ToastAddDeviceSuccessBinding;

public class AddDeviceSuccessWindowView extends FrameLayout {
    private static final String TAG = "AddDeviceSuccessWindowView";
    private ToastAddDeviceSuccessBinding binding;

    public AddDeviceSuccessWindowView(Context context) {
        super(context, null);
        init(context);
    }

    public AddDeviceSuccessWindowView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public AddDeviceSuccessWindowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void init(Context context) {
        binding = ToastAddDeviceSuccessBinding.inflate(LayoutInflater.from(context), this, true);

        binding.toastOutBox.setOnClickListener(v -> {
            //dismiss();
        });

        binding.toastContentContainer.setOnClickListener(v -> {

        });

        binding.toastLookUpDevice.setOnClickListener(v -> {
            dismiss();
//            DeviceConnectedWindowView deviceConnectedWindowView = new DeviceConnectedWindowView(getContext());
//            WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
//            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
//                    WindowManager.LayoutParams.MATCH_PARENT,
//                    WindowManager.LayoutParams.MATCH_PARENT,
//                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//                    PixelFormat.TRANSLUCENT);
//            windowManager.addView(deviceConnectedWindowView, params);
            if (onLookupDeviceClickListener != null) {
                onLookupDeviceClickListener.onLookupDeviceClick();
            }
        });
    }

    public void dismiss() {
        WindowManager windowManager = (WindowManager) getContext().getSystemService(WINDOW_SERVICE);
        windowManager.removeView(this);
    }

    public interface OnLookupDeviceClickListener {
        void onLookupDeviceClick();
    }

    private OnLookupDeviceClickListener onLookupDeviceClickListener;

    public void setOnLookupDeviceClickListener(OnLookupDeviceClickListener listener) {
        this.onLookupDeviceClickListener = listener;
    }
}