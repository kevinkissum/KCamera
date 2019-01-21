package com.example.kevin.kcamera;

import android.app.Fragment;
import android.content.ContentResolver;
import android.hardware.camera2.CameraDevice;
import android.os.SystemClock;
import android.telecom.VideoProfile;
import android.util.Log;
import android.view.View;

class PhotoModule extends CameraModule implements ModuleController {
    public static final String TAG = "PhotoModule";
    private boolean mPaused;
    private PhotoUI mUI;
    private CameraActivity mActivity;
    private AppController mAppController;
    private ContentResolver mContentResolver;
    private int mCameraId;
    private CameraDevice mCameraDevice;

    public PhotoModule(AppController app) {
    }

    @Override
    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {
        mActivity = activity;
        // TODO: Need to look at the controller interface to see if we can get
        // rid of passing in the activity directly.
        mAppController = mActivity;

//        mUI = new PhotoUI(mActivity, this, mActivity.getModuleLayoutRoot());
//        mActivity.setPreviewStatusListener(mUI);

        SettingsManager settingsManager = mActivity.getSettingsManager();
        // TODO: Move this to SettingsManager as a part of upgrade procedure.
        // Aspect Ratio selection dialog is only shown for Nexus 4, 5 and 6.
//        if (mAppController.getCameraAppUI().shouldShowAspectRatioDialog()) {
            // Switch to back camera to set aspect ratio.
//            settingsManager.setToDefault(mAppController.getModuleScope(), Keys.KEY_CAMERA_ID);
//        }
        mCameraId = settingsManager.getInteger(mAppController.getModuleScope(),
                Keys.KEY_CAMERA_ID);

        mContentResolver = mActivity.getContentResolver();

        // Surface texture is from camera screen nail and startPreview needs it.
        // This must be done before startPreview.
//        mIsImageCaptureIntent = isImageCaptureIntent();
//        mUI.setCountdownFinishedListener(this);

//        mQuickCapture = mActivity.getIntent().getBooleanExtra(EXTRA_QUICK_CAPTURE, false);
//        mHeadingSensor = new HeadingSensor(AndroidServices.instance().provideSensorManager());
//        mCountdownSoundPlayer = new SoundPlayer(mAppController.getAndroidContext());

//        try {
//            mOneCameraManager = OneCameraModule.provideOneCameraManager();
//        } catch (OneCameraException e) {
//            Log.e(TAG, "Hardware manager failed to open.");
//        }

        // TODO: Make this a part of app controller API.
//        View cancelButton = mActivity.findViewById(R.id.shutter_cancel_button);
//        cancelButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                cancelCountDown();
//            }
//        });
    }

    @Override
    public void resume() {
        mPaused = false;

//        mCountdownSoundPlayer.loadSound(R.raw.timer_final_second);
//        mCountdownSoundPlayer.loadSound(R.raw.timer_increment);
//        if (mFocusManager != null) {
            // If camera is not open when resume is called, focus manager will
            // not be initialized yet, in which case it will start listening to
            // preview area size change later in the initialization.
//            mAppController.addPreviewAreaSizeChangedListener(mFocusManager);
//        }
//        mAppController.addPreviewAreaSizeChangedListener(mUI);

        CameraProvider camProvider = mActivity.getCameraProvider();
        if (camProvider == null) {
            // No camera provider, the Activity is destroyed already.
            return;
        }

        // Close the review UI if it's currently visible.
//        mUI.hidePostCaptureAlert();
//        mUI.hideIntentReviewImageView();

        requestCameraOpen();

//        mJpegPictureCallbackTime = 0;
//        mZoomValue = 1.0f;

//        mOnResumeTime = SystemClock.uptimeMillis();
//        checkDisplayRotation();

        // If first time initialization is not finished, put it in the
        // message queue.
//        if (!mFirstTimeInitialized) {
//            mHandler.sendEmptyMessage(MSG_FIRST_TIME_INIT);
//        } else {
//            initializeSecondTime();
//        }

//        mHeadingSensor.activate();

//        getServices().getRemoteShutterListener().onModuleReady(this);
//        SessionStatsCollector.instance().sessionActive(true);
    }

    private void requestCameraOpen() {
        Log.v(TAG, "requestCameraOpen");
        mActivity.getCameraProvider().requestCamera(mCameraId, true);
    }

