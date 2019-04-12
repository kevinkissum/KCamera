package com.example.kevin.kcamera.Ex;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AndroidCamera2AgentImpl extends CameraAgent {

    public static final String TAG = "CAM_AndCam2AgntImp";

    private final HandlerThread mCameraHandlerThread;
    private final Camera2Handler mCameraHandler;
    private final CameraManager mCameraManager;
    private int mNumCameraDevices;
    private final ArrayList<String> mCameraDevices;
    private final CameraExceptionHandler mExceptionHandler;
    private DispatchThread mDispatchThread;
    private CameraStateHolder mCameraState;
    private Camera2RequestSettingsSet mPersistentSettings;
    private boolean mNeedThumb;

    public AndroidCamera2AgentImpl(Context context) {
        mCameraHandlerThread = new HandlerThread("Camera2 Handler Thread");
        mCameraHandlerThread.start();
        mCameraHandler = new Camera2Handler(mCameraHandlerThread.getLooper());
        mExceptionHandler = new CameraExceptionHandler(mCameraHandler);
        mCameraState = new AndroidCamera2StateHolder();
        mDispatchThread = new DispatchThread(mCameraHandler, mCameraHandlerThread);
        mDispatchThread.start();
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        // SPRD
//        mNoisemaker = new MediaActionSound();
//        mNoisemaker.load(MediaActionSound.SHUTTER_CLICK);

        mNumCameraDevices = 0;
        mCameraDevices = new ArrayList<String>();
        updateCameraDevices();
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
        return mExceptionHandler;
    }

    // TODO: Some indices may now be invalid; ensure everyone can handle that and update the docs
    @Override
    public CameraDeviceInfo getCameraDeviceInfo() {
        updateCameraDevices();
        return new AndroidCamera2DeviceInfo(mCameraManager, mCameraDevices.toArray(new String[0]),
                mNumCameraDevices);
    }

    /**
     * Updates the camera device index assignments stored in {@link mCameraDevices}, without
     * reappropriating any currently-assigned index.
     * @return Whether the operation was successful
     */
    private boolean updateCameraDevices() {
        try {
            String[] currentCameraDevices = mCameraManager.getCameraIdList();
            Set<String> currentSet = new HashSet<String>(Arrays.asList(currentCameraDevices));

            // Invalidate the indices assigned to any camera devices that are no longer present
            for (int index = 0; index < mCameraDevices.size(); ++index) {
                if (!currentSet.contains(mCameraDevices.get(index))) {
                    mCameraDevices.set(index, null);
                    --mNumCameraDevices;
                }
            }

            // Assign fresh indices to any new camera devices
            currentSet.removeAll(mCameraDevices); // The devices we didn't know about
            for (String device : currentCameraDevices) {
                if (currentSet.contains(device)) {
                    mCameraDevices.add(device);
                    ++mNumCameraDevices;
                }
            }

            return true;
        } catch (CameraAccessException ex) {
            Log.e(TAG, "Could not get device listing from camera subsystem", ex);
            return false;
        }
    }

    private static class AndroidCamera2DeviceInfo implements CameraDeviceInfo {
        private final CameraManager mCameraManager;
        private final String[] mCameraIds;
        private final int mNumberOfCameras;
        private final int mFirstBackCameraId;
        private final int mFirstFrontCameraId;

        public AndroidCamera2DeviceInfo(CameraManager cameraManager,
                                        String[] cameraIds, int numberOfCameras) {
            mCameraManager = cameraManager;
            mCameraIds = cameraIds;
            mNumberOfCameras = numberOfCameras;

            int firstBackId = NO_DEVICE;
            int firstFrontId = NO_DEVICE;
            for (int id = 0; id < cameraIds.length; ++id) {
                try {
                    int lensDirection = cameraManager.getCameraCharacteristics(cameraIds[id])
                            .get(CameraCharacteristics.LENS_FACING);
                    if (firstBackId == NO_DEVICE &&
                            lensDirection == CameraCharacteristics.LENS_FACING_BACK) {
                        firstBackId = id;
                        // SPRD
//                        super.updateFeatureEnable(cameraManager, cameraIds[firstBackId]);
                    }
                    if (firstFrontId == NO_DEVICE &&
                            lensDirection == CameraCharacteristics.LENS_FACING_FRONT) {
                        firstFrontId = id;
                    }
                } catch (CameraAccessException ex) {
                    Log.w(TAG, "Couldn't get characteristics of camera '" + id + "'", ex);
                }
            }
            mFirstBackCameraId = firstBackId;
            mFirstFrontCameraId = firstFrontId;
        }

        @Override
        public Characteristics getCharacteristics(int cameraId) {
            // SPRD
//            cameraId = super.getActualCameraId(cameraId);

            String actualId = mCameraIds[cameraId];
            try {
                CameraCharacteristics info = mCameraManager.getCameraCharacteristics(actualId);
                return new AndroidCharacteristics2(info);
            } catch (CameraAccessException ex) {
                return null;
            }
        }

        @Override
        public int getNumberOfCameras() {
            return mNumberOfCameras;
        }

        @Override
        public int getFirstBackCameraId() {
            return mFirstBackCameraId;
        }

        @Override
        public int getFirstFrontCameraId() {
            return mFirstFrontCameraId;
        }

        private static class AndroidCharacteristics2 extends Characteristics {
            private CameraCharacteristics mCameraInfo;

            AndroidCharacteristics2(CameraCharacteristics cameraInfo) {
                mCameraInfo = cameraInfo;
            }

            @Override
            public boolean isFacingBack() {
                return mCameraInfo.get(CameraCharacteristics.LENS_FACING)
                        .equals(CameraCharacteristics.LENS_FACING_BACK);
            }

            @Override
            public boolean isFacingFront() {
                return mCameraInfo.get(CameraCharacteristics.LENS_FACING)
                        .equals(CameraCharacteristics.LENS_FACING_FRONT);
            }

            @Override
            public int getSensorOrientation() {
                return mCameraInfo.get(CameraCharacteristics.SENSOR_ORIENTATION);
            }

            @Override
            public Matrix getPreviewTransform(int currentDisplayOrientation,
                                              RectF surfaceDimensions,
                                              RectF desiredBounds) {
                if (!orientationIsValid(currentDisplayOrientation)) {
                    return new Matrix();
                }

                // The system transparently transforms the image to fill the surface
                // when the device is in its natural orientation. We rotate the
                // coordinates of the rectangle's corners to be relative to the
                // original image, instead of to the current screen orientation.
                float[] surfacePolygon = rotate(convertRectToPoly(surfaceDimensions),
                        2 * currentDisplayOrientation / 90);
                float[] desiredPolygon = convertRectToPoly(desiredBounds);

                Matrix transform = new Matrix();
                // Use polygons instead of rectangles so that rotation will be
                // calculated, since that is not done by the new camera API.
                transform.setPolyToPoly(surfacePolygon, 0, desiredPolygon, 0, 4);
                return transform;
            }

            @Override
            public boolean canDisableShutterSound() {
                return true;
            }

            private static float[] convertRectToPoly(RectF rf) {
                return new float[] {rf.left, rf.top, rf.right, rf.top,
                        rf.right, rf.bottom, rf.left, rf.bottom};
            }

            private static float[] rotate(float[] arr, int times) {
                if (times < 0) {
                    times = times % arr.length + arr.length;
                }

                float[] res = new float[arr.length];
                for (int offset = 0; offset < arr.length; ++offset) {
                    res[offset] = arr[(times + offset) % arr.length];
                }
                return res;
            }
        }
    }

    protected class AndroidCamera2ProxyImpl extends CameraAgent.CameraProxy {
        private final AndroidCamera2AgentImpl mCameraAgent;
        private final int mCameraIndex;
        private final CameraDevice mCamera;
        private final CameraDeviceInfo.Characteristics mCharacteristics;
        private final AndroidCamera2Capabilities mCapabilities;
        protected CameraSettings mLastSettings;
        private boolean mShutterSoundEnabled;
        protected Runnable takePictureRunnable = null;

        public AndroidCamera2ProxyImpl(AndroidCamera2AgentImpl agent, int cameraIndex, CameraDevice camera, CameraDeviceInfo.Characteristics characteristics, CameraCharacteristics properties) {
            mCameraAgent = agent;
            mCameraIndex = cameraIndex;
            mCamera = camera;
            mCharacteristics = characteristics;
            mCapabilities = new AndroidCamera2Capabilities(properties);
            mLastSettings = null;
            mShutterSoundEnabled = true;
        }

        @Override
        public int getCameraId() {
            return 0;
        }

        @Override
        public CameraAgent getAgent() {
            return mCameraAgent;
        }

        @Override
        public DispatchThread getDispatchThread() {
            return AndroidCamera2AgentImpl.this.getDispatchThread();
        }

        @Override
        public CameraSettings getSettings() {
            if (mLastSettings == null) {
                mLastSettings = mCameraHandler.buildSettings(mCapabilities);
            }
            return mLastSettings;        }

        @Override
        public Handler getCameraHandler() {
            return AndroidCamera2AgentImpl.this.getCameraHandler();
        }

        @Override
        public CameraCapabilities getCapabilities() {
            return mCapabilities;
        }

        @Override
        public CameraStateHolder getCameraState() {
            return mCameraState;
        }

        @Override
        public boolean applySettings(CameraSettings settings) {
            if (settings == null) {
                Log.w(TAG, "null parameters in applySettings()");
                return false;
            }
            if (!(settings instanceof AndroidCamera2Settings)) {
                Log.e(TAG, "Provided settings not compatible with the backing framework API");
                return false;
            }

            // Wait for any state that isn't OPENED
            if (applySettingsHelper(settings, ~AndroidCamera2StateHolder.CAMERA_UNOPENED)) {
                mLastSettings = settings;
                return true;
            }
            return false;
        }
    }

    protected static abstract class CaptureAvailableListener
            extends CameraCaptureSession.CaptureCallback
            implements ImageReader.OnImageAvailableListener {};

    private abstract class CameraResultStateCallback
            extends CameraCaptureSession.CaptureCallback {
        public abstract void monitorControlStates(CaptureResult result);

        public abstract void resetState();
    }

    protected class Camera2Handler extends HistoryHandler {
        // Caller-provided when leaving CAMERA_UNOPENED state:
        private CameraOpenCallback mOpenCallback;
        private int mCameraIndex;
        private String mCameraId;
        private int mCancelAfPending = 0;

        // Available in CAMERA_UNCONFIGURED state and above:
        protected CameraDevice mCamera;
        protected AndroidCamera2ProxyImpl mCameraProxy;
        // SPRD
//        private Camera2RequestSettingsSet mPersistentSettings;
        protected Rect mActiveArray;
        private boolean mLegacyDevice;

        // Available in CAMERA_CONFIGURED state and above:
        protected Size mPreviewSize;
        protected Size mPhotoSize;
        protected Size mThumbnailSize;

        // Available in PREVIEW_READY state and above:
        protected SurfaceTexture mPreviewTexture;
        // SPRD
        protected SurfaceHolder mSurfaceHolder;
        protected Surface mPreviewSurface;
        protected CameraCaptureSession mSession;
        protected ImageReader mCaptureReader;
        protected ImageReader mThumbnailReader;

        // Available from the beginning of PREVIEW_ACTIVE until the first preview frame arrives:
        private CameraStartPreviewCallback mOneshotPreviewingCallback;

        // Available in FOCUS_LOCKED between AF trigger receipt and whenever the lens stops moving:
        private CameraAFCallback mOneshotAfCallback;

        // Available when taking picture between AE trigger receipt and autoexposure convergence
        private CaptureAvailableListener mOneshotCaptureCallback;

        // Available whenever setAutoFocusMoveCallback() was last invoked with a non-null argument:
        private CameraAFMoveCallback mPassiveAfCallback;

        // Gets reset on every state change
        private int mCurrentAeState = CaptureResult.CONTROL_AE_STATE_INACTIVE;

        Camera2Handler(Looper looper) {
            super(looper);
        }

        public CameraSettings buildSettings(AndroidCamera2Capabilities caps) {
            try {
                return new AndroidCamera2Settings(mCamera, CameraDevice.TEMPLATE_PREVIEW,
                        mActiveArray, mPreviewSize, mPhotoSize);
            } catch (CameraAccessException ex) {
                Log.e(TAG, "Unable to query camera device to build settings representation");
                return null;
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.i(TAG, "AppFW handleMessage - action = '" + CameraActions.stringify(msg.what) + "'");
            int cameraAction = msg.what;
            try {
                switch (cameraAction) {
                    case CameraActions.OPEN_CAMERA:
                    case CameraActions.RECONNECT: {
                        CameraOpenCallback openCallback = (CameraOpenCallback) msg.obj;
                        int cameraIndex = msg.arg1;

                        if (mCameraState.getState() > AndroidCamera2StateHolder.CAMERA_UNOPENED) {
                            openCallback.onDeviceOpenedAlready(cameraIndex,
                                    generateHistoryString(cameraIndex));
                            break;
                        }

                        mOpenCallback = openCallback;
                        mCameraIndex = cameraIndex;

                        /*
                         * SPRD: Fix bug 591216 that add new feature 3d range finding, only support API2 currently @{
                         * Original Code
                         *
                        mCameraId = mCameraDevices.get(mCameraIndex);
                         */
                        if (mCameraIndex == 5 || mCameraIndex == 6 || mCameraIndex == 7 || mCameraIndex == 12
                                || mCameraIndex == 13 || mCameraIndex == 15) {
                            mCameraId = "" + mCameraIndex;
                        } else {
                            mCameraId = mCameraDevices.get(mCameraIndex);
                        }
                        /* @} */

                        Log.i(TAG, String.format("Opening camera index %d (id %s) with camera2 API",
                                cameraIndex, mCameraId));

                        if (mCameraId == null) {
                            mOpenCallback.onCameraDisabled(msg.arg1);
                            break;
                        }
                        mCameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, this);

                        break;
                    }

                    case CameraActions.SET_PREVIEW_TEXTURE_ASYNC: {
                        setPreviewTexture((SurfaceTexture) msg.obj);
                        break;
                    }

                    case CameraActions.APPLY_SETTINGS: {
                        AndroidCamera2Settings settings = (AndroidCamera2Settings) msg.obj;
                        applyToRequest(settings);
                        break;
                    }

                    case CameraActions.START_PREVIEW_ASYNC: {
                        if (mCameraState.getState() !=
                                AndroidCamera2StateHolder.CAMERA_PREVIEW_READY) {
                            // TODO: Provide better feedback here?
                            Log.w(TAG, "Refusing to start preview at inappropriate time");
                            break;
                        }

                        mOneshotPreviewingCallback = (CameraStartPreviewCallback) msg.obj;
                        changeState(AndroidCamera2StateHolder.CAMERA_PREVIEW_ACTIVE);
                        try {
                            Log.w("kk", ">>>>>>>>>>. start preview");
                            mSession.setRepeatingRequest(
                                    mPersistentSettings.createRequest(mCamera,
                                            CameraDevice.TEMPLATE_PREVIEW, mPreviewSurface),
                                    /*listener*/mCameraResultStateCallback, /*handler*/this);
                        } catch(CameraAccessException ex) {
                            Log.w(TAG, "Unable to start preview", ex);
                            changeState(AndroidCamera2StateHolder.CAMERA_PREVIEW_READY);
                        }
                        break;
                    }
                }
            } catch (final Exception ex) {
                // SPRD
                Log.w(TAG, "catch exception " + ex.getMessage());

                if (cameraAction != CameraActions.RELEASE && mCamera != null) {
                    // TODO: Handle this better
                    mCamera.close();
                    mCamera = null;
                } else if (mCamera == null) {
                    if (cameraAction == CameraActions.OPEN_CAMERA) {
                        if (mOpenCallback != null) {
                            mOpenCallback.onDeviceOpenFailure(mCameraIndex,
                                    generateHistoryString(mCameraIndex));
                        }
                    } else {
                        Log.w(TAG, "Cannot handle message " + msg.what + ", mCamera is null");
                    }
                    return;
                }

                if (ex instanceof RuntimeException) {
                    String commandHistory = generateHistoryString(Integer.parseInt(mCameraId));
                    mExceptionHandler.onCameraException((RuntimeException) ex, commandHistory,
                            cameraAction, mCameraState.getState());
                }
            } finally {
                WaitDoneBundle.unblockSyncWaiters(msg);
            }
        }

        // This callback monitors our connection to and disconnection from camera devices.
        private CameraDevice.StateCallback mCameraDeviceStateCallback =
                new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(CameraDevice camera) {
                        // SPRD
                        Log.i(TAG,"onOpened CameraDevice will camera=" + camera);
                        if (mCameraId == null) {
                            return;
                        }

                        mCamera = camera;
                        if (mOpenCallback != null) {
                            try {
                                //mCameraId 和 mCameraIndex 仍是一一对应的关系
                                CameraCharacteristics props =
                                        mCameraManager.getCameraCharacteristics(mCameraId);
                                CameraDeviceInfo.Characteristics characteristics =
                                        getCameraDeviceInfo().getCharacteristics(mCameraIndex);

                        /*
                         * SPRD @{
                         * Original Code
                         *
                        mCameraProxy = new AndroidCamera2ProxyImpl(AndroidCamera2AgentImpl.this,
                                mCameraIndex, mCamera, characteristics, props);
                         */
//                                if (mSprdAgentImpl == null) {
                                    mCameraProxy = new AndroidCamera2ProxyImpl(AndroidCamera2AgentImpl.this,
                                            mCameraIndex, mCamera, characteristics, props);
//                                } else {
//                                    mCameraProxy = mSprdAgentImpl.new SprdAndroidCamera2ProxyImpl(
//                                            mSprdAgentImpl, mCameraIndex, mCamera, characteristics, props);
//                                }
                                /* @} */

                                mPersistentSettings = new Camera2RequestSettingsSet();
                                mActiveArray =
                                        props.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                                mLegacyDevice =
                                        props.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) ==
                                                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
                                //camera open之后的状态
                                changeState(AndroidCamera2StateHolder.CAMERA_UNCONFIGURED);
                                //该callback是CameraAgent中的CameraOpenCallbackForward，它会再进行callback回到cameraControl中
                                mOpenCallback.onCameraOpened(mCameraProxy);
                            } catch (CameraAccessException ex) {
                                mOpenCallback.onDeviceOpenFailure(mCameraIndex,
                                        generateHistoryString(mCameraIndex));
                            }
                        }

                        // SPRD
                        Log.i(TAG,"onOpened CameraDevice end mOpenCallback=" + mOpenCallback);
                    }

                    @Override
                    public void onDisconnected(CameraDevice camera) {
                        Log.w(TAG, "Camera device '" + mCameraIndex + "' was disconnected");
                    }

                    @Override
                    public void onError(CameraDevice camera, int error) {
                        Log.e(TAG, "Camera device '" + mCameraIndex + "' encountered error code '" +
                                error + '\'');
                        if (mOpenCallback != null) {
                            mOpenCallback.onDeviceOpenFailure(mCameraIndex,
                                    generateHistoryString(mCameraIndex));
                        }
                    }
        };


        // This callback monitors requested captures and notifies any relevant callbacks.
        private CameraResultStateCallback mCameraResultStateCallback =
                new CameraResultStateCallback() {
                    private int mLastAfState = -1;
                    private long mLastAfFrameNumber = -1;
                    private long mLastAeFrameNumber = -1;

                    @Override
                    public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                                    CaptureResult result) {
                        monitorControlStates(result);
                    }

                    @Override
                    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                                   TotalCaptureResult result) {
                        monitorControlStates(result);
                    }

                    @Override
                    public void monitorControlStates(CaptureResult result) {
                        Integer afStateMaybe = result.get(CaptureResult.CONTROL_AF_STATE);
                        if (afStateMaybe != null) {
                            int afState = afStateMaybe;
                            // Since we handle both partial and total results for multiple frames here, we
                            // might get the final callbacks for an earlier frame after receiving one or
                            // more that correspond to the next one. To prevent our data from oscillating,
                            // we never consider AF states that are older than the last one we've seen.
                            if (result.getFrameNumber() > mLastAfFrameNumber) {
                                boolean afStateChanged = afState != mLastAfState;
                                mLastAfState = afState;
                                mLastAfFrameNumber = result.getFrameNumber();

                                switch (afState) {
                                    case CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN:
                                    case CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED:
                                    case CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED: {
                                        if (afStateChanged && mPassiveAfCallback != null) {
                                            // A CameraAFMoveCallback is attached. If we just started to
                                            // scan, the motor is moving; otherwise, it has settled.
                                            mPassiveAfCallback.onAutoFocusMoving(
                                                    afState == CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN,
                                                    mCameraProxy);
                                        }
                                        break;
                                    }

                                    case CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED:
                                    case CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED: {
                                        // This check must be made regardless of whether the focus state has
                                        // changed recently to avoid infinite waiting during autoFocus()
                                        // when the algorithm has already either converged or failed to.
                                        if (mOneshotAfCallback != null) {
                                            // A call to autoFocus() was just made to request a focus lock.
                                            // Notify the caller that the lens is now indefinitely fixed,
                                            // and report whether the image we're stuck with is in focus.
                                            mOneshotAfCallback.onAutoFocus(
                                                    afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                                                    mCameraProxy);
                                            mOneshotAfCallback = null;
                                        }
                                        break;
                                    }
                                }
                            }

                            // SPRD
//                            onMonitorControlStates(result);
                        }

                        Integer aeStateMaybe = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeStateMaybe != null) {
                            int aeState = aeStateMaybe;
                            // Since we handle both partial and total results for multiple frames here, we
                            // might get the final callbacks for an earlier frame after receiving one or
                            // more that correspond to the next one. To prevent our data from oscillating,
                            // we never consider AE states that are older than the last one we've seen.
                            if (result.getFrameNumber() > mLastAeFrameNumber) {
                                mCurrentAeState = aeStateMaybe;
                                mLastAeFrameNumber = result.getFrameNumber();

                                switch (aeState) {
                                    case CaptureResult.CONTROL_AE_STATE_CONVERGED:
                                    case CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED:
                                    case CaptureResult.CONTROL_AE_STATE_LOCKED: {
                                        // This check must be made regardless of whether the exposure state
                                        // has changed recently to avoid infinite waiting during
                                        // takePicture() when the algorithm has already converged.
                                        if (mOneshotCaptureCallback != null) {
                                            // A call to takePicture() was just made, and autoexposure
                                            // converged so it's time to initiate the capture!
                                            mCaptureReader.setOnImageAvailableListener(
                                                    /*listener*/mOneshotCaptureCallback,
                                                    /*handler*/Camera2Handler.this);
                                            try {
                                                mSession.capture(
                                                        mPersistentSettings.createRequest(mCamera,
                                                                CameraDevice.TEMPLATE_STILL_CAPTURE,
                                                                mCaptureReader.getSurface()),
                                                        /*callback*/mOneshotCaptureCallback,
                                                        /*handler*/Camera2Handler.this);
                                            } catch (CameraAccessException ex) {
                                                Log.e(TAG, "Unable to initiate capture", ex);
                                            } finally {
                                                mOneshotCaptureCallback = null;
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void resetState() {
                        mLastAfState = -1;
                        mLastAfFrameNumber = -1;
                        mLastAeFrameNumber = -1;
                    }

                    @Override
                    public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request,
                                                CaptureFailure failure) {
                        Log.e(TAG, "Capture attempt failed with reason " + failure.getReason());
                    }};

        // This callback monitors our camera session (i.e. our transition into and out of preview).
        protected CameraCaptureSession.StateCallback mCameraPreviewStateCallback =
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(CameraCaptureSession session) {
                        mSession = session;
                        changeState(AndroidCamera2StateHolder.CAMERA_PREVIEW_READY);
                    }

                    @Override
                    public void onConfigureFailed(CameraCaptureSession session) {
                        // TODO: Invoke a callback
                        Log.e(TAG, "Failed to configure the camera for capture");
                    }

                    @Override
                    public void onActive(CameraCaptureSession session) {
                        if (mOneshotPreviewingCallback != null) {
                            // The session is up and processing preview requests. Inform the caller.
                            mOneshotPreviewingCallback.onPreviewStarted();
                            mOneshotPreviewingCallback = null;
                        }
                    }};

        protected void changeState(int newState) {
            if (mCameraState.getState() != newState) {
                mCameraState.setState(newState);
                if (newState < AndroidCamera2StateHolder.CAMERA_PREVIEW_ACTIVE) {
                    mCurrentAeState = CaptureResult.CONTROL_AE_STATE_INACTIVE;
                    mCameraResultStateCallback.resetState();
                }
            }
        }

        /**
         * Simply propagates settings from provided {@link CameraSettings}
         * object to our {@link CaptureRequest.Builder} for use in captures.
         * <p>Most conversions to match the API 2 formats are performed by
         * {@link AndroidCamera2Capabilities.IntegralStringifier}; otherwise
         * any final adjustments are done here before updating the builder.</p>
         *
         * @param settings The new/updated settings
         */
        private void applyToRequest(AndroidCamera2Settings settings) {
            // TODO: If invoked when in PREVIEW_READY state, a new preview size will not take effect

            mPersistentSettings.union(settings.getRequestSettings());
            mPreviewSize = settings.getCurrentPreviewSize();
            mPhotoSize = settings.getCurrentPhotoSize();
            mThumbnailSize = settings.getExifThumbnailSize();
//            mNeedThumb = settings.getNeedThumbCallBack();

            if (mCameraState.getState() >= AndroidCamera2StateHolder.CAMERA_PREVIEW_ACTIVE) {
                // If we're already previewing, reflect most settings immediately
                try {
                    mSession.setRepeatingRequest(
                            mPersistentSettings.createRequest(mCamera,
                                    CameraDevice.TEMPLATE_PREVIEW, mPreviewSurface),
                            /*listener*/mCameraResultStateCallback, /*handler*/this);
                } catch (CameraAccessException ex) {
                    Log.e(TAG, "Failed to apply updated request settings", ex);
                }
            } else if (mCameraState.getState() < AndroidCamera2StateHolder.CAMERA_PREVIEW_READY) {
                // If we're already ready to preview, this doesn't regress our state
                changeState(AndroidCamera2StateHolder.CAMERA_CONFIGURED);
            }
        }

        private void setPreviewTexture(SurfaceTexture surfaceTexture) {
            // SPRD
            long start = System.currentTimeMillis();

            // TODO: Must be called after providing a .*Settings populated with sizes
            // TODO: We don't technically offer a selection of sizes tailored to SurfaceTextures!

            // TODO: Handle this error condition with a callback or exception
            if (mCameraState.getState() < AndroidCamera2StateHolder.CAMERA_CONFIGURED) {
                Log.w(TAG, "Ignoring texture setting at inappropriate time " + mCameraState.getState());
//                return;
            }

            // Avoid initializing another capture session unless we absolutely have to
            if (surfaceTexture == mPreviewTexture) {
                Log.i(TAG, "Optimizing out redundant preview texture setting");
                return;
            }

            if (mSession != null) {
                closePreviewSession();
            }

            mPreviewTexture = surfaceTexture;
            surfaceTexture.setDefaultBufferSize(mPreviewSize.width(), mPreviewSize.height());

            if (mPreviewSurface != null) {
                mPreviewSurface.release();
            }
            mPreviewSurface = new Surface(surfaceTexture);

            if (mCaptureReader != null) {
                mCaptureReader.close();
            }
            mCaptureReader = ImageReader.newInstance(
                    mPhotoSize.width(), mPhotoSize.height(), ImageFormat.JPEG, 1);
            if (mThumbnailReader != null) {
                mThumbnailReader.close();
            }
            try {
                if (mNeedThumb) {
                    mThumbnailReader = ImageReader.newInstance(
                            mThumbnailSize.width(), mThumbnailSize.height(), ImageFormat.YUV_420_888, 1);
                    mCamera.createCaptureSession(
                            Arrays.asList(mPreviewSurface, mCaptureReader.getSurface(), mThumbnailReader.getSurface()),
                            mCameraPreviewStateCallback, this);
                } else {
                    mCamera.createCaptureSession(
                            Arrays.asList(mPreviewSurface, mCaptureReader.getSurface()),
                            mCameraPreviewStateCallback, this);
                }
            } catch (CameraAccessException ex) {
                Log.e(TAG, "Failed to create camera capture session", ex);
            }

            // SPRD
            long end = System.currentTimeMillis();
            Log.i(TAG, "setPreviewTexture cost " + (end - start));
        }

        protected void closePreviewSession() {
            try {
                mSession.abortCaptures();
                mSession = null;
            } catch (CameraAccessException ex) {
                Log.e(TAG, "Failed to close existing camera capture session", ex);
            }
            changeState(AndroidCamera2StateHolder.CAMERA_CONFIGURED);
        }
    }

    /**
     * A linear state machine: each state entails all the states below it.
     */
    protected static class AndroidCamera2StateHolder extends CameraStateHolder {
        // Usage flow: openCamera() -> applySettings() -> setPreviewTexture() -> startPreview() ->
        //             autoFocus() -> takePicture()
        // States are mutually exclusive, but must be separate bits so that they can be used with
        // the StateHolder#waitForStates() and StateHolder#waitToAvoidStates() methods.
        // Do not set the state to be a combination of these values!
        /* Camera states */
        /**
         * No camera device is opened.
         */
        public static final int CAMERA_UNOPENED = 1 << 0;
        /**
         * A camera is opened, but no settings have been provided.
         */
        public static final int CAMERA_UNCONFIGURED = 1 << 1;
        /**
         * The open camera has been configured by providing it with settings.
         */
        public static final int CAMERA_CONFIGURED = 1 << 2;
        /**
         * A capture session is ready to stream a preview, but still has no repeating request.
         */
        public static final int CAMERA_PREVIEW_READY = 1 << 3;
        /**
         * A preview is currently being streamed.
         */
        public static final int CAMERA_PREVIEW_ACTIVE = 1 << 4;
        /**
         * The lens is locked on a particular region.
         */
        public static final int CAMERA_FOCUS_LOCKED = 1 << 5;

        public AndroidCamera2StateHolder() {
            this(CAMERA_UNOPENED);
        }

        public AndroidCamera2StateHolder(int state) {
            super(state);
        }
    }

}
