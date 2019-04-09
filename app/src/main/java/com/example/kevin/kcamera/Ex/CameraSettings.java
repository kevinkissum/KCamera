package com.example.kevin.kcamera.Ex;

import android.util.Log;
import android.util.Size;

public abstract class CameraSettings {

    public final static String TAG = "CameraSettings";

    protected boolean mSizesLocked;
    private Size mCurrentPhotoSize;
    private Size mCurrentPreviewSize;
    private CameraCapabilities.FocusMode mCurrentFocusMode;


    public boolean setPhotoSize(Size photoSize) {
        if (mSizesLocked) {
            Log.w(TAG, "Attempt to change photo size while locked");
            return false;
        }

        mCurrentPhotoSize = new Size(photoSize.getWidth(), photoSize.getHeight());
        return true;
    }

    public boolean setPreviewSize(Size previewSize) {
        if (mSizesLocked) {
            Log.w(TAG, "Attempt to change preview size while locked");
            return false;
        }

        mCurrentPreviewSize =  new Size(previewSize.getWidth(), previewSize.getHeight());
        return true;
    }

    public Size getCurrentPreviewSize() {
        return new Size(mCurrentPreviewSize.getWidth(), mCurrentPreviewSize.getHeight());
    }

    public Size getCurrentPhotoSize() {
        return new Size(mCurrentPhotoSize.getWidth(), mCurrentPhotoSize.getHeight());
    }

    public CameraCapabilities.FocusMode getCurrentFocusMode() {
        return mCurrentFocusMode;
    }

}
