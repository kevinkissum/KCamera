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
import com.example.kevin.kcamera.View.RotateImageView;
import com.example.kevin.kcamera.View.ShutterButton;
import com.example.kevin.kcamera.View.StickyBottomCaptureLayout;

public class PhotoUI implements TextureView.SurfaceTextureListener, ShutterButton.OnShutterButtonListener, View.OnClickListener {

    private static final String TAG = "PhotoUI";
    private MainActivityLayout mRootView;
    private AutoFitTextureView mTextureView;
    private IPhotoUIStatusListener mPresenter;
    private ShutterButton mShutter;
    private CaptureLayoutHelper mCaptureLayoutHelper;
    private Context mAppContext;
    private BottomBar mBottomBar;
    private StickyBottomCaptureLayout mStickyBottomCaptureLayout;
    private RotateImageView mSwitchCamera;
    private CameraActivity mActivity;

    private final ButtonManager.ButtonCallback mCameraCallback =
            new ButtonManager.ButtonCallback() {
                @Override
                public void onStateChanged(int state) {

                    ButtonManager buttonManager = mActivity.getButtonManager();
//                    buttonManager.disableCameraButtonAndBlock();
                    Log.d(TAG, "Start to switch camera. cameraId=" + state);
                    mPresenter.switchCamera();
                }
            };

    private ButtonManager.ButtonCallback getDisableButtonCallback(final int conflictingButton) {
        return new ButtonManager.ButtonCallback() {
            @Override
            public void onStateChanged(int state) {
                mActivity.getButtonManager().disableButton(conflictingButton);
            }
        };
    }

    public PhotoUI(CameraActivity activity, MainActivityLayout rootView) {
        mRootView = rootView;
        mActivity = activity;
        init();
    }

    private void init() {
        Resources res = mAppContext.getResources();
        mTextureView = (AutoFitTextureView) mRootView.findViewById(R.id.preview_content);
        mTextureView.setSurfaceTextureListener(this);
        mShutter = mRootView.findViewById(R.id.shutter_button);
        mShutter.addOnShutterButtonListener(this);
        mCaptureLayoutHelper = new CaptureLayoutHelper(
                res.getDimensionPixelSize(R.dimen.bottom_bar_height_min),
                res.getDimensionPixelSize(R.dimen.bottom_bar_height_max),
                res.getDimensionPixelSize(R.dimen.bottom_bar_height_optimal));
        mRootView.setNonDecorWindowSizeChangedListener(mCaptureLayoutHelper);
        mBottomBar = mRootView.findViewById(R.id.bottom_bar);
        mStickyBottomCaptureLayout = mRootView.findViewById(R.id.sticky_bottom_capture_layout);
        mBottomBar.setCaptureLayoutHelper(mCaptureLayoutHelper);
        mStickyBottomCaptureLayout.setCaptureLayoutHelper(mCaptureLayoutHelper);
        mSwitchCamera = mRootView.findViewById(R.id.camera_switch);
        mSwitchCamera.setOnClickListener(this);
        ButtonManager buttonManager = mActivity.getButtonManager();
        buttonManager.initializeButton(
                ButtonManager.BUTTON_CAMERA, mCameraCallback,
                getDisableButtonCallback(ButtonManager.BUTTON_HDR));
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mPresenter.onPreviewUIReady(surface, width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        mCaptureLayoutHelper.setAspectRatio((float)width / height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public void setPreViewSize(int width, int height) {
        mTextureView.setAspectRatio(width, height);
    }

    public void setPresenter(IPhotoUIStatusListener presenter) {
        mPresenter = presenter;
    }

    @Override
    public void onShutterButtonFocus(boolean pressed) {

    }

    @Override
    public void onShutterButtonClick() {
        mPresenter.onShutterButtonClick();
    }

    @Override
    public void onShutterButtonLongPressed() {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.camera_switch:
                mPresenter.switchCamera();
                break;
        }
    }

    public interface NonDecorWindowSizeChangedListener {
        public void onNonDecorWindowSizeChanged(int width, int height, int rotation);
    }
}
