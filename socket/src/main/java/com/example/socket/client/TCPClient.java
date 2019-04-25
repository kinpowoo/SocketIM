package com.example.socket.client;

import com.example.socket.client.bean.DeviceInfo;
import com.example.socket.clink.net.qiujuer.clink.utils.CloseUtils;
import com.example.socket.server.TCPServer;
import com.example.socket.server.handle.ClientHandle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TCPClient {
    private final Socket socket;
    private final ReadHandler readHandler;
    private final PrintStream printStream;

    private TCPClient(Socket socket,ReadHandler readHandler) throws IOException{
        this.socket = socket;
        this.readHandler = readHandler;
        this.printStream = new PrintStream(socket.getOutputStream());
    }


    public static TCPClient startWith(DeviceInfo deviceInfo) throws IOException{
        Socket s = new Socket();
        s.setSoTimeout(3000);        // SET TIME OUT DURATION

        //连接本地，端口2000，超时时间3秒
        s.connect(new InetSocketAddress(Inet4Address.getByName(deviceInfo.getAddress()),
                deviceInfo.getPort()),3000);

        System.out.println("客户端已连接到服务器~");
        System.out.printf("客户端address:%s, port:%d\n",s.getLocalAddress().getHostAddress(),s.getLocalPort());
        System.out.printf("服务端address:%s, port:%d\n",s.getInetAddress().getHostAddress(),s.getPort());
        try {
            ReadHandler readHandler = new ReadHandler(s.getInputStream());
            readHandler.start();
            return new TCPClient(s,readHandler);
        }catch (IOException e){
            System.out.println("socket读写操作出现异常");
            CloseUtils.close(s);
        }
        return null;
    }


    //发送信息
    public void send(String msg){
        printStream.println(msg);
    }

    //退出客户端
    public void exit(){
        //退出读线程，关闭打印流和socket
        readHandler.exit();
        CloseUtils.close(printStream);
        CloseUtils.close(socket);
    }



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

}
