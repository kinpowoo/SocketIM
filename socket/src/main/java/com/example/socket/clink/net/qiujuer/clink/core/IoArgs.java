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

    //从bytes[]数组中读数据到 buffer
    public int readFrom(byte[] bytes,int offset,int count){
        int size = Math.min(count,buffer.remaining());
        if(size<=0){
            return 0;
        }
        buffer.put(bytes,offset,size);
        return size;
    }

    //从buffer中读数据到 bytes[] 数组
    public int writeTo(byte[] bytes,int offset){
        int size = Math.min(bytes.length-offset,buffer.remaining());
        buffer.get(bytes,offset,size);
        return size;
    }

    //从channel中读数据到 buffer
    public int readFrom(ReadableByteChannel channel) throws IOException{
        int bytesProduced = 0;
        while(buffer.hasRemaining()){
            int len = channel.read(buffer);
            if(len<0){
                throw new EOFException();
            }
            bytesProduced+=len;
        }
        return bytesProduced;
    }

    //从 buffer 中写数据到 channel 中
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
        int bytesProduced = 0;
        while(buffer.hasRemaining()){
            int len = channel.read(buffer);
            if(len<0){
                throw new EOFException();
            }
            bytesProduced+=len;
        }
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
        this.limit = Math.min(limit,buffer.capacity());
    }

    public int readLength(){
        return buffer.getInt();
    }

    public int capacity(){
        return buffer.capacity();
    }

    public boolean remained() {
        return buffer.remaining() > 0;
    }

    /**
     * 填充空数据
     * @param bodyRemaining
     * @return
     */
    public int fillEmpty(int bodyRemaining) {
        int fillSize = Math.min(bodyRemaining,buffer.remaining());
        buffer.position(buffer.position() + fillSize);   //只移动buffer的位置
        return fillSize;
    }

    /**
     * 清空一部分数据，不读取一部分数据
     * @param bodyRemaining
     * @return
     */
    public int setEmpty(int bodyRemaining) {
        int emptySize = Math.min(bodyRemaining,buffer.remaining());
        buffer.position(buffer.position() + emptySize);   //只移动buffer的位置
        return emptySize;
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
