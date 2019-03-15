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
import android.hardware.camera2.CameraDevice;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.example.kevin.kcamera.Interface.CameraOpenCallback;
import com.example.kevin.kcamera.View.MainActivityLayout;

import java.lang.ref.WeakReference;


public class CameraActivity extends AppCompatActivity implements CameraOpenCallback {

    public static final String TAG = "CAM_CamActivity";
    private boolean mHasCriticalPermissions;
    private int mCameraId;
    private CameraController mCameraController;
    private Context mAppContext;
    private Handler mMainHandler;
    private PhotoModule mCurrentModule;
    private CameraAppUI mCameraUI;
    private MainActivityLayout mRootView;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPermissions();
        setContentView(R.layout.activity_camera);
        init();
        requestCameraOpen();
     }

    private void init() {
        mMainHandler = new MainHandler(this, getMainLooper());
        mAppContext = getApplicationContext();
        mCurrentModule = new PhotoModule(mAppContext, this);
        mCameraController = new CameraController(mAppContext, mMainHandler, this);
        mCameraUI = new CameraAppUI(mRootView);
    }

    private void requestCameraOpen() {
        mCameraController.requestCamera(mCameraId, true);
    }

    public FrameLayout getModuleLayoutRoot() {
        return mRootView;
    }


    @Override
    protected void onStart() {
        super.onStart();

    }


    /**
     * Checks if any of the needed Android runtime permissions are missing.
     * If they are, then launch the permissions activity under one of the following conditions:
     * a) The permissions dialogs have not run yet. We will ask for permission only once.
     * b) If the missing permissions are critical to the app running, we will display a fatal error dialog.
     * Critical permissions are: camera, microphone and storage. The app cannot run without them.
     * Non-critical permission is location.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.v(TAG, "not running on M, skipping permission checks");
            mHasCriticalPermissions = true;
            return;
        }

        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            mHasCriticalPermissions = true;
        } else {
            mHasCriticalPermissions = false;
            Intent intent = new Intent(this, PermissionsActivity.class);
            startActivity(intent);
            finish();
        }
    }



    @Override
    public void onCameraOpened() {
        mCurrentModule.onCameraAvailable(mCameraController);
    }

    private static class MainHandler extends Handler {
        final WeakReference<CameraActivity> mActivity;

        public MainHandler(CameraActivity activity, Looper looper) {
            super(looper);
            mActivity = new WeakReference<CameraActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {

        }
    }

}
