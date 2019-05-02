package com.example.socket.clink.net.qiujuer.clink.box;

import com.example.socket.clink.net.qiujuer.clink.core.SendPacket;

public class StringSendPacket extends SendPacket {
    private byte[] bytes;

    public StringSendPacket(String msg) {
        this.bytes = msg.getBytes();
        this.length = bytes.length;
    }

    @Override
    public byte[] bytes() {
        return bytes;
    }


    @Override
    public void close() {
        super.close();
    }
}
