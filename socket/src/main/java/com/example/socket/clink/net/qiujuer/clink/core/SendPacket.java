package com.example.socket.clink.net.qiujuer.clink.core;

import java.io.IOException;
import java.io.InputStream;

/**
 * 发送包的定义
 */
public abstract class SendPacket<T extends InputStream> extends Packet<T>{
    private boolean isCanceled;  //是否已取消

    public boolean isCanceled(){
        return isCanceled;
    }

    /**
     * 设置取消发送标记
     */
    public void cancel(){
        isCanceled = true;
    }

}
