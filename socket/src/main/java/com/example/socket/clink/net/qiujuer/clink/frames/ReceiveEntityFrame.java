package com.example.socket.clink.net.qiujuer.clink.frames;

import com.example.socket.clink.net.qiujuer.clink.core.IoArgs;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public class ReceiveEntityFrame extends AbsReceiveFrame{
    private WritableByteChannel channel;

    ReceiveEntityFrame(byte[] header){
        super(header);
    }

    public void bindPacketChannel(WritableByteChannel channel){
        this.channel = channel;
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) throws IOException {
        return channel == null?ioArgs.setEmpty(bodyRemaining)
                :ioArgs.writeTo(channel);
    }
}
