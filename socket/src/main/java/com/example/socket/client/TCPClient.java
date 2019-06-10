package com.example.socket.client;

import com.example.socket.client.bean.DeviceInfo;
import com.example.socket.clink.net.qiujuer.clink.core.Connector;
import com.example.socket.clink.net.qiujuer.clink.core.Packet;
import com.example.socket.clink.net.qiujuer.clink.core.ReceivePacket;
import com.example.socket.clink.net.qiujuer.clink.utils.CloseUtils;
import com.example.socket.clink.net.qiujuer.clink.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class TCPClient extends Connector {
    private final File cachePath;

    private TCPClient(SocketChannel socketChannel,File cachePath) throws IOException{
        this.cachePath = cachePath;
        setup(socketChannel);
    }

    public static TCPClient startWith(DeviceInfo deviceInfo,File cacheDir) throws IOException{
        SocketChannel s = SocketChannel.open();

        //连接本地，端口2000，超时时间3秒
        s.connect(new InetSocketAddress(Inet4Address.getByName(deviceInfo.getAddress()),
                deviceInfo.getPort()));

        System.out.println("客户端已连接到服务器~");
        System.out.println("客户端信息"+s.getLocalAddress().toString());
        System.out.println("服务端信息"+s.getRemoteAddress().toString());
        try {
            return new TCPClient(s,cacheDir);
        }catch (IOException e){
            System.out.println("连接异常");
            CloseUtils.close(s);
        }
        return null;
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        System.out.println("连接已断开，无法读取数据");
    }

    @Override
    protected File createNewReceiveFile() {
        File tempFile = FileUtils.createRandomTemp(cachePath);
        return tempFile;
    }

    //退出客户端
    public void exit(){
        //退出读线程，
        CloseUtils.close(this);
    }

    @Override
    protected void onReceivedPacket(ReceivePacket packet) {
        super.onReceivedPacket(packet);
        if(packet.type() == Packet.TYPE_MEMORY_STRING){
            String string = (String) packet.entity();
            System.out.println(key.toString()+":"+string);
        }
    }

    /**
    //读取线程
    static class ReadHandler extends Thread{
        private boolean done = false;
        private final InputStream inputStream;

        public ReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run(){
            super.run();
            try {
                //在这里处理读取服务端传回的信息
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

                //只要客户端不表示断开，就一直通信
                do{
                    //来自客户端的信息
                    String str;
                    try {
                        str= br.readLine();
                        if(str==null){
                            System.out.println("客户端已断开与服务器连接,无法读取信息！");
                            break;
                        }
                        //不为null便打印信息
                        System.out.println("客户端接收到的信息:"+str);
                        System.out.print(">>:");
                    }catch (SocketTimeoutException e){
                        //如果读取时间超时，继续等待
                        continue;
                    }
                }while (!done);
            }catch (IOException E){
                if(!done){  //如果不是自己手动中止
                    System.out.println("客户端IO流读取异常，连接断开："+E.getMessage());
                }
            }finally {
                //关闭输入流
                CloseUtils.close(inputStream);
            }
        }

        void exit(){
            done = true;
            CloseUtils.close(inputStream);
        }
    }
        */

}
