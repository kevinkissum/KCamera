package com.example.kevin.kcamera.Presenter;

import android.graphics.SurfaceTexture;

import com.example.kevin.kcamera.Interface.IPhotoModuleControll;
import com.example.kevin.kcamera.Interface.IPhotoUIStatusListener;
import com.example.kevin.kcamera.PhotoModule;
import com.example.kevin.kcamera.PhotoUI;

public class PhotoUI2ModulePresenter implements IPhotoUIStatusListener, IPhotoModuleControll {

    public static final String TAG = "Presenter";

    private PhotoModule mPhotoModule;
    private PhotoUI mPhotoUI;

    public PhotoUI2ModulePresenter(PhotoModule photoModule, PhotoUI photoUI) {
        mPhotoModule = photoModule;
        mPhotoUI = photoUI;
    }

    @Override
    public void onPreviewUIReady(SurfaceTexture surface, int width, int height) {
        mPhotoModule.openCamera(surface, width, height);
    }

    @Override
    public void setPreViewSize(int width, int height) {
        mPhotoUI.setPreViewSize(width, height);
    }
}