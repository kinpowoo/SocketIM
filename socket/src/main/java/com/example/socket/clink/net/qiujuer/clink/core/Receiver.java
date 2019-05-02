package com.example.socket.clink.net.qiujuer.clink.core;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver extends Closeable {
    void setReceiveListener(IOArgs.IOArgsEventListener listener);
    boolean receiveAsync(IOArgs ioArgs) throws IOException;
}
