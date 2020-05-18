package com.ke.zhu.camerademo.UI;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.os.Environment;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.ke.zhu.camerademo.JniUtils;
import com.ke.zhu.camerademo.R;
import com.ke.zhu.camerademo.util.CameraHelp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;

public class CameraActivity extends AppCompatActivity implements CameraHelp.CamerDataCallback {


    private CameraHelp cameraHelp;

    private FileOutputStream fileOutputStream3 = null;
    private ImageView pic;
    private RelativeLayout ll_root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        pic = findViewById(R.id.iv_pic);
        ll_root = findViewById(R.id.ll_root);
        TextureView textureview = findViewById(R.id.textureview);
        cameraHelp = new CameraHelp(textureview, this);
        cameraHelp.setCameraCallback(this);
        try {
            File file3 = new File(Environment.getExternalStorageDirectory().getPath()+"/cam.yuv");
            fileOutputStream3 = new FileOutputStream(file3);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraHelp.releaseCamera();

        try {
            fileOutputStream3.close();
            fileOutputStream3 = null;
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void cameraCallback(byte[] data) {
        if (fileOutputStream3 != null) {
            byte[] nv21= new byte[data.length];
            //由于进行了旋转 那么宽高需要对换
            JniUtils.yuvI420ToNV21(data,nv21,cameraHelp.PREVIEW_MAX_HEIGHT,cameraHelp.PREVIEW_MAX_WIDTH );
            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, cameraHelp.PREVIEW_MAX_HEIGHT,cameraHelp.PREVIEW_MAX_WIDTH, null);
            ByteArrayOutputStream fOut = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, cameraHelp.PREVIEW_MAX_HEIGHT,cameraHelp.PREVIEW_MAX_WIDTH), 100, fOut);
            //将byte生成bitmap
            byte[] bitData = fOut.toByteArray();
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitData, 0, bitData.length);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pic.setImageBitmap(bitmap);
                    ll_root.setVisibility(View.VISIBLE);
                }
            });

        }
    }

    public void take_pic(View view) {
        cameraHelp.takPic();
    }

    public void change_camera(View view) {
        cameraHelp.changeCamera();
    }

    public void iv_closr(View view) {
        ll_root.setVisibility(View.GONE);
    }
}
