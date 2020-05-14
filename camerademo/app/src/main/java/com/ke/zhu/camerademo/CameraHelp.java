package com.ke.zhu.camerademo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraHelp {
    private String TAG = "CameraHelp";
    private TextureView txt_view;
    private CameraManager cameraManager;
    private String cameraId = "";
    private CameraCharacteristics cameraCharacteristics;
    private Integer cameraSensorOrientation;
    private int rotation;

    public int PREVIEW_MAX_WIDTH = 1920;
    public int PREVIEW_MAX_HEIGHT = 1080;
    private int SAVE_MAX_WIDTH = 1920;
    private int SAVE_MAX_HEIGHT = 1080;

    private boolean exchange;
    private Size savePicSizeList;
    private Size previewSizeList;
    private SurfaceTexture surfaceTexture;
    private ImageReader imageReader;
    private Handler cameraHandler;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private boolean canExchangeCamera;
    private boolean isMirror;
    private boolean canTakePic;
    private Integer cameraFacing = CameraCharacteristics.LENS_FACING_BACK;
    private Activity context;
    private CamerDataCallback camerDataCallback;

    public CameraHelp(TextureView textureView, Activity context) {
        this.txt_view = textureView;
        this.context = context;
        rotation = context.getWindow().getWindowManager().getDefaultDisplay().getRotation();
        HandlerThread cameraThread = new HandlerThread("camera_thread");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
        textureView.setSurfaceTextureListener(surfaceTextureListener);
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int i, int i1) {
            //TODO:初始化相机
            try {
                surfaceTexture = surface;
                initCamera();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initCamera() throws CameraAccessException {
        if (context == null) {
            Log.e("camera", "context 不能为空");
            return;
        }
        //获取cameramanage
        cameraManager = (CameraManager) context.getSystemService(context.CAMERA_SERVICE);
        //获取相机idCameraDevice
        String[] cameraIdList = getCameraId();
        if (cameraIdList == null && cameraIdList.length == 0) {
            Log.e("camera", "camera初始化失败");
            return;
        }
        //打开指定相机id 默认打开后置摄像头
        for (String id : cameraIdList) {
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(id);
            Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing == cameraFacing) {
                cameraId = id;
                this.cameraCharacteristics = cameraCharacteristics;
            }
        }
        cameraSensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] savePicSize = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
        Size[] previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class);
        exchange = exchangeWidthAndHeight(rotation, cameraSensorOrientation);
        savePicSizeList = getBestSize(savePicSize, SAVE_MAX_WIDTH, SAVE_MAX_HEIGHT);
        previewSizeList = getBestSize(previewSize, PREVIEW_MAX_WIDTH, PREVIEW_MAX_HEIGHT);
        surfaceTexture.setDefaultBufferSize(previewSizeList.getWidth(), previewSizeList.getHeight());

        //当设置格式为JPEG时 onImageAvailableListener回调不会执行,当拍照是创建拍照的会话时才会回调
        imageReader = ImageReader.newInstance(previewSizeList.getWidth(), previewSizeList.getHeight(), ImageFormat.YUV_420_888, 1);
        imageReader.setOnImageAvailableListener(onImageAvailableListener, cameraHandler);
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        cameraManager.openCamera(cameraId, stateCallback, cameraHandler);

    }


    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
//            canTakePic = true;
            createCaptureSession(camera);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
//            canTakePic = false;
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void createCaptureSession(CameraDevice camera) {
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            Surface surface = new Surface(txt_view.getSurfaceTexture());
            captureRequestBuilder.addTarget(surface);
            captureRequestBuilder.addTarget(imageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);    // 闪光灯
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE); // 自动对焦
            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), createCaptureSession, cameraHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.StateCallback createCaptureSession = new CameraCaptureSession.StateCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            cameraCaptureSession = session;
            try {
                session.setRepeatingRequest(captureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                }, cameraHandler);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    };

    CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            canExchangeCamera = true;
