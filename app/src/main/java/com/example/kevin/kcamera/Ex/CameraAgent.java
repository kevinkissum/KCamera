package com.example.kevin.kcamera.Ex;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraDevice;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public abstract class CameraAgent {

    public final static String TAG = "CAM_CameraAgent";
    public static long CAMERA_OPERATION_TIMEOUT_MS = 3500;

    /**
     * An interface to be called for any events when opening or closing the
     * camera device. This error callback is different from the one defined
     * in the framework, {@link android.hardware.Camera.ErrorCallback}, which
     * is used after the camera is opened.
     */
    public static interface CameraOpenCallback {
        /**
         * Callback when camera open succeeds.
         */
        public void onCameraOpened(CameraProxy camera);

        /**
         * Callback when {@link com.android.camera.CameraDisabledException} is
         * caught.
         *
         * @param cameraId The disabled camera.
         */
        public void onCameraDisabled(int cameraId);

        /**
         * Callback when {@link com.android.camera.CameraHardwareException} is
         * caught.
         *
         * @param cameraId The camera with the hardware failure.
         * @param info The extra info regarding this failure.
         */
        public void onDeviceOpenFailure(int cameraId, String info);

        /**
         * Callback when trying to open the camera which is already opened.
         *
         * @param cameraId The camera which is causing the open error.
         */
        public void onDeviceOpenedAlready(int cameraId, String info);

        /**
         * Callback when {@link java.io.IOException} is caught during
         * {@link android.hardware.Camera#reconnect()}.
         *
         * @param mgr The {@link CameraAgent}
         *            with the reconnect failure.
         */
        public void onReconnectionFailure(CameraAgent mgr, String info);
    }

    /**
     * An interface to be called when the camera preview has started.
     */
    public static interface CameraStartPreviewCallback {
        /**
         * Callback when the preview starts.
         */
        public void onPreviewStarted();
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.AutoFocusCallback}.
     */
    public static interface CameraAFCallback {
        public void onAutoFocus(boolean focused, CameraProxy camera);
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.AutoFocusMoveCallback}.
     */
    public static interface CameraAFMoveCallback {
        public void onAutoFocusMoving(boolean moving, CameraProxy camera);

    }

    /**
     * Opens the camera of the specified ID asynchronously. The camera device
     * will be opened in the camera handler thread and will be returned through
     * the {@link CameraAgent.CameraOpenCallback#
     * onCameraOpened(com.android.camera.cameradevice.CameraAgent.CameraProxy)}.
     *
     * @param handler The {@link android.os.Handler} in which the callback
     *                was handled.
     * @param callback The callback for the result.
     * @param cameraId The camera ID to open.
     */
    public void openCamera(final Handler handler, final int cameraId,
                           final CameraOpenCallback callback) {
        try {
            getDispatchThread().runJob(new Runnable() {
                @Override
                public void run() {
                    getCameraHandler().obtainMessage(CameraActions.OPEN_CAMERA, cameraId, 0,
                            CameraOpenCallbackForward.getNewInstance(handler, callback)).sendToTarget();
                }
            });
        } catch (final RuntimeException ex) {
            getCameraExceptionHandler().onDispatchThreadException(ex);
        }
    }

    public abstract static class CameraProxy {

        public abstract int getCameraId();
        public abstract CameraAgent getAgent();
        public abstract DispatchThread getDispatchThread();
        public abstract CameraSettings getSettings();
        public abstract Handler getCameraHandler();
        public abstract CameraCapabilities getCapabilities();
        public abstract CameraStateHolder getCameraState();
        public abstract boolean applySettings(CameraSettings settings);

        public void setPreviewTexture(final SurfaceTexture surfaceTexture) {
            try {
                getDispatchThread().runJob(new Runnable() {
                    @Override
                    public void run() {
                        getCameraHandler()
                                .obtainMessage(CameraActions.SET_PREVIEW_TEXTURE_ASYNC, surfaceTexture)
                                .sendToTarget();
                    }});
            } catch (final RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void startPreview() {
            try {
                getDispatchThread().runJob(new Runnable() {
                    @Override
                    public void run() {
                        getCameraHandler()
                                .obtainMessage(CameraActions.START_PREVIEW_ASYNC, null).sendToTarget();
                    }});
            } catch (final RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        /**
         * Default implementation of {@link #applySettings(CameraSettings)}
         * that is only missing the set of states it needs to wait for
         * before applying the settings.
         *
         * @param settings The settings to use on the device.
         * @param statesToAwait Bitwise OR of the required camera states.
         * @return Whether the settings can be applied.
         */
        protected boolean applySettingsHelper(CameraSettings settings,
                                              final int statesToAwait) {
            if (settings == null) {
                Log.v(TAG, "null argument in applySettings()");
                return false;
            }
            if (!getCapabilities().supports(settings)) {
                Log.w(TAG, "Unsupported settings in applySettings()");
                return false;
            }

            final CameraSettings copyOfSettings = settings.copy();
            try {
                getDispatchThread().runJob(new Runnable() {
                    @Override
                    public void run() {
                        CameraStateHolder cameraState = getCameraState();
                        // Don't bother to wait since camera is in bad state.
                        /*
                         * SPRD fix bug622519 should not wait when state is unopend @{
                         * Original Code
                         *
                        if (cameraState.isInvalid()) {
                            return;
                        }
                         */
                        if (cameraState.isInvalid() || cameraState.getState() == 1) {
                            return;
                        }
                        /* @} */
                        cameraState.waitForStates(statesToAwait);
                        getCameraHandler().obtainMessage(CameraActions.APPLY_SETTINGS, copyOfSettings)
                                .sendToTarget();
                    }});
            } catch (final RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
            return true;
        }

    }
    /**
     * A callback helps to invoke the original callback on another
     * {@link android.os.Handler}.
     */
    public static class CameraOpenCallbackForward implements CameraOpenCallback {
        private final Handler mHandler;

        private final CameraOpenCallback mCallback;

        /**
         * Returns a new instance of {@link FaceDetectionCallbackForward}.
         *
         * @param handler The handler in which the callback will be invoked in.
         * @param cb The callback to be invoked.
         * @return The instance of the {@link FaceDetectionCallbackForward}, or
         *         null if any parameter is null.
         */
        public static CameraOpenCallbackForward getNewInstance(
                Handler handler, CameraOpenCallback cb) {
            if (handler == null || cb == null) {
                return null;
            }
            return new CameraOpenCallbackForward(handler, cb);
        }

        private CameraOpenCallbackForward(Handler h, CameraOpenCallback cb) {
            // Given that we are using the main thread handler, we can create it
            // here instead of holding onto the PhotoModule objects. In this
            // way, we can avoid memory leak.
            mHandler = new Handler(Looper.getMainLooper());
            mCallback = cb;
        }

        @Override
        public void onCameraOpened(final CameraProxy camera) {
            /*
             * SPRD: Fix bug 607678 onOpened messages result error during processing @{
             * Original Code
             *
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onCameraOpened(camera);
                }});
             */
            Log.i(TAG, " onCameraOpened , CameraProxy = " + camera);

//            mHandler.postAtFrontOfQueue(new Runnable() {
//                @Override
//                public void run() {
//                    // SPRD: if the state changes to CAMERA_UNOPENED before we are here,
//                    // NullException will happen, so make sure the current state is correct.
//                    // About the state defination, see implemention of child class.
//                    if (camera.getCameraState().getState() == 2) {
//                        mCallback.onCameraOpened(camera);
//                    }
//                }
//            });

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onCameraOpened(camera);
                }});
        }

        @Override
        public void onCameraDisabled(final int cameraId) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onCameraDisabled(cameraId);
                }});
        }

        @Override
        public void onDeviceOpenFailure(final int cameraId, final String info) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onDeviceOpenFailure(cameraId, info);
                }});
        }

        @Override
        public void onDeviceOpenedAlready(final int cameraId, final String info) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onDeviceOpenedAlready(cameraId, info);
                }});
        }
        @Override
        public void onReconnectionFailure(final CameraAgent mgr, final String info) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onReconnectionFailure(mgr, info);
                }});
        }

    }

    public abstract DispatchThread getDispatchThread();

    protected abstract Handler getCameraHandler();

    protected abstract CameraExceptionHandler getCameraExceptionHandler();

    public abstract CameraDeviceInfo getCameraDeviceInfo();


    public static class WaitDoneBundle {
        public final Runnable mUnlockRunnable;
        public final Object mWaitLock;

        WaitDoneBundle() {
            mWaitLock = new Object();
            mUnlockRunnable = new Runnable() {
                @Override
                public void run() {
                    synchronized (mWaitLock) {
                        mWaitLock.notifyAll();
                    }
                }};
        }

        /**
         * Notify all synchronous waiters waiting on message completion with {@link #mWaitLock}.
         *
         * <p>This assumes that the message was sent with {@code this} as the {@code Message#obj}.
         * Otherwise the message is ignored.</p>
         */
        /*package*/ static void unblockSyncWaiters(Message msg) {
            if (msg == null) return;

            if (msg.obj instanceof WaitDoneBundle) {
                WaitDoneBundle bundle = (WaitDoneBundle)msg.obj;
                bundle.mUnlockRunnable.run();
            }
        }
    }

}
