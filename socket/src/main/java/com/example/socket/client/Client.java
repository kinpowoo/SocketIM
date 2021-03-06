package com.example.socket.client;

import com.example.socket.client.bean.DeviceInfo;
import com.example.socket.clink.net.qiujuer.clink.box.FileSendPacket;
import com.example.socket.clink.net.qiujuer.clink.core.IoContext;
import com.example.socket.clink.net.qiujuer.clink.impl.IoSelectorProvider;
import com.example.socket.clink.net.qiujuer.clink.utils.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Client {

    public static void main(String[] args) throws IOException {
        File cachePath = FileUtils.getCacheDir("client");

        IoContext.setup()
                .ioProvider(new IoSelectorProvider())
                .start();

        //创建搜索者对象，并指定超时时间，如果在指定时间内没有设备响应，结束
        DeviceInfo info = UDPSearcher.searchServer(10000);
        System.out.println("Server:"+info);

        TCPClient tcpClient = null;
        if(info!=null){
            try {
                tcpClient = TCPClient.startWith(info,cachePath);
                if(tcpClient==null){
                    return;
                }
                write(tcpClient);
            }catch (IOException e){
                e.printStackTrace();
            }finally {
                if(tcpClient!=null){
                    tcpClient.exit();
                }
            }
        }

        IoContext.get().close();
    }



    //写操作，发送到服务端，直接用键盘输入流
    private static void write(TCPClient client) throws IOException{
        //获取键盘输入流，并转成bufferedReader
        InputStream is = System.in;
        BufferedReader bis = new BufferedReader(new InputStreamReader(is));

        //下面的循环用于不断将键盘写入的内容发送给服务器
        do{
            //将客户端的输入发送给服务器
            System.out.print(">>:");
            //键盘读取一行
            String c2s = bis.readLine();
            if("00bye00".equalsIgnoreCase(c2s)){
                //如果输入上面的字符，中止输入
                break;
            }

            //想要发送文件
            if(c2s.startsWith("--f")){
                String[] array = c2s.split(" ");
                if(array.length>=2){
                    String filePath = array[1];
                    File file = new File(filePath);
                    if(file.exists() && file.isFile()){
                        //如果文件存在
                        FileSendPacket packet = new FileSendPacket(file);
                        client.send(packet);
                        continue;   //如果发送文件成功，直接跳过
                    }
                }
            }

            //发送字符串
            client.send(c2s);
        }while (true);
    }

}
