package com.deepal.ivi.hmi.ipvehiclecommon.viewmode;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.deepal.ivi.hmi.ipcommon.connection.NetworkServiceManager;
import com.deepal.ivi.hmi.ipcommon.data.DispatchData;
import com.deepal.ivi.hmi.ipcommon.data.bean.MetaDataBean;
import com.deepal.ivi.hmi.ipcommon.data.bean.MeterBasicDataBean;
import com.deepal.ivi.hmi.ipcommon.data.bean.SetWrite;
import com.deepal.ivi.hmi.ipcommon.iInterface.DataCallBack;
import com.deepal.ivi.hmi.ipcommon.iInterface.ServerCallback;
import com.deepal.ivi.hmi.ipcommon.util.SettingsManager;
import com.deepal.ivi.hmi.ipvehiclecommon.IApplication;
import com.deepal.ivi.hmi.ipvehiclecommon.R;
import com.deepal.ivi.hmi.ipvehiclecommon.model.VehicleDataManager;
import com.deepal.ivi.hmi.ipcommon.util.AndroidUtil;
import com.deepal.ivi.hmi.ipcommon.util.MeterDataBuild;

import java.io.IOException;
import java.time.LocalTime;


public class InstrumentPanelViewModel extends ViewModel implements DataCallBack {
    private static final String TAG = "InstrumentPanelViewModel";
    private MetaDataBean metaDataBean ;
    public final MutableLiveData<Integer> isClientConnected = new MutableLiveData<>();
    private MeterDataBuild builder = MeterDataBuild.INSTANCE;
    private byte [] dataToSend ;
    public MutableLiveData<MetaDataBean> metaDataLiveData = new MutableLiveData<>();
    public MutableLiveData<String> showContent = new MutableLiveData<>();

    private final int SERVER_PORT = 10007;
    private final String SERVER_IP = "192.168.2.101";
    private int light = 0;
    public MutableLiveData<Integer> lightLiveData = new MutableLiveData<>();
    private final String carSeries = IApplication.getInstance().getApplicationContext().getString(R.string.currentVehicleType);
    private boolean isDomestic;
    private int theme = 0;
    private int apperMode = 0;

    // 添加网络服务管理器
    private NetworkServiceManager networkService;
    private boolean isUpdateDataActive;
    private boolean initServerCallBack;
    public InstrumentPanelViewModel() {
        networkService = NetworkServiceManager.getInstance(SERVER_IP, SERVER_PORT);
        VehicleDataManager.getInstance().addDataCallBack(this);
        isDomestic = !((carSeries.split("_"))[2]).contains("g");
        Log.i(TAG, "车型："+ carSeries+",isDomestic : " + isDomestic);
    }


    public void init() {
        Log.i(TAG, "init");
        if (!initServerCallBack){
            networkService.setServerCallback(new ServerCallback() {
                @Override
                public void connectClientStatus(boolean status,String reason) {
                    Log.d(TAG, "仪表连接状态:Client connection status: " + status);
                    isClientConnected.postValue(status?1:0);
                    showContent.postValue(status ? "客户端已连接" : "客户端断开连接,原因是：" + reason) ;
                }
                @Override
                public void receiveMsgFromClient(byte[] data) {
                    handleReceivedData(data);
                }
            });
            initServerCallBack = true;
        }else {
            Log.i(TAG,"ViewModel 已经初始化，不再初始化");
        }
    }

    public void startServer() {
        networkService.startServer();
        showContent.postValue("服务器启动，监听端口：" + SERVER_PORT);
    }

    // 处理接收到的数据
    private void handleReceivedData(byte[] receivedData) {
        String message = AndroidUtil.bytesToHex(receivedData);
        Log.d(TAG, "接收到客户端消息：" + message);
        showContent.postValue("收到：" + message);

        MeterBasicDataBean basicBean = MeterDataBuild.INSTANCE.dispatchAnalyzeData(receivedData);
        if (basicBean.getStatus() == 2) {
            setIpInitData();
        }
        if (basicBean.getStatus() == 3) {
            try {
                loopBuildUpdateData();
            } catch (InterruptedException | IOException e) {
                Log.e(TAG, "更新数据循环异常", e);
            }
        }
    }

