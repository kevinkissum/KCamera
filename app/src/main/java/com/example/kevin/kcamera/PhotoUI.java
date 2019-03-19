package com.example.kevin.kcamera;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.view.TextureView;
import android.widget.FrameLayout;

import com.example.kevin.kcamera.Interface.IPhotoUIStatusListener;
import com.example.kevin.kcamera.Presenter.PhotoUI2ModulePresenter;
import com.example.kevin.kcamera.View.AutoFitTextureView;
import com.example.kevin.kcamera.View.MainActivityLayout;
import com.example.kevin.kcamera.View.ShutterButton;

public class PhotoUI implements TextureView.SurfaceTextureListener, ShutterButton.OnShutterButtonListener{

    private MainActivityLayout mRootView;
    private AutoFitTextureView mTextureView;
    private IPhotoUIStatusListener mPresenter;
    private ShutterButton mShutter;
    private CaptureLayoutHelper mCaptureLayoutHelper;
    private Context mAppContext;


    public PhotoUI(Context context, MainActivityLayout rootView) {
        mRootView = rootView;
        mAppContext = context;
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
        android.util.Log.d("kk", "  photoUI shutter click ");
        mPresenter.onShutterButtonClick();
    }

    @Override
    public void onShutterButtonLongPressed() {

    }

    public interface NonDecorWindowSizeChangedListener {
        public void onNonDecorWindowSizeChanged(int width, int height, int rotation);
    }
}
