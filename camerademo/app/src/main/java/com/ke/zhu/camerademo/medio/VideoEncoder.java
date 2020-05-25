package com.ke.zhu.camerademo.medio;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.ke.zhu.camerademo.Callback;
import com.ke.zhu.camerademo.JniUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

public class VideoEncoder {
    private int width;
    private int height;
    private int framerate;
    private LinkedBlockingQueue<byte[]> yuvQueue = new LinkedBlockingQueue<>(10);
    private MediaCodec mediaCodec;
    private final static int TIMEOUT_USEC = 12000;
    private byte[] configByte;
    private final MediaFormat videoFormat;
    private long presentationTimeUs;


    public VideoEncoder(int width, int height, int framerate) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void putYUVData(byte[] data) {
        if (data != null) {
            if (yuvQueue.size() >= 10) {
                yuvQueue.poll();
            }
            yuvQueue.add(data);
        }
    }


    private boolean isRunning;
    private boolean vEncoderEnd;


    public void startEncoder() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mediaCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                mediaCodec.start();
                isRunning = true;
                vEncoderEnd = false;
                byte[] input = null;

                long generateIndex = 0;
                presentationTimeUs = System.currentTimeMillis() * 1000;
                while (isRunning) {
                    if (yuvQueue.size() > 0) {
                        //该数据格式必须为nv12格式
                        byte[] buffer = yuvQueue.poll();
                        byte[] nv12Data = new byte[width * height * 3 / 2];
                        JniUtils.I420ToNV12(buffer, width, height, nv12Data);
                        input = nv12Data;
                    }
                    if (input != null && mediaCodec != null) {
                        int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
                        if (inputBufferIndex >= 0) {
                            long pts = System.currentTimeMillis() * 1000 - presentationTimeUs;
                            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                            inputBuffer.clear();
                            inputBuffer.put(input);
                            if (vEncoderEnd) {
                                Log.e("视频结束","视频结束帧");
                                mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            } else {
                                mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, pts, 0);
                            }
                        }
                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);

                        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            // Subsequent data will conform to new format.
                            MediaFormat format = mediaCodec.getOutputFormat();
                            if (callcack != null) {
                                callcack.outMediaFormat(0, format);
                            }
                        }
                        while (outputBufferIndex >= 0) {
                            ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
//                            byte[] outData = new byte[bufferInfo.size];
//                            outputBuffer.get(outData);

                            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                // You shoud set output format to muxer here when you target Android4.3 or less
                                // but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
                                // therefor we should expand and prepare output format from buffer data.
                                // This sample is for API>=18(>=Android 4.3), just ignore this flag here

                                bufferInfo.size = 0;
                            }
                            if (bufferInfo.size != 0 && !vEncoderEnd) {
                                callcack.mediaCallback(0, outputBuffer, bufferInfo);
                            }
                            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);

                            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                isRunning = false;
                                return;
                            }
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
            }
        }).start();
    }

    private Callback callcack;

    public void setCallcack(Callback callcack) {
        this.callcack = callcack;
    }

    public void stopRecord() {
        vEncoderEnd = true;
    }
}
