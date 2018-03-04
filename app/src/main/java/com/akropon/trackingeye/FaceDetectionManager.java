package com.akropon.trackingeye;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Log;
import android.util.SizeF;

import static android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE;
import static android.hardware.camera2.CaptureResult.LENS_FOCAL_LENGTH;

/**
 * Created by akropon on 24.09.2017.
 */

public class FaceDetectionManager {
    CameraCharacteristics cameraCharacteristics;

    float cameraMatrixWidth;
    float cameraMatrixHeight;

    SizeF sensor_info_physical_size;

    public FaceDetectionManager(CameraCharacteristics cameraCharacteristics) {
        this.cameraCharacteristics = cameraCharacteristics;

        Rect cameraResolution = cameraCharacteristics.get(
                CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        cameraMatrixWidth = cameraResolution.right;
        cameraMatrixHeight = cameraResolution.bottom;

        sensor_info_physical_size = cameraCharacteristics.get(
                CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);

        Log.d("akropon-tag", "camera resolution? ="
                + " " + cameraResolution.left
                + " " + cameraResolution.top
                + " " + cameraResolution.right
                + " " + cameraResolution.bottom);

    }


    public float getCameraMatrixWidth() {
        return cameraMatrixWidth;
    }

    public float getCameraMatrixHeight() {
        return cameraMatrixHeight;
    }

    public SizeF getSensor_info_physical_size() {
        return sensor_info_physical_size;
    }


    public CameraCharacteristics getCameraCharacteristics() {
        return cameraCharacteristics;
    }

    public float getPosRatioByWidth(float pos) {
        return pos / cameraMatrixWidth;
    }


    public float getPosRatioByHeight(float pos) {
        return pos / cameraMatrixHeight;
    }


    public float getCameraToScreenImageCenterRatioOffset(CaptureResult result, Bounds ratioBounds) {

        float lfl = result.get(LENS_FOCAL_LENGTH);
        double angle = 2 * Math.atan(
                sensor_info_physical_size.getWidth() / (2 * lfl));

        double ro = ratioBounds.r - ratioBounds.l;
        if (ro < 0) ro = -ro;
        return (float) (ro * angle / Math.PI * 0.5);
    }


}
