package com.ke.zhu.camerademo.UI;

import android.os.Bundle;
import android.view.TextureView;
import android.view.View;

import com.ke.zhu.camerademo.R;
import com.ke.zhu.camerademo.medio.AVmediaMuxer;
import com.ke.zhu.camerademo.util.AVMuxer;
import com.ke.zhu.camerademo.util.CameraHelp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AVEncoderActivity2 extends AppCompatActivity implements CameraHelp.CamerDataCallback {
    private CameraHelp cameraHelp;
    private AVmediaMuxer aVmediaMuxer;
    private AVMuxer avMuxer;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        TextureView textureview = findViewById(R.id.textureview);
        cameraHelp = new CameraHelp(textureview, this);
        cameraHelp.setCameraCallback(this);
        avMuxer = new AVMuxer();
        avMuxer.initMediaMuxer(getExternalCacheDir().getPath() + "/test.mp4");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraHelp.releaseCamera();
        if (avMuxer!=null){
            avMuxer.stop();
            avMuxer =null;
        }
    }
    boolean canVideo;
    public void take_pic(View view) {
        if (canVideo){
            canVideo = false;
            cameraHelp.startVideo(false);
            avMuxer.stop();
        }else {
            canVideo = true;
            cameraHelp.startVideo(true);
            avMuxer.start();
        }
    }

    @Override
    public void cameraCallback(byte[] data) {
        if (data!=null)
        avMuxer.putVideoData(data);
    }

    @Override
    public void cameraStartSuccess() {
        avMuxer.initEncoder(cameraHelp.PREVIEW_MAX_HEIGHT,cameraHelp.PREVIEW_MAX_WIDTH);

    }
}
