package com.example.socket.clink.net.qiujuer.clink.core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class IOArgs {
    private int limit = 256;
    private byte[] byteBuffer = new byte[256];
    private ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);

    //从bytes中读数据到 buffer
    public int readFrom(byte[] bytes,int offset){
        int size = Math.min(bytes.length-offset,buffer.remaining());
        buffer.put(bytes,offset,size);
        return size;
    }

    //从 buffer 中写数据到bytes中
    public int writeTo(byte[] bytes,int offset){
        int size = Math.min(bytes.length-offset,buffer.remaining());
        buffer.get(bytes,offset,size);
        return size;
    }


    /**
     * 从SocketChannel读取数据
     * @param channel
     * @return
     * @throws IOException
     */
    public int readFrom(SocketChannel channel) throws IOException {
        startWriting();
        int bytesProduced = 0;
        while(buffer.hasRemaining()){
            int len = channel.read(buffer);
            if(len<0){
                throw new EOFException();
            }
            bytesProduced+=len;
        }
        finishWriting();
        return bytesProduced;
    }

    //向socket channel中写出数据
    public int writeTo(SocketChannel channel) throws IOException{
        int bytesProduced = 0;
        while(buffer.hasRemaining()){
            int len = channel.write(buffer);
            if(len<0){
                throw new EOFException();
            }
            bytesProduced+=len;
        }
        return bytesProduced;
    }

    public void startWriting(){
        //重置所有操作
        buffer.clear();
        //自定义容纳区间
        buffer.limit(limit);
    }

    public void finishWriting(){
        buffer.flip();
    };


    /**
     * 设置单次写操作的容纳区间
     */
    public void setLimit(int limit){
        this.limit = limit;
    }


    public void writeLength(int length){
        buffer.putInt(length);
    }

    public int readLength(){
        return buffer.getInt();
    }

    public int capacity(){
        return buffer.capacity();
    }


    public interface IOArgsEventListener{
        void onStarted(IOArgs args);
        void onCompleted(IOArgs args);
    }


}
