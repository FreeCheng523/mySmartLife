package com.deepal.ivi.hmi.ipvehiclecommon.viewmode;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.deepal.ivi.hmi.ipcommon.connection.NetworkServiceManager;
import com.deepal.ivi.hmi.ipcommon.data.DispatchData;
import com.deepal.ivi.hmi.ipcommon.data.bean.MetaDataBean;
import com.deepal.ivi.hmi.ipcommon.data.bean.MeterBasicDataBean;
import com.deepal.ivi.hmi.ipcommon.iInterface.DataCallBack;
import com.deepal.ivi.hmi.ipcommon.iInterface.ServerCallback;
import com.deepal.ivi.hmi.ipcommon.util.AndroidUtil;
import com.deepal.ivi.hmi.ipcommon.util.MeterDataBuild;
import com.deepal.ivi.hmi.ipvehiclecommon.model.VehicleDataManager;

import java.io.IOException;
import java.time.LocalTime;

/**
 * tinnove
 */
public class InstrumentPanelViewModel extends ViewModel implements DataCallBack {
    private static final String TAG = "InstrumentPanelViewModel";
    private MetaDataBean metaDataBean ;
    public final MutableLiveData<Integer> isClientConnected = new MutableLiveData<>();
    private MeterDataBuild builder = MeterDataBuild.INSTANCE;
    private byte [] dataToSend ;
    public MutableLiveData<String> showContent = new MutableLiveData<>();

    private final int SERVER_PORT = 10007;
    private final String SERVER_IP = "192.168.2.101";
    private int light = 0;
    public MutableLiveData<Integer> lightLiveData = new MutableLiveData<>();
    private int theme = 0;
    private int apperMode = 0;
    private boolean metaDataBeanChanged = false;
    // 网络服务管理器
    private NetworkServiceManager networkService;
    private boolean isUpdateDataActive;
    public InstrumentPanelViewModel() {
        networkService = NetworkServiceManager.getInstance(SERVER_IP, SERVER_PORT);
        VehicleDataManager.getInstance().addDataCallBack(this);
    }

    private ServerCallback serverCallbackInstance = new ServerCallback() {
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
    };

    public void init() {
        Log.i(TAG, "init");
        networkService.setServerCallback(serverCallbackInstance);
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
                //loopBuildUpdateData();
                loopUpdateData();
            } catch (InterruptedException | IOException e) {
                Log.e(TAG, "更新数据循环异常", e);
            }
        }
    }


    public void setIpInitData(){
        metaDataBean = VehicleDataManager.getInstance().getMetaDataBean();
        Log.i(TAG, "初始化IP的数据:"+metaDataBean.toString());
        dataToSend = builder.buildDomesticInitData(
                metaDataBean.getCarType(),
                metaDataBean.getLight(),
                metaDataBean.getTheme(),
                metaDataBean.getMode(),
                metaDataBean.getHour(),
                metaDataBean.getMinute(),
                metaDataBean.getVcuRdySts(),
                metaDataBean.getTemperature(),
                metaDataBean.getDriverMode(),
                metaDataBean.getEnergerMagMode(),
                metaDataBean.getEnergyOption(),
                Math.round(metaDataBean.getSpeed()),
                Math.round(metaDataBean.getEvMileage()) ,
                metaDataBean.getPowerBarRatio(),
                Math.round(metaDataBean.getOilMileage()),
                metaDataBean.getOilPercent(),
                metaDataBean.getVehicleGear(),
                metaDataBean.getLeftLightStatus(),
                metaDataBean.getRightLightStatus());
        sendDataToIP();
    }

    public void setTheme(int value){
        this.theme = value;
        dataToSend = builder.buildSetTheme(value);
        sendDataToIP();
    }

    public void setLight(int operateValue, String operateType) {
        Log.i(TAG, "currLight:" +this.light+",setLight,operateValue: " + operateType);
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
        if (metaDataBean != null) {
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
        } else {
            Log.e(TAG, "数据未初始化完成");
        }
    }

    public void loopUpdateData() throws InterruptedException, IOException {
        isUpdateDataActive = true;
        if (metaDataBean == null) {
            metaDataBean = VehicleDataManager.getInstance().getMetaDataBean();
        }
        while (isUpdateDataActive) {
            Log.i(TAG, "当前车辆数据值: " +
                    "\n 车辆类型: " + metaDataBean.getCarType() +
                    "\n 当前时间: " + LocalTime.now().getHour() + ":" +  LocalTime.now().getMinute() +
                    "\n 准备状态: " + metaDataBean.getVcuRdySts() +
                    "\n 温度: " + metaDataBean.getTemperature() +
                    "\n 驾驶模式: " + metaDataBean.getDriverMode() +
                    "\n 能源模式: " + metaDataBean.getEnergerMagMode() +
                    "\n 能耗选项: " + metaDataBean.getEnergyOption() +
                    "\n 车速: " + metaDataBean.getSpeed() +
                    "\n 纯电剩余里程: " + metaDataBean.getEvMileage() +
                    "\n 剩余电量: " + metaDataBean.getPowerBarRatio() +
                    "\n 燃油剩余里程: " + metaDataBean.getOilMileage() +
                    "\n 剩余油量: " + metaDataBean.getOilPercent() +
                    "\n 档位: " + metaDataBean.getVehicleGear() +
                    "\n 左转向灯: " + metaDataBean.getLeftLightStatus() +
                    "\n 右转向灯: " + metaDataBean.getRightLightStatus());
            dataToSend = MeterDataBuild.INSTANCE.buildDomesticUpdateData(
                    metaDataBean.getCarType(), LocalTime.now().getHour(), LocalTime.now().getMinute(),
                    metaDataBean.getVcuRdySts() , metaDataBean.getTemperature(), metaDataBean.getDriverMode(),
                    metaDataBean.getEnergerMagMode(),metaDataBean.getEnergyOption(), (int)(metaDataBean.getSpeed()),
                    Math.round(metaDataBean.getEvMileage()), metaDataBean.getPowerBarRatio(), Math.round(metaDataBean.getOilMileage()),
                    metaDataBean.getOilPercent(), metaDataBean.getVehicleGear(),
                    metaDataBean.getLeftLightStatus(), metaDataBean.getRightLightStatus());
            sendDataToIP();
            Thread.sleep(35);
        }

    }

    @Override
    public void onDataChange(DispatchData dispatchData) {
        if (null != dispatchData){
            metaDataBean = dispatchData.getData();
            Log.d(TAG, "onDataChange: MeterData= "+metaDataBean.toString());
            switch (dispatchData.getDataType()){
                case "apperMode":
                    setMode(metaDataBean.getMode());
                    break;
                case "light":
                    setLight(metaDataBean.getLight() ,"auto");
                    break;
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
    @Override
    protected void onCleared() {
        super.onCleared();
    }

    public void updateRecoValue(boolean b) {
        Log.i(TAG, "重连 updateRecoValue: " + b);
        if (networkService != null) {
            networkService.isToReconnect(b);
        }
    }
}
