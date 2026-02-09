package com.deepal.ivi.hmi.smartlife.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.deepal.ivi.hmi.smartlife.databinding.ToastConfirmDeleteDeviceBinding;
import com.deepal.ivi.hmi.smartlife.utils.BlurBackgroundHelper;

public class ConfirmDeleteDeviceDialog extends Dialog {
    private static final String TAG = "ConfirmDeleteDeviceDialog";
    private ToastConfirmDeleteDeviceBinding binding;
    private OnConfirmDeleteListener onConfirmDeleteListener;

    public ConfirmDeleteDeviceDialog(@NonNull Context context) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ToastConfirmDeleteDeviceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupWindow();
        initViews();
    }

    private void setupWindow() {
        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER;

            // 保持原有的 flags

            params.format = android.graphics.PixelFormat.TRANSLUCENT;

            window.setAttributes(params);
        }
    }

    private void initViews() {
        Log.d(TAG, "ConfirmDeleteDeviceDialog 已创建");

        // 点击外部背景 dismiss
        binding.toastOutBox.setOnClickListener(v -> {
            dismiss();
        });

        // 阻止内容区域点击事件传递（避免点击内容区域也dismiss）
        binding.toastContentContainer.setOnClickListener(v -> {
            // 空实现，阻止事件传递
        });

        // 取消按钮
        binding.confirmDeleteNo.setOnClickListener(v -> {
            dismiss();
            Log.i(TAG, "confirmDeleteNo");
        });

        // 确认删除按钮
        binding.confirmDeleteYes.setOnClickListener(v -> {
            if (onConfirmDeleteListener != null) {
                onConfirmDeleteListener.onConfirmDelete();
                Log.i(TAG, "confirmDeleteYes");
            }
            dismiss(); // 执行删除操作后关闭对话框
        });
    }

    /**
     * 显示带模糊效果的对话框
     */
    public void showWithBlur() {
        BlurBackgroundHelper.applyBlurBehind(this);
        show();
    }

    /**
     * 普通显示（无模糊效果）
     */
    @Override
    public void show() {
        super.show();
    }

    public interface OnConfirmDeleteListener {
        void onConfirmDelete();
    }

    public void setOnConfirmDeleteListener(OnConfirmDeleteListener listener) {
        this.onConfirmDeleteListener = listener;
    }

    @Override
    public void dismiss() {
        super.dismiss();
        Log.d(TAG, "ConfirmDeleteDeviceDialog 已关闭");
    }
}