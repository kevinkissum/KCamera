package com.example.kevin.kcamera;

import android.Manifest;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraDevice;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.Window;

import com.example.kevin.kcamera.Ex.AndroidCamera2AgentImpl;
import com.example.kevin.kcamera.Ex.CameraAgent;
import com.example.kevin.kcamera.Interface.OnStorageUpdateDoneListener;
import com.example.kevin.kcamera.Presenter.PhotoUI2ModulePresenter;
import com.example.kevin.kcamera.View.MainActivityLayout;
import com.example.kevin.kcamera.Interface.AppController;
import com.example.kevin.kcamera.View.ModeListView;

import java.lang.ref.WeakReference;


public class CameraActivity extends AppCompatActivity implements AppController, CameraAgent.CameraOpenCallback {

    public static final String TAG = "CAM_CamActivity";

    private static final String INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE =
            "android.media.action.STILL_IMAGE_CAMERA_SECURE";
    public static final String ACTION_IMAGE_CAPTURE_SECURE =
            "android.media.action.IMAGE_CAPTURE_SECURE";

    // The intent extra for camera from secure lock screen. True if the gallery
    // should only show newly captured pictures. sSecureAlbumId does not
    // increment. This is used when switching between camera, camcorder, and
    // panorama. If the extra is not set, it is in the normal camera mode.
    public static final String SECURE_CAMERA_EXTRA = "secure_camera";

