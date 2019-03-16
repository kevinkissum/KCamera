package com.example.kevin.kcamera;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.example.kevin.kcamera.Presenter.PhotoUI2ModulePresenter;
import com.example.kevin.kcamera.View.MainActivityLayout;

import java.lang.ref.WeakReference;


public class CameraActivity extends AppCompatActivity {

    public static final String TAG = "CAM_CamActivity";
    private boolean mHasCriticalPermissions;
    private int mCameraId;
    private CameraController mCameraController;
    private Context mAppContext;
    private Handler mMainHandler;
    private PhotoModule mCurrentModule;
    private CameraAppUI mCameraUI;
    private PhotoUI mPhotoUI;
    private MainActivityLayout mRootView;
    private PhotoUI2ModulePresenter mPresenter;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPermissions();
        setContentView(R.layout.activity_camera);
        init();
     }

    private void init() {
        mRootView = (MainActivityLayout) findViewById(R.id.activity_root_view);
        mMainHandler = new MainHandler(this, getMainLooper());
        mAppContext = getApplicationContext();
        mCurrentModule = new PhotoModule(this, mMainHandler);
        mPhotoUI = new PhotoUI(mRootView);
        mPresenter = new PhotoUI2ModulePresenter(mCurrentModule, mPhotoUI);
        mCurrentModule.setPresenter(mPresenter);
        mPhotoUI.setPresenter(mPresenter);
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

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
