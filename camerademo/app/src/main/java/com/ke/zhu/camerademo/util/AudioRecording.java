package com.ke.zhu.camerademo.util;


import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioRecording {
    private String TAG = "AudioRecording";
    //采样率
    private int sampleRateInHz = 41400;
    private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_STEREO;//单声道
    //音频格式
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private boolean audioRecording;
    private AudioRecord audioRecord;
    private int minBufferSize;
    private int pcmFormat;
    private int channelConfiga;

    public void AudioRecording() {

    }

    public void initAudioRecording() {
        //音频采样率，44100是目前的标准，但是某些设备仍然支持22050,16000,11025,8000,4000
        int[] sampleRates = {44100, 22050, 16000, 11025, 8000, 4000};
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        for (int i = 0; i < sampleRates.length; i++) {
            minBufferSize = 2*AudioRecord.getMinBufferSize(sampleRates[i], channelConfig, audioFormat);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRates[i], channelConfig, audioFormat, minBufferSize);
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED){
                audioRecord = null;
                continue;
            }
            channelConfiga = channelConfig == AudioFormat.CHANNEL_CONFIGURATION_STEREO ? 2 : 1;
            minBufferSize =  Math.min(4096, minBufferSize);
            sampleRateInHz =sampleRates[i] ;
            pcmFormat = 16;
            break;
        }
    }

    public void startRecord() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                audioRecording = true;
                if (audioRecord != null) {
                    audioRecord.startRecording();
                }
                byte[] outByte = new byte[minBufferSize];
                while (audioRecording) {
                    audioRecord.read(outByte, 0, outByte.length);
                    if (callback!=null){
                        callback.callback(outByte);
                    }
                }
                audioRecord.stop();
                audioRecord.release();
                Log.e(TAG, "录音停止采集");
            }
        }).start();
    }


    public int getSampleRateInHz() {
        return sampleRateInHz;
    }

    public int getChannelConfig() {
        return channelConfiga;
    }

    public int getPcmFormat() {
        return pcmFormat;
    }

    public void stopRecording() {
        audioRecording = false;
    }
    private AudioRecordCallback callback;
    public void setCallback(AudioRecordCallback callback){
        this.callback = callback;
    }

    public interface AudioRecordCallback{
        void callback(byte[] buffer);
    }

}
