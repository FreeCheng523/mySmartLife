package com.smartlife.fragrance.ui.function;

import static android.view.View.GONE;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.smartlife.fragrance.utils.FunctionConfigCheck;

import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import com.mine.baselibrary.dialog.BaseDialogFragment;
import com.mine.baselibrary.window.ViewPositionUtil;
import com.smartlife.fragrance.R;
import com.smartlife.fragrance.data.model.FragranceDevice;
import com.smartlife.fragrance.databinding.FragranceBinding;
import com.smartlife.fragrance.ui.status.DeviceStatusViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import kotlin.Pair;
import com.smartlife.fragrance.data.model.ConnectionState;
/**
 * 香氛机控制Dialog
 * 功能：氛围灯联动、颜色选择
 * 联动开关打开时隐藏颜色选择，关闭时显示
 */
public class FragranceDialog extends BaseDialogFragment<FragranceBinding> {
    private static final String TAG = "FragranceDialog";
    private static final String ARG_LIGHT_LINKED = "arg_light_linked";
    private static final String ARG_COLOR = "arg_color";
    private static final String ARG_MAC_ADDRESS = "arg_mac_address"; // 新增：设备MAC地址参数

    private boolean mIsDeviceConnected = true;
    private DeviceStatusViewModel mViewModel;
    private OnFragranceControlListener mListener;
    private String mMacAddress; // 设备MAC地址
    private int mInitialColor;

    // 颜色映射：预设颜色 ↔ View ID
    private final Map<String, Integer> mColorToIdMap = new HashMap<>();
    private final Map<Integer, String> mIdToColorMap = new HashMap<>();
    private final String[] mColorArr = {"#FF00FF", "#FF6A00", "#FFFF00", "#00FF00", "#0000FF",  "#C400FF", "#00FFFF", "#FF0000"};

