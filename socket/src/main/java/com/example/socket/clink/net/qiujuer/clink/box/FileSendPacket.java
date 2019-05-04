package com.example.socket.clink.net.qiujuer.clink.box;

import com.example.socket.clink.net.qiujuer.clink.core.SendPacket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;


public class FileSendPacket extends SendPacket<FileInputStream> {
    private File file;

    public FileSendPacket(File file) {
        this.file = file;
        this.length = file.length();
    }

    @Override
    public byte type() {
        return TYPE_STREAM_FILE;
    }

    /**
     * 使用File构建文件读取流
     * @return
     */
    @Override
    protected FileInputStream createStream() {
        try {
            return new FileInputStream(this.file);
        }catch (FileNotFoundException e){
            return null;
        }
    }
}
