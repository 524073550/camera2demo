package com.ke.zhu.camerademo.UI;

import android.os.Bundle;
import android.view.TextureView;
import android.view.View;

import com.ke.zhu.camerademo.R;
import com.ke.zhu.camerademo.medio.AVmediaMuxer;
import com.ke.zhu.camerademo.util.CameraHelp;
import com.ke.zhu.camerademo.util.H264Encoder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AVEncoderActivity extends AppCompatActivity implements CameraHelp.CamerDataCallback {
    private CameraHelp cameraHelp;
    private AVmediaMuxer aVmediaMuxer;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        TextureView textureview = findViewById(R.id.textureview);
        cameraHelp = new CameraHelp(textureview, this);
        cameraHelp.setCameraCallback(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraHelp.releaseCamera();
    }
    boolean canVideo;
    public void take_pic(View view) {
        if (canVideo){
            canVideo = false;
            cameraHelp.startVideo(false);
            aVmediaMuxer.stopMediaMuxer();
        }else {
            canVideo = true;
            aVmediaMuxer.startEncoder();
            cameraHelp.startVideo(true);
        }
    }

    @Override
    public void cameraCallback(byte[] data) {
        aVmediaMuxer.startVoideEncoder(data);
    }

    @Override
    public void cameraStartSuccess() {
        aVmediaMuxer = new AVmediaMuxer(getExternalCacheDir().getPath() + "/test.mp4",cameraHelp.PREVIEW_MAX_HEIGHT,cameraHelp.PREVIEW_MAX_WIDTH);
        aVmediaMuxer.start();
    }
}
