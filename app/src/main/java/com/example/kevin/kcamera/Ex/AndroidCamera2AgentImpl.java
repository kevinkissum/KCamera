package com.example.kevin.kcamera.Ex;

import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.util.ArrayList;

public class AndroidCamera2AgentImpl extends CameraAgent {


    private final HandlerThread mCameraHandlerThread;
    private final Camera2Handler mCameraHandler;
    private final CameraManager mCameraManager;
    private final int mNumCameraDevices;
    private final ArrayList<String> mCameraDevices;
    private DispatchThread mDispatchThread;

    public AndroidCamera2AgentImpl(Context context) {
        mCameraHandlerThread = new HandlerThread("Camera2 Handler Thread");
        mCameraHandlerThread.start();
        mCameraHandler = new Camera2Handler(mCameraHandlerThread.getLooper());
//        mExceptionHandler = new CameraExceptionHandler(mCameraHandler);
//        mCameraState = new AndroidCamera2StateHolder();
        mDispatchThread = new DispatchThread(mCameraHandler, mCameraHandlerThread);
        mDispatchThread.start();
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        // SPRD
//        mNoisemaker = new MediaActionSound();
//        mNoisemaker.load(MediaActionSound.SHUTTER_CLICK);

        mNumCameraDevices = 0;
        mCameraDevices = new ArrayList<String>();
//        updateCameraDevices();
    }

    @Override
    public DispatchThread getDispatchThread() {
        return mDispatchThread;
    }

    @Override
    protected Handler getCameraHandler() {
        return mCameraHandler;
    }

    @Override
    protected CameraExceptionHandler getCameraExceptionHandler() {
        return null;
    }

    protected class AndroidCamera2ProxyImpl extends CameraAgent.CameraProxy {

        @Override
        public int getCameraId() {
            return 0;
        }

        @Override
        public CameraAgent getAgent() {
            return null;
        }

        @Override
        public DispatchThread getDispatchThread() {
            return null;
        }

        @Override
        public Handler getCameraHandler() {
            return null;
        }
    }


        protected class Camera2Handler extends HistoryHandler {

        Camera2Handler(Looper looper) {
            super(looper);
        }
    }

}
