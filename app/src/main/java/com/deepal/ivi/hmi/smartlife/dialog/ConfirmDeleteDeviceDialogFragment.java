package com.deepal.ivi.hmi.smartlife.dialog;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.deepal.ivi.hmi.smartlife.databinding.ToastConfirmDeleteDeviceBinding;
import com.mine.baselibrary.dialog.BaseDialogFragment;

public class ConfirmDeleteDeviceDialogFragment extends BaseDialogFragment<ToastConfirmDeleteDeviceBinding> {
    private static final String TAG = "ConfirmDeleteDeviceDialogFragment";

    private OnConfirmDeleteListener onDeleteListener;

    public ConfirmDeleteDeviceDialogFragment() {
        super(
                true,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true,
                false
        );
    }

    @Nullable
    @Override
    protected ToastConfirmDeleteDeviceBinding createViewBinding(@Nullable LayoutInflater inflater, @Nullable ViewGroup container) {
        if (inflater == null) {
            return null;
        }
        return ToastConfirmDeleteDeviceBinding.inflate(inflater, container, false);
    }


    @Nullable
    @Override
    protected View getContentLayout() {
        return binding != null ? binding.getRoot() : null;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
    }

    private void setupViews() {
        Log.d(TAG, "ConfirmDeleteDeviceDialogFragment 已创建");

        if (binding == null) return;

        // 点击外部背景 dismiss
        binding.toastOutBox.setOnClickListener(v -> {
            dismiss();
            Log.d(TAG, "点击外部背景关闭");
        });

        // 阻止内容区域点击事件传递
        binding.toastContentContainer.setOnClickListener(v -> {
        });

        // 取消按钮
        binding.confirmDeleteNo.setOnClickListener(v -> {
            dismiss();
            Log.i(TAG, "confirmDeleteNo: 用户取消删除");
        });

        // 确认删除按钮
        binding.confirmDeleteYes.setOnClickListener(v -> {
            if (onDeleteListener != null) {
                onDeleteListener.onConfirmDelete();
                Log.i(TAG, "confirmDeleteYes: 用户确认删除");
            }
            dismiss();
        });
    }

    public static ConfirmDeleteDeviceDialogFragment newInstance() {
        return new ConfirmDeleteDeviceDialogFragment();
    }

    public void setOnConfirmDeleteListener(OnConfirmDeleteListener listener) {
        this.onDeleteListener = listener;
    }

    public interface OnConfirmDeleteListener {
        void onConfirmDelete();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "ConfirmDeleteDeviceDialogFragment 视图销毁");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "ConfirmDeleteDeviceDialogFragment 完全销毁");
        super.onDestroy();
    }
}