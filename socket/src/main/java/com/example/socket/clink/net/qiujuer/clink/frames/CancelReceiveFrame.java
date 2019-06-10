package com.example.socket.clink.net.qiujuer.clink.frames;

import com.example.socket.clink.net.qiujuer.clink.core.IoArgs;

/**
 * 接收到取消帧CancelSendFrame后进行的处理
 */
public class CancelReceiveFrame extends AbsReceiveFrame{

    CancelReceiveFrame(byte[] header){
        super(header);
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) {
        return 0;
    }
}
