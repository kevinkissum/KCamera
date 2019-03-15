package com.example.kevin.kcamera;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraDevice;
import android.util.Log;
import android.util.Size;

public class PhotoModule {


    private static final String TAG = "PhotoModule";
    private CameraController mCameraControl;
    private CameraActivity mActivity;
    private Context mContext;
    private PhotoUI mUI;

    public PhotoModule(Context mAppContext, CameraActivity activity) {
        init(mAppContext, activity);
    }

    private void init(Context mAppContext, CameraActivity activity) {
        mActivity = activity;
        mContext = mAppContext;
        mUI = new PhotoUI(mActivity, this, mActivity.getModuleLayoutRoot());
    }

    public void onCameraAvailable(CameraController controller) {
        mCameraControl = controller;
        startPreview();
    }

    private void startPreview() {
        updateParametersPreViewSize();
        mCameraControl.setPreviewDisplay(mUI.getSurfaceHolder());
        mCameraControl.startPreView();
    }

    private void updateParametersPreViewSize() {
        if (mCameraControl == null) {
            Log.w(TAG, "attempting to set picture size without caemra device");
            return;
        }

        Size pictureSize;
        try {
            pictureSize = mAppController.getResolutionSetting()
                    .getPictureSize(DataModuleManager.getInstance(mAppController.getAndroidContext()),
                            mAppController.getCameraProvider()
                                    .getCurrentCameraId(), cameraFacing);
        } catch (OneCameraAccessException ex) {
            mAppController.getFatalErrorHandler()
                    .onGenericCameraAccessFailure();
            return;
        }
        mCameraSettings.setPhotoSize(pictureSize.toPortabilitySize());

        if (ApiHelper.IS_NEXUS_5) {
            if (ResolutionUtil.NEXUS_5_LARGE_16_BY_9.equals(pictureSize)) {
                mShouldResizeTo16x9 = true;
            } else {
                mShouldResizeTo16x9 = false;
            }
        }

        // SPRD: add fix bug 555245 do not display thumbnail picture in MTP/PTP Mode at pc
        mCameraSettings.setExifThumbnailSize(CameraUtil.getAdaptedThumbnailSize(pictureSize,
                mAppController.getCameraProvider()).toPortabilitySize());

        // Set a preview size that is closest to the viewfinder height and has
        // the right aspect ratio.
        List<Size> sizes = Size.convert(mCameraCapabilities
                .getSupportedPreviewSizes());
        Size optimalSize = CameraUtil.getOptimalPreviewSize(sizes,
                (double) pictureSize.width() / pictureSize.height());
        Size original = new Size(mCameraSettings.getCurrentPreviewSize());
        if (!optimalSize.equals(original)) {
            Log.i(TAG, "setting preview size. optimal: " + optimalSize
                    + "original: " + original);
            mCameraSettings.setPreviewSize(optimalSize.toPortabilitySize());
        }

        if (optimalSize.width() != 0 && optimalSize.height() != 0) {
            Log.i(TAG, "updating aspect ratio");
            mUI.updatePreviewAspectRatio((float) optimalSize.width()
                    / (float) optimalSize.height());
        }
        Log.d(TAG, "Preview size is " + optimalSize);
    }
}
