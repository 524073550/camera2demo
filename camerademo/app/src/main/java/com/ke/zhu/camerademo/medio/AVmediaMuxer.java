package com.ke.zhu.camerademo.medio;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import com.ke.zhu.camerademo.Callback;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

public class AVmediaMuxer {
    private String TAG = "AVmediaMuxer";
    private MediaMuxer mediaMuxer;
    private AudioRecord audioRecord;
    private int width;
    private int height;
    private VideoEncoder videoEncoder;
    private LinkedBlockingQueue<MuxerData> linkedBlockingQueue = new LinkedBlockingQueue();
    private int audioTrack;
    private boolean addAudioTrack;
    private int videoTrack;
    private boolean addvideoTrack;

    public AVmediaMuxer(String outfile, int width, int height) {
        this.width = width;
        this.height = height;
        initMuxer(outfile);
    }

    private boolean isRuning;

    private void initMuxer(String outfile) {
        try {
            Log.d(TAG, "创建媒体混合器 start...");
            mediaMuxer = new MediaMuxer(outfile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            Log.d(TAG, "创建媒体混合器 done...");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "创建媒体混合器 error: " + e.toString());
        }

        audioRecord = new AudioRecord();
        videoEncoder = new VideoEncoder(width, height, 30);
        setListener();

    }

    public void startVoideEncoder(byte[] buffer) {
        videoEncoder.putYUVData(buffer);
    }

    public void start() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                isRuning = true;
                while (isRuning) {
                    try {
                        if (addAudioTrack && addvideoTrack) {
                            MuxerData take = linkedBlockingQueue.take();
                            if (take != null) {
                                int track = -1;
                                if (take.trackIndex == 0) {
                                    track = videoTrack;
                                } else if (take.trackIndex == 1) {
                                    track = audioTrack;
                                }
                                Log.d(TAG, " " + track + "    写入混合数据大小 " + take.bufferInfo.size);
                                mediaMuxer.writeSampleData(track, take.byteBuf, take.bufferInfo);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                linkedBlockingQueue.clear();
                stopMediaMuxer();
            }
        }).start();

    }

    public void startEncoder() {
        audioRecord.startRecord();
        videoEncoder.startEncoder();
    }

    public void stopMediaMuxer() {
        addAudioTrack = false;
        addvideoTrack = false;
        mediaMuxer.stop();
        mediaMuxer.release();
        audioRecord.stopRecord();
        videoEncoder.stopRecord();

    }

    private void setListener() {
        audioRecord.setCallback(new Callback() {
            @Override
            public void mediaCallback(int trackIndex, ByteBuffer outBuf, MediaCodec.BufferInfo bufferInfo) {
                try {
                    linkedBlockingQueue.put(new MuxerData(trackIndex, outBuf, bufferInfo));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void outMediaFormat(int trackIndex, MediaFormat mediaFormat) {
                if (mediaMuxer != null) {
                    audioTrack = mediaMuxer.addTrack(mediaFormat);
                    addAudioTrack = true;
                }
                if (addAudioTrack && addvideoTrack) {
                    mediaMuxer.start();
                }
            }
        });

        videoEncoder.setCallcack(new Callback() {
            @Override
            public void mediaCallback(int trackIndex, ByteBuffer outBuf, MediaCodec.BufferInfo bufferInfo) {
                try {
                    linkedBlockingQueue.put(new MuxerData(trackIndex, outBuf, bufferInfo));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void outMediaFormat(int trackIndex, MediaFormat mediaFormat) {
                if (mediaMuxer != null) {
                    videoTrack = mediaMuxer.addTrack(mediaFormat);
                    addvideoTrack = true;
                }
                if (addAudioTrack && addvideoTrack) {
                    mediaMuxer.start();
                }
            }
        });
    }

    /**
     * 封装需要传输的数据类型
     */
    public static class MuxerData {
        int trackIndex;
        ByteBuffer byteBuf;
        MediaCodec.BufferInfo bufferInfo;

        public MuxerData(int trackIndex, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
            this.trackIndex = trackIndex;
            this.byteBuf = byteBuf;
            this.bufferInfo = bufferInfo;
        }
    }
}
