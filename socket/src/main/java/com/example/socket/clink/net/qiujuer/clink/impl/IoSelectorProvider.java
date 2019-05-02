package com.example.socket.clink.net.qiujuer.clink.impl;

import com.example.socket.clink.net.qiujuer.clink.core.IOProvider;
import com.example.socket.clink.net.qiujuer.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class IoSelectorProvider implements IOProvider {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    //因为 readSelector 和 writeSelector 的监听工作 select()分别开辟了新线程
    //而 readSelector 和 writeSelector 的 register 操作是在外面的线程中，会涉及线程安全
    //所以用 原子性的锁来确保在注册时
    private final AtomicBoolean inRegInput = new AtomicBoolean(false);
    private final AtomicBoolean inRegOutput = new AtomicBoolean(false);

    private final Selector readSelector;
    private final Selector writeSelector;

    private final HashMap<SelectionKey,Runnable> inputCallbackMap = new HashMap<>();
    private final HashMap<SelectionKey,Runnable> outputCallbackMap = new HashMap<>();

    private final ExecutorService inputHandPool;
    private final ExecutorService outputHandPool;

    public IoSelectorProvider() throws IOException{
        readSelector = Selector.open();
        writeSelector = Selector.open();

        inputHandPool = Executors.newFixedThreadPool(4,new MyThreadFactory(
                "IoProvider - Input - Thread -"));
        outputHandPool = Executors.newFixedThreadPool(4,new MyThreadFactory(
                "IoProvider - Input - Thread -"));


        //开启输入输出的监听
        startRead();
        startWrite();
    }

    private void startRead(){
        Thread thread = new Thread("Clink IoSelectorProvider Read Selector Thread"){
            @Override
            public void run() {
                super.run();
                while(!isClosed.get()){
                    try {
                        if (readSelector.select() == 0) {
                            //等待注册读事件完成
                            waitSelectionKeyRegister(inRegInput);
                            continue;
                        }

                        Set<SelectionKey> selectionKeys = readSelector.selectedKeys();
                        for (SelectionKey key : selectionKeys) {
                            if (key.isValid()) {
                                handleSelectionKey(key,SelectionKey.OP_READ,inputCallbackMap,
                                        inputHandPool);
                            }
                        }
                        //清空
                        selectionKeys.clear();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        };

        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }



    private void startWrite(){
        Thread thread = new Thread("Clink IoSelectorProvider Write Selector Thread"){
            @Override
            public void run() {
                super.run();
                while(!isClosed.get()){
                    try {
                        if (writeSelector.select() == 0) {
                            //等待注册写事件完成
                            waitSelectionKeyRegister(inRegOutput);
                            continue;
                        }

                        Set<SelectionKey> selectionKeys = writeSelector.selectedKeys();
                        for (SelectionKey key : selectionKeys) {
                            if (key.isValid()) {
                                handleSelectionKey(key,SelectionKey.OP_WRITE,outputCallbackMap,
                                        outputHandPool);
                            }
                        }
                        //清空
                        selectionKeys.clear();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        };

        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }



    @Override
    public boolean registerInput(SocketChannel channel, HandleInputCallback callback) {
        return register(channel,readSelector,SelectionKey.OP_READ,inRegInput,
                inputCallbackMap, callback) !=null;
    }

    @Override
    public boolean registerOutput(SocketChannel channel, HandleOutputCallback callback) {
        return register(channel,writeSelector,SelectionKey.OP_WRITE,inRegOutput,
                outputCallbackMap, callback) !=null;
    }

    @Override
    public void unregisterInput(SocketChannel channel) {
        unregister(channel,readSelector,inputCallbackMap);
    }

    @Override
    public void unregisterOutput(SocketChannel channel) {
        unregister(channel,writeSelector,outputCallbackMap);
    }

    @Override
    public void close() throws IOException {
        if(isClosed.compareAndSet(false,true)){
            //停掉线程池
            inputHandPool.shutdown();
            outputHandPool.shutdown();

            //清除待处理事件回调
            inputCallbackMap.clear();
            outputCallbackMap.clear();

            //停止阻塞，
            readSelector.wakeup();
            writeSelector.wakeup();

            CloseUtils.close(readSelector,writeSelector);
        }
    }


    private void waitSelectionKeyRegister(final AtomicBoolean locker){
        synchronized (locker){
            if(locker.get()){
                //如果当前为锁定状态,进行等待
                try {
                    locker.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    //注册事件方法
    private SelectionKey register(SocketChannel channel,Selector selector,int ops,AtomicBoolean locker,
                          HashMap<SelectionKey,Runnable> map,Runnable runnable){

        synchronized (locker){
            //设置锁定状态
            locker.set(true);
            try {
                //唤醒当前 selector,让 selector不处于select()阻塞状态，而是立即返回
                //这样新注册的事件才能够被下一轮的 select() 遍历到
                selector.wakeup();

                SelectionKey key = null;
                //如果channel注册过事件
                if(channel.isRegistered()){
                    //如果注册的key事件在 read 或 write 处理中被取消关注了，
                    //这时因为该key已经被注册过，所以不需要重复注册，只需要为
                    //这个key重新添加 ops事件 关注即可
                    key = channel.keyFor(selector);
                    if(key!=null) {
                        key.interestOps(key.interestOps() | ops);
                    }
                }
                if(key == null) {
                    //注册事件得到key
                    System.out.println("注册"+ops+"事件");
                    key = channel.register(selector, ops);
                    map.put(key,runnable);
                }
                return key;
            }catch (IOException e){
                return null;
            } finally {
                //解除锁定状态
                locker.set(false);
                try {
                    //这里之所以要捕获异常，是因为如果没有线程被阻塞，提醒会抛出异常
                    locker.notify();
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        }
    }


    //反注册事件方法
    private void unregister(SocketChannel channel,Selector selector, HashMap<SelectionKey,Runnable> map){
        if(channel.isRegistered()){
            SelectionKey key = channel.keyFor(selector);
            if(key!=null){
                //取消监听，这里的 cancel 是取消key中所有关注的事件，包括Readable,Writable事件
                //因为我们是读写的事件分别用两个 selector 分开管理，所以可以这样写，如果你将读写
                //事件注册到了同一个 Selector中时，用 key.cancel() 会将两个事件同时取消监听，所以
                //在注册到同一selector时，用 selectionKey.interestOps(selectionKey.interestOps() & ~ops);比较合适，
                //因为它只取消了指定事件的监听
                key.cancel();
                map.remove(key);
                //让selector的 select()方法立即返回，进行下一轮循环，
                //这样被 cancel 的key将不再被遍历
                selector.wakeup();
            }
        }
    }


    //处理selection Key
    private void handleSelectionKey(SelectionKey selectionKey,int ops,HashMap<SelectionKey,Runnable> map,
                            ExecutorService threadPool){
        //重点，将当前正在处理的selectionKey关联的读取事件取消关注，因为有可以这里的处理逻辑还没跑完，
        //而外层调用的 set.clear()操作将所有事件都移除了，select()又从关注事件中拉取可读或可写事件，
        //就会形成一个循环，造成大量事件的堆积
        selectionKey.interestOps(selectionKey.interestOps() & ~ops);  //对事件取消关注

        Runnable runnable = null;
        try {
            runnable = map.get(selectionKey);
        }catch (Exception ig){

        }
        if(runnable != null && !threadPool.isShutdown()){
            //异步调度
            threadPool.execute(runnable);
        }
    }




    static class MyThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        MyThreadFactory(String namePrefix) {
            SecurityManager var1 = System.getSecurityManager();
            this.group = var1 != null ? var1.getThreadGroup() : Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix;
        }

        public Thread newThread(Runnable var1) {
            Thread var2 = new Thread(this.group, var1, this.namePrefix + this.threadNumber.getAndIncrement(), 0L);
            if (var2.isDaemon()) {
                var2.setDaemon(false);
            }

            if (var2.getPriority() != 5) {
                var2.setPriority(5);
            }

            return var2;
        }
    }
}
