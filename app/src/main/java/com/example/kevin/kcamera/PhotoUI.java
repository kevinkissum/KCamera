package com.example.kevin.kcamera;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import com.example.kevin.kcamera.Interface.IPhotoUIStatusListener;
import com.example.kevin.kcamera.Presenter.PhotoUI2ModulePresenter;
import com.example.kevin.kcamera.View.AutoFitTextureView;
import com.example.kevin.kcamera.View.BottomBar;
import com.example.kevin.kcamera.View.MainActivityLayout;
import com.example.kevin.kcamera.View.MultiToggleImageButton;
import com.example.kevin.kcamera.View.RotateImageView;
import com.example.kevin.kcamera.View.ShutterButton;
import com.example.kevin.kcamera.View.StickyBottomCaptureLayout;

public class PhotoUI implements TextureView.SurfaceTextureListener, ShutterButton.OnShutterButtonListener {

    private static final String TAG = "PhotoUI";
    private MainActivityLayout mRootView;
    private AutoFitTextureView mTextureView;
    private IPhotoUIStatusListener mPresenter;
    private ShutterButton mShutter;
    private CaptureLayoutHelper mCaptureLayoutHelper;
    private BottomBar mBottomBar;
    private StickyBottomCaptureLayout mStickyBottomCaptureLayout;
    private MultiToggleImageButton mSwitchCamera;
    private CameraActivity mActivity;


    public PhotoUI(CameraActivity activity, MainActivityLayout rootView) {
        mRootView = rootView;
        mActivity = activity;
        init();
    }

    private void init() {
        Resources res = mActivity.getResources();

    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }


    @Override
    public void onShutterButtonFocus(boolean pressed) {

    }

    @Override
    public void onShutterButtonClick() {

    }

    @Override
    public void onShutterButtonLongPressed() {

    }


}
