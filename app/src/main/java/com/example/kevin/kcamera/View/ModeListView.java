package com.example.kevin.kcamera.View;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import java.util.List;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.example.kevin.kcamera.AndroidServices;
import com.example.kevin.kcamera.CaptureLayoutHelper;
import com.example.kevin.kcamera.R;
import com.example.kevin.kcamera.Util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class ModeListView extends FrameLayout {

    public static final String TAG = "ModeListView";

    // Animation Durations
    private static final int DEFAULT_DURATION_MS = 200;
    private static final int FLY_IN_DURATION_MS = 0;
    private static final int HOLD_DURATION_MS = 0;
    private static final int FLY_OUT_DURATION_MS = 850;
    private static final int START_DELAY_MS = 100;
    private static final int TOTAL_DURATION_MS = FLY_IN_DURATION_MS + HOLD_DURATION_MS
            + FLY_OUT_DURATION_MS;
    private static final int HIDE_SHIMMY_DELAY_MS = 1000;
    // Assumption for time since last scroll when no data point for last scroll.
    private static final int SCROLL_INTERVAL_MS = 50;
    // Last 20% percent of the drawer opening should be slow to ensure soft landing.
    private static final float SLOW_ZONE_PERCENTAGE = 0.2f;

    private static final int NO_ITEM_SELECTED = -1;

    // Scrolling delay between non-focused item and focused item
    private static final int DELAY_MS = 30;
    // If the fling velocity exceeds this threshold, snap to full screen at a constant
    // speed. Unit: pixel/ms.
    private static final float VELOCITY_THRESHOLD = 2f;

    /**
     * A factor to change the UI responsiveness on a scroll.
     * e.g. A scroll factor of 0.5 means UI will move half as fast as the finger.
     */
    private static final float SCROLL_FACTOR = 0.5f;
    // 60% opaque black background.
    private static final int BACKGROUND_TRANSPARENTCY = (int) (0.6f * 255);
    private static final int PREVIEW_DOWN_SAMPLE_FACTOR = 4;
    // Threshold, below which snap back will happen.
    private static final float SNAP_BACK_THRESHOLD_RATIO = 0.33f;

    private long mLastScrollTime;
    private int mListBackgroundColor;
    private LinearLayout mListView;
    private View mSettingsButton;
    private int mTotalModes;
    private AnimatorSet mAnimatorSet;
    private int mFocusItem = NO_ITEM_SELECTED;
    private int[] mInputPixels;
    private int[] mOutputPixels;
    private float mModeListOpenFactor = 1f;

    private View mChildViewTouched = null;
    private MotionEvent mLastChildTouchEvent = null;
    private int mVisibleWidth = 0;

    // Width and height of this view. They get updated in onLayout()
    // Unit for width and height are pixels.
    private int mWidth;
    private int mHeight;
    private float mScrollTrendX = 0f;
    private float mScrollTrendY = 0f;
    private ArrayList<Integer> mSupportedModes;
    private long mCurrentTime;
    private float mVelocityX; // Unit: pixel/ms.
    private long mLastDownTime = 0;
    private ModeSelectorItem[] mModeSelectorItems;
    private CaptureLayoutHelper mCaptureLayoutHelper;


    public ModeListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(List<Integer> modeIndexList) {
        int[] modeSequence = getResources()
                .getIntArray(R.array.camera_modes_in_nav_drawer_if_supported);
        int[] visibleModes = getResources()
                .getIntArray(R.array.camera_modes_always_visible);

        // Mark the supported modes in a boolean array to preserve the
        // sequence of the modes
        SparseBooleanArray modeIsSupported = new SparseBooleanArray();
        for (int i = 0; i < modeIndexList.size(); i++) {
            int mode = modeIndexList.get(i);
            modeIsSupported.put(mode, true);
        }
        for (int i = 0; i < visibleModes.length; i++) {
            int mode = visibleModes[i];
            modeIsSupported.put(mode, true);
        }

        // Put the indices of supported modes into an array preserving their
        // display order.
        mSupportedModes = new ArrayList<Integer>();
        for (int i = 0; i < modeSequence.length; i++) {
            int mode = modeSequence[i];
            if (modeIsSupported.get(mode, false)) {
                mSupportedModes.add(mode);
            }
        }
        mTotalModes = mSupportedModes.size();
        initializeModeSelectorItems();

    }

    public void setCaptureLayoutHelper(CaptureLayoutHelper helper) {
        mCaptureLayoutHelper = helper;
    }

    private void initializeModeSelectorItems() {
        mModeSelectorItems = new ModeSelectorItem[mTotalModes];
        // Inflate the mode selector items and add them to a linear layout
        LayoutInflater inflater = AndroidServices.instance().provideLayoutInflater();
        mListView = (LinearLayout) findViewById(R.id.mode_list);
        for (int i = 0; i < mTotalModes; i++) {
            final ModeSelectorItem selectorItem =
                    (ModeSelectorItem) inflater.inflate(R.layout.mode_selector, null);
            mListView.addView(selectorItem);
            // Sets the top padding of the top item to 0.
            if (i == 0) {
                selectorItem.setPadding(selectorItem.getPaddingLeft(), 0,
                        selectorItem.getPaddingRight(), selectorItem.getPaddingBottom());
            }
            // Sets the bottom padding of the bottom item to 0.
            if (i == mTotalModes - 1) {
                selectorItem.setPadding(selectorItem.getPaddingLeft(), selectorItem.getPaddingTop(),
                        selectorItem.getPaddingRight(), 0);
            }

            int modeId = getModeIndex(i);
            selectorItem.setHighlightColor(getResources()
                    .getColor(Util.getCameraThemeColorId(modeId, getContext())));

            // Set image
            selectorItem.setImageResource(Util.getCameraModeIconResId(modeId, getContext()));

            // Set text
            selectorItem.setText(Util.getCameraModeText(modeId, getContext()));

            // Set content description (for a11y)
            selectorItem.setContentDescription(Util
                    .getCameraModeContentDescription(modeId, getContext()));
            selectorItem.setModeId(modeId);
            selectorItem.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemSelected(selectorItem);
                }
            });

            mModeSelectorItems[i] = selectorItem;
        }
        // During drawer opening/closing, we change the visible width of the mode
        // items in sequence, so we listen to the last item's visible width change
        // for a good timing to do corresponding UI adjustments.
