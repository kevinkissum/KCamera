package com.example.kevin.kcamera;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;


public class CameraActivity extends AppCompatActivity {

    public static final String TAG = "CAM_CamActivity";
    private boolean mHasCriticalPermissions;
    private CameraController mCameraController;
    private Context mAppContext;
    private Handler mMainHandler;
    private ModuleManagerImpl mModuleManager;

    private class MainHandler extends Handler {
        public MainHandler(CameraActivity cameraActivity, Looper mainLooper) {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
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
        ModulesInfo.setupModules(mAppContext, mModuleManager, mFeatureConfig);

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
