package com.deepal.ivi.hmi.ipcommon.util;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.deepal.ivi.hmi.ipcommon.IPcApplication;

import java.util.Calendar;

public class AndroidUtil {
    private static Context mContext = IPcApplication.getInstance().getApplicationContext();
    private final static  String TAG = "AndroidUtil";
    public static final int LEVEL_MAX = 100;
    public static final int LEVEL_MIN = 1;
    public static final int VALUE_MIN = 1;
    public static final int VALUE_MAX = 255;
    private static final float VALUE_PER_LEVEL = 1.0f * (VALUE_MAX - VALUE_MIN) / (LEVEL_MAX - LEVEL_MIN);
    public static int getBrightnessValue() {
        try {
            return Settings.System.getInt(mContext.getContentResolver(), "main_control_brightness");
        } catch (Settings.SettingNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取当前屏幕亮度
     * @return
     */
    public static int getLightness(){
        int value = 0;
        try {
            value = Settings.System.getInt(mContext.getContentResolver(), "main_control_brightness");
            int level = LEVEL_MIN;
            if (value >= VALUE_MAX) {
                level = LEVEL_MAX;
            } else if (value > VALUE_MIN) {
                // Value转换level，向下取整，level的计算减去了LEVEL_MIN，此时需要补上LEVEL_MIN
                level = (int) Math.floor(value / VALUE_PER_LEVEL) + LEVEL_MIN;
            }
            Log.i(TAG, "getBrightnessByLevel = " + level + " value = " + value);
            return level;
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "获取亮度异常");
            e.printStackTrace();
        }
        return -1;
    }

    public static int setLightNormal(int light){
        if (light >= 0 && light < 21) {
            light = 20;
        } else if (21 <= light && light < 41) {
            light = 40;
        } else if (41 <= light && light < 61) {
            light = 60;
        }else if (61 <= light && light < 81) {
            light = 80;
        } else  if (81 <= light && light < 101) {
            light = 100;
        }else {
            light = 60;
        }
        return light;
    }

    public static int convertValueToIp(String type,int value){
        int ipValue = -1;
        switch(type){
            case "carType":{
                if (value == 0){
                    ipValue = 1;
                }else if (value == 1) {
                    ipValue = 0;
                }
                break;
            }
            case "energyType":{
                if (value == 0){
                    ipValue = 1;
                }else if (value == 1) {
                    ipValue = 2;
                }
                break;
            }
        }
        Log.i(TAG, "数据转换：" + type + ": " + value + " -> " + ipValue);
        return ipValue;
    }

    /**
     * 获取当前中控模式
     * @return
     */
    public static int getApperanceMode() {
        Activity mActivity = IPcApplication.getCurrActivity();
        UiModeManager uiModeManager = (UiModeManager) mActivity.getSystemService(Context.UI_MODE_SERVICE);
        int nightMode = uiModeManager.getNightMode();
        switch (nightMode) {
            case UiModeManager.MODE_NIGHT_AUTO:
                // 自动模式（根据时间或日出日落）
                break;
            case UiModeManager.MODE_NIGHT_YES:
                // 深色模式（手动开启）
                break;
            case UiModeManager.MODE_NIGHT_NO:
                // 浅色模式（手动关闭）
                break;
            case UiModeManager.MODE_NIGHT_CUSTOM:
                // 自定义模式（Android 12+）
                break;
            default:
                // 未知模式
                break;
        }
        Log.d(TAG, "中控模式 Night mode: " + nightMode);
        return nightMode;
    }


    /**
     * 根据传入的字段返回当前时间的年、月、日、时、分、秒
     * @param field 支持的字段：year/month/day/hour/minute/second
     * @return 对应的时间值，如果字段无效则返回 -1
     */
    public static int getTimeField(String field) {
        Calendar calendar = Calendar.getInstance();
        switch (field.toLowerCase()) {
            case "year":
                return calendar.get(Calendar.YEAR);
            case "month":
                // 注意：Calendar.MONTH 返回 0-11，所以 +1
                return calendar.get(Calendar.MONTH) + 1;
            case "day":
                return calendar.get(Calendar.DAY_OF_MONTH);
            case "hour":
                return calendar.get(Calendar.HOUR_OF_DAY); // 24小时制
            case "minute":
                return calendar.get(Calendar.MINUTE);
            case "second":
                return calendar.get(Calendar.SECOND);
            default:
                return -1; // 无效字段
        }
    }

    public static int getNearestLevel(int progress) {
        int[] brightnessLevels = {20, 40, 60, 80, 100};
        int nearestLevel = brightnessLevels[0];
        int minDifference = Math.abs(progress - nearestLevel);

        for (int level : brightnessLevels) {
            int difference = Math.abs(progress - level);
            if (difference < minDifference) {
                minDifference = difference;
                nearestLevel = level;
            }
        }

        return nearestLevel;
    }
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    public static int getBrightnessLevel() {
        int value = getBrightnessValue();
        int level = LEVEL_MIN;
        if (value >= VALUE_MAX) {
            level = LEVEL_MAX;
        } else if (value > VALUE_MIN) {
            // Value转换level，向下取整，level的计算减去了LEVEL_MIN，此时需要补上LEVEL_MIN
            level = (int) Math.floor(value / VALUE_PER_LEVEL) + LEVEL_MIN;
        }
        Log.i(TAG, "getBrightnessByLevel = " + level + " value = " + value);
        return level;
    }

    /**
     * 从字节数组中提取指定范围的字节并转换为整型数值
     * @param data 字节数组
     * @param start 起始字节索引（从0开始）
     * @param length 要提取的字节数量
     * @return 整型数值
     */
    public static int extractBytesToInt(byte[] data, int start, int length) {
        if (data == null || start < 0 || length <= 0 || start + length > data.length) {
            Log.e(TAG, "extractBytesToInt 参数错误");
            return -1;
        }

        int result = 0;
        for (int i = 0; i < length; i++) {
            result = (result << 8) | (data[start + i] & 0xFF);
        }
        Log.i(TAG, "extractBytesToInt: " + bytesToHex(data) + " -> " + result);
        return result;
    }

    /**
     * 从单个字节中提取指定位数和长度的值
     * @param data 要提取的字节数据
     * @param start 起始位索引（从0开始，从高位到低位）
     * @param length 要提取的位长度
     * @return 提取的整型数值
     */
    public static int extractBitsFromByte(byte data, int start, int length) {
        if (start < 0 || start > 7 || length <= 0 || start + length > 8) {
            Log.e(TAG, "extractBitsFromByte 参数错误: start=" + start + ", length=" + length);
            return -1;
        }

        int unsignedData = data & 0xFF;
        int mask = (1 << length) - 1;

        // 直接右移start位，然后取低length位
        int result = (unsignedData >> start) & mask;

        Log.i(TAG, "extractBitsFromByte: data=" + String.format("0x%02X", data) +
                ", start=" + start + ", length=" + length + " -> " + result);
        return result;
    }
}
