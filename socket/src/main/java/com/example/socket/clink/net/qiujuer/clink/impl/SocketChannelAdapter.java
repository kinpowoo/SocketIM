package com.example.socket.clink.net.qiujuer.clink.impl;

import com.example.socket.clink.net.qiujuer.clink.core.IOArgs;
import com.example.socket.clink.net.qiujuer.clink.core.IOProvider;
import com.example.socket.clink.net.qiujuer.clink.core.Receiver;
import com.example.socket.clink.net.qiujuer.clink.core.Sender;
import com.example.socket.clink.net.qiujuer.clink.utils.CloseUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketChannelAdapter implements Sender, Receiver, Closeable {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final SocketChannel channel;
    private final IOProvider ioProvider;
    private final OnChannelStatusChangedListener listener;

    private IOArgs.IOArgsEventListener receiverIoEventListener;
    private IOArgs.IOArgsEventListener senderIoEventListener;


    public SocketChannelAdapter(SocketChannel channel, IOProvider ioProvider,
                                OnChannelStatusChangedListener listener) throws IOException{
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;

        channel.configureBlocking(false);
    }

    @Override
    public boolean receiveAsync(IOArgs.IOArgsEventListener listener) throws IOException {
        if(isClosed.get()){
            throw new IOException("Current channel is closed!");
        }
        receiverIoEventListener = listener;
        return ioProvider.registerInput(channel,inputCallback);
    }

    @Override
    public boolean sendAsync(IOArgs args, IOArgs.IOArgsEventListener listener) throws IOException {
        if(isClosed.get()){
            throw new IOException("Current channel is closed!");
        }
        senderIoEventListener = listener;
        outputCallback.setAttach(args);   //设置要写出的数据
        return ioProvider.registerOutput(channel,outputCallback);
    }


    //关闭
    @Override
    public void close() throws IOException {
        //判断isClose是否==false,如果是false，则将其更新为true
        if(isClosed.compareAndSet(false,true)){
            ioProvider.unregisterInput(channel);
            ioProvider.unregisterInput(channel);

            CloseUtils.close(channel);
            //回调当前Channel已关闭
            listener.onChannelClosed(channel);
        }
    }


    //真正从 channel 中读取数据的地方
    private final IOProvider.HandleInputCallback inputCallback = new IOProvider.HandleInputCallback() {
        @Override
        protected void canProviderInput() {
            //如果已经关闭，直接返回
            if(isClosed.get()){
                return;
            }
            IOArgs args = new IOArgs();
            //将全局变量转为本地变量
            IOArgs.IOArgsEventListener receiverIoEventListener = SocketChannelAdapter.this.receiverIoEventListener;
            if(receiverIoEventListener!=null) {
                receiverIoEventListener.onStarted(args);
            }

            //开始读取
            try {
                if(args.read(channel)>0 && listener!=null){
                    receiverIoEventListener.onCompleted(args);
                }else{
                    throw new IOException("Cannot read any data!");
                }
            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };


    //真正处理往 channel 中写出数据的地方
    private final IOProvider.HandleOutputCallback outputCallback = new IOProvider.HandleOutputCallback() {
        @Override
        protected void canProviderOutput(Object attach) {
            //如果已经关闭，直接返回
            if(isClosed.get()){
                return;
            }

            senderIoEventListener.onCompleted(null);
        }
    };


    public interface OnChannelStatusChangedListener{
        void onChannelClosed(SocketChannel channel);
    }
}