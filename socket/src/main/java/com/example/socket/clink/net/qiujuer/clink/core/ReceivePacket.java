package com.example.socket.clink.net.qiujuer.clink.core;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 接收包的定义
 */
public abstract class ReceivePacket<Stream extends OutputStream,Entity> extends Packet<Stream> {
    //当前接收包的最终实体形式，可以是字符串，byte数组，或者文件
    private Entity entity;

    public ReceivePacket(long len) {
        this.length = len;
    }

    /**
     * 最终接收到的数据实体
     */
    public Entity entity(){
        return entity;
    }

    /**
     * 根据接收的流转化为对应的实体
     *
     * @param stream {@link OutputStream}
     */
    protected abstract Entity buildEntity(Stream stream);

    /**
     * 先关闭流，随后将流的内容转化为对应的实体
     */
    @Override
    protected final void closeStream(Stream stream) throws IOException{
        super.closeStream(stream);
        entity = buildEntity(stream);
    };
}
