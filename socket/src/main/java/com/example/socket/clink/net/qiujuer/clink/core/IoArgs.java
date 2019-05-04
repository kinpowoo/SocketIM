package com.example.socket.clink.net.qiujuer.clink.core;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

/**
 * 将IoArgs改为流的输入输出
 */
public class IoArgs {
    private int limit = 256;
    private ByteBuffer buffer = ByteBuffer.allocate(limit);

    //从bytes中读数据到 buffer
    public int readFrom(ReadableByteChannel channel) throws IOException{
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

    //从 buffer 中写数据到bytes中
    public int writeTo(WritableByteChannel channel) throws IOException{
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
        startWriting();
        buffer.putInt(length);
        finishWriting();
    }

    public int readLength(){
        return buffer.getInt();
    }

    public int capacity(){
        return buffer.capacity();
    }


    /**
     *  IoArgs提供者、处理者；数据的生产或消费者
     */
    public interface IOArgsEventProcessor{
        IoArgs provideIoArgs();   //提供IoArgs
        void onConsumeFailed(IoArgs args,Exception e);   //消费失败
        void onConsumeCompleted(IoArgs args);    //消费成功
    }


}
