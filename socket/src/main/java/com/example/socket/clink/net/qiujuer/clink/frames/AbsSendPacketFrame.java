package com.example.socket.clink.net.qiujuer.clink.frames;


import com.example.socket.clink.net.qiujuer.clink.core.Frame;
import com.example.socket.clink.net.qiujuer.clink.core.IoArgs;
import com.example.socket.clink.net.qiujuer.clink.core.SendPacket;

import java.io.IOException;

public abstract class AbsSendPacketFrame extends AbsSendFrame {
    protected volatile SendPacket<?> packet;

    public AbsSendPacketFrame(int length, byte type, byte flag, short identifier,SendPacket packet){
        super(length,type,flag,identifier);
        this.packet = packet;
    }

    /**
     * 获取当前发送的Packet
     * @return
     */
    public synchronized SendPacket getPacket(){
        return packet;
    }

    @Override
    public synchronized boolean handle(IoArgs ioArgs) throws IOException {
        if(packet == null && !isSending()){
            //已取消，并且未发送任何数据，直接返回结束，发送下一帧
            return true;
        }
        return super.handle(ioArgs);
    }

    @Override
    public final synchronized Frame nextFrame() {
        return packet == null?null:buildNextFrame();
    }



    //True,当前帧没有发送任何数据
    public boolean abort(){
        boolean isSending = isSending();
        if(isSending){
            fillDirtyDataOnAbort();
        }
        packet = null;
        return !isSending;
    }

    //填充假数据
    private void fillDirtyDataOnAbort() {
    }

    protected abstract Frame buildNextFrame();
}
