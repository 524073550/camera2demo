package com.ke.zhu.camerademo.UI;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.ke.zhu.camerademo.R;
import com.ke.zhu.camerademo.util.PcmToWavUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;

public class AudioRecordActivity extends AppCompatActivity {
    private Integer mRecordBufferSize;
    private String TAG = "AudioRecord";
    private File handlerWavFile;
    //采样率
    private int sampleRateInHz= 8000;
    //声道
    private int channelConfig= AudioFormat.CHANNEL_IN_MONO;

    //音频格式
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);
        initAudioRecord();

    }
    private AudioRecord mAudioRecord;

    private void initAudioRecord() {
        mRecordBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz
                ,channelConfig
                , audioFormat);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC
                , sampleRateInHz
                ,channelConfig
                , audioFormat
                , mRecordBufferSize);
    }

    private boolean mWhetherRecord;
    private File pcmFile;

    private void startRecord() {
        pcmFile = new File(AudioRecordActivity.this.getExternalCacheDir().getPath(), "audioRecord.pcm");
        mWhetherRecord = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                mAudioRecord.startRecording();//开始录制
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(pcmFile);
                    byte[] bytes = new byte[mRecordBufferSize];
                    while (mWhetherRecord) {
                        mAudioRecord.read(bytes, 0, bytes.length);//读取流
                        fileOutputStream.write(bytes);
                        fileOutputStream.flush();

                    }
                    Log.e(TAG, "run: 暂停录制");
                    mAudioRecord.stop();//停止录制
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    addHeadData();//添加音频头部信息并且转成wav格式
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    mAudioRecord.stop();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    private void addHeadData() {
        pcmFile = new File(AudioRecordActivity.this.getExternalCacheDir().getPath(), "audioRecord.pcm");
        handlerWavFile = new File(AudioRecordActivity.this.getExternalCacheDir().getPath(), "audioRecord_handler.wav");
        PcmToWavUtil pcmToWavUtil = new PcmToWavUtil(sampleRateInHz, channelConfig,audioFormat);
        pcmToWavUtil.pcmToWav(pcmFile.toString(), handlerWavFile.toString());
    }

    public void start(View view) {
        startRecord();
    }

    public void stop(View view) {
        mWhetherRecord = false;
    }


}
