package com.deepal.ivi.hmi.smartlife.utils;

public final class ClickThrottle {
    private static final long WINDOW = 500L;          // 500 ms 内只响应一次
    private static long lastClickTime;

    public static boolean isSafe() {
        long now = System.currentTimeMillis();
        if (now - lastClickTime > WINDOW) {
            lastClickTime = now;
            return true;
        }
        return false;
    }
}