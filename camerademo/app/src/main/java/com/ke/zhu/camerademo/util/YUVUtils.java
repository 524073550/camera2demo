package com.ke.zhu.camerademo.util;

public class YUVUtils {

    public static byte[] getNV21(byte[] y,byte[] uv){
        if (y==null||uv==null){
            return null;
        }
        byte[] na21 = new byte[y.length+uv.length];
        System.arraycopy(y, 0, na21, 0, y.length);
        System.arraycopy(uv, 0, na21, y.length, uv.length);
        return na21;
    }
}
