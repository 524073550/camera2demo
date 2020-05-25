package com.ke.zhu.camerademo.util;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import com.ke.zhu.camerademo.com.Contans;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

public class AVMuxer {
    private String TAG = "AVMuxer";
    private AudioRecording audioRecording;
    private AVEncoder avEncoder;
    private MediaMuxer mediaMuxer;
    private int audi0TrackIndex;
    private int videoTrackIndex;
    private boolean audioFormat;
    private boolean videoFormat;
    private boolean isInit;
    private LinkedBlockingQueue<MuxerData> muxerData = new LinkedBlockingQueue<>();
    private boolean avMuxerRuning;
    private Thread thread;

    public AVMuxer() {
    }

    public void initMediaMuxer(String path) {
        if (isInit){
            Log.e(TAG,"已经过创建媒体合成器");
            return;
        }
        try {
            mediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            Log.e(TAG, "创建媒体合成器失败");
            e.printStackTrace();
        }
        Log.e(TAG, "创建媒体合成器成功");
        audioRecording = new AudioRecording();
        avEncoder = new AVEncoder();
        setListener();

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (avMuxerRuning && !Thread.interrupted()) {
                    try {
                        if (audioFormat && videoFormat) {
                            MuxerData data = muxerData.poll();
                            if (data==null){
                                continue;
                            }
                            int track = -1;
                            if (data.trackIndex == Contans.VIDEOTRACK) {
                                track = videoTrackIndex;
                            } else if (data.trackIndex == Contans.AUDIOTRACK) {
                                track = audi0TrackIndex;
                            }
//                            Log.e(TAG, data.toString());
                            mediaMuxer.writeSampleData(track, data.byteBuffer, data.bufferInfo);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                muxerData.clear();
                mediaMuxer.stop();
                mediaMuxer.release();

                Log.e(TAG, "停止音视频合成");
            }
        });
        thread.start();
        avMuxerRuning = true;
    }

    public void initEncoder(int width, int height) {
        if (!isInit) {
            audioRecording.initAudioRecording();
            avEncoder.initVideoEncoder(width, height, 30);
            avEncoder.initAudioEncoder(audioRecording.getSampleRateInHz(), audioRecording.getChannelConfig(), audioRecording.getPcmFormat());
            isInit = true;
        }
    }

    public void putVideoData(byte[] buffer) {
        if (avEncoder != null) {
            avEncoder.putVideoData(buffer);
        }
    }

    public void start() {
        audioRecording.startRecord();
        avEncoder.startEncoder();
    }

    public void stop() {
        avEncoder.stopEncoder();
        audioRecording.stopRecording();
        avMuxerRuning = false;
        audioFormat = false;
        videoFormat = false;
        thread.interrupt();
    }

    private void setListener() {
        audioRecording.setCallback(new AudioRecording.AudioRecordCallback() {
            @Override
            public void callback(byte[] buffer) {
                if (avEncoder != null)
                    avEncoder.putAudioData(buffer);
            }
        });

        avEncoder.setCallback(new AVEncoder.Callback() {
            @Override
            public void mediaFormatCallback(int trackIndex, MediaFormat mediaFormat) {
                if (Contans.AUDIOTRACK == trackIndex) {
                    if (mediaMuxer != null) {
                        audi0TrackIndex = mediaMuxer.addTrack(mediaFormat);
                        audioFormat = true;
                    }
                } else if (Contans.VIDEOTRACK == trackIndex) {
                    if (mediaMuxer != null) {
                        videoTrackIndex = mediaMuxer.addTrack(mediaFormat);
                        videoFormat = true;
                    }
                }

                if (videoFormat && audioFormat) {
                    mediaMuxer.start();
                    Log.e(TAG, "开始合成音视频");
                }
            }

            @Override
            public void trackVideoCallBack(int trackIndex, ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
                if (mediaMuxer!=null){
                    muxerData.add(new MuxerData(trackIndex,byteBuffer,bufferInfo));
                }
            }

            @Override
            public void trackAudioCallBack(int trackIndex, ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
                if (mediaMuxer!=null){
                    muxerData.add(new MuxerData(trackIndex,byteBuffer,bufferInfo));
                }
            }


        });
    }


    public class MuxerData {
        private int trackIndex;
        private ByteBuffer byteBuffer;
        private MediaCodec.BufferInfo bufferInfo;

        public MuxerData(int trackIndex, ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
            this.trackIndex = trackIndex;
            this.byteBuffer = byteBuffer;
            this.bufferInfo = bufferInfo;
        }

        @Override
        public String toString() {
            return "MuxerData{" +
                    "trackIndex=" + trackIndex +
                    ", byteBuffer=" + byteBuffer.remaining() +
                    ", bufferInfo=" + bufferInfo.size +
                    '}';
        }
    }
}
