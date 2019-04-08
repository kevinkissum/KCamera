package com.example.kevin.kcamera.Presenter;

import android.graphics.SurfaceTexture;
import android.util.Log;

import com.example.kevin.kcamera.CameraAppUI;
import com.example.kevin.kcamera.Interface.IPhotoModuleControll;
import com.example.kevin.kcamera.Interface.IPhotoUIStatusListener;
import com.example.kevin.kcamera.PhotoModule;
import com.example.kevin.kcamera.PhotoUI;

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
        mPhotoModule.openCamera(surface, width, height);
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
        mPhotoModule.takePicture();
    }

    @Override
    public void switchCamera() {
        mPhotoModule.switchCamera();
    }

    public void OpenCamera() {
        mPhotoModule.openCamera();
    }
}
