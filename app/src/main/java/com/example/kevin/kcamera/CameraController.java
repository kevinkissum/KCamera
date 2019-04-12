package com.example.kevin.kcamera;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.example.kevin.kcamera.Ex.AndroidCamera2Settings;
import com.example.kevin.kcamera.Ex.CameraAgent;
import com.example.kevin.kcamera.Ex.CameraDeviceInfo;
import com.example.kevin.kcamera.Ex.CameraExceptionHandler;
import com.example.kevin.kcamera.Interface.CameraProvider;

import java.util.Comparator;

import static com.example.kevin.kcamera.CameraStates.STATE_PREVIEW;
import static com.example.kevin.kcamera.CameraStates.STATE_WAITING_LOCK;
import static com.example.kevin.kcamera.CameraStates.STATE_WAITING_PRECAPTURE;

public class CameraController implements CameraAgent.CameraOpenCallback , CameraProvider {

    public static final String TAG = "CameraController";

    private static final int EMPTY_REQUEST = -1;

    private final Handler mainHandler;
    private int mRequestingCameraId = -1;
    private CameraManager mCameraManager;
    private CameraAgent mCameraAgent;

    private CameraAgent.CameraOpenCallback mCallbackReceiver;
    private CameraAgent.CameraProxy mCameraProxy;
    private SurfaceTexture mPreviewTexture;
    private AndroidCamera2Settings mCameraSetting;
    private ImageReader mImageReader;
    private CameraActivity mActivity;

