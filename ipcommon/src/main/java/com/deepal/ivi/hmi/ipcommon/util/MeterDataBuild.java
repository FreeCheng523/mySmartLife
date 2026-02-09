package com.deepal.ivi.hmi.ipcommon.util;




import com.deepal.ivi.hmi.ipcommon.data.bean.MeterBasicDataBean;
import com.deepal.ivi.hmi.ipcommon.data.bean.MeterUpgradeBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MeterDataBuild {
    public static final MeterDataBuild INSTANCE = new MeterDataBuild();


    // 构建初始化数据包-国内
    public byte[] buildDomesticInitData(int carType, int light, int theme, int mode, int hour, int minute,
                                int readyStatus, int temperature, int driverMode, int drvMode,
                                int dis, int speed, int eleDriver, int eleRatio, int oilDriver,
                                int oilRatio, int gear, int turnLeft, int turnRight) {
        byte lightByte;
        if (light >= 0 && light < 21) {
            lightByte = 0;
        } else {
            if (21 <= light && light < 41) {
                lightByte = 1;
            } else {
                if (61 <= light && light < 81) {
                    lightByte = 3;
                } else {
                    lightByte = 81 <= light && light < 101 ? (byte) 4 : (byte) 2;
                }
            }
        }

        byte themeByte;
        switch (theme) {
            case 1:
                themeByte = 1;
                break;
            case 2:
                themeByte = 2;
                break;
            default:
                themeByte = 0;
                break;
        }

        byte modeByte = mode == 1 ? (byte) 1 : (byte) 0;

        byte turnBytes = (turnLeft == 2 && turnRight == 1) ?
                (byte) 1 : (turnLeft == 1 && turnRight == 2) ? (byte) 2 :
                (turnLeft == 2 && turnRight == 2) ? (byte) 3 : (byte) 0;

        int newTotalDriver;
        byte[] eleDriverByte = {0, 0};
        byte[] oilDriverByte = {0, 0};
        switch (carType) {
            case 0:
                eleDriverByte = intToByteArrayLittleEndian(eleDriver);
                oilDriverByte = intToByteArrayLittleEndian(oilDriver);
                int newTotalDriver2 = eleDriver + oilDriver;
                newTotalDriver = newTotalDriver2;
                break;
            case 1:
                eleDriverByte = intToByteArrayLittleEndian(eleDriver);
                newTotalDriver = eleDriver;
                break;
            default:
                newTotalDriver = 0;
                break;
        }
        byte[] totalDriverByte = intToByteArrayLittleEndian(newTotalDriver);

        byte[] speedByte = intToByteArrayLittleEndian(speed);

        byte[] bodyBytes = {
                2, 2, 23,
                (byte) carType,
                themeByte,
                modeByte,
                lightByte,
                (byte) readyStatus,
                (byte) gear,
                (byte) hour,
                (byte) minute,
                (byte) dis,
                turnBytes,
                (byte) driverMode,
                (byte) drvMode,
                (byte) temperature,
                (byte) eleRatio,
                (byte) oilRatio,
                eleDriverByte[0],
                eleDriverByte[1],
                oilDriverByte[0],
                oilDriverByte[1],
                totalDriverByte[0],
                totalDriverByte[1],
                speedByte[0],
                speedByte[1]};
        return completeBytes(bodyBytes);
    }

    // 构建初始化数据包-国内
    public  byte[] buildDomesticUpdateData(int carType, int hour, int minute, int readyStatus,
                                   int temperature, int driverMode, int energyMode, int energyCnseSelect,
                                   int speed, int eleDriver, int eleRatio, int oilDriver,
                                   int oilRatio, int gear,int turnLeft, int turnRight) {
        //byte turnBytes = (turnLeft == 2 && turnRight == 1) ? (byte) 1 : (turnLeft == 1 && turnRight == 2) ? (byte) 2 : (turnLeft == 2 && turnRight == 2) ? (byte) 3 : (byte) 0;
        byte turnBytes = (turnLeft == 1 && turnRight == 0) ? (byte) 1 : (turnLeft ==0 && turnRight == 1) ? (byte) 2 : (turnLeft == 1 && turnRight == 1) ? (byte) 3 : (byte) 0;
        byte[] eleDriverByte = {0, 0};
        byte[] oilDriverByte = {0, 0};
        int newTotalDriver = 0;
        switch (carType) {
            case 0:
                eleDriverByte = intToByteArrayLittleEndian(eleDriver);
                oilDriverByte = intToByteArrayLittleEndian(oilDriver);
                newTotalDriver = eleDriver + oilDriver;
                break;
            case 1:
                eleDriverByte = intToByteArrayLittleEndian(eleDriver);
                newTotalDriver = eleDriver;
                break;
        }
        byte[] totalDriverByte = intToByteArrayLittleEndian(newTotalDriver);
        byte[] speedByte = intToByteArrayLittleEndian(speed);
        byte[] bodyBytes = {3, 2, 19, (byte) readyStatus, (byte) gear, (byte) hour, (byte) minute, (byte) energyCnseSelect, turnBytes, (byte) driverMode, (byte) energyMode, (byte) temperature, (byte) eleRatio, (byte) oilRatio, eleDriverByte[0], eleDriverByte[1], oilDriverByte[0], oilDriverByte[1], totalDriverByte[0], totalDriverByte[1], speedByte[0], speedByte[1]};
        return completeBytes(bodyBytes);
    }

    // 构建初始化数据包-海外
    public byte[] buildGlobalInitData(int carType, int light, int theme, int mode, int language,int unit,int hour, int minute,
                                int readyStatus, int temperature, int driverMode, int drvMode,
                                int dis, int speed, int eleDriver, int eleRatio, int oilDriver,
                                int oilRatio, int gear, int turnLeft, int turnRight) {
        byte lightByte;
        if (light >= 0 && light < 21) {
            lightByte = 0;
        } else {
            if (21 <= light && light < 41) {
                lightByte = 1;
            } else {
                if (61 <= light && light < 81) {
                    lightByte = 3;
                } else {
                    lightByte = 81 <= light && light < 101 ? (byte) 4 : (byte) 2;
                }
            }
        }

        byte themeByte;
        switch (theme) {
            case 1:
                themeByte = 1;
                break;
            case 2:
                themeByte = 2;
                break;
            default:
                themeByte = 0;
                break;
        }

        byte modeByte = mode == 1 ? (byte) 1 : (byte) 0;

        byte turnBytes = (turnLeft == 2 && turnRight == 1) ?
                (byte) 1 : (turnLeft == 1 && turnRight == 2) ? (byte) 2 :
                (turnLeft == 2 && turnRight == 2) ? (byte) 3 : (byte) 0;

        int newTotalDriver;
        byte[] eleDriverByte = {0, 0};
        byte[] oilDriverByte = {0, 0};
        switch (carType) {
            case 0:
                eleDriverByte = intToByteArrayLittleEndian(eleDriver);
                oilDriverByte = intToByteArrayLittleEndian(oilDriver);
                int newTotalDriver2 = eleDriver + oilDriver;
                newTotalDriver = newTotalDriver2;
                break;
            case 1:
                eleDriverByte = intToByteArrayLittleEndian(eleDriver);
                newTotalDriver = eleDriver;
                break;
            default:
                newTotalDriver = 0;
                break;
        }
        byte[] totalDriverByte = intToByteArrayLittleEndian(newTotalDriver);

        byte[] speedByte = intToByteArrayLittleEndian(speed);

        byte[] bodyBytes = {
                2, 2, 25,
                (byte) carType,
                themeByte,
                modeByte,
                (byte)language,
                (byte)unit,
                lightByte,
                (byte) readyStatus,
                (byte) gear,
                (byte) hour,
                (byte) minute,
                (byte) dis,
                turnBytes,
                (byte) driverMode,
                (byte) drvMode,
                (byte) temperature,
                (byte) eleRatio,
                (byte) oilRatio,
                eleDriverByte[0],
                eleDriverByte[1],
                oilDriverByte[0],
                oilDriverByte[1],
                totalDriverByte[0],
                totalDriverByte[1],
                speedByte[0],
                speedByte[1]};
        return completeBytes(bodyBytes);
    }

    //构建更新数据包-海外
    public  byte[] buildGlobalUpdateData(int carType, int language,int unit,int hour, int minute, int readyStatus,
                                   int temperature, int driverMode, int energyMode, int energyCnseSelect,
                                   int speed, int eleDriver, int eleRatio, int oilDriver,
                                   int oilRatio, int gear,int turnLeft, int turnRight) {
        //byte turnBytes = (turnLeft == 2 && turnRight == 1) ? (byte) 1 : (turnLeft == 1 && turnRight == 2) ? (byte) 2 : (turnLeft == 2 && turnRight == 2) ? (byte) 3 : (byte) 0;
        byte turnBytes = (turnLeft == 1 && turnRight == 0) ? (byte) 1 : (turnLeft ==0 && turnRight == 1) ? (byte) 2 : (turnLeft == 1 && turnRight == 1) ? (byte) 3 : (byte) 0;
        byte[] eleDriverByte = {0, 0};
        byte[] oilDriverByte = {0, 0};
        int newTotalDriver = 0;
        switch (carType) {
            case 0:
                eleDriverByte = intToByteArrayLittleEndian(eleDriver);
                oilDriverByte = intToByteArrayLittleEndian(oilDriver);
                newTotalDriver = eleDriver + oilDriver;
                break;
            case 1:
                eleDriverByte = intToByteArrayLittleEndian(eleDriver);
                newTotalDriver = eleDriver;
                break;
        }
        byte[] totalDriverByte = intToByteArrayLittleEndian(newTotalDriver);
        byte[] speedByte = intToByteArrayLittleEndian(speed);
        byte[] bodyBytes = {3, 2, 21,(byte)language,(byte)unit,(byte) readyStatus, (byte) gear, (byte) hour, (byte) minute, (byte) energyCnseSelect, turnBytes, (byte) driverMode, (byte) energyMode, (byte) temperature, (byte) eleRatio, (byte) oilRatio, eleDriverByte[0], eleDriverByte[1], oilDriverByte[0], oilDriverByte[1], totalDriverByte[0], totalDriverByte[1], speedByte[0], speedByte[1]};
        return completeBytes(bodyBytes);
    }

    public byte[] buildSetLight(int light) {
        int lightByte;
        if (light >= 0 && light < 21) {
            lightByte = 0;
        } else {
            if (21 <= light && light < 41) {
                lightByte = 1;
            } else {
                if (61 <= light && light < 81) {
                    lightByte = 3;
                } else {
                    if (81 <= light && light < 101) {
                        lightByte = 4;
                    } else {
                        lightByte = 2;
                    }
                }
            }
        }
        byte[] bodyBytes = {4, 1, 1, (byte) lightByte};
        return completeBytes(bodyBytes);
    }

    public byte[] buildSetTheme(int theme) {
        byte themeByte;
        switch (theme) {
            case 1:
                themeByte = 1;
                break;
            case 2:
                themeByte = 2;
                break;
            default:
                themeByte = 0;
                break;
        }
        byte[] bodyBytes = {4, 3, 1, themeByte};
        return completeBytes(bodyBytes);
    }

    public  byte[] buildSetMode(int mode) {
        byte modeByte;
        if (mode == 1) {
            modeByte = 1;
        } else {
            modeByte = 0;
        }
        byte[] bodyBytes = {4, 5, 1, modeByte};
        return completeBytes(bodyBytes);
    }

    // 解析数据包（心跳、待机、请求、灯光、主题、模式）
    public MeterBasicDataBean dispatchAnalyzeData(byte[] byteArray) throws NumberFormatException {
        if (byteArray == null || byteArray.length == 0 || !checkHead(byteArray)) {
            return null;
        }

        String position5 = byteToHexString(byteArray[4]);
        String position6 = byteToHexString(byteArray[5]);

        if ("01".equals(position5) && "01".equals(position6)) {
            return analyzeHeartbeatData();
        } else if ("02".equals(position5) && "01".equals(position6)) {
            return analyzeStandbyData();
        } else if ("03".equals(position5) && "01".equals(position6)) {
            return analyzeReqData();
        } else if ("04".equals(position5) && "02".equals(position6)) {
            return analyzeLight(extractData(byteArray));
        } else if ("04".equals(position5) && "04".equals(position6)) {
            return analyzeTheme(extractData(byteArray));
        } else if ("04".equals(position5) && "06".equals(position6)) {
            return analyzeMode(extractData(byteArray));
        }

        return null;
    }

    private List<Byte> extractData(byte[] byteArray) {
        int length = byteArray[6];
        List<Byte> data = new ArrayList<>();
        for (int i = 7; i < 7 + length; i++) {
            data.add(byteArray[i]);
        }
        return data;
    }
    public MeterBasicDataBean analyzeHeartbeatData() {
        return new MeterBasicDataBean(1, false, 0, 0, 0);
    }

    public MeterBasicDataBean analyzeStandbyData() {
        return new MeterBasicDataBean(2, false, 0, 0, 0);
    }

    public MeterBasicDataBean analyzeReqData() {
        return new MeterBasicDataBean(3, false, 0, 0, 0);
    }

    public MeterBasicDataBean analyzeLight(List list) throws NumberFormatException {
        int brightnessValue;
        if (!list.isEmpty()) {
            int level = Integer.parseInt(byteToHexString(((Number) list.get(0)).byteValue()));
            switch (level) {
                case 0:
                    brightnessValue = 20;
                    break;
                case 1:
                    brightnessValue = 40;
                    break;
                case 2:
                    brightnessValue = 60;
                    break;
                case 3:
                default:
                    brightnessValue = 80;
                    break;
                case 4:
                    brightnessValue = 100;
                    break;
            }
            return new MeterBasicDataBean(4, false, brightnessValue, 0, 0);
        }
        return null;
    }

    public MeterBasicDataBean analyzeTheme(List<Byte> list) {
        if (!list.isEmpty()) {
            int themeValue = list.get(0) & 0xFF;
            return new MeterBasicDataBean(5, false, 0, themeValue, 0);
        }
        return null;
    }

    public MeterBasicDataBean analyzeMode(List<Byte> list) {
        if (!list.isEmpty()) {
            int modelValue = list.get(0) & 0xFF;
            return new MeterBasicDataBean(6, false, 0, 0, modelValue);
        }
        return null;
    }

    // CRC 校验与字节拼接
    public byte[] completeBytes(byte[] bodyBytes) {
        byte[] crc16Array = intToByteArrayLittleEndian(calculateCRC16(bodyBytes));
        byte[] full = new byte[headBytes().length + bodyBytes.length + crc16Array.length + tailBytes().length];
        System.arraycopy(headBytes(), 0, full, 0, headBytes().length);
        System.arraycopy(bodyBytes, 0, full, headBytes().length, bodyBytes.length);
        System.arraycopy(crc16Array, 0, full, headBytes().length + bodyBytes.length, crc16Array.length);
        System.arraycopy(tailBytes(), 0, full, headBytes().length + bodyBytes.length + crc16Array.length, tailBytes().length);
        return full;
    }

    public int calculateCRC16(byte[] data) {
        int crc = 0xFFFF;
        for (byte b : data) {
            crc ^= ((b & 0xFF) << 8);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ 0x1021;
                } else {
                    crc <<= 1;
                }
            }
        }
        return crc & 0xFFFF;
    }

    public byte[] intToByteArrayLittleEndian(int value) {
        return new byte[]{
                (byte) (value & 0xFF),
                (byte) ((value >> 8) & 0xFF)
        };
    }
    public byte[] buildGetIpInfo() {
        return new byte[]{-1, 123, 91, 60, 113, 1, 0, 0, 76, 16, 95, -42, 62, 93, 125, -1};
    }

    public byte[] intToByteArrayLittleEndian2(int value) {
        return new byte[]{
                (byte) (value & 0xFF),
                (byte) ((value >> 8) & 0xFF),
                (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 24) & 0xFF)
        };
    }

    public byte[] buildInitUpgrade() {
        return new byte[]{-1, 123, 91, 60, 115, 1, 0, 0, -107, 56, 69, 106, 62, 93, 125, -1};
    }

    public byte[] buildCheckFw(int type, int packetNum) {
        byte typeByte;
        switch (type) {
            case 1:
                typeByte = 4;
                break;
            case 2:
                typeByte = 5;
                break;
            default:
                typeByte = 0;
                break;
        }
        byte[] packetNumByte = intToByteArrayLittleEndian2(packetNum);
        byte[] bodyBytes = {114, 1, 5, 0, typeByte};

        byte[] combined = new byte[bodyBytes.length + packetNumByte.length];
        System.arraycopy(bodyBytes, 0, combined, 0, bodyBytes.length);
        System.arraycopy(packetNumByte, 0, combined, bodyBytes.length, packetNumByte.length)   ;
        return completeBytes2(combined);
    }

    public byte[] buildWriteFw(byte[] dataBytes) {
        if (dataBytes == null) {
            throw new IllegalArgumentException("dataBytes cannot be null");
        }
        byte[] dataLengthByte = intToByteArrayLittleEndian(dataBytes.length);
        byte[] header = {114, 3};

        // 合并数组：header + dataLength + dataBytes
        byte[] combined = new byte[header.length + dataLengthByte.length + dataBytes.length];
        int offset = 0;

        System.arraycopy(header, 0, combined, offset, header.length);
        offset += header.length;

        System.arraycopy(dataLengthByte, 0, combined, offset, dataLengthByte.length);
        offset += dataLengthByte.length;

        System.arraycopy(dataBytes, 0, combined, offset, dataBytes.length);

        return completeBytes2(combined);

    }

    public byte[] buildVerifyFw() {
        return new byte[]{-1, 123, 91, 60, 114, 5, 0, 0, 62, 16, 74, -79, 62, 93, 125, -1};
    }

    public MeterUpgradeBean dispatchAnalyzeData2(byte[] byteArray) throws NumberFormatException {
        if (byteArray == null || byteArray.length == 0 || !checkHead(byteArray)) {
            return null;
        }

        String position5 = byteToHexString(byteArray[4]);
        String position6 = byteToHexString(byteArray[5]);
        int dataLength = byteArray[6] & 0xFF; // 无符号处理

        // 提取有效数据部分
        int start = 8;
        int end = Math.min(start + dataLength, byteArray.length);
        List<Byte> data = new ArrayList<>();
        for (int i = start; i < end; i++) {
            data.add(byteArray[i]);
        }

        // 根据指令类型路由到不同的解析方法
        if ("71".equals(position5) && "02".equals(position6)) {
            return analyzeIpInfoData(data);
        } else if ("73".equals(position5) && "02".equals(position6)) {
            return analyzeInitStateData(data);
        } else if ("72".equals(position5) && "02".equals(position6)) {
            return analyzeCheckStateData(data);
        } else if ("72".equals(position5) && "04".equals(position6)) {
            return analyzeWriteStateData(data);
        } else if ("72".equals(position5) && "06".equals(position6)) {
            return analyzeVerifyStateData(data);
        }

        return null;
    }

    public MeterUpgradeBean analyzeIpInfoData(List<Byte> list) {
        if (list == null || list.isEmpty()) return null;

        StringBuilder hexBuilder = new StringBuilder();
        for (Byte b : list) {
            hexBuilder.append(byteToHexString(b));
        }
        String hexString = hexBuilder.toString();
        List<String> ipParts = new ArrayList<>();

        // 每32位(16进制表示8字符)分割为一个IP字段
        for (int i = 0; i < hexString.length(); i += 8) {
            int end = Math.min(i + 8, hexString.length());
            String hexPart = hexString.substring(i, end);
            ipParts.add(convertHexToIp(hexPart));
        }

        // 确保至少有4个部分
        while (ipParts.size() < 4) {
            ipParts.add("");
        }

        return new MeterUpgradeBean(7, 0,
                ipParts.get(0), ipParts.get(1),
                ipParts.get(2), ipParts.get(3), 2, null);
    }
    private String convertHexToIp(String hex) {
        // 实际实现应根据协议规范转换十六进制到IP
        // 这里简化处理直接返回十六进制字符串
        return hex;
    }
    public MeterUpgradeBean analyzeInitStateData(List list) throws NumberFormatException {
        if (!list.isEmpty()) {
            int result = Integer.parseInt(byteToHexString(((Number) list.get(0)).byteValue()));
            return new MeterUpgradeBean(11, result, null, null, null, null, 60, null);
        }
        return null;
    }

    public  MeterUpgradeBean analyzeCheckStateData(List list) throws NumberFormatException {
        if (!list.isEmpty()) {
            int result = Integer.parseInt(byteToHexString(((Number) list.get(0)).byteValue()));
            return new MeterUpgradeBean(8, result, null, null, null, null, 60, null);
        }
        return null;
    }

    public MeterUpgradeBean analyzeWriteStateData(List list) throws NumberFormatException {
        if (!list.isEmpty()) {
            int result = Integer.parseInt(byteToHexString(((Number) list.get(1)).byteValue()));
            return new MeterUpgradeBean(9, result, null, null, null, null, 60, null);
        }
        return null;
    }

    public MeterUpgradeBean analyzeVerifyStateData(List list) throws NumberFormatException {
        if (!list.isEmpty()) {
            int result = Integer.parseInt(byteToHexString(((Number) list.get(0)).byteValue()));
            return new MeterUpgradeBean(10, result, null, null, null, null, 60, null);
        }
        return null;
    }

    public byte[] completeBytes2(byte[] bodyBytes) {
        byte[] crc32Array = intToByteArrayLittleEndian2(calculateCRC32(bodyBytes));
        /*byte[] byteArray = ArraysKt___ArraysJvmKt.plus(ArraysKt___ArraysJvmKt.plus(ArraysKt___ArraysJvmKt.plus(headBytes(), bodyBytes), crc32Array), tailBytes());
        return byteArray;*/
        // 计算总长度
        int totalLength = headBytes().length + bodyBytes.length + crc32Array.length + tailBytes().length;
        byte[] full = new byte[totalLength];
        int offset = 0;

        // 头部
        System.arraycopy(headBytes(), 0, full, offset, headBytes().length);
        offset += headBytes().length;

        // 主体
        System.arraycopy(bodyBytes, 0, full, offset, bodyBytes.length);
        offset += bodyBytes.length;

        // CRC32
        System.arraycopy(crc32Array, 0, full, offset, crc32Array.length);
        offset += crc32Array.length;

        // 尾部
        System.arraycopy(tailBytes(), 0, full, offset, tailBytes().length);

        return full;
    }

    public  int calculateCRC32(byte[] data) {
        int i;
        int[] table = new int[256];
        int length = table.length;
        for (int i2 = 0; i2 < length; i2++) {
            int crc = i2 << 24;
            for (int j = 0; j < 8; j++) {
                if ((Integer.MIN_VALUE & crc) != 0) {
                    i = (crc << 1) ^ 79764919;
                } else {
                    i = crc << 1;
                }
                crc = i;
            }
            table[i2] = crc;
        }
        int crc2 = -1;
        for (byte b : data) {
            int index = ((crc2 >>> 24) ^ b) & 255;
            crc2 = (crc2 << 8) ^ table[index];
        }
        return crc2;
    }

    public byte[] headBytes() {
        return new byte[]{-1, 123, 91, 60};
    }

    public byte[] tailBytes() {
        return new byte[]{62, 93, 125, -1};
    }

    public boolean checkHead(byte[] bodyBytes) {
        if (bodyBytes.length < 4) return false;
        return (bodyBytes[0] == -1 && bodyBytes[1] == 123 &&
                bodyBytes[2] == 91 && bodyBytes[3] == 60);
    }

    public String byteToHexString(byte b) {
        return String.format("%02x", b & 0xFF).toUpperCase(Locale.ROOT);
    }
}