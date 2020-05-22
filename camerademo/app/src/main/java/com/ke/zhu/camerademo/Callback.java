package com.ke.zhu.camerademo;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

public interface Callback {
    void mediaCallback(int trackIndex, ByteBuffer outBuf, MediaCodec.BufferInfo bufferInfo);

    void outMediaFormat(int trackIndex, MediaFormat mediaFormat);
}
