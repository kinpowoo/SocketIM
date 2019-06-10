package com.example.socket.clink.net.qiujuer.clink.frames;

import com.example.socket.clink.net.qiujuer.clink.core.Frame;
import com.example.socket.clink.net.qiujuer.clink.core.IoArgs;

import java.io.IOException;

public abstract class AbsReceiveFrame extends Frame {
    //帧体可读写剩余大小
    volatile int bodyRemaining;

    AbsReceiveFrame(byte[] header){
        super(header);
        bodyRemaining = getBodyLength();
    }

    @Override
    public synchronized boolean handle(IoArgs ioArgs) throws IOException {
        if(bodyRemaining == 0){
            //如果已读取所有数据,直接返回true
            return true;
        }
        bodyRemaining -= consumeBody(ioArgs);
        return bodyRemaining == 0;
    }

    protected abstract int consumeBody(IoArgs ioArgs) throws IOException;

    @Override
    public final Frame nextFrame() {
        return null;
    }

    @Override
    public int getConsumableLength() {
        return bodyRemaining;
    }
}
