package com.example.socket.clink.net.qiujuer.clink.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class IOArgs {

    private byte[] byteBuffer = new byte[256];
    private ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);

    public int read(SocketChannel channel) throws IOException {
        buffer.clear();
        return channel.read(buffer);
    }

    public int write(SocketChannel channel) throws IOException{
        return channel.write(buffer);
    }

    public String bufferToString(){
        //丢弃换行符
        return new String(byteBuffer,0,buffer.position()-1);
    }

    public interface IOArgsEventListener{
        void onStarted(IOArgs args);
        void onCompleted(IOArgs args);
    }


}
