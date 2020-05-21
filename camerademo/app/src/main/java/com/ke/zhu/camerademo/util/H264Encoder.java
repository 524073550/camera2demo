package com.ke.zhu.camerademo.util;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import com.ke.zhu.camerademo.JniUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;


public class H264Encoder {
    private int width;
    private int height;
    private int framerate;
    private LinkedBlockingQueue<byte[]> yuvQueue = new LinkedBlockingQueue<>(10);
    private MediaCodec mediaCodec;
    private final static int TIMEOUT_USEC = 12000;
    private byte[] configByte;
    private final MediaFormat videoFormat;

    public H264Encoder(int width, int height, int framerate) {
        this.width = width;
        this.height = height;
        this.framerate = framerate;
        videoFormat = MediaFormat.createVideoFormat("video/avc", width, height);
        //描述视频格式中内容的颜色格式的键。
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 5);
        //帧率
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);
        //关键帧
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
            mediaCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BufferedOutputStream bufferedOutputStream;

    public void setOutputStrem(BufferedOutputStream outputStrem) {
        bufferedOutputStream = outputStrem;
    }

    public void putYUVData(byte[] data) {
        if (data != null) {
            if (yuvQueue.size() >= 10) {
                yuvQueue.poll();
            }
            yuvQueue.add(data);
        }
    }

    boolean isRunning;


    public void startEncoder() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                isRunning = true;
                byte[] input = null;
                long pts = 0;
                long generateIndex = 0;

                while (isRunning) {

                    if (yuvQueue.size() > 0) {
                        //该数据格式必须为nv12格式
                        byte[] buffer = yuvQueue.poll();
                        byte[] nv12Data = new byte[width * height * 3 / 2];
                        JniUtils.I420ToNV12(buffer, width, height, nv12Data);
                        input = nv12Data;
                    }
                    if (input != null && mediaCodec != null && bufferedOutputStream != null) {
                        int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
                        if (inputBufferIndex >= 0) {
                            pts = computePresentationTime(generateIndex);
                            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                            inputBuffer.clear();
                            inputBuffer.put(input);
                            mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, pts, 0);
                        }
                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                        while (outputBufferIndex >= 0) {
                            ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
                            byte[] outData = new byte[bufferInfo.size];
                            outputBuffer.get(outData);
                            if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                                //初始化/编解码器特定数据，而不是媒体数据,
                                configByte = new byte[bufferInfo.size];
                                configByte = outData;
                            } else if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                                //关键帧信息
                                byte[] frameData = new byte[configByte.length + outData.length];
                                System.arraycopy(configByte, 0, frameData, 0, configByte.length);
                                System.arraycopy(outData, 0, frameData, configByte.length, outData.length);
                                try {
                                    bufferedOutputStream.write(frameData, 0, frameData.length);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                try {
                                    bufferedOutputStream.write(outData, 0, outData.length);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                        }

                    } else {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (mediaCodec != null) {
                    mediaCodec.stop();
                    mediaCodec.release();
                }
                if (bufferedOutputStream != null) {
                    try {
                        bufferedOutputStream.flush();
                        bufferedOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }).start();
    }

    /**
     * 根据帧数生成时间戳
     *
     * @param frameIndex
     * @return
     */
    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / framerate;
    }
}
