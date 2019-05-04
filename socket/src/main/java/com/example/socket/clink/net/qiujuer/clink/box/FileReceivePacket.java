package com.example.socket.clink.net.qiujuer.clink.box;

import com.example.socket.clink.net.qiujuer.clink.core.ReceivePacket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * 文件接收包
 */
public class FileReceivePacket extends ReceivePacket<FileOutputStream,File> {

    private File file;

    public FileReceivePacket(long len, File file) {
        super(len);
        this.file = file;
    }

    @Override
    public byte type() {
        return TYPE_STREAM_FILE;
    }

    /**
     * 从流转变为对应实体时直接返回创建时传入的File文件
     *
     * @param stream {@link FileOutputStream}
     * @return
     */
    @Override
    protected File buildEntity(FileOutputStream stream) {
        return file;
    }

    @Override
    protected FileOutputStream createStream() {
        try {
            return new FileOutputStream(file);
        }catch (FileNotFoundException e){
            return null;
        }
    }
}
