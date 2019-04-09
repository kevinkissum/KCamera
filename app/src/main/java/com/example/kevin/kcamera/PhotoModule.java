package com.example.kevin.kcamera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;

import com.example.kevin.kcamera.Abstract.CameraModule;
import com.example.kevin.kcamera.Ex.AndroidCamera2Settings;
import com.example.kevin.kcamera.Ex.CameraAgent;
import com.example.kevin.kcamera.Ex.CameraCapabilities;
import com.example.kevin.kcamera.Interface.AppController;
import com.example.kevin.kcamera.Interface.IPhotoModuleControll;
import com.example.kevin.kcamera.Interface.PhotoController;

import java.lang.ref.WeakReference;

public class PhotoModule extends CameraModule implements PhotoController {


    private static final String TAG = "PhotoModule";
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
    private CameraAgent.CameraProxy mCameraProxy;
    private final Handler mHandler = new MainHandler(this);


    private final Runnable mDoSnapRunnable = new Runnable() {
        @Override
        public void run() {
            onShutterButtonClick();
        }
    };

    public PhotoModule(CameraActivity activity, Handler handler) {
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

    public void requestCameraOpen(SurfaceTexture surface, int width, int height) {
        mCameraControl.setSurfaceTexture(surface, width, height);
        mCameraControl.requestCamera(mCameraId, true);
    }

    private void requestCameraOpen() {
        Log.v(TAG, "requestCameraOpen");
        mActivity.getCameraProvider().requestCamera(mCameraId, true);
    }

    public void startPreview() {
        if (mCameraProxy == null) {
            Log.i(TAG, "attempted to start preview before camera device");
            // do nothing
            return;
        }

        if (!checkPreviewPreconditions()) {
            return;
        }

        updateParametersPictureSize();

        mCameraProxy.setPreviewTexture(mActivity.getCameraAppUI().getSurfaceTexture());

        Log.i(TAG, "startPreview");

        CameraAgent.CameraStartPreviewCallback startPreviewCallback =
                new CameraAgent.CameraStartPreviewCallback() {
                    @Override
                    public void onPreviewStarted() {
//                        mFocusManager.onPreviewStarted();
                        PhotoModule.this.onPreviewStarted();
                        if (mSnapshotOnIdle) {
                            mHandler.post(mDoSnapRunnable);
                        }
                    }
                };
//        if (GservicesHelper.useCamera2ApiThroughPortabilityLayer(mActivity.getContentResolver())) {
            mCameraProxy.startPreview();
            startPreviewCallback.onPreviewStarted();
//        } else {
//            mCameraDevice.startPreviewWithCallback(new Handler(Looper.getMainLooper()),
//                    startPreviewCallback);
//        }
        /*mCameraControl.startPreview();*/
    }

    private void onPreviewStarted() {
        mAppController.onPreviewStarted();
        mAppController.setShutterEnabled(true);
        setCameraState(IDLE);
//        startFaceDetection();
    }

    private void setCameraState(int idle) {

    }


    /**
     * Returns whether we can/should start the preview or not.
     */
    private boolean checkPreviewPreconditions() {
        if (mPaused) {
            return false;
        }

        if (mCameraProxy == null) {
            Log.w(TAG, "startPreview: camera device not ready yet.");
            return false;
        }

//        SurfaceTexture st = mActivity.getCameraAppUI().getSurfaceTexture();
//        if (st == null) {
//            Log.w(TAG, "startPreview: surfaceTexture is not ready.");
//            return false;
//        }
//
//        if (!mCameraPreviewParamsReady) {
//            Log.w(TAG, "startPreview: parameters for preview is not ready.");
//            return false;
//        }
        return true;
    }

    private void updateParametersPictureSize() {
        if (mCameraProxy == null) {
            Log.w(TAG, "attempting to set picture size without caemra device");
            return;
        }

        Size pictureSize = CameraUtil.getLargestPictureSize(mContext, mCameraId);
        Size preViewSize = CameraUtil.getBestPreViewSize(mContext, mCameraId);
        mCameraSettings.setPhotoSize(pictureSize);
        mCameraSettings.setPreviewSize(preViewSize);
//        mCameraProxy.applySettings(mCameraSettings);
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

        /*mCameraControl.startTakePicture();*/
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
        requestCameraOpen();
    }

    private void closeCamera() {
        mCameraControl.closeCamera();
    }


    public void setPresenter(IPhotoModuleControll presenter) {
        mPhotoControl = presenter;
    }

    @Override
    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {

    }

    @Override
    public void resume() {
        mPaused = false;
        requestCameraOpen();

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
    public void onCameraAvailable(CameraAgent.CameraProxy cameraProxy) {
        Log.i(TAG, "onCameraAvailable");
        if (mPaused) {
            return;
        }
        mCameraProxy = cameraProxy;
        startPreview();
//        onCameraOpened();

    }

    // Snapshots can only be taken after this is called. It should be called
    // once only. We could have done these things in onCreate() but we want to
    // make preview screen appear as soon as possible.
    private void initializeFirstTime() {
//        if (mFirstTimeInitialized || mPaused) {
//            return;
//        }
//
//        mUI.initializeFirstTime();
//
//        // We set the listener only when both service and shutterbutton
//        // are initialized.
//        getServices().getMemoryManager().addListener(this);
//
//        mNamedImages = new NamedImages();
//
//        mFirstTimeInitialized = true;
//        addIdleHandler();
//
//        mActivity.updateStorageSpaceAndHint(null);
    }

    // If the Camera is idle, update the parameters immediately, otherwise
    // accumulate them in mUpdateSet and update later.
    private void setCameraParametersWhenIdle(int additionalUpdateSet) {
//        mUpdateSet |= additionalUpdateSet;
//        if (mCameraDevice == null) {
//            // We will update all the parameters when we open the device, so
//            // we don't need to do anything now.
//            mUpdateSet = 0;
//            return;
//        } else if (isCameraIdle()) {
//            setCameraParameters(mUpdateSet);
//            updateSceneMode();
//            mUpdateSet = 0;
//        } else {
//            if (!mHandler.hasMessages(MSG_SET_CAMERA_PARAMETERS_WHEN_IDLE)) {
//                mHandler.sendEmptyMessageDelayed(MSG_SET_CAMERA_PARAMETERS_WHEN_IDLE, 1000);
//            }
//        }
    }

    /**
     * This Handler is used to post message back onto the main thread of the
     * application
     */
    private static class MainHandler extends Handler {
        // Messages defined for the UI thread handler.
        private static final int MSG_FIRST_TIME_INIT = 1;
        private static final int MSG_SET_CAMERA_PARAMETERS_WHEN_IDLE = 2;
        private final WeakReference<PhotoModule> mModule;

        public MainHandler(PhotoModule module) {
            super(Looper.getMainLooper());
            mModule = new WeakReference<PhotoModule>(module);
        }

        @Override
        public void handleMessage(Message msg) {
            PhotoModule module = mModule.get();
            if (module == null) {
                return;
            }
            switch (msg.what) {
                case MSG_FIRST_TIME_INIT: {
                    module.initializeFirstTime();
                    break;
                }

                case MSG_SET_CAMERA_PARAMETERS_WHEN_IDLE: {
                    module.setCameraParametersWhenIdle(0);
                    break;
                }
            }
        }
    }
}