    private Integer mSensorOrientation;
    private Size mPreviewSize;
    private Size mPhotoSize;
    private boolean mFlashSupported;
    private SurfaceTexture mSurfaceTexture;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest mPreviewRequest;
    private int mState = STATE_PREVIEW;
    private int mSurfaceWidth, mSurfaceHeight;

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
//            mainHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
            Log.d(TAG, "OnImageAvailableListener");
        }

    };

    //
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {
        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
//                    Log.d(TAG, "CaptureCallback STATE_PREVIEW ");
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    Log.d(TAG, "CaptureCallback STATE_WAITING_LOCK " + afState);
                    if (afState == null) {
                        captureStillPicture();
                        // CONTROL_AF_STATE_FOCUSED_LOCKED AF 已经focused并且locked
                        // CONTROL_AF_STATE_NOT_FOCUSED_LOCKED AF 失败但是也locked
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        Log.d(TAG, "CaptureCallback CONTROL_AE_STATE " + aeState);
                        if (aeState == null ||
                                // AE has a good set of control values for current scene.
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = CameraStates.STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                // 拍照前等待曝光完成都一种状态
                case STATE_WAITING_PRECAPTURE: {
                    Log.d(TAG, "CaptureCallback STATE_WAITING_PRECAPTURE ");
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
                    Log.d(TAG, "CaptureCallback STATE_WAITING_NON_PRECAPTURE ");
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = CameraStates.STATE_PICTURE_TAKEN;
                        captureStillPicture();
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

    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void captureStillPicture() {
        try {
            final Activity activity = mActivity;
            if (null == activity || null == mCameraProxy) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder = null
                    /*mCameraProxy.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)*/;
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
//            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
//                    CameraUtil.showToast(mActivity, "Saved: " + mFile);
                    Log.d(TAG, "Save pic ".toString());
                    unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mainHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public CameraController(CameraActivity activity, CameraAgent cameraAgent, Handler handler, CameraAgent.CameraOpenCallback receiver) {
        mActivity = activity;
        mCameraManager = (CameraManager)mActivity.getSystemService(Context.CAMERA_SERVICE);
        mainHandler = handler;
        mCallbackReceiver = receiver;
        mCameraAgent = cameraAgent;
    }

    @Override
    public void requestCamera(int id) {

    }

    public void requestCamera(int id, boolean useNewApi) {
        Log.d(TAG, "requestCamera " + id );
        if (mRequestingCameraId == id) {
            return;
        }
        mRequestingCameraId = id;
        if (mCameraProxy == null) {
            checkAndOpenCamera(mCameraAgent, id, mainHandler, this);
        } /*else if (mCameraProxy.getCameraId() != id) {
            Log.d(TAG, "different camera already opened, closing then reopening");
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
            Log.d(TAG, "reconnecting to use the existing camera");
            mCameraProxy.reconnect(mCallbackHandler, this);
            mCameraProxy = null;
        }*/
    }

    @Override
    public boolean waitingForCamera() {
        return false;
    }

    @Override
    public void releaseCamera(int id) {

    }

    @Override
    public void setCameraExceptionHandler(CameraExceptionHandler exceptionHandler) {

    }

    @Override
    public CameraDeviceInfo.Characteristics getCharacteristics(int cameraId) {
        return null;
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

    private void checkAndOpenCamera(CameraAgent cameraAgent,
                                    final int cameraId, Handler handler, final CameraAgent.CameraOpenCallback cb) {
        Log.d(TAG, "checkAndOpenCamera");
        try {
            cameraAgent.openCamera(handler, cameraId, cb);
        } catch (Exception ex) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onCameraDisabled(cameraId);
                }
            });
        }
    }

    private void setUpCameraOutputs() {
        try {
            CameraCharacteristics characteristics
                    = mCameraManager.getCameraCharacteristics(mRequestingCameraId + "");

            //配置流， 即setPhotoSize
            mImageReader = ImageReader.newInstance(mPhotoSize.getWidth(), mPhotoSize.getHeight(),
                    ImageFormat.JPEG, /*maxImages*/2);
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

            // Check if the flash is supported.
            Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            mFlashSupported = available == null ? false : available;

        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "  setUpCameraOutputs error " + e);
        }


    }


    private void createCameraPreviewSession() {
        try {
            // SurfaceTexture用来捕获视频流中的图像帧的，视频流可以是相机预览或者视频解码数据。
            // SurfaceTexture可以作为android.hardware.camera2, MediaCodec, MediaPlayer, 和 Allocation这些类的目标视频数据输出对象。
            // 可以调用updateTexImage()方法从视频流数据中更新当前帧。
            SurfaceTexture texture = mSurfaceTexture;
            // assert condition;
            // 这里condition是一个必须为真(true)的表达式。如果表达式的结果为true，那么断言为真，并且无任何行动.
            // 如果表达式为false，则断言失败，则会抛出一个AssertionError对象。这个AssertionError继承于Error对象，
            // 而Error继承于Throwable，Error是和Exception并列的一个错误对象，通常用于表达系统级运行错误。
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            // 输出到SurfaceTexture需要进行如下设置
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            // Surface就是SurfaceView中使用的Surface，就是内存中的一段绘图缓冲区。
            // Surface是用来管理数据的。（句柄）
            // 1、通过Surface（因为Surface是句柄）就可以获得原生缓冲器以及其中的内容。就像在C++语言中，可以通过一个文件的句柄，就可以获得文件的内容一样。
            // 2、原始缓冲区（a raw buffer）是用于保存当前窗口的像素数据的。
            // 3、Surface中有一个Canvas成员，专门用于画图的。
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            // 我们跟相机通信只有通过CameraCaptureSession。而要和CameraCaptureSession通信就是发送请求。这里我们相当于在创建请求的一些参数。
            // TEMPLATE_RECORD   创建适合录像的请求。
            // TEMPLATE_PREVIEW 创建一个适合于相机预览窗口的请求。
            // TEMPLATE_STILL_CAPTURE 创建适用于静态图像捕获的请求
            // TEMPLATE_VIDEO_SNAPSHOT  在录制视频时创建适合静态图像捕获的请求。
//            mPreviewRequestBuilder
//                    = mCameraProxy.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            // 创建会话是一项昂贵的操作，可能需要几百毫秒，因为它需要配置摄像机设备的内部管道并分配内存缓冲区以将图像发送到所需目标。因此，设置是异步完成的，
            // 如果摄像机设备创建了新会话，则会关闭先前的会话，并将调用其关联的StateCallback＃onClosed回调。
            // 如果在会话关闭后调用，则所有会话方法都将抛出IllegalStateException。
            // 关闭会话会清除所有重复请求（就像调用了stopRepeating（）一样），但在新创建的会话接管并重新配置摄像机设备之前，仍将正常完成所有正在进行的捕获请求。
            // 如果你想节约时间可以invoke abortCaptures，它会discard the remaining requests.
            // CameraDevices为每一个surface都会创建一个流（Session），还可以加其他类型都surface如：
            // SurfaceView
            // MediaCodec
            // MediaRecorder
            // AllocationF
           /* mCameraProxy.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        // StateCallback
                        // 创建Session创建成功之后回调， 如果有request submit则之后回调onActive， 反之回调onReady
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraProxy) {
                                Log.e("kk", " configure mCameraProxy " + mCameraProxy);
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                // 通过set 为请求request添加字段，所有的字段在CaptureRequest中均能找到。
                                // auto focus默认支持的， 亟需给它指定一种模式
                                // CONTROL_AF_MODE_OFF
                                // CONTROL_AF_MODE_AUTO
                                // CONTROL_AF_MODE_MACRO
                                // CONTROL_AF_MODE_CONTINUOUS_VIDEO
                                // CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                // CONTROL_AF_MODE_EDOF
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                // Flash 是AE mode
                                setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mainHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                                Log.e("kk", " configure failed " + e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            CameraUtil.showToast(mActivity,"onConfigureFailed Failed");
                        }
                    }, null
            );*/
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("kk", " Exception " + e);
        }
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    public void takePicture() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void startTakePicture() {
        lockFocus();
    }

    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            Log.e(TAG, " lockFocus CONTROL_AF_TRIGGER >> STATE_WAITING_LOCK ");
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, " lockFocus " + e);
        }
    }

    @Override
    public void onCameraOpened(CameraAgent.CameraProxy camera) {
        Log.v(TAG, "onCameraOpened");
        if (mRequestingCameraId != camera.getCameraId()) {
            return;
        }
        mCameraProxy = camera;
        mRequestingCameraId = EMPTY_REQUEST;
        if (mCallbackReceiver != null) {
            mCallbackReceiver.onCameraOpened(camera);
        }
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

    public void setSurfaceTexture(SurfaceTexture surface, int width, int height) {
        mSurfaceTexture = surface;
        mSurfaceWidth = width;
        mSurfaceHeight = height;
    }

    public void closeCamera(boolean synced) {
        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (null != mCameraProxy) {
//            mCameraProxy.close();
            mCameraProxy = null;
        }
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    public void startPreview() {
        setUpCameraOutputs();
        createCameraPreviewSession();
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
