package com.example.socket.clink.net.qiujuer.clink.core;

import java.io.Closeable;

/**
 * 公共的数据封装
 * 提供类型以及数据长度的定义
 */
public abstract class Packet implements Closeable {
    protected byte type;  //类型，
    protected int length;  //数据长度

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    @Override
    public void close(){}

}
