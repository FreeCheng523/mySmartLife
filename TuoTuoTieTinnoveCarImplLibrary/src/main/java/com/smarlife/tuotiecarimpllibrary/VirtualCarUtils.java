package com.smarlife.tuotiecarimpllibrary;

import android.content.Context;
import android.util.Log;

import com.openos.virtualcar.VirtualCar;
import com.openos.virtualcar.VirtualCarPropertyCallBack;
import com.openos.virtualcar.VirtualCarPropertyManager;
import com.openos.virtualcar.VirtualServiceReadyListener;
import com.openos.virtualcar.entity.VirtualCarValue;
import com.openos.virtualcar.exception.VirtualCarException;
import com.openos.virtualcar.property.VirtualCarCabinManager;
import com.openos.virtualcar.property.VirtualCarHvacManager;
import com.openos.virtualcar.property.VirtualCarMcuManager;
import com.openos.virtualcar.property.VirtualCarSensorManager;

import timber.log.Timber;

/**
 * @des: 虚拟车工具类
 *
 */
public class VirtualCarUtils {

    /**
     * 日志打印tag
     */
    private static final String TAG = "VirtualCarHandler";

    private VirtualCar mVirtualCar;

    public VirtualCarPropertyManager propertyManager;

    private static final int GLOBAL_AREA = 0x0;

    /**
     * 车速
     */
    private float mSpeed = 0.0f;


    /**
     * 挡位
     */
    private int mGear = 0;

    /**
     * 车架号
     */
    private String mVin = null;

    public VirtualCarUtils(Context context) {
        initVirtual(context);
    }

    /**
     *  虚拟车监听回调接口常量
     *  车速-VirtualCarSensorManager.SENSOR_TYPE_CAR_SPEED = 0x31600202
     *  挡位-VirtualCarSensorManager.SENSOR_TYPE_CURRENT_GEAR = 0x31400231
     *  车架号-VirtualCarMcuManager.ID_VIN_INFO = 0x31100A07
     */
    private static final int[] FUNC_IDS = {
            VirtualCarSensorManager.SENSOR_TYPE_CAR_SPEED,
            VirtualCarSensorManager.SENSOR_TYPE_CURRENT_GEAR,
            VirtualCarMcuManager.ID_VIN_INFO,
            VirtualCarCabinManager.ID_DOOR_TRUNK_POS,
            VirtualCarCabinManager.ID_CHILD_LOCK,
            VirtualCarSensorManager.SENSOR_TYPE_STEER_WHEEL_HEATG,
            VirtualCarHvacManager.ID_ZONED_SEAT_TEMP,
            VirtualCarHvacManager.ID_ZONED_FAN_DIRECTION,
            VirtualCarCabinManager.ID_PHEV_DRV_MODE_SET,
            VirtualCarHvacManager.ID_WINDOW_DEFROSTER_ON,
            VirtualCarHvacManager.ID_ZONED_TEMP_SETPOINT,
            VirtualCarHvacManager.ID_ZONED_FAN_SPEED_SETPOINT,
            //空调开关
            VirtualCarHvacManager.ID_ZONED_HVAC_POWER_ON,
            //小憩模式
            VirtualCarCabinManager.ID_HVAC_SCENE_MODE_ACT,
            //后视镜
            VirtualCarCabinManager.ID_BACK_MIRROR_SW,
            //二排安全带未系报警提示音
            VirtualCarCabinManager.ID_REAR_SEATBELT_WARNING_SW,
            //雨刮模式
            VirtualCarCabinManager.ID_WIPER_ADJUST_MODE,
            //座椅通风开
            VirtualCarHvacManager.ID_ZONED_HVAC_SEAT_VENTILATION,
            //老板键
            VirtualCarCabinManager.ID_BODY_SEAT_STATUS,
            //低速行人报警音
            VirtualCarCabinManager.ID_IP_CLUSTER_SOUND_EFFECT_SWITCH
    };

    private void initVirtual(Context context) {
        mVirtualCar = VirtualCar.createVirtualCar(context, listener, null);
    }

    private VirtualServiceReadyListener listener = new VirtualServiceReadyListener() {
        @Override
        public void serviceReadySuccess() {
            propertyManager = mVirtualCar.getVirtualCarManager(VirtualCar.PROPERTY_SERVICE);
            //开始注册相关的监听回调
            try {
                propertyManager.register(FUNC_IDS, carPropertyCallBack);
            } catch (VirtualCarException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void serviceReadyFailed() {
            Log.i(TAG,"virtual car service failed");
        }
    };

    private final VirtualCarPropertyCallBack carPropertyCallBack = new VirtualCarPropertyCallBack() {
        @Override
        public void onCallBack(VirtualCarValue virtualCarValue) {
            if (virtualCarValue == null) {
                return;
            }
            switch (virtualCarValue.getFuncId()) {
                case VirtualCarSensorManager.SENSOR_TYPE_CAR_SPEED:
                    mSpeed = (float) virtualCarValue.getValue();
                    break;
                case VirtualCarSensorManager.SENSOR_TYPE_CURRENT_GEAR:
                    mGear = (int) virtualCarValue.getValue();
                    break;
                case VirtualCarMcuManager.ID_VIN_INFO:
                    mVin = (String) virtualCarValue.getValue();
                    break;
            }
        }
    };

    /**
     * 获取车速
     */
    public float getSpeed() throws VirtualCarException {
        if (propertyManager != null) {
            mSpeed = (float) propertyManager.getValue(VirtualCarSensorManager.SENSOR_TYPE_CAR_SPEED, GLOBAL_AREA);
        }
        return mSpeed;
    }


    /**
     * 获取配置字
     */
    public byte[] getStings() throws VirtualCarException {
        if (propertyManager != null) {
            return (byte[]) propertyManager.getValue(0x31700A04, GLOBAL_AREA);
        }
        return null;
    }

    /**
     * 获取挡位
     * GEAR_NEUTRAL : 1 N挡
     * GEAR_REVERSE : 2 R挡
     * GEAR_PARK : 4 P挡
     * GEAR_DRIVE : 8 D挡
     */
    public int getGear() throws VirtualCarException {
        if (propertyManager != null) {
            mGear = (int) propertyManager.getValue(VirtualCarSensorManager.SENSOR_TYPE_CURRENT_GEAR, GLOBAL_AREA);
        }
        return mGear;
    }

    /**
     *
     * @return 车架号
     */
    public String getVin() throws VirtualCarException {
        if (propertyManager != null) {
            mVin = (String) propertyManager.getValue(VirtualCarMcuManager.ID_VIN_INFO, GLOBAL_AREA);
        }
        return mVin;
    }

    public int getToggleTrunk() throws VirtualCarException {
        int letrunk = 0;
        if (propertyManager != null) {
            letrunk = (int) propertyManager.getValue(VirtualCarCabinManager.ID_DOOR_TRUNK_POS, 0);
        }
        return letrunk;
    }

    public void setToggleTrunk(int newState) throws VirtualCarException {
        if (propertyManager != null) {
            Timber.tag(TAG).w("propertyManager不为空: %s", newState);
            propertyManager.setValue(VirtualCarCabinManager.ID_DOOR_TRUNK_POS, 0,newState);
        }

    }
    //童锁
    public int getToggleChildLock(int newState) throws VirtualCarException {
        int childLock = 0;
        if (propertyManager != null) {
            childLock = (int) propertyManager.getValue(VirtualCarCabinManager.ID_CHILD_LOCK, newState);
        }
        return childLock;
    }

    public void setToggleChildLock(int newState,int typeS) throws VirtualCarException {
        if (propertyManager != null) {

            propertyManager.setValue(VirtualCarCabinManager.ID_CHILD_LOCK, typeS,newState);
        }

    }

    //方向盘加热
    public Object getToggleSteeringWheelHeat() throws VirtualCarException {
        Object wheelheat = 0;
        if (propertyManager != null) {
            wheelheat = propertyManager.getValue(VirtualCarSensorManager.SENSOR_TYPE_STEER_WHEEL_HEATG, 0x0);
        }
        return wheelheat;
    }

    public void setToggleSteeringWheelHeat(int newState) throws VirtualCarException {
        if (propertyManager != null) {

            propertyManager.setValue(VirtualCarSensorManager.SENSOR_TYPE_STEER_WHEEL_HEATG, 0x0,newState);
        }

    }

    //座椅加热
    public Object getToggleMainSeatHeat(int types) throws VirtualCarException {
        Object wheelheat = 0;
        if (propertyManager != null) {
            wheelheat = propertyManager.getValue(VirtualCarHvacManager.ID_ZONED_SEAT_TEMP, types);
        }
        return wheelheat;
    }

    public void setToggleMainSeatHeat(int newStatem,int typs) throws VirtualCarException {
        if (propertyManager != null) {

            propertyManager.setValue(VirtualCarHvacManager.ID_ZONED_SEAT_TEMP, typs,newStatem);
        }

    }

    //空调吹风方向
    public Object getAdjustAcWindDirection() throws VirtualCarException {
        Object wheelheat = 0;
        if (propertyManager != null) {
            wheelheat = propertyManager.getValue(VirtualCarHvacManager.ID_ZONED_FAN_DIRECTION, 1);
        }
        return wheelheat;
    }

    public void setAdjustAcWindDirection(int newStatemp) throws VirtualCarException {
        if (propertyManager != null) {

            propertyManager.setValue(VirtualCarHvacManager.ID_ZONED_FAN_DIRECTION, 1,newStatemp);
        }

    }

    //驾驶模式和能量管理模式
    public Object getAdjustDrivingMode(int types) throws VirtualCarException {
        Object wheelheat = 0;
        if (propertyManager != null) {
            wheelheat = propertyManager.getValue(VirtualCarCabinManager.ID_PHEV_DRV_MODE_SET, types);
        }
        return wheelheat;
    }

    public void setAdjustDrivingMode(int newStatemp,int types) throws VirtualCarException {
        if (propertyManager != null) {

            propertyManager.setValue(VirtualCarCabinManager.ID_PHEV_DRV_MODE_SET, types,newStatemp);
        }

    }

    //除霜除雾
    public Object getToggleDefrost(int types) throws VirtualCarException {
        Object wheelheat = 0;
        if (propertyManager != null) {
            wheelheat = propertyManager.getValue(VirtualCarHvacManager.ID_WINDOW_DEFROSTER_ON, types);
        }
        return wheelheat;
    }

    public void setToggleDefrost(int newStatemp,int types) throws VirtualCarException {
        if (propertyManager != null) {

            propertyManager.setValue(VirtualCarHvacManager.ID_WINDOW_DEFROSTER_ON, types,newStatemp);
        }

    }

    //空调风量
    public Object getFanSpeed() throws VirtualCarException {
        Object wheelheat = 0;
        if (propertyManager != null) {
            wheelheat = propertyManager.getValue(VirtualCarHvacManager.ID_ZONED_FAN_SPEED_SETPOINT, 1);
        }
        return wheelheat;
    }

    public void setFanSpeed(int newStatemp) throws VirtualCarException {
        if (propertyManager != null) {

            propertyManager.setValue(VirtualCarHvacManager.ID_ZONED_FAN_SPEED_SETPOINT, 1,newStatemp);
        }

    }

    //温度调节
    public Object getACTemperature() throws VirtualCarException {
        Object wheelheat = 0;
        if (propertyManager != null) {
            wheelheat = propertyManager.getValue(VirtualCarHvacManager.ID_ZONED_TEMP_SETPOINT, 1);
        }
        return wheelheat;
    }

    public void setACTemperature(float newStatemp) throws VirtualCarException {
        if (propertyManager != null) {

            propertyManager.setValue(VirtualCarHvacManager.ID_ZONED_TEMP_SETPOINT, 1,newStatemp);
        }

    }

    //空调开关
    public Object getAirCdFront() throws VirtualCarException {
        Object wheelheat = 0;
        if (propertyManager != null) {
            wheelheat = propertyManager.getValue( VirtualCarHvacManager.ID_ZONED_HVAC_POWER_ON, 1);
        }
        return wheelheat;
    }

    public void setAirCdFront(int newStatemp) throws VirtualCarException {
        if (propertyManager != null) {

            propertyManager.setValue( VirtualCarHvacManager.ID_ZONED_HVAC_POWER_ON, 1,newStatemp);
        }

    }

    //小憩模式
    public Object getSeatRest() throws VirtualCarException {
        Object wheelheat = 0;
        if (propertyManager != null) {
            wheelheat = propertyManager.getValue(VirtualCarCabinManager.ID_HVAC_SCENE_MODE_ACT, 0);
        }
        return wheelheat;
    }

    public void setSeatRest(int newStatemp) throws VirtualCarException {
        if (propertyManager != null) {

            propertyManager.setValue(VirtualCarCabinManager.ID_HVAC_SCENE_MODE_ACT, 0,newStatemp);
        }

    }

    //后视镜
    public Object getRearViewMirrorFold() throws VirtualCarException {
        Object wheelheat = 0;
        if (propertyManager != null) {
            wheelheat = propertyManager.getValue( VirtualCarCabinManager.ID_BACK_MIRROR_SW, 0);
        }
        return wheelheat;
    }

    public void setRearViewMirrorFold(int newStatemp) throws VirtualCarException {
        if (propertyManager != null) {

            propertyManager.setValue( VirtualCarCabinManager.ID_BACK_MIRROR_SW, 0,newStatemp);
        }

    }

    //二排安全带未系报警提示音
    public Object getSeatBeltCheck() throws VirtualCarException {
        Object wheelheat = 0;
        if (propertyManager != null) {
            wheelheat = propertyManager.getValue( VirtualCarCabinManager.ID_REAR_SEATBELT_WARNING_SW, 0);
        }
        return wheelheat;
    }

    public void setSeatBeltCheck(int newStatemp) throws VirtualCarException {
        if (propertyManager != null) {

            propertyManager.setValue( VirtualCarCabinManager.ID_REAR_SEATBELT_WARNING_SW, 0,newStatemp);
        }

    }

    //雨刮模式 1是前2是后
    public Object getWiper(int type) throws VirtualCarException {
        Object wheelheat = 0;
        if (propertyManager != null) {
            wheelheat = propertyManager.getValue( VirtualCarCabinManager.ID_WIPER_ADJUST_MODE, type);
        }
        return wheelheat;
    }

    public void setWiper(int type,int newStatemp) throws VirtualCarException {
        if (propertyManager != null) {

            propertyManager.setValue( VirtualCarCabinManager.ID_WIPER_ADJUST_MODE, type,newStatemp);
        }

    }

    //老板键
    public Object getHorizontalPositionSts() throws VirtualCarException {
        Object wheelheat = 0;
        if (propertyManager != null) {
            wheelheat = propertyManager.getValue( VirtualCarCabinManager.ID_BODY_SEAT_STATUS, 0);
        }
        return wheelheat;
    }

    public void setHorizontalPositionSts(int newStatemp) throws VirtualCarException {
        if (propertyManager != null) {

            propertyManager.setValue( VirtualCarCabinManager.ID_BODY_SEAT_STATUS, 0,newStatemp);
        }

    }

    //座椅通风 1是主驾4是副驾
    public Object getSeatVentilation(int type) throws VirtualCarException {
        Object wheelheat = 0;
        if (propertyManager != null) {
            wheelheat = propertyManager.getValue( VirtualCarHvacManager.ID_ZONED_HVAC_SEAT_VENTILATION, type);
        }
        return wheelheat;
    }

    public void setSeatVentilation(int type,int newStatemp) throws VirtualCarException {
        if (propertyManager != null) {

            propertyManager.setValue( VirtualCarHvacManager.ID_ZONED_HVAC_SEAT_VENTILATION, type,newStatemp);
        }

    }

    //低速行人报警音
    public Object getLowSpeedPedestrianAlarm() throws VirtualCarException {
        Object wheelheat = 0;
        if (propertyManager != null) {
            wheelheat = propertyManager.getValue( VirtualCarCabinManager.ID_IP_CLUSTER_SOUND_EFFECT_SWITCH, 3);
        }
        return wheelheat;
    }

    public void setLowSpeedPedestrianAlarm(int newStatemp) throws VirtualCarException {
        if (propertyManager != null) {

            propertyManager.setValue( VirtualCarCabinManager.ID_IP_CLUSTER_SOUND_EFFECT_SWITCH, 3,newStatemp);
        }

    }
}
