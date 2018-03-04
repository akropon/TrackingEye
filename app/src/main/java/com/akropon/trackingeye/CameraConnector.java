package com.akropon.trackingeye;


import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE;
import static android.hardware.camera2.CaptureResult.LENS_FOCAL_LENGTH;

/**
 * Created by akropon on 23.09.2017.
 */

public class CameraConnector {
    LaunchActivity launchActivity;
    boolean cameraWasChecked;
    boolean frontCameraFound;


    private TextureView textureView;
    //private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    /*static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }*/

    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    public FaceDetectionManager faceDetectionManager;
    boolean isTurnedOn;


    public CameraConnector(LaunchActivity launchActivity) {
        this.launchActivity = launchActivity;
        isTurnedOn = false;
    }

    public void onCreate() {
        textureView = new TextureView(launchActivity);
        SurfaceTexture surfaceTexture = new SurfaceTexture(1000);
        surfaceTexture.setDefaultBufferSize(1024, 1024); //TODO че-то с размерами придумать
        textureView.setSurfaceTexture(surfaceTexture);
        //textureView.setSurfaceTextureListener(textureListener);
    }


    public boolean isTurnedOn() {
        return isTurnedOn;
    }

    /*TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                //open your camera here
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                // Transform you image captured size according to the surface width and height
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        };*/
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.d("akropon-tag", "camera-stateCallback.onOpened()");
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.d("akropon-tag", "camera-stateCallback.onDisconnected()");
            //cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e("akropon-tag", "camera-stateCallback.onError(), error="+error);
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            //assert (texture == null);
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }



                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;

                    setup3AControlsLocked(captureRequestBuilder);

                    captureRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                            CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL);


                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(launchActivity, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        Log.d("akropon-tag", "cameraConnector.openCamera()");
        CameraManager manager = (CameraManager) launchActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            boolean frontCameraFound = false;
            for (String cameraIdIterator : manager.getCameraIdList()) {
                /*Log.d(TAG, "cameraID = "+cameraIdIterator+", Lens_facing = "
                        +manager.getCameraCharacteristics(cameraIdIterator).get(
                                CameraCharacteristics.LENS_FACING));*/
                if (manager.getCameraCharacteristics(cameraIdIterator)
                        .get(CameraCharacteristics.LENS_FACING)
                        == CameraCharacteristics.LENS_FACING_FRONT) {
                    cameraId = cameraIdIterator;
                    frontCameraFound = true;
                    Log.d("akropon-tag", "cameraConnector.openCamera() - front camera was found");
                    break;
                }
            }
            if (frontCameraFound == false) {
                Log.e("akropon-tag", "cameraConnector.openCamera() - front camera was not found");
                System.exit(1);
            }
            //cameraId = manager.getCameraIdList()[0]; // TODO comment this line
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            faceDetectionManager = new FaceDetectionManager(characteristics);

            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(launchActivity, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(launchActivity,
                        new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION);
                return;
            }

            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void updatePreview() {
        if (null == cameraDevice) {
            Log.e("akropon-tag", "cameraConnector.updatePreview() - error, cameraDevice is null.");
            System.exit(2);
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(),
                    mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(launchActivity, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                System.exit(0);
            }
        }
    }


    /*public void onResume() {
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            Log.e(TAG, "textureView is available");
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }

    }*/

    public void turnOn() {

        if (isTurnedOn == false) {
            startBackgroundThread();
            openCamera();
            isTurnedOn = true;
        } else {
            // TODO add logging
        }
    }


    public void turnOff() {
        if (isTurnedOn == true) {
            closeCamera();
            stopBackgroundThread();
            isTurnedOn = false;
        } else {
            // TODO add logging
        }

    }


    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        void process(CaptureResult result) {
            //Integer mode = result.get(CaptureResult.STATISTICS_FACE_DETECT_MODE);

            Face[] faces = result.get(CaptureResult.STATISTICS_FACES);

            /*if(faces != null && mode != null) {
                Log.e("tag", "faces : " + faces.length + " , mode : " + mode);
            }*/

            if (faces.length == 0) {
                launchActivity.faceNotFound_threadsafe();
                launchActivity.faceLost();
            } else {
                final Rect bounds = faces[0].getBounds();
                int x = (bounds.left+bounds.right) / 2;
                int y = (bounds.top+bounds.bottom) / 2;
                launchActivity.faceDetected_threadsafe(x, y);
                launchActivity.detectedFaceBound_threadsafe(bounds.left, bounds.top,
                        bounds.right, bounds.bottom);
                launchActivity.faceDetected(new Bounds(
                        faceDetectionManager.getPosRatioByWidth(bounds.left),
                        faceDetectionManager.getPosRatioByHeight(bounds.top),
                        faceDetectionManager.getPosRatioByWidth(bounds.right),
                        faceDetectionManager.getPosRatioByHeight(bounds.bottom)),
                        result);
            }

            /*switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }*/
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };


    /**
     * Configure the given {@link CaptureRequest.Builder} to use auto-focus, auto-exposure, and
     * auto-white-balance controls if available.
     * <p/>
     * Call this only with {@link #//mCameraStateLock} held.
     *
     * @param builder the builder to configure.
     */
    private void setup3AControlsLocked(CaptureRequest.Builder builder) {
        // Enable auto-magical 3A run by camera device
        //builder.set(CaptureRequest.CONTROL_MODE,
        //        CaptureRequest.CONTROL_MODE_AUTO);


        builder.set(CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_AUTO);


        builder.set(CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON);


        builder.set(CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_AUTO);
    }
}
