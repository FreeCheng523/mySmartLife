package com.deepal.ivi.hmi.ipvehiclecommon.model;

import android.content.Context;
import android.content.res.Configuration;

import java.util.ArrayList;
import java.util.List;

public class ThemeManager {
    private static ThemeManager instance;
    private List<OnThemeChangeListener> listeners = new ArrayList<>();
    
    public interface OnThemeChangeListener {
        void onThemeChanged(boolean isDarkMode);
    }
    
    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }
    
    public void addListener(OnThemeChangeListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(OnThemeChangeListener listener) {
        listeners.remove(listener);
    }
    
    public void notifyThemeChange(Context context) {
        boolean isDarkMode = isDarkMode(context);
        for (OnThemeChangeListener listener : listeners) {
            listener.onThemeChanged(isDarkMode);
        }
    }
    
    public boolean isDarkMode(Context context) {
        int nightMode = context.getResources().getConfiguration().uiMode & 
                       Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }
}