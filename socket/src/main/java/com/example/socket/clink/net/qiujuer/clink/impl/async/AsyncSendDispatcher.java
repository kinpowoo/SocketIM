package com.example.socket.clink.net.qiujuer.clink.impl.async;

import com.example.socket.clink.net.qiujuer.clink.core.IoArgs;
import com.example.socket.clink.net.qiujuer.clink.core.SendDispatcher;
import com.example.socket.clink.net.qiujuer.clink.core.SendPacket;
import com.example.socket.clink.net.qiujuer.clink.core.Sender;
import com.example.socket.clink.net.qiujuer.clink.utils.CloseUtils;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSendDispatcher implements SendDispatcher,IoArgs.IOArgsEventProcessor,
        AsyncPacketReader.PacketProvider {
    private final Sender sender;
    //一个线程安全的非阻塞队列
    private final Queue<SendPacket> sendQueue = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    //是否是在发送
    private final AtomicBoolean isSending = new AtomicBoolean();

    private final AsyncPacketReader reader = new AsyncPacketReader(this);
    private final Object queueLock = new Object();


    /**
     * 构造函数
     * @param sender
     */
    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        //设置监听
        sender.setSendListener(this);
    }

    @Override
    public void send(SendPacket packet) {
        synchronized (queueLock) {
            sendQueue.offer(packet);
            if (isSending.compareAndSet(false, true)) {
                if(reader.requestTakePacket()){
                    requestSend();
                }
            }
        }
    }

    @Override
    public void cancel(SendPacket packet) {
        boolean ret;
        synchronized (queueLock){
            ret = sendQueue.remove(packet);
        }
        if(ret){
            packet.cancel();
            return;
        }
        reader.cancel(packet);
    }


    //拿出一条数据
    @Override
    public SendPacket takePacket(){
        SendPacket packet;
        synchronized (queueLock){
            packet = sendQueue.poll(); //取出一条数据
            if(packet == null){
                //队列为空，取消发送状态
                isSending.set(false);
                return null;
            }
        }
        if(packet.isCanceled()){
            //已取消，不用发送,接着取下一条
            return takePacket();
        }
        return packet;
    }


    /**
     * 完成Packet发送
     * @param packet
     */
    @Override
    public void completedPacket(SendPacket packet,boolean isSucceed) {
        CloseUtils.close(packet);
    }


    /**
     * 请求发送
     */
    private void requestSend(){
        try {
            sender.postSendAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }


    private void closeAndNotify(){
        CloseUtils.close(this);
    }

    //自身关闭方法
    @Override
    public void close(){
        if(isClosed.compareAndSet(false,true)){
            isSending.set(false);
            //reader关闭
            reader.close();
        }
    }


    @Override
    public IoArgs provideIoArgs() {
        return reader.fillData();
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        if(args!=null){
            e.printStackTrace();
        }else{
            //todo
        }
    }

    @Override
    public void onConsumeCompleted(IoArgs args) {
        //继续发送当前包
        if(reader.requestTakePacket()){
            requestSend();
        }
    }
}
