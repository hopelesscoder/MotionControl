package com.hopelesscoder.motioncontrol;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.Arrays;

public class VideoProcessingService extends Service {
    private static final String TAG = "VideoProcessing";
    private static final int CAMERA = CameraCharacteristics.LENS_FACING_FRONT;
    private CameraDevice camera;
    private CameraCaptureSession session;
    private ImageReader imageReader;

    private CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            VideoProcessingService.this.camera = camera;
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
        }

        @Override
        public void onError(CameraDevice camera, int error) {
        }
    };

    private CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            VideoProcessingService.this.session = session;
            try {
                session.setRepeatingRequest(createCaptureRequest(), null, null);
            } catch (CameraAccessException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
        }
    };

    private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image img = reader.acquireLatestImage();
            processImage(img);
            img.close();
        }
    };

    @Override
    public void onCreate() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            manager.openCamera(getCamera(manager), cameraStateCallback, null);
            imageReader = ImageReader.newInstance(320, 240, ImageFormat.YUV_420_888, 30 * 600); //fps * 10 min
            imageReader.setOnImageAvailableListener(onImageAvailableListener, null);
        } catch (CameraAccessException e){
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     *  Return the Camera Id which matches the field CAMERA.
     */
    public String getCamera(CameraManager manager){
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cOrientation == CAMERA) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            camera.createCaptureSession(Arrays.asList(imageReader.getSurface()), sessionStateCallback, null);
        } catch (CameraAccessException e){
            Log.e(TAG, e.getMessage());
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        try {
            session.abortCaptures();
        } catch (CameraAccessException e){
            Log.e(TAG, e.getMessage());
        }
        session.close();
    }

    /**
     *  Process image data as desired.
     */
    private void processImage(Image image){
        //Process image data
    }

    private CaptureRequest createCaptureRequest() {
        try {
            CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(imageReader.getSurface());
            return builder.build();
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}