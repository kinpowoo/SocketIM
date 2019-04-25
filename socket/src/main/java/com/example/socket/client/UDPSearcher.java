package com.example.socket.client;

import com.example.socket.client.bean.DeviceInfo;
import com.example.socket.clink.net.qiujuer.clink.utils.ByteUtils;
import com.example.socket.constants.UDPConstants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class UDPSearcher {

    private static final int LISTEN_PORT = UDPConstants.PORT_CLIENT_RESPONSE;
    static int time = 1;

    public static DeviceInfo searchServer(int timeout){
        System.out.println("UDPSearcher Started...");

        //创建一个栅栏，用于阻塞线程直到成功收到设备的回送
        CountDownLatch receiveLatch = new CountDownLatch(1);
        Listener listener = null;
        try {
            listener = listen(receiveLatch);
            sendBroadcast();
            receiveLatch.await(timeout, TimeUnit.MILLISECONDS);
        }catch (Exception e){
            e.printStackTrace();
        }
        if(listener==null){
            return null;
        }
        List<DeviceInfo> devices = listener.getDevicesAndClose();
        if(devices.size()>0){
            return devices.get(0);
        }
        return null;
    }

    private static Listener listen(CountDownLatch receiveLatch) throws InterruptedException{
        System.out.println("UDPSearcher start listening...");
        //该栅栏用于提示搜索监听对象已经启动完成，即监听线程启动完成
        CountDownLatch startLatch = new CountDownLatch(1);
        Listener listener = new Listener(LISTEN_PORT,startLatch,receiveLatch);
        listener.start();
        //开始阻塞线程
        startLatch.await();
        //只有当 listener 线程 run 方法里 startLatch调用了countDown方法
        //这里的listener 才会返回
        return listener;
    }

    private static void sendBroadcast() throws IOException{
        System.out.println("UDPSearcher sendBroadcast started...");
        //作为搜索方，让系统自动分配监听端口
        DatagramSocket ds = new DatagramSocket();
        ByteBuffer bb = ByteBuffer.allocate(128);
        //一样的，先装入公共头
        bb.put(UDPConstants.HEADER);
        //然后表明这是搜索方身份
        bb.putShort((short)1);
        //指明让设备回送数据的端口
        bb.putInt(LISTEN_PORT);

        DatagramPacket broadcastPacket = new DatagramPacket(bb.array(),
                bb.position()+1);
        //广播的地址和端口号
        broadcastPacket.setAddress(InetAddress.getByName("255.255.255.255"));
        broadcastPacket.setPort(UDPConstants.PORT_SERVER);

        System.out.println("服务器第"+time+"次发送广播");
        time++;
        ds.send(broadcastPacket);
        ds.close();

        System.out.println("UDPSearcher finished sendBroadcast...");
    }

    private static class Listener extends Thread{
        private final int listenPort;
        private final CountDownLatch startLatch;
        private final CountDownLatch receiveLatch;
        private final List<DeviceInfo> devices = new ArrayList<>();
        private final byte[] buffer = new byte[128];
        //前面公共头 + 身份类型 + 回送端口 所用的长度
        private final int minLen = UDPConstants.HEADER.length + 2 + 4;
        private boolean done = false;
        private DatagramSocket ds = null;

        public Listener(int listenPort, CountDownLatch startLatch, CountDownLatch receiveLatch) {
            this.listenPort = listenPort;
            this.startLatch = startLatch;
            this.receiveLatch = receiveLatch;
        }

        @Override
        public void run() {
            super.run();

            //通知已经启动，可以返回 listener 对象了
            startLatch.countDown();

            try {
                //监听回送端口
                ds = new DatagramSocket(listenPort);
                //构建接收实体
                DatagramPacket receivePacket = new DatagramPacket(buffer,buffer.length);

                while (!done){
                    //接收
                    ds.receive(receivePacket);

                    //打印接收到的信息与发送者的信息
                    String senderIP = receivePacket.getAddress().getHostAddress();
                    int senderPort = receivePacket.getPort();
                    int dataLen = receivePacket.getLength();
                    byte[] data = receivePacket.getData();
                    boolean isValid = dataLen>=minLen && ByteUtils.startWith(data,UDPConstants.HEADER);

                    System.out.println("UDPSearcher receive from ip:"+senderIP+"\tport:"+senderPort
                            +"\tdataValid:"+isValid);
                    if(!isValid){
                        //无效,跳过这个包
                        continue;
                    }
                    //这里初始化ByteBuffer，用buffer里的内容作为ByteBuffer的内容，
                    //取 HEADER.length 到 dataLen 中间的数据作为ByteBuffer的数据，
                    //前面HEADER的数据就相当于丢弃了
                    ByteBuffer bb = ByteBuffer.wrap(buffer,UDPConstants.HEADER.length,dataLen);
                    final short cmd = bb.getShort();
                    final int devicePort = bb.getInt();
                    //如果设备返回的身份不为2或者返回的设备端口小于等于0，认为该设备无效，跳过
                    if(cmd!=2 || devicePort<=0){
                        System.out.println("UDPSearcher receive cmd:"+cmd+"\tdevicePort:"+devicePort);
                        continue;
                    }
                    //公共头+身份类型+设备端口号后面就是设备的唯一识别码
                    String sn = new String(buffer,minLen,dataLen-minLen);
                    DeviceInfo info = new DeviceInfo(sn,devicePort,senderIP);
                    devices.add(info);

                    //成功收一个设备响应后，让阻塞的线程恢复
                    receiveLatch.countDown();
                }
            }catch (Exception e){
            }finally {
                close();
            }
            System.out.println("UDPSearcher listener finished....");
        }

        private void close(){
            if(ds!=null){
                ds.close();
                ds = null;
            }
        }

        public List<DeviceInfo> getDevicesAndClose(){
            done = true;
            close();
            System.out.println("UDPSearcher Finished...");
            return devices;
        }

    }

}
