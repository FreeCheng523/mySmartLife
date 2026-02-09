package com.deepal.ivi.hmi.smartlife.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import java.util.List;

public class LocalStoreManager {
    private static final String TAG = "LocalStoreManager";

    private SharedPreferences mSP;


    private static class LocalStoreManagerInstance {
        private static final LocalStoreManager instance = new LocalStoreManager();
    }

    private LocalStoreManager() {

    }

    public static LocalStoreManager getInstance() {
        return LocalStoreManagerInstance.instance;
    }

    public void init(Context context) {
        mSP = context.getSharedPreferences("miniagent.cache",Context.MODE_PRIVATE);
    }

    public void write(String key, String value) {
        if (mSP != null) {
            mSP.edit().putString(key, value).apply();
        }
    }

    public String read(String key, String defaultValue) {
        if (mSP != null) {
            return mSP.getString(key, defaultValue);
        }
        return defaultValue;
    }

    public <T> void storeData(String key, T data) {
        if (data != null) {
            String json = new Gson().toJson(data);
            Log.i(TAG, "storeData json: " + json);
            write(key, json);
        } else {
            Log.i(TAG, "storeData: key:" + key + ", the data is null!");
        }
    }

    public <T> List<T> getStoreData(String key, Class<T> cls) {
        String json = read(key, "");
        if (!TextUtils.isEmpty(json)) {
            return GsonParser.parseList(json, cls);
        } else {
            return null;
        }
    }
    public String getStoreString(String key) {
        return read(key, "");
    }

}