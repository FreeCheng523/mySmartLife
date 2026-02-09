package com.deepal.ivi.hmi.ipcommon.data.bean;

public class MetaDataBean {
    // 动力类型
    private int carType;

    @Override
    public String toString() {
        return "MetaDataBean{" +
                "carType=" + carType +
                ", light=" + light +
                ", theme=" + theme +
                ", mode=" + mode +
                ", hour=" + hour +
                ", minute=" + minute +
                ", vcuRdySts=" + vcuRdySts +
                ", temperature=" + temperature +
                ", driverMode=" + driverMode +
                ", energerMagMode=" + energerMagMode +
                ", dis=" + dis +
                ", speed=" + speed +
                ", evMileage=" + evMileage +
                ", powerBarRatio=" + powerBarRatio +
                ", oilMileage=" + oilMileage +
                ", oilPercent=" + oilPercent +
                ", vehicleGear=" + vehicleGear +
                ", powerMode=" + powerMode +
                ", leftLightStatus=" + leftLightStatus +
                ", rightLightStatus=" + rightLightStatus +
                ", vehicleDriveStatus=" + vehicleDriveStatus +
                ", energyOption=" + energyOption +
                ", leftFrontTirePressure=" + leftFrontTirePressure +
                ", rightFrontTirePressure=" + rightFrontTirePressure +
                ", rightRearTirePressure=" + rightRearTirePressure +
                ", leftRearTirePressure=" + leftRearTirePressure +
                '}';
    }

    // 亮度
    private int light;
    // 主题
    private int theme;
    // 中控显示模式
    private int mode;

    // 语言
    private int language;
    private int unit;
    // 时
    private int hour;
    // 分
    private int minute;
    // READY指示灯
    private int vcuRdySts;
    //温度
    private int temperature;
    // 驾驶模式
    private int driverMode;
    // drvMode 这个是能源模式还是整车可行驶状态
    private int energerMagMode;
    // dis 不知道含义
    private int dis;
     // 车速
    private float speed;
    // 纯电剩余里程
    private float evMileage;
    // 电量进度条比例
    private int powerBarRatio;
    // 燃油续驶里程
    private float oilMileage;
    // 油量百分比
    private int oilPercent;
    // 整车实际挡位
    private int vehicleGear;
    // 电源模式
    private int powerMode;
    // 左转向灯工作状态
    private int leftLightStatus;
    // 右转向灯工作状态
    private int rightLightStatus;

    /*下面两个应该是可用的，因为不知道字段含义，所以先没用*/
    // 整车可行驶状态
    private int vehicleDriveStatus;
    // 能耗选项设置
    private int energyOption;
    public MetaDataBean(){};
    public MetaDataBean(int carType, int light, int theme, int mode, int hour, int minute,
                        int vcuRdySts, int temperature, int driverMode, int energerMagMode,
                        int dis, float speed, float evMileage, int powerBarRatio,
                        float oilMileage, int oilPercent, int vehicleGear,
                        int leftLightStatus, int rightLightStatus) {
        this.carType = carType;
        this.light = light;
        this.theme = theme;
        this.mode = mode;
        this.hour = hour;
        this.minute = minute;
        this.vcuRdySts = vcuRdySts;
        this.temperature = temperature;
        this.driverMode = driverMode;
        this.energerMagMode = energerMagMode;
        this.dis = dis;
        this.speed = speed;
        this.evMileage = evMileage;
        this.powerBarRatio = powerBarRatio;
        this.oilMileage = oilMileage;
        this.oilPercent = oilPercent;
        this.vehicleGear = vehicleGear;
        this.leftLightStatus = leftLightStatus;
        this.rightLightStatus = rightLightStatus;
    }

    // 左前轮胎压力
    private float leftFrontTirePressure;
    // 右前轮胎压力
    private float rightFrontTirePressure;
    // 右后轮胎压力
    private float rightRearTirePressure;
    // 左后轮胎压力
    private float leftRearTirePressure;

    public int getCarType() {
        return carType;
    }

    public int getVcuRdySts() {
        return vcuRdySts;
    }

    public void setVcuRdySts(int vcuRdySts) {
        this.vcuRdySts = vcuRdySts;
    }

    public void setCarType(int carType) {
        this.carType = carType;
    }

    public int getLanguage() {
        return language;
    }

    public void setLanguage(int language) {
        this.language = language;
    }

    public int getUnit() {
        return unit;
    }

    public void setUnit(int unit) {
        this.unit = unit;
    }

    public int getLight() {
        return light;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public int getDis() {
        return dis;
    }

    public void setDis(int dis) {
        this.dis = dis;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public void setLight(int light) {
        this.light = light;
    }

    public int getTheme() {
        return theme;
    }

    public void setTheme(int theme) {
        this.theme = theme;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getPowerMode() {
        return powerMode;
    }

    public void setPowerMode(int powerMode) {
        this.powerMode = powerMode;
    }

    public int getEnergerMagMode() {
        return energerMagMode;
    }

    public void setEnergerMagMode(int energerMagMode) {
        this.energerMagMode = energerMagMode;
    }

    public float getEvMileage() {
        return evMileage;
    }

    public void setEvMileage(float evMileage) {
        this.evMileage = evMileage;
    }

    public float getOilMileage() {
        return oilMileage;
    }

    public void setOilMileage(float oilMileage) {
        this.oilMileage = oilMileage;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public int getLeftLightStatus() {
        return leftLightStatus;
    }

    public void setLeftLightStatus(int leftLightStatus) {
        this.leftLightStatus = leftLightStatus;
    }

    public int getRightLightStatus() {
        return rightLightStatus;
    }

    public void setRightLightStatus(int rightLightStatus) {
        this.rightLightStatus = rightLightStatus;
    }

    public int getVehicleDriveStatus() {
        return vehicleDriveStatus;
    }

    public void setVehicleDriveStatus(int vehicleDriveStatus) {
        this.vehicleDriveStatus = vehicleDriveStatus;
    }

    public int getVehicleGear() {
        return vehicleGear;
    }

    public void setVehicleGear(int vehicleGear) {
        this.vehicleGear = vehicleGear;
    }

    public float getLeftFrontTirePressure() {
        return leftFrontTirePressure;
    }

    public void setLeftFrontTirePressure(float leftFrontTirePressure) {
        this.leftFrontTirePressure = leftFrontTirePressure;
    }

    public float getRightFrontTirePressure() {
        return rightFrontTirePressure;
    }

    public void setRightFrontTirePressure(float rightFrontTirePressure) {
        this.rightFrontTirePressure = rightFrontTirePressure;
    }

    public float getRightRearTirePressure() {
        return rightRearTirePressure;
    }

    public void setRightRearTirePressure(float rightRearTirePressure) {
        this.rightRearTirePressure = rightRearTirePressure;
    }

    public float getLeftRearTirePressure() {
        return leftRearTirePressure;
    }

    public void setLeftRearTirePressure(float leftRearTirePressure) {
        this.leftRearTirePressure = leftRearTirePressure;
    }

    public int getDriverMode() {
        return driverMode;
    }

    public void setDriverMode(int driverMode) {
        this.driverMode = driverMode;
    }

    public int getPowerBarRatio() {
        return powerBarRatio;
    }

    public void setPowerBarRatio(int powerBarRatio) {
        this.powerBarRatio = powerBarRatio;
    }

    public int getOilPercent() {
        return oilPercent;
    }

    public void setOilPercent(int oilPercent) {
        this.oilPercent = oilPercent;
    }

    public int getEnergyOption() {
        return energyOption;
    }

    public void setEnergyOption(int energyOption) {
        this.energyOption = energyOption;
    }
}
