package com.deepal.ivi.hmi.ipcommon.data;

import androidx.annotation.NonNull;

public class DispatchData {
    private Object data;
    private String srcDataType;

    public DispatchData(Object data, String dataType) {
        this.data = data;
        this.srcDataType = dataType;
    }

    public <T> T getData(){
        return (T)data;
    }

    public String getDataType(){
        return srcDataType;
    }

    @NonNull
    @Override
    public String toString() {
        return "data=" + data + ", srcDataType=" + srcDataType;
    }
}
