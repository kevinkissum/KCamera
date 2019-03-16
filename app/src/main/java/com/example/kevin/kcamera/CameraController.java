package com.example.kevin.kcamera;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.example.kevin.kcamera.Interface.ICameraControll;
import com.example.kevin.kcamera.CameraStates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraController extends CameraDevice.StateCallback {

    public static final String TAG = "CameraController";

    private final Handler mainHandler;
    private int mRequestingCameraId = -1;
    private CameraManager mCameraManager;

    private ICameraControll mCameraControl;
    private CameraDevice mCameraDevice;
    private SurfaceTexture mPreviewTexture;
    private AndroidCamera2Settings mCameraSetting;
    private ImageReader mImageReader;
    private CameraActivity mActivity;

    private Integer mSensorOrientation;
    private Size mPreviewSize;
    private boolean mFlashSupported;
    private SurfaceTexture mSurfaceTexture;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest mPreviewRequest;
    private int mState = CameraStates.STATE_PREVIEW;

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
//            mainHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
            Log.v(TAG, "OnImageAvailableListener");
        }

    };

    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {
        private void process(CaptureResult result) {
            switch (mState) {
                case CameraStates.STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
//                    Log.v(TAG, "CaptureCallback STATE_PREVIEW ");
                    break;
                }
                case CameraStates.STATE_WAITING_LOCK: {
                    Log.v(TAG, "CaptureCallback STATE_WAITING_LOCK ");
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
//                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = CameraStates.STATE_PICTURE_TAKEN;
//                            captureStillPicture();
                        } else {
//                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case CameraStates.STATE_WAITING_PRECAPTURE: {
                    Log.v(TAG, "CaptureCallback STATE_WAITING_PRECAPTURE ");
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = CameraStates.STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case CameraStates.STATE_WAITING_NON_PRECAPTURE: {
                    Log.v(TAG, "CaptureCallback STATE_WAITING_NON_PRECAPTURE ");
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = CameraStates.STATE_PICTURE_TAKEN;
//                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    public CameraController(CameraActivity activity, Handler handler, ICameraControll receiver) {
        mActivity = activity;
        mCameraManager = (CameraManager)mActivity.getSystemService(Context.CAMERA_SERVICE);
        mainHandler = handler;
        mCameraControl = receiver;
        mCameraSetting = new AndroidCamera2Settings();
    }


    public void requestCamera(int id, boolean useNewApi, int width, int height) {
        Log.v(TAG, "requestCamera");
        if (mRequestingCameraId == id) {
            return;
        }

        mRequestingCameraId = id;

        if (mCameraDevice == null) {
            // No camera yet.
            setUpCameraOutputs(width, height);
            checkAndOpenCamera(mCameraManager, id, mainHandler, this);
        } /*else if (mCameraProxy.getCameraId() != id) {
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
        }*/
    }

    private void checkAndOpenCamera(CameraManager cameraManager,
                                           final int cameraId, Handler handler, final CameraDevice.StateCallback cb) {
        Log.v(TAG, "checkAndOpenCamera");
        try {
            cameraManager.openCamera(cameraId + "", cb, handler);
        } catch (Exception ex) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onCameraDisabled(cameraId);
                }
            });
        }
    }

    private void setUpCameraOutputs(int width, int height) {
        try {
            CameraCharacteristics characteristics
                    = mCameraManager.getCameraCharacteristics(mRequestingCameraId + "");
            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                return;
            }
            // For still image captures, we use the largest available size.
            Size largest = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                    new CompareSizesByArea());
            //配置流， 即setPhotoSize
            mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                    ImageFormat.JPEG, /*maxImages*/2);
            Log.e(TAG, "配置流， 即setPhotoSize: " + largest.getWidth() +  " * " + largest.getHeight()) ;
            mImageReader.setOnImageAvailableListener(
                    mOnImageAvailableListener, mainHandler);

            int displayRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            boolean swappedDimensions = false;
            switch (displayRotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_180:
                    if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                        swappedDimensions = true;
                    }
                    break;
                case Surface.ROTATION_90:
                case Surface.ROTATION_270:
                    if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                        swappedDimensions = true;
                    }
                    break;
                default:
                    Log.e(TAG, "Display rotation is invalid: " + displayRotation);
            }
            Point displaySize = new Point();
            mActivity.getWindowManager().getDefaultDisplay().getSize(displaySize);
            int rotatedPreviewWidth = width;
            int rotatedPreviewHeight = height;
            int maxPreviewWidth = displaySize.x;
            int maxPreviewHeight = displaySize.y;

            if (swappedDimensions) {
                rotatedPreviewWidth = height;
                rotatedPreviewHeight = width;
                maxPreviewWidth = displaySize.y;
                maxPreviewHeight = displaySize.x;
            }

            if (maxPreviewWidth > CameraStates.MAX_PREVIEW_WIDTH) {
                maxPreviewWidth = CameraStates.MAX_PREVIEW_WIDTH;
            }

            if (maxPreviewHeight > CameraStates.MAX_PREVIEW_HEIGHT) {
                maxPreviewHeight = CameraStates.MAX_PREVIEW_HEIGHT;
            }
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                    maxPreviewHeight, largest);
            int orientation = mActivity.getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mCameraControl.changePreViewSize(
                        mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mCameraControl.changePreViewSize(
                        mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
            Log.e(TAG, "   changePreViewSize: " + mPreviewSize.getWidth() +  " * " + mPreviewSize.getHeight()) ;
            // Check if the flash is supported.
            Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            mFlashSupported = available == null ? false : available;

        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "  setUpCameraOutputs error " + e);
        }


    }

    private Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                     int textureViewHeight, int maxWidth,
                                     int maxHeight, Size aspectRatio) {
        Log.e(TAG, "   changePreViewSize: " + textureViewWidth +  " * " + textureViewHeight +  "   " + maxWidth +  " * " + maxHeight) ;

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            Log.e(TAG, "   chooseOptimalSize: " + option.getWidth() +  " * " + option.getHeight()) ;
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mSurfaceTexture;
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
//                                setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mainHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            Util.showToast(mActivity,"onConfigureFailed Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void onCameraDisabled(int cameraId) {

    }

    @Override
    public void onOpened(@NonNull CameraDevice camera) {
        mCameraDevice = camera;
        if (mCameraControl != null) {
            mCameraControl.onCameraOpened();
        }
        createCameraPreviewSession();
    }


    @Override
    public void onDisconnected(@NonNull CameraDevice camera) {

    }

    @Override
    public void onError(@NonNull CameraDevice camera, int error) {

    }

    public void setSurfaceTexture(SurfaceTexture surface) {
        mSurfaceTexture = surface;
    }


    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

}
