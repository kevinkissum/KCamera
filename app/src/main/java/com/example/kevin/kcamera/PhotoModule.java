package com.example.kevin.kcamera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;

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

    public PhotoModule(CameraActivity activity, Handler handler) {
        mCameraControl = new CameraController(activity, handler, this);
    }


    public void openCamera(SurfaceTexture surface, int width, int height) {
        mCameraControl.setSurfaceTexture(surface);
        mCameraControl.requestCamera(0, true, width, height);
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
}
