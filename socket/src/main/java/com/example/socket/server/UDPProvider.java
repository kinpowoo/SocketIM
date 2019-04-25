package com.example.socket.server;

import com.example.socket.clink.net.qiujuer.clink.utils.ByteUtils;
import com.example.socket.constants.UDPConstants;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.UUID;

public class UDPProvider {
    private static Provider PROVIDER_INSTANCE;

    static int time = 1;

    static void start(int port){
        stop();
        String sn = UUID.randomUUID().toString();
        Provider provider = new Provider(sn,port);
        provider.start();
        PROVIDER_INSTANCE = provider;
    }

    static void stop(){
        if(PROVIDER_INSTANCE!=null){
            PROVIDER_INSTANCE.exit();
            PROVIDER_INSTANCE = null;
        }
    }


    private static class Provider extends Thread{
        private final byte[] sn;
        private final int port;
        private boolean done = false;
        private DatagramSocket ds = null;
        //存储消息的Buffer
        final byte[] buffer = new byte[128];

        public Provider(String sn, int port) {
            this.sn = sn.getBytes();
            this.port = port;
        }

        @Override
        public void run() {
            super.run();
            System.out.println("UDPProvider started...");

            try {
                //监听来自搜索者固定的端口
                ds = new DatagramSocket(UDPConstants.PORT_SERVER);
                //构建一个接收包
                DatagramPacket receivePack = new DatagramPacket(buffer,buffer.length);

                while (!done){
                    //接收包
                    ds.receive(receivePack);

                    //打印接收到的信息与发送者的信息
                    String senderIP = receivePack.getAddress().getHostName();
                    int senderPort = receivePack.getPort();
                    //目标数据及其长度
                    byte[] senderData = receivePack.getData();
                    int dataLen = receivePack.getLength();

                    //判断发送过来的数据是否符合规范，不符合规范就不处理
                    //数据前面需要包括一份 HEADER + 2（short类型标识数据发送者的身份,1/2）
                    // + 4 (int类型指明要回传数据的端口)
                    boolean isValid = dataLen >= (UDPConstants.HEADER.length +2 +4)
                             && ByteUtils.startWith(senderData,UDPConstants.HEADER);

                    System.out.println("我第"+time+"次收到来自Searcher发送的广播");
                    time++;

                    System.out.println("UDPProvider receive from ip:"+senderIP+"\tport:"+senderPort
                            +"\tdataValid:"+isValid);

                    if(!isValid){
                        //如果数据不符合规范，跳过这条数据
                        continue;
                    }

                    //如果符合，解析命令与回送端口
                    //需要解析的数据从HEADER后面开始，所以跳过公共HEADER部分
                    int index = UDPConstants.HEADER.length;
                    //先将senderData[index]这个值左移8位，然后index++,再取一位数据与前面一个进行或运算，
                    // 得到short值，因为short占两个字节，即00000000 00000000，前面一个需要先向左移动8位，
                    // 再与后一个数或运算拼接，得到完整的short值，& 0xff 只是起到净化数据的作用，将高于8位的置0
                    // 因为 0xff 的二进制为11111111，转为int32位后为 00000000 00000000 00000000 11111111，
                    // 0 & 0 = 0，0 & 1 = 0，1 & 0 = 0，1 & 1 = 1，根据这个规律，可以将高于8位的无论是0还是1，
                    // 与0相与后都是0，就起到了净化的作用，因为 byte[index]转成short后成8位扩展到16位，会产生
                    // 补位现象，数据的实际大小可能发生变化，所以需要将高位清0，将byte左移8位也能达到将高于8位
                    // 的值给舍弃掉，低8位移到了高8位，低8位补0，
                    // 0|0=0；  0|1=1；  1|0=1；   1|1=1；
                    // 0^0=0；  0^1=1；  1^0=1；   1^1=0；
                    short cmd = (short)((senderData[index++]<<8) | (senderData[index++] & 0xff));

                    //解析指定回送的端口号
                    int responsePort =  (((senderData[index++]) << 24) |
                            ((senderData[index++] & 0xff) << 16) |
                            ((senderData[index++] & 0xff) << 8) |
                            ((senderData[index] & 0xff)));

                    //判断合法性，cmd == 1 表示这是UDPSearcher发送过来的
                    if(cmd == 1 && responsePort>0){
                        //构建一份回送数据
                        // wrap()方法会将传入的byte数组清空，再往这个数组里写入内容，
                        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
                        //首先放入公共头
                        byteBuffer.put(UDPConstants.HEADER);
                        //再放入身份类型，2代表客户端设备
                        byteBuffer.putShort((short) 2);
                        //将自己的设备端口返回
                        byteBuffer.putInt(port);
                        byteBuffer.put(sn);

                        // position方法可以获取ByteBuffer当前的指针位置，现在指针处于尾部，
                        // 即得到了 byteBuffer 里面数据的长度
                        int len = byteBuffer.position();

                        //用byteBuffer的内容构造数据包，并指定发送的接收者的ip和端口
                        DatagramPacket responsePacket = new DatagramPacket(buffer,len);
                        responsePacket.setAddress(receivePack.getAddress());
                        responsePacket.setPort(responsePort);
                        ds.send(responsePacket);
                        System.out.println("UDPProvider response to:"+senderIP+"\tport:"+responsePort
                                +"\tDataLen:"+len);
                    }else{
                        //如果无效，
                        System.out.println("UDPProvider receive cmd nonsupport,cmd:"+cmd+"\tport:"+port);
                    }
                }

            }catch (Exception e){
            }finally {
                close();
            }
            //完成
            System.out.println("UDPProvider Finished...");
        }

        private void close(){
            if(ds!=null){
                ds.close();
                ds = null;
            }
        }

        //提供结束
        private void exit(){
            done = true;
            close();
        }

    }

}
