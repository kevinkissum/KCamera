package com.example.kevin.kcamera;

import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera.Area;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.example.kevin.kcamera.Ex.CameraCapabilities;
import com.example.kevin.kcamera.Interface.AppController;
import com.example.kevin.kcamera.Interface.FocusRing;
import com.example.kevin.kcamera.Interface.MotionManager;
import com.example.kevin.kcamera.Interface.PreviewStatusListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/* A class that handles everything about focus in still picture mode.
 * This also handles the metering area because it is the same as focus area.
 *
 * The test cases:
 * (1) The camera has continuous autofocus. Move the camera. Take a picture when
 *     CAF is not in progress.
 * (2) The camera has continuous autofocus. Move the camera. Take a picture when
 *     CAF is in progress.
 * (3) The camera has face detection. Point the camera at some faces. Hold the
 *     shutter. Release to take a picture.
 * (4) The camera has face detection. Point the camera at some faces. Single tap
 *     the shutter to take a picture.
 * (5) The camera has autofocus. Single tap the shutter to take a picture.
 * (6) The camera has autofocus. Hold the shutter. Release to take a picture.
 * (7) The camera has no autofocus. Single tap the shutter and take a picture.
 * (8) The camera has autofocus and supports focus area. Touch the screen to
 *     trigger autofocus. Take a picture.
 * (9) The camera has autofocus and supports focus area. Touch the screen to
 *     trigger autofocus. Wait until it times out.
 * (10) The camera has no autofocus and supports metering area. Touch the screen
 *     to change metering area.
 */
public class FocusOverlayManager implements PreviewStatusListener.PreviewAreaChangedListener,
        MotionManager.MotionListener {
    private static final String TAG = "CAM_FocusOverlayMgr";

    private static final int RESET_TOUCH_FOCUS = 0;

//    private static final int RESET_TOUCH_FOCUS_DELAY_MILLIS = Settings3A.getFocusHoldMillis();

//    public static final float AF_REGION_BOX = Settings3A.getAutoFocusRegionWidth();
//    public static final float AE_REGION_BOX = Settings3A.getMeteringRegionWidth();

    private int mState = STATE_IDLE;
    private static final int STATE_IDLE = 0; // Focus is not active.
    private static final int STATE_FOCUSING = 1; // Focus is in progress.
    // Focus is in progress and the camera should take a picture after focus finishes.
    private static final int STATE_FOCUSING_SNAP_ON_FINISH = 2;
    private static final int STATE_SUCCESS = 3; // Focus finishes and succeeds.
    private static final int STATE_FAIL = 4; // Focus finishes and fails.

    private boolean mInitialized;
    private boolean mFocusAreaSupported;
    private boolean mMeteringAreaSupported;
    private boolean mLockAeAwbNeeded;
    private boolean mAeAwbLock;
    private CameraCoordinateTransformer mCoordinateTransformer;

    private boolean mMirror; // true if the camera is front-facing.
    private int mDisplayOrientation;
    private List<Area> mFocusArea; // focus area in driver format
    private List<Area> mMeteringArea; // metering area in driver format
//    private CameraCapabilities.FocusMode mFocusMode;
//    private final List<CameraCapabilities.FocusMode> mDefaultFocusModes;
//    private CameraCapabilities.FocusMode mOverrideFocusMode;
//    private CameraCapabilities mCapabilities;
    private final AppController mAppController;
    //    private final SettingsManager mSettingsManager;
    private final Handler mHandler;
    Listener mListener;
    private boolean mPreviousMoving;
        private final FocusRing mFocusRing;
    private final Rect mPreviewRect = new Rect(0, 0, 0, 0);
    private boolean mFocusLocked;

    public void removeMessages() {

    }

    public void onAutoFocus(boolean focused, boolean shutterButtonPressed) {
        if (mState == STATE_FOCUSING_SNAP_ON_FINISH) {
            // Take the picture no matter focus succeeds or fails. No need
            // to play the AF sound if we're about to play the shutter
            // sound.
            if (focused) {
                mState = STATE_SUCCESS;
            } else {
                mState = STATE_FAIL;
            }
            capture();
        } else if (mState == STATE_FOCUSING) {
            // This happens when (1) user is half-pressing the focus key or
            // (2) touch focus is triggered. Play the focus tone. Do not
            // take the picture now.
            if (focused) {
                mState = STATE_SUCCESS;
            } else {
                mState = STATE_FAIL;
            }
            // If this is triggered by touch focus, cancel focus after a
            // while.
            if (mFocusArea != null) {
                mFocusLocked = true;
//                mHandler.sendEmptyMessageDelayed(RESET_TOUCH_FOCUS, RESET_TOUCH_FOCUS_DELAY_MILLIS);
            }
            if (shutterButtonPressed) {
                // Lock AE & AWB so users can half-press shutter and recompose.
//                lockAeAwbIfNeeded();
            }
        } else if (mState == STATE_IDLE) {
            // User has released the focus key before focus completes.
            // Do nothing.
        }
    }

    public void onPreviewStopped() {
        mState = STATE_IDLE;
    }

    /**
     * TODO: Refactor this so that we either don't need a handler or make
     * mListener not be the activity.
     */
    public interface Listener {
        public void autoFocus();

        public void cancelAutoFocus();

        public boolean capture();

        public void startFaceDetection();

        public void stopFaceDetection();

        public void setFocusParameters();
    }

    /**
     * Manual tap to focus parameters
     */
//    private TouchCoordinate mTouchCoordinate;
    private long mTouchTime;
    private static class MainHandler extends Handler {
        /**
         * The outer mListener at the moment is actually the CameraActivity,
         * which we would leak if we didn't break the GC path here using a
         * WeakReference.
         */
        final WeakReference<FocusOverlayManager> mManager;

        public MainHandler(FocusOverlayManager manager, Looper looper) {
            super(looper);
            mManager = new WeakReference<FocusOverlayManager>(manager);
        }

        @Override
        public void handleMessage(Message msg) {
            FocusOverlayManager manager = mManager.get();
            if (manager == null) {
                return;
            }

            switch (msg.what) {
                case RESET_TOUCH_FOCUS: {
                    manager.cancelAutoFocus();
                    manager.mListener.startFaceDetection();
                    break;
                }
            }
        }


    }

    public FocusOverlayManager(AppController appController,
                               List<CameraCapabilities.FocusMode> defaultFocusModes, CameraCapabilities capabilities,
                               Listener listener, boolean mirror, Looper looper, FocusRing focusRing) {
        mAppController = appController;
//        mSettingsManager = appController.getSettingsManager();
        mHandler = new MainHandler(this, looper);
//        mDefaultFocusModes = new ArrayList<CameraCapabilities.FocusMode>(defaultFocusModes);
//        updateCapabilities(capabilities);
        mListener = listener;
//        setMirror(mirror);
        mFocusRing = focusRing;
        mFocusLocked = false;
    }

    public boolean isFocusingSnapOnFinish() {
        return mState == STATE_FOCUSING_SNAP_ON_FINISH;
    }

    private void cancelAutoFocus() {
        Log.v(TAG, "Cancel autofocus.");
        // Reset the tap area before calling mListener.cancelAutofocus.
        // Otherwise, focus mode stays at auto and the tap area passed to the
        // driver is not reset.
        resetTouchFocus();
        mListener.cancelAutoFocus();
        mState = STATE_IDLE;
        mFocusLocked = false;
        mHandler.removeMessages(RESET_TOUCH_FOCUS);
    }

    public void resetTouchFocus() {
        if (!mInitialized) {
            return;
        }

        mFocusArea = null;
        mMeteringArea = null;
        // This will cause current module to call getFocusAreas() and
        // getMeteringAreas() and send updated regions to camera.
        mListener.setFocusParameters();

    }

    public void focusAndCapture(CameraCapabilities.FocusMode currentFocusMode) {
        if (!mInitialized) {
//            return;
        }

        if (!needAutoFocusCall(currentFocusMode)) {
            // Focus is not needed.
            capture();
        } else if (mState == STATE_SUCCESS || mState == STATE_FAIL) {
            // Focus is done already.
            capture();
        } else if (mState == STATE_FOCUSING) {
            // Still focusing and will not trigger snap upon finish.
            mState = STATE_FOCUSING_SNAP_ON_FINISH;
        } else if (mState == STATE_IDLE) {
            autoFocusAndCapture();
        }
    }

    private void autoFocusAndCapture() {
        autoFocus(STATE_FOCUSING_SNAP_ON_FINISH);
    }

    private void autoFocus(int focusingState) {
        mListener.autoFocus();
        mState = focusingState;
        mHandler.removeMessages(RESET_TOUCH_FOCUS);
    }

    private void capture() {
        if (mListener.capture()) {
            mState = STATE_IDLE;
            mHandler.removeMessages(RESET_TOUCH_FOCUS);
        }
    }

    /** This setter should be the only way to mutate mPreviewRect. */
    public void setPreviewRect(Rect previewRect) {
        if (!mPreviewRect.equals(previewRect)) {
            mPreviewRect.set(previewRect);
            mFocusRing.configurePreviewDimensions(CameraUtil.rectToRectF(mPreviewRect));
            resetCoordinateTransformer();
            mInitialized = true;
        }
    }

    private void resetCoordinateTransformer() {
        if (mPreviewRect.width() > 0 && mPreviewRect.height() > 0) {
            mCoordinateTransformer = new CameraCoordinateTransformer(mMirror, mDisplayOrientation,
                    CameraUtil.rectToRectF(mPreviewRect));
        } else {
            Log.w(TAG, "The coordinate transformer could not be built because the preview rect"
                    + "did not have a width and height");
        }
    }

    private boolean needAutoFocusCall(CameraCapabilities.FocusMode focusMode) {
        return !(focusMode == CameraCapabilities.FocusMode.INFINITY
                || focusMode == CameraCapabilities.FocusMode.FIXED
                || focusMode == CameraCapabilities.FocusMode.EXTENDED_DOF);
    }

    @Override
    public void onMoving() {

    }

    @Override
    public void onPreviewAreaChanged(RectF previewArea) {
        setPreviewRect(CameraUtil.rectFToRect(previewArea));
    }

}