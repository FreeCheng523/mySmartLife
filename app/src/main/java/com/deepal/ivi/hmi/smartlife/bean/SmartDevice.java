package com.deepal.ivi.hmi.smartlife.bean;

import java.io.Serializable;

public class SmartDevice implements Serializable {
    public SmartDevice() {}

    public SmartDevice(String deviceName, int deviceType, int connectType, int connectStatus,long addedTime,String bleName) {
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.connectType = connectType;
        this.connectStatus = connectStatus;
        this.addedTime = addedTime;
        this.bleName = bleName;
    }


    public SmartDevice(String deviceName, int deviceType, int connectType, int connectStatus) {
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.connectType = connectType;
        this.connectStatus = connectStatus;
    }


    private long addedTime;

    /**
     * 设备是否开机状态
     */
    private boolean isPowerOn = false;  // 设置默认值（可选，boolean 默认就是 false）


    /**
     * 设备id(唯一).
     * 蓝牙设备为mac地址, USB设备为序列号.
     */
    private String deviceId;

    /**
     * 设备名(支持重命名).
     */
    private String deviceName;


    /**
     * 设备名(蓝牙原名，不可修改).
     */
    private String bleName;
    /**
     * 设备原始名称.
     * 蓝牙设备为蓝牙名称，USB设备为设备名称.
     */
    private String productName;

    /**
     * 设备类型(支持新增, 可用于匹配设备配图).
     * 0-default
     * 1-小仪表
     * 2-物理按键
     * 3-妥妥贴
     * 4-香氛
     * 5-负离子
     */
    private int deviceType;

    /**
     * 连接方式.
     * 0-USB
     * 1-BLE
     * 2-PIN
     * 3-WIFI
     */
    private int connectType;

    /**
     * 连接状态.
     * 0-待连接
     * 1-未连接
     * 2-连接中
     * 3-已连接
     * 4-连接失败
     */
    private int connectStatus;

    /**
     * 设备电量(仅返回无线设备电量).
     */
    private int deviceBattery;



    public String getDeviceName() {
        return deviceName;
    }
    public String getBleName() {
        return bleName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public int getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public int getConnectType() {
        return connectType;
    }

    public void setConnectType(int connectType) {
        this.connectType = connectType;
    }

    public int getConnectStatus() {
        return connectStatus;
    }

    public void setConnectStatus(int connectStatus) {
        this.connectStatus = connectStatus;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    public void setDeviceBleName(String  bleName) {
        this.bleName = bleName;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getDeviceBattery() {
        return deviceBattery;
    }

    public void setDeviceBattery(int deviceBattery) {
        this.deviceBattery = deviceBattery;
    }


    public boolean isPowerOn() {
        return isPowerOn;
    }

    public void setIsPowerOn(boolean isPowerOn) {
        this.isPowerOn = isPowerOn;
    }

    public long getAddedTime() {
        return addedTime;
    }

    public void setAddedTime(long addedTime) {
        this.addedTime = addedTime;
    }

    // 重写toString方法，方便调试
    @Override
    public String toString() {
        return "AgileSmartDevice{" +
                ", macAddress='" + getDeviceId() + '\'' +
                ", batteryLevel=" + getDeviceBattery() +
                ", deviceName='" + getDeviceName() + '\'' +
                ", deviceType=" + getDeviceType() +'\'' +
                ", connectStatus=" + getConnectStatus() +'\'' +
                ", addedTime=" + getAddedTime() +'\'' +
                '}';
    }
}