package com.example.socket.clink.net.qiujuer.clink.frames;

import com.example.socket.clink.net.qiujuer.clink.core.Frame;
import com.example.socket.clink.net.qiujuer.clink.core.IoArgs;
import com.example.socket.clink.net.qiujuer.clink.core.SendPacket;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

public class SendEntityFrame extends AbsSendPacketFrame {
    private final long unConsumeLength;  //未消费长度
    private final ReadableByteChannel channel;


    SendEntityFrame(short identifier, long entityLength,
                           ReadableByteChannel channel,
                           SendPacket packet) {
        super((int)Math.min(entityLength,Frame.MAX_CAPACITY),
                Frame.TYPE_PACKET_ENTITY,
                Frame.FLAG_NONE,
                identifier, packet);
        //1234567890 要发送的数据
        //1234 5678 90 分成三帧
        //10 4,6 4,2 2   总长度，发送长度、剩余长度
        this.unConsumeLength = entityLength - bodyRemaining;
        this.channel = channel;
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) throws IOException {
        if(packet == null){
            //已调用取消发送，则填充假数据
            return ioArgs.fillEmpty(bodyRemaining);
        }
        return ioArgs.readFrom(channel);
    }

    @Override
    public Frame buildNextFrame() {
        if(unConsumeLength == 0){
            return null;
        }
        return new SendEntityFrame(getBodyIdentifier(),
                unConsumeLength,channel,packet);
    }
}
