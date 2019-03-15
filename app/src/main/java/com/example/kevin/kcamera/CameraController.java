package com.example.kevin.kcamera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.example.kevin.kcamera.Interface.CameraOpenCallback;

public class CameraController extends CameraDevice.StateCallback {

    public static final String TAG = "CameraController";
    private final Handler mainHandler;
    private int mRequestingCameraId;
    private CameraManager mCameraManager;

    private CameraOpenCallback mReceiver;
    private CameraDevice mCameraDevice;
    private SurfaceTexture mPreviewTexture;
    private AndroidCamera2Settings mCameraSetting;

    public CameraController(Context appContext, Handler handler, CameraOpenCallback receiver) {
        mCameraManager = (CameraManager)appContext.getSystemService(Context.CAMERA_SERVICE);
        mainHandler = handler;
        mReceiver = receiver;
        mCameraSetting = new AndroidCamera2Settings();
    }

    public void requestCamera(int id, boolean useNewApi) {
        Log.v(TAG, "requestCamera");
        if (mRequestingCameraId == id) {
            return;
        }

        mRequestingCameraId = id;

        if (mCameraDevice == null) {
            // No camera yet.
            checkAndOpenCamera(mCameraManager, id, mainHandler, this);
        } else if (mCameraProxy.getCameraId() != id) {
            Log.v(TAG, "different camera already opened, closing then reopening");
            // Already has camera opened, and is switching cameras and/or APIs.
            if (useNewApi) {
                mCameraAgentNg.closeCamera(mCameraProxy, true);
            } else {
                // if using API2 ensure API1 usage is also synced
//                mCameraAgent.closeCamera(mCameraProxy, syncClose);
            }
            checkAndOpenCamera(mCameraManager, id, mainHandler, this);
        } else {
            // The same camera, just do a reconnect.
            Log.v(TAG, "reconnecting to use the existing camera");
            mCameraProxy.reconnect(mCallbackHandler, this);
            mCameraProxy = null;
        }

    }

    private void checkAndOpenCamera(CameraManager cameraManager,
                                           final int cameraId, Handler handler, final CameraDevice.StateCallback cb) {
        Log.v(TAG, "checkAndOpenCamera");
        try {
            cameraManager.openCamera(cameraId, cb, handler);
        } catch (Exception ex) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onCameraDisabled(cameraId);
                }
            });
        }
    }

    private void onCameraDisabled(int cameraId) {

    }

    @Override
    public void onOpened(@NonNull CameraDevice camera) {
        mCameraDevice = camera;
        if (mReceiver != null) {
            mReceiver.onCameraOpened();
        }
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice camera) {

    }

    @Override
    public void onError(@NonNull CameraDevice camera, int error) {

    }

    public void startPreView() {

    }

    public void setPreviewDisplay(TextureView textureView) {
        mPreviewTexture = textureView.getSurfaceTexture();
        mPreviewTexture.setDefaultBufferSize(mPreviewSize.width(), mPreviewSize.height());
    }

    public AndroidCamera2Settings getmCameraSetting() {
        return mCameraSetting;
    }
}
