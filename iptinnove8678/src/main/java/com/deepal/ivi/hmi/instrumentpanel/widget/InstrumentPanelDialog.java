package com.deepal.ivi.hmi.instrumentpanel.widget;

import android.app.Dialog;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;

import com.deepal.ivi.hmi.ipvehiclecommon.IApplication;
import com.deepal.ivi.hmi.instrumentpanel.R;
import com.deepal.ivi.hmi.ipcommon.data.bean.SetWrite;
import com.deepal.ivi.hmi.instrumentpanel.databinding.IpDialogInstrumentPanelBinding;
import com.deepal.ivi.hmi.ipcommon.util.SettingsManager;
import com.deepal.ivi.hmi.ipvehiclecommon.model.VehicleDataManager;
import com.deepal.ivi.hmi.ipcommon.util.AndroidUtil;
import com.deepal.ivi.hmi.ipvehiclecommon.viewmode.InstrumentPanelViewModel;

/**
 * tinnove8678
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
    public InstrumentPanelDialog(Context context, String ipName) {
        this.context = context;
        this.ipName = ipName;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        Window window = dialog.getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        window.setDimAmount(0.3f); //这里必须要加，因为会有默认透明度
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
        window.setAttributes(params);
        // 设置点击外部区域不关闭弹窗
        //dialog.setCanceledOnTouchOutside(false);
        getInitData();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL,R.style.IPBlurDialogTheme);
        //mViewModel = new ViewModelProvider(requireActivity()).get(InstrumentPanelViewModel.class);
        mViewModel = IApplication.getInstrumentPanelViewModel();
        Log.i(TAG, "InstrumentPanelDialog 的mViewModel == "+mViewModel);
        mViewModel.init();
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
        Log.i(TAG, "IP 小仪表,onresume");
        showView();
    }

    private void setBrightnessSwitchShow(boolean isShow){
        if (isShow){
            mBinding.brightnessSeekBar.setVisibility(View.VISIBLE);
            mBinding.valueLow.setVisibility(View.VISIBLE);
            mBinding.valueHigh.setVisibility(View.VISIBLE);

            // 恢复原来的约束
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mBinding.themeTextView.getLayoutParams();
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            params.topToBottom = ConstraintLayout.LayoutParams.UNSET;
            params.topMargin = getResources().getDimensionPixelSize(R.dimen.ip_dp_224);
            mBinding.themeTextView.setLayoutParams(params);
        }else {
            mBinding.brightnessSeekBar.setVisibility(View.GONE);
            mBinding.valueLow.setVisibility(View.GONE);
            mBinding.valueHigh.setVisibility(View.GONE);

            // 调整 themeTextView 的约束到 autoBrightnessSwitch 下方
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mBinding.themeTextView.getLayoutParams();
            params.topToTop = ConstraintLayout.LayoutParams.UNSET;
            params.topToBottom = R.id.brightnessTextView;
            params.topMargin = getResources().getDimensionPixelSize(R.dimen.ip_dp_20);
            mBinding.themeTextView.setLayoutParams(params);
        }
    }
    private void setView() {
        mBinding.autoBrightnessSwitch.setOnClickListener(view -> {
            Log.i(TAG, "自动亮度被点击,点击前的状态："+ swithStatus);
            if (swithStatus == 0){
                swithStatus = 1;
                mBinding.autoBrightnessSwitch.setImageResource(R.drawable.switch_on);
                setBrightnessSwitchShow(false);
            }else {
                swithStatus = 0;
                mBinding.autoBrightnessSwitch.setImageResource(R.drawable.switch_off);
                setBrightnessSwitchShow(true);
            }
            subscribeLightSwitch();
        });
        mBinding.them1.setOnClickListener(view -> {
            Log.d(TAG, "主题1被选中");
            theme = 1;
            setThemeImg();
        });
        mBinding.them2.setOnClickListener(view -> {
            Log.d(TAG, "主题2被选中");
            theme = 2;
            setThemeImg();
        });
        mBinding.them3.setOnClickListener(view -> {
            theme = 3;
            Log.d(TAG, "主题3被选中");
            setThemeImg();

        });
        mBinding.brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int adjustedProgress = AndroidUtil.getNearestLevel(progress);
                    if (adjustedProgress != progress) {
                        seekBar.setProgress(adjustedProgress);
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
        mViewModel.lightLiveData.observe(getViewLifecycleOwner(), outsideLightValueObserver);
        mBinding.autoRadioButton.setOnClickListener(view -> {
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            this.showMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES ? 1 : 0;
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
        mBinding.titleIp.setText(ipName);
        //渲染开关
        if (swithStatus == 0){
            mBinding.autoBrightnessSwitch.setImageResource(R.drawable.switch_off);
            setBrightnessSwitchShow(true);
        }else {
            mBinding.autoBrightnessSwitch.setImageResource(R.drawable.switch_on);
            setBrightnessSwitchShow(false);
        }
        subscribeLightSwitch();
        //渲染亮度进度
        showLightSeekBar();

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
        mViewModel.setMode(showMode);

    }
    public void subscribeLightSwitch() {
        Log.i(TAG, "subscribeLightSwitch,swithStatus = "+ swithStatus);
        if (swithStatus == 1){
            VehicleDataManager.getInstance().setLightSwithStatus(true);
            Log.i(TAG, "自动亮度打开，Brightness observer registered.");
            int crtValue = VehicleDataManager.getInstance().getBrightness();
            Log.i(TAG, "当前亮度值："+crtValue);
            this.light = AndroidUtil.getNearestLevel(crtValue);
            mViewModel.setLight(AndroidUtil.getNearestLevel(crtValue), "auto");
        }else {
            VehicleDataManager.getInstance().setLightSwithStatus(false);
            Log.i(TAG, "自动亮关闭，Brightness observer unregistered.");
        }
        SettingsManager.setIntData(SetWrite.AUTO_SWITCH_STATUS,swithStatus);
    }

    public void setThemeImg(){
        Log.i(TAG, "setThemeImg,theme = "+ theme);
        if (theme == 1){
            mBinding.themeRadioImg1.setImageResource(R.drawable.theme_checked_circle);
            mBinding.themeRadioImg2.setImageResource(R.drawable.theme_unchecked_circle);
            mBinding.themeRadioImg3.setImageResource(R.drawable.theme_unchecked_circle);
            mBinding.themeApperImg1.setImageResource(R.drawable.theme_bg);
            mBinding.themeApperImg1.setVisibility(View.VISIBLE);
            mBinding.themeApperImg2.setVisibility(View.GONE);
            mBinding.themeApperImg3.setVisibility(View.GONE);
        }else if (theme == 2){
            mBinding.themeRadioImg2.setImageResource(R.drawable.theme_checked_circle);
            mBinding.themeRadioImg1.setImageResource(R.drawable.theme_unchecked_circle);
            mBinding.themeRadioImg3.setImageResource(R.drawable.theme_unchecked_circle);
            mBinding.themeApperImg2.setVisibility(View.VISIBLE);
            mBinding.themeApperImg2.setImageResource(R.drawable.theme_bg);
            mBinding.themeApperImg1.setVisibility(View.GONE);
            mBinding.themeApperImg3.setVisibility(View.GONE);
        }else if (theme == 3) {
            mBinding.themeRadioImg3.setImageResource(R.drawable.theme_checked_circle);
            mBinding.themeRadioImg2.setImageResource(R.drawable.theme_unchecked_circle);
            mBinding.themeRadioImg1.setImageResource(R.drawable.theme_unchecked_circle);
            mBinding.themeApperImg3.setVisibility(View.VISIBLE);
            mBinding.themeApperImg3.setImageResource(R.drawable.theme_bg);
            mBinding.themeApperImg2.setVisibility(View.GONE);
            mBinding.themeApperImg1.setVisibility(View.GONE);
        }
        SettingsManager.setIntData(SetWrite.SET_THEME,theme);
        mViewModel.setTheme(theme);
    }

    private void showLightSeekBar() {
        thumbDrawable = new ThumbWithTextDrawable(requireContext(), mBinding.brightnessSeekBar.getProgress());
        mBinding.brightnessSeekBar.setThumb(thumbDrawable);
        mBinding.brightnessSeekBar.setMinimumHeight(thumbDrawable.getIntrinsicHeight());
        // 添加 padding 防止两端被裁剪
        int thumbWidth = thumbDrawable.getIntrinsicWidth(); //72
        mBinding.brightnessSeekBar.setPadding(thumbWidth/2, 0, thumbWidth/2, 0); //修改百分百显示不全问题
        mBinding.brightnessSeekBar.setProgress(light);
        mViewModel.setLight(light, "auto");
    }

    // 处理模式变化
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
                SettingsManager.setIntData(SetWrite.SHOW_MODE,-1);
                break;
        }
    }

    private Observer<Integer>outsideLightValueObserver =  outsideLightValue-> {
        if (thumbDrawable !=null){
            int level = AndroidUtil.getNearestLevel(outsideLightValue);
            thumbDrawable.setProgress(level);
            mBinding.brightnessSeekBar.setProgress(level);
            Log.i(TAG, "当前车机亮度值 "+outsideLightValue+"对应级别: "+level);
        }
    };

}

