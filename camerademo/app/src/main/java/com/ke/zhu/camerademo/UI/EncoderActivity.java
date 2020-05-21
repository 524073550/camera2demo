package com.ke.zhu.camerademo.UI;

import android.os.Bundle;
import android.os.Environment;
import android.view.TextureView;
import android.view.View;

import com.ke.zhu.camerademo.R;
import com.ke.zhu.camerademo.util.CameraHelp;
import com.ke.zhu.camerademo.util.H264Encoder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class EncoderActivity extends AppCompatActivity implements CameraHelp.CamerDataCallback {
    private CameraHelp cameraHelp;
    private H264Encoder h264Encoder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        TextureView textureview = findViewById(R.id.textureview);
        cameraHelp = new CameraHelp(textureview, this);
        cameraHelp.setCameraCallback(this);
        h264Encoder = new H264Encoder(cameraHelp.PREVIEW_MAX_HEIGHT, cameraHelp.PREVIEW_MAX_WIDTH, 30);
        try {
            File file = new File(getExternalCacheDir().getPath() + "/test.mp4");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            h264Encoder.setOutputStrem(bufferedOutputStream);
            h264Encoder.startEncoder();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraHelp.releaseCamera();
    }

    public void take_pic(View view) {
        cameraHelp.startVideo();
    }

    @Override
    public void cameraCallback(byte[] data) {
        h264Encoder.putYUVData(data);
    }
}
