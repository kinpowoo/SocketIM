package com.example.socket.clink.net.qiujuer.clink.impl;

import com.example.socket.clink.net.qiujuer.clink.core.IoArgs;
import com.example.socket.clink.net.qiujuer.clink.core.IoProvider;
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
    private final IoProvider ioProvider;
    private final OnChannelStatusChangedListener listener;

    private IoArgs.IOArgsEventProcessor receiverIoEventProcessor;
    private IoArgs.IOArgsEventProcessor senderIoEventProcessor;

    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider,
                                OnChannelStatusChangedListener listener) throws IOException{
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;

        channel.configureBlocking(false);
    }


    @Override
    public boolean postReceiveAsync() throws IOException {
        if(isClosed.get()){
            throw new IOException("Current channel is closed!");
        }
        return ioProvider.registerInput(channel,inputCallback);
    }

    @Override
    public void setReceiveListener(IoArgs.IOArgsEventProcessor processor) {
        receiverIoEventProcessor = processor;
    }

    @Override
    public void setSendListener(IoArgs.IOArgsEventProcessor processor) {
        senderIoEventProcessor = processor;
    }

    @Override
    public boolean postSendAsync() throws IOException {
        if(isClosed.get()){
            throw new IOException("Current channel is closed!");
        }
        return ioProvider.registerOutput(channel,outputCallback);
    }




    //关闭
    @Override
    public void close(){
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
    private final IoProvider.HandleInputCallback inputCallback = new IoProvider.HandleInputCallback() {
        @Override
        protected void canProviderInput() {
            //如果已经关闭，直接返回
            if(isClosed.get()){
                return;
            }
            //将全局变量转为本地变量
            IoArgs.IOArgsEventProcessor processor= receiverIoEventProcessor;
            IoArgs args = processor.provideIoArgs();

            //开始读取
            try {
                if(args.readFrom(channel)>0){
                    processor.onConsumeCompleted(args);
                }else{
                    processor.onConsumeFailed(args,new IOException("Cannot read any data!"));
                }
            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };


    //真正处理往 channel 中写出数据的地方
    private final IoProvider.HandleOutputCallback outputCallback = new IoProvider.HandleOutputCallback() {
        @Override
        protected void canProviderOutput() {
            //如果已经关闭，直接返回
            if(isClosed.get()){
                return;
            }
            IoArgs.IOArgsEventProcessor processor = senderIoEventProcessor;
            IoArgs args = processor.provideIoArgs();
            // todo
            //开始写出
            try {
                if(args.writeTo(channel)>0){
                    processor.onConsumeCompleted(args);
                }else{
                    processor.onConsumeFailed(args,new IOException("Cannot write any data!"));
                }
            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };




    public interface OnChannelStatusChangedListener{
        void onChannelClosed(SocketChannel channel);
    }
}
