package com.example.socket.clink.net.qiujuer.clink.frames;

import com.example.socket.clink.net.qiujuer.clink.core.Frame;
import com.example.socket.clink.net.qiujuer.clink.core.IoArgs;
import com.example.socket.clink.net.qiujuer.clink.core.SendPacket;

import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * 首帧数据，包括整个Packet的长度，Packet的类型，
 *
 * Data Length : 5byte 40位
 * Data Type : 1byte 8位
 * Data head info : Max 256
 */
public class SendHeaderFrame extends AbsSendPacketFrame{
    static final byte PACKET_HEADER_FRAME_MIN_LENGTH = 6;
    private final byte[] body;

    public SendHeaderFrame(short identifier, SendPacket packet){
        super(PACKET_HEADER_FRAME_MIN_LENGTH,
                Frame.TYPE_PACKET_HEADER,
                Frame.FLAG_NONE,
                identifier,
                packet);
        final long packetLength = packet.length();
        final byte packetType = packet.type();
        final byte[] packetHeaderInfo = packet.headerInfo();

        body = new byte[bodyRemaining];
        //前五位存储长度
        body[0] = (byte)(packetLength >>32);
        body[1] = (byte)(packetLength >>24);
        body[2] = (byte)(packetLength >>16);
        body[3] = (byte)(packetLength >>8);
        body[4] = (byte)(packetLength);
        //类型
        body[5] = packetType;
        //将headerInfo复制到body中
        if(packetHeaderInfo!=null){
            System.arraycopy(packetHeaderInfo,0,body,
                    PACKET_HEADER_FRAME_MIN_LENGTH,packetHeaderInfo.length);
        }
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) {
        int count  = bodyRemaining;
        int offset = body.length - count;
        return ioArgs.readFrom(body,offset,count);
    }


    //发完头帧后，就要开始发数据体帧了，
    @Override
    public Frame buildNextFrame() {
        InputStream stream = packet.open();
        ReadableByteChannel channel = Channels.newChannel(stream);
        return new SendEntityFrame(getBodyIdentifier(),
                packet.length(),channel,packet);
    }
}
