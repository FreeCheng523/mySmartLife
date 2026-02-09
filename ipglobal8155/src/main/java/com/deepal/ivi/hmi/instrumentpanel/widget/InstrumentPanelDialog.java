package com.deepal.ivi.hmi.instrumentpanel.widget;

import android.app.Dialog;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;

import com.deepal.ivi.hmi.instrumentpanel.R;
import com.deepal.ivi.hmi.instrumentpanel.databinding.IpDialogInstrumentPanelBinding;
import com.deepal.ivi.hmi.ipvehiclecommon.IApplication;
import com.deepal.ivi.hmi.ipcommon.data.bean.SetWrite;
import com.deepal.ivi.hmi.ipcommon.util.SettingsManager;
import com.deepal.ivi.hmi.ipvehiclecommon.service.MonitorService;
import com.deepal.ivi.hmi.ipcommon.util.AndroidUtil;
import com.deepal.ivi.hmi.ipvehiclecommon.viewmode.InstrumentPanelViewModel;

/**
 * domestic155
 */
public class InstrumentPanelDialog extends DialogFragment {
    private final static String TAG = "InstrumentPanelDialog";
    private ThumbWithTextDrawable thumbDrawable;
    private InstrumentPanelViewModel mViewModel;
    private IpDialogInstrumentPanelBinding mBinding;
    private int swithStatus ;
    private int light = 60;
    private int theme ;
    private int showMode ;
    private Context context;
    private String ipName;
    public InstrumentPanelDialog(Context context,String ipName) {
        this.context = context;
        this.ipName = ipName;
    }
    public InstrumentPanelDialog(Context context) {
        this.context = context;
        this.ipName = "";
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                // 获取屏幕尺寸
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                Log.d(TAG, "Screen dimensions - Width: " + displayMetrics.widthPixels + "px");
                window.setLayout((int) getResources().getDimension(R.dimen.ip_dp_999), (int) getResources().getDimension(R.dimen.ip_dp_851));
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                WindowManager.LayoutParams params = window.getAttributes();
                params.dimAmount = 0.05f;
                window.setAttributes(params);
                Log.i(TAG, "window dimensions - width: " + window.getAttributes().width + "px");
            }
            // 设置点击外部区域不关闭弹窗
            dialog.setCanceledOnTouchOutside(false);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //mViewModel = new ViewModelProvider(requireActivity()).get(InstrumentPanelViewModel.class);
        mViewModel = IApplication.getInstrumentPanelViewModel();
        mViewModel.init();
        getInitData();
    }

    private void getInitData() {
        swithStatus = SettingsManager.getIntData(SetWrite.AUTO_SWITCH_STATUS,0);
        Log.i(TAG, "swithStatus: " + swithStatus);
        light = SettingsManager.getIntData(SetWrite.SET_LIGHT_VALUE,60);
        Log.i(TAG, "light: " + light);
        theme = SettingsManager.getIntData(SetWrite.SET_THEME,0);
        Log.i(TAG, "theme: " + theme);
        showMode = SettingsManager.getIntData(SetWrite.SHOW_MODE,0);
        Log.i(TAG, "showMode: " + showMode);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = IpDialogInstrumentPanelBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setView();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG,"IP 小仪表,onresume");
        showView();
    }

    private void setView() {
        mBinding.autoBrightnessSwitch.setOnClickListener(view -> {
            Log.i(TAG, "自动亮度被点击,点击前的状态："+ swithStatus);
            if (swithStatus == 0){
                swithStatus = 1;
                mBinding.autoBrightnessSwitch.setImageResource(R.mipmap.light_swith_on);
                this.light = AndroidUtil.getNearestLevel(AndroidUtil.getLightness());
                SettingsManager.setIntData(SetWrite.SET_LIGHT_VALUE,this.light);
                mViewModel.setLight(this.light,"auto");
            }else {
                swithStatus = 0;
                mBinding.autoBrightnessSwitch.setImageResource(R.mipmap.light_swith_off);
            }
            subscribeLightSwitch();
        });
        mBinding.them1.setOnClickListener(view -> {
            Log.d("TAG", "主题1被选中");
            theme = 1;
            setThemeImg();
        });
        mBinding.them2.setOnClickListener(view -> {
            Log.d("TAG", "主题2被选中");
            theme = 2;
            setThemeImg();
        });
        mBinding.them3.setOnClickListener(view -> {
            theme = 3;
            Log.d("TAG", "主题3被选中");
            setThemeImg();

        });
        mBinding.brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int adjustedProgress = AndroidUtil.getNearestLevel(progress);
                    if (adjustedProgress != progress) {
                        seekBar.setProgress(adjustedProgress);
                        SettingsManager.setIntData(SetWrite.SET_LIGHT_VALUE,adjustedProgress);
                        return;
                    }
                }
                thumbDrawable.setProgress(progress);
                Log.i(TAG, "light progress: "+progress);
                mViewModel.setLight(progress, "auto");
                SettingsManager.setIntData(SetWrite.SET_LIGHT_VALUE,progress);
                seekBar.invalidate();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {
                thumbDrawable.setAlpha(255);
            }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                thumbDrawable.setAlpha(200);
            }
        });

        mBinding.autoRadioButton.setOnClickListener(view -> {
            this.showMode = AndroidUtil.getApperanceMode();
            mViewModel.setMode(showMode);
            SettingsManager.setIntData(SetWrite.SHOW_MODE,-1);
            IApplication.getInstance().registerComponentCallbacks(new ComponentCallbacks2() {
                @Override
                public void onConfigurationChanged(@NonNull Configuration configuration) {
                    int currentNightMode = configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK;
                    handleModeChange(currentNightMode);
                }
                @Override
                public void onTrimMemory(int i) {

                }
                @Override
                public void onLowMemory() {

                }
            });
        });
        mBinding.lightRadioButton.setOnClickListener(view -> {
            mViewModel.setMode(0);
            SettingsManager.setIntData(SetWrite.SHOW_MODE,0);
        });
        mBinding.darkRadioButton.setOnClickListener(view -> {
            mViewModel.setMode(1);
            SettingsManager.setIntData(SetWrite.SHOW_MODE,1);
        });
        mBinding.exit.setOnClickListener(view -> {
            dismiss();
        });
    }

    public void showView(){
        mBinding.titleIp.setText(this.ipName);
        //渲染亮度进度
        showLightSeekBar();
        //渲染开关
        if (swithStatus == 0){
            mBinding.autoBrightnessSwitch.setImageResource(R.mipmap.light_swith_off);
            mViewModel.lightLiveData.removeObserver(brightnessObserver);
        }else {
            mBinding.autoBrightnessSwitch.setImageResource(R.mipmap.light_swith_on);
        }
        subscribeLightSwitch();


        //渲染主题
        setThemeImg();
        mViewModel.setTheme(theme);

        //渲染模式
        if (showMode == 0){
            mBinding.displayModeRadioGroup.check(R.id.lightRadioButton);
        } else if (showMode == 1){
            mBinding.displayModeRadioGroup.check(R.id.darkRadioButton);
        }else {
            mBinding.displayModeRadioGroup.check(R.id.autoRadioButton);
        }
        Log.i(TAG, "显示模式："+AndroidUtil.getApperanceMode()+",showmode:"+showMode);
        mViewModel.setMode(showMode==-1?AndroidUtil.getApperanceMode():showMode);
    }

    public void subscribeLightSwitch() {
        Log.i(TAG, "subscribeLightSwitch,swithStatus = "+ swithStatus);
        if (swithStatus == 1){
            Log.i(TAG, "自动亮度打开，Brightness observer registered.开始注册服务");
            MonitorService.setBrightnessChangeListener(mViewModel);
            // 启动前台服务
            Intent serviceIntent = new Intent(context, MonitorService.class);
            context.startForegroundService(serviceIntent);
            mViewModel.lightLiveData.observe(this,brightnessObserver);
            Log.d(TAG, "启动前台服务成功");

        }else {
            MonitorService.setBrightnessChangeListener(null);
            context.stopService(new Intent(context, MonitorService.class));
            Log.i(TAG, "自动亮度关闭，Brightness observer unregistered.");
        }
        SettingsManager.setIntData(SetWrite.AUTO_SWITCH_STATUS,swithStatus);
    }

    public void setThemeImg(){
        Log.i(TAG, "setThemeImg,theme = "+ theme);
        if (theme == 1){
            mBinding.themeRadioImg1.setImageResource(R.mipmap.theme_checked_rectangle);
            mBinding.themeRadioImg2.setImageResource(R.mipmap.theme_unchecked_rectangle);
            mBinding.themeRadioImg3.setImageResource(R.mipmap.theme_unchecked_rectangle);
        }else if (theme == 2){
            mBinding.themeRadioImg2.setImageResource(R.mipmap.theme_checked_rectangle);
            mBinding.themeRadioImg1.setImageResource(R.mipmap.theme_unchecked_rectangle);
            mBinding.themeRadioImg3.setImageResource(R.mipmap.theme_unchecked_rectangle);
        }else if (theme == 3) {
            mBinding.themeRadioImg3.setImageResource(R.mipmap.theme_checked_rectangle);
            mBinding.themeRadioImg2.setImageResource(R.mipmap.theme_unchecked_rectangle);
            mBinding.themeRadioImg1.setImageResource(R.mipmap.theme_unchecked_rectangle);
        }
        SettingsManager.setIntData(SetWrite.SET_THEME,theme);
        mViewModel.setTheme(theme);
    }

    private void showLightSeekBar() {
        thumbDrawable = new ThumbWithTextDrawable(requireContext(), mBinding.brightnessSeekBar.getProgress());
        mBinding.brightnessSeekBar.setThumb(thumbDrawable);
        // 添加 padding 防止两端被裁剪
        int thumbWidth = thumbDrawable.getIntrinsicWidth();
        int thumbHeight = thumbDrawable.getIntrinsicHeight();
        mBinding.brightnessSeekBar.setPadding(thumbWidth/2, thumbHeight/2, thumbWidth/2, thumbHeight/2); //修改百分百显示不全问题
        mBinding.brightnessSeekBar.setProgress(light);
        mViewModel.setLight(light, "auto");
    }

    private Observer<Integer> brightnessObserver = new Observer<>() {
        @Override
        public void onChanged(Integer integer) {
            Log.i(TAG, "亮度onChanged: " + integer);
            mBinding.brightnessSeekBar.setProgress(integer);
        }
    };

    // 新增方法：处理模式变化
    private void handleModeChange(int nightMode) {
        Log.i(TAG, "handleModeChange: "+nightMode);
        switch (nightMode) {
            case Configuration.UI_MODE_NIGHT_YES:
                // 深色模式
                mViewModel.setMode(1); // 对应深色模式
                break;
            case Configuration.UI_MODE_NIGHT_NO:
                // 浅色模式
                mViewModel.setMode(0); // 对应浅色模式
                break;
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
            default:
                // 未定义/自动模式
                mViewModel.setMode(0); // 对应自动模式
                break;
        }
    }
}

