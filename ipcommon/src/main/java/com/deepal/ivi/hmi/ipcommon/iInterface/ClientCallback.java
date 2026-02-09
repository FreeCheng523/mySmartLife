package com.deepal.ivi.hmi.ipcommon.iInterface;

public interface ClientCallback {
    void connectServerStatus(boolean z);

    void receiveMsgFromServer(byte[] bArr);
}