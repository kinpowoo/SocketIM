package com.example.socket.server.handle;

import com.example.socket.clink.net.qiujuer.clink.core.Connector;
import com.example.socket.clink.net.qiujuer.clink.core.Packet;
import com.example.socket.clink.net.qiujuer.clink.core.ReceivePacket;
import com.example.socket.clink.net.qiujuer.clink.utils.CloseUtils;
import com.example.socket.clink.net.qiujuer.clink.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;


public class ClientHandle extends Connector{
    private final File cachePath;
    private final ClientHandleCallback callback;
    private final String clientInfo;

    public ClientHandle(SocketChannel socketChannel, final ClientHandleCallback callback,
                        File cachePath) throws IOException {
        setup(socketChannel);
        this.cachePath = cachePath;
        this.callback = callback;
        this.clientInfo = socketChannel.getRemoteAddress().toString();

        //打印新连接的客户端信息
        System.out.println("新连入的客户端:"+clientInfo);
    }

    public void exit(){
        CloseUtils.close(this);
        System.out.printf("客户端address:%s 已退出！\n",clientInfo);
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        exitBySelf();
    }

    @Override
    protected File createNewReceiveFile() {
        return FileUtils.createRandomTemp(cachePath);
    }

    @Override
    protected void onReceivedPacket(ReceivePacket packet) {
        super.onReceivedPacket(packet);
        if(packet.type() == Packet.TYPE_MEMORY_STRING){
            String string = (String) packet.entity();
            System.out.println(key.toString()+":"+string);
            callback.onNewMessageArrive(this,string);
        }
    }



    //自身出异常而被迫关闭
    private void exitBySelf(){
        exit();
        callback.onSelfClosed(this);
    }


    //给外部调用者的回调，通知外部自己因为自身抛异常而非正常退出
    public interface ClientHandleCallback{
        void onSelfClosed(ClientHandle clientHandle);
        void onNewMessageArrive(ClientHandle clientHandle,String msg);
    }

}
