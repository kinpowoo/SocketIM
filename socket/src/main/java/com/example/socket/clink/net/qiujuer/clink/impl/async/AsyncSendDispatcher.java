package com.example.socket.clink.net.qiujuer.clink.impl.async;

import com.example.socket.clink.net.qiujuer.clink.core.IoArgs;
import com.example.socket.clink.net.qiujuer.clink.core.SendDispatcher;
import com.example.socket.clink.net.qiujuer.clink.core.SendPacket;
import com.example.socket.clink.net.qiujuer.clink.core.Sender;
import com.example.socket.clink.net.qiujuer.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSendDispatcher implements SendDispatcher,IoArgs.IOArgsEventProcessor {
    private final Sender sender;
    //一个线程安全的非阻塞队列
    private final Queue<SendPacket> sendQueue = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    //是否是在发送
    private final AtomicBoolean isSending = new AtomicBoolean();


    private IoArgs ioArgs = new IoArgs();
    private SendPacket<?> packetTemp;  //当前发送的数据
    private long total;     //当前packet最大的值
    private long position;  //当前packet已发送长度

    private ReadableByteChannel packetChannel;

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
        sendQueue.offer(packet);
        if(isSending.compareAndSet(false,true)){
             sendNextPacket();
        }
    }

    //拿出一条数据
    private SendPacket takePacket(){
        SendPacket packet = sendQueue.poll();   //取出一条数据
        if(packet !=null && packet.isCanceled()){
            //已取消，不用发送,接着取下一条
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
        total = packet.length();
        position = 0;
        sendCurrentPacket();
    }

    private void sendCurrentPacket(){
        if(position>=total){   //表示已经发送完了一条数据
            completePacket(position==total);
            sendNextPacket();  //开始发送下一条
            return;
        }
        try {
            sender.postSendAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    private void completePacket(boolean isSuccess){
        SendPacket packet = this.packetTemp;
        if(packet==null){
            return;
        }
        CloseUtils.close(packet,packetChannel);
        packetTemp = null;
        packetChannel = null;
        total = 0;
        position = 0;
    }



    private void closeAndNotify(){
        CloseUtils.close(this);
    }

    //自身关闭方法
    @Override
    public void close() {
        if(isClosed.compareAndSet(false,true)){
            isSending.set(false);
            //异常关闭导致的完成
            completePacket(false);
        }
    }

    @Override
    public void cancel(SendPacket packet) {

    }


    @Override
    public IoArgs provideIoArgs() {
        IoArgs args = ioArgs;
        if(packetChannel == null){
            packetChannel = Channels.newChannel(packetTemp.open());
            args.setLimit(4);
            args.writeLength((int)packetTemp.length());
        }else{
            args.setLimit((int)Math.min(args.capacity(),total-position));
            try {
                int count = args.readFrom(packetChannel);
                position+=count;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return args;
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onConsumeCompleted(IoArgs args) {
        //继续发送当前包
        sendCurrentPacket();
    }
}
