package com.ke.zhu.camerademo.util;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

import com.ke.zhu.camerademo.JniUtils;
import com.ke.zhu.camerademo.com.Contans;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

public class AVEncoder {
    private String TAG = "AVEncoder";
    private MediaCodec videoEncoder;
    private String VIDEO_MIME_TYPE = "video/avc"; // H.264 Advanced Video

    private int width;
    private int height;
    private int FRAME_INTERVAL = 5;
    private int colorFormat = 0;

    private MediaCodec audioEncoder;
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm"; //

    private boolean videoEncoderStart;
    private boolean videoEncoderEnd;
    private boolean audioEncoderStart;
    private boolean audioEncoderEnd;
    private Thread videoThread;
    private MediaFormat videoFormat;
    private long timeoutUs = 10000;
    private long presentationTimeUs;
    private LinkedBlockingQueue<byte[]> videoQueue = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
    private MediaFormat audioFormat;
    private Thread audioEncoderThread;

    public void initVideoEncoder(int width, int height, int fps) {
        //注意给过来的数据是旋转了90度的数据,宽高是相反的
        this.width = width;
        this.height = height;

        //选择系统用于编码H264的编码器信息
        MediaCodecInfo mediaCodecInfo = selectCodec(VIDEO_MIME_TYPE);
        if (mediaCodecInfo == null) {
            Log.e(TAG, "设备不支持的格式 ===" + VIDEO_MIME_TYPE);
            return;
        }

        //根据MIME格式,选择颜色格式
        selectColorFormat(mediaCodecInfo, VIDEO_MIME_TYPE);
        if (colorFormat == 0) {
            return;
        }

        videoFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, width, height);
        //设置比特率
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 5);
        //设置帧率
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
        //设置支持的颜色
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        //设置关键帧时间
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, FRAME_INTERVAL);
        try {
            videoEncoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
        } catch (IOException e) {
            Log.e(TAG, "视频编码器创建失败");
            e.printStackTrace();
        }
        Log.e(TAG, "视频编码器创建成功");
    }


    public void initAudioEncoder(int sampleRate, int channelCount, int audioFrom) {
        MediaCodecInfo mediaCodecInfo = selectCodec(AUDIO_MIME_TYPE);
        if (mediaCodecInfo == null) {
            Log.e(TAG, "设备不支持的格式 ===" + AUDIO_MIME_TYPE);
            return;
        }
        int rate = audioFrom * sampleRate * channelCount;
        //channelCount != channelConfig 此处channelCount表示音频的通道数 如单声道为1 双声道为2
        audioFormat = MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, sampleRate, channelCount);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_STEREO);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, rate);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelCount);
        audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
        Log.e(TAG, "format === " + audioFormat.toString());
        try {
            audioEncoder = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void selectColorFormat(MediaCodecInfo mediaCodecInfo, String videoMimeType) {
        MediaCodecInfo.CodecCapabilities capabilitiesForType = mediaCodecInfo.getCapabilitiesForType(videoMimeType);
        int[] colorFormats = capabilitiesForType.colorFormats;
        for (int i = 0; i < colorFormats.length; i++) {
            boolean recognizedFormat = isRecognizedFormat(colorFormats[i]);
            if (recognizedFormat) {
                colorFormat = colorFormats[i];
                break;
            }
        }
    }

    private boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            // these are the formats we know how to handle for this test
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar://对应Camera预览格式I420(YV21/YUV420P)
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar: //对应Camera预览格式NV12
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar://对应Camera预览格式NV21
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar: {////对应Camera预览格式YV12
                return true;
            }
            default:
                return false;
        }
    }

    private MediaCodecInfo selectCodec(String videoMimeType) {
        int codecCount = MediaCodecList.getCodecCount();
        for (int i = 0; i < codecCount; i++) {
            MediaCodecInfo codecInfoAt = MediaCodecList.getCodecInfoAt(i);
            boolean encoder = codecInfoAt.isEncoder();
            if (!encoder) {
                continue;
            }
            String[] supportedTypes = codecInfoAt.getSupportedTypes();
            for (int j = 0; j < supportedTypes.length; j++) {
                if (supportedTypes[j].equalsIgnoreCase(videoMimeType)) {
                    return codecInfoAt;
                }
            }
        }
        return null;
    }

    public void putVideoData(byte[] buffer) {
        if (videoEncoderStart) {
            videoQueue.add(buffer);
        }
    }

    public void putAudioData(byte[] buffer) {
        audioQueue.add(buffer);
    }


    public void startEncoder() {
        startAudioEncoder();
        startVideoEncoder();
    }

    public void stopEncoder() {
        videoEncoderEnd = true;
        audioEncoderEnd = true;
    }

    private void startVideoEncoder() {
        if (videoEncoderStart) {
            Log.e(TAG, "视频正在录制");
            return;
        }
        videoThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (videoEncoder != null) {
                    videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                    videoEncoder.start();
                }
                videoEncoderStart = true;
                presentationTimeUs = System.currentTimeMillis() * 1000;

                while (videoEncoderStart) {
                    try {
                        //传进来的格式为I420
                        byte[] data = videoQueue.poll();
                        if (data != null) {
                            byte[] videoDst = new byte[width * height * 3 / 2];
                            JniUtils.I420ToNV12(data, width, height, videoDst);
                            /*switch (colorFormat) {
                                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar://对应Camera预览格式I420(YV21/YUV420P)
                                    break;
                                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar: //对应Camera预览格式NV12
                                    JniUtils.I420ToNV12(data,width,height,videoDst);
                                    break;
                                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar://对应Camera预览格式NV21
                                    JniUtils.yuvI420ToNV21(data,videoDst,width,height);
                                    break;
                                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar: //对应Camera预览格式YV12

                                    break;

                                default:
                                    break;
                            }*/
                            int inputBufferIndex = videoEncoder.dequeueInputBuffer(timeoutUs);
                            if (inputBufferIndex >= 0) {
                                ByteBuffer inputBuffer = videoEncoder.getInputBuffer(inputBufferIndex);
                                inputBuffer.clear();
                                inputBuffer.put(videoDst);
                                long pst = System.currentTimeMillis() * 1000 - presentationTimeUs;
                                if (videoEncoderEnd) {
                                    Log.e(TAG, "视频数据结束帧");
                                    videoEncoder.queueInputBuffer(inputBufferIndex, 0, videoDst.length, pst, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                } else {
                                    videoEncoder.queueInputBuffer(inputBufferIndex, 0, videoDst.length, pst, 0);
                                }
                                MediaCodec.BufferInfo mediaCodecInfo = new MediaCodec.BufferInfo();
                                int outputBufferIndex = videoEncoder.dequeueOutputBuffer(mediaCodecInfo, timeoutUs);
                                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                    MediaFormat outputFormat = videoEncoder.getOutputFormat();
                                    if (callback != null) {
                                        callback.mediaFormatCallback(Contans.VIDEOTRACK, outputFormat);
                                    }
                                }

                                while (outputBufferIndex >= 0) {
                                    ByteBuffer outputBuffer = videoEncoder.getOutputBuffer(outputBufferIndex);
                                    if ((mediaCodecInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                        // You shoud set output format to muxer here when you target Android4.3 or less
                                        // but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
                                        // therefor we should expand and prepare output format from buffer data.
                                        // This sample is for API>=18(>=Android 4.3), just ignore this flag here
                                        mediaCodecInfo.size = 0;
                                    }

                                    if (mediaCodecInfo.size != 0 && !videoEncoderEnd && callback != null) {
                                        callback.trackVideoCallBack(Contans.VIDEOTRACK, outputBuffer, mediaCodecInfo);
                                    }
                                    videoEncoder.releaseOutputBuffer(outputBufferIndex, false);
                                    outputBufferIndex = videoEncoder.dequeueOutputBuffer(mediaCodecInfo, 0);
                                    if (mediaCodecInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                                        videoEncoderStart = false;

                                    }
                                }
                            }

                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                videoEncoder.stop();
                audioEncoder.release();
                Log.e(TAG, "视频录制关闭");
            }
        });
        videoThread.start();
        ;
        videoEncoderStart = true;
    }

    private void startAudioEncoder() {
        if (audioEncoderStart) {
            Log.e(TAG, "音频正在录制");
            return;
        }

        audioEncoderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (audioEncoder != null) {
                    audioEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                    audioEncoder.start();
                }
                audioEncoderStart = true;
                presentationTimeUs = System.currentTimeMillis() * 1000;
                while (audioEncoderStart) {
                    try {
                        byte[] data = audioQueue.take();
                        if (data != null) {
                            int inputBufferIndex = audioEncoder.dequeueInputBuffer(timeoutUs);
                            if (inputBufferIndex >= 0) {
                                ByteBuffer inputBuffer = audioEncoder.getInputBuffer(inputBufferIndex);
                                inputBuffer.clear();
                                inputBuffer.put(data);
                                long pts = System.currentTimeMillis() * 1000 - presentationTimeUs;
                                if (audioEncoderEnd) {
                                    audioEncoder.queueInputBuffer(inputBufferIndex, 0, data.length, pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                } else {
                                    audioEncoder.queueInputBuffer(inputBufferIndex, 0, data.length, pts, 0);
                                }
                            }

                            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                            int outputBufferIndex = audioEncoder.dequeueOutputBuffer(bufferInfo, timeoutUs);

                            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                MediaFormat outputFormat = audioEncoder.getOutputFormat();
                                if (callback != null) {
                                    callback.mediaFormatCallback(Contans.AUDIOTRACK, outputFormat);
                                }
                            }

                            while (outputBufferIndex >= 0) {
                                ByteBuffer outputBuffer = audioEncoder.getOutputBuffer(outputBufferIndex);
                                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                    bufferInfo.size = 0;
                                }
                                if (bufferInfo.size != 0 && !audioEncoderEnd && callback != null) {
                                    callback.trackAudioCallBack(Contans.AUDIOTRACK, outputBuffer, bufferInfo);
                                }
                                audioEncoder.releaseOutputBuffer(outputBufferIndex, false);
                                outputBufferIndex = audioEncoder.dequeueOutputBuffer(bufferInfo, 0);
                                if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                                    audioEncoderStart = false;

                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                audioEncoder.stop();
                audioEncoder.release();
                Log.e(TAG, "音频录制关闭");
            }
        });
        audioEncoderThread.start();
        audioEncoderStart = true;
    }

    private Callback callback;

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public interface Callback {
        void mediaFormatCallback(int trackIndex, MediaFormat mediaFormat);

        void trackVideoCallBack(int trackIndex, ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo);

        void trackAudioCallBack(int trackIndex, ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo);
    }
}
