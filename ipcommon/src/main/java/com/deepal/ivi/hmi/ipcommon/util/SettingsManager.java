package com.deepal.ivi.hmi.ipcommon.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import com.deepal.ivi.hmi.ipcommon.IPcApplication;


public class SettingsManager {

    private static final String TAG = "SettingsManager";
    private static final String PREF_NAME = "app_settings";

    private static SharedPreferences getSharedPreferences() {
        return IPcApplication.getInstance().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static SharedPreferences.Editor getEditor() {
        return getSharedPreferences().edit();
    }

    /**
     * 设置Int类型的值
     * @param key key值
     * @param value int值
     */
    public static void setIntData(String key, int value) {
        try {
            SharedPreferences.Editor editor = getEditor();
            editor.putInt(key, value);
            boolean result = editor.commit(); // 或者使用 editor.apply() 异步提交
            Log.d(TAG, "setIntData: key = " + key + ", value = " + value + ", result = " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取Int类型的值
     * @param key key值
     * @return int值
     */
    public static int getIntData(String key, int defValue) {
        int value = getSharedPreferences().getInt(key, defValue);
        Log.d(TAG, "getIntData: key = " + key + ", value = " + value);
        return value;
    }

    public static void setLongData(String key, long value) {
        try {
            SharedPreferences.Editor editor = getEditor();
            editor.putLong(key, value);
            boolean result = editor.commit(); // 或者使用 editor.apply() 异步提交
            Log.d(TAG, "setLongData: key = " + key + ", value = " + value + ", result = " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Long getLongData(String key, long defValue) {
        Long value = getSharedPreferences().getLong(key, defValue);
        Log.d(TAG, "getLongData: key = " + key + ", value = " + value);
        return value;
    }

    // 可选：添加其他数据类型的方法
    public static void setStringData(String key, String value) {
        try {
            SharedPreferences.Editor editor = getEditor();
            editor.putString(key, value);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getStringData(String key, String defValue) {
        return getSharedPreferences().getString(key, defValue);
    }

    public static void setBooleanData(String key, boolean value) {
        try {
            SharedPreferences.Editor editor = getEditor();
            editor.putBoolean(key, value);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean getBooleanData(String key, boolean defValue) {
        return getSharedPreferences().getBoolean(key, defValue);
    }

    // 清除指定key的数据
    public static void removeData(String key) {
        try {
            SharedPreferences.Editor editor = getEditor();
            editor.remove(key);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 清除所有数据
    public static void clearAllData() {
        try {
            SharedPreferences.Editor editor = getEditor();
            editor.clear();
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
