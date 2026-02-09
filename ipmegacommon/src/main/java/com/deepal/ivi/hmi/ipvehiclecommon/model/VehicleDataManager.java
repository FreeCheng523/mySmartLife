package com.deepal.ivi.hmi.ipvehiclecommon.model;

import static com.deepal.smartlifemiddleware.MegaPropertyId.BcmTurnIndcrLe;
import static com.deepal.smartlifemiddleware.MegaPropertyId.BcmTurnIndcrRi;
import static com.deepal.smartlifemiddleware.MegaPropertyId.CdcDrvrModSetSts;
import static com.deepal.smartlifemiddleware.MegaPropertyId.CdcResiSocDisp;
import static com.deepal.smartlifemiddleware.MegaPropertyId.EspVehSpd;
import static com.deepal.smartlifemiddleware.MegaPropertyId.TpmsLeFrntTireP;
import static com.deepal.smartlifemiddleware.MegaPropertyId.TpmsLeReTireP;
import static com.deepal.smartlifemiddleware.MegaPropertyId.TpmsRiFrntTire;
import static com.deepal.smartlifemiddleware.MegaPropertyId.TpmsRiReTireP;
import static com.deepal.smartlifemiddleware.MegaPropertyId.VcuEnyMagtMod;
import static com.deepal.smartlifemiddleware.MegaPropertyId.VcuEvDrvResiMilg;
import static com.deepal.smartlifemiddleware.MegaPropertyId.VcuFuelLevelPercent;
import static com.deepal.smartlifemiddleware.MegaPropertyId.VcuFulLimdDrvgDst;
import static com.deepal.smartlifemiddleware.MegaPropertyId.VcuGearPosn;
import static com.deepal.smartlifemiddleware.MegaPropertyId.VcuRdySts;
import static com.deepal.smartlifemiddleware.MegaPropertyId.VcuVehAvrgEgyCnseLongTime;
import static com.deepal.smartlifemiddleware.MegaPropertyId.VcuVehEgyCnseSelectCfm;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;

import com.deepal.ivi.hmi.ipcommon.data.DispatchData;
import com.deepal.ivi.hmi.ipcommon.data.bean.MetaDataBean;
import com.deepal.ivi.hmi.ipcommon.data.bean.SetWrite;
import com.deepal.ivi.hmi.ipcommon.iInterface.DataCallBack;
import com.deepal.ivi.hmi.ipcommon.util.AndroidUtil;
import com.deepal.ivi.hmi.ipcommon.util.SettingsManager;
import com.deepal.smartlifemiddleware.MegaCarInfo;

import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import mega.car.hardware.CarPropertyValue;
import mega.car.hardware.property.CarPropertyManager;

public class VehicleDataManager {
    private static final String TAG = "VehicleDataManager";
    private volatile static MetaDataBean metaDataBean;
    private CopyOnWriteArrayList<DataCallBack> dataCallBackList;
    private static VehicleDataManager instance ;
    private boolean toIp = false;
    private Context mContext;
    private VehicleDataManager() {
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
        try {
            this.mContext = mContext;
            initData();
            subScribleSignalValue();
        } catch (Exception e) {
            Log.e(TAG, "错误，init: "+ e);
        }
    }

    public void addDataCallBack(DataCallBack dataCallBack){
        dataCallBackList.add(dataCallBack);
    }


