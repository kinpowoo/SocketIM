package com.example.socket.server.handle;

import com.example.socket.clink.net.qiujuer.clink.core.Connector;
import com.example.socket.clink.net.qiujuer.clink.utils.CloseUtils;
import java.io.IOException;
import java.nio.channels.SocketChannel;


public class ClientHandle extends Connector{
    private final ClientHandleCallback callback;
    private final String clientInfo;

    public ClientHandle(SocketChannel client, final ClientHandleCallback callback) throws IOException {
        setup(client);
        this.callback = callback;
        this.clientInfo = client.getRemoteAddress().toString();

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
    protected void onReceiveNewMessage(String str) {
        super.onReceiveNewMessage(str);
        callback.onNewMessageArrive(this,str);
    }

    //自身出异常而被迫关闭
    private void exitBySelf(){
        exit();
        if(callback!=null){
            callback.onSelfClosed(this);
        }
    }


    //给外部调用者的回调，通知外部自己因为自身抛异常而非正常退出
    public interface ClientHandleCallback{
        void onSelfClosed(ClientHandle clientHandle);
        void onNewMessageArrive(ClientHandle clientHandle,String msg);
    }


    //读取线程
    /**
    class ClientReadHandler extends Thread{
        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer byteBuffer;

        public ClientReadHandler(Selector selector) {
            this.selector = selector;
            this.byteBuffer = ByteBuffer.allocate(256);
        }

        @Override
        public void run(){
            super.run();
            try {
                //只要客户端不表示断开，就一直通信
                do{
                    //来自客户端的信息
                    if(selector.select()==0){
                        if(done){
                            break;
                        }
                        continue;
                    }

                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while(iterator.hasNext()){
                        if(done){
                            break;
                        }
                        SelectionKey key = iterator.next();
                        iterator.remove();

                        if(key.isReadable()){
                            SocketChannel socketChannel = (SocketChannel) key.channel();
                            byteBuffer.clear();
                            int read = socketChannel.read(byteBuffer);
                            if(read>0){
                                //丢弃换行符，该换行符是我们自己定义的，即每次传输数据时以换行符做为结束标识
                                String str = new String(byteBuffer.array(),0,read -1);
                                //将接收到的客户端消息返回到上一级
                                if(callback!=null) {
                                    callback.onNewMessageArrive(ClientHandle.this,str);
                                }
                            }else{
                                System.out.println("客户端已无法读取数据！");
                                //退出当前客户端并中止循环
                                ClientHandle.this.exitBySelf();
                                break;
                            }
                        }
                    }
                }while (!done);
            }catch (IOException E){
                if(!done){  //如果不是自己手动中止
                    System.out.println("客户端IO流读取异常，连接断开");
                    ClientHandle.this.exitBySelf();
                }
            }finally {
                //关闭输入流
                CloseUtils.close(selector);
            }
        }

        void exit(){
            done = true;
            selector.wakeup();
            CloseUtils.close(selector);
        }
    }

    //输出线程
    class ClientWriteHandler{
        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer byteBuffer;
        private final ExecutorService executorService;

        ClientWriteHandler(Selector selector){
            this.selector = selector;
            this.byteBuffer = ByteBuffer.allocate(256);

            //构造一个单线程的线程池,用于打印消息
            executorService = Executors.newSingleThreadExecutor();
        }

        void exit(){
            done = true;
            CloseUtils.close(selector);
            executorService.shutdownNow();  //将执行发送任务的线程池也立即停止
        }

        void send(String str){
            str = str+'\n';

            if(done){
                //如果写线程已shutdown，不继续后续操作
                return;
            }

            //reset方法，它是将当前的position设置为0
            //rewind是将mark重置为-1,position重置为0;
            //mark()会让 mark = position，让mark的值等于当前 position位置
            //clear方法是真正的重置，将mark=-1,position=0,limit=capacity(即当前buffer的容量),
            //                    但是数据不会清空，因为position=0后，再往里面填充数据会覆盖原数据

            //所以，在写数据时，用 clear()重置一下各个变量，最后写进去的长度就是 position的值
            //相反，在读数据时，用 flip() 翻转一下各个变量，让 limit = position, position = 0,就可以读到所有数据

            byteBuffer.clear();
            //再将字符串装载到byteBuffer中
            byteBuffer.put(str.getBytes());

            //对byteBuffer进行反转，这是重点,首先我们 clear() buffer，position指针位置回到0 ,limit = 256
            //然后将 str 放到 byteBuffer 中，byteBuffer 的 position 指针 == limit长度 == str.length 长度,
            //我们发送byteBuffer数据时，是将 position指针位置->limit位置之间的数据发送出去，这种情况下发出去的是空数据
            // flip() 操作会将  limit = position, position = 0,这时我们发送出去的数据又是完整长度了
            byteBuffer.flip();

            //当byteBuffer还有可发送数据时
            while (!done && byteBuffer.hasRemaining()){
                try {
                    //小于0代表写出异常
                    // len = 0合法，为什么呢，因为我们当前的 socketChannel是非阻塞的，我们也没有
                    // 用 Selector 来注册可写事件监听，所以直接调用 socketChannel 的写方法，可能
                    // 当前网卡繁忙无法发送数据，会直接返回0，所以这并不是异常
                    int len = client.write(byteBuffer);
                    if(len<0){
                        System.out.println("客户端已无法写出数据！");
                        ClientHandle.this.exitBySelf();
                        break;
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }
     */


}
