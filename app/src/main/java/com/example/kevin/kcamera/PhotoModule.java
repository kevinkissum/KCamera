package com.example.kevin.kcamera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.util.Log;
import android.util.Size;

import com.example.kevin.kcamera.Interface.ICameraControll;
import com.example.kevin.kcamera.Interface.IPhotoModuleControll;
import com.example.kevin.kcamera.Presenter.PhotoUI2ModulePresenter;

import java.util.List;

public class PhotoModule implements ICameraControll{


    private static final String TAG = "PhotoModule";
    private CameraController mCameraControl;
    private CameraActivity mActivity;
    private Context mContext;
    private PhotoUI mUI;
    private IPhotoModuleControll mPhotoControl;
    private boolean mPaused;
    private int mCameraId;
    private AndroidCamera2Settings mCameraSettings;

    public PhotoModule(CameraActivity activity, Handler handler) {
        mCameraControl = new CameraController(activity, handler, this);
        mContext = activity.getApplicationContext();
        mActivity = activity;
        init();
    }

    public void init() {
        mUI = new PhotoUI(mActivity, mActivity.getModuleLayoutRoot());

    }


    public void openCamera(SurfaceTexture surface, int width, int height) {
        mCameraControl.setSurfaceTexture(surface, width, height);
        mCameraControl.requestCamera(mCameraId, true);
    }


    @Override
    public void onCameraOpened() {
        Log.d(TAG, "onCameraAvailable");
        if (mPaused) {
            return;
        }
        if (mCameraSettings == null) {
            mCameraSettings = mCameraControl.getCameraSettings();
        }
        startPreview();

    }

    public void startPreview() {
        updateSettingAfterOpencamera();
        mCameraControl.startPreview();
    }

    private void updateSettingAfterOpencamera() {
        updateParametersPictureSize();
    }

    private void updateParametersPictureSize() {
        if (mCameraControl == null) {
            Log.w(TAG, "attempting to set picture size without caemra device");
            return;
        }

        Size pictureSize = Util.getLargestPictureSize(mContext, mCameraId);
        Size preViewSize = Util.getBestPreViewSize(mContext, mCameraId);
        mCameraSettings.setPhotoSize(pictureSize);
        mCameraSettings.setPreviewSize(preViewSize);
        mCameraControl.applySettings(mCameraSettings);
        Size currentSize = mCameraSettings.getCurrentPreviewSize();

        if (currentSize.getWidth() != 0 && currentSize.getHeight() != 0) {
            Log.v(TAG, "updating aspect ratio");
            mPhotoControl.updatePreviewAspectRatio(currentSize.getHeight(), currentSize.getWidth());
        }
        Log.d(TAG, "PictureSize is " + pictureSize);
        Log.d(TAG, "Preview size is " + preViewSize);
    }

    public void takePicture() {
        mCameraControl.startTakePicture();
    }

    public void switchCamera() {
        if (mPaused) {
            return;
        }
        closeCamera();
        mCameraId ^= 1;
        mCameraControl.requestCamera(mCameraId, true);
    }

    private void closeCamera() {
        mCameraControl.closeCamera();
    }

    public void openCamera() {
        mCameraControl.requestCamera(mCameraId, true);
    }

    public void setPresenter(IPhotoModuleControll presenter) {
        mPhotoControl = presenter;
    }
}
