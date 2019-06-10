package com.example.socket.clink.net.qiujuer.clink.core;

import java.io.IOException;

/**
 * 包头： 6byte 32位
 * 包体: Stream
 *
 * 头部定义：
 *  当前帧大小 2byte 16位 0~65535
 *  帧类型 Type   1byte 8位 -128~127 ： 区分是首帧还是数据帧
 *  帧标志信息 Flag  1byte 8位 00001111 :   存储加密方式
 *  对应包唯一标示 Identifier   1byte 8位 1~255
 *  预留空间 Other   1byte 8位
 *  数据区     Frame Payload xxxxx 0~65535
 */
public abstract class Frame {
    //头部字节长度
    public static final byte FRAME_HEADER_LENGTH = 6;
    //可以容纳的最大长度
    public static final int MAX_CAPACITY = 66635;
    //数据类型,包头类型，包数据实体类型，发送取消类型，接收拒绝类型
    public static final byte TYPE_PACKET_HEADER = 11;
    public static final byte TYPE_PACKET_ENTITY = 12;
    public static final byte TYPE_COMMAND_SEND_CANCEL = 41;
    public static final byte TYPE_COMMAND_RECEIVE_REJECT = 42;

    public static final byte FLAG_NONE = 0;  //暂时用不到

    protected final byte[] header = new byte[FRAME_HEADER_LENGTH];
    public Frame(int length,byte type,byte flag,short identifier){
        if(length<0 || length>MAX_CAPACITY){
            throw new RuntimeException("");
        }
        if(identifier <1 || identifier>255){
            throw new RuntimeException("");
        }
        //两者就组成了最终长度，也就是int值的后16位，0—65535
        header[0] = (byte)(length>>8);  //先将int值的第二个8位取出来，也就是将低八位舍弃
        header[1] = (byte)(length);       //存储低八位

        header[2] = type;
        header[3] = flag;

        header[4] = (byte)identifier;
        header[5] = 0;
    }

    public Frame(byte[] header){
        System.arraycopy(header,0,this.header,0,FRAME_HEADER_LENGTH);
    }

    public int getBodyLength(){
        //因为两个8位组装后
        return ((((int)header[0]) & 0xff)<<8) |
                (((int)header[0]) & 0xff);
    }

    public byte getBodyFlag(){
        return header[3];
    }

    public short getBodyIdentifier(){
        return (short) (((short)header[4]) & 0xff);
    }

    public abstract boolean handle(IoArgs ioArgs) throws IOException;

    /**
     * 构建帧时，帧头这些额外信息所占用的内存，在文件很大，帧数很多时，
     * 消耗的内存也很大，所以我们只唯护一个帧，只有当前帧发送完成，
     * 我们才去构建下一个帧，至于发送成功与否，通过返回的boolean判断
     * 当整个数据发送完成，nextFrame返回null
     */
    public abstract Frame nextFrame();

    //获取帧体可接收数据的长度
    public abstract int getConsumableLength();
}
