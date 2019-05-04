package com.example.socket.clink.net.qiujuer.clink.core;

import java.io.Closeable;
import java.io.IOException;

public interface Sender extends Closeable {
    //设置回调
    void setSendListener(IoArgs.IOArgsEventProcessor processor);

    //发送的异步实现
    boolean postSendAsync() throws IOException;
}
