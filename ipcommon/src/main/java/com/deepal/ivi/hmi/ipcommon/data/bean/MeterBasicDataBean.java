package com.deepal.ivi.hmi.ipcommon.data.bean;

/**
 * 用于封装仪表盘的基本数据
 */
public final class MeterBasicDataBean {
    // 亮度自读调节
    private final boolean brightnessSwitch;
    // 亮度值
    private final int brightnessValue;
    // 模式值
    private final int modelValue;
    // 状态
    private final int status;
    // 主题值
    private final int themeValue;

    public MeterBasicDataBean(int status, boolean brightnessSwitch, int brightnessValue, int themeValue, int modelValue) {
        this.status = status;
        this.brightnessSwitch = brightnessSwitch;
        this.brightnessValue = brightnessValue;
        this.themeValue = themeValue;
        this.modelValue = modelValue;
    }

    public boolean getBrightnessSwitch() {
        return brightnessSwitch;
    }

    public int getBrightnessValue() {
        return brightnessValue;
    }

    public int getModelValue() {
        return modelValue;
    }

    public int getStatus() {
        return status;
    }

    public int getThemeValue() {
        return themeValue;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MeterBasicDataBean)) return false;

        MeterBasicDataBean other = (MeterBasicDataBean) obj;
        return brightnessSwitch == other.brightnessSwitch &&
                status == other.status &&
                brightnessValue == other.brightnessValue &&
                themeValue == other.themeValue &&
                modelValue == other.modelValue;
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(brightnessSwitch);
        result = 31 * result + Integer.hashCode(status);
        result = 31 * result + Integer.hashCode(brightnessValue);
        result = 31 * result + Integer.hashCode(themeValue);
        result = 31 * result + Integer.hashCode(modelValue);
        return result;
    }

    @Override
    public String toString() {
        return "MeterBasicDataBean{" +
                "brightnessSwitch=" + brightnessSwitch +
                ", brightnessValue=" + brightnessValue +
                ", modelValue=" + modelValue +
                ", status=" + status +
                ", themeValue=" + themeValue +
                '}';
    }
}
