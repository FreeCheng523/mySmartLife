package com.deepal.ivi.hmi.smartlife.bean;

public class AgileSmartDevice extends SmartDevice {

    // 默认构造函数
    public AgileSmartDevice() {
        super();
    }

    // 带参数的构造函数
    public AgileSmartDevice(String deviceName, int deviceType, int connectType, int connectStatus,long addedTime,String bleName) {
        super(deviceName, deviceType, connectType, connectStatus,addedTime,bleName);
    }

    // 使用deviceId作为MAC地址的便捷方法
    public String getMacAddress() {
        return getDeviceId(); // deviceId就是MAC地址
    }

    public void setMacAddress(String macAddress) {
        setDeviceId(macAddress); // 设置deviceId即为设置MAC地址
    }

    // 使用deviceBattery作为电池电量的便捷方法
    public int getBatteryLevel() {
        return getDeviceBattery();
    }

    public void setBatteryLevel(int batteryLevel) {
        setDeviceBattery(batteryLevel);
    }
    public void setBleName(String bleName) {
        setDeviceBleName(bleName);
    }

}