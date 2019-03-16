package com.example.kevin.kcamera;

import android.graphics.SurfaceTexture;
import android.view.TextureView;
import android.widget.FrameLayout;

import com.example.kevin.kcamera.Interface.IPhotoUIStatusListener;
import com.example.kevin.kcamera.Presenter.PhotoUI2ModulePresenter;
import com.example.kevin.kcamera.View.MainActivityLayout;

public class PhotoUI implements TextureView.SurfaceTextureListener{

    private FrameLayout mRootView;
    private TextureView mTextureView;
    private IPhotoUIStatusListener mPresenter;


    public PhotoUI(MainActivityLayout rootView) {
        mRootView = rootView;
        init();
    }

    private void init() {
        mTextureView = (TextureView) mRootView.findViewById(R.id.preview_content);
        mTextureView.setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mPresenter.onPreviewUIReady(surface, width, height);
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

    public void setPreViewSize(int width, int height) {

    }

    public void setPresenter(IPhotoUIStatusListener presenter) {
        mPresenter = presenter;
    }
}
