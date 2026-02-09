package com.deepal.ivi.hmi.smartlife.utils;

import android.view.View;

public abstract class ThrottleClickListener implements View.OnClickListener {
    @Override
    public void onClick(View v) {
        if (ClickThrottle.isSafe()) {
            onThrottleClick(v);
        }
    }
    protected abstract void onThrottleClick(View v);
}