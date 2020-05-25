package com.ke.zhu.camerademo.medio;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import com.ke.zhu.camerademo.Callback;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioRecord {
    private Integer mRecordBufferSize;
    private String TAG = "AudioRecord";
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";
    //采样率
    private int sampleRateInHz = 44100;
    //声道
    private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_STEREO;

    //音频格式
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    //比特率
    private static final int BIT_RATE = 64000*16*3;

    private final int TIMEOUT_USEC = 10000;

    private android.media.AudioRecord mAudioRecord;
    private boolean mWhetherRecord;
    private Callback callback;
    private MediaCodec mediaCodec;
    private long presentationTimeUs;
    private boolean isRuning;

    public AudioRecord() {
        initAudioRecord();
    }

    private void initAudioRecord() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        mRecordBufferSize = android.media.AudioRecord.getMinBufferSize(sampleRateInHz
                , channelConfig
                , audioFormat);
        mAudioRecord = new android.media.AudioRecord(MediaRecorder.AudioSource.MIC
                , sampleRateInHz
                , channelConfig
                , audioFormat
                , mRecordBufferSize);

        MediaFormat audioFormat = MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, sampleRateInHz, channelConfig);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_STEREO);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelConfig);
        audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRateInHz);

        try {
            mediaCodec = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
            mediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void startRecord() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mAudioRecord != null) {
                    mAudioRecord.startRecording();//开始录制
                }
                if (mediaCodec != null) {
                    mediaCodec.start();
                }
                presentationTimeUs = System.currentTimeMillis() * 1000;
                try {
                    int buffSize =  Math.min(4096, mRecordBufferSize);
                    byte[] bytes = new byte[buffSize];
                    while (!isRuning) {
                        mAudioRecord.read(bytes, 0, buffSize);//读取流
                        int inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
                        if (inputBufferIndex >= 0) {
                            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                            inputBuffer.clear();
                            inputBuffer.put(bytes);
                        }
                        //计算pts，这个值是一定要设置的
                        long pts = System.currentTimeMillis() * 1000 - presentationTimeUs;
                        //停止
                        if (mWhetherRecord) {
                            mediaCodec.queueInputBuffer(inputBufferIndex, 0, bytes.length, pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        } else {
                            mediaCodec.queueInputBuffer(inputBufferIndex, 0, bytes.length, pts, 0);
                        }
                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            // Subsequent data will conform to new format.
                            MediaFormat format = mediaCodec.getOutputFormat();
                            if (callback!=null)
                            callback.outMediaFormat(1,format);
                        }

                        while (outputBufferIndex >= 0) {
                            ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
                            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                // You shoud set output format to muxer here when you target Android4.3 or less
                                // but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
                                // therefor we should expand and prepare output format from buffer data.
                                // This sample is for API>=18(>=Android 4.3), just ignore this flag here
                                Log.d(TAG, "drain:BUFFER_FLAG_CODEC_CONFIG===");
                                bufferInfo.size = 0;
                            }
                            if (bufferInfo.size!=0&&!mWhetherRecord){
                                callback.mediaCallback(1,outputBuffer,bufferInfo);
                            }

                            mediaCodec.releaseOutputBuffer(outputBufferIndex,false);
                            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                Log.e(TAG, "Recv Audio Encoder===BUFFER_FLAG_END_OF_STREAM=====");
                                isRuning = true;

                            }
                        }
                    }
                    mAudioRecord.stop();//停止录制
                    mAudioRecord.release();
                    Log.e(TAG,"停止录音");
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    public void stopRecord() {
            mWhetherRecord = true;
    }

    public int getSampleRateInHz() {
        return sampleRateInHz;
    }

    public int getChannelConfig() {
        return channelConfig;
    }

    public int getAudioFormat() {
        return audioFormat;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }


}
