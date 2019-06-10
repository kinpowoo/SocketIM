package com.example.socket.clink.net.qiujuer.clink.impl.async;

import com.example.socket.clink.net.qiujuer.clink.core.Frame;
import com.example.socket.clink.net.qiujuer.clink.core.IoArgs;
import com.example.socket.clink.net.qiujuer.clink.core.SendPacket;
import com.example.socket.clink.net.qiujuer.clink.core.ds.BytePriorityNode;
import com.example.socket.clink.net.qiujuer.clink.frames.AbsSendPacketFrame;
import com.example.socket.clink.net.qiujuer.clink.frames.CancelSendFrame;
import com.example.socket.clink.net.qiujuer.clink.frames.SendEntityFrame;
import com.example.socket.clink.net.qiujuer.clink.frames.SendHeaderFrame;

import java.io.Closeable;
import java.io.IOException;

public class AsyncPacketReader implements Closeable {
    private volatile IoArgs args = new IoArgs();
    private final PacketProvider provider;

    private volatile BytePriorityNode<Frame> node;
    private volatile int nodeSize = 0;

    // 1,2,3..255,1,2,3..255 作为每一个帧的标识
    private short lastIdentifier = 0;  //每发送成功一个，自增1，到255后重置

    AsyncPacketReader(PacketProvider provider){
        this.provider = provider;
    }

    /**
     * 取消Packet对应帧的发送，如果当前Packet已发送部分数据（就算只是头数据）
     * 也应该在当前帧队列中发送一份取消发送的标志
     * @param packet
     */
    synchronized void cancel(SendPacket packet) {
        if (nodeSize == 0) {
            return;
        }

        for (BytePriorityNode<Frame> x = node, before = null;
             x != null;
             before = x, x = x.next) {
            Frame frame = x.item;
            if (frame instanceof AbsSendPacketFrame) {
                AbsSendPacketFrame packetFrame = (AbsSendPacketFrame) frame;
                if (packetFrame.getPacket() == packet) {
                    boolean removable = packetFrame.abort();
                    if (removable) {
                        // A B C, 移除B，将 A->next = C, before->next = x->next
                        removeFrame(x, before);
                        if (packetFrame instanceof SendHeaderFrame) {
                            //头帧，并且未被发送任何数据，直接取消
                            break;
                        }
                        //添加一个终止帧，通知接收方
                        CancelSendFrame cancelSendFrame = new CancelSendFrame(
                                packetFrame.getBodyIdentifier());
                        appendNewFrame(cancelSendFrame);
                        //意外终止，返回失败
                        provider.completedPacket(packet, false);
                        break;
                    }
                }
            }
        }
    }



    /**
     * 请求从 provider 队列中拿一份 Packet 进行发送
     * 如果当前 reader中有可以用于网络发送的数据，则返回True
     * @return
     */
    boolean requestTakePacket() {
        synchronized (this) {
            if (nodeSize >= 1) {
                return true;
            }
        }
        SendPacket packet = provider.takePacket();
        if(packet!=null){
            short identifier = generateIdentifier();
            SendHeaderFrame frame = new SendHeaderFrame(identifier,packet);
            appendNewFrame(frame);
        }
        synchronized (this) {
            return nodeSize != 0;
        }
    }



    @Override
    public synchronized void close(){
        while (node!=null){
            Frame frame = node.item;
            if(frame instanceof AbsSendPacketFrame){
                SendPacket packet = ((AbsSendPacketFrame)frame).getPacket();
                provider.completedPacket(packet,false);
            }
        }
        nodeSize = 0;
        node = null;
    }

    /**
     * 添加新帧
     * @param frame
     */
    private synchronized void appendNewFrame(Frame frame) {
        BytePriorityNode<Frame> newNode = new BytePriorityNode<>(frame);
        if(node!=null){
            //使用优先级别添加到链表
            node.appendWithPriority(newNode);
        }else{
            node = newNode;
        }
        nodeSize++;
    }

    /**
     * 从当前帧中填充数据到IoArgs中
     * 如果当前有可用于发送的帧，则填充数据并返回，如果填充失败则返回null
     * @return
     */
    IoArgs fillData() {
       Frame currentFrame =  getCurrentFrame();
       if(currentFrame == null){
           return null;
       }
       try {
           if(currentFrame.handle(args)){
               //消费完本帧
               //尝试基于本帧构建后续帧
               Frame nextFrame = currentFrame.nextFrame();
               if(nextFrame!=null){
                   appendNewFrame(nextFrame);
               }else if(currentFrame instanceof SendEntityFrame){
                   //如果当前是实体帧，而下一帧为null
                   //表明当前packet已发送完成
                   provider.completedPacket(((SendEntityFrame) currentFrame).getPacket(),
                           true);
               }
               //从链头弹出
               popCurrentFrame();
           }
           return args;
       }catch (IOException e){
           e.printStackTrace();
       }
       return null;
    }



    private short generateIdentifier(){
        short identifier = ++lastIdentifier;
        if(identifier == 255){
            lastIdentifier = 0;
        }
        return identifier;
    }

    private synchronized Frame getCurrentFrame() {
        if(node==null){
            return null;
        }
        return node.item;
    }

    //从队列中弹出当前帧
    private synchronized void popCurrentFrame() {
        node = node.next;
        nodeSize--;
        if(node == null){
            requestTakePacket();
        }
    }

    /**
     * 移除一帧
     * @param x  当前要移除的帧
     * @param before  当前移除帧的前一帧
     */
    private void removeFrame(BytePriorityNode<Frame> x, BytePriorityNode<Frame> before) {
        if(before == null){
            //如果前一帧为null，说明x为首帧
            node = x.next;
        }else {
            before.next = x.next;
        }
        nodeSize--;
        if(node == null){
            requestTakePacket();
        }
    }


    interface PacketProvider{
        SendPacket takePacket();
        void completedPacket(SendPacket packet,boolean isSucceed);
    }
}