    @Override
    public void pause() {
        Log.v(TAG, "pause");
        mPaused = true;
//        getServices().getRemoteShutterListener().onModuleExit();
//        SessionStatsCollector.instance().sessionActive(false);
//
//        mHeadingSensor.deactivate();

        // Reset the focus first. Camera CTS does not guarantee that
        // cancelAutoFocus is allowed after preview stops.
//        if (mCameraDevice != null && mCameraState != PREVIEW_STOPPED) {
//            mCameraDevice.cancelAutoFocus();
//        }

        // If the camera has not been opened asynchronously yet,
        // and startPreview hasn't been called, then this is a no-op.
        // (e.g. onResume -> onPause -> onResume).
        stopPreview();
//        cancelCountDown();
//        mCountdownSoundPlayer.unloadSound(R.raw.timer_final_second);
//        mCountdownSoundPlayer.unloadSound(R.raw.timer_increment);
//
//        mNamedImages = null;
        // If we are in an image capture intent and has taken
        // a picture, we just clear it in onPause.
//        mJpegImageData = null;

        // Remove the messages and runnables in the queue.
//        mHandler.removeCallbacksAndMessages(null);

//        if (mMotionManager != null) {
//            mMotionManager.removeListener(mFocusManager);
//            mMotionManager = null;
//        }

        closeCamera();
//        mActivity.enableKeepScreenOn(false);
        mUI.onPause();

//        mPendingSwitchCameraId = -1;
//        if (mFocusManager != null) {
//            mFocusManager.removeMessages();
//        }
//        getServices().getMemoryManager().removeListener(this);
//        mAppController.removePreviewAreaSizeChangedListener(mFocusManager);
//        mAppController.removePreviewAreaSizeChangedListener(mUI);
//
//        SettingsManager settingsManager = mActivity.getSettingsManager();
//        settingsManager.removeListener(this);
    }

    private void closeCamera() {

    }

    private void stopPreview() {
//        if (mCameraDevice != null && mCameraState != PREVIEW_STOPPED) {
//            Log.i(TAG, "stopPreview");
//            mCameraDevice.stopPreview();
//            mFaceDetectionStarted = false;
//        }
//        setCameraState(PREVIEW_STOPPED);
//        if (mFocusManager != null) {
//            mFocusManager.onPreviewStopped();
//        }
//        SessionStatsCollector.instance().previewActive(false);

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
    public void onCameraAvailable(CameraDevice cameraProxy) {
        Log.i(TAG, "onCameraAvailable");
        if (mPaused) {
            return;
        }
        mCameraDevice = cameraProxy;

        initializeCapabilities();
        // mCameraCapabilities is guaranteed to initialized at this point.
        mAppController.getCameraAppUI().showAccessibilityZoomUI(
                mCameraCapabilities.getMaxZoomRatio());


        // Reset zoom value index.
        mZoomValue = 1.0f;
        if (mFocusManager == null) {
            initializeFocusManager();
        }
        mFocusManager.updateCapabilities(mCameraCapabilities);

        // Do camera parameter dependent initialization.
        mCameraSettings = mCameraDevice.getSettings();
        // Set a default flash mode and focus mode
        if (mCameraSettings.getCurrentFlashMode() == null) {
            mCameraSettings.setFlashMode(VideoProfile.CameraCapabilities.FlashMode.NO_FLASH);
        }
        if (mCameraSettings.getCurrentFocusMode() == null) {
            mCameraSettings.setFocusMode(VideoProfile.CameraCapabilities.FocusMode.AUTO);
        }

        setCameraParameters(UPDATE_PARAM_ALL);
        // Set a listener which updates camera parameters based
        // on changed settings.
        SettingsManager settingsManager = mActivity.getSettingsManager();
        settingsManager.addListener(this);
        mCameraPreviewParamsReady = true;

        startPreview();

        onCameraOpened();

        mHardwareSpec = new HardwareSpecImpl(getCameraProvider(), mCameraCapabilities,
                mAppController.getCameraFeatureConfig(), isCameraFrontFacing());

        ButtonManager buttonManager = mActivity.getButtonManager();
        buttonManager.enableCameraButton();
    }

    @Override
    public boolean isUsingBottomBar() {
        return false;
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
