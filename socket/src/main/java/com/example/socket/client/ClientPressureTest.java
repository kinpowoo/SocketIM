package com.example.socket.client;

import com.example.socket.client.bean.DeviceInfo;
import com.example.socket.clink.net.qiujuer.clink.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientPressureTest {
    private static boolean done;

    public static void main(String []args) throws IOException{
        File cachePath = FileUtils.getCacheDir("client/test");

        //创建搜索者对象，并指定超时时间，如果在指定时间内没有设备响应，结束
        DeviceInfo info = UDPSearcher.searchServer(10000);
        System.out.println("Server:"+info);
        if(info == null){
            return;
        }

        int size = 0;
        final List<TCPClient> tcpClientList = new ArrayList<>();
        for (int i=0;i<10;i++){
            try {
                TCPClient tcpClient = TCPClient.startWith(info,cachePath);
                if(tcpClient == null){
                        System.out.println("连接异常");
                        continue;
                }
                //添加到list
                tcpClientList.add(tcpClient);
                System.out.println("连接成功"+(++size));
            }catch (IOException e){
                System.out.println("连接异常");
            }

            //连接服务器的间隔
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //读取一行开始
        System.in.read();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (!done){
                    for (TCPClient tcpClient:tcpClientList){
                        tcpClient.send("HELLO~~");
                    }
                    //每隔一秒再发送一轮
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();

        //读取一行结束
        System.in.read();
        done = true;
        //等待上面的线程执行完成
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //客客端结束
        for (TCPClient tcpClient : tcpClientList) {
            tcpClient.exit();
        }
    }
}
