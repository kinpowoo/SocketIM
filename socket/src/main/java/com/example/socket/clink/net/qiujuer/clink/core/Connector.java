package com.example.socket.clink.net.qiujuer.clink.core;

import com.example.socket.clink.net.qiujuer.clink.box.StringReceivePacket;
import com.example.socket.clink.net.qiujuer.clink.box.StringSendPacket;
import com.example.socket.clink.net.qiujuer.clink.impl.SocketChannelAdapter;
import com.example.socket.clink.net.qiujuer.clink.impl.async.AsyncReceiveDispatcher;
import com.example.socket.clink.net.qiujuer.clink.impl.async.AsyncSendDispatcher;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

public class Connector implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {

    private UUID key = UUID.randomUUID();
    private SocketChannel socketChannel;
    private Sender sender;
    private Receiver receiver;
    private SendDispatcher sendDispatcher;
    private ReceiveDispatcher receiveDispatcher;

    public void setup(SocketChannel socketChannel) throws IOException{
        this.socketChannel = socketChannel;

        IOContext ioContext = IOContext.get();
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


    /**
     * 接收消息完成回调
     */
    private ReceiveDispatcher.ReceivePacketCallback receivePacketCallback =
            new ReceiveDispatcher.ReceivePacketCallback() {
        @Override
        public void onReceivePacketCompleted(ReceivePacket packet) {
            if(packet instanceof StringReceivePacket){
                String msg = ((StringReceivePacket)packet).string();
                onReceiveNewMessage(msg);
            }
        }
    };

}
