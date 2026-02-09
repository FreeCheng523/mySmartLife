package com.deepal.ivi.hmi.smartlife.dialog;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import com.deepal.ivi.hmi.smartlife.dialog.ConfirmDeleteDeviceDialog;
import com.deepal.ivi.hmi.smartlife.databinding.MenuDeviceCardBinding;
import com.mine.baselibrary.dialog.BaseDialogFragment;

import java.util.HashSet;
import java.util.Set;

public class DeviceMenuDialog extends BaseDialogFragment<MenuDeviceCardBinding> {

    private String deviceMacAddress;
    private String currentDeviceName; // 新增：当前设备名称
    private Set<String> existingDeviceNames; // 新增：已存在的设备名称集合
    private OnDeleteListener onDeleteListener;
    private OnDisconnectListener onDisconnectListener;
    private OnRenameListener onRenameListener;
    private int connectStatus;
    private int connectType;

    public void setConnectStatus(int status) { this.connectStatus = status; }
    public void setConnectType(int type) { this.connectType = type; }
    public void setDeviceMacAddress(String macAddress) { this.deviceMacAddress = macAddress; }

    // 新增方法：设置当前设备名称
    public void setCurrentDeviceName(String name) {
        this.currentDeviceName = name;
    }

    // 新增方法：设置已存在的设备名称
    public void setExistingDeviceNames(Set<String> names) {
        this.existingDeviceNames = names != null ? new HashSet<>(names) : new HashSet<>();
    }

    public interface OnDeleteListener {
        void onDelete();
    }
    public interface OnDisconnectListener {
        void onDisconnect(String deviceMacAddress);
    }
    public interface OnRenameListener {
        void onRename(String newName);
    }

    /* 构造器：直接调用最简父类构造，开启后方模糊，小窗口 */
    public DeviceMenuDialog() {
        super(true, WindowManager.LayoutParams.WRAP_CONTENT,  WindowManager.LayoutParams.WRAP_CONTENT, false, true);
    }

    public void setOnDeleteListener(OnDeleteListener l) {
        this.onDeleteListener = l;
    }
    public void setOnDisconnectListener(OnDisconnectListener l) {
        this.onDisconnectListener = l;
    }
    public void setOnRenameListener(OnRenameListener l) {
        this.onRenameListener = l;
    }

    @Override
    public MenuDeviceCardBinding createViewBinding(@NonNull LayoutInflater inflater,
                                                   @Nullable ViewGroup container) {
        return MenuDeviceCardBinding.inflate(inflater, container, false);
    }

    @Override
    public View getContentLayout() {
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("DeviceMenuDialog", "connectStatus=" + connectStatus
                + " ,connectType=" + connectType
                + " ,mac=" + deviceMacAddress);
        // 重置所有按钮状态
        binding.menuDelete.setVisibility(View.GONE);
        binding.menuDisconnect.setVisibility(View.GONE);
        binding.menuRename.setVisibility(View.GONE);

        if (connectStatus == 3) { // 已连接
            if (connectType == 0 || connectType == 2) {
                // 已连接有线设备：只有设备重命名
                binding.menuRename.setVisibility(View.VISIBLE);
                binding.menuDisconnect.setVisibility(View.GONE);
            } else {
                // 已连接无线设备：断开连接 + 设备重命名
                binding.menuDisconnect.setVisibility(View.VISIBLE);
                binding.menuRename.setVisibility(View.VISIBLE);
            }
        } else {
            // 未连接：删除设备 + 设备重命名
            binding.menuDelete.setVisibility(View.VISIBLE);
            binding.menuRename.setVisibility(View.VISIBLE);
        }

        binding.menuDelete.setOnClickListener(v -> {
            dismiss();

         ConfirmDeleteDeviceDialog confirmDialog =
                    new ConfirmDeleteDeviceDialog(getContext());
            confirmDialog.setOnConfirmDeleteListener(() -> {
                // 用户确认后才执行原来的删除回调
                if (onDeleteListener != null) onDeleteListener.onDelete();
                confirmDialog.dismiss();
            });
            confirmDialog.showWithBlur();
        });

        // 2.断开连接
        binding.menuDisconnect.setOnClickListener(v -> {
            dismiss();
            if (onDisconnectListener != null) {
                onDisconnectListener.onDisconnect(deviceMacAddress);
            }
        });

        binding.menuRename.setOnClickListener(v -> {
            dismiss();

            RenameDeviceDialog renameDialog = new RenameDeviceDialog(getContext());

            // 使用传入的当前设备名称
            if (currentDeviceName != null) {
                renameDialog.setCurrentName(currentDeviceName);
            }

            // 使用传入的已存在设备名称集合
            if (existingDeviceNames != null) {
                renameDialog.setExistingNames(existingDeviceNames);
            }

            renameDialog.setOnRenameConfirmListener(newName -> {
                // 重命名逻辑
                if (onRenameListener != null) {
                    onRenameListener.onRename(newName);
                }
            });
            renameDialog.showWithBlur();
        });
    }

    /* 对外快速 show */
    public static void show(FragmentManager fm,
                            OnDeleteListener deleteAction,
                            OnDisconnectListener disconnectAction,
                            OnRenameListener renameAction,
                            int x, int y,
                            int connectStatus, int connectType,
                            String deviceMacAddress,
                            String currentDeviceName,
                            Set<String> existingDeviceNames) {
        DeviceMenuDialog dialog = new DeviceMenuDialog();
        dialog.setOnDeleteListener(deleteAction);
        dialog.setOnDisconnectListener(disconnectAction);
        dialog.setOnRenameListener(renameAction);
        dialog.setArguments(createPositionBundle(Gravity.START | Gravity.TOP, x, y));
        dialog.setConnectStatus(connectStatus);
        dialog.setConnectType(connectType);
        dialog.setDeviceMacAddress(deviceMacAddress);
        dialog.setCurrentDeviceName(currentDeviceName);
        dialog.setExistingDeviceNames(existingDeviceNames);
        dialog.show(fm, "DeviceMenuDialog");
    }

    /* 保持向后兼容的旧版本 */
    public static void show(FragmentManager fm,
                            OnDeleteListener deleteAction,
                            OnDisconnectListener disconnectAction,
                            OnRenameListener renameAction,
                            int x, int y,
                            int connectStatus, int connectType,
                            String deviceMacAddress) {
        show(fm, deleteAction, disconnectAction, renameAction, x, y,
                connectStatus, connectType, deviceMacAddress, "", new HashSet<>());
    }
}