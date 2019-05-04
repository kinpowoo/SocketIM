package com.example.socket.clink.net.qiujuer.clink.impl.async;

import com.example.socket.clink.net.qiujuer.clink.core.IoArgs;
import com.example.socket.clink.net.qiujuer.clink.core.Packet;
import com.example.socket.clink.net.qiujuer.clink.core.ReceiveDispatcher;
import com.example.socket.clink.net.qiujuer.clink.core.ReceivePacket;
import com.example.socket.clink.net.qiujuer.clink.core.Receiver;
import com.example.socket.clink.net.qiujuer.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncReceiveDispatcher implements ReceiveDispatcher,IoArgs.IOArgsEventProcessor {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Receiver receiver;
    private final ReceivePacketCallback callback;

    private IoArgs ioArgs = new IoArgs();
    private ReceivePacket<?,?> packetTemp;   //正在接收的packet
    private WritableByteChannel packetChannel;
    private long total;
    private long position;


    /**
     * 构造函数
     * @param receiver
     */
    public AsyncReceiveDispatcher(Receiver receiver,ReceivePacketCallback callback) {
        this.receiver = receiver;
        this.receiver.setReceiveListener(this);
        this.callback = callback;
    }


    @Override
    public void start() {
        registerReceive();
    }

    private void registerReceive(){
        try {
            receiver.postReceiveAsync();
        }catch (IOException e){
            closeAndNotify();
        }
    }


    @Override
    public void stop() {

    }

    private void closeAndNotify(){
        CloseUtils.close(this);
    }

    @Override
    public void close() {
        if(isClosed.compareAndSet(false,true)){
            completePacket(false);
        }
    }


    /**
     * 解析数据到packet
     */
    private void assemblePacket(IoArgs args){
        if(packetTemp == null){
            int length = args.readLength();
            byte type = length>200? Packet.TYPE_STREAM_FILE:
                    Packet.TYPE_MEOMORY_STRING;
            packetTemp = callback.onArrivedNewPacket(type,length);
            packetChannel = Channels.newChannel(packetTemp.open());

            total = length;
            position = 0;
        }
        //将args中的数据写到通道中
        try {
            int count = args.writeTo(packetChannel);
            position += count;

            //检查是否已完成一份Packet接收
            if (position == total) {
                completePacket(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
            completePacket(false);
        }
    }

    /**
     * 完成数据接收操作
     */
    private void completePacket(boolean isSucceed){
        ReceivePacket packet = this.packetTemp;
        CloseUtils.close(packet);
        packetTemp = null;

        WritableByteChannel channel = this.packetChannel;
        CloseUtils.close(channel);
        packetChannel = null;

        if(packet!=null){
            callback.onReceivePacketCompleted(packet);
        }
    }

    @Override
    public IoArgs provideIoArgs() {
        IoArgs args = ioArgs;
        int receiveSize;   //每次可以接收数据的大小
        if(packetTemp==null){
            receiveSize = 4;
        }else{
            receiveSize = (int)Math.min(total-position,args.capacity());
        }
        //设置本次接收数据大小
        args.setLimit(receiveSize);
        return args;
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onConsumeCompleted(IoArgs args) {
        //解析一条数据
        assemblePacket(args);
        //继续接收下一条数据
        registerReceive();
    }
}
