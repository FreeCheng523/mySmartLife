package com.deepal.ivi.hmi.smartlife.adapter;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

import com.adayo.service.utils.MKDisplayStatus;
import com.deepal.ivi.hmi.smartlife.BluetoothCommon;
import com.deepal.ivi.hmi.smartlife.MainActivity;
import com.deepal.ivi.hmi.smartlife.R;
import com.deepal.ivi.hmi.smartlife.base.BaseApplication;
import com.deepal.ivi.hmi.smartlife.bean.AgileSmartDevice;
import com.deepal.ivi.hmi.smartlife.bean.SmartDevice;
import com.deepal.ivi.hmi.smartlife.databinding.ItemSmartDeviceBinding;
import com.deepal.ivi.hmi.smartlife.dialog.DeviceMenuDialog;
import com.deepal.ivi.hmi.smartlife.utils.BatteryView;
import com.deepal.ivi.hmi.smartlife.utils.BlurBackgroundHelper;
import com.deepal.ivi.hmi.smartlife.utils.LocalStoreManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.deepal.ivi.hmi.smartlife.utils.ThrottleClickListener;
import com.mine.baselibrary.window.ToastUtilOverApplication;
import com.mine.baselibrary.window.ViewPositionUtil;

import android.widget.PopupWindow;

import org.jetbrains.annotations.NotNull;

import kotlin.Pair;

public class SmartDeviceAdapter extends RecyclerView.Adapter<SmartDeviceAdapter.DeviceCardHolder> {


    private Context mContext;
    private List<SmartDevice> smartDeviceList;
    private int clientConnected = -1; // 添加客户端连接状态变量

    private PopupWindow mPopupMenu;

    public static void showBlurDialog(Dialog dialog) {
        BlurBackgroundHelper.applyBlurBehind(dialog);
        dialog.show();
    }
    public void closeMenu() {
        if (mPopupMenu != null && mPopupMenu.isShowing()) {
            mPopupMenu.dismiss();
        }
    }


    private BatteryView batteryView;

    private static String TAG = "SmartDeviceAdapter";
    public SmartDeviceAdapter(Context mContext, List<SmartDevice> smartDeviceList) {
        this.mContext = mContext;
        this.smartDeviceList = smartDeviceList;
    }

