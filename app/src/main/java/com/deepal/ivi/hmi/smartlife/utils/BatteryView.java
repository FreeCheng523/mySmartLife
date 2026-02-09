package com.deepal.ivi.hmi.smartlife.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.deepal.ivi.hmi.smartlife.R;

public class BatteryView extends RelativeLayout {

    private ImageView batteryImageView;
    private int batteryLevel = 50;
    private int lowBatteryResId;
    private int mediumBatteryResId;
    private int highBatteryResId;

    public BatteryView(Context context) {
        super(context);
        init(context, null);
    }

    public BatteryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BatteryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        // 1. 创建 ImageView
        batteryImageView = new ImageView(context);
        LayoutParams params = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        );
        batteryImageView.setLayoutParams(params);
        batteryImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        addView(batteryImageView);

        // 2. 读取 XML 配置的图片资源
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.BatteryView);
            batteryLevel = ta.getInt(R.styleable.BatteryView_batteryLevel, 50);
            lowBatteryResId = ta.getResourceId(R.styleable.BatteryView_lowBatteryImage, R.drawable.battery10);
            mediumBatteryResId = ta.getResourceId(R.styleable.BatteryView_mediumBatteryImage, R.drawable.battery20);
            highBatteryResId = ta.getResourceId(R.styleable.BatteryView_highBatteryImage, R.drawable.battery60);
            ta.recycle();
        }

        // 3. 首次加载图片
        updateBatteryImage();
    }

    /**
     * 根据电量切换图片
     */
    private void updateBatteryImage() {
        int resId = getResIdByLevel(batteryLevel);
        if (resId != 0) {
            batteryImageView.setImageResource(resId);
        }
    }

    private int getResIdByLevel(int level) {
        if (level <= 10) return lowBatteryResId;
        if (level <= 50) return mediumBatteryResId;
        return highBatteryResId;
    }

    public void setBatteryLevel(int level) {
        if (level < 0) level = 0;
        if (level > 100) level = 100;
        this.batteryLevel = level;
        updateBatteryImage();
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryImages(int low, int medium, int high) {
        this.lowBatteryResId = low;
        this.mediumBatteryResId = medium;
        this.highBatteryResId = high;
        updateBatteryImage();
    }
}