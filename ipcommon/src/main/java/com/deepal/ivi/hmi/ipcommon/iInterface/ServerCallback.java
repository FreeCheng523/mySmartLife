package com.deepal.ivi.hmi.ipcommon.iInterface;

public interface ServerCallback {
    void connectClientStatus(boolean z,String reason);

    void receiveMsgFromClient(byte[] bArr);
}