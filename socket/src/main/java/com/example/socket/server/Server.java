package com.example.socket.server;

import com.example.socket.clink.net.qiujuer.clink.core.IOContext;
import com.example.socket.clink.net.qiujuer.clink.impl.IoSelectorProvider;
import com.example.socket.constants.TCPConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Server {

    public static void main(String[] args) throws IOException {
        IOContext.setup()
                .ioProvider(new IoSelectorProvider())
                .start();

        //启动TCP Server
        TCPServer tcpServer = new TCPServer(TCPConstants.PORT_SERVER);
        boolean isSucceed = tcpServer.start();
        if(!isSucceed){
            System.out.println("Start TCP Server failed!");
            return;
        }

        //启动UDP Provider,这个端口号返回给前来搜索的服务端
        UDPProvider.start(TCPConstants.PORT_SERVER);

        // 这里不再是输入任何字符退出服务端，我们把输入的值发送回给所有客户端，除非输入00bye00，才
        // 停止服务端
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String str;
        do{
            str = bufferedReader.readLine();
            tcpServer.broadcast(str);
            System.out.print(">>:");
        }while (!str.equalsIgnoreCase("00bye00"));

        UDPProvider.stop();
        //关闭tcp
        tcpServer.stop();

        IOContext.get().close();
    }

}
