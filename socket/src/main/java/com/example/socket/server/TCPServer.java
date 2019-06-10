package com.example.socket.server;

import com.example.socket.clink.net.qiujuer.clink.utils.CloseUtils;
import com.example.socket.server.handle.ClientHandle;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements ClientHandle.ClientHandleCallback {
    private final int port;
    private final File cacheDir;
    private ClientListener mListener;
    //用一个list来装载所有已连接的客户端,在多线程并发情况下需要对该list操作进行同步
    private List<ClientHandle> clients = new ArrayList<>();
    //维护一个转发线程池
    private ExecutorService forwardThreadPool;

    private Selector selector;
    private ServerSocketChannel server;

    public TCPServer(int port,File cacheDir){
        this.port = port;
        this.cacheDir = cacheDir;
        //转发线程池
        forwardThreadPool = Executors.newSingleThreadExecutor();
    }

    public boolean start(){
        try {
            selector = Selector.open();
            server = ServerSocketChannel.open();
            //设为非阻塞
            server.configureBlocking(false);
            //绑定本地端口
            server.socket().bind(new InetSocketAddress(port));
            //注册客户端到达监听
            server.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("服务器信息："+server.getLocalAddress().toString());

            ClientListener listener = this.mListener = new ClientListener();
            listener.start();
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop(){
        if(mListener!=null){
            mListener.exit();
        }
        CloseUtils.close(server);
        CloseUtils.close(selector);

        //停止服务端的时候，所有相连接的客户端也要断开连接
        synchronized (TCPServer.this) {
            for (ClientHandle clientHandle : clients) {
                clientHandle.exit();
            }
            clients.clear();
        }
        //停止线程池
        forwardThreadPool.shutdownNow();
    }

     synchronized void broadcast(String str){
        //给所有相连的客户端都发送一条消息
        for (ClientHandle clientHandle:clients){
            clientHandle.send(str);
        }
    }


    //多客户端情况下，需要进行同步操作
    @Override
    public synchronized void onSelfClosed(ClientHandle clientHandle) {
        clients.remove(clientHandle);
    }

    @Override
    public void onNewMessageArrive(final ClientHandle clientHandle,final String msg) {
        System.out.print(">>:");

        //在这里对新消息进行转发到其它服务器，这里不能用synchronized来阻塞线程，因为
        //接收消息是很快的，如果阻塞线程有可能会让新消息丢失未被处理，所以需要将转发操作在新线程进行
        forwardThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (TCPServer.this){
                    for (ClientHandle handle:clients){
                        if(handle.equals(clientHandle)){
                            //如果是自己，就不用转发了
                            continue;
                        }
                        handle.send(msg);
                    }
                }
            }
        });
    }


    private class ClientListener extends Thread{
        private boolean done = false;

        @Override
        public void run() {
            super.run();
            Selector localSelector = TCPServer.this.selector;
            System.out.println("服务器已启动！");
            //等待客户端连接
            do {
                try {
                    if(localSelector.select()==0){
                        if(done){
                            break;
                        }
                        continue;
                    }
                    Iterator<SelectionKey> iterator = localSelector.selectedKeys().iterator();
                    while(iterator.hasNext()){
                        if(done){
                            break;
                        }
                        SelectionKey key = iterator.next();
                        iterator.remove();

                        //检查当前 key的状态是否是我们关注的客户端到达状态
                        if(key.isAcceptable()){
                            //这样我们就不用通过 serverSocket的accept()方法来阻塞监听客户端的到达
                            //直接有客户端到达事件时才处理
                            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                            //非阻塞状态拿到客户端连接
                            SocketChannel socketChannel = serverSocketChannel.accept();

                            //客户端构建异步线程
                            ClientHandle clientHandle = null;
                            try {
                                clientHandle = new ClientHandle(socketChannel,TCPServer.this,
                                        cacheDir);
                                //将clientHandle添加进列表,需要同步块
                                synchronized (TCPServer.this) {
                                    clients.add(clientHandle);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                System.out.println("客户端连接异常："+e.getMessage());
                            }
                        }
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            } while (!done);

            System.out.println("服务器已关闭！");
        }


        public void exit(){
            done = true;
            //唤醒当前Selector的select()阻塞,
            selector.wakeup();
        }
    }
}
