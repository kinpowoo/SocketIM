package com.example.socket.clink.net.qiujuer.clink.impl.async;

import com.example.socket.clink.net.qiujuer.clink.box.StringReceivePacket;
import com.example.socket.clink.net.qiujuer.clink.core.IOArgs;
import com.example.socket.clink.net.qiujuer.clink.core.ReceiveDispatcher;
import com.example.socket.clink.net.qiujuer.clink.core.ReceivePacket;
import com.example.socket.clink.net.qiujuer.clink.core.Receiver;
import com.example.socket.clink.net.qiujuer.clink.core.SendDispatcher;
import com.example.socket.clink.net.qiujuer.clink.core.SendPacket;
import com.example.socket.clink.net.qiujuer.clink.core.Sender;
import com.example.socket.clink.net.qiujuer.clink.utils.CloseUtils;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncReceiveDispatcher implements ReceiveDispatcher {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Receiver receiver;
    private final ReceivePacketCallback callback;

    private IOArgs ioArgs = new IOArgs();
    private ReceivePacket packetTemp;
    private byte[] buffer;
    private int total;
    private int position;


    /**
     * 构造函数
     * @param receiver
     */
    public AsyncReceiveDispatcher(Receiver receiver,ReceivePacketCallback callback) {
        this.receiver = receiver;
        this.receiver.setReceiveListener(eventListener);
        this.callback = callback;
    }



    @Override
    public void start() {
        registerReceive();
    }

    private void registerReceive(){
        try {
            receiver.receiveAsync(ioArgs);
        }catch (IOException e){
            closeAndNotify();
        }
    }

    private void closeAndNotify(){
        CloseUtils.close(this);
    }


    @Override
    public void stop() {

    }

    @Override
    public void close() throws IOException {
        if(isClosed.compareAndSet(false,true)){
            ReceivePacket packet = this.packetTemp;
            if(packet!=null){
                packetTemp = null;
                CloseUtils.close(packet);
            }
        }
    }


    /**
     * 完成数据接收操作
     */
    private void completePacket(){
        ReceivePacket packet = this.packetTemp;
        CloseUtils.close(packet);
        callback.onReceivePacketCompleted(packet);
    }


    private IOArgs.IOArgsEventListener eventListener = new IOArgs.IOArgsEventListener() {
        @Override
        public void onStarted(IOArgs args) {
            //
            int receiveSize;
            if(packetTemp==null){
                receiveSize = 4;
            }else{
                receiveSize = Math.min(total-position,args.capacity());
            }
            //设置本次接收数据大小
            args.setLimit(receiveSize);

        }
        @Override
        public void onCompleted(IOArgs args) {
            assemblePacket(args);
            //继续接收下一条数据
            registerReceive();
        }
    };


    /**
     * 解析数据到packet
     *
     */
    private void assemblePacket(IOArgs args){
        if(packetTemp == null){
            int length = args.readLength();
            packetTemp = new StringReceivePacket(length);
            buffer = new byte[length];
            total = length;
            position = 0;
        }
        int count = args.writeTo(buffer,0);
        if(count>0) {
            packetTemp.save(buffer, 0);
            position += count;

            //检查是否已完成一份Packet接收
            if (position == total) {
                completePacket();
                packetTemp = null;
            }
        }
    }

}
