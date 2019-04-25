package com.example.socket.clink.net.qiujuer.clink.core;

import com.example.socket.clink.net.qiujuer.clink.utils.CloseUtils;

import java.io.Closeable;
import java.io.IOException;

public class IOContext implements Closeable {

    private static IOContext instance;
    private final IOProvider ioProvider;

    private IOContext(IOProvider ioProvider){
        this.ioProvider = ioProvider;
    }

    public IOProvider getIoProvider(){
        return ioProvider;
    }

    public static IOContext get(){
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
        private IOProvider ioProvider;

        private StartedBoot(){

        }

        public StartedBoot ioProvider(IOProvider ioProvider){
            this.ioProvider = ioProvider;
            return this;
        }

        public IOContext start(){
            instance = new IOContext(ioProvider);
            return instance;
        }
    }
}
