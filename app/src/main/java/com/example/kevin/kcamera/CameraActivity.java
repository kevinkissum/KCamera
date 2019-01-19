package com.example.kevin.kcamera;

import android.Manifest;
import android.app.ActionBar;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;


public class CameraActivity extends AppCompatActivity implements AppController{

    public static final String TAG = "CAM_CamActivity";
    private boolean mHasCriticalPermissions;
    private CameraController mCameraController;
    private Context mAppContext;
    private Handler mMainHandler;
    private ModuleManagerImpl mModuleManager;
    private ActionBar mActionBar;
    private int mCurrentModeIndex;
    private CameraModule mCurrentModule;
    private CameraAppUI mCameraAppUI;
    private boolean mIsActivityRunning;
    private boolean mPaused;
    private SettingsManager mSettingsManager;

    @Override
    public Context getAndroidContext() {
        return null;
    }

    @Override
    public Dialog createDialog() {
        return null;
    }

    @Override
    public String getModuleScope() {
        return null;
    }

    @Override
    public String getCameraScope() {
        return null;
    }

    @Override
    public void launchActivityByIntent(Intent intent) {

    }

    @Override
    public boolean isPaused() {
        return false;
    }

    @Override
    public ModuleController getCurrentModuleController() {
        return null;
    }

    @Override
    public int getCurrentModuleIndex() {
        return 0;
    }

    @Override
    public int getModuleId(int modeIndex) {
        return 0;
    }

    @Override
    public int getQuickSwitchToModuleId(int currentModuleIndex) {
        return 0;
    }

    @Override
    public int getPreferredChildModeIndex(int modeIndex) {
        return 0;
    }

    @Override
    public void onModeSelected(int modeIndex) {
        if (mCurrentModeIndex == modeIndex) {
            return;
        }

//        CameraPerformanceTracker.onEvent(CameraPerformanceTracker.MODE_SWITCH_START);
        // Record last used camera mode for quick switching
        if (modeIndex == getResources().getInteger(R.integer.camera_mode_photo)
                || modeIndex == getResources().getInteger(R.integer.camera_mode_gcam)) {
//            mSettingsManager.set(SettingsManager.SCOPE_GLOBAL,
//                    Keys.KEY_CAMERA_MODULE_LAST_USED,
//                    modeIndex);
        }

        closeModule(mCurrentModule);

        // Select the correct module index from the mode switcher index.
        modeIndex = getPreferredChildModeIndex(modeIndex);
        setModuleFromModeIndex(modeIndex);

//        mCameraAppUI.resetBottomControls(mCurrentModule, modeIndex);
//        mCameraAppUI.addShutterListener(mCurrentModule);
        openModule(mCurrentModule);
        // Store the module index so we can use it the next time the Camera
        // starts up.
//        mSettingsManager.set(SettingsManager.SCOPE_GLOBAL,
//                Keys.KEY_STARTUP_MODULE_INDEX, modeIndex);
    }

    private void openModule(CameraModule module) {
        module.init(this, isSecureCamera(), isCaptureIntent());
//        module.hardResetSettings(mSettingsManager);
        // Hide accessibility zoom UI by default. Modules will enable it themselves if required.
//        getCameraAppUI().hideAccessibilityZoomUI();
        if (!mPaused) {
            module.resume();
//            UsageStatistics.instance().changeScreen(currentUserInterfaceMode(),
//                    NavigationChange.InteractionCause.BUTTON);
//            updatePreviewVisibility();
        }
    }

    private boolean isSecureCamera() {
        return false;
    }

    private void closeModule(CameraModule module) {
        module.pause();
//        mCameraAppUI.clearModuleUI();
    }

    @Override
    public void onSettingsSelected() {

    }

    @Override
    public void freezeScreenUntilPreviewReady() {

    }

    @Override
    public SurfaceTexture getPreviewBuffer() {
        return null;
    }

    @Override
    public void onPreviewReadyToStart() {

    }

    @Override
    public void onPreviewStarted() {

    }