//            canTakePic = true;
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }
    };

    private boolean isFrist;

    /**
     * I420: YYYYYYYY UU VV    =>YUV420P
     * YV12: YYYYYYYY VV UU    =>YUV420P
     * NV12: YYYYYYYY UVUV     =>YUV420SP
     * NV21: YYYYYYYY VUVU     =>YUV420SP
     * <p>
     * pixelStride  1
     * rowStride   1280
     * buffersize   921600
     * width  1280
     * height  720
     * Finished reading data from plane  0
     * pixelStride  2
     * rowStride   1280
     * buffersize   460799
     * width  1280
     * height  720
     * Finished reading data from plane  1
     * pixelStride  2
     * rowStride   1280
     * buffersize   460799
     * width  1280
     * height  720
     * Finished reading data from plane  2
     * <p>
     * 当pixelStride为2 时plan[1]中存储的数据为UVUV  plan[2]中存储的数据为VUVU
     * 所以结论为 plane[0] + plane[1] 可得NV12
     * plane[0] + plane[2] 可得NV21
     * 当pixelStride为1 时plan[1]中存储的数据为UUUU  plan[2]中存储的数据为VVVV
     * 所以结论为 plane[0]+ plane[1]+ plane[2] 可得I420
     * plane[0]+ plane[2]+ plane[1] 可得YV12
     */
    byte[] y = new byte[PREVIEW_MAX_WIDTH * PREVIEW_MAX_HEIGHT];
    byte[] vu = new byte[PREVIEW_MAX_WIDTH * PREVIEW_MAX_HEIGHT / 2 - 1];
    byte[] dst = new byte[PREVIEW_MAX_WIDTH * PREVIEW_MAX_HEIGHT * 3 / 2];
    byte[] dstRotate = new byte[PREVIEW_MAX_WIDTH * PREVIEW_MAX_HEIGHT * 3 / 2];

    ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @SuppressLint("NewApi")
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            if (image == null) {
                return;
            }
            Image.Plane[] planes = image.getPlanes();
            if (!isFrist) {
                isFrist = true;
                for (int i = 0; i < planes.length; i++) {
                    int rowStride = planes[i].getRowStride();
                    int pixelStride = planes[i].getPixelStride();
                    int remaining = planes[i].getBuffer().remaining();
                    int width = image.getWidth();
                    int height = image.getHeight();
                    Log.i(TAG, "pixelStride  " + pixelStride);
                    Log.i(TAG, "rowStride   " + rowStride);
                    Log.i(TAG, "buffersize   " + remaining);
                    Log.i(TAG, "width  " + width);
                    Log.i(TAG, "height  " + height);
                    Log.i(TAG, "Finished reading data from plane  " + i);
                }
            }

            if (camerDataCallback != null && canTakePic) {
                canTakePic = false;
                planes[0].getBuffer().get(y);
                planes[2].getBuffer().get(vu);
                JniUtils.nv21ToI420(y, vu, PREVIEW_MAX_WIDTH, PREVIEW_MAX_HEIGHT, dst);
                JniUtils.I420Rotate(dst, PREVIEW_MAX_WIDTH, PREVIEW_MAX_HEIGHT, dstRotate, 90,isMirror);
                camerDataCallback.cameraCallback(dstRotate);
                /*byte[] y = new byte[planes[0].getBuffer().remaining()];
                planes[0].getBuffer().get(y);
                byte[] uv = new byte[planes[2].getBuffer().remaining()];
                planes[2].getBuffer().get(uv);
                //1382399
                byte[] nv21 = YUVUtils.getNV21(y, uv);
                if (nv21 != null) {
                    camerDataCallback.cameraCallback(nv21);
                }*/
            }
            image.close();
        }
    };


    public void setCameraCallback(CamerDataCallback cameraCallback) {
        this.camerDataCallback = cameraCallback;
    }


    public interface CamerDataCallback {
        void cameraCallback(byte[] data);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Size getBestSize(Size[] previewSize, int maxWidth, int maxHeight) {
        float aspectRatio = (float) maxWidth / maxHeight;
        List<Size> sizes = new ArrayList<>();
        for (Size size : previewSize) {
            if ((float) size.getWidth() / size.getHeight() == aspectRatio && size.getWidth() <= maxWidth && size.getHeight() <= maxHeight) {
                sizes.add(size);
            }
        }
        return Collections.max(sizes, new Comparator<Size>() {
            @Override
            public int compare(Size o1, Size o2) {
                return o1.getHeight() - o2.getHeight();
            }
        });
    }

    private boolean exchangeWidthAndHeight(int rotation, Integer cameraSensorOrientation) {
        boolean exchange = false;
        switch (rotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                if (cameraSensorOrientation == 90 || cameraSensorOrientation == 270) {
                    exchange = true;
                }
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                if (cameraSensorOrientation == 0 || cameraSensorOrientation == 180) {
                    exchange = true;
                }
                break;
        }

        return exchange;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private String[] getCameraId() {
        String[] cameraIdList = null;
        try {
            cameraIdList = cameraManager.getCameraIdList();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cameraIdList;
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void releaseCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    public void changeCamera() {
        if (cameraDevice != null) {
            releaseCamera();
            if (cameraFacing == CameraCharacteristics.LENS_FACING_BACK) {
                cameraFacing = CameraCharacteristics.LENS_FACING_FRONT;
                isMirror = true;
            } else {
                cameraFacing = CameraCharacteristics.LENS_FACING_BACK;
                isMirror = false;
            }

            try {
                initCamera();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void takPic() {
        if (cameraDevice == null) return;
        canTakePic = true;
        /*// 创建拍照需要的CaptureRequest.Builder
        final CaptureRequest.Builder captureRequestBuilder;
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // 将imageReader的surface作为CaptureRequest.Builder的目标
            captureRequestBuilder.addTarget(imageReader.getSurface());
            // 自动对焦
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 自动曝光
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 获取手机方向
            int rotation = context.getWindowManager().getDefaultDisplay().getRotation();
            // 根据设备方向计算设置照片的方向
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, cameraSensorOrientation);
            //拍照
            CaptureRequest mCaptureRequest = captureRequestBuilder.build();
            cameraCaptureSession.capture(mCaptureRequest, null, cameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }*/
    }

}
