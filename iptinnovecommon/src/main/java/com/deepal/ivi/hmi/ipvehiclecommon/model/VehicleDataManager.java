package com.deepal.ivi.hmi.ipvehiclecommon.model;


import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.VehicleAreaInOutCAR;
import android.car.VehicleAreaType;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.CarSensorEvent;
import android.car.hardware.CarSensorManager;
import android.car.hardware.CarVendorExtensionManager;
import android.car.hardware.adas.CarAdasManager;
import android.car.hardware.cabin.CarCabinManager;
import android.car.hardware.cluster.CarClusterInteractionManager;
import android.car.hardware.dvr.CarDvrManager;
import android.car.hardware.hvac.CarHvacManager;
import android.car.hardware.mcu.CarMcuManager;
import android.car.hardware.property.CarPropertyManager;
import android.car.media.CarAudioManager;
import android.content.Context;
import android.util.Log;

import com.deepal.ivi.hmi.ipcommon.util.SettingsManager;
import com.deepal.ivi.hmi.ipvehiclecommon.IApplication;
import com.deepal.ivi.hmi.ipcommon.data.DispatchData;
import com.deepal.ivi.hmi.ipcommon.data.bean.MetaDataBean;
import com.deepal.ivi.hmi.ipcommon.data.bean.SetWrite;
import com.deepal.ivi.hmi.ipcommon.iInterface.DataCallBack;
import com.deepal.ivi.hmi.ipcommon.util.AndroidUtil;

import java.time.LocalTime;
import java.util.concurrent.CopyOnWriteArrayList;


public class VehicleDataManager implements CarSensorManager.OnSensorChangedListener,CarPropertyManager.CarPropertyEventListener,ThemeManager.OnThemeChangeListener {
    private static final String TAG = "VehicleDataManager";
    private volatile static MetaDataBean metaDataBean;
    private CopyOnWriteArrayList<DataCallBack> dataCallBackList;
    private static VehicleDataManager instance ;
    private CarSensorManager mCarSensorManager;
    private CarAdasManager mCarAdasManager;
    private CarVendorExtensionManager mCarVendorExtensionManager;
    private CarHvacManager mCarHvacManager;
    private CarCabinManager mCarCabinManager;
    private CarClusterInteractionManager mCarClusterInteractionManager;
    private CarDvrManager mCarDvrManager;
    private CarMcuManager mCarMcuManager;
    private CarAudioManager mCarAudioManager;
    private CarPropertyManager mCarPropertyManager;
    private boolean isCarServiceReady;
    private CarSensorEvent mCarSensorEvent;
    private CarPropertyValue carPropertyValue;
    private boolean toIp = false;
    private boolean swithStatus;
    private int errRes = -1;
    private Context mContext;
    public VehicleDataManager() {
        dataCallBackList = new CopyOnWriteArrayList<>();
    }

    public static VehicleDataManager getInstance() {
        if (instance == null) {
            synchronized (VehicleDataManager.class){
                if (instance == null) {
                    instance = new VehicleDataManager();
                }
                return instance;
            }
        }
        return instance;
    }

    public void init(Context mContext){
        this.mContext = mContext;
        initCar(IApplication.getInstance());
        initData();
        ThemeManager.getInstance().addListener(this);
    }

    public void initCar(Context context) {
        Log.i(TAG, "initCar: 开始初始化Car的各种Manager");
        Car.createCar(context,null,500, new Car.CarServiceLifecycleListener() {
            @Override
            public void onLifecycleChanged(Car car, boolean ready) {
                if(ready){
                    Log.i(TAG, "initCar: Car服务已就绪");
                    /**获取对应的manager实例，只有在此获取到实例后，才可以调用manager里的接口**/
                    mCarSensorManager = (CarSensorManager) car.getCarManager(android.car.Car.SENSOR_SERVICE);
                    mCarAdasManager = (CarAdasManager) car.getCarManager(Car.ADAS_SERVICE);
                    mCarVendorExtensionManager = (CarVendorExtensionManager) car.getCarManager(Car.VENDOR_EXTENSION_SERVICE);
                    mCarHvacManager = (CarHvacManager) car.getCarManager(Car.HVAC_SERVICE);
                    mCarCabinManager = (CarCabinManager) car.getCarManager(Car.CABIN_SERVICE);
                    mCarDvrManager = (CarDvrManager) car.getCarManager(Car.CAR_DVR_SERVICE);
                    mCarMcuManager = (CarMcuManager) car.getCarManager(Car.MCU_SERVICE);
                    mCarAudioManager = (CarAudioManager) car.getCarManager(Car.AUDIO_SERVICE);
                    mCarPropertyManager= (CarPropertyManager ) car.getCarManager(Car.PROPERTY_SERVICE);
                    mCarClusterInteractionManager = (CarClusterInteractionManager) car.getCarManager(Car.INSTRUMENT_PANEL_SERVICE);
                    Log.i(TAG, "initCar: 获取到manager实例，开始注册信号监听");
                    registerSignal();
                }
            }
        });
    }

