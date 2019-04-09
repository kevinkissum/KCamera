package com.example.kevin.kcamera.Presenter;

import android.graphics.SurfaceTexture;
import android.util.Log;

import com.example.kevin.kcamera.CameraAppUI;
import com.example.kevin.kcamera.Interface.IPhotoModuleControll;
import com.example.kevin.kcamera.Interface.IPhotoUIStatusListener;
import com.example.kevin.kcamera.PhotoModule;

public class PhotoUI2ModulePresenter implements IPhotoUIStatusListener, IPhotoModuleControll {

    public static final String TAG = "Presenter";

    private PhotoModule mPhotoModule;
    private CameraAppUI mCameraUI;

    public PhotoUI2ModulePresenter(PhotoModule photoModule, CameraAppUI cameraAppUI) {
        mPhotoModule = photoModule;
        mCameraUI = cameraAppUI;
    }

    @Override
    public void onPreviewUIReady(SurfaceTexture surface, int width, int height) {
//        mPhotoModule.requestCameraOpen(surface, width, height);
    }

    @Override
    public void setPreViewSize(int width, int height) {
        mCameraUI.setPreViewSize(width, height);
    }

    @Override
    public void updatePreviewAspectRatio(int width, int height) {
        mCameraUI.setPreViewSize(width, height);
    }

    @Override
    public void onShutterButtonClick() {
        Log.d("kk", " onShutterButtionClick ");
        mPhotoModule.onShutterButtonClick();
    }

    @Override
    public void switchCamera() {
        mPhotoModule.switchCamera();
    }

}
