package com.ke.zhu.camerademo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;

import com.ke.zhu.camerademo.UI.AVEncoderActivity;
import com.ke.zhu.camerademo.UI.AVEncoderActivity2;
import com.ke.zhu.camerademo.UI.AudioRecordActivity;
import com.ke.zhu.camerademo.UI.CameraActivity;
import com.ke.zhu.camerademo.UI.EncoderActivity;

import java.sql.Array;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {


    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        JniUtils.getFfmpegInfo();
       new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                handler = new Handler(){
                    @Override
                    public void handleMessage(@NonNull Message msg) {
                        switch ( msg.what){
                            case 1:
                                Log.e("收到消息","收到消息");
                                break;
                        }
                    }
                };
                Looper.loop();
            }
        }).start();

    }

    public void camera(View view) {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    public void AudioRecord(View view) {
        handler.sendEmptyMessage(1);
        Intent intent = new Intent(this, AudioRecordActivity.class);
        startActivity(intent);
    }

    public void video(View view) {
        Intent intent = new Intent(this, EncoderActivity.class);
        startActivity(intent);
    }

    public void medio(View view) {
        Intent intent = new Intent(this, AVEncoderActivity2.class);
        startActivity(intent);
    }
}
