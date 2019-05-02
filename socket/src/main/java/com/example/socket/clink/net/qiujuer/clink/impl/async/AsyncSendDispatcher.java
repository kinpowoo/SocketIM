package com.example.socket.clink.net.qiujuer.clink.impl.async;

import com.example.socket.clink.net.qiujuer.clink.core.IOArgs;
import com.example.socket.clink.net.qiujuer.clink.core.SendDispatcher;
import com.example.socket.clink.net.qiujuer.clink.core.SendPacket;
import com.example.socket.clink.net.qiujuer.clink.core.Sender;
import com.example.socket.clink.net.qiujuer.clink.utils.CloseUtils;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSendDispatcher implements SendDispatcher {
    private final Sender sender;
    //一个线程安全的非阻塞队列
    private final Queue<SendPacket> sendQueue = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    //是否是在发送
    private final AtomicBoolean isSending = new AtomicBoolean();


    private IOArgs ioArgs = new IOArgs();
    private SendPacket packetTemp;
    private int total;     //当前packet最大的值
    private int position;  //当前packet已发送长度


    /**
     * 构造函数
     * @param sender
     */
    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
    }

    @Override
    public void send(SendPacket packet) {
        sendQueue.offer(packet);
        if(isSending.compareAndSet(false,true)){
            sendNextPacket();
        }
    }

    //拿出一条数据
    private SendPacket takePacket(){
        SendPacket packet = sendQueue.poll();   //取出一条数据
        if(packet !=null && packet.isCanceled()){
            //已取消，不用发送
            return takePacket();
        }
        return packet;
    }

    //发送一条数据
    private void sendNextPacket(){
        //如果之前那条数据不等于空
        SendPacket temp = packetTemp;
        if(temp!=null){
            CloseUtils.close(temp);
        }

        SendPacket packet = packetTemp = takePacket();
        if(packet == null){
            //没有数据可发送了,设置发送状态为false
            isSending.set(false);
            return;
        }
        total = packet.getLength();
        position = 0;
        sendCurrentPacket();
    }

    private void sendCurrentPacket(){
        IOArgs args = ioArgs;
        args.startWriting();

        if(position>=total){   //表示已经发送完了一条数据
            sendNextPacket();  //开始发送下一条
            return;
        }else if(position==0){  //表示刚刚开始发送
            //首包，需要携带长度信息
            args.writeLength(total);
        }
        byte[] bytes = packetTemp.bytes();
        //把bytes的数据写入到IoArgs
        int count = args.readFrom(bytes,position);
        position+=count;

        //完成封装
        args.finishWriting();

        try {
            sender.sendAsync(args,eventListener);
        } catch (IOException e) {
            closeAndNotity();
        }
    }

    private void closeAndNotity(){
        CloseUtils.close(this);
    }

    //自身关闭方法
    @Override
    public void close() {
        if(isClosed.compareAndSet(false,true)){
            isSending.set(false);
            SendPacket packet = this.packetTemp;
            if(packet!=null){
               packetTemp = null;
               CloseUtils.close(packet);
            }
        }
    }

    @Override
    public void cancel(SendPacket packet) {

    }


    private final IOArgs.IOArgsEventListener eventListener = new IOArgs.IOArgsEventListener() {
        @Override
        public void onStarted(IOArgs args) {

        }
        @Override
        public void onCompleted(IOArgs args) {
            //继续发送当前包
            sendCurrentPacket();
        }
    };
}