//        mModeSelectorItems[mTotalModes - 1].setVisibleWidthChangedListener(this);
//        resetModeSelectors();
    }

    private int getModeIndex(int modeSelectorIndex) {
        if (modeSelectorIndex < mTotalModes && modeSelectorIndex >= 0) {
            return mSupportedModes.get(modeSelectorIndex);
        }
        Log.e(TAG, "Invalid mode selector index: " + modeSelectorIndex + ", total modes: " +
                mTotalModes);
        return getResources().getInteger(R.integer.camera_mode_photo);
    }

    private void onItemSelected(ModeSelectorItem selectedItem) {
        int modeId = selectedItem.getModeId();
//        mModeSwitchListener.onModeButtonPressed(modeId);
//        mCurrentStateManager.getCurrentState().onItemSelected(selectedItem);
    }



    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mWidth = right - left;
        mHeight = bottom - top - getPaddingTop() - getPaddingBottom();

        updateModeListLayout();

//        if (mCurrentStateManager.getCurrentState().getCurrentAnimationEffects() != null) {
//            mCurrentStateManager.getCurrentState().getCurrentAnimationEffects().setSize(
//                    mWidth, mHeight);
//        }
    }

    private void updateModeListLayout() {
        if (mCaptureLayoutHelper == null) {
            Log.e(TAG, "Capture layout helper needs to be set first.");
            return;
        }
        // Center mode drawer in the portion of camera preview that is not covered by
        // bottom bar.
        RectF uncoveredPreviewArea = mCaptureLayoutHelper.getUncoveredPreviewRect();
        Log.d("kk", " mode list view " + uncoveredPreviewArea.toShortString());
        // Align left:
        mListView.setTranslationX(uncoveredPreviewArea.left);
        // Align center vertical:
        mListView.setTranslationY(uncoveredPreviewArea.centerY()
                - mListView.getMeasuredHeight() / 2);

    }


}
