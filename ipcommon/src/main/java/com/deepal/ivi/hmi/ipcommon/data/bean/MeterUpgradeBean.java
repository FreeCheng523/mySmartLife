package com.deepal.ivi.hmi.ipcommon.data.bean;

public final class MeterUpgradeBean {
    private final int status;
    private final int result;
    private final String ip;
    private final String mac;
    private final String version;
    private final String file;
    private final int progress;
    private final Object extra;

    public MeterUpgradeBean(int status, int result, String ip, String mac, String version, String file, int progress, Object extra) {
        this.status = status;
        this.result = result;
        this.ip = ip;
        this.mac = mac;
        this.version = version;
        this.file = file;
        this.progress = progress;
        this.extra = extra;
    }

    public int getStatus() {
        return status;
    }

    public int getResult() {
        return result;
    }

    public String getIp() {
        return ip;
    }

    public String getMac() {
        return mac;
    }

    public String getVersion() {
        return version;
    }

    public String getFile() {
        return file;
    }

    public int getProgress() {
        return progress;
    }

    public Object getExtra() {
        return extra;
    }
}
