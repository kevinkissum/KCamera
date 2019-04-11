package com.example.kevin.kcamera.Ex;

import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.util.Size;

public class AndroidCamera2Settings extends CameraSettings {


    public AndroidCamera2Settings(CameraDevice mCamera, int templatePreview, Rect mActiveArray, Size mPreviewSize, Size mPhotoSize)  throws CameraAccessException {

    }

    @Override
    public CameraSettings copy() {
        return null;
    }
}
