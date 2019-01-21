package com.example.kevin.kcamera;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Created by kevin on 2019/1/15.
 */

public class CameraController implements /*CameraAgent.CameraOpenCallback,*/ CameraProvider , CameraDevice.StateCallback {

    public static final String TAG = "CameraController";
    private final Context mContext;
    private final Handler mCallbackHandler;
    private int mRequestingCameraId = -1;
    private CameraDevice.StateCallback mStateCallback;
    private CameraDevice mCameraProxy;
    private static final int EMPTY_REQUEST = -1;

    public CameraController(Context context, CameraDevice.StateCallback cb, Handler handler) {
        mContext = context;
        mCallbackHandler = handler;
        mStateCallback = cb;
    }

    @Override
    public void requestCamera(int id, boolean useNewApi) {
        Log.v(TAG, "requestCamera");
        // Based on
        // (mRequestingCameraId == id, mRequestingCameraId == EMPTY_REQUEST),
        // we have (T, T), (T, F), (F, T), (F, F).
        // (T, T): implies id == EMPTY_REQUEST. We don't allow this to happen
        //         here. Return.
        // (F, F): A previous request hasn't been fulfilled yet. Return.
        // (T, F): Already requested the same camera. No-op. Return.
        // (F, T): Nothing is going on. Continue.
//        if (mRequestingCameraId != EMPTY_REQUEST || mRequestingCameraId == id) {
//            return;
//        }
//        if (mInfo == null) {
//            return;
//        }
        mRequestingCameraId = id;
//        mActiveCameraDeviceTracker.onCameraOpening(CameraId.fromLegacyId(id));

        // Only actually use the new API if it's supported on this device.
//        useNewApi = mCameraAgentNg != null && useNewApi;
//        CameraAgent cameraManager = useNewApi ? mCameraAgentNg : mCameraAgent;
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);

//        if (mCameraProxy == null) {
            // No camera yet.
//            checkAndOpenCamera(cameraManager, id, mCallbackHandler, this);
            checkAndOpenCamera(manager, id, mCallbackHandler, mStateCallback);
//        } else if (mCameraProxy.getCameraId() != id || mUsingNewApi != useNewApi) {
//            boolean syncClose = GservicesHelper.useCamera2ApiThroughPortabilityLayer(mContext
//                    .getContentResolver());
//            Log.v(TAG, "different camera already opened, closing then reopening");
            // Already has camera opened, and is switching cameras and/or APIs.
//            if (mUsingNewApi) {
//                mCameraAgentNg.closeCamera(mCameraProxy, true);
//            } else {
                // if using API2 ensure API1 usage is also synced
//                mCameraAgent.closeCamera(mCameraProxy, syncClose);
//            }
//            checkAndOpenCamera(cameraManager, id, mCallbackHandler, this);
//        } else {
            // The same camera, just do a reconnect.
//            Log.v(TAG, "reconnecting to use the existing camera");
//            mCameraProxy.reconnect(mCallbackHandler, this);
//            mCameraProxy = null;
//        }

//        mUsingNewApi = useNewApi;
//        mInfo = cameraManager.getCameraDeviceInfo();
    }

    private static void checkAndOpenCamera(CameraManager cameraManager,
                                           final int cameraId, Handler handler, final CameraDevice.StateCallback cb) {
        Log.v(TAG, "checkAndOpenCamera");
        try {
//            CameraUtil.throwIfCameraDisabled();
            cameraManager.openCamera(cameraId, this, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
//            handler.post(new Runnable() {
//                @Override
//                public void run() {
//                    cb.onCameraDisabled(cameraId);
//                }
//            });
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }


    public void closeCamera(boolean b) {

    }

    @Override
    public void requestCamera(int id) {
        requestCamera(id, false);
    }

    @Override
    public boolean waitingForCamera() {
        return false;
    }

    @Override
    public void releaseCamera(int id) {

    }

    @Override
    public CameraId getCurrentCameraId() {
        return null;
    }

    @Override
    public int getNumberOfCameras() {
        return 0;
    }

    @Override
    public int getFirstBackCameraId() {
        return 0;
    }

    @Override
    public int getFirstFrontCameraId() {
        return 0;
    }

    @Override
    public boolean isFrontFacingCamera(int id) {
        return false;
    }

    @Override
    public boolean isBackFacingCamera(int id) {
        return false;
    }

    @Override
    public void onOpened(@NonNull CameraDevice camera) {
        Log.v(TAG, "onCameraOpened");
//        if (mRequestingCameraId != camera.getCameraId()) {
//            return;
//        }
        mCameraProxy = camera;
        mRequestingCameraId = EMPTY_REQUEST;
        if (mStateCallback != null) {
            mStateCallback.onOpened(camera);
        }
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice camera) {
        Log.v(TAG, "onDisconnected");
        if (mStateCallback != null) {
            mStateCallback.onDisconnected(camera);
        }
    }

    @Override
    public void onError(@NonNull CameraDevice camera, int i) {
        Log.v(TAG, "onError");
        if (mStateCallback != null) {
            mStateCallback.onError(camera, i);
        }
    }
}
