package com.example.socket.clink.net.qiujuer.clink.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * 公共的数据封装
 * 提供类型以及数据长度的定义
 *
 * 将Packet改为流传输，以支持大文件发送
 */
public abstract class Packet<Stream extends Closeable> implements Closeable {
    //bytes类型，可以直接存储在内存中
    public static final byte TYPE_MEMORY_BYTES  = 1;
    //String类型，可以直接存储在内存中
    public static final byte TYPE_MEMORY_STRING = 2;
    //文件类型，无法直接存储在内存中，因为体积过大，只能以流的形式读取
    public static final byte TYPE_STREAM_FILE = 3;
    //长链接流 类型
    public static final byte TYPE_STREAM_DIRECT = 4;

    protected long length;  //数据长度,因为文件有可能几个G大小，用int表示范围不够
    private Stream stream;   //输入流


    //返回具体的类型
    public abstract byte type();


    public long length() {
        return length;
    }


    /**
     * 打开一个流，返回是泛型，可以是InputStream 或 OutputStream
     * 加上final关键字后子类无法继承该方法
     * @return
     */
    public final Stream open(){
        if(stream == null){
            stream = createStream();
        }
        return stream;
    }

    /**
     * 创建一个流
     * @return
     */
    protected abstract Stream createStream();

    @Override
    public final void close() throws IOException {
        if(stream!=null){
            closeStream(stream);
            stream = null;
        }
    }

    protected void closeStream(Stream stream) throws IOException {
        stream.close();
    }


    /**
     * 头部额外信息，用于携带额外的校验信息等
     * return byte 数组，最大255长度
     */
    public byte[] headerInfo(){
        return null;
    }


}