    public void setIpInitData(){
        metaDataBean = VehicleDataManager.getInstance().getMetaDataBean();
        Log.i(TAG, "当前初始化的车辆数据值: "+metaDataBean.toString());
        int light = SettingsManager.getIntData(SetWrite.SET_LIGHT_VALUE,60);                                             //亮度
        int theme = SettingsManager.getIntData(SetWrite.SET_THEME,1);                                                                      // 主题，先默认设置为1吧
        int apperanceMode = SettingsManager.getIntData(SetWrite.SHOW_MODE,0);
        Log.i(TAG,"当前存储的亮度:"+light+",主题："+theme+",模式值为："+apperanceMode);
        int speed =  Math.round(metaDataBean.getSpeed());
        int eleRemainMile = Math.round(metaDataBean.getEvMileage());           // 纯电剩余里程
        int oilRemainMile = Math.round(metaDataBean.getOilMileage());          // 燃油剩余里程
        if (isDomestic){
            dataToSend = builder.buildDomesticInitData(
                    metaDataBean.getCarType(),
                    light,
                    theme,
                    apperanceMode,
                    metaDataBean.getHour(),
                    metaDataBean.getMinute(),
                    metaDataBean.getVcuRdySts(),
                    metaDataBean.getTemperature(),
                    metaDataBean.getDriverMode(),
                    metaDataBean.getEnergerMagMode(),
                    metaDataBean.getEnergyOption(),
                    speed,
                    eleRemainMile,
                    metaDataBean.getPowerBarRatio(),
                    oilRemainMile,
                    metaDataBean.getOilPercent(),
                    metaDataBean.getVehicleGear(),
                    metaDataBean.getLeftLightStatus(),
                    metaDataBean.getRightLightStatus());
        }else {
            dataToSend = builder.buildGlobalInitData(
                    metaDataBean.getCarType(),
                    metaDataBean.getLight(),
                    metaDataBean.getTheme(),
                    metaDataBean.getLanguage(),
                    metaDataBean.getUnit(),
                    metaDataBean.getMode(),
                    metaDataBean.getHour(),
                    metaDataBean.getMinute(),
                    metaDataBean.getVcuRdySts(),
                    metaDataBean.getTemperature(),
                    metaDataBean.getDriverMode(),
                    metaDataBean.getEnergerMagMode(),
                    metaDataBean.getEnergyOption(),
                    speed,
                    eleRemainMile,
                    metaDataBean.getPowerBarRatio(),
                    oilRemainMile,
                    metaDataBean.getOilPercent(),
                    metaDataBean.getVehicleGear(),
                    metaDataBean.getLeftLightStatus(),
                    metaDataBean.getRightLightStatus());
        }
        sendDataToIP();
    }

    public void setTheme(int value){
        this.theme = value;
        dataToSend = builder.buildSetTheme(value);
        sendDataToIP();
    }

    public void setLight(int operateValue, String operateType) {
        Log.i(TAG, "currLight:+" +this.light+",setLight,operateValue: " + operateType);
        switch (operateType){
            case "sub":{
                if (this.light-operateValue<=0){
                    this.light = 20;
                }else {
                    this.light = AndroidUtil.setLightNormal(light-operateValue);
                }
                break;
            }case "add":{
                if (this.light+operateValue>=101){
                    this.light = 100;
                }else {
                    this.light = AndroidUtil.setLightNormal(light+operateValue);
                }
                break;
            } case "auto":{
                this.light = operateValue;
                break;
            }
        }
        this.light = AndroidUtil.getNearestLevel(this.light);
        lightLiveData.setValue(this.light);
        dataToSend = builder.buildSetLight(this.light);
        sendDataToIP();
    }

