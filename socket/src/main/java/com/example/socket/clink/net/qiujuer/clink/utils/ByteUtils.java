package com.example.socket.clink.net.qiujuer.clink.utils;

public class ByteUtils {
    //是否以某组 byte[] 数据开头
    public static boolean startWith(byte[] src,byte[] start){
        boolean isStartWith = true;
        for(int i=0;i<start.length;i++){
            if(src[i]!=start[i]){
                isStartWith = false;
            }
        }
        return isStartWith;
    }

}
