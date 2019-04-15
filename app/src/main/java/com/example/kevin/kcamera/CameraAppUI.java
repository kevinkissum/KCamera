package com.example.kevin.kcamera;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;

import com.example.kevin.kcamera.Interface.AppController;
import com.example.kevin.kcamera.View.AutoFitTextureView;
import com.example.kevin.kcamera.View.BottomBar;
import com.example.kevin.kcamera.View.MainActivityLayout;
import com.example.kevin.kcamera.View.ModeListView;
import com.example.kevin.kcamera.View.MultiToggleImageButton;
import com.example.kevin.kcamera.View.PreviewOverlay;
import com.example.kevin.kcamera.View.ShutterButton;
import com.example.kevin.kcamera.View.StickyBottomCaptureLayout;

public class CameraAppUI implements TextureView.SurfaceTextureListener,
        ShutterButton.OnShutterButtonListener, View.OnClickListener,
        ModeListView.ModeListOpenListener {

    private static final String TAG = "CameraAppUI";

    private final static int IDLE = 0;
    private final static int SWIPE_UP = 1;
    private final static int SWIPE_DOWN = 2;
    private final static int SWIPE_LEFT = 3;
    private final static int SWIPE_RIGHT = 4;
    private final static int SWIPE_TIME_OUT_MS = 500;
    private final AppController mController;


    private Context mContext;
    private MainActivityLayout mRootView;
    private AutoFitTextureView mTextureView;
    private ShutterButton mShutter;
    private CaptureLayoutHelper mCaptureLayoutHelper;
    private BottomBar mBottomBar;
    private StickyBottomCaptureLayout mStickyBottomCaptureLayout;
    private MultiToggleImageButton mSwitchCamera;
    private PreviewOverlay mPreviewOverlay;
    private CameraActivity mActivity;
    private int mSwipeState;
    private boolean mSwipeEnabled;
    private int mSlop;
    private GestureDetector mGestureDetector;
    private ModeListView mModeListView;
    private boolean mDisableAllUserInteractions;
    private SurfaceTexture mSurface;

    private final ButtonManager.ButtonCallback mCameraCallback =
            new ButtonManager.ButtonCallback() {
                @Override
                public void onStateChanged(int state) {

                    ButtonManager buttonManager = mController.getButtonManager();
//                    buttonManager.disableCameraButtonAndBlock();
                    Log.d(TAG, "Start to switch camera. cameraId=" + state);
                }
            };

    private ButtonManager.ButtonCallback getDisableButtonCallback(final int conflictingButton) {
        return new ButtonManager.ButtonCallback() {
            @Override
            public void onStateChanged(int state) {
                mController.getButtonManager().disableButton(conflictingButton);
            }
        };
    }

    public CameraAppUI(AppController controller, CameraActivity activity, MainActivityLayout rootView) {
        mRootView = rootView;
        mActivity = activity;
        mContext = activity.getApplicationContext();
        mController = controller;
        init();
    }

    private void init() {
        Resources res = mActivity.getResources();
        mTextureView = (AutoFitTextureView) mRootView.findViewById(R.id.preview_content);
        mTextureView.setSurfaceTextureListener(this);
        mShutter = mRootView.findViewById(R.id.shutter_button);
        mShutter.addOnShutterButtonListener(this);
        mCaptureLayoutHelper = new CaptureLayoutHelper(
                res.getDimensionPixelSize(R.dimen.bottom_bar_height_min),
                res.getDimensionPixelSize(R.dimen.bottom_bar_height_max),
                res.getDimensionPixelSize(R.dimen.bottom_bar_height_optimal));
        mRootView.setNonDecorWindowSizeChangedListener(mCaptureLayoutHelper);
        mBottomBar = mRootView.findViewById(R.id.bottom_bar);
        mStickyBottomCaptureLayout = mRootView.findViewById(R.id.sticky_bottom_capture_layout);
        mBottomBar.setCaptureLayoutHelper(mCaptureLayoutHelper);
        mStickyBottomCaptureLayout.setCaptureLayoutHelper(mCaptureLayoutHelper);
        mSwitchCamera = mRootView.findViewById(R.id.camera_switch);
        mSwitchCamera.setOnClickListener(this);
        mGestureDetector = new GestureDetector(mContext, new MyGestureListener());
        mPreviewOverlay = mRootView.findViewById(R.id.preview_overlay);
        mPreviewOverlay.GestureDetectorListener(mGestureDetector);
        mModeListView = mRootView.findViewById(R.id.mode_list_layout);
        mModeListView.setCaptureLayoutHelper(mCaptureLayoutHelper);
        mModeListView.setModeListOpenListener(this);

    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mSurface = surface;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        mCaptureLayoutHelper.setAspectRatio((float)width / height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public void setPreViewSize(int width, int height) {
        mTextureView.setAspectRatio(width, height);
    }

    @Override
    public void onShutterButtonFocus(boolean pressed) {

    }

    @Override
    public void onShutterButtonClick() {
    }


    @Override
    public void onShutterButtonLongPressed() {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.camera_switch:
                break;
        }
    }

    @Override
    public void onOpenFullScreen() {

    }

    @Override
    public void onModeListOpenProgress(float progress) {

    }

    @Override
    public void onModeListClosed() {

    }

    public void onResume() {
        showShimmyDelayed();
    }

    private void showShimmyDelayed() {
        // 隐藏modeListView
        mModeListView.showModeSwitcherHint();
    }

    public boolean isShutterButtonEnabled() {
        return mBottomBar.isShutterButtonEnabled();
    }

    public void setDisableAllUserInteractions(boolean disable) {
//        if (disable) {
//            disableModeOptions();
//            setShutterButtonEnabled(false);
//            setSwipeEnabled(false);
//            mModeListView.hideAnimated();
//        } else {
//            enableModeOptions();
//            setShutterButtonEnabled(true);
//            setSwipeEnabled(true);
//        }
//        mDisableAllUserInteractions = disable;
    }

    public void setShutterButtonEnabled(final boolean enabled) {
        if (!mDisableAllUserInteractions) {
            mBottomBar.post(new Runnable() {
                @Override
                public void run() {
                    mBottomBar.setShutterButtonEnabled(enabled);
                }
            });
        }
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurface;
    }

    public void addShutterListener(ShutterButton.OnShutterButtonListener listener) {
        mShutter.addOnShutterButtonListener(listener);
    }

    public void prepareModuleUI() {
        addShutterListener(mController.getCurrentModuleController());
    }

    public interface NonDecorWindowSizeChangedListener {
        public void onNonDecorWindowSizeChanged(int width, int height, int rotation);
    }

    // 测试发现该GestureListener不需要， Mainlayout将事件拦截， 并在其TouchEvent中调用modelistView的onTouchEvent
    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private MotionEvent mDown;

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent ev, float distanceX, float distanceY) {
//            if (ev.getEventTime() - ev.getDownTime() > SWIPE_TIME_OUT_MS
//                    || mSwipeState != IDLE
//                    || !mSwipeEnabled) {
//                return false;
//            }
            Log.d("kk", " app ui MyGestureListener onScroll ");
            int deltaX = (int) (ev.getX() - mDown.getX());
            int deltaY = (int) (ev.getY() - mDown.getY());
            if (ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
                if (Math.abs(deltaX) > mSlop || Math.abs(deltaY) > mSlop) {
                    // Calculate the direction of the swipe.
                    if (deltaX >= Math.abs(deltaY)) {
                        // Swipe right.
                        setSwipeState(SWIPE_RIGHT);
                        Log.d("kk", " app ui SWIPE_RIGHT ");
                    } else if (deltaX <= -Math.abs(deltaY)) {
                        // Swipe left.
                        Log.d("kk", " app ui SWIPE_LEFT ");
                        setSwipeState(SWIPE_LEFT);
                    }
                }
            }
            return true;
        }

        private void setSwipeState(int swipeState) {
            mSwipeState = swipeState;
            // Notify new swipe detected.
            onSwipeDetected(swipeState);
        }

        @Override
        public boolean onDown(MotionEvent ev) {
            Log.d("kk", " app ui MyGestureListener onDown ");
            mDown = MotionEvent.obtain(ev);
            mSwipeState = IDLE;
            return false;
        }


    }

    private void onSwipeDetected(int swipeState) {
        if (swipeState == SWIPE_UP || swipeState == SWIPE_DOWN) {
        } else if (swipeState == SWIPE_LEFT) {
            // Pass the touch sequence to filmstrip layout.
//            mAppRootView.redirectTouchEventsTo(mFilmstripLayout);
        } else if (swipeState == SWIPE_RIGHT) {
            // Pass the touch to mode switcher
            mRootView.redirectTouchEventsTo(mModeListView);
        }
    }


}