    public void setMode(int apperMode) {
        Log.i(TAG, "显示模式 setMode: " + apperMode);
        this.apperMode = apperMode;
        dataToSend = builder.buildSetMode(apperMode);
        sendDataToIP();
    }

    public void sendDataToIP(){
        Log.d(TAG, "发送数据：" + AndroidUtil.bytesToHex(dataToSend));
        networkService.sendData(dataToSend, new NetworkServiceManager.SendCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "数据发送成功");
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "数据发送失败", e);
            }
        });
    }


    public void loopBuildUpdateData() throws InterruptedException, IOException {
        if (metaDataBean == null){
            metaDataBean = VehicleDataManager.getInstance().getMetaDataBean();
        }
        isUpdateDataActive = true;
        while (isUpdateDataActive) {
            Log.i(TAG, "当前车辆数据值: " + metaDataBean.toString());
            int carType = metaDataBean.getCarType();           //这里不要再嗲用AndroidUtil来转，否则又转回去了
            int hour = LocalTime.now().getHour();
            int minute = LocalTime.now().getMinute();
            int energyCnseSelect = AndroidUtil.convertValueToIp("energyType",
                    metaDataBean.getEnergyOption());                              // 能耗选项设置:CLTC、WLTC
            int speed = Math.round(metaDataBean.getSpeed());                           //车速
            int eleRemainMile = Math.round(metaDataBean.getEvMileage());             //纯电剩余里程
            int oilRemainMile = Math.round(metaDataBean.getOilMileage());            //燃油剩余里程
            if (isDomestic){
                dataToSend = MeterDataBuild.INSTANCE.buildDomesticUpdateData(
                        carType,
                        hour,
                        minute,
                        metaDataBean.getVcuRdySts(),
                        metaDataBean.getTemperature(),
                        metaDataBean.getDriverMode(),
                        metaDataBean.getEnergerMagMode(),
                        energyCnseSelect,
                        speed,
                        eleRemainMile,
                        metaDataBean.getPowerBarRatio(),
                        oilRemainMile,
                        metaDataBean.getOilPercent(),
                        metaDataBean.getVehicleGear(),
                        metaDataBean.getLeftLightStatus(),
                        metaDataBean.getRightLightStatus());
            }else {
                dataToSend = MeterDataBuild.INSTANCE.buildGlobalUpdateData(
                        carType,
                        metaDataBean.getLanguage(),
                        metaDataBean.getUnit(),
                        hour,
                        minute,
                        metaDataBean.getVcuRdySts(),
                        metaDataBean.getTemperature(),
                        metaDataBean.getDriverMode(),
                        metaDataBean.getEnergerMagMode(),
                        energyCnseSelect,
                        speed,
                        eleRemainMile,
                        metaDataBean.getPowerBarRatio(),
                        oilRemainMile,
                        metaDataBean.getOilPercent(),
                        metaDataBean.getVehicleGear(),
                        metaDataBean.getLeftLightStatus(),
                        metaDataBean.getRightLightStatus());
            }
            sendDataToIP();
            Thread.sleep(40);
        }
    }

    @Override
    public void onDataChange(DispatchData dispatchData) {
        if (null != dispatchData){
            switch (dispatchData.getDataType()){
                case "MeterData":
                    metaDataBean = dispatchData.getData();
                    Log.d(TAG, "onDataChange: MeterData");
                    break;
                case "light":
                    this.light = dispatchData.getData();
                    setLight(this.light ,"auto");
                    Log.i(TAG, "onDataChange,light : "+this.light);
            }
        }

    }
    /**
     * 停止网络服务
     */
    public void stopServer() {
        if (networkService != null) {
            networkService.stopServer();
        }
        isUpdateDataActive = false;
    }
    public void updateRecoValue(boolean b) {
        Log.i(TAG, "重连 updateRecoValue: " + b);
        if (networkService != null) {
            networkService.isToReconnect(b);
        }
    }
    @Override
    protected void onCleared() {
        super.onCleared();
    }

}