    private void initData()  {
        int carType  = AndroidUtil.convertValueToIp("carType",MegaCarInfo.INSTANCE.getPowerMode());                         //车辆类型
        int light = SettingsManager.getIntData(SetWrite.SET_LIGHT_VALUE,60);                                             //亮度
        int theme = SettingsManager.getIntData(SetWrite.SET_THEME,1);                                                                      // 主题，先默认设置为1吧
        int apperanceMode = SettingsManager.getIntData(SetWrite.SHOW_MODE,0);                        // 显示模式
        int hour = AndroidUtil.getTimeField("hour");                                // 时
        int minutes = AndroidUtil.getTimeField("minute");                           // 分
        int readyStatus =  MegaCarInfo.INSTANCE.getVcuRdySts();                     // ready状态
        int temperature = 25;                                                       // 主题，先默认设置为25吧
        int driveMode = MegaCarInfo.INSTANCE.getCdcDrvrModSetSts();                 //驾驶模式：舒适模式、经济模式
        int energyMode = MegaCarInfo.INSTANCE.getVcuEnyMagtMod();                   //能源模式：市区模式、高速模式
        int energyCnseSelect = AndroidUtil.convertValueToIp("energyType",
                MegaCarInfo.INSTANCE.getVcuVehEgyCnseSelectCfm());                   // 能耗选项设置:CLTC、WLTC
        float speed =   MegaCarInfo.INSTANCE.getEspVehSpd();                         // 速度
        float eleRemainMile = MegaCarInfo.INSTANCE.getVcuEvDrvResiMilg();           // 纯电剩余里程
        int eleRatio = MegaCarInfo.INSTANCE.getCdcResiSocDisp();                    //剩余电量
        float oilRemainMile = MegaCarInfo.INSTANCE.getVcuFulLimdDrvgDst();          // 燃油剩余里程
        int oilRatio = MegaCarInfo.INSTANCE.getVcuFuelLevelPercent();               //剩余油量
        int gear = MegaCarInfo.INSTANCE.getVcuGearPosn();                           // 挡位
        int turnLeft = MegaCarInfo.INSTANCE.getBcmTurnIndcrLe();                    //左转向灯
        int turnRight = MegaCarInfo.INSTANCE.getBcmTurnIndcrRi();                   //右转向灯
        Log.i(TAG, "当前车辆数据值: " +
                "\n 车辆类型: " + carType +
                "\n 车辆亮度: " + light +
                "\n 车辆主题: " + theme +
                "\n 显示模式: " + apperanceMode +
                "\n 当前时间: " + hour + ":" + minutes +
                "\n 准备状态: " + readyStatus +
                "\n 车辆温度: " + temperature +
                "\n 驾驶模式: " + driveMode +
                "\n 能源模式: " + energyMode +
                "\n 能耗选项: " + energyCnseSelect +
                "\n 车速: "    + speed +
                "\n 纯电剩余里程: " + eleRemainMile +
                "\n 剩余电量: " + eleRatio +
                "\n 燃油剩余里程: " + oilRemainMile +
                "\n 剩余油量: " + oilRatio +
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
        getLanguage();
        getUnit();
        Log.i(TAG, "初始化数据成功,当前MetaBean数据值："+metaDataBean.toString());
    }

    public MetaDataBean getMetaDataBean() {
        return metaDataBean;
    }


    private void subScribleSignalValue(){
        mContext.getContentResolver().registerContentObserver(Settings.System.CONTENT_URI,
                true,
                new ContentObserver(new Handler()) {
                    @Override
                    public void onChange(boolean selfChange) {
                        super.onChange(selfChange);
                        getLanguage();
                        getUnit();
                        setToIp();
                    }

                    @Override
                    public void onChange(boolean selfChange, @Nullable Uri uri) {
                        super.onChange(selfChange, uri);
                        getLanguage();
                        getUnit();
                        setToIp();
                    }
                });
        MegaCarInfo.INSTANCE.registerSignal(new CarPropertyManager.CarPropertyEventCallback() {
            @Override
            public void onChangeEvent(CarPropertyValue carPropertyValue) {
                if (carPropertyValue != null) {
                    int propertyId = carPropertyValue.getPropertyId();
                    Object value = carPropertyValue.getValue();
                    String propertyName = "propertyName";
                    switch (propertyId) {
                        case VcuEnyMagtMod: //监听：√
                            propertyName = "能量管理模式反馈";
                            metaDataBean.setEnergerMagMode((int) value);
                            toIp = true;
                            break;
                        case VcuEvDrvResiMilg://监听：√
                            propertyName = "纯电剩余里程";
                            metaDataBean.setEvMileage((float) value);
                            toIp = true;
                            break;
                        case VcuFulLimdDrvgDst:
                            propertyName = "燃油续驶里程";
                            metaDataBean.setOilMileage((float) value);
                            toIp = true;
                            break;
                        case VcuVehAvrgEgyCnseLongTime://监听：√
                            propertyName = "长期平均能耗";
                            break;
                        case EspVehSpd: //监听：√
                            propertyName = "车速信号";
                            metaDataBean.setSpeed((float) value);
                            toIp = true;
                            break;
                        case BcmTurnIndcrLe://监听：√
                            propertyName = "左转向灯工作状态";
                            metaDataBean.setLeftLightStatus((Integer) value);
                            toIp = true;
                            break;
                        case BcmTurnIndcrRi://监听：√
                            propertyName = "右转向灯工作状态";
                            metaDataBean.setRightLightStatus((Integer) value);
                            toIp = true;
                            break;
                        case VcuRdySts: //监听：√
                            propertyName = "整车可行驶状态";
                            metaDataBean.setVcuRdySts((Integer) value);
                            toIp = true;
                            break;
                        case VcuGearPosn://监听：√
                            // 挡位处理逻辑
                            int gearValue = (int) value;
                            metaDataBean.setVehicleGear(gearValue);
                            String gearStr;
                            toIp = true;
                            switch (gearValue) {
                                case 0: gearStr = "停车档"; break;
                                case 1: gearStr = "倒挡"; break;
                                case 2: gearStr = "N挡"; break;
                                case 3: gearStr = "D档"; break;
                                case 4: gearStr = "S档"; break;
                                default: gearStr = "无效"; break;
                            }
                            Log.i(TAG, "档位变化: gearValue" + gearValue + ", gearStr: " + gearStr);
                            break;
                        case TpmsLeFrntTireP:
                            propertyName = "左前轮胎压力";
                            break;
                        case TpmsRiFrntTire:
                            propertyName = "右前轮胎压力";
                            break;
                        case TpmsRiReTireP:
                            propertyName = "右后轮胎压力";
                            break;
                        case TpmsLeReTireP:
                            propertyName = "左后轮胎压力";
                            break;
                        case CdcDrvrModSetSts://监听：√
                            propertyName = "驾驶模式设置状态";
                            metaDataBean.setDriverMode((Integer) value);
                            toIp = true;
                            break;
                        case CdcResiSocDisp://监听：√
                            propertyName = "电量进度条比例";
                            metaDataBean.setPowerBarRatio((Integer) value);
                            toIp = true;
                            break;
                        case VcuFuelLevelPercent:
                            propertyName = "油量百分比";
                            metaDataBean.setOilPercent((Integer) value);
                            toIp = true;
                            break;
                        case VcuVehEgyCnseSelectCfm://监听：√
                            propertyName = "能耗选项设置结果";
                            metaDataBean.setEnergyOption((Integer) value);
                            toIp = true;
                            break;
                    }
                    Log.d(TAG, "propertyName: " + propertyName + ", value: " + value);
                    Log.d(TAG, "更新的数据："+metaDataBean.toString());
                    if (toIp){
                        setToIp();
                        toIp = false;
                    }
                }
            }

            @Override
            public void onErrorEvent(int i, int i1) {}
        },null);   // null表示不添加额外 propertyId，使用已有的 mSignalIdSet
    }

    public void setToIp() {
        for (DataCallBack dataCallBack : dataCallBackList){
            dataCallBack.onDataChange(new DispatchData(metaDataBean, "MeterData"));
        }
    }
    public int getUnit() {
        String language = Locale.getDefault().getLanguage();
        int res = language.equals("zh")? 0x01: 0x00;
        Log.i(TAG, "getUnit: " + res);
        metaDataBean.setUnit(res);
        return res;
    }

    public int getLanguage() {
        int language = 0; // 默认值为英文 0x00
        Locale defaultLocale = Locale.getDefault();
        String languageCode = defaultLocale.getLanguage();
        Log.i(TAG, "当前语言 languageCode: " + languageCode);
        String countryCode = defaultLocale.getCountry();
        switch (languageCode) {
            case "fr": // 法语
                language = 0x01;
                break;
            case "de": // 德语
                language = 0x02;
                break;
            case "it": // 意大利语
                language = 0x03;
                break;
            case "es": // 西班牙语
                language = 0x04;
                break;
            case "nl": // 荷兰语
                language = 0x05;
                break;
            case "sv": // 瑞典语
                language = 0x06;
                break;
            case "da": // 丹麦语
                language = 0x07;
                break;
            case "no": // 挪威语
                language = 0x08;
                break;
            case "pl": // 波兰语
                language = 0x09;
                break;
            case "pt": // 葡萄牙语
                language = 0x0A;
                break;
            case "zh": // 中文
                // 区分简体中文和繁体中文
                if ("TW".equals(countryCode) || "HK".equals(countryCode) || "MO".equals(countryCode)) {
                    language = 0x0C; // 中文繁体
                } else {
                    language = 0x0B; // 中文简体
                }
                break;
            case "th": // 泰语
                language = 0x0D;
                break;
            case "id": // 印尼语
                language = 0x0E;
                break;
            case "ms": // 马来语
                language = 0x0F;
                break;
            case "ar": // 阿拉伯语
                language = 0x10;
                break;
            case "ru": // 俄语
                language = 0x11;
                break;
            case "vi": // 越南语
                language = 0x12;
                break;
            case "he": // 希伯来语
                language = 0x13;
                break;
            case "el": // 希腊语
                language = 0x14;
                break;
            case "hu": // 匈牙利语
                language = 0x15;
                break;
            case "tr": // 土耳其语
                language = 0x16;
                break;
            case "fi": // 芬兰语
                language = 0x17;
                break;
            case "ro": // 罗马尼亚语
                language = 0x18;
                break;
            case "cs": // 捷克语
                language = 0x19;
                break;
            default: // 0x1A-0xFF 视为无效值
                language = 0; // 默认英文
                break;
        }
        metaDataBean.setLanguage(language);
        return language;
    }
}