   /**全局遍历**/
   private RecyclerView recyclerView;

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    public void closeAllMenus() {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            for (int i = 0; i < getItemCount(); i++) {
                RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(i);
                if (holder instanceof DeviceCardHolder) {
                    ((DeviceCardHolder) holder).closeMenu();
                }
            }
        }, 150);   // 150 ms 系统把第二次点击事件分发完
    }

    /**
     * 用于表示每个选项卡的视图容器
     */
    class DeviceCardHolder extends RecyclerView.ViewHolder {
        public ViewDataBinding viewBinding;

        public DeviceCardHolder(ViewDataBinding viewBinding) {
            super(viewBinding.getRoot());
            this.viewBinding = viewBinding;
        }
        public void closeMenu() {
            ItemSmartDeviceBinding binding = (ItemSmartDeviceBinding) viewBinding;
//            binding.menuContainer.setVisibility(View.GONE);
            binding.mask1.setVisibility(View.GONE);
        }

    }

    /**
     * 创建 ViewHolder 并初始化视图。
     * 使用数据绑定库加载布局文件 item_smart_device.xml，这个布局文件是RecyclerView中单个元素的布局文件。
     * 将绑定对象传递给 ViewHolder
     */

    @NonNull
    @Override
    public DeviceCardHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSmartDeviceBinding viewBinding = DataBindingUtil.inflate(LayoutInflater.from(mContext), R.layout.item_smart_device, parent, false);
        return new DeviceCardHolder(viewBinding);
    }
    private int getIconResByType(int devicetype, String bleName) {
        switch (devicetype) {
            case 1:
                return R.drawable.icon_small_meter;
            case 2:
                return R.drawable.icon_physical_button;
            case 3:
             Log.e("bleName",String.valueOf(bleName));
                if (bleName != null) {
                    if (bleName.contains("DeepalTag_E")) {
                        return R.drawable.icon_tuotuo;
                    } else if (bleName.contains("DeepalTag_L")) {
                        return R.drawable.icon_tuotuo2;
                    }
                }
                return R.drawable.icon_tuotuo;
            case 4:
                return R.drawable.icon_fragrance;
            default:
                return R.drawable.icon_error;
        }
    }
    private int getConnectMethod(int type, int connectStatus) {
        switch (type) {
            case 0:   // usb
                switch (connectStatus) {
                    case 0:  return R.drawable.icon_usb;
                    case 1:  return R.drawable.icon_usb;
                    case 3:  return R.drawable.icon_usb1;
                }
                break;
            case 2:   // pin
                switch (connectStatus) {
                    case 0:  return R.drawable.pin0;
                    case 1:  return R.drawable.pin0;
                    case 3:  return R.drawable.pin1;
                }
                break;
            case 1:   // 蓝牙图标
                switch (connectStatus) {
                    case 0:  return R.drawable.icon_ble;
                    case 1:  return R.drawable.icon_ble;
                    case 3:  return R.drawable.icon_ble1;
                }
                break;
        }
        return R.drawable.icon_error;
    }

    /**
     * 将数据绑定到 ViewHolder 中的视图。
     */

    @Override
    public void onBindViewHolder(@NonNull DeviceCardHolder holder, int position) {
        Log.d("SmartLife", "onBindViewHolder: " + smartDeviceList.get(position).getDeviceName());
        ItemSmartDeviceBinding binding = (ItemSmartDeviceBinding) holder.viewBinding;

//        电池电量赋值
        SmartDevice smartDeviceTemp = smartDeviceList.get(position);
        if(smartDeviceTemp instanceof AgileSmartDevice) {
            AgileSmartDevice agileSmartDevice = (AgileSmartDevice) smartDeviceTemp;
            binding.batteryView.setBatteryLevel(agileSmartDevice.getBatteryLevel());
        }

        binding.deviceName.setText(smartDeviceList.get(position).getDeviceName());
        String name = smartDeviceList.get(position).getDeviceName();
        String bleName = smartDeviceList.get(position).getBleName();
        int devicetype = smartDeviceList.get(position).getDeviceType();
        binding.deviceImage.setImageResource(getIconResByType(devicetype,bleName));

        int connectStatus = smartDeviceList.get(position).getConnectStatus();
        int type = smartDeviceList.get(position).getConnectType();
        int icon = getConnectMethod(type, connectStatus);


        //香氛第一期没有电池显示
        if( devicetype  == 3){
            binding.batteryView.setVisibility(View.VISIBLE);
        }else{
            binding.batteryView.setVisibility(View.GONE);
        }
        //新增香氛卡片开启/关闭
        if (devicetype == 4) {
            Boolean isPowerOn = smartDeviceList.get(position).isPowerOn();
            if (isPowerOn) {
                binding.powerOff.setImageResource(R.drawable.power_on);
                binding.powerOff.setVisibility(View.VISIBLE);
            } else {
                binding.powerOff.setImageResource(R.drawable.power_off);
                binding.powerOff.setVisibility(View.VISIBLE);
            }
        } else {
            binding.powerOff.setVisibility(View.GONE);
        }

        binding.deviceIsConnectLin.setVisibility(View.VISIBLE);
        binding.deviceIsConnectImage.setVisibility(View.VISIBLE);
        binding.deviceIsConnectImage.setImageResource(icon);

        if (connectStatus == 1) {
            binding.deviceIsConnectText.setText("未连接");
            binding.deviceIsConnectText.setTextColor(ContextCompat.getColor(mContext,  R.color.d4_white));
            binding.batteryView.setVisibility(View.GONE);
            binding.powerOff.setVisibility(View.GONE);
        } else if (connectStatus == 0) {
            binding.deviceIsConnectText.setText("待连接");
            binding.batteryView.setVisibility(View.GONE);
            binding.powerOff.setVisibility(View.GONE);
        } else if (connectStatus == 3) {
            binding.deviceIsConnectText.setText("已连接");
            binding.deviceIsConnectText.setTextColor(ContextCompat.getColor(mContext, R.color.abc_decor_view_status_guard_light));
        }


        binding.device.setOnClickListener(new ThrottleClickListener() {
            @Override
            protected void onThrottleClick(View v) {
                closeAllMenus();
                int pos = getSafePosition(holder);
                if (pos == -1) return;
                if (onDeviceItemClickListener != null) {
                    onDeviceItemClickListener.onDeviceItemClick(smartDeviceList.get(pos), 3);
                }
                if (connectStatus == 1) {
                    tryConnectDialog(pos);
                }
            }
        });

        binding.deviceMenu.setOnClickListener(v -> {
            int pos = getSafePosition(holder);
            if (pos == -1) return;
            SmartDevice device = smartDeviceList.get(pos);
            String currentDeviceName = device.getDeviceName();
            Set<String> existingNames = getExistingDeviceNames(device);//所有设备名称
            @NotNull Pair<@NotNull Integer, @NotNull Integer> position1 = ViewPositionUtil.getViewCenterOnScreen(binding.deviceMenu);
            DeviceMenuDialog.show(
                    ((MainActivity) mContext).getSupportFragmentManager(),
                    () -> {   // 删除回调
                        SmartDevice removed = smartDeviceList.get(pos);
                        // ✅ 删除逻辑已移到MainActivity.onDeviceRemoved()中处理
                        // 妥妥帖和香氛设备从数据库删除，其他设备从SP删除
                        if (onDeviceItemClickListener != null) {
                            onDeviceItemClickListener.onDeviceRemoved(removed,pos);
                        }
                    },
                    (mac)-> {// 断开连接回调
                        if (onDeviceItemClickListener != null) {
                            onDeviceItemClickListener.onDeviceDisconnected(device);
                        }
                        notifyItemChanged(pos);
                    },
                    (newName) -> { // 重命名回调
                        // 更新设备名称
                        device.setDeviceName(newName);

                        if (onDeviceItemClickListener != null) {
                            onDeviceItemClickListener.onDeviceRename(device, newName);
                        }
                        notifyItemChanged(pos);
                        LocalStoreManager.getInstance()
                                .storeData("key.store.device", smartDeviceList);
                        Log.d("Rename", "存储后的设备列表: " + smartDeviceList);

                    },
                            position1.getFirst()-290,
                            position1.getSecond()+45,
                            device.getConnectStatus(),
                            device.getConnectType(),
                            device.getDeviceId(),
                            currentDeviceName,
                            existingNames
            );

        });

        binding.mask1.setOnClickListener(v -> {
            binding.mask1.setVisibility(View.GONE);
        });

        binding.powerOff.setOnClickListener(v -> {
            int pos = getSafePosition(holder);
            if (pos == -1) return;
            SmartDevice device = smartDeviceList.get(pos);

            boolean newPowerState = !device.isPowerOn();
            device.setIsPowerOn(newPowerState);

            int newImageRes = newPowerState ? R.drawable.power_on : R.drawable.power_off;
            binding.powerOff.setImageResource(newImageRes);

            if (onDeviceItemClickListener != null) {
                onDeviceItemClickListener.onPowerStateChanged(device, newPowerState);
            }
        });

    }

    /**
     * 返回选项卡的总数
     */
    @Override
    public int getItemCount() {
        return smartDeviceList.size();
    }

    public void updateItem(int position, SmartDevice newSmartDevice) {
        Log.d("UI_UPDATE", "updateItem called for position: " + position);
        if (smartDeviceList == null) return;
        if (position < 0 || position >= smartDeviceList.size()) return;
        if (newSmartDevice.getConnectStatus() == 3) closeAllMenus();
        smartDeviceList.set(position, newSmartDevice);
        notifyItemChanged(position);
    }
    public List<SmartDevice> getSmartDeviceItems() {
        return smartDeviceList;
    }

    public interface OnDeviceItemClickListener {
        void onDeviceItemClick(SmartDevice device,int action);
        void onDeviceRemoved(SmartDevice device,int position);
        void onPowerStateChanged(SmartDevice device, boolean newPowerState);
        void onDeviceDisconnected(SmartDevice device);//新增断开连接接口
        void onDeviceRename(SmartDevice device,String reName);

        void onCancelConnectWhenConnecting(SmartDevice device);
    }

    private Set<String> getExistingDeviceNames(SmartDevice currentDevice) {
        Set<String> existingNames = new HashSet<>();
        for (SmartDevice device : smartDeviceList) {
            // 排除当前设备
            if (device == currentDevice) {
                continue;
            }

            String deviceName = device.getDeviceName();
            if (deviceName != null && !deviceName.trim().isEmpty()) {
                existingNames.add(deviceName.trim());
            }
        }
        return existingNames;
    }

    private OnDeviceItemClickListener onDeviceItemClickListener;

    public void setOnDeviceItemClickListener(OnDeviceItemClickListener listener) {
        this.onDeviceItemClickListener = listener;
    }
    public class ConfirmDeleteDeviceWindowView {

        private final Dialog dialog;
        private Runnable onConfirmDeleteListener;

        public ConfirmDeleteDeviceWindowView(Context context) {
            dialog = new Dialog(context);
            dialog.setContentView(R.layout.toast_confirm_delete_device);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(true);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // 绑定按钮事件
            dialog.findViewById(R.id.confirm_delete_yes).setOnClickListener(v -> {
                if (onConfirmDeleteListener != null) {
                    onConfirmDeleteListener.run();
                }
            });

            dialog.findViewById(R.id.confirm_delete_no).setOnClickListener(v -> dismiss());
        }

        public void setOnConfirmDeleteListener(Runnable listener) {
            this.onConfirmDeleteListener = listener;
        }

        public void show() {
            if (!dialog.isShowing()) {
                showBlurDialog(dialog);
            }
        }

        public void dismiss() {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }

    /**设备未连接，可以点击连接**/
    private void tryConnectDialog(int position) {
        Log.d("SmartLife2", "tryConnectDialog called for: " + smartDeviceList.get(position).getDeviceName());
        Dialog dlg = new Dialog(mContext);
        dlg.setContentView(R.layout.toast_device_unconnected);
        dlg.setCancelable(true);
        dlg.setCanceledOnTouchOutside(false);
        dlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View back = dlg.findViewById(R.id.toast_back_image);
        back.setOnClickListener(v -> {
            dlg.dismiss();
        });
        dlg.findViewById(R.id.toast_device_connected_text_bottom).setOnClickListener(v -> {

            SmartDevice smartDevice = smartDeviceList.get(position);
            int connectType = smartDevice.getConnectType();
            if (connectType ==1 && !BluetoothCommon.isBluetoothEnabled()) {
                new ToastUtilOverApplication().showToast(BaseApplication.getInstance(),"请打开蓝牙");
                return;
            }

            dlg.dismiss();
            // 实时找到这个设备的新位置
            int newPos = findPositionByDeviceName(smartDeviceList.get(position).getDeviceName());
            if (newPos != -1) {
                showConnectingDialog(newPos);
            }
        });
        showBlurDialog(dlg);
        Log.d("SmartLife2", "Dialog shown: " + dlg.getWindow() + " | isShowing=" + dlg.isShowing());
    }
     private CountDownTimer connectingTimer;
    /**连接设备中**/
    public void showConnectingDialog(int position) {

        if(position<0){
            Log.e("SmartLife2", "showConnectingDialog position error ");
            return;
        }

        if (onDeviceItemClickListener != null) {
                onDeviceItemClickListener.onDeviceItemClick(smartDeviceList.get(position), 1);
        }

        Dialog dlg = new Dialog(mContext);
        dlg.setContentView(R.layout.dialog_add_device1);
        dlg.setCanceledOnTouchOutside(false);
        dlg.setCancelable(true);
        dlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        ProgressBar progressBar = dlg.findViewById(R.id.progress_bar);
        TextView percentText = dlg.findViewById(R.id.toast_device_connected_text);

        if (connectingTimer != null) {
            connectingTimer.cancel();
            connectingTimer = null;
        }
        connectingTimer = new CountDownTimer(25_000, 800) { // 150 ms 更新一次
            @Override
            public void onTick(long millisUntilFinished) {
                SmartDevice smartDevice = smartDeviceList.get(position);
                Log.i(TAG, "设备连接:" + smartDevice.getDeviceName() +" 状态:"+ (smartDevice.getConnectStatus()==1?"连接中":"已连接"));
                int progress = (int) ((25_000 - millisUntilFinished) * 100 / 25_000);
                progressBar.setProgress(progress);
                TextView tvPercent = dlg.findViewById(R.id.tv_percent);
                tvPercent.setText(progress + "%");
                if (smartDevice.getDeviceType() == 2 && MKDisplayStatus.INSTANCE.displayMKStatus(mContext) == 13) {
                    dlg.dismiss();
                    showSuccessDialog(position);
                    cancel();
                    connectingTimer = null;
                    updateItem(position, new SmartDevice("中控物理按键", 2, 2, 3));
                } else if (smartDevice.getDeviceType() == 1 && clientConnected == 1) {
                    smartDevice.setConnectStatus(3);
                    dlg.dismiss();
                    updateItem(position, smartDevice);
                    showSuccessDialog(position);
                    cancel();
                    connectingTimer = null;
                } else if (smartDevice.getDeviceType() == 3 && 3 == smartDevice.getConnectStatus()) {
                    smartDevice.setConnectStatus(3);
                    Log.d("SmartLife0801", smartDevice.toString() + "connetctStatus:" + smartDevice.getConnectStatus());
                    dlg.dismiss();
                    updateItem(position, smartDevice);
                    showSuccessDialog(position);
                    cancel();
                    connectingTimer = null;
                }else if(smartDevice.getDeviceType() == 4 && 3 == smartDevice.getConnectStatus()){//香氛
                    smartDevice.setConnectStatus(3);
                    Log.d("SmartLife0801", smartDevice.toString() + "connetctStatus:" + smartDevice.getConnectStatus());
                    dlg.dismiss();
                    updateItem(position, smartDevice);
                    showSuccessDialog(position);
                    cancel();
                    connectingTimer = null;
                }
                //Toast.makeText(mContext, "连接状态"+clientConnected +",日志："+msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFinish() {
                dlg.dismiss();
                if (connectingTimer != null) {
                    showConnectFailDialog(position);
                }
                connectingTimer = null;
            }
        };
        View.OnClickListener cancelListener = v -> {
            if (connectingTimer != null) {
                connectingTimer.cancel();
                connectingTimer = null;
            }
            if (onDeviceItemClickListener != null) {
                try {
                    onDeviceItemClickListener.onCancelConnectWhenConnecting(smartDeviceList.get(position));
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG,"onDeviceItemClickListener",e);
                }
            }
            dlg.dismiss();
        };
        dlg.findViewById(R.id.toast_back_image).setOnClickListener(cancelListener);
        dlg.findViewById(R.id.toast_device_disconnect_text_bottom).setOnClickListener(cancelListener);

        showBlurDialog(dlg);
        connectingTimer.start();}

    // 添加设置客户端连接状态的方法
    public void setClientConnected(int connected,String msg) {
        Log.d("小仪表发过来的值：", "ClientConnected: " + connected+" ,msg:"+msg);
        this.clientConnected = connected;
    }
    /**连接成功弹窗**/
    private void showSuccessDialog(int position) {
        Dialog dlg = new Dialog(mContext);
        dlg.setContentView(R.layout.toast_add_device_success);
        dlg.setCancelable(true);
        dlg.setCanceledOnTouchOutside(false);
        dlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvCount = dlg.findViewById(R.id.toast_add_success_text1);

        dlg.findViewById(R.id.toast_back_image).setOnClickListener(v -> dlg.dismiss());
        dlg.findViewById(R.id.toast_look_up_device).setOnClickListener(v -> {
            dlg.dismiss();
            SmartDevice smartDevice = smartDeviceList.get(position);
            if (smartDevice.getDeviceName().equals("小仪表")||smartDevice.getDeviceType()==3||smartDevice.getDeviceType()==4){
                onDeviceItemClickListener.onDeviceItemClick(smartDevice,3);
            }
        });
        showBlurDialog(dlg);

        // 倒计时 5→0 秒
        new CountDownTimer(5_000 + 100, 1_000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int sec = (int) (millisUntilFinished / 1_000);
                tvCount.setText(sec + "s");
            }
            @Override
            public void onFinish() {
                dlg.dismiss();
            }
        }.start();
        // 更新卡片
    }

    /***连接失败****/
    public Dialog showConnectFailDialog(int position) {
        if(position<0){
            return null;
        }
        Dialog dlg = new Dialog(mContext);
        dlg.setContentView(R.layout.dialog_connect_fail);
        dlg.setCancelable(true);
        dlg.setCanceledOnTouchOutside(false);
        dlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dlg.findViewById(R.id.toast_back_image)
                .setOnClickListener(v -> dlg.dismiss());

        dlg.findViewById(R.id.toast_device_connected_text_bottom).setOnClickListener(v -> {
            dlg.dismiss();
            int newPos = findPositionByDeviceName(smartDeviceList.get(position).getDeviceName());

            showConnectingDialog(newPos);

        });
        showBlurDialog(dlg);
        return dlg;
    }

    /**
     * 根据设备ID找到在 smartDeviceList 中的位置，找不到返回 -1
     */
    public int findPosition(String deviceId) {
        if (deviceId == null || smartDeviceList == null) {
            return -1;
        }

        for (int i = 0; i < smartDeviceList.size(); i++) {
            SmartDevice device = smartDeviceList.get(i);
            if (device != null && deviceId.equals(device.getDeviceId())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 实时获取position，若已失效返回 -1
     */
    private int getSafePosition(DeviceCardHolder holder) {
        int pos = holder.getAdapterPosition();
        if (pos == RecyclerView.NO_POSITION || pos >= smartDeviceList.size()) {
            return -1;
        }
        return pos;
    }

    /**
     * 根据设备名找到在 smartDeviceList 中的最新位置，找不到返回 -1
     */
    private int findPositionByDeviceName(String name) {
        for (int i = 0; i < smartDeviceList.size(); i++) {
            if (name.equals(smartDeviceList.get(i).getDeviceName())) {
                return i;
            }
        }
        return -1;
    }
}