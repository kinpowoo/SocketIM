package com.example.socket.clink.net.qiujuer.clink.impl.async;

import com.example.socket.clink.net.qiujuer.clink.core.Frame;
import com.example.socket.clink.net.qiujuer.clink.core.IoArgs;
import com.example.socket.clink.net.qiujuer.clink.core.Packet;
import com.example.socket.clink.net.qiujuer.clink.core.ReceivePacket;
import com.example.socket.clink.net.qiujuer.clink.frames.AbsReceiveFrame;
import com.example.socket.clink.net.qiujuer.clink.frames.CancelReceiveFrame;
import com.example.socket.clink.net.qiujuer.clink.frames.ReceiveEntityFrame;
import com.example.socket.clink.net.qiujuer.clink.frames.ReceiveFrameFactory;
import com.example.socket.clink.net.qiujuer.clink.frames.ReceiveHeaderFrame;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.HashMap;

/**
 *  接收帧操作，可能出现的情况，Packet1 和 Packet2 被同时发送
 *  Packet1有5帧，Packet2有2帧，在发送了Packet1的2帧后，发送了Packet2
 *  的第一帧，接着又发送Packet1的第3帧，接着Packet2的第2帧，然后是
 *  Packet1的第4,5帧，这种情况下我们要组合这些帧到各自的Packet中，需要
 *  一个 HashMap 来维护各个 Packet
 */
public class AsyncPacketWriter implements Closeable {
    private final PacketProvider provider;
    // short 是 identifier 标识
    private final HashMap<Short,PacketModel> packetMap = new HashMap<>();
    private final IoArgs ioArgs = new IoArgs();
    private volatile Frame frameTemp;  //当前接收帧


    AsyncPacketWriter(PacketProvider provider){
        this.provider = provider;
    }


    /**
     * 构建一份数据容纳封装
     * 当前帧如果没有则返回至少 6 字节长度的IoArgs
     * 如果当前帧有可消费数据，则返回当前帧未消费完成的长度
     * @return
     */
    synchronized IoArgs takeIoArgs() {
        ioArgs.setLimit(frameTemp==null?
                Frame.FRAME_HEADER_LENGTH:frameTemp.getConsumableLength());
        return ioArgs;
    }

    /**
     * 消费 IoArgs中的数据
     * @param args
     */
    synchronized void consumeIoArgs(IoArgs args) {
        if(frameTemp == null){
            Frame temp;
            do{
                temp = buildNewFrame(args);;
            }while (temp == null && args.remained());

            if(temp == null){
                return;
            }
            frameTemp = temp;
            if(!args.remained()){
                return;
            }
        }

        Frame currentFrame = frameTemp;
        do{
            try {
                if(currentFrame.handle(args)){
                    if(currentFrame instanceof ReceiveHeaderFrame){
                        ReceiveHeaderFrame headerFrame = (ReceiveHeaderFrame) currentFrame;
                        //如果是头帧，构建一个Packet，用来接收后续数据帧
                        ReceivePacket packet = provider.takePacket(headerFrame.getPacketType(),
                                headerFrame.getPacketLength(),headerFrame.getPacketHeaderInfo());
                        //将packet放入HashMap,以便进行后面帧接收的维护
                        appendNewPacket(headerFrame.getBodyIdentifier(),packet);
                    }else if(currentFrame instanceof ReceiveEntityFrame){
                        //如果是实体帧，
                        completeEntityFrame((ReceiveEntityFrame)currentFrame);
                    }
                    frameTemp = null;
                    break;
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }while (args.remained());
    }

    /**
     *
     * @param currentFrame
     */
    private void completeEntityFrame(ReceiveEntityFrame currentFrame) {
        synchronized (packetMap) {
            short identifier = currentFrame.getBodyIdentifier();
            int length = currentFrame.getBodyLength();
            PacketModel model = packetMap.get(identifier);
            //model代表一个Packet,将model中Packet的长度递减每一帧的长度，得到
            //剩余未接收长度
            model.unReceiveLength -= length;
            if (model.unReceiveLength <= 0) {
                //代表已经接收完成，通知完成
                provider.completedPacket(model.packet, true);
                //并将当前 Packet 从 HashMap 中移除
                packetMap.remove(identifier);
            }
        }
    }

    /**
     * 将 Packet 根据 identifier 来放入HashMap,放入之前要对 Packet 封装成 PacketModel
     * @param bodyIdentifier
     * @param packet
     */
    private void appendNewPacket(short bodyIdentifier, ReceivePacket packet) {
        synchronized (packetMap) {
            PacketModel model = new PacketModel(packet);
            packetMap.put(bodyIdentifier, model);
        }
    }

    /**
     * 构建一个新的帧
     * @param args
     * @return
     */
    private Frame buildNewFrame(IoArgs args) {
        AbsReceiveFrame frame = ReceiveFrameFactory.createInstance(args);
        if(frame instanceof CancelReceiveFrame){
            cancelReceivePacket(frame.getBodyIdentifier());
            return null;
        }else if(frame instanceof ReceiveEntityFrame){
            WritableByteChannel channel = getPacketChannel(frame.getBodyIdentifier());
            ((ReceiveEntityFrame)frame).bindPacketChannel(channel);
        }
        return frame;
    }


    /**
     * 根据帧的唯一标识获取通道
     * @param bodyIdentifier
     * @return
     */
    private WritableByteChannel getPacketChannel(short bodyIdentifier) {
        synchronized (packetMap) {
            //当close异常关闭时，可能无法得到 model
            PacketModel model = packetMap.get(bodyIdentifier);
            return model==null?null:model.channel;
        }
    }

    /**
     * 取消接收Packet
     * @param bodyIdentifier
     */
    private void cancelReceivePacket(short bodyIdentifier) {
        synchronized (packetMap) {
            PacketModel model = packetMap.get(bodyIdentifier);
            if (model != null) {
                ReceivePacket packet = model.packet;
                //强制完成
                provider.completedPacket(packet, false);
            }
        }
    }

    /**
     * 并闭操作，关闭时若当前还有正在接收的Packet,
     * 则尝试停止对应的Packet接收
     * @throws IOException
     */
    @Override
    public void close() {
        synchronized(packetMap) {
            Collection<PacketModel> values = packetMap.values();
            for (PacketModel v : values) {
                //通知强制完成
                provider.completedPacket(v.packet, false);
            }
            //清空map
            packetMap.clear();
        }
    }


    interface PacketProvider{
        ReceivePacket takePacket(byte type,long length,byte[] headerInfo);
        void completedPacket(ReceivePacket packet,boolean isSucceed);
    }


    /**
     * 对接收包的封装，比如接收的进度，channel通道管理
     */
    static class PacketModel{
        final ReceivePacket packet;
        final WritableByteChannel channel;
        volatile long unReceiveLength;

        PacketModel(ReceivePacket<?,?> packet){
            this.packet = packet;
            this.channel = Channels.newChannel(packet.open());
            this.unReceiveLength = packet.length();
        }
    }
}