    @NonNull
    @Override
    public FragranceBinding createViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragranceBinding.inflate(inflater, container, false);
    }

    @Override
    protected View getContentLayout() {
        return binding.getRoot();
    }


    public interface OnFragranceControlListener {
        void onLightLinkChanged(boolean linked);

        void onColorChanged(int colorValue);

        void onDialogDismiss();
        void onDeviceDisconnected();
    }

    public FragranceDialog() {
        super(true, 1600, 960);
    }

    public static FragranceDialog newInstance(String macAddress, boolean linked, int color) {
        FragranceDialog dialog = new FragranceDialog();
        Bundle args = new Bundle();
        args.putString(ARG_MAC_ADDRESS, macAddress);
        args.putBoolean(ARG_LIGHT_LINKED, linked);
        args.putInt(ARG_COLOR, color);
        dialog.setArguments(args);
        return dialog;
    }

    public void setControlListener(OnFragranceControlListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(DeviceStatusViewModel.class);

        // 从 Bundle 中读取参数
        Bundle args = getArguments();
        if (args != null) {
            mMacAddress = args.getString(ARG_MAC_ADDRESS, "");
            mInitialColor = args.getInt(ARG_COLOR, 0);

        }
        observeDeviceConnection();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = FragranceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initColorMaps();
        initViews();
        setupSeekBar();
        setupColorClickListeners();
        if (mInitialColor != 0) {
            updateColorSelection(mInitialColor);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "香氛Dialog onResume");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mIsDeviceConnected = false;
    }

    /**
     * 监听设备连接状态变化
     */
    private void observeDeviceConnection() {
        if (mMacAddress == null || mMacAddress.isEmpty()) {
            Log.w(TAG, "MAC地址为空，无法监听设备连接状态");
            return;
        }

        // 监听设备数据变化，检查连接状态
        mViewModel.deviceInfo(mMacAddress).observe(this, new Observer<FragranceDevice>() {
            @Override
            public void onChanged(FragranceDevice fragranceDevice) {
                if (fragranceDevice == null) {
                    // 设备数据为null，设备可能已断开连接或移除
                    handleDeviceDisconnected();
                    return;
                }
                // 检查设备连接状态
                ConnectionState connectStatus = fragranceDevice.getConnectionState();
                Log.d(TAG, "设备连接状态: " + connectStatus);

                if (connectStatus == ConnectionState.CONNECTED) {
                    if (!mIsDeviceConnected) {
                        mIsDeviceConnected = true;
                        Log.i(TAG, "设备重新连接");
                    }
                } else {
                    // 设备未连接
                    if (mIsDeviceConnected) {
                        handleDeviceDisconnected();
                    }
                }
            }
        });
    }

    /**
     * 处理设备断开连接
     */
    private void handleDeviceDisconnected() {
        if (!mIsDeviceConnected) {
            return; // 已经是断开状态，避免重复处理
        }
        mIsDeviceConnected = false;
        Log.w(TAG, "设备已断开连接，自动关闭Dialog");

        try {
            FragranceTipsDialog tipDialog = (FragranceTipsDialog) getParentFragmentManager()
                    .findFragmentByTag("fragrance_tip_dialog");
            if (tipDialog != null) {
                tipDialog.dismiss();
                Log.d(TAG, "成功关闭 FragranceTipsDialog");
            }
        } catch (Exception e) {
            Log.e(TAG, "关闭子Dialog时出错", e);
        }

        autoCloseOnDisconnect();
    }

    /**
     * 设备断开连接时自动关闭Dialog
     */
    private void autoCloseOnDisconnect() {
        if (!isAdded() || isRemoving()) {
            return;
        }
        // 在UI线程执行
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 通知监听器
                    if (mListener != null) {
                        mListener.onDeviceDisconnected();
                    }
                    dismiss();
                } catch (Exception e) {
                    Log.e(TAG, "关闭Dialog时发生错误", e);
                }
            }
        });
    }


    private void initColorMaps() {
        int[] colorViewIds = {
                R.id.colorButton1, R.id.colorButton2, R.id.colorButton3, R.id.colorButton4,
                R.id.colorButton5, R.id.colorButton6, R.id.colorButton7, R.id.colorButton8
        };

        for (int i = 0; i < mColorArr.length; i++) {
            mColorToIdMap.put(mColorArr[i], colorViewIds[i]);
            mIdToColorMap.put(colorViewIds[i], mColorArr[i]);

        }
    }



    FragranceDevice lastFragranceDevice;

    boolean firstIn = true;
    private void initViews() {

        mViewModel.deviceInfo(mMacAddress).observe(this, new Observer<FragranceDevice>() {
            @Override
            public void onChanged(FragranceDevice fragranceDevice) {

                if(!firstIn){
                    return;
                }

                firstIn = false;

                lastFragranceDevice = fragranceDevice;
                if (fragranceDevice != null && fragranceDevice.getDisplayName() != null) {
                    binding.titleIp.setText(fragranceDevice.getDisplayName());
                }

                fragranceDevice.getLightColor();

                boolean isSyncLightBrightness = fragranceDevice.getSyncLightBrightness();
                updateSwitchUI(isSyncLightBrightness);

                int visibility = isSyncLightBrightness ? GONE : View.VISIBLE;
                binding.lightColorTextView.setVisibility(visibility);
                binding.colorButtonGrid.setVisibility(visibility);
                binding.seekBarHue.setVisibility(visibility);

                String lightColor = fragranceDevice.getLightColor();
                if (lightColor != null && !lightColor.isEmpty()) {
                    try {
                        // String -> int -> HSV
                        int colorInt = Color.parseColor("#"+lightColor);
                        float[] hsv = new float[3];
                        Color.colorToHSV(colorInt, hsv);
                        int hue = (int) hsv[0];

                        Log.d(TAG, "↑↑↑ 接收颜色 - Hex: " + lightColor + ", Hue: " + hue);
                        // 更新SeekBar
                        binding.seekBarHue.setProgress(hue);
                        // 更新颜色选择框
                        updateColorSelection(hue);
                        mInitialColor = hue;
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "无效的颜色格式: " + lightColor, e);
                    }
                }
            }
        });

        // 是否有氛围灯配置
        Context context = requireContext();
        boolean hasAmbientlight = FunctionConfigCheck.getIFunctionConfigCheck(context).hasAmbientlight();
        // 氛围灯联动开关
        if(hasAmbientlight){
            binding.lightSwitch.setVisibility(View.VISIBLE);
            binding.LighttoCarTextView.setVisibility(View.VISIBLE);
            binding.fraTipsButton.setVisibility(View.VISIBLE);
            binding.lightSwitch.setOnClickListener(v -> {

                if (lastFragranceDevice != null) {
                    boolean isOn = lastFragranceDevice.getSyncLightBrightness();
                    boolean newState = !isOn;
                    // 1. 立即更新UI状态（提供即时反馈）
                    updateSwitchUI(newState);
                    // 2. 发送控制命令
                    mViewModel.setSyncLightBrightness(mMacAddress, newState);
                    // 3. 监听ViewModel的状态变化来确保UI与数据同步
                    observeSwitchStateChange(newState);
                    if (mListener != null) mListener.onLightLinkChanged(newState);
                }
            });
            binding.fraTipsButton.setOnClickListener(v -> {
                showTipDialog(v);
            });
        }else {
            binding.lightSwitch.setVisibility(View.GONE);
            binding.LighttoCarTextView.setVisibility(View.GONE);
            binding.fraTipsButton.setVisibility(View.GONE);
        }

        binding.exit.setOnClickListener(v -> {
            if (mListener != null) mListener.onDialogDismiss();
            dismiss();
        });

    }
    private void showTipDialog(View v) {
        FragranceTipsDialog tipDialog = FragranceTipsDialog.newInstance();
        @NotNull Pair<@NotNull Integer, @NotNull Integer> position1 = ViewPositionUtil.getViewCenterOnScreen(binding.fraTipsButton);

        Bundle args = BaseDialogFragment.createPositionBundle(
                Gravity.TOP | Gravity.START,
                position1.getFirst()-135,
                position1.getSecond()+15

        );
        tipDialog.setArguments(args);

        tipDialog.show(getParentFragmentManager(), "fragrance_tip_dialog");
    }

    // 添加UI更新方法
    private void updateSwitchUI(boolean isOn) {
        Log.d(TAG,"updateSwitchUI 收到 isOn=" + isOn);
        binding.lightSwitch.setImageResource(isOn ? R.drawable.switch_on : R.drawable.switch_off);

        int visibility = isOn ? GONE : View.VISIBLE;
        binding.lightColorTextView.setVisibility(visibility);
        binding.colorButtonGrid.setVisibility(visibility);
        binding.seekBarHue.setVisibility(visibility);
        /* ---- 新增：关闭联动时立即用设备当前颜色刷一遍UI ---- */
        if (!isOn && lastFragranceDevice != null) {
            String lightColor = lastFragranceDevice.getLightColor();
            if (lightColor != null && !lightColor.isEmpty()) {
                try {
                    int colorInt = Color.parseColor("#" + lightColor);
                    float[] hsv = new float[3];
                    Color.colorToHSV(colorInt, hsv);
                    int hue = (int) hsv[0];
                    binding.seekBarHue.setProgress(hue);
                    updateColorSelection(hue);
                    mInitialColor = hue;
                    Log.d(TAG, "关闭联动时刷新颜色 hue=" + hue);
                } catch (IllegalArgumentException ignore) {}
            }
        }
    }

    // 监听状态变化确保同步
    private void observeSwitchStateChange(boolean expectedState) {

        mViewModel.deviceInfo(mMacAddress).observe(getViewLifecycleOwner(), device -> {
            if (device != null && device.getSyncLightBrightness() == expectedState) {
                // 状态已同步，更新lastFragranceDevice
                Log.d(TAG,"observeSwitchStateChange收到设备数据 sync="+ device.getSyncLightBrightness());
                lastFragranceDevice = device;
            }
        });
    }

    private void setupSeekBar() {
        binding.seekBarHue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 拖动停止后，获取最终进度值
                int finalHue = seekBar.getProgress();
                // 1. 反向选中最接近的预设色卡
                updateColorSelection(finalHue);
                // 2. 计算颜色值并下发到设备
                if (mMacAddress != null && !mMacAddress.isEmpty()) {
                    float[] hsv = {finalHue, 1.0f, 1.0f};
                    int color = Color.HSVToColor(hsv);
                    mViewModel.setColor(mMacAddress, color);
                    Log.d(TAG, "下发颜色 - Hue: " + finalHue + ", RGB: " + String.format("#%06X", color));
                } // 3. 通知外部监听
                if (mListener != null) {
                    mListener.onColorChanged(finalHue);
                }
            }
        });
    }


    private void setupColorClickListeners() {
        for (int i = 0; i < binding.colorButtonGrid.getChildCount(); i++) {
            final View colorButton = binding.colorButtonGrid.getChildAt(i);
            colorButton.setOnClickListener(v -> {
                String colorHex = mIdToColorMap.get(v.getId());
                if (colorHex != null) {
                    int hue = Integer.parseInt((String) v.getTag());
                    Log.d(TAG, "↓↓↓ 下发颜色 - Hex: " + colorHex + ", Hue: " + hue);
                    binding.seekBarHue.setProgress(hue);
                    mInitialColor = hue;

                    // 更新UI
                    updateColorSelection(hue);

                    // 设置颜色到设备
                    if (mMacAddress != null && !mMacAddress.isEmpty()) {
                        mViewModel.setColor(mMacAddress, colorHex);
                    }
                    if (mListener != null) mListener.onColorChanged(hue);
                }
            });
        }
    }

    private void observeDeviceData() {
        if (mMacAddress != null && !mMacAddress.isEmpty()) {
            mViewModel.deviceInfo(mMacAddress).observe(getViewLifecycleOwner(), device -> {
                if (device != null) {
                    updateUIWithDeviceData(device);
                }
            });
        }
    }

    private void updateUIWithDeviceData(com.smartlife.fragrance.data.model.FragranceDevice device) {

    }
    private void clearAllSelections() {
        binding.colorButton1.setSelected(false);
        binding.colorButton2.setSelected(false);
        binding.colorButton3.setSelected(false);
        binding.colorButton4.setSelected(false);
        binding.colorButton5.setSelected(false);
        binding.colorButton6.setSelected(false);
        binding.colorButton7.setSelected(false);
        binding.colorButton8.setSelected(false);
    }
    private void updateColorSelection(int hue) {
        // 清空所有选中状态
        clearAllSelections();

        // 找到最接近的预设颜色按钮
        for (int i = 0; i < binding.colorButtonGrid.getChildCount(); i++) {
            View colorButton = binding.colorButtonGrid.getChildAt(i);
            Object tag = colorButton.getTag();
            if (tag != null) {
                try {
                    int buttonHue = Integer.parseInt(tag.toString());
                    // 允许一定的误差范围
                    if (Math.abs(buttonHue - hue) <= 10) {
                        colorButton.setSelected(true);
                        break;
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}