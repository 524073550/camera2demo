package com.ke.zhu.camerademo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.ke.zhu.camerademo.UI.AVEncoderActivity;
import com.ke.zhu.camerademo.UI.AVEncoderActivity2;
import com.ke.zhu.camerademo.UI.AudioRecordActivity;
import com.ke.zhu.camerademo.UI.CameraActivity;
import com.ke.zhu.camerademo.UI.EncoderActivity;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void camera(View view) {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    public void AudioRecord(View view) {
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
