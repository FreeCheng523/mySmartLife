package com.smartlife.tuotuotiemeijiacarimplibrary;



import android.content.Context;
import android.content.Intent;

import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.mega.nexus.os.MegaSystemProperties;


import java.util.Arrays;



import mega.car.MegaCarProperty;
import mega.car.config.Driving;

import mega.car.hardware.CarPropertyValue;
import mega.log.MLog;

public class DriveModelManager {
    private static final String TAG = DriveModelManager.class.getSimpleName();
    public static final int ID_EXCLUSIVE = 4;
    public static boolean sIsEve = MegaSystemProperties.getInt("ro.hardware.ecu.config.POWER_MODE",0) == 0X1;
    public static final int[] DRV_STANDARD_DETAIL = new int[]{1, 1, 1, 1};
    public static final int[] DRV_SPORT_DETAIL = new int[]{2, 2, 2, 2};
    public static final int[] DRV_ECO_DETAIL = new int[]{1, 2, 2, 2};


    public static final boolean SDK_VERSION_R = Build.VERSION.SDK_INT ==
            Build.VERSION_CODES.R;
    /**
     * 车型读取
     * 0x0=C385/C673 基础车
     * 0x1=C385-ICA2
     * 0x2=C385-ASE
     * 0x3=C385-MCA
     * 0x4=C673-ICA
     * 0x5=C385-ICA2-SVP
     * 0x6=C673-SVP
     * 0x7-0x20=预留
     * 0x21：673-6
     * 0x22：385-2
     * 0x23：D587G
     * 0x24：D587
     */
    public static final String PROP_ECU_CONFIG_C385_VEHICLE_TYPE =
            SDK_VERSION_R ? "ro.ecu.config.C385_VEHICLE_TYPE"
                    : "ro.hardware.ecu.config.C385_VEHICLE_TYPE";

    public static final String ELECTRIC_TAILGATE =
            SDK_VERSION_R ? "ro.ecu.config.ELECTRIC_TAILGATE"
                    : "ro.hardware.ecu.config.ELECTRIC_TAILGATE";

    public static final String POWER_MODE =
            SDK_VERSION_R ? "ro.ecu.config.POWER_MODE"
                    : "ro.hardware.ecu.config.POWER_MODE";
    /**
     *子车型
     * 0x0=无
     * 0x1=C673-5
     * 0x2=C385-ICA2-SVP-VAVE
     */
    public static final String PROP_ECU_CONFIG_SUB_VEHICLEMODEL =
            SDK_VERSION_R ? "ro.ecu.config.SUB_VEHICLEMODEL"
                    : "ro.hardware.ecu.config.SUB_VEHICLEMODEL";

    /**
     * DEFAULT_VALUE 格式说明:
     * 长度 1 + 5 + 1 + 5
     * 1 ： 代表记忆的驾驶模式
     * 5 ： 代表自定义模式，分别是加速、回收、转向、刹车、空调能耗
     * 1 :  代表是否有专属模式 ： 默认0没有， 1有，只有这个值=1，后面的专属模式才有效
     * 5 ： 代表专属模式，分别是加速、回收、转向、刹车、空调能耗
     */
    public static final String DEFAULT_VALUE = sIsEve ? "3,1,1,1,1,0,0,0,0,0,0,0"
            : "1,1,1,1,1,0,0,0,0,0,0,0";
    public static final int LENGTH_DEFAULT_VALUE = 12;

    public static final String KEY_DRIVE_MODE = "drive_model_carsettings";

    public static final String INTENT_ACTION = "com.mega.carsetting.drive.mode";
    public static final String INTENT_KEY_DRIVE_MODE = "drive_mode";
    public static final String PACKAGE_MEGA_CARSETTINGS = "com.mega.carsettings";


    public static int[] getExclusiveIntArray(@NonNull Context context) {
        String save = getSaveValue(context);
        return getIntArray(save, 6, 12);
    }

    private static int[] getIntArray(String save, int start, int end) {
        String[] split = save.split(",");
        int[] ret = new int[end - start];
        if (end <= split.length) {
            for (int i = 0; i < ret.length; i++) {
                ret[i] = Integer.parseInt(split[start + i]);
            }
        }
        return ret;

    }