    /**
     * 注册信号监听
     */
    public void registerSignal() {
        try {
            if (mCarSensorManager != null) {
                //车速√
                mCarSensorManager.registerListener(this, CarSensorManager.SENSOR_TYPE_CAR_SPEED, CarSensorManager.SENSOR_RATE_NORMAL);
                //挡位√：
                mCarSensorManager.registerListener(this, CarSensorManager.SENSOR_TYPE_GEAR_INFO, CarSensorManager.SENSOR_RATE_NORMAL);
                //油量
                mCarSensorManager.registerListener(this, CarSensorManager.ID_PCU_FUEL_LEFT_OVER, CarSensorManager.SENSOR_RATE_NORMAL);
                //燃油剩余里程
                mCarSensorManager.registerListener(this, CarSensorManager.SENSOR_TYPE_RANGE_REMAINING, CarSensorManager.SENSOR_RATE_NORMAL);
            }
            if (mCarPropertyManager != null){
                //能耗选项√
                mCarPropertyManager.registerListener(this,CarCabinManager.ID_ENG_DIS_REL_REQ,VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL);
                //ready状态√
                mCarPropertyManager.registerListener(this,CarAdasManager.ID_DRIVE_ASSIST_STATUS_FEEDBACK,VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL);
                //左转向灯√
                mCarPropertyManager.registerListener(this,CarCabinManager.ID_BODY_TURN_LEFT_SIGNAL_STATE, VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL);
                //右转向灯√
                mCarPropertyManager.registerListener(this,CarCabinManager.ID_BODY_TURN_RIGHT_SIGNAL_STATE, VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL);
                //驾驶模式√
                mCarPropertyManager.registerListener(this,CarCabinManager.ID_PHEV_DRV_MODE_SET, VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL);
                //纯电续航里程
                mCarPropertyManager.registerListener(this,CarCabinManager.PCU_EDTE_DISP, VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL);
                //纯电剩余电量
                mCarPropertyManager.registerListener(this, CarCabinManager.ID_PHEV_CHRG_DIS_MODE, VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL);
                //亮度√
                mCarPropertyManager.registerListener(this, CarCabinManager.ID_DISPLAY_BRIGHTNESS, VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL);
                //温度
                mCarPropertyManager.registerListener(this, CarHvacManager.ID_HVAC_IN_OUT_TEMP, VehicleAreaInOutCAR.IN_OUT_CAR_OUTSIDE);
                // 油量
                mCarPropertyManager.registerListener(this, CarCabinManager.ID_PLATFORM_POWER_TWO, VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL);
                // 油量
                mCarPropertyManager.registerListener(this, CarClusterInteractionManager.ID_IP_DRV_SHOW_INFO, VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL);
            }
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "registerSignal error : " + e.getMessage());
        }
    }

    public void setLightSwithStatus(boolean status){
        this.swithStatus = status;
    }
    public void addDataCallBack(DataCallBack dataCallBack){
        dataCallBackList.add(dataCallBack);
    }

    @Override
    public void onThemeChanged(boolean isDarkMode) {
        Log.i(TAG, "监听显示模式: " + (isDarkMode?"深色模式":"浅色模式"));
        metaDataBean.setMode(isDarkMode?1:0);
        sendToViewModel("apperMode");
    }

    public MetaDataBean getMetaDataBean(){
        return metaDataBean;
    }

    @Override
    public void onErrorEvent(int propertyId, int areaId) {
        Log.e(TAG, "监听属性值报错onErrorEvent： propertyId: " + propertyId +", areaId:"+ areaId);
    }

    private float calSpeed(float speed){
        float value ;
        if (speed <= 5.0){
            value =  (float) Math.ceil(speed);
        }else {
            value =  (float)Math.floor((speed / 0.98)+2.0f);
        }
        Log.i(TAG, "监听车速: " + speed + "->" + value);
        return value;
    }

    @Override
    public void onSensorChanged(CarSensorEvent carSensorEvent) {
        int propertyId = carSensorEvent.sensorType;
        Log.d(TAG, "梧桐车机返回属性值 propertyId: " + propertyId );
        switch (propertyId) {
            case CarSensorManager.SENSOR_TYPE_CAR_SPEED:
                float speed = carSensorEvent.floatValues[0];
                metaDataBean.setSpeed(calSpeed(speed));
                toIp = true;
                Log.i(TAG, "监听车速: " + speed);
                break;
            case CarSensorManager.SENSOR_TYPE_GEAR_INFO:
                int gearValue = getVcuGearPosn(); //不需要转，直接传给小仪表
                String gearStr = "";
                switch (gearValue) {
                    case 2: gearStr = "N档"; break;
                    case 1: gearStr = "R挡"; break;
                    case 0: gearStr = "P挡"; break;
                    case 3: gearStr = "D档"; break;
                }
                Log.d(TAG, "监听档位信息-> gearValue：" + gearValue + ", 对应值："+ gearStr);
                metaDataBean.setVehicleGear(gearValue); //不需要转，直接传
                toIp = true;
                break;
            case CarSensorManager.ID_PCU_FUEL_LEFT_OVER:
                //剩余油量
                float oilPercent = mCarSensorEvent.intValues[0];
                Log.i(TAG, "监听剩余油量: " + oilPercent);
                //metaDataBean.setOilPercent((int) oilPercent);
                break;
            case CarSensorManager.SENSOR_TYPE_RANGE_REMAINING:
                float value = carSensorEvent.getCarRangeRemaining( null).value;
                metaDataBean.setOilMileage( value);
                Log.i(TAG, "监听燃油续驶里程: " + value);
                toIp = true;
                break;
            case CarSensorManager.SENSOR_TYPE_NIGHT:
                int state = carSensorEvent.intValues[0];
                Log.i(TAG, "监听黑夜模式isNightMode？ " + state);
                metaDataBean.setMode(state);
                toIp = true;
                break;
        }
        if (toIp && metaDataBean != null){
            Log.i(TAG, "MetaDataBean数据发送1:"+metaDataBean.toString());
            sendToViewModel("MeterData");
        }

    }

    private void sendToViewModel(String type) {
        for (DataCallBack dataCallBack:dataCallBackList){
               Log.i(TAG, "更新："+type);
               dataCallBack.onDataChange(new DispatchData(metaDataBean,type));
               toIp = false;
            }

    }

    @Override
    public void onChangeEvent(CarPropertyValue carPropertyValue) {
        int propertyId = carPropertyValue.getPropertyId();
        Log.d(TAG, "梧桐车机返回属性值 propertyId: " + propertyId );
        int areaId = -999 ;
        switch (propertyId) {
            case CarCabinManager.ID_DISPLAY_BRIGHTNESS:
                int brightValue = (int) carPropertyValue.getValue();
                metaDataBean.setLight(brightValue);
                Log.i(TAG, "监听亮度值: " + brightValue);
                if (swithStatus){
                    sendToViewModel("light");
                }
                break;
            case CarCabinManager.ID_ENG_DIS_REL_REQ:
                // 能耗显示相关请求\反馈
                //areaId=0x00,value=PCU_eDTEDesignformula_FB（0x0=动态计算;0x1=CLTC;0x2=WLTC;0x3=Reserve）
                areaId = carPropertyValue.getAreaId();
                int energyOptionValue = (int) carPropertyValue.getValue();
                if (areaId == 0x00){
                    metaDataBean.setEnergyOption(energyOptionValue);
                    Log.i(TAG, "监听能耗显示相关反馈: " + energyOptionValue);
                    toIp = true;
                }
                break;
            case CarAdasManager.ID_DRIVE_ASSIST_STATUS_FEEDBACK:
                // data[0]=0x08,data[1]=PCU_VcuRdySts（0x0=no Ready；0x1=Ready）
                areaId = carPropertyValue.getAreaId();
                int rndyStatus = (int) carPropertyValue.getValue();
                if (areaId == 0x08){
                    Log.i(TAG, "监听PCU_VcuRdySts: " + rndyStatus);
                    metaDataBean.setVcuRdySts(rndyStatus);
                    toIp = true;
                }else {
                    Log.i(TAG,
                            "ID_DRIVE_ASSIST_STATUS_FEEDBACK的反馈值非ready状态: " +
                                    "areaId:"+ areaId +
                                    "value :"+ rndyStatus);
                }
                break;
            case CarCabinManager.ID_BODY_TURN_LEFT_SIGNAL_STATE:
                int turnLeft = (int) carPropertyValue.getValue();
                Log.i(TAG, "监听左转向灯: " + turnLeft);
                metaDataBean.setLeftLightStatus(turnLeft);
                toIp = true;
                break;
            case CarCabinManager.ID_BODY_TURN_RIGHT_SIGNAL_STATE:
                //0x0=OFF；0x1=ON；
                //0x2=Not used；0x3=Error
                int turnRight = (int) carPropertyValue.getValue();
                Log.i(TAG, "监听右转向灯: " + turnRight);
                metaDataBean.setRightLightStatus(turnRight);
                toIp = true;
                break;
            case CarCabinManager.ID_PHEV_DRV_MODE_SET:
                areaId = carPropertyValue.getAreaId();
                int driveValue = (int) carPropertyValue.getValue();
                if (areaId == 0x4){
                    if (driveValue == 2){//市区模式
                        metaDataBean.setEnergerMagMode(0);
                    }else if (driveValue == 3){//高速模式
                        metaDataBean.setEnergerMagMode(1);
                    } else if (driveValue == 6) { //山地模式
                        metaDataBean.setEnergerMagMode(2);
                    }
                    toIp = true;
                    Log.i(TAG, "能源模式监听值: " + driveValue);
                }else if(areaId == 0x1){
                    //data[0]=0x04,data[1]=PCU_REEVDrvMod（0x0=reserved；0x1=经济；0x2=舒适；0x3=运动；0x4=自定义；其他就设专属吧
                    metaDataBean.setDriverMode(driveValue);
                    toIp = true;
                    Log.i(TAG, " 驾驶模式监听值: " + driveValue);
                }
                break;
            case CarCabinManager.PCU_EDTE_DISP:
                int dis = (int) carPropertyValue.getValue();
                Log.i(TAG, "监听纯电续航里程: " + dis);
                metaDataBean.setEvMileage(dis);
                toIp = true;
                break;
            case CarCabinManager.ID_PHEV_CHRG_DIS_MODE:
                //int eleBar = (int) carPropertyValue.getValue();
                int eleBar = getCdcResiSocDisp();
                Log.i(TAG, "监听纯电电量: " + eleBar);
                metaDataBean.setPowerBarRatio(eleBar);
                break;
            case CarHvacManager.ID_HVAC_IN_OUT_TEMP:
                int temperature = (int) carPropertyValue.getValue();
                Log.i(TAG, "监听车外温度值: " + temperature);
                metaDataBean.setTemperature(temperature);
                toIp = true;
                break;
            case CarClusterInteractionManager.ID_IP_DRV_SHOW_INFO:
                int fuelPercentCluster = getClusterInteractionManagerFuelLevelPercent();
                metaDataBean.setOilPercent(fuelPercentCluster);
                Log.i(TAG, "CarClusterInteractionManager获取的燃油比例: "+fuelPercentCluster);
                break;

        }
        if (toIp &&  metaDataBean != null){
            Log.i(TAG, "MetaDataBean数据发送2:"+metaDataBean.toString());
            sendToViewModel("MeterData");
        }

    }

    private void initData()  {
        int carType  = getCarType();                                                //车辆类型
        int light = SettingsManager.getIntData(SetWrite.SET_LIGHT_VALUE,60); //亮度
        int theme = SettingsManager.getIntData(SetWrite.SET_THEME,1);                                                                // 主题，先默认设置为1吧
        int apperanceMode = SettingsManager.getIntData(SetWrite.SHOW_MODE,0);  // 显示模式
        int hour = LocalTime.now().getHour();                                // 时
        int minutes = LocalTime.now().getMinute();                           // 分
        int readyStatus =  getVcuRdySts();                                          // ready状态
        int temperature = getTemperature();                                                       // 温度，先默认设置为25吧
        int driveMode = getCdcDrvrModSetSts();                                      //驾驶模式：舒适模式、经济模式
        int energyMode = getVcuEnyMagtMod();                                        //能源模式：市区模式、高速模式
        int energyCnseSelect = getVcuVehEgyCnseSelectCfm();                         // 能耗选项设置:CLTC、WLTC
        float speed =  getEspVehSpd();                                              // 速度
        float eleRemainMile = getVcuEvDrvResiMilg();                                // 纯电剩余里程
        int eleRatio = getCdcResiSocDisp();                                         //剩余电量
        float oilRemainMile = getVcuFulLimdDrvgDst();          // 燃油剩余里程
        int oilRatio = getClusterInteractionManagerFuelLevelPercent();               //剩余油量getVcuFuelLevelPercent();
        int gear = getVcuGearPosn();                           // 挡位
        int turnLeft =  getBcmTurnIndcrLe();                    //左转向灯
        int turnRight =  getBcmTurnIndcrRi();                   //右转向灯
        Log.i(TAG, "当前车辆数据值: " +
                "\n 车辆类型: " + carType +
                "\n 车辆亮度: " + light +
                "\n 车辆主题: " + theme +
                "\n 显示模式: " + apperanceMode +
                "\n 当前时间: " + hour + ":" + minutes +
                "\n 准备状态: " + readyStatus +
                "\n 车辆温度: " + temperature +
                "\n 驾驶模式: " + driveMode +
                "\n 能源模式: " + energyMode +                  // ×
                "\n 能耗选项: " + energyCnseSelect +
                "\n 车速: "    + speed +
                "\n 纯电剩余里程: " + eleRemainMile +
                "\n 剩余电量: " + eleRatio +                  // ×
                "\n 燃油剩余里程: " + oilRemainMile +          // ×
                "\n 剩余油量: " + oilRatio +                  // ×
                "\n 档位: " + gear +
                "\n 左转向灯: " + turnLeft +
                "\n 右转向灯: " + turnRight);
        metaDataBean = new MetaDataBean(
                carType,
                light,
                theme,
                apperanceMode,
                hour,
                minutes,
                readyStatus,
                temperature,
                driveMode,
                energyMode,
                energyCnseSelect,
                speed,
                eleRemainMile,
                eleRatio,
                oilRemainMile,
                oilRatio,
                gear,
                turnLeft,
                turnRight
        );
    }

    public int getBrightness() {
        try {
            int brightValue = mCarCabinManager.getIntProperty(CarCabinManager.ID_DISPLAY_BRIGHTNESS, VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL);
            return brightValue;
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "获取车辆亮度异常"+e.getMessage());
        }
        return errRes;
    }

    /**
     * 获取车辆类型
     * 0：无
     * 1：燃油
     * 2：PHEV 插混
     * 3：REEV 增程
     * 4：纯电
     * 5：HEV  普通混动
     * @return
     */

    public int getCarType() {
        int res = 1;
        try {
            byte []  offlineData = mCarVendorExtensionManager.getBytesProperty(CarVendorExtensionManager.ID_VENDOR_OFF_LINE_STATUS, 0);
            String offlineDataHex = AndroidUtil.bytesToHex(offlineData);
            Log.i(TAG, "车型配置字: " + offlineDataHex);
            int carType = AndroidUtil.extractBitsFromByte(offlineData[0], 5, 3);
            Log.i(TAG, "车辆类型: " + carType);
            switch (carType){
                case 1:
                case 2:
                case 3:
                case 5:
                    res = 0;
                    break;
            }
            Log.i(TAG, "车辆类型: " + res);
            return res;
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "获取车辆类型异常"+e.getMessage());
        }
        return errRes;
    }


    /**
     * 获取车辆准备状态
     * @return areaId=0x08,value=PCU_VcuRdySts（0x0=no Ready；0x1=Ready）
     */
    public int getVcuRdySts(){
        try {
            int vcuRdySts =mCarAdasManager.getIntProperty(CarAdasManager.ID_DRIVE_ASSIST_STATUS_FEEDBACK,0x8);
            Log.i(TAG, "车辆准备状态: " + vcuRdySts);
            return vcuRdySts;
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "获取车辆准备状态异常"+e.getMessage());
        }
        return errRes;
    }

    /**
     * 获取驾驶模式
     * areaId=0x03,value=HU_SpecialModeSet
     * （0x0=reserved;0x1=市区模式;0x2=长途模式;0x3=山地模式;0x4=应急模式;0x5=驻车模式;
     *  0x6=Super Race）
     * @return
     */
    public int getCdcDrvrModSetSts() {
        int res=0;
        try {
            /**
             * 	舒适  2：
             * 	经济  1：
             * 	运动  3：
             * 	专属：4
             * 	自定义：5
             */
            int driveMode = mCarCabinManager.getIntProperty(CarCabinManager.ID_PHEV_DRV_MODE_SET, 0x1);
            switch (driveMode){
                case 1:{
                    res = 2;
                    break;
                }
                case 2:{
                    res = 0;
                    break;
                }
                case 3:{
                    res = 1;
                    break;
                }
                case 4:{
                    res = 4;
                    break;
                }
                case 5:{
                    res = 3;
                    break;
                }
            }
            Log.i(TAG, "驾驶模式值: " + driveMode+"转换值: " + res);
            return res;
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "获取驾驶模式异常" + e.getMessage());
        }
        return errRes;
    }

    /**
     * 获取能源模式
     * areaId=0x05,value=HU_REEVDrvMod（0x0=reserved;0x1=智能模式;
     * 0x2=市区模式;0x3=高速模式;0x4=应急模式;0x5=驻车充电模式;0x6~0x7=reserved）
     * @return
     */
    public int getVcuEnyMagtMod(){
        try {
            int value = mCarCabinManager.getIntProperty(CarCabinManager.ID_PHEV_DRV_MODE_SET,0x4);
            Log.i(TAG, "能源模式值: " + value);
            return value;
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "获取能源模式值异常"+e.getMessage());
        }
        return errRes;
    }

    /**
     * 获取能耗选项
     * areaId=0x00,value=THU_eDTEDesignformula（0x0=动态计算;0x1=CLTC;0x2=WLTC;0x3=Reserve）
     * @return
     */
    public int getVcuVehEgyCnseSelectCfm(){
        try {
            int value = mCarCabinManager.getIntProperty(CarCabinManager.ID_ENG_DIS_REL_REQ,0x0);
            Log.i(TAG, "能源选项值: " + value);
            return value; //不需要转换直接传给小仪表
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "获取能源选项值异常"+e.getMessage());
        }
        return errRes;
    }

    /**
     * 获取车速
     * @return
     */
    public float getEspVehSpd(){
        try {
            mCarSensorEvent = mCarSensorManager.getLatestSensorEvent(CarSensorManager.SENSOR_TYPE_CAR_SPEED);
            float value = mCarSensorEvent.floatValues[0];
            Log.i(TAG, "车速值: " + value);
            return value;
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "车速值"+e.getMessage());
        }
        return errRes;
    }

    /**
     * 获取纯电剩余里程
     * @return
     */
    public int getVcuEvDrvResiMilg(){
        try {
            int value = mCarCabinManager.getIntProperty(CarCabinManager.PCU_EDTE_DISP,0x0);
            Log.i(TAG, "获取纯电剩余里程: " + value);
            return value;
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "获取纯电剩余里程"+e.getMessage());
        }
        return errRes;
    }


    /**
     * 获取电量
     * @return
     */
    public int getCdcResiSocDisp(){
        try {
            int value = mCarCabinManager.getIntProperty(CarCabinManager.ID_PHEV_CHRG_DIS_MODE,0x5);
            Log.i(TAG, "获取纯电电量: " + value+"%");
            return value;
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "获取纯电电量"+e.getMessage());
        }
        return errRes;
    }

    /**
     * 获取油量比例
     * @return
     */
    public int getVcuFuelLevelPercent(){
        try {
            mCarSensorEvent = mCarSensorManager.getLatestSensorEvent(CarSensorManager.ID_PCU_FUEL_LEFT_OVER);
            int oidBar = mCarSensorEvent.intValues[0];
            Log.i(TAG, "油量比例: " + oidBar);
            //return BigDecimal.valueOf(oidBar).multiply(BigDecimal.valueOf(100)) .setScale(0, RoundingMode.HALF_UP).intValue();
            return oidBar;
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "获取油量比例"+e.getMessage());
        }
        return errRes;
    }

    public int getClusterInteractionManagerFuelLevelPercent(){
        try {
            int value = mCarClusterInteractionManager.getIntProperty(CarClusterInteractionManager.ID_IP_DRV_SHOW_INFO,0x0);
            Log.i(TAG, "getClusterInteractionManagerFuelLevelPercent获取燃油比例: " + value+"%");
            return value;
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "getClusterInteractionManagerFuelLevelPercent获取燃油比例："+e.getMessage());
        }
        return errRes;

    }


    /**
     * 获取燃油剩余里程
     * @return
     */
    public float getVcuFulLimdDrvgDst(){
        try {
            mCarSensorEvent = mCarSensorManager.getLatestSensorEvent(CarSensorManager.SENSOR_TYPE_RANGE_REMAINING);
            float oidBar = mCarSensorEvent.getCarRangeRemaining(null).value;
            Log.i(TAG, "获取燃油剩余里程: " + oidBar);
            return oidBar;
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "获取燃油剩余里程"+e.getMessage());
        }
        return errRes;
    }

    /**
     * 获取档位信息
     * @return
     */
    public int getVcuGearPosn(){
        int value = -1;
        try {
            mCarSensorEvent = mCarSensorManager.getLatestSensorEvent(CarSensorManager.SENSOR_TYPE_GEAR_INFO);
            int gear = mCarSensorEvent.intValues[0]; //档位:0x0=Parking；0x1=Reverse；0x2=Neutral；0x3=D
            Log.i(TAG, "获取档位信息: " + gear);
            return gear;
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "获取档位信息"+e.getMessage());
        }
        return value ;
    }

    /**
     * 获取左转向灯信息
     * @return
     */
    public int getBcmTurnIndcrLe(){
        try {
            int value = mCarCabinManager.getIntProperty(CarCabinManager.ID_BODY_TURN_LEFT_SIGNAL_STATE,VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL);
            int res = value==0?1:2;
            Log.i(TAG, "获取左转向灯信息: " + value+",to IP: "+ res);
            return res;
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "获取左转向灯信息"+e.getMessage());
        }
        return errRes;
    }

    /**
     * 获取有转向灯信息
     * @return
     */
    public int getBcmTurnIndcrRi(){
        try {
            int value = mCarCabinManager.getIntProperty(CarCabinManager.ID_BODY_TURN_RIGHT_SIGNAL_STATE,VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL);
            int res = value==0?1:2;
            Log.i(TAG, "获取右转向灯信息: " + value+",to IP: "+ res);
            return res;
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "获取右转向灯信息"+e.getMessage());
        }
        return errRes;
    }

    public int getTemperature(){
        try {
            int outTemperature = mCarHvacManager.getIntProperty(CarHvacManager.ID_HVAC_IN_OUT_TEMP,VehicleAreaInOutCAR.IN_OUT_CAR_OUTSIDE);
            Log.i(TAG, "获取温度: " + outTemperature);
            return outTemperature;
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "获取温度"+e.getMessage());
        }
        return errRes;
    }

}
