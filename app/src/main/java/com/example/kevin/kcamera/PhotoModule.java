package com.example.kevin.kcamera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.util.Log;

import com.example.kevin.kcamera.Interface.ICameraControll;
import com.example.kevin.kcamera.Interface.IPhotoModuleControll;
import com.example.kevin.kcamera.Presenter.PhotoUI2ModulePresenter;

public class PhotoModule implements ICameraControll{


    private static final String TAG = "PhotoModule";
    private CameraController mCameraControl;
    private CameraActivity mActivity;
    private Context mContext;
    private PhotoUI mUI;
    private IPhotoModuleControll mPhotoControl;
    private boolean mPaused;
    private int mCameraId;

    public PhotoModule(CameraActivity activity, Handler handler) {
        mCameraControl = new CameraController(activity, handler, this);
    }


    public void openCamera(SurfaceTexture surface, int width, int height) {
        mCameraControl.setSurfaceTexture(surface, width, height);
        mCameraControl.requestCamera(mCameraId, true);
    }


    @Override
    public void onCameraOpened() {

    }

    @Override
    public void changePreViewSize(int width, int height) {
        mPhotoControl.setPreViewSize(width, height);
    }

    public void setPresenter(IPhotoModuleControll photoControl) {
        mPhotoControl = photoControl;
    }

    public void takePicture() {
        mCameraControl.StartTakePicture();
    }

    public void switchCamera() {
        if (mPaused) {
            return;
        }
        closeCamera();
        mCameraId ^= 1;
        mCameraControl.requestCamera(mCameraId, true);
    }

    private void closeCamera() {
        mCameraControl.closeCamera();
    }
}
