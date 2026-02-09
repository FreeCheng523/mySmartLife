package com.zkjd.lingdong.ui.function_kotlin;

import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.mine.baselibrary.window.ToastUtilOverApplication;
import com.zkjd.lingdong.R;
import com.mine.baselibrary.dialog.BaseDialogFragment;
import com.zkjd.lingdong.databinding.LightSetBinding;
import com.zkjd.lingdong.libs.GradientSeekBar;
import com.zkjd.lingdong.model.LedColor;
import com.zkjd.lingdong.ui.TuoTuoTieSettingTipsFragmentDialog;
import com.zkjd.lingdong.ui.led.PreventAndLedColorAndAudioViewModel;
import java.util.HashMap;
import java.util.Map;
import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function4;

@AndroidEntryPoint
public class LightSetDialog extends BaseDialogFragment<LightSetBinding> {
    
    private LinearLayout colorPreview;
    private GradientSeekBar seekBarHue;
    private TextView tvHueValue, tvHexValue, tvHue;

    private ImageView switchIcon;
    private PopupWindow popupWindow;
    private PopupWindow popupWindow1;

    private TextView tab1, tab2, tab3;
    private MediaPlayer mediaPlayer;
    private int[] soundResources = {
            R.raw.click_sound, // 需要在res/raw目录下放置三个音效文件
            R.raw.dong,
            R.raw.dack
    };

    private int swithNotStatus ;
    private int swithVoiceStatus ;

    // 声明为成员变量，避免每次调用都重新创建
    private final String[] colorArr = {"#FF0000","#FF6A00","#FFFF00","#00FF00","#0000FF","#FF00FF","#00FFFF","#C400FF"};
    private View[] colorViewIds;
    private final Map<String, Integer> colorToIdMap = new HashMap<>();

    private final Map<Integer, String> idToColorMap = new HashMap<>();

    private Context mContext;
    private static final String ARG_DEVICE_MAC = "device_mac";
    private String deviceMac; // 添加成员变量保存 deviceMac

    private PreventAndLedColorAndAudioViewModel mPreventAndLedColorAndAudioViewModel;

    @ApplicationContext
    Application applicationContext;

    public LightSetDialog(Context context, OnDialogClickListener listener){
        super(true,1600, 960);
        this.mContext = context;
        onDialogListener = listener;
    }

    // 必需的无参构造函数
    public LightSetDialog() {
        // Fragment 需要公共无参构造函数
    }

    // 静态工厂方法 - 修改为接受 Context 和 listener
    public static LightSetDialog newInstance(Context context, String deviceMac, OnDialogClickListener listener) {
        LightSetDialog fragment = new LightSetDialog(context, listener);
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_MAC, deviceMac);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    private void setupPopupWindow() {
        // 使用正确的布局文件名 popup_tips
        View popupView = LayoutInflater.from(getActivity()).inflate(R.layout.popup_tips, null);

        // 将px转换为dp以确保更好的适配性
//        float density = getResources().getDisplayMetrics().density;
        int widthInPx = 1300;
        int heightInPx = 140;

        // 创建PopupWindow，设置固定尺寸
        popupWindow = new PopupWindow(
                popupView,
                widthInPx,  // 直接使用px值
                heightInPx, // 直接使用px值
                false        // 设置为true确保可以获取焦点
        );


        // 设置背景，确保点击外部可关闭
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);

    }

    private void setupPopupWindow1() {
        // 使用正确的布局文件名 popup_tips
        View popupView = LayoutInflater.from(getActivity()).inflate(R.layout.popup_tips1, null);

        // 将px转换为dp以确保更好的适配性
//        float density = getResources().getDisplayMetrics().density;
        int widthInPx = 810;
        int heightInPx = 135;

        // 创建PopupWindow1，设置固定尺寸
        popupWindow1 = new PopupWindow(
                popupView,
                widthInPx,  // 直接使用px值
                heightInPx, // 直接使用px值
                false        // 设置为true确保可以获取焦点
        );


        // 设置背景，确保点击外部可关闭
        popupWindow1.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow1.setOutsideTouchable(true);

    }

    private void showPopupWindow() {
        if (popupWindow == null) return;

        // 获取TextView的位置作为锚点
        TextView textView = binding.textTitle1;

        // 显示在TextView下方
        popupWindow.showAsDropDown(textView, -12, 12);

        // 或者显示在图标下方（备用方案）
        // popupWindow1.showAsDropDown(switchIcon1, 0, 0);
    }

    private void showPopupWindow1() {
        if (popupWindow1 == null) return;

        // 获取TextView的位置作为锚点
        TextView textView = binding.textTitle2;

        // 显示在TextView下方
        popupWindow1.showAsDropDown(textView, -9, 12);

        // 或者显示在图标下方（备用方案）
        // popupWindow1.showAsDropDown(switchIcon1, 0, 0);
    }



    public void init(View view){

        setupPopupWindow();
        setupPopupWindow1();

        binding.swichIcon1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupWindow();
            }
        });

        binding.swichIcon2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupWindow1();
            }
        });

        binding.titleIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TuoTuoTieSettingTipsFragmentDialog dialog = TuoTuoTieSettingTipsFragmentDialog.newInstance("", "");
                dialog.show(getParentFragmentManager(), "permission_denied_tips", new TuoTuoTieSettingTipsFragmentDialog.OnTipsDialogClickListener() {
                    @Override
                    public void onConfirm() {
                        // 用户点击确认，可以在这里添加跳转到设置页面的逻辑


                    }

                    @Override
                    public void onDismiss() {

                    }
                });
            }
        });

        //按钮和颜色的映射
        initColorId();

        getInitData();

        //开关初始化
        switchInit();

        // 预设颜色按钮点击事件
        setupColorClickListeners(view);

        // 初始化视图
        initViews();

        // 设置SeekBar监听器
        setupSeekBar();

        // 颜色初始化
        //colorToId("#FFFF00");
        // 更新颜色显示
        //updateColorDisplay();

        // 初始化选项卡
        tab1 = binding.tab1;
        tab2 = binding.tab2;
        tab3 = binding.tab3;

        // 设置默认选中第一个选项卡
