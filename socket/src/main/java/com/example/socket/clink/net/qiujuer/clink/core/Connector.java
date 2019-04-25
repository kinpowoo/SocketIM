package com.example.socket.clink.net.qiujuer.clink.core;

import com.example.socket.clink.net.qiujuer.clink.impl.SocketChannelAdapter;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

public class Connector implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {

    private UUID key = UUID.randomUUID();
    private SocketChannel socketChannel;
    private Sender sender;
    private Receiver receiver;

    public void setup(SocketChannel socketChannel) throws IOException{
        this.socketChannel = socketChannel;

        IOContext ioContext = IOContext.get();
        SocketChannelAdapter adapter = new SocketChannelAdapter(socketChannel,
                ioContext.getIoProvider(),this);
        this.sender = adapter;
        this.receiver = adapter;

        //开始读取数据
        readNextMessage();
    }


    private void readNextMessage(){
        if(receiver!=null){
            try {
                receiver.receiveAsync(echoReceiveListener);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("接收数据异常："+e.getMessage());
            }
        }
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void onChannelClosed(SocketChannel channel) {

    }

    private IOArgs.IOArgsEventListener echoReceiveListener = new IOArgs.IOArgsEventListener() {
        @Override
        public void onStarted(IOArgs args) {

        }

        @Override
        public void onCompleted(IOArgs args) {
            //打印
            onReceiveNewMessage(args.bufferToString());
            //读取下一条数据
            readNextMessage();
        }
    };

    protected void onReceiveNewMessage(String str){
        System.out.println(key.toString()+":"+str);
    }

}
