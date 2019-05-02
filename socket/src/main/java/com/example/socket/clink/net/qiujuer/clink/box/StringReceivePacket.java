package com.example.socket.clink.net.qiujuer.clink.box;

import com.example.socket.clink.net.qiujuer.clink.core.ReceivePacket;

public class StringReceivePacket extends ReceivePacket {
    private byte[] buffer;   //数据缓存副本
    private int position;    //存放数据的起始点

    public StringReceivePacket(int len){
        this.buffer = new byte[len];
        length = len;
    }



    @Override
    public void save(byte[] bytes, int count) {
        System.arraycopy(bytes,0,buffer,position,count);
        position += count;   //将起始点加上接收的长度
    }

    public String string(){
        return new String(buffer);
    }

    @Override
    public void close() {
        super.close();
    }
}