    @Override
    public void setupOneShotPreviewListener() {

    }

    @Override
    public void updatePreviewAspectRatio(float aspectRatio) {

    }

    @Override
    public void updatePreviewTransformFullscreen(Matrix matrix, float aspectRatio) {

    }

    @Override
    public RectF getFullscreenRect() {
        return null;
    }

    @Override
    public void updatePreviewTransform(Matrix matrix) {

    }

    @Override
    public FrameLayout getModuleLayoutRoot() {
        return null;
    }

    @Override
    public void lockOrientation() {

    }

    @Override
    public void unlockOrientation() {

    }

    @Override
    public void setShutterEventsListener(ShutterEventsListener listener) {

    }

    @Override
    public void setShutterEnabled(boolean enabled) {

    }

    @Override
    public boolean isShutterEnabled() {
        return false;
    }

    @Override
    public void startFlashAnimation(boolean shortFlash) {

    }

    @Override
    public void startPreCaptureAnimation() {

    }

    @Override
    public void cancelPreCaptureAnimation() {

    }

    @Override
    public void startPostCaptureAnimation() {

    }

    @Override
    public void startPostCaptureAnimation(Bitmap thumbnail) {

    }

    @Override
    public void cancelPostCaptureAnimation() {

    }

    @Override
    public void notifyNewMedia(Uri uri) {

    }

    @Override
    public void enableKeepScreenOn(boolean enabled) {

    }

    @Override
    public CameraProvider getCameraProvider() {
        return mCameraController;
    }

    @Override
    public LocationManager getLocationManager() {
        return null;
    }

    @Override
    public SettingsManager getSettingsManager() {
        return mSettingsManager;
    }

    @Override
    public CameraServices getServices() {
        return CameraServicesImpl.instance();
    }

    @Override
    public ModuleManager getModuleManager() {
        return null;
    }

    @Override
    public boolean isAutoRotateScreen() {
        return false;
    }

    @Override
    public void finishActivityWithIntentCompleted(Intent resultIntent) {

    }

    @Override
    public void finishActivityWithIntentCanceled() {

    }

    private class MainHandler extends Handler {
        public MainHandler(CameraActivity cameraActivity, Looper mainLooper) {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAppContext = getApplicationContext();
        mMainHandler = new MainHandler(this, getMainLooper());
        mSettingsManager = getServices().getSettingsManager();
        checkSelfPermission();
        try {
            mCameraController = new CameraController(/*mAppContext, this, mMainHandler,
                    CameraAgentFactory.getAndroidCameraAgent(mAppContext,
                            CameraAgentFactory.CameraApi.API_1),
                    CameraAgentFactory.getAndroidCameraAgent(mAppContext,
                            CameraAgentFactory.CameraApi.AUTO),
                    mActiveCameraDeviceTracker);
            mCameraController.setCameraExceptionHandler(
                    new CameraExceptionHandler(mCameraExceptionCallback, mMainHandler)*/);
        } catch (AssertionError e) {
            Log.e(TAG, "Creating camera controller failed.", e);
//            mFatalErrorHandler.onGenericCameraAccessFailure();
        }
        mModuleManager = new ModuleManagerImpl();
        ModulesInfo.setupModules(mAppContext, mModuleManager, /*mFeatureConfig*/null);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_camera);
//        getWindow().setBackgroundDrawable(null);
        mActionBar = getActionBar();
//        mActionBar.setBackgroundDrawable(new ColorDrawable(0x00000000));
        setModuleFromModeIndex(getModeIndex());
//        mCameraAppUI = new CameraAppUI(this,
//                (MainActivityLayout) findViewById(R.id.activity_root_view), isCaptureIntent());


    }

