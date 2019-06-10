package com.example.socket.clink.net.qiujuer.clink.frames;

import com.example.socket.clink.net.qiujuer.clink.core.Frame;
import com.example.socket.clink.net.qiujuer.clink.core.IoArgs;

public class CancelSendFrame extends AbsSendFrame{
    public CancelSendFrame(short identifier) {
        super(0,
                Frame.TYPE_COMMAND_SEND_CANCEL,
                Frame.FLAG_NONE,
                identifier);
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) {
        return 0;
    }

    @Override
    public Frame nextFrame() {
        return null;
    }
}
