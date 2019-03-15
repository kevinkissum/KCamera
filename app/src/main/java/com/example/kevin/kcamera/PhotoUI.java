package com.example.kevin.kcamera;

import android.view.TextureView;
import android.widget.FrameLayout;

public class PhotoUI {

    private final FrameLayout mRootView;
    private final PhotoModule mController;
    private final CameraActivity mActivity;
    private TextureView mTextureView;

    public PhotoUI(CameraActivity activity, PhotoModule controller, FrameLayout parent) {
        mActivity = activity;
        mController = controller;
        mRootView = parent;
        init();
    }

    private void init() {
        mTextureView = (TextureView) mRootView.findViewById(R.id.preview_content);
    }

    public TextureView getSurfaceHolder() {
        return mTextureView;
    }
}
