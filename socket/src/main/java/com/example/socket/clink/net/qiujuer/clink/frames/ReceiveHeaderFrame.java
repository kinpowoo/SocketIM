package com.example.socket.clink.net.qiujuer.clink.frames;

import com.example.socket.clink.net.qiujuer.clink.core.IoArgs;

public class ReceiveHeaderFrame extends AbsReceiveFrame{
    private final byte[] body;

    public ReceiveHeaderFrame(byte[] header) {
        super(header);
        body = new byte[bodyRemaining];
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) {
        int offset = body.length - bodyRemaining;
        return ioArgs.writeTo(body,offset);
    }

    public long getPacketLength(){
        //将header前五个字节所拼成的长度取出来，为 long 类型
        return ((((long)body[0]) & 0xFFL) << 32)
                | ((((long)body[0]) & 0xFFL) << 24)
                |((((long)body[0]) & 0xFFL) << 16)
                |((((long)body[0]) & 0xFFL) << 8)
                |(((long)body[0]) & 0xFFL);
    }

    public byte getPacketType() {
        return body[5];
    }

    public byte[] getPacketHeaderInfo(){
        //如果body长度大于最小头部信息长度，说明它有头部信息，将头部信息提取出来
        if(body.length > SendHeaderFrame.PACKET_HEADER_FRAME_MIN_LENGTH){
            byte[] headerInfo = new byte[body.length - SendHeaderFrame.PACKET_HEADER_FRAME_MIN_LENGTH];
            System.arraycopy(body,SendHeaderFrame.PACKET_HEADER_FRAME_MIN_LENGTH,
                    headerInfo,0,headerInfo.length);
            return headerInfo;
        }
        return null;
    }
}
