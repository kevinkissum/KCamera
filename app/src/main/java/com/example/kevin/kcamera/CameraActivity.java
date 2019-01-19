package com.example.kevin.kcamera;

import android.Manifest;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;


public class CameraActivity extends AppCompatActivity {

    public static final String TAG = "CAM_CamActivity";
    private boolean mHasCriticalPermissions;
    private CameraController mCameraController;
    private Context mAppContext;
    private Handler mMainHandler;
    private ModuleManagerImpl mModuleManager;
    private ActionBar mActionBar;
    private int mCurrentModeIndex;
    private CameraModule mCurrentModule;

    private class MainHandler extends Handler {
        public MainHandler(CameraActivity cameraActivity, Looper mainLooper) {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAppContext = getApplicationContext();
        mMainHandler = new MainHandler(this, getMainLooper());
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
            mCameraController.closeCamera(true);
        }
        mCurrentModeIndex = agent.getModuleId();
        mCurrentModule = (CameraModule) agent.createModule(this, getIntent());
    }

    private int getModeIndex() {
        int modeIndex = -1;
        int photoIndex = getResources().getInteger(R.integer.camera_mode_photo);
        int videoIndex = getResources().getInteger(R.integer.camera_mode_video);
        int gcamIndex = getResources().getInteger(R.integer.camera_mode_gcam);
        int captureIntentIndex =
                getResources().getInteger(R.integer.camera_mode_capture_intent);
        String intentAction = getIntent().getAction();
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
