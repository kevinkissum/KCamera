package com.example.kevin.kcamera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;

import com.example.kevin.kcamera.Abstract.CameraModule;
import com.example.kevin.kcamera.Ex.AndroidCamera2Settings;
import com.example.kevin.kcamera.Ex.CameraCapabilities;
import com.example.kevin.kcamera.Interface.AppController;
import com.example.kevin.kcamera.Interface.ICameraControll;
import com.example.kevin.kcamera.Interface.IPhotoModuleControll;
import com.example.kevin.kcamera.Interface.PhotoController;

public class PhotoModule extends CameraModule implements ICameraControll, PhotoController {


    private static final String TAG = "PhotoModule";
    private CameraController mCameraControl;
    private CameraActivity mActivity;
    private Context mContext;
    private PhotoUI mUI;
    private IPhotoModuleControll mPhotoControl;
    private boolean mPaused;
    private int mCameraId;
    private AndroidCamera2Settings mCameraSettings;
    private int mCameraState;
    private AppController mAppController;
    private boolean mVolumeButtonClickedFlag;
    private FocusOverlayManager mFocusManager;
    private boolean mIsImageCaptureIntent;
    private boolean mSnapshotOnIdle;
    private CameraCapabilities.SceneMode mSceneMode;


    public PhotoModule(CameraActivity activity, Handler handler) {
        mCameraControl = new CameraController(activity, handler, this);
        mContext = activity.getApplicationContext();
        mActivity = activity;
        mAppController = activity;
        init();
    }

    public void init() {
        mUI = new PhotoUI(mActivity, mActivity.getModuleLayoutRoot());
        mIsImageCaptureIntent = isImageCaptureIntent();

    }

    @Override
    public boolean isImageCaptureIntent() {
        String action = mActivity.getIntent().getAction();
        return (MediaStore.ACTION_IMAGE_CAPTURE.equals(action)
                || CameraActivity.ACTION_IMAGE_CAPTURE_SECURE.equals(action));
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

        Size pictureSize = CameraUtil.getLargestPictureSize(mContext, mCameraId);
        Size preViewSize = CameraUtil.getBestPreViewSize(mContext, mCameraId);
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

    public void onShutterButtonClick() {
        if (mPaused || (mCameraState == SWITCHING_CAMERA)
                || (mCameraState == PREVIEW_STOPPED)
                || !mAppController.isShutterEnabled()) {
            mVolumeButtonClickedFlag = false;
            return;
        }
        // Do not take the picture if there is not enough storage.
        if (mActivity.getStorageSpaceBytes() <= Storage.LOW_STORAGE_THRESHOLD_BYTES) {
            Log.e(TAG, "Not enough space or storage not ready. remaining="
                    + mActivity.getStorageSpaceBytes());
            mVolumeButtonClickedFlag = false;
            return;
        }

        mAppController.setShutterEnabled(false);

//        int countDownDuration = mActivity.getSettingsManager()
//                .getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_COUNTDOWN_DURATION);
//        mTimerDuration = countDownDuration;
//        if (countDownDuration > 0) {
//            // Start count down.
//            mAppController.getCameraAppUI().transitionToCancel();
//            mAppController.getCameraAppUI().hideModeOptions();
//            mUI.startCountdown(countDownDuration);
//            return;
//        } else {
            focusAndCapture();
//        }

        mCameraControl.startTakePicture();
    }

    private void focusAndCapture() {
        // If the user wants to do a snapshot while the previous one is still
        // in progress, remember the fact and do it after we finish the previous
        // one and re-start the preview. Snapshot in progress also includes the
        // state that autofocus is focusing and a picture will be taken when
        // focus callback arrives.
        if ((mFocusManager.isFocusingSnapOnFinish() || mCameraState == SNAPSHOT_IN_PROGRESS)) {
            if (!mIsImageCaptureIntent) {
                mSnapshotOnIdle = true;
            }
            return;
        }

        mSnapshotOnIdle = false;
        mFocusManager.focusAndCapture(mCameraSettings.getCurrentFocusMode());
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

    @Override
    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {

    }

    @Override
    public void resume() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void onPreviewVisibilityChanged(int visibility) {

    }

    @Override
    public void onLayoutOrientationChanged(boolean isLandscape) {

    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void onCameraAvailable() {

    }
}
