package com.example.socket.clink.net.qiujuer.clink.impl.async;

import com.example.socket.clink.net.qiujuer.clink.core.IoArgs;
import com.example.socket.clink.net.qiujuer.clink.core.ReceiveDispatcher;
import com.example.socket.clink.net.qiujuer.clink.core.ReceivePacket;
import com.example.socket.clink.net.qiujuer.clink.core.Receiver;
import com.example.socket.clink.net.qiujuer.clink.utils.CloseUtils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncReceiveDispatcher implements ReceiveDispatcher,IoArgs.IOArgsEventProcessor,
        AsyncPacketWriter.PacketProvider {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Receiver receiver;
    private final ReceivePacketCallback callback;
    private final AsyncPacketWriter writer = new AsyncPacketWriter(this);

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
    public void close(){
        if(isClosed.compareAndSet(false,true)){
            writer.close();
        }
    }


    @Override
    public IoArgs provideIoArgs() {
        return writer.takeIoArgs();
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onConsumeCompleted(IoArgs args) {
        do {
            writer.consumeIoArgs(args);
        }while (args.remained());
        //继续接收下一条数据
        registerReceive();
    }

    @Override
    public ReceivePacket takePacket(byte type, long length, byte[] headerInfo) {
        return callback.onArrivedNewPacket(type,length);
    }

    @Override
    public void completedPacket(ReceivePacket packet, boolean isSucceed) {
        CloseUtils.close(packet);
        callback.onReceivePacketCompleted(packet);
    }
}