//        selectTab(tab1, 0);



        // 设置选项卡点击监听
        tab1.setOnClickListener(v -> {
            selectTab(tab1, 0,true);
            mPreventAndLedColorAndAudioViewModel.toSaveAudioName(deviceMac,"1");
        });
        tab2.setOnClickListener(v ->{
            selectTab(tab2, 1,true);
            mPreventAndLedColorAndAudioViewModel.toSaveAudioName(deviceMac,"2");
        });
        tab3.setOnClickListener(v -> {
            selectTab(tab3, 2,true);
            mPreventAndLedColorAndAudioViewModel.toSaveAudioName(deviceMac,"3");
        });
    }

    private void getInitData() {
        swithNotStatus = 0;
        Log.i(TAG, "swithNotStatus: " + swithNotStatus);
        swithVoiceStatus = 0;
        Log.i(TAG, "swithVoiceStatus: " + swithVoiceStatus);
    }

    //开关初始化
    private void switchInit(){

        //      防误触开关
        binding.autoBrightnessSwitch.setOnClickListener(view -> {
            Log.i(TAG, "自动亮度被点击,点击前的状态："+ swithNotStatus);
            mPreventAndLedColorAndAudioViewModel.getPreventAccidental(deviceMac, new Function1<Boolean, Unit>() {
                @Override
                public Unit invoke(Boolean isReturnControl) {
                    if(isReturnControl){
                        if (swithNotStatus == 0){
                            swithNotStatus = 1;
                            binding.autoBrightnessSwitch.setImageResource(R.drawable.switch_on);
                            mPreventAndLedColorAndAudioViewModel.setPreventAccidental(deviceMac,false);
                        }else {
                            swithNotStatus = 0;
                            binding.autoBrightnessSwitch.setImageResource(R.drawable.switch_off);
                            mPreventAndLedColorAndAudioViewModel.setPreventAccidental(deviceMac,true);
                        }
                    }else{
                        new ToastUtilOverApplication().showToast(mContext,"请长按妥妥贴5-10s\n进入返控模式后设置防误触");
                    }
                    return null;
                }
            });

        });

        //      控制音效设置开关
        binding.autoBrightnessSwitch1.setOnClickListener(view -> {
            Log.i(TAG, "自动亮度被点击,点击前的状态："+ swithVoiceStatus);
            if (swithVoiceStatus == 0){
                swithVoiceStatus = 1;
                binding.autoBrightnessSwitch1.setImageResource(R.drawable.switch_on);
                binding.tabContainer.setVisibility(View.VISIBLE);
                mPreventAndLedColorAndAudioViewModel.toggleMusic(deviceMac,true);
            }else {
                swithVoiceStatus = 0;
                binding.autoBrightnessSwitch1.setImageResource(R.drawable.switch_off);
                binding.tabContainer.setVisibility(View.GONE);
                mPreventAndLedColorAndAudioViewModel.toggleMusic(deviceMac,false);
            }
        });

//        SwitchButtonView switchButton = binding.switchButton;
//        SwitchButtonView switchVoice = binding.switchVoice;
//
//        // 设置防误触改变监听器
//        switchButton.setOnCheckedChangeListener(new SwitchButtonView.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(SwitchButtonView buttonView, boolean isChecked) {
//                Toast.makeText(mContext, "防误触开关状态: " + (isChecked ? "开启" : "关闭"), Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        // 设置音效开关监听器
//        switchVoice.setOnCheckedChangeListener(new SwitchButtonView.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(SwitchButtonView buttonView, boolean isChecked) {
//                Toast.makeText(mContext, "音效开关状态: " + (isChecked ? "开启" : "关闭"), Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        // 以编程方式设置状态
//        switchButton.setChecked(true);
    }

    // 预先建立颜色到View ID的映射
    private void initColorId(){
        colorViewIds = new View[]{binding.colorButton1, binding.colorButton2, binding.colorButton3,
                binding.colorButton4, binding.colorButton5, binding.colorButton6, binding.colorButton7, binding.colorButton8};

        for (int i = 0; i < colorArr.length; i++) {
            colorToIdMap.put(colorArr[i], colorViewIds[i].getId());
            idToColorMap.put(colorViewIds[i].getId(),colorArr[i]);
        }
    }

    private void selectTab(TextView selectedTab, int tabIndex,boolean playSound) {
        // 重置所有选项卡状态
        tab1.setSelected(false);
        tab2.setSelected(false);
        tab3.setSelected(false);

        // 设置选中状态
        selectedTab.setSelected(true);

        if(playSound) {
            // 播放对应音效
           // playSound(soundResources[tabIndex]);
        }
    }

    private void playSound(int soundResource) {
        Log.i(TAG,"playSound");
        // 释放之前的MediaPlayer资源
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        // 创建并播放音效
        mediaPlayer = MediaPlayer.create(mContext, soundResource);
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    @Override
    public void onDestroy() {
        //释放可能未关闭的弹框
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
        if (popupWindow1 != null && popupWindow1.isShowing()) {
            popupWindow1.dismiss();
        }
        super.onDestroy();
        // 释放MediaPlayer资源
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected View getContentLayout() {
        return binding.dialogContainer;
    }

    @Override
    protected LightSetBinding createViewBinding(LayoutInflater inflater, ViewGroup container) {
        return LightSetBinding.inflate(inflater,container,false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 使用Hilt标准方式创建ViewModel，因为PreventAndLedColorAndAudioViewModel已经用@HiltViewModel注解
        mPreventAndLedColorAndAudioViewModel = new ViewModelProvider(this)
                .get(PreventAndLedColorAndAudioViewModel.class);
        // 获取传递的参数并保存到成员变量
        if (getArguments() != null) {
            deviceMac = getArguments().getString(ARG_DEVICE_MAC);
            // 在这里可以使用 deviceMac
            setDeviceMac(deviceMac);
            mPreventAndLedColorAndAudioViewModel.observedIsPreventAccidental(deviceMac, new Function1<Boolean, Unit>() {
                @Override
                public Unit invoke(Boolean isPreventAccidental) {
                    return null;
                }
            });
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view);
        setupViewModelObservers();
        mPreventAndLedColorAndAudioViewModel.isMusicOn(deviceMac, new Function1<Boolean, Unit>() {
            @Override
            public Unit invoke(Boolean isMusicOn) {
                if(isMusicOn){
                    binding.tabContainer.setVisibility(View.VISIBLE);
                }else{
                    binding.tabContainer.setVisibility(View.GONE);
                }
                return null;
            }
        });
    }

    private void initViews() {
        colorPreview = binding.colorPreview;
        seekBarHue = binding.seekBarHue;
        tvHueValue = binding.tvHueValue;
        tvHexValue = binding.tvHexValue;

        //返回
        binding.linkBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallBack != null) {
                    mCallBack.onConfirmClick();
                }
                dismiss();
            }
        });
    }

    /**
     * 设置ViewModel状态监听
     */
    private void setupViewModelObservers() {

        //setDeviceMac


        //toggleMusic

        //设置颜色
        //mPreventAndLedColorAndAudioViewModel.selectCustomColor()

        //设置防误触
        //mPreventAndLedColorAndAudioViewModel.setPreventAccidental();

        //设置音效
        //mPreventAndLedColorAndAudioViewModel.toSaveAudioName

        // 监听UI事件
        mPreventAndLedColorAndAudioViewModel.getUiEventsState().observe(getViewLifecycleOwner(), uiEvent -> {
            if (uiEvent != null) {
                handleUiEvent(uiEvent);
            }
        });
    }

    /**
     * 设置设备MAC地址并加载设备数据
     */
    private void setDeviceMac(String deviceMac) {
        if (mPreventAndLedColorAndAudioViewModel != null && deviceMac != null) {
            // 加载设备相关数据
            mPreventAndLedColorAndAudioViewModel.loadDeviceColor(deviceMac, new Function4<LedColor, Boolean, Boolean, String, Unit>() {
                @Override
                public Unit invoke(LedColor ledColor, Boolean isPreventAccidental, Boolean isCanMusic, String musicName) {
                    Log.i(TAG, "invoke: "+ ledColor.toHex() + ":"+isPreventAccidental + ":" + isCanMusic  + ":" + musicName);

//                    String preColor = String.format("#%06X", (ledColor.getColor()));
//                    String preColor = String.format("#%06X", (0xFFFFFF & ledColor.getColor()));

                    float hue = getHueFromHexColor(ledColor.toHex());
                    binding.seekBarHue.setProgress((int) hue);
                    // 更新数值显示

//                    Log.i(TAG, "invoke111: " + hue);
//
//                    // 将HSV转换为RGB颜色 (饱和度=1, 亮度=1)
//                    float[] hsv = {(int) hue, 1.0f, 1.0f};
//                    int color = Color.HSVToColor(hsv);
//
//                    Log.i(TAG, "invoke222: " + color);
                    Integer viewId = colorToIdMap.get(ledColor.toHex());
                    if (viewId != null) {
                        updateBorderColor(viewId);
                    } else {
                        clearAllSelections();
                    }

                    //匹配颜色

                    //防误触
                    if(isPreventAccidental){
                        swithNotStatus = 1;
                        binding.autoBrightnessSwitch.setImageResource(R.drawable.switch_on);
                    }else {
                        swithNotStatus = 0;
                        binding.autoBrightnessSwitch.setImageResource(R.drawable.switch_off);
                    }

                    //是否开启音乐
                    if (isCanMusic){
                        swithVoiceStatus = 1;
                        binding.autoBrightnessSwitch1.setImageResource(R.drawable.switch_on);
                    }else {
                        swithVoiceStatus = 0;
                        binding.autoBrightnessSwitch1.setImageResource(R.drawable.switch_off);
                    }

                    //音乐类型
                    if(musicName.equals("1")){
                        selectTab(tab1, 0,false);
                    }else if (musicName.equals("2")){
                        selectTab(tab2, 1,false);
                    }else {
                        selectTab(tab3, 2,false);
                    }
                    return null;
                }
            });
        }
    }

    /**
     * 处理ViewModel的UI事件
     */
    private void handleUiEvent(com.zkjd.lingdong.ui.led.PreventAndLedColorAndAudioViewModel.UiEvent uiEvent) {
        if (uiEvent instanceof com.zkjd.lingdong.ui.led.PreventAndLedColorAndAudioViewModel.UiEvent.ShowError) {
            com.zkjd.lingdong.ui.led.PreventAndLedColorAndAudioViewModel.UiEvent.ShowError errorEvent = 
                (com.zkjd.lingdong.ui.led.PreventAndLedColorAndAudioViewModel.UiEvent.ShowError) uiEvent;
            // 显示错误信息
            new ToastUtilOverApplication().showToast(mContext,errorEvent.getMessage());
            Log.e(TAG, "ViewModel错误: " + errorEvent.getMessage());
        } else if (uiEvent instanceof com.zkjd.lingdong.ui.led.PreventAndLedColorAndAudioViewModel.UiEvent.ColorApplied) {
            // 颜色应用成功
            new ToastUtilOverApplication().showToast(mContext,"颜色设置已保存");
            Log.i(TAG, "颜色设置已应用");
        }
    }

    private void setupSeekBar() {
        seekBarHue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            private int recoredPosition = 0 ;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
               
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                recoredPosition = seekBar.getProgress();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 检查条件，如果不满足则阻止滑动
                mPreventAndLedColorAndAudioViewModel.isReturnControl(deviceMac, new Function1<Boolean, Unit>() {
                    @Override
                    public Unit invoke(Boolean isReturnControl) {
                        if (!isReturnControl) {
                            seekBar.setProgress(recoredPosition);
                            new ToastUtilOverApplication().showToast(mContext, "请长按妥妥贴5-10s\n进入返控模式后设置颜色");

                        }else{
                            updateColorDisplayAccordingSeekBar();
                        }
                        return Unit.INSTANCE;
                    }
                });

            }
        });
    }

    /**
     * 从颜色值获取色相(Hue)
     * @param color 颜色值 (int)
     * @return 色相值 (0-360)
     */
    private float getHueFromColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        return hsv[0]; // hsv[0] 就是色相值，范围 0-360
    }

    /**
     * 从HEX颜色字符串获取色相
     * @param hexColor HEX颜色字符串，如 "#FF0000"
     * @return 色相值 (0-360)
     */
    private float getHueFromHexColor(String hexColor) {
        try {
            int color = Color.parseColor(hexColor);
            return getHueFromColor(color);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return 0f;
        }
    }

    private void updateColorDisplayAccordingSeekBar() {
        // 获取当前色相值 (0-360)
        int hue = seekBarHue.getProgress();

        // 更新数值显示
//        tvHue.setText(hue + "°");

        // 将HSV转换为RGB颜色 (饱和度=1, 亮度=1)
        float[] hsv = {hue, 1.0f, 1.0f};
        int color = Color.HSVToColor(hsv);



        // 更新色相和HEX文本
        tvHueValue.setText("色相: " + hue + "°");
        tvHexValue.setText("HEX: " + String.format("#%06X", (0xFFFFFF & color)));

        String preColor = String.format("#%06X", (0xFFFFFF & color));


        mPreventAndLedColorAndAudioViewModel.selectCustomColor(preColor);

        setColorOfPlatte(preColor);
        Log.i(TAG, "updateColorDisplay preColor: " + preColor + hue);



        // 更新预览区域背景色
        colorPreview.setBackgroundColor(color);
        // 根据背景色亮度调整文本颜色
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        double brightness = 0.299 * red + 0.587 * green + 0.114 * blue;
        int textColor = brightness > 128 ? Color.BLACK : Color.WHITE;

        // 获取预览区域中的所有TextView并设置文本颜色
        for (int i = 0; i < colorPreview.getChildCount(); i++) {
            View child = colorPreview.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(textColor);
            }
        }

        // 强制重绘SeekBar以更新渐变颜色
        seekBarHue.invalidate();
    }

    // 预设颜色按钮点击事件
    private void setupColorClickListeners(View rootView) {
        // 为每个颜色方块设置点击监听器
        int[] colorViewIds = {R.id.colorButton1, R.id.colorButton2, R.id.colorButton3, R.id.colorButton4, R.id.colorButton5, R.id.colorButton6, R.id.colorButton7, R.id.colorButton8};
        for (int id : colorViewIds) {

            TextView colorView = rootView.findViewById(id);

            colorView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    mPreventAndLedColorAndAudioViewModel.isReturnControl(deviceMac, new Function1<Boolean, Unit>() {
                        @Override
                        public Unit invoke(Boolean isReturnControl) {
                            if(!isReturnControl){
                                new ToastUtilOverApplication().showToast(mContext,"请长按妥妥贴5-10s\n进入返控模式后设置颜色");
                            }else{
                                String color = idToColorMap.get(view.getId());
                                String hueValue = (String) view.getTag();
                                int preId = view.getId();
                                if (hueValue != null) {
                                    try {
                                        int hue = Integer.parseInt(hueValue);
                                        // 更新SeekBar位置
                                        seekBarHue.setProgress(hue);
                                        // 更新颜色显示
                                        updateColorDisplayAccordingSeekBar();
                                        //跟新选中按钮边框颜色
                                        updateBorderColor(preId);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            return Unit.INSTANCE;
                        }
                    });
                }
            });
        }
    }

    //    将颜色转化为id
    private void setColorOfPlatte(String preColor){
        Integer viewId = colorToIdMap.get(preColor);
        if (viewId != null) {
            updateBorderColor(viewId);
        } else {
            clearAllSelections();
        }

    }

    //根据id聚焦当前颜色选项卡
    private void updateBorderColor(int preId){
        clearAllSelections();
        getView().findViewById(preId).setSelected(true);
        Log.i(TAG, "updateBorderColor: " + preId);
    }

    // 清除所有选择状态的方法
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

    private Callback mCallBack;

    public void setCallBack(Callback callback) {
        this.mCallBack = callback;
    }

    public interface Callback{
        //        将点击事件传递到root页面
        void onConfirmClick();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}