package com.example.kevin.kcamera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;

import com.example.kevin.kcamera.Abstract.CameraModule;
import com.example.kevin.kcamera.Ex.CameraDeviceInfo;
import com.example.kevin.kcamera.Ex.exif.ExifInterface;
import com.example.kevin.kcamera.Ex.exif.ExifTag;
import com.example.kevin.kcamera.Ex.exif.Rational;
import com.example.kevin.kcamera.Interface.MediaSaver;
import com.example.kevin.kcamera.Size;
import com.example.kevin.kcamera.Ex.AndroidCamera2Settings;
import com.example.kevin.kcamera.Ex.CameraAgent;
import com.example.kevin.kcamera.Ex.CameraCapabilities;
import com.example.kevin.kcamera.Ex.CameraSettings;
import com.example.kevin.kcamera.Interface.AppController;
import com.example.kevin.kcamera.Interface.PhotoController;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Vector;

public class PhotoModule extends CameraModule implements PhotoController, FocusOverlayManager.Listener {


    private static final String TAG = "CAM_PhotoModule";
    public static final int DEFAULT_CAPTURE_PIXELS = 1920 * 1080;

    protected CameraCapabilities mCameraCapabilities;
    private boolean mFocusAreaSupported;
    private boolean mMeteringAreaSupported;
    private boolean mAeLockSupported;
    private boolean mAwbLockSupported;
    private boolean mContinuousFocusSupported;

    private CameraActivity mActivity;
    private Context mContext;
    private PhotoUI mUI;
    private boolean mPaused;
    private int mCameraId;
    private CameraSettings mCameraSettings;
    private int mCameraState;
    private AppController mAppController;
    private boolean mVolumeButtonClickedFlag;
    private FocusOverlayManager mFocusManager;
    private boolean mIsImageCaptureIntent;
    private boolean mSnapshotOnIdle;
    private CameraCapabilities.SceneMode mSceneMode;
    private CameraAgent.CameraProxy mCameraProxy;
    private final Handler mHandler = new MainHandler(this);
    private boolean mShouldResizeTo16x9;
    private int mCameraPreviewWidth;
    private int mCameraPreviewHeight;
    private boolean mMirror;
    private long mCaptureStartTime;
    private int mPostViewPictureCallbackTime;
    private int mJpegRotation;
    private long mFocusStartTime;
    private long mAutoFocusTime;
    private boolean mFaceDetectionStarted;
    private long mJpegPictureCallbackTime;
    private long mPictureDisplayedToJpegCallbackTime;
    private long mJpegCallbackFinishTime;
    private NamedImages mNamedImages;

    private final AutoFocusCallback mAutoFocusCallback =
            new AutoFocusCallback();


    private final Runnable mDoSnapRunnable = new Runnable() {
        @Override
        public void run() {
            onShutterButtonClick();
        }
    };

    private final MediaSaver.OnMediaSavedListener mOnMediaSavedListener =
            new MediaSaver.OnMediaSavedListener() {

                @Override
                public void onMediaSaved(Uri uri) {
                    if (uri != null) {
//                        mActivity.notifyNewMedia(uri);
                    } else {
//                        onError();
                    }
                }
            };

    public PhotoModule (AppController app) {
        super(app);

    }

    @Override
    public void init(CameraActivity activity) {
        mContext = activity.getApplicationContext();
        mActivity = activity;
        mAppController = activity;
        mUI = new PhotoUI(mActivity, mActivity.getModuleLayoutRoot());
        mIsImageCaptureIntent = isImageCaptureIntent();
    }

    @Override
    public boolean isImageCaptureIntent() {
        String action = mActivity.getIntent().getAction();
        return (MediaStore.ACTION_IMAGE_CAPTURE.equals(action)
                || CameraActivity.ACTION_IMAGE_CAPTURE_SECURE.equals(action));
    }

    public void requestCameraOpen(SurfaceTexture surface, int width, int height) {
//        mCameraControl.setSurfaceTexture(surface, width, height);
//        mCameraControl.requestCamera(mCameraId, true);
    }

    private void requestCameraOpen() {
        Log.v(TAG, "requestCameraOpen");
        mActivity.getCameraProvider().requestCamera(mCameraId, true);
    }

    public void startPreview() {
        if (mCameraProxy == null) {
            Log.i(TAG, "attempted to start preview before camera device");
            // do nothing
            return;
        }

        if (!checkPreviewPreconditions()) {
            return;
        }

        updateParametersPictureSize();

        mCameraProxy.setPreviewTexture(mActivity.getCameraAppUI().getSurfaceTexture());

        Log.i(TAG, "startPreview");

        CameraAgent.CameraStartPreviewCallback startPreviewCallback =
                new CameraAgent.CameraStartPreviewCallback() {
                    @Override
                    public void onPreviewStarted() {
//                        mFocusManager.onPreviewStarted();
                        PhotoModule.this.onPreviewStarted();
                        if (mSnapshotOnIdle) {
                            mHandler.post(mDoSnapRunnable);
                        }
                    }
                };
//        if (GservicesHelper.useCamera2ApiThroughPortabilityLayer(mActivity.getContentResolver())) {
            mCameraProxy.startPreview();
            startPreviewCallback.onPreviewStarted();
//        } else {
//            mCameraDevice.startPreviewWithCallback(new Handler(Looper.getMainLooper()),
//                    startPreviewCallback);
//        }
    }

    private void onPreviewStarted() {
        mAppController.onPreviewStarted();
        mAppController.setShutterEnabled(true);
        setCameraState(IDLE);
//        startFaceDetection();
    }

    private void setCameraState(int state) {
        mCameraState = state;
        switch (state) {
            case PREVIEW_STOPPED:
            case SNAPSHOT_IN_PROGRESS:
            case SWITCHING_CAMERA:
                // TODO: Tell app UI to disable swipe
                break;

        }
    }


    /**
     * Returns whether we can/should start the preview or not.
     */
    private boolean checkPreviewPreconditions() {
        if (mPaused) {
            return false;
        }

        if (mCameraProxy == null) {
            Log.w(TAG, "startPreview: camera device not ready yet.");
            return false;
        }

//        SurfaceTexture st = mActivity.getCameraAppUI().getSurfaceTexture();
//        if (st == null) {
//            Log.w(TAG, "startPreview: surfaceTexture is not ready.");
//            return false;
//        }
//
//        if (!mCameraPreviewParamsReady) {
//            Log.w(TAG, "startPreview: parameters for preview is not ready.");
//            return false;
//        }
        return true;
    }

    private void updateParametersPictureSize() {
        if (mCameraProxy == null) {
            Log.w(TAG, "attempting to set picture size without caemra device");
            return;
        }
        List<Size> sizesPhoto = Size.convert(mCameraCapabilities.getSupportedPhotoSizes());
        if (!findBestPreviewSize(sizesPhoto, true, true)) {
            Log.w(TAG, "No 16:9 ratio preview size supported.");
            if (!findBestPreviewSize(sizesPhoto, false, true)) {
                Log.w(TAG, "Can't find a supported preview size smaller than 960x720.");
                findBestPreviewSize(sizesPhoto, false, false);
            }
        }

        CameraPictureSizesCacher.updateSizesForCamera(mAppController.getAndroidContext(),
                mCameraProxy.getCameraId(), sizesPhoto);



        mCameraSettings.setPhotoSize(new com.example.kevin.kcamera.Ex.Size(mCameraPreviewWidth, mCameraPreviewHeight));



        // Set a preview size that is closest to the viewfinder height and has
        // the right aspect ratio.
        List<Size> sizesPreview = Size.convert(mCameraCapabilities.getSupportedPreviewSizes());
        Size optimalSize = CameraUtil.getOptimalPreviewSize(sizesPreview,
                (double) mCameraPreviewWidth / mCameraPreviewHeight);
        Size original = new Size(mCameraSettings.getCurrentPreviewSize());
        if (!optimalSize.equals(original)) {
            Log.v(TAG, "setting preview size. optimal: " + optimalSize + " original: " + original);
            mCameraSettings.setPreviewSize(optimalSize.toPortabilitySize());

            mCameraProxy.applySettings(mCameraSettings);
            mCameraSettings = mCameraProxy.getSettings();
        }

        if (optimalSize.width() != 0 && optimalSize.height() != 0) {
            Log.v(TAG, "updating aspect ratio");
            mAppController.updatePreviewAspectRatio((float) optimalSize.width()
                    / (float) optimalSize.height());
        }
        Log.d(TAG, "Preview size is " + optimalSize);
    }

    private boolean findBestPreviewSize(List<Size> supportedSizes, boolean need16To9,
                                        boolean needSmaller) {
        int pixelsDiff = DEFAULT_CAPTURE_PIXELS;
        boolean hasFound = false;
        Size displayRealSize = CameraUtil.getDefaultDisplayRealSize();
        float targetRation = (float)displayRealSize.getHeight() / (float)displayRealSize.getWidth() == 18/9f ? 18/9f : 16/9f;
        for (Size size : supportedSizes) {
            int h = size.height();
            int w = size.width();
            // we only want 16:9 format.
            int d = DEFAULT_CAPTURE_PIXELS - h * w;
            if (needSmaller && d < 0) { // no bigger preview than 960x720.
                continue;
            }
            if (need16To9 &&(float)w/h != targetRation) {//SPRD:fix bug 614910 change the preview size to 16:9
                continue;
            }
            d = Math.abs(d);
            if (d < pixelsDiff) {
                mCameraPreviewWidth = w;
                mCameraPreviewHeight = h;
                pixelsDiff = d;
                hasFound = true;
            }
        }
        return hasFound;
    }


    private boolean isCameraFrontFacing() {
        return mAppController.getCameraProvider().getCharacteristics(mCameraId)
                .isFacingFront();
    }

    @Override
    public void onShutterButtonFocus(boolean pressed) {

    }

    @Override
    public void onShutterButtonClick() {
        Log.d(TAG, "  onShutterButtonClick ");
        if (mPaused || (mCameraState == SWITCHING_CAMERA)
                || (mCameraState == PREVIEW_STOPPED)
                || !mAppController.isShutterEnabled()) {
            mVolumeButtonClickedFlag = false;
            return;
        }
        // Do not take the picture if there is not enough storage.
        if (mActivity.getStorageSpaceBytes() <= Storage.LOW_STORAGE_THRESHOLD_BYTES) {
            Log.e(TAG, "Not enough space or storage not ready. remaining="
                    + mActivity.getStorageSpaceBytes());
            mVolumeButtonClickedFlag = false;
//            return;
        }

        mAppController.setShutterEnabled(false);

//        int countDownDuration = mActivity.getSettingsManager()
//                .getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_COUNTDOWN_DURATION);
//        mTimerDuration = countDownDuration;
//        if (countDownDuration > 0) {
//            // Start count down.
//            mAppController.getCameraAppUI().transitionToCancel();
//            mAppController.getCameraAppUI().hideModeOptions();
//            mUI.startCountdown(countDownDuration);
//            return;
//        } else {
            focusAndCapture();
//        }

        /*mCameraControl.startTakePicture();*/
    }

    @Override
    public void onShutterButtonLongPressed() {

    }

    private void focusAndCapture() {
        // If the user wants to do a snapshot while the previous one is still
        // in progress, remember the fact and do it after we finish the previous
        // one and re-start the preview. Snapshot in progress also includes the
        // state that autofocus is focusing and a picture will be taken when
        // focus callback arrives.
        Log.d("kk", "mCameraSettings.getCurrentFocusMode()  " + mCameraSettings.getCurrentFocusMode());
        if ((mFocusManager.isFocusingSnapOnFinish() || mCameraState == SNAPSHOT_IN_PROGRESS)) {
            if (!mIsImageCaptureIntent) {
                mSnapshotOnIdle = true;
            }
            return;
        }
        Log.d("kk", "mCameraSettings.getCurrentFocusMode()  " + mCameraSettings.getCurrentFocusMode());
        mSnapshotOnIdle = false;
        mFocusManager.focusAndCapture(mCameraSettings.getCurrentFocusMode());
    }

    public void switchCamera() {
        if (mPaused) {
            return;
        }
        closeCamera();
        mCameraId ^= 1;
//        mCameraControl.requestCamera(mCameraId, true);
        requestCameraOpen();
    }

    private void closeCamera() {
//        mCameraControl.closeCamera();
    }

    @Override
    public void resume() {
        mPaused = false;
        requestCameraOpen();
        mNamedImages = new NamedImages();

    }

    @Override
    public void pause() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void onPreviewVisibilityChanged(int visibility) {

    }

    @Override
    public void onLayoutOrientationChanged(boolean isLandscape) {

    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void onCameraAvailable(CameraAgent.CameraProxy cameraProxy) {
        Log.i(TAG, "onCameraAvailable " + cameraProxy);
        if (mPaused) {
            return;
        }
        mCameraProxy = cameraProxy;
        initializeCapabilities();
        if (mFocusManager == null) {
            initializeFocusManager();
        }
        mCameraSettings = mCameraProxy.getSettings();
        mCameraSettings.setFocusMode(CameraCapabilities.FocusMode.AUTO);
        startPreview();
//        onCameraOpened();

    }

    private void initializeFocusManager() {
        if (mFocusManager != null) {
            mFocusManager.removeMessages();
        } else {
//            mMirror = isCameraFrontFacing();
//            String[] defaultFocusModesStrings = mActivity.getResources().getStringArray(
//                    R.array.pref_camera_focusmode_default_array);
//            ArrayList<CameraCapabilities.FocusMode> defaultFocusModes =
//                    new ArrayList<CameraCapabilities.FocusMode>();
//            CameraCapabilities.Stringifier stringifier = mCameraCapabilities.getStringifier();
//            for (String modeString : defaultFocusModesStrings) {
//                CameraCapabilities.FocusMode mode = stringifier.focusModeFromString(modeString);
//                if (mode != null) {
//                    defaultFocusModes.add(mode);
//                }
//            }
            mFocusManager =
                    new FocusOverlayManager(mAppController, /*defaultFocusModes*/null,
                            mCameraCapabilities, this, mMirror, mActivity.getMainLooper(),
                            null);
//            mMotionManager = getServices().getMotionManager();
//            if (mMotionManager != null) {
//                mMotionManager.addListener(mFocusManager);
//            }
        }
//        mAppController.addPreviewAreaSizeChangedListener(mFocusManager);
    }

    private void initializeCapabilities() {
        mCameraCapabilities = mCameraProxy.getCapabilities();
        mFocusAreaSupported = mCameraCapabilities
                .supports(CameraCapabilities.Feature.FOCUS_AREA);
        mMeteringAreaSupported = mCameraCapabilities
                .supports(CameraCapabilities.Feature.METERING_AREA);
        mAeLockSupported = mCameraCapabilities
                .supports(CameraCapabilities.Feature.AUTO_EXPOSURE_LOCK);
        mAwbLockSupported = mCameraCapabilities
                .supports(CameraCapabilities.Feature.AUTO_WHITE_BALANCE_LOCK);
        mContinuousFocusSupported = mCameraCapabilities
                .supports(CameraCapabilities.FocusMode.CONTINUOUS_PICTURE);
    }


    @Override
    public void autoFocus() {
        if (mCameraProxy == null) {
            return;
        }
        Log.v(TAG,"Starting auto focus");
        mFocusStartTime = System.currentTimeMillis();
        mCameraProxy.autoFocus(mHandler, mAutoFocusCallback);
        setCameraState(FOCUSING);
    }

    @Override
    public void cancelAutoFocus() {

    }

    @Override
    public boolean capture() {
        Log.i(TAG, "capture");
        // If we are already in the middle of taking a snapshot or the image
        // save request is full then ignore.
        if (mCameraProxy == null || mCameraState == SNAPSHOT_IN_PROGRESS
                || mCameraState == SWITCHING_CAMERA) {
            return false;
        }
        setCameraState(SNAPSHOT_IN_PROGRESS);

        mCaptureStartTime = System.currentTimeMillis();

        mPostViewPictureCallbackTime = 0;
//        mJpegImageData = null;

        final boolean animateBefore = (mSceneMode == CameraCapabilities.SceneMode.HDR);

        if (animateBefore) {
//            animateAfterShutter();
        }

//        Location loc = mActivity.getLocationManager().getCurrentLocation();
//        CameraUtil.setGpsParameters(mCameraSettings, loc);
        mCameraProxy.applySettings(mCameraSettings);

        // Set JPEG orientation. Even if screen UI is locked in portrait, camera orientation should
        // still match device orientation (e.g., users should always get landscape photos while
        // capturing by putting device in landscape.)
        CameraDeviceInfo.Characteristics info = mActivity.getCameraProvider().getCharacteristics(mCameraId);
        int sensorOrientation = info.getSensorOrientation();
        int deviceOrientation =
                mAppController.getOrientationManager().getDeviceOrientation().getDegrees();
        boolean isFrontCamera = info.isFacingFront();
        mJpegRotation =
                CameraUtil.getImageRotation(sensorOrientation, deviceOrientation, isFrontCamera);
        mCameraProxy.setJpegOrientation(mJpegRotation);

        mCameraProxy.takePicture(mHandler,
                /*new ShutterCallback(!animateBefore)*/null,
                null, null,
                new JpegPictureCallback(null));
        mNamedImages.nameNewImage(mCaptureStartTime);

//        mFaceDetectionStarted = false;
        return true;    }

    @Override
    public void startFaceDetection() {

    }

    @Override
    public void stopFaceDetection() {

    }

    @Override
    public void setFocusParameters() {

    }

    private final class AutoFocusCallback implements CameraAgent.CameraAFCallback {
        @Override
        public void onAutoFocus(boolean focused, CameraAgent.CameraProxy camera) {
            if (mPaused) {
                return;
            }

            mAutoFocusTime = System.currentTimeMillis() - mFocusStartTime;
            Log.v(TAG, "mAutoFocusTime = " + mAutoFocusTime + "ms   focused = "+focused);
            setCameraState(IDLE);
            mFocusManager.onAutoFocus(focused, false);
        }
    }

    private final class JpegPictureCallback implements CameraAgent.CameraPictureCallback {
        Location mLocation;

        public JpegPictureCallback(Location loc) {
            mLocation = loc;
        }

        @Override
        public void onPictureTaken(final byte[] originalJpegData, final CameraAgent.CameraProxy camera) {
            Log.i(TAG, "onPictureTaken");
            mAppController.setShutterEnabled(true);
            if (mPaused) {
                return;
            }
            if (mIsImageCaptureIntent) {
                stopPreview();
            }
            if (mSceneMode == CameraCapabilities.SceneMode.HDR) {
//                mUI.setSwipingEnabled(true);
            }

            mJpegPictureCallbackTime = System.currentTimeMillis();
            // If postview callback has arrived, the captured image is displayed
            // in postview callback. If not, the captured image is displayed in
            // raw picture callback.
//            if (mPostViewPictureCallbackTime != 0) {
//                mShutterToPictureDisplayedTime =
//                        mPostViewPictureCallbackTime - mShutterCallbackTime;
//                mPictureDisplayedToJpegCallbackTime =
//                        mJpegPictureCallbackTime - mPostViewPictureCallbackTime;
//            } else {
//                mShutterToPictureDisplayedTime =
//                        mRawPictureCallbackTime - mShutterCallbackTime;
//                mPictureDisplayedToJpegCallbackTime =
//                        mJpegPictureCallbackTime - mRawPictureCallbackTime;
//            }
            Log.v(TAG, "mPictureDisplayedToJpegCallbackTime = "
                    + mPictureDisplayedToJpegCallbackTime + "ms");

            if (!mIsImageCaptureIntent) {
                setupPreview();
            }

            long now = System.currentTimeMillis();
            mJpegCallbackFinishTime = now - mJpegPictureCallbackTime;
            Log.v(TAG, "mJpegCallbackFinishTime = " + mJpegCallbackFinishTime + "ms");
            mJpegPictureCallbackTime = 0;

            final ExifInterface exif = Exif.getExif(originalJpegData);
            final NamedImages.NamedEntity name = mNamedImages.getNextNameEntity();
            if (mShouldResizeTo16x9) {
//                final ResizeBundle dataBundle = new ResizeBundle();
//                dataBundle.jpegData = originalJpegData;
//                dataBundle.targetAspectRatio = ResolutionUtil.NEXUS_5_LARGE_16_BY_9_ASPECT_RATIO;
//                dataBundle.exif = exif;
//                new AsyncTask<ResizeBundle, Void, ResizeBundle>() {
//
//                    @Override
//                    protected ResizeBundle doInBackground(ResizeBundle... resizeBundles) {
//                        return cropJpegDataToAspectRatio(resizeBundles[0]);
//                    }
//
//                    @Override
//                    protected void onPostExecute(ResizeBundle result) {
//                        saveFinalPhoto(result.jpegData, name, result.exif, camera);
//                    }
//                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, dataBundle);

            } else {
                saveFinalPhoto(originalJpegData, name, exif, camera);
            }
        }
    }

    void saveFinalPhoto(final byte[] jpegData, NamedImages.NamedEntity name, final ExifInterface exif,
                        CameraAgent.CameraProxy camera) {
        int orientation = Exif.getOrientation(exif);

        float zoomValue = 1.0f;
        if (mCameraCapabilities.supports(CameraCapabilities.Feature.ZOOM)) {
            zoomValue = mCameraSettings.getCurrentZoomRatio();
        }
        boolean hdrOn = CameraCapabilities.SceneMode.HDR == mSceneMode;
//        String flashSetting =
//                mActivity.getSettingsManager().getString(mAppController.getCameraScope(),
//                        Keys.KEY_FLASH_MODE);
//        boolean gridLinesOn = Keys.areGridLinesOn(mActivity.getSettingsManager());
//        UsageStatistics.instance().photoCaptureDoneEvent(
//                eventprotos.NavigationChange.Mode.PHOTO_CAPTURE,
//                name.title + ".jpg", exif,
//                isCameraFrontFacing(), hdrOn, zoomValue, flashSetting, gridLinesOn,
//                (float) mTimerDuration, null, mShutterTouchCoordinate, mVolumeButtonClickedFlag,
//                null, null, null);
//        mShutterTouchCoordinate = null;
        mVolumeButtonClickedFlag = false;

        if (!mIsImageCaptureIntent) {
            // Calculate the width and the height of the jpeg.
            Integer exifWidth = exif.getTagIntValue(ExifInterface.TAG_PIXEL_X_DIMENSION);
            Integer exifHeight = exif.getTagIntValue(ExifInterface.TAG_PIXEL_Y_DIMENSION);
            int width, height;
            if (mShouldResizeTo16x9 && exifWidth != null && exifHeight != null) {
                width = exifWidth;
                height = exifHeight;
            } else {
                Size s = new Size(mCameraSettings.getCurrentPhotoSize());
                if ((mJpegRotation + orientation) % 180 == 0) {
                    width = s.width();
                    height = s.height();
                } else {
                    width = s.height();
                    height = s.width();
                }
            }
            String title = (name == null) ? null : name.title;
            long date = (name == null) ? -1 : name.date;

            // Handle debug mode outputs
//            if (mDebugUri != null) {
//                // If using a debug uri, save jpeg there.
//                saveToDebugUri(jpegData);
//
//                // Adjust the title of the debug image shown in mediastore.
//                if (title != null) {
//                    title = DEBUG_IMAGE_PREFIX + title;
//                }
//            }

            if (title == null) {
                Log.e(TAG, "Unbalanced name/data pair");
            } else {
                if (date == -1) {
                    date = mCaptureStartTime;
                }
//                int heading = mHeadingSensor.getCurrentHeading();
//                if (heading != HeadingSensor.INVALID_HEADING) {
//                    // heading direction has been updated by the sensor.
//                    ExifTag directionRefTag = exif.buildTag(
//                            ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
//                            ExifInterface.GpsTrackRef.MAGNETIC_DIRECTION);
//                    ExifTag directionTag = exif.buildTag(
//                            ExifInterface.TAG_GPS_IMG_DIRECTION,
//                            new Rational(heading, 1));
//                    exif.setTag(directionRefTag);
//                    exif.setTag(directionTag);
//                }
                getServices().getMediaSaver().addImage(
                        jpegData, title, date, null, width, height,
                        orientation, exif, mOnMediaSavedListener);
            }
            // Animate capture with real jpeg data instead of a preview
            // frame.
//            mUI.animateCapture(jpegData, orientation, mMirror);
        } else {
//            mJpegImageData = jpegData;
//            if (!mQuickCapture) {
                Log.v(TAG, "showing UI");
//                mUI.showCapturedImageForReview(jpegData, orientation, mMirror);
//            } else {
//                onCaptureDone();
//            }
        }

        // Send the taken photo to remote shutter listeners, if any are
        // registered.
//        getServices().getRemoteShutterListener().onPictureTaken(jpegData);

        // Check this in advance of each shot so we don't add to shutter
        // latency. It's true that someone else could write to the SD card
        // in the mean time and fill it, but that could have happened
        // between the shutter press and saving the JPEG too.
        mActivity.updateStorageSpaceAndHint(null);
    }

    private void setupPreview() {
        Log.i(TAG, "setupPreview");
        mFocusManager.resetTouchFocus();
        startPreview();
    }

    private void stopPreview() {
        if (mCameraProxy != null && mCameraState != PREVIEW_STOPPED) {
            Log.i(TAG, "stopPreview");
            mCameraProxy.stopPreview();
            mFaceDetectionStarted = false;
        }
        setCameraState(PREVIEW_STOPPED);
        if (mFocusManager != null) {
            mFocusManager.onPreviewStopped();
        }
    }

    public static class NamedImages {
        private final Vector<NamedEntity> mQueue;

        public NamedImages() {
            mQueue = new Vector<NamedEntity>();
        }

        public void nameNewImage(long date) {
            NamedEntity r = new NamedEntity();
            r.title = CameraUtil.instance().createJpegName(date);
            r.date = date;
            mQueue.add(r);
        }

        public NamedEntity getNextNameEntity() {
            synchronized (mQueue) {
                if (!mQueue.isEmpty()) {
                    return mQueue.remove(0);
                }
            }
            return null;
        }

        public static class NamedEntity {
            public String title;
            public long date;
        }
    }


    /**
         * This Handler is used to post message back onto the main thread of the
         * application
         */
    private static class MainHandler extends Handler {
            // Messages defined for the UI thread handler.
            private static final int MSG_FIRST_TIME_INIT = 1;
            private static final int MSG_SET_CAMERA_PARAMETERS_WHEN_IDLE = 2;
            private final WeakReference<PhotoModule> mModule;

            public MainHandler(PhotoModule module) {
                super(Looper.getMainLooper());
                mModule = new WeakReference<PhotoModule>(module);
            }

            @Override
            public void handleMessage(Message msg) {
                PhotoModule module = mModule.get();
                if (module == null) {
                    return;
                }

            }
        }
}
