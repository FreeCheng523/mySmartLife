package com.smarlife.tuotiecarimpllibrary;

import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.property.CarPropertyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class CarPropertyUtils {
    private final String TAG = CarPropertyUtils.class.getSimpleName();
    private static CarPropertyUtils mInstance;
    private final List<Integer> mRegisterId = new ArrayList<>();
    private final List<ISpeedListener> mSpeedListener = new ArrayList<>();

    //车速ID
    public static final int ID_CAR_SPEED = 0x11600207;
    //横纵向加速度ID 
    public static final int ID_ESP_LAT_LONG_ACCEL =  0x21703442;

    //配置字
    public static final int ID_CAR_TYPE =0x2170f003;


    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
        }
    };

    public void registerSpeedListener(ISpeedListener speedListener) {
        if (speedListener != null && !mSpeedListener.contains(speedListener)) {
            mSpeedListener.add(speedListener);
        }

    }

    public void unregisterSpeedListener(ISpeedListener speedListener) {
        if (speedListener != null && mSpeedListener.contains(speedListener)) {
            mSpeedListener.remove(speedListener);
        }
    }

    /**
     * 通知车速变化值(在主线程回调)
     *
     * @param speed
     */
    private void notifySpeedChange(float speed) {
        if (mSpeedListener.size() > 0) {
            mHandler.post(() -> {
                for (ISpeedListener iSpeedListener : mSpeedListener) {
                    if (iSpeedListener != null) {
                        iSpeedListener.onSpeedChange(speed);
                    }
                }
            });
        }
    }

    /**
     * 通知车速值有效性(在主线程回调)
     *
     * @param isValid ture 有效 false 无效
     */
    private void notifySpeedValid(boolean isValid) {
        if (mSpeedListener.size() > 0) {
            mHandler.post(() -> {
                for (ISpeedListener iSpeedListener : mSpeedListener) {
                    if (iSpeedListener != null) {
                        iSpeedListener.onSpeedValid(isValid);
                    }
                }
            });
        }
    }

    interface ISpeedListener {
        void onSpeedChange(float speed);
        void onSpeedValid(boolean isValid);
    }

    /**
     * 获取车速值
     *
     * @return
     */
    public float getSpeed() {
        float speed = getFloatStatus(ID_CAR_SPEED, 0);
        Log.i(TAG, "getSpeed: speed=" + speed);
        return speed;
    }

    /**
     * 获取车的配置字
     *
     * @return
     */
    public byte[] getSettings() {
        byte[] speed = getFBaseStatus(ID_CAR_TYPE, 0);
        Log.i(TAG, "getSettings: byte=" + speed);
        return speed;
    }

    /**
     * 回调CAN 信号值变化
     */
    private final CarPropertyManager.CarPropertyEventListener mCarPropertyEventCallback =
            new CarPropertyManager.CarPropertyEventListener() {
                @Override
                public void onChangeEvent(CarPropertyValue carPropertyValue) {
                    if (carPropertyValue != null) {
						//value 注意判断值类型，再做强转
                        Object value = carPropertyValue.getValue();
                        int propertyId = carPropertyValue.getPropertyId();
                        int area = carPropertyValue.getAreaId();
                        if (value == null) {
                            Log.e(TAG, "carPropertyValue.getValue() = null");
                            return;
                        }
                        if (propertyId == ID_CAR_SPEED && area==0) {
                            //回调车速变化值
                            float speed = (float) value;
//                            Log.i(TAG, "onChangeEvent: speed="+speed);
                            notifySpeedChange(speed);
                        }else if(propertyId == ID_ESP_LAT_LONG_ACCEL && area==0){
							// if(value instanceof byte[])
							byte[] bytes= (byte[])value;
						//bytes[0] 横向加速度  
						// bytes[1] 高位  bytes[2] 低位 纵向加速度 

                        }
                    }
                }
                @Override
                public void onErrorEvent(int i, int i1) {

                }
            };

    private Context mContext;
    private Car mCar;

    private CarPropertyManager propertyManager;

    /**
     * Car服务
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "car - onServiceConnected");
            try {
                Log.i(TAG, "正在给propertyManager付值");
                propertyManager = (CarPropertyManager) mCar.getCarManager(Car.PROPERTY_SERVICE);
                registerCarDataChangeCallBack(mRegisterId);
//                float speed = getSpeed();
////                notifySpeedChange(speed);
            if(propertyManager!=null)
                Log.i(TAG, "propertyManager已赋值");
            else
                Log.i(TAG, "propertyManager未赋值");
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "car - onServiceDisconnected");

            Log.i(TAG, "propertyManager赋值失败");
            mCar.disconnect();//断连
            mCar = Car.createCar(mContext, mConnection);//重新创建
            mCar.connect();//重连
        }
    };

    private CarPropertyUtils() {
        registerIds();
    }

    public static CarPropertyUtils getInstance() {
        if (mInstance == null) {
            mInstance = new CarPropertyUtils();
        }
        return mInstance;
    }

    public void connectCar(Context context) {
        mContext = context;
        connectCar();
    }

	//注册ID列表
    private void registerIds() {
        mRegisterId.add(ID_CAR_SPEED);//车速信号
        mRegisterId.add(ID_ESP_LAT_LONG_ACCEL);//车速值有效性
        Log.d(TAG, "注册配置字");
        mRegisterId.add(ID_CAR_TYPE);
    }
	
		//注册全部ID列表
    public void registerIds(List<Integer> ids) {
		mRegisterId.clear();//车速值有效性
        mRegisterId.addAll(ids);
       

    }

    /**
     * 连接Car服务
     */
    private void connectCar() {
        Log.i(TAG, "connectCar");
//        mCar = Car.createCar(mContext, null, 1000, mCarServiceLifecycleListener);//连接服务，负责压低音量
        mCar = Car.createCar(mContext, mConnection);//连接服务
        if (mCar != null) {//连接服务成功
            Log.i(TAG, "connectCar success");
            mCar.connect();
        } else {//注册监听失败，则重新注册
            Log.w(TAG, "connectCar false , so reconnect");
            mHandler.postDelayed(this::connectCar, 1000);
        }
    }

    /********************
     * 注册回调的ID
     * @param ids 监听的id
     * @return boolean
     */
    public boolean registerCarDataChangeCallBack(List<Integer> ids) {
        Log.i(TAG, "");
        if (mCar != null && mCar.isConnected()) {
            if (ids != null) {
                int len = ids.size();
                for (int i = 0; i < len; i++) {
                    if (propertyManager != null) {
                        try {
                            boolean isRegister = propertyManager.registerListener(mCarPropertyEventCallback, ids.get(i), 0.0f);
                            if (!isRegister) {
                                Log.w(TAG, "isRegister = false , " + ids.get(i));
                            }

                        } catch (CarNotConnectedException e) {
                            e.printStackTrace();
                        }

                    } else {
                        Log.w(TAG, "propertyManager == null , please check!!");
                    }
                }
                return true;
            }
        } else {
            Log.w(TAG, "register failed");
        }
        return false;
    }


    public void setIntValue(int id, int value) {
        setIntProperty(id, 0, value);
    }

    //下发信号，发送信号
    public void setIntProperty(int id, int area, int value) {
        try {
			if (mCar != null && mCar.isConnected()) {
            propertyManager.setIntProperty(id, area, value);
			}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**************************
     * 获取返回值为int类型的接口(VehiclePropertyType中的类型为int)
     * @param propId :填receive表格中的，Android中自定义Key
     * @param area  :填receive表格中的：Area value
     * @return int
     */
    public int getIntStatus(int propId, int area) {
        try {
            if (mCar != null && mCar.isConnected()) {
                return propertyManager.getIntProperty(propId, area);

            } else {
                Log.d(TAG, "[> get <]  get int failed");
            }
        } catch (CarNotConnectedException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**************************
     * 获取返回值为int类型的接口(VehiclePropertyType中的类型为int)
     * @param propId :填receive表格中的，Android中自定义Key
     * @param area  :填receive表格中的：Area value
     * @return int
     */
    public int[] getIntArray(int propId, int area) {
        try {
            if (mCar != null && mCar.isConnected()) {
                return propertyManager.getIntArrayProperty(propId, area);
            } else {
                Log.d(TAG, "[> get <]  get int failed");
            }
        } catch (CarNotConnectedException e) {
            e.printStackTrace();
        }
        return new int[]{0};
    }

    /**************************
     * 获取返回值为float类型的接口(VehiclePropertyType中的类型为float)
     * @param propId :填receive表格中的，Android中自定义Key
     * @param area  :填receive表格中的：Area value
     * @return float
     */
    public float getFloatStatus(int propId, int area) {
        try {
            if (mCar != null && mCar.isConnected()) {
                return propertyManager.getFloatProperty(propId, area);
            } else {
                Log.d(TAG, "[> get <]  get float failed");
            }
        } catch (CarNotConnectedException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public byte[] getFBaseStatus(int propId, int area) {
        try {
            Log.d(TAG, "准备读取配置字");

            if (mCar != null && mCar.isConnected()) {
                byte[] bytss =null;
                Log.d(TAG, "获取配置字");
                if (propertyManager != null) {
                    bytss = propertyManager.getBytesProperty(propId, area);
                    Log.d(TAG, "已获取配置字,返回数据");
                } else {
                    Log.i(TAG, "propertyManager为空");
                }
                return bytss;
            } else {
                Log.d(TAG, "[> get <]  get float failed");
            }
        } catch (CarNotConnectedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
