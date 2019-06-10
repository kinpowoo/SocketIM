package com.example.socket.clink.net.qiujuer.clink.core;

import com.example.socket.clink.net.qiujuer.clink.box.BytesReceivePacket;
import com.example.socket.clink.net.qiujuer.clink.box.FileReceivePacket;
import com.example.socket.clink.net.qiujuer.clink.box.StringReceivePacket;
import com.example.socket.clink.net.qiujuer.clink.box.StringSendPacket;
import com.example.socket.clink.net.qiujuer.clink.impl.SocketChannelAdapter;
import com.example.socket.clink.net.qiujuer.clink.impl.async.AsyncReceiveDispatcher;
import com.example.socket.clink.net.qiujuer.clink.impl.async.AsyncSendDispatcher;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

public abstract class Connector implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {

    protected UUID key = UUID.randomUUID();
    private SocketChannel socketChannel;
    private Sender sender;
    private Receiver receiver;
    private SendDispatcher sendDispatcher;
    private ReceiveDispatcher receiveDispatcher;

    public void setup(SocketChannel socketChannel) throws IOException{
        this.socketChannel = socketChannel;

        IoContext ioContext = IoContext.get();
        SocketChannelAdapter adapter = new SocketChannelAdapter(socketChannel,
                ioContext.getIoProvider(),this);
        this.sender = adapter;
        this.receiver = adapter;

        sendDispatcher = new AsyncSendDispatcher(sender);
        receiveDispatcher = new AsyncReceiveDispatcher(receiver,receivePacketCallback);

        //启动接收
        receiveDispatcher.start();
    }

    public void send(String msg){
        SendPacket packet = new StringSendPacket(msg);
        sendDispatcher.send(packet);
    }

    public void send(SendPacket packet){
        sendDispatcher.send(packet);
    }

    @Override
    public void close() throws IOException {
        receiveDispatcher.close();
        sendDispatcher.close();
        sender.close();
        receiver.close();
        socketChannel.close();
    }


    @Override
    public void onChannelClosed(SocketChannel channel) {

    }


    protected void onReceiveNewMessage(String str){
        System.out.println(key.toString()+":"+str);
    }


    protected void onReceivedPacket(ReceivePacket packet){
        System.out.println(key.toString()+":[New Packet]-Type:"+packet.type()
                +", Length:"+packet.length);
    }


    /**
     * 创建一个临时的接收文件
     * @return
     */
    protected abstract File createNewReceiveFile();

    /**
     * 接收消息完成回调
     */
    private ReceiveDispatcher.ReceivePacketCallback receivePacketCallback =
            new ReceiveDispatcher.ReceivePacketCallback() {
        @Override
        public void onReceivePacketCompleted(ReceivePacket packet) {
            onReceivedPacket(packet);
        }

        @Override
        public ReceivePacket<?, ?> onArrivedNewPacket(byte type, long length) {
            switch (type){
                case Packet.TYPE_MEMORY_BYTES:
                    return new BytesReceivePacket(length);
                case Packet.TYPE_MEMORY_STRING:
                    return new StringReceivePacket(length);
                case Packet.TYPE_STREAM_FILE:
                    return new FileReceivePacket(length,createNewReceiveFile());
                case Packet.TYPE_STREAM_DIRECT:
                    return new BytesReceivePacket(length);
                default:
                    throw new UnsupportedOperationException("Unsupported type:"+type);
            }
        }
    };

}
