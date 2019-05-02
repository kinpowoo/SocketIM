package com.example.socket.clink.net.qiujuer.clink.core;

/**
 * 发送包的定义
 */
public abstract class SendPacket extends Packet{
    private boolean isCanceled;  //是否已取消
    public abstract byte[] bytes();   //要发送的内容

    public boolean isCanceled(){
        return isCanceled;
    }

}