    private static final int MSG_CLEAR_SCREEN_ON_FLAG = 2;
    private static final long SCREEN_DELAY_MS = 2 * 60 * 1000; // 2 mins.
    /** Load metadata for 10 items ahead of our current. */
    private static final int FILMSTRIP_PRELOAD_AHEAD_ITEMS = 10;
    private static final int PERMISSIONS_ACTIVITY_REQUEST_CODE = 1;
    private static final int PERMISSIONS_RESULT_CODE_OK = 1;
    private static final int PERMISSIONS_RESULT_CODE_FAILED = 2;
    private boolean mHasCriticalPermissions;
    private int mCameraId;
    private CameraController mCameraController;
    private Context mAppContext;
    private Handler mMainHandler;
    private PhotoModule mCurrentModule;
    private CameraAppUI mCameraAppUI;
    private PhotoUI mPhotoUI;
    private MainActivityLayout mRootView;
    private PhotoUI2ModulePresenter mPresenter;
    private ActionBar mActionBar;
    private ButtonManager mButtonManager;
    private ModeListView mModeListView;
    private ModuleManagerImpl mModuleManager;
    private boolean mModeListVisible;
    private final Object mStorageSpaceLock = new Object();
    private long mStorageSpaceBytes = Storage.LOW_STORAGE_THRESHOLD_BYTES;
    private boolean mIsActivityRunning;
    private boolean mPaused;
    private OnScreenHint mStorageHint;
    private CameraController mCameraControl;
    private int mCurrentModeIndex;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPermissions();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_camera);
        getSupportActionBar().hide();
        mCameraControl = new CameraController(this, new AndroidCamera2AgentImpl(getApplicationContext()), mMainHandler, this);
        init();
     }

    private void init() {
        AndroidContext.initialize(getApplicationContext());
        mRootView = (MainActivityLayout) findViewById(R.id.activity_root_view);
        mMainHandler = new MainHandler(this, getMainLooper());
        mAppContext = getApplicationContext();
        mCameraAppUI = new CameraAppUI(this, mRootView);
        mCurrentModule = new PhotoModule(this, mMainHandler);
        mPresenter = new PhotoUI2ModulePresenter(mCurrentModule, mCameraAppUI);
        mCurrentModule.setPresenter(mPresenter);
        mCameraAppUI.setPresenter(mPresenter);
        mModeListView = (ModeListView) findViewById(R.id.mode_list_layout);
        mModuleManager = new ModuleManagerImpl();
        ModulesInfo.setupModules(mAppContext, mModuleManager);
        mModeListView.init(mModuleManager.getSupportedModeIndexList());

    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraAppUI.onResume();
        mCurrentModule.resume();
        updateStorageSpaceAndHint(null);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.d(TAG, "not running on M, skipping permission checks");
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
    public ButtonManager getButtonManager() {
        if (mButtonManager == null) {
            mButtonManager = new ButtonManager(this);
        }
        return mButtonManager;
    }

    @Override
    public boolean isShutterEnabled() {
        return mCameraAppUI.isShutterButtonEnabled();
    }

    @Override
    public void setShutterEnabled(boolean enabled) {
        mCameraAppUI.setShutterButtonEnabled(enabled);
    }

    @Override
    public void onPreviewStarted() {

    }

    public MainActivityLayout getModuleLayoutRoot() {
        return mRootView;
    }

    public long getStorageSpaceBytes() {
        synchronized (mStorageSpaceLock) {
            return mStorageSpaceBytes;
        }
    }

    protected void updateStorageSpaceAndHint(final OnStorageUpdateDoneListener callback) {
        /*
         * We execute disk operations on a background thread in order to
         * free up the UI thread.  Synchronizing on the lock below ensures
         * that when getStorageSpaceBytes is called, the main thread waits
         * until this method has completed.
         *
         * However, .execute() does not ensure this execution block will be
         * run right away (.execute() schedules this AsyncTask for sometime
         * in the future. executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
         * tries to execute the task in parellel with other AsyncTasks, but
         * there's still no guarantee).
         * e.g. don't call this then immediately call getStorageSpaceBytes().
         * Instead, pass in an OnStorageUpdateDoneListener.
         */
        (new AsyncTask<Void, Void, Long>() {
            @Override
            protected Long doInBackground(Void ... arg) {
                synchronized (mStorageSpaceLock) {
                    mStorageSpaceBytes = Storage.getAvailableSpace();
                    return mStorageSpaceBytes;
                }
            }

            @Override
            protected void onPostExecute(Long bytes) {
                updateStorageHint(bytes);
                // This callback returns after I/O to check disk, so we could be
                // pausing and shutting down. If so, don't bother invoking.
                if (callback != null && !mPaused) {
                    callback.onStorageUpdateDone(bytes);
                } else {
                    Log.v(TAG, "ignoring storage callback after activity pause");
                }
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    protected void updateStorageHint(long storageSpace) {
        if (!mIsActivityRunning) {
            return;
        }

        String message = null;
        if (storageSpace == Storage.UNAVAILABLE) {
            message = getString(R.string.no_storage);
        } else if (storageSpace == Storage.PREPARING) {
            message = getString(R.string.preparing_sd);
        } else if (storageSpace == Storage.UNKNOWN_SIZE) {
            message = getString(R.string.access_sd_fail);
        } else if (storageSpace <= Storage.LOW_STORAGE_THRESHOLD_BYTES) {
            message = getString(R.string.spaceIsLow_content);
        }

        if (message != null) {
            Log.w(TAG, "Storage warning: " + message);
            if (mStorageHint == null) {
                mStorageHint = OnScreenHint.makeText(CameraActivity.this, message);
            } else {
                mStorageHint.setText(message);
            }
            mStorageHint.show();

            // Disable all user interactions,
            mCameraAppUI.setDisableAllUserInteractions(true);
        } else if (mStorageHint != null) {
            mStorageHint.cancel();
            mStorageHint = null;

            // Re-enable all user interactions.
            mCameraAppUI.setDisableAllUserInteractions(false);
        }
    }

    @Override
    public void onCameraOpened(CameraAgent.CameraProxy camera) {
        Log.v(TAG, "onCameraOpened");
        if (mPaused) {
            // We've paused, but just asynchronously opened the camera. Close it
            // because we should be releasing the camera when paused to allow
            // other apps to access it.
            Log.v(TAG, "received onCameraOpened but activity is paused, closing Camera");
            mCameraController.closeCamera(false);
            return;
        }

        if (!mModuleManager.getModuleAgent(mCurrentModeIndex).requestAppForCamera()) {
            // We shouldn't be here. Just close the camera and leave.
            mCameraController.closeCamera(false);
            throw new IllegalStateException("Camera opened but the module shouldn't be " +
                    "requesting");
        }
        if (mCurrentModule != null) {
//            resetExposureCompensationToDefault(camera);
            try {
                mCurrentModule.onCameraAvailable(camera);
            } catch (RuntimeException ex) {
                Log.e(TAG, "Error connecting to camera", ex);
//                mFatalErrorHandler.onCameraOpenFailure();
            }
        } else {
            Log.v(TAG, "mCurrentModule null, not invoking onCameraAvailable");
        }
        Log.v(TAG, "invoking onChangeCamera");
//        mCameraAppUI.onChangeCamera();
    }

    @Override
    public void onCameraDisabled(int cameraId) {

    }

    @Override
    public void onDeviceOpenFailure(int cameraId, String info) {

    }

    @Override
    public void onDeviceOpenedAlready(int cameraId, String info) {

    }

    @Override
    public void onReconnectionFailure(CameraAgent mgr, String info) {

    }

    public CameraController getCameraProvider() {
        return mCameraController;
    }

    public CameraAppUI getCameraAppUI() {
        return mCameraAppUI;
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
