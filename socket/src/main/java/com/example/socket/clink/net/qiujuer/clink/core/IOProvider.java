package com.example.socket.clink.net.qiujuer.clink.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

public interface IOProvider extends Closeable {

    boolean registerInput(SocketChannel channel,HandleInputCallback callback);
    boolean registerOutput(SocketChannel channel,HandleOutputCallback callback);

    void unregisterInput(SocketChannel channel);
    void unregisterOutput(SocketChannel channel);


    abstract class HandleInputCallback implements Runnable{
        @Override
        public final void run(){
            canProviderInput();
        }

        protected abstract void canProviderInput();
    }

    abstract class HandleOutputCallback implements Runnable{
        private Object attach;

        @Override
        public final void run(){
            canProviderOutput(attach);
        }

        public final void setAttach(Object attach){
            this.attach = attach;
        }

        protected abstract void canProviderOutput(Object attach);
    }
}
