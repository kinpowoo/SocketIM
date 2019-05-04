package com.example.socket.clink.net.qiujuer.clink.box;

import com.example.socket.clink.net.qiujuer.clink.core.ReceivePacket;

import java.io.ByteArrayOutputStream;

/**
 * 定义最基础的基于 ByteArrayOutputStream 的输出接收包
 * @param <Entity>
 */
public abstract class AbsByteArrayReceivePacket<Entity> extends
        ReceivePacket<ByteArrayOutputStream,Entity> {

    public AbsByteArrayReceivePacket(long len) {
        super(len);
    }

    @Override
    protected final ByteArrayOutputStream createStream(){
        return new ByteArrayOutputStream((int) length);
    }
}
