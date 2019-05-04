package com.example.socket.clink.net.qiujuer.clink.core;

import java.io.Closeable;
import java.io.IOException;

public class IoContext implements Closeable {

    private static IoContext instance;
    private final IoProvider ioProvider;

    private IoContext(IoProvider ioProvider){
        this.ioProvider = ioProvider;
    }

    public IoProvider getIoProvider(){
        return ioProvider;
    }

    public static IoContext get(){
        return instance;
    }

    public static StartedBoot setup(){
        return new StartedBoot();
    }

    @Override
    public void close() throws IOException{
        ioProvider.close();
    }

    public static class StartedBoot{
        private IoProvider ioProvider;

        private StartedBoot(){

        }

        public StartedBoot ioProvider(IoProvider ioProvider){
            this.ioProvider = ioProvider;
            return this;
        }

        public IoContext start(){
            instance = new IoContext(ioProvider);
            return instance;
        }
    }
}