    @Override
    protected void onStart() {
        super.onStart();
        mIsActivityRunning = true;
        /*
         * If we're starting after launching a different Activity (lockscreen),
         * we need to use the last mode used in the other Activity, and
         * not the old one from this Activity.
         *
         * This needs to happen before CameraAppUI.resume() in order to set the
         * mode cover icon to the actual last mode used.
         *
         * Right now we exclude capture intents from this logic.
         */
        int modeIndex = getModeIndex();
        if (!isCaptureIntent() && mCurrentModeIndex != modeIndex) {
            onModeSelected(modeIndex);
        }
    }

    private boolean isCaptureIntent() {
        if (MediaStore.ACTION_VIDEO_CAPTURE.equals(getIntent().getAction())
                || MediaStore.ACTION_IMAGE_CAPTURE.equals(getIntent().getAction())
                || MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(getIntent().getAction())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets the mCurrentModuleIndex, creates a new module instance for the given
     * index an sets it as mCurrentModule.
     */
    private void setModuleFromModeIndex(int modeIndex) {
        ModuleManagerImpl.ModuleAgent agent = mModuleManager.getModuleAgent(modeIndex);
        if (agent == null) {
            return;
        }
        if (!agent.requestAppForCamera()) {
//            mCameraController.closeCamera(true);
        }
        mCurrentModeIndex = agent.getModuleId();
        mCurrentModule = (CameraModule) agent.createModule(this, getIntent());
    }

    private int getModeIndex() {
        int modeIndex = /*-1*/0;
        int photoIndex = getResources().getInteger(R.integer.camera_mode_photo);
        int videoIndex = getResources().getInteger(R.integer.camera_mode_video);
        int gcamIndex = getResources().getInteger(R.integer.camera_mode_gcam);
        int captureIntentIndex =
                getResources().getInteger(R.integer.camera_mode_capture_intent);
        String intentAction = getIntent().getAction();
        Log.d(TAG, " action " + intentAction);
        if (MediaStore.INTENT_ACTION_VIDEO_CAMERA.equals(intentAction)
                || MediaStore.ACTION_VIDEO_CAPTURE.equals(intentAction)) {
            modeIndex = videoIndex;
        } else if (MediaStore.ACTION_IMAGE_CAPTURE.equals(intentAction)
                || MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(intentAction)) {
            // Capture intent.
            modeIndex = captureIntentIndex;
        } else if (MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA.equals(intentAction)
                ||MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE.equals(intentAction)
                || MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(intentAction)) {
//            modeIndex = mSettingsManager.getInteger(SettingsManager.SCOPE_GLOBAL,
//                    Keys.KEY_CAMERA_MODULE_LAST_USED);

            // For upgraders who have not seen the aspect ratio selection screen,
            // we need to drop them back in the photo module and have them select
            // aspect ratio.
            // TODO: Move this to SettingsManager as an upgrade procedure.
//            if (!mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL,
//                    Keys.KEY_USER_SELECTED_ASPECT_RATIO)) {
//                modeIndex = photoIndex;
//            }
//        } else {
            // If the activity has not been started using an explicit intent,
            // read the module index from the last time the user changed modes
//            modeIndex = mSettingsManager.getInteger(SettingsManager.SCOPE_GLOBAL,
//                    Keys.KEY_STARTUP_MODULE_INDEX);
//            if ((modeIndex == gcamIndex &&
//                    !GcamHelper.hasGcamAsSeparateModule(mFeatureConfig)) || modeIndex < 0) {
//                modeIndex = photoIndex;
//            }
        }
        return modeIndex;
    }

    /*
    check camera audio extenal storage, location permission
     */
    private void checkSelfPermission() {
        //if API > M
        if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
            Log.v(TAG, "not running on M, skipping permission checks");
            mHasCriticalPermissions = true;
            return;
        }
        //check camera, record, read external
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            mHasCriticalPermissions = true;
        } else {
            mHasCriticalPermissions = false;
        }
        //start permission activity2332
        if ((checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED /*&&
                !mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HAS_SEEN_PERMISSIONS_DIALOGS)*/) ||
                !mHasCriticalPermissions) {
            Intent intent = new Intent(this, PermissionsActivity.class);
            startActivity(intent);
            finish();
        }

    }

}
