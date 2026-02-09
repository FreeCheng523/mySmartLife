package com.deepal.ivi.hmi.smartlife.bean;

public class DynamicStickerButton {
    private String id;
    /**
     * 应用控制域.
     * 0-MEDIA 媒体控制
     * 1-APP 应用启动
     * 2-CAR 车机控制
     * 3-CAR_TYPE 车辆型号
     */
    private int category;
    /**
     * 设备名称.
     */
    private String name;
    /**
     * 执行的操作代码.
     */
    private String actionCode;
    /**
     * 图标资源ID.
     */
    private int iconResId;
    /**
     * 按钮类型.
     * 1-按钮
     * 2-旋钮
     * 3-车型
     */
    private int buttonType;
    /**
     * 配置字.
     */
    private String configWords;
}
