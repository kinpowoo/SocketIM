package com.example.socket.clink.net.qiujuer.clink.utils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class FileUtils {
    //缓存公共目录
    private static final String CACHE_DIR = "cache";

    /**
     * 创建临时目录
     * @param dir
     * @return
     */
    public static File getCacheDir(String dir){
        String path = System.getProperty("user.dir")+File.separator+CACHE_DIR
                +File.separator+dir;
        File file = new File(path);
        if(!file.exists()){
            if(!file.mkdirs()){  //这里创建目录要连父目录也一并创建，避免出错
                throw new RuntimeException("创建临时目录失败:"+path);
            }
        }
        return file;
    }

    /**
     * 创建临时文件
     * @param path
     * @return
     */
    public static File createRandomTemp(File path){
        String fileName = UUID.randomUUID().toString()+".tmp";
        File file = new File(path,fileName);
        try {
            file.createNewFile();
        }catch (IOException e){
            e.printStackTrace();
        }
        return file;
    }

}
