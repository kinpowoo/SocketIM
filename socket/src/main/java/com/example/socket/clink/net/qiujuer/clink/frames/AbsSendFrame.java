package com.example.socket.clink.net.qiujuer.clink.frames;


import com.example.socket.clink.net.qiujuer.clink.core.Frame;
import com.example.socket.clink.net.qiujuer.clink.core.IoArgs;

import java.io.IOException;

public abstract class AbsSendFrame extends Frame {
    //volatile表示该变量多线程可见
    volatile byte headerRemaining = Frame.FRAME_HEADER_LENGTH;
    volatile int bodyRemaining;

    public AbsSendFrame(int length,byte type,byte flag,short identifier){
        super(length,type,flag,identifier);
        bodyRemaining = length;
    }

    //消费数据，同时间内只允许一条线程对其进行消费
    @Override
    public synchronized boolean handle(IoArgs ioArgs) throws IOException {
        try {
            ioArgs.setLimit(headerRemaining + bodyRemaining);
            ioArgs.startWriting();
            //如果ioArgs有可写空间，且头部信息还没有写入完成，进行头部数据写入
            if (headerRemaining > 0 && ioArgs.remained()) {
                headerRemaining -= consumeHeader(ioArgs);
            }
            //如果头部数据已写完，ioArgs还有可写入空间，且数据体还有数据，写入数据体
            if (headerRemaining == 0 && ioArgs.remained() && bodyRemaining > 0) {
                bodyRemaining -= consumeBody(ioArgs);
            }
            return headerRemaining == 0 && bodyRemaining == 0;
        }finally {
            ioArgs.finishWriting();
        }
    }

    protected abstract int consumeBody(IoArgs ioArgs) throws IOException;

    private int consumeHeader(IoArgs ioArgs) {
        int count  = headerRemaining;
        int offset = header.length - count;
        return (byte)ioArgs.readFrom(header,offset,count);
    }


    synchronized boolean isSending(){
        return headerRemaining < Frame.FRAME_HEADER_LENGTH;
    }

    @Override
    public synchronized int getConsumableLength() {
        return headerRemaining+bodyRemaining;
    }
}
