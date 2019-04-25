package com.example.socket.clink.net.qiujuer.clink.core;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver extends Closeable {
    boolean receiveAsync(IOArgs.IOArgsEventListener listener) throws IOException;
}
