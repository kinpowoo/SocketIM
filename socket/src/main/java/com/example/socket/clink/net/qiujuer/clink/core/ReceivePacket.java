package com.example.socket.clink.net.qiujuer.clink.core;

/**
 * 接收包的定义
 */
public abstract class ReceivePacket extends Packet {
    public abstract void save(byte[] bytes,int count);  //将bytes保存指定count长度

}