    /**
     * 配置字
     */
    public static String getSettingString(String stringCode) {
        return  SDK_VERSION_R ? "ro.ecu.config."+stringCode
                : "ro.hardware.ecu.config."+stringCode;
    }


    /**
     * 获取配字置
     */
    public static int getModels(String stringCode) {
        return MegaSystemProperties.getInt(stringCode, 0);
    }

    /**
     * 获取加速
     */
    public static int getAcc(Context context) {
        return Integer.parseInt(getSaveValue(context).split(",")[1]);
    }

    /**
     * 获取回收
     */
    public static int getRecycle(Context context) {
        return Integer.parseInt(getSaveValue(context).split(",")[2]);
    }

    /**
     * 获取转向
     */
    public static int getEps(Context context) {
        return Integer.parseInt(getSaveValue(context).split(",")[3]);
    }


    /**
     * 获取制动助力
     */
    public static int getEbp(Context context) {
        return Integer.parseInt(getSaveValue(context).split(",")[4]);
    }

    public static int getDriveMode() {
        return MegaCarProperty.getInstance().getIntProp(Driving.ID_DRV_MODE);
    }

    public static String getSaveValue(Context context) {
        String saveValue = Settings.Global.getString(context.getContentResolver(), KEY_DRIVE_MODE);
        if (checkValueInvalid(saveValue)) {
            saveValue = DEFAULT_VALUE;
        }
        //save(context, DEFAULT_VALUE);
        MLog.d(TAG, "getSaveValue = " + saveValue);
        return saveValue;
    }

    public static boolean checkValueInvalid(String saveValue) {
        if (saveValue == null) {
            return true;
        }
        String[] split = saveValue.split(",");
        boolean invalid = TextUtils.isEmpty(saveValue) || !saveValue.contains(",")
                || saveValue.length() < DEFAULT_VALUE.length()
                || saveValue.contains("[")
                || saveValue.contains("]")
                || saveValue.contains(" ")
                || split.length != LENGTH_DEFAULT_VALUE;
        MLog.d(TAG, "checkValueInvalid = " + saveValue + "  " + invalid);
        return invalid;
    }

    private static int[] getDriveModeExt(int mode, Context context) {
        int[] ret = new int[4];
        if (mode == 1) {
            ret = DRV_STANDARD_DETAIL;
        } else if (mode == 2) {
            ret = DRV_SPORT_DETAIL;
        } else if (mode == 3) {
            ret = DRV_ECO_DETAIL;
        } else if (mode == 4) {
            int[] ext = getExclusiveIntArray(context);
            if (ext[0] == 1) {
                System.arraycopy(ext, 1, ret, 0, ret.length);
            } else {
                return null;
            }
        } else if (mode == 0) {
            int accMode = getAcc(context);
            int rcyMode = getRecycle(context);
            int epsMode = getEps(context);
            int brkMode = getEbp(context);
            ret = new int[]{accMode, rcyMode, epsMode, brkMode};
        }
        return ret;
    }

    public static boolean setDriveMode(int mode, Context context) {
        CarPropertyValue<Integer> propertyValue = new CarPropertyValue<>(Driving.ID_DRV_MODE, mode);
        int[] ext = getDriveModeExt(mode, context);
        MLog.i("setDriveMode ext = = " + Arrays.toString(ext) + " mode = " + mode);
        if (ext != null) {
            propertyValue.setExtension(ext);
            MegaCarProperty.getInstance().setRawProp(propertyValue);
            return true;
        }
        setExclusiveMode(mode, context);
        return false;
    }

    public static boolean hasExclusiveMode(Context context) {
        int[] ext = getDriveModeExt(ID_EXCLUSIVE, context);
        return ext != null;
    }

    public static boolean setExclusiveMode(int mode, Context context) {
        Intent intent = new Intent(INTENT_ACTION);
        intent.putExtra(INTENT_KEY_DRIVE_MODE, mode);
        intent.setPackage(PACKAGE_MEGA_CARSETTINGS);
        context.startService(intent);
        return false;
    }

}