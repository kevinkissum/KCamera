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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.SparseBooleanArray;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.example.kevin.kcamera.AndroidServices;
import com.example.kevin.kcamera.AnimationEffects;
import com.example.kevin.kcamera.CameraUtil;
import com.example.kevin.kcamera.CaptureLayoutHelper;
import com.example.kevin.kcamera.Gusterpolator;
import com.example.kevin.kcamera.R;

import java.util.ArrayList;
import java.util.LinkedList;


public class ModeListView extends FrameLayout implements ModeSelectorItem.VisibleWidthChangedListener {

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
    private CurrentStateManager mCurrentStateManager = new CurrentStateManager();
    private ModeListOpenListener mModeListOpenListener;
    private GestureDetector mGestureDetector;
    private final LinkedList<TimeBasedPosition> mPositionHistory
            = new LinkedList<TimeBasedPosition>();
    private ModeListVisibilityChangedListener mVisibilityChangedListener;

    public interface ModeListOpenListener {
        /**
         * Mode list will open to full screen after current animation.
         */
        public void onOpenFullScreen();

        /**
         * Updates the listener with the current progress of mode drawer opening.
         *
         * @param progress progress of the mode drawer opening, ranging [0f, 1f]
         *                 0 means mode drawer is fully closed, 1 indicates a fully
         *                 open mode drawer.
         */
        public void onModeListOpenProgress(float progress);

        /**
         * Gets called when mode list is completely closed.
         */
        public void onModeListClosed();
    }

    private final GestureDetector.OnGestureListener mOnGestureListener
            = new GestureDetector.SimpleOnGestureListener(){
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            Log.d("kk", " mOnGestureListener onScroll  ");

            mCurrentStateManager.getCurrentState().onScroll(e1, e2, distanceX, distanceY);
            mLastScrollTime = System.currentTimeMillis();
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
            mCurrentStateManager.getCurrentState().onSingleTapUp(ev);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            // Cache velocity in the unit pixel/ms.
            mVelocityX = velocityX / 1000f * SCROLL_FACTOR;
            mCurrentStateManager.getCurrentState().onFling(e1, e2, velocityX, velocityY);
            return true;
        }

        @Override
        public boolean onDown(MotionEvent ev) {
            mVelocityX = 0;
            mCurrentStateManager.getCurrentState().onDown(ev);
            return true;
        }
    };

    public ModeListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mGestureDetector = new GestureDetector(context, mOnGestureListener);
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
        onModeListOpenRatioUpdate(0);
        if (mCurrentStateManager.getCurrentState() == null) {
            mCurrentStateManager.setCurrentState(new FullyHiddenState());
        }

    }

    public void showModeSwitcherHint() {
        // 隐藏modeListView, 只有FullyHiddenState进行了实现， 而一开始默认就是FullyHiddenState
        mCurrentStateManager.getCurrentState().showSwitcherHint();
    }

    @Override
    public void setVisibility(int visibility) {
        ModeListState currentState = mCurrentStateManager.getCurrentState();
        if (currentState != null && !currentState.shouldHandleVisibilityChange(visibility)) {
            return;
        }
        super.setVisibility(visibility);
    }

    public void setVisibilityChangedListener(ModeListVisibilityChangedListener listener) {
        mVisibilityChangedListener = listener;
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
                    .getColor(CameraUtil.getCameraThemeColorId(modeId, getContext())));

            // Set image
            selectorItem.setImageResource(CameraUtil.getCameraModeIconResId(modeId, getContext()));

            // Set text
            Log.d("kk", "  setText " + CameraUtil.getCameraModeText(modeId, getContext()) );
            selectorItem.setText(CameraUtil.getCameraModeText(modeId, getContext()));

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
        mModeSelectorItems[mTotalModes - 1].setVisibleWidthChangedListener(this);
        resetModeSelectors();
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

        if (mCurrentStateManager.getCurrentState().getCurrentAnimationEffects() != null) {
            mCurrentStateManager.getCurrentState().getCurrentAnimationEffects().setSize(
                    mWidth, mHeight);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        Log.d("kk", " ModeListView onTouchEvent   ");

        // Reset touch forward recipient
        if (MotionEvent.ACTION_DOWN == ev.getActionMasked()) {
            mChildViewTouched = null;
        }

        if (!mCurrentStateManager.getCurrentState().shouldHandleTouchEvent(ev)) {
            return false;
        }
        getParent().requestDisallowInterceptTouchEvent(true);
        super.onTouchEvent(ev);

        mGestureDetector.onTouchEvent(ev);
        mCurrentStateManager.getCurrentState().onTouchEvent(ev);
        return true;
    }



    private void updateModeListLayout() {
        if (mCaptureLayoutHelper == null) {
            Log.e(TAG, "Capture layout helper needs to be set first.");
            return;
        }
        RectF uncoveredPreviewArea = mCaptureLayoutHelper.getUncoveredPreviewRect();
//        Log.d("kk", " mode list view " + uncoveredPreviewArea.toShortString());
        // Align left:
        mListView.setTranslationX(uncoveredPreviewArea.left);
        // Align center vertical:
        mListView.setTranslationY(uncoveredPreviewArea.centerY()
                - mListView.getMeasuredHeight() / 2);

    }

    private void reset() {
        resetModeSelectors();
        mScrollTrendX = 0f;
        mScrollTrendY = 0f;
        setVisibility(INVISIBLE);
    }

    private void resetModeSelectors() {
        for (int i = 0; i < mModeSelectorItems.length; i++) {
            mModeSelectorItems[i].setVisibleWidth(0);
        }
    }

    public void setModeListOpenListener(ModeListOpenListener listener) {
        mModeListOpenListener = listener;
    }

    private Animator snapToFullScreen() {
        Animator animator;
        int focusItem = mFocusItem == NO_ITEM_SELECTED ? 0 : mFocusItem;
        int fullWidth = mModeSelectorItems[focusItem].getMaxVisibleWidth();
        Log.d("kk", " >>>> mVelocityX " + mVelocityX);

        if (mVelocityX <= VELOCITY_THRESHOLD) {
            animator = animateListToWidth(fullWidth);
        } else {
            // If the fling velocity exceeds this threshold, snap to full screen
            // at a constant speed.
            animator = animateListToWidthAtVelocity(VELOCITY_THRESHOLD, fullWidth);
        }
        if (mModeListOpenListener != null) {
            mModeListOpenListener.onOpenFullScreen();
        }
        return animator;
    }


    @Override
    public void onVisibleWidthChanged(int visibleWidth) {
        mVisibleWidth = visibleWidth;

        // When the longest mode item is entirely shown (across the screen), the
        // background should be 50% transparent.
        int maxVisibleWidth = mModeSelectorItems[0].getMaxVisibleWidth();
        visibleWidth = Math.min(maxVisibleWidth, visibleWidth);
        if (visibleWidth != maxVisibleWidth) {
            // No longer full screen.
            cancelForwardingTouchEvent();
        }
        float openRatio = (float) visibleWidth / maxVisibleWidth;
        onModeListOpenRatioUpdate(openRatio * mModeListOpenFactor);
    }

    /**
     * Gets called when UI elements such as background and gear icon need to adjust
     * their appearance based on the percentage of the mode list opening.
     *
     * @param openRatio percentage of the mode list opening, ranging [0f, 1f]
     */
    private void onModeListOpenRatioUpdate(float openRatio) {
        for (int i = 0; i < mModeSelectorItems.length; i++) {
            mModeSelectorItems[i].setTextAlpha(openRatio);
        }
        setBackgroundAlpha((int) (BACKGROUND_TRANSPARENTCY * openRatio));
        if (mModeListOpenListener != null) {
            mModeListOpenListener.onModeListOpenProgress(openRatio);
        }
    }

    /**
     * Sets the alpha on the list background. This is called whenever the list
     * is scrolling or animating, so that background can adjust its dimness.
     *
     * @param alpha new alpha to be applied on list background color
     */
    private void setBackgroundAlpha(int alpha) {
        // Make sure alpha is valid.
        alpha = alpha & 0xFF;
        // Change alpha on the background color.
        mListBackgroundColor = mListBackgroundColor & 0xFFFFFF;
        mListBackgroundColor = mListBackgroundColor | (alpha << 24);
        // Set new color to list background.
        setBackgroundColor(mListBackgroundColor);
    }

    /**
     * Cancels the touch event forwarding by sending a cancel event to the recipient
     * view and resetting the touch forward recipient to ensure no more events
     * can be forwarded in the current series of the touch events.
     */
    private void cancelForwardingTouchEvent() {
        if (mChildViewTouched != null) {
            mLastChildTouchEvent.setAction(MotionEvent.ACTION_CANCEL);
            mChildViewTouched.onTouchEvent(mLastChildTouchEvent);
            mChildViewTouched = null;
        }
    }

    private Animator snapBack() {
        return snapBack(true);
    }

    public Animator snapBack(boolean withAnimation) {
        if (withAnimation) {
            if (mVelocityX > -VELOCITY_THRESHOLD * SCROLL_FACTOR) {
                return animateListToWidth(0);
            } else {
                return animateListToWidthAtVelocity(mVelocityX, 0);
            }
        } else {
            setVisibility(INVISIBLE);
            resetModeSelectors();
            return null;
        }
    }

    private Animator animateListToWidth(int delay, int duration,
                                        TimeInterpolator interpolator, int... width) {
        if (mAnimatorSet != null && mAnimatorSet.isRunning()) {
            mAnimatorSet.end();
        }

        ArrayList<Animator> animators = new ArrayList<Animator>();
        boolean animateModeItemsInOrder = true;
        if (delay < 0) {
            animateModeItemsInOrder = false;
            delay *= -1;
        }
        for (int i = 0; i < mTotalModes; i++) {
            ObjectAnimator animator;
            if (animateModeItemsInOrder) {
                animator = ObjectAnimator.ofInt(mModeSelectorItems[i],
                        "visibleWidth", width);
            } else {
                animator = ObjectAnimator.ofInt(mModeSelectorItems[mTotalModes - 1 -i],
                        "visibleWidth", width);
            }
            animator.setDuration(duration);
            animators.add(animator);
        }

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animators);
        mAnimatorSet.setInterpolator(interpolator);
        mAnimatorSet.start();

        return mAnimatorSet;
    }

    private Animator animateListToWidthAtVelocity(float velocity, int width) {
        if (mAnimatorSet != null && mAnimatorSet.isRunning()) {
            mAnimatorSet.end();
        }

        ArrayList<Animator> animators = new ArrayList<Animator>();
        for (int i = 0; i < mTotalModes; i++) {
            ObjectAnimator animator = ObjectAnimator.ofInt(mModeSelectorItems[i],
                    "visibleWidth", width);
            int duration = (int) (width / velocity);
            animator.setDuration(duration);
            animators.add(animator);
        }

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animators);
        mAnimatorSet.setInterpolator(null);
        mAnimatorSet.start();

        return mAnimatorSet;
    }


    private Animator animateListToWidth(int... width) {
        return animateListToWidth(0, DEFAULT_DURATION_MS, null, width);
    }

    private void scroll(int itemId, float deltaX, float deltaY) {
        // Scrolling trend on X and Y axis, to track the trend by biasing
        // towards latest touch events.
        mScrollTrendX = mScrollTrendX * 0.3f + deltaX * 0.7f;
        mScrollTrendY = mScrollTrendY * 0.3f + deltaY * 0.7f;

        // TODO: Change how the curve is calculated below when UX finalize their design.
        mCurrentTime = SystemClock.uptimeMillis();
        float longestWidth;
        if (itemId != NO_ITEM_SELECTED) {
            longestWidth = mModeSelectorItems[itemId].getVisibleWidth();
        } else {
            longestWidth = mModeSelectorItems[0].getVisibleWidth();
        }
        float newPosition = longestWidth - deltaX;
        int maxVisibleWidth = mModeSelectorItems[0].getMaxVisibleWidth();
        newPosition = Math.min(newPosition, getMaxMovementBasedOnPosition((int) longestWidth,
                maxVisibleWidth));
        newPosition = Math.max(newPosition, 0);
        insertNewPosition(newPosition, mCurrentTime);

        for (int i = 0; i < mModeSelectorItems.length; i++) {
            mModeSelectorItems[i].setVisibleWidth((int) newPosition);
        }
    }

    private void insertNewPosition(float position, long time) {
        // TODO: Consider re-using stale position objects rather than
        // always creating new position objects.
        mPositionHistory.add(new TimeBasedPosition(position, time));

        // Positions that are from too long ago will not be of any use for
        // future position interpolation. So we need to remove those positions
        // from the list.
        long timeCutoff = time - (mTotalModes - 1) * DELAY_MS;
        while (mPositionHistory.size() > 0) {
            // Remove all the position items that are prior to the cutoff time.
            TimeBasedPosition historyPosition = mPositionHistory.getFirst();
            if (historyPosition.getTimeStamp() < timeCutoff) {
                mPositionHistory.removeFirst();
            } else {
                break;
            }
        }
    }

    public float getMaxMovementBasedOnPosition(int lastVisibleWidth, int maxWidth) {
        int timeElapsed = (int) (System.currentTimeMillis() - mLastScrollTime);
        if (timeElapsed > SCROLL_INTERVAL_MS) {
            timeElapsed = SCROLL_INTERVAL_MS;
        }
        float position;
        int slowZone = (int) (maxWidth * SLOW_ZONE_PERCENTAGE);
        if (lastVisibleWidth < (maxWidth - slowZone)) {
            position = VELOCITY_THRESHOLD * timeElapsed + lastVisibleWidth;
        } else {
            float percentageIntoSlowZone = (lastVisibleWidth - (maxWidth - slowZone)) / slowZone;
            float velocity = (1 - percentageIntoSlowZone) * VELOCITY_THRESHOLD;
            position = velocity * timeElapsed + lastVisibleWidth;
        }
        position = Math.min(maxWidth, position);
        return position;
    }

    private boolean isTouchInsideList(MotionEvent ev) {
        // Ignore the tap if it happens outside of the mode list linear layout.
        float x = ev.getX() - mListView.getX();
        float y = ev.getY() - mListView.getY();
        if (x < 0 || x > mListView.getWidth() || y < 0 || y > mListView.getHeight()) {
            return false;
        }
        return true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Log.d("kk", " onWindowFocusChanged " + hasFocus );
        mCurrentStateManager.getCurrentState().onWindowFocusChanged(hasFocus);
    }

    /**
     * Forward touch events to a recipient child view. Before feeding the motion
     * event into the child view, the event needs to be converted in child view's
     * coordinates.
     */
    private void forwardTouchEventToChild(MotionEvent ev) {
        if (mChildViewTouched != null) {
            float x = ev.getX() - mListView.getX();
            float y = ev.getY() - mListView.getY();
            x -= mChildViewTouched.getLeft();
            y -= mChildViewTouched.getTop();

            mLastChildTouchEvent = MotionEvent.obtain(ev);
            mLastChildTouchEvent.setLocation(x, y);
            mChildViewTouched.onTouchEvent(mLastChildTouchEvent);
        }
    }

    private static class TimeBasedPosition {
        private final float mPosition;
        private final long mTimeStamp;
        public TimeBasedPosition(float position, long time) {
            mPosition = position;
            mTimeStamp = time;
        }

        public float getPosition() {
            return mPosition;
        }

        public long getTimeStamp() {
            return mTimeStamp;
        }
    }

    private class CurrentStateManager {
        private ModeListState mCurrentState;

        ModeListState getCurrentState() {
            return mCurrentState;
        }

        void setCurrentState(ModeListState state) {
            mCurrentState = state;
            state.onCurrentState();
        }
    }

    private int getFocusItem(float x, float y) {
        // Convert coordinates into child view's coordinates.
        x -= mListView.getX();
        y -= mListView.getY();

        for (int i = 0; i < mModeSelectorItems.length; i++) {
            if (y <= mModeSelectorItems[i].getBottom()) {
                return i;
            }
        }
        return mModeSelectorItems.length - 1;
    }

    private void setSwipeMode(boolean swipeIn) {
        for (int i = 0 ; i < mModeSelectorItems.length; i++) {
            mModeSelectorItems[i].onSwipeModeChanged(swipeIn);
        }
    }

    private boolean shouldSnapBack() {
        int itemId = Math.max(0, mFocusItem);
        if (Math.abs(mVelocityX) > VELOCITY_THRESHOLD) {
            // Fling to open / close
            return mVelocityX < 0;
        } else if (mModeSelectorItems[itemId].getVisibleWidth()
                < mModeSelectorItems[itemId].getMaxVisibleWidth() * SNAP_BACK_THRESHOLD_RATIO) {
            return true;
        } else if (Math.abs(mScrollTrendX) > Math.abs(mScrollTrendY) && mScrollTrendX > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * ModeListState defines a set of functions through which the view could manage
     * or change the states. Sub-classes could selectively override these functions
     * accordingly to respect the specific requirements for each state. By overriding
     * these methods, state transition can also be achieved.
     */
    private abstract class ModeListState implements GestureDetector.OnGestureListener {
        protected AnimationEffects mCurrentAnimationEffects = null;

        /**
         * Called by the state manager when this state instance becomes the current
         * mode list state.
         */
        public void onCurrentState() {
            // Do nothing.
        }

        /**
         * If supported, this should show the mode switcher and starts the accordion
         * animation with a delay. If the view does not currently have focus, (e.g.
         * There are popups on top of it.) start the delayed accordion animation
         * when it gains focus. Otherwise, start the animation with a delay right
         * away.
         */
        public void showSwitcherHint() {
            // Do nothing.
        }

        /**
         * Gets the currently running animation effects for the current state.
         */
        public AnimationEffects getCurrentAnimationEffects() {
            return mCurrentAnimationEffects;
        }

        /**
         * Returns true if the touch event should be handled, false otherwise.
         *
         * @param ev motion event to be handled
         * @return true if the event should be handled, false otherwise.
         */
        public boolean shouldHandleTouchEvent(MotionEvent ev) {
            return true;
        }

        /**
         * Handles touch event. This will be called if
         * {@link ModeListState#shouldHandleTouchEvent(android.view.MotionEvent)}
         * returns {@code true}
         *
         * @param ev touch event to be handled
         * @return always true
         */
        public boolean onTouchEvent(MotionEvent ev) {
            return true;
        }

        /**
         * Gets called when the window focus has changed.
         *
         * @param hasFocus whether current window has focus
         */
        public void onWindowFocusChanged(boolean hasFocus) {
            // Default to do nothing.
        }

        /**
         * Gets called when back key is pressed.
         *
         * @return true if handled, false otherwise.
         */
        public boolean onBackPressed() {
            return false;
        }

        /**
         * Gets called when menu key is pressed.
         *
         * @return true if handled, false otherwise.
         */
        public boolean onMenuPressed() {
            return false;
        }

        /**
         * Gets called when there is a {@link View#setVisibility(int)} call to
         * change the visibility of the mode drawer. Visibility change does not
         * always make sense, for example there can be an outside call to make
         * the mode drawer visible when it is in the fully hidden state. The logic
         * is that the mode drawer can only be made visible when user swipe it in.
         *
         * @param visibility the proposed visibility change
         * @return true if the visibility change is valid and therefore should be
         *         handled, false otherwise.
         */
        public boolean shouldHandleVisibilityChange(int visibility) {
            return true;
        }

        /**
         * If supported, this should start blurring the camera preview and
         * start the mode switch.
         *
         * @param selectedItem mode item that has been selected
         */
        public void onItemSelected(ModeSelectorItem selectedItem) {
            // Do nothing.
        }

        /**
         * This gets called when mode switch has finished and UI needs to
         * pinhole into the new mode through animation.
         */
        public void startModeSelectionAnimation() {
            // Do nothing.
        }

        /**
         * Hide the mode drawer and switch to fully hidden state.
         */
        public void hide() {
            // Do nothing.
        }

        /**
         * Hide the mode drawer (with animation, if supported)
         * and switch to fully hidden state.
         * Default is to simply call {@link #hide()}.
         */
        public void hideAnimated() {
            hide();
        }

        /***************GestureListener implementation*****************/
        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            // Do nothing.
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            // Do nothing.
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }

    /**
     * Fully hidden state. Transitioning to ScrollingState and ShimmyState are supported
     * in this state.
     */
    private class FullyHiddenState extends ModeListState {
        private Animator mAnimator = null;
        private boolean mShouldBeVisible = false;

        public FullyHiddenState() {
            reset();
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d("kk", " FullyHiddenState " );
            mShouldBeVisible = true;
            // Change visibility, and switch to scrolling state.
            resetModeSelectors();
            mCurrentStateManager.setCurrentState(new ScrollingState());
            return true;
        }

        @Override
        public void showSwitcherHint() {
            mShouldBeVisible = true;
            mCurrentStateManager.setCurrentState(new ShimmyState());
        }

        @Override
        public boolean shouldHandleTouchEvent(MotionEvent ev) {
            return true;
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
                mFocusItem = getFocusItem(ev.getX(), ev.getY());
                setSwipeMode(true);
            }
            return true;
        }

        @Override
        public boolean shouldHandleVisibilityChange(int visibility) {
            if (mAnimator != null) {
                return false;
            }
            if (visibility == VISIBLE && !mShouldBeVisible) {
                return false;
            }
            return true;
        }


    }

    private class ScrollingState extends ModeListState {
        private Animator mAnimator = null;

        public ScrollingState() {
            setVisibility(VISIBLE);
        }


        /**
         * onScroll 和 onFiling 区别
         * scroll 是 滑动就一直触发
         *
         * onFling 是 手松开瞬间触发
         */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // Scroll based on the scrolling distance on the currently focused
            // item.
            // 通过改过 width值， 调用invalidate， ondraw重新绘制，达到动画的效果。
            scroll(mFocusItem, distanceX * SCROLL_FACTOR,
                    distanceY * SCROLL_FACTOR);
            return true;
        }

        @Override
        public boolean shouldHandleTouchEvent(MotionEvent ev) {
            // If the snap back/to full screen animation is on going, ignore any
            // touch.
            if (mAnimator != null) {
                return false;
            }
            return true;
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (ev.getActionMasked() == MotionEvent.ACTION_UP ||
                    ev.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                // 根据滑动距离判断是否显示还是隐藏
                final boolean shouldSnapBack = shouldSnapBack();
                if (shouldSnapBack) {
                    mAnimator = snapBack();
                } else {
                    mAnimator = snapToFullScreen();
                }
                mAnimator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mAnimator = null;
                        mFocusItem = NO_ITEM_SELECTED;
                        if (shouldSnapBack) {
                            mCurrentStateManager.setCurrentState(new FullyHiddenState());
                        } else {
                            mCurrentStateManager.setCurrentState(new FullyShownState());
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
            }
            return true;
        }
    }

    private class FullyShownState extends ModeListState {
        private Animator mAnimator = null;

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // Go to scrolling state.
            if (distanceX > 0) {
                // Swipe out
                cancelForwardingTouchEvent();
                mCurrentStateManager.setCurrentState(new ScrollingState());
            }
            return true;
        }

        @Override
        public boolean shouldHandleTouchEvent(MotionEvent ev) {
            if (mAnimator != null && mAnimator.isRunning()) {
                return false;
            }
            return true;
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
                mFocusItem = NO_ITEM_SELECTED;
                setSwipeMode(false);
                // If the down event happens inside the mode list, find out which
                // mode item is being touched and forward all the subsequent touch
                // events to that mode item for its pressed state and click handling.
                if (isTouchInsideList(ev)) {
                    mChildViewTouched = mModeSelectorItems[getFocusItem(ev.getX(), ev.getY())];
                }
            }
            forwardTouchEventToChild(ev);
            return true;
        }


        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
            // If the tap is not inside the mode drawer area, snap back.
            if(!isTouchInsideList(ev)) {
                snapBackAndHide();
                return false;
            }
            return true;
        }

        @Override
        public boolean onBackPressed() {
            snapBackAndHide();
            return true;
        }


        @Override
        public void onItemSelected(ModeSelectorItem selectedItem) {
            mCurrentStateManager.setCurrentState(new SelectedState(selectedItem));
        }

        /**
         * Snaps back the mode list and go to the fully hidden state.
         */
        private void snapBackAndHide() {
            mAnimator = snapBack(true);
            if (mAnimator != null) {
                mAnimator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mAnimator = null;
                        mCurrentStateManager.setCurrentState(new FullyHiddenState());
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
            } else {
                mCurrentStateManager.setCurrentState(new FullyHiddenState());
            }
        }

        @Override
        public void hide() {
            if (mAnimator != null) {
                mAnimator.cancel();
            } else {
                mCurrentStateManager.setCurrentState(new FullyHiddenState());
            }
        }

    }

    private class SelectedState extends ModeListState {
        public SelectedState(ModeSelectorItem selectedItem) {
            final int modeId = selectedItem.getModeId();
            // Un-highlight all the modes.
            for (int i = 0; i < mModeSelectorItems.length; i++) {
                mModeSelectorItems[i].setSelected(false);
            }

            PeepholeAnimationEffect effect = new PeepholeAnimationEffect();
            effect.setSize(mWidth, mHeight);

            // Calculate the position of the icon in the selected item, and
            // start animation from that position.
            int[] location = new int[2];
            // Gets icon's center position in relative to the window.
            selectedItem.getIconCenterLocationInWindow(location);
            int iconX = location[0];
            int iconY = location[1];
            // Gets current view's top left position relative to the window.
            getLocationInWindow(location);
            // Calculate icon location relative to this view
            iconX -= location[0];
            iconY -= location[1];

            effect.setAnimationStartingPosition(iconX, iconY);
            effect.setModeSpecificColor(selectedItem.getHighlightColor());
//            if (mScreenShotProvider != null) {
//                effect.setBackground(mScreenShotProvider
//                                .getPreviewFrame(PREVIEW_DOWN_SAMPLE_FACTOR),
//                        mCaptureLayoutHelper.getPreviewRect());
//                effect.setBackgroundOverlay(mScreenShotProvider.getPreviewOverlayAndControls());
//            }
            mCurrentAnimationEffects = effect;
            effect.startFadeoutAnimation(null, selectedItem, iconX, iconY, modeId);
            invalidate();
        }

        @Override
        public boolean shouldHandleTouchEvent(MotionEvent ev) {
            return false;
        }

        @Override
        public void startModeSelectionAnimation() {
            mCurrentAnimationEffects.startAnimation(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mCurrentAnimationEffects = null;
                    mCurrentStateManager.setCurrentState(new FullyHiddenState());
                }
            });
        }

        @Override
        public void hide() {
            if (!mCurrentAnimationEffects.cancelAnimation()) {
                mCurrentAnimationEffects = null;
                mCurrentStateManager.setCurrentState(new FullyHiddenState());
            }
        }
    }

    private class PeepholeAnimationEffect extends AnimationEffects {

        private final static int UNSET = -1;
        private final static int PEEP_HOLE_ANIMATION_DURATION_MS = 500;

        private final Paint mMaskPaint = new Paint();
        private final RectF mBackgroundDrawArea = new RectF();

        private int mPeepHoleCenterX = UNSET;
        private int mPeepHoleCenterY = UNSET;
        private float mRadius = 0f;
        private ValueAnimator mPeepHoleAnimator;
        private ValueAnimator mFadeOutAlphaAnimator;
        private ValueAnimator mRevealAlphaAnimator;
        private Bitmap mBackground;
        private Bitmap mBackgroundOverlay;

        private Paint mCirclePaint = new Paint();
        private Paint mCoverPaint = new Paint();

//        private TouchCircleDrawable mCircleDrawable;

        public PeepholeAnimationEffect() {
            mMaskPaint.setAlpha(0);
            mMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

            mCirclePaint.setColor(0);
            mCirclePaint.setAlpha(0);

            mCoverPaint.setColor(0);
            mCoverPaint.setAlpha(0);

            setupAnimators();
        }

        private void setupAnimators() {
            mFadeOutAlphaAnimator = ValueAnimator.ofInt(0, 255);
            mFadeOutAlphaAnimator.setDuration(100);
//            mFadeOutAlphaAnimator.setInterpolator(Gusterpolator.INSTANCE);
            mFadeOutAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mCoverPaint.setAlpha((Integer) animation.getAnimatedValue());
                    invalidate();
                }
            });
            mFadeOutAlphaAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    // Sets a HW layer on the view for the animation.
                    setLayerType(LAYER_TYPE_HARDWARE, null);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    // Sets the layer type back to NONE as a workaround for b/12594617.
                    setLayerType(LAYER_TYPE_NONE, null);
                }
            });

            /////////////////

            mRevealAlphaAnimator = ValueAnimator.ofInt(255, 0);
            mRevealAlphaAnimator.setDuration(PEEP_HOLE_ANIMATION_DURATION_MS);
//            mRevealAlphaAnimator.setInterpolator(Gusterpolator.INSTANCE);
            mRevealAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int alpha = (Integer) animation.getAnimatedValue();
                    mCirclePaint.setAlpha(alpha);
                    mCoverPaint.setAlpha(alpha);
                }
            });
            mRevealAlphaAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    // Sets a HW layer on the view for the animation.
                    setLayerType(LAYER_TYPE_HARDWARE, null);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    // Sets the layer type back to NONE as a workaround for b/12594617.
                    setLayerType(LAYER_TYPE_NONE, null);
                }
            });

            ////////////////

            int horizontalDistanceToFarEdge = Math.max(mPeepHoleCenterX, mWidth - mPeepHoleCenterX);
            int verticalDistanceToFarEdge = Math.max(mPeepHoleCenterY, mHeight - mPeepHoleCenterY);
            int endRadius = (int) (Math.sqrt(horizontalDistanceToFarEdge * horizontalDistanceToFarEdge
                    + verticalDistanceToFarEdge * verticalDistanceToFarEdge));
            int startRadius = getResources().getDimensionPixelSize(
                    R.dimen.mode_selector_icon_block_width) / 2;

            mPeepHoleAnimator = ValueAnimator.ofFloat(startRadius, endRadius);
            mPeepHoleAnimator.setDuration(PEEP_HOLE_ANIMATION_DURATION_MS);
//            mPeepHoleAnimator.setInterpolator(Gusterpolator.INSTANCE);
            mPeepHoleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    // Modify mask by enlarging the hole
                    mRadius = (Float) mPeepHoleAnimator.getAnimatedValue();
                    invalidate();
                }
            });
            mPeepHoleAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    // Sets a HW layer on the view for the animation.
                    setLayerType(LAYER_TYPE_HARDWARE, null);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    // Sets the layer type back to NONE as a workaround for b/12594617.
                    setLayerType(LAYER_TYPE_NONE, null);
                }
            });

            ////////////////
            int size = getContext().getResources()
                    .getDimensionPixelSize(R.dimen.mode_selector_icon_block_width);
//            mCircleDrawable = new TouchCircleDrawable(getContext().getResources());
//            mCircleDrawable.setSize(size, size);
//            mCircleDrawable.setUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//                @Override
//                public void onAnimationUpdate(ValueAnimator animation) {
//                    invalidate();
//                }
//            });
        }

        @Override
        public void setSize(int width, int height) {
            mWidth = width;
            mHeight = height;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return true;
        }

        @Override
        public void drawForeground(Canvas canvas) {
            // Draw the circle in clear mode
            if (mPeepHoleAnimator != null) {
                // Draw a transparent circle using clear mode
                canvas.drawCircle(mPeepHoleCenterX, mPeepHoleCenterY, mRadius, mMaskPaint);
                canvas.drawCircle(mPeepHoleCenterX, mPeepHoleCenterY, mRadius, mCirclePaint);
            }
        }

        public void setAnimationStartingPosition(int x, int y) {
            mPeepHoleCenterX = x;
            mPeepHoleCenterY = y;
        }

        public void setModeSpecificColor(int color) {
            mCirclePaint.setColor(color & 0x00ffffff);
        }

        /**
         * Sets the bitmap to be drawn in the background and the drawArea to draw
         * the bitmap.
         *
         * @param background image to be drawn in the background
         * @param drawArea area to draw the background image
         */
        public void setBackground(Bitmap background, RectF drawArea) {
            mBackground = background;
            mBackgroundDrawArea.set(drawArea);
        }

        /**
         * Sets the overlay image to be drawn on top of the background.
         */
        public void setBackgroundOverlay(Bitmap overlay) {
            mBackgroundOverlay = overlay;
        }

        @Override
        public void drawBackground(Canvas canvas) {
            if (mBackground != null && mBackgroundOverlay != null) {
                canvas.drawBitmap(mBackground, null, mBackgroundDrawArea, null);
                canvas.drawPaint(mCoverPaint);
                canvas.drawBitmap(mBackgroundOverlay, 0, 0, null);

//                if (mCircleDrawable != null) {
//                    mCircleDrawable.draw(canvas);
//                }
            }
        }

        @Override
        public boolean shouldDrawSuper() {
            // No need to draw super when mBackgroundOverlay is being drawn, as
            // background overlay already contains what's drawn in super.
            return (mBackground == null || mBackgroundOverlay == null);
        }

        public void startFadeoutAnimation(Animator.AnimatorListener listener,
                                          final ModeSelectorItem selectedItem,
                                          int x, int y, final int modeId) {
            mCoverPaint.setColor(0);
            mCoverPaint.setAlpha(0);

//            mCircleDrawable.setIconDrawable(
//                    selectedItem.getIcon().getIconDrawableClone(),
//                    selectedItem.getIcon().getIconDrawableSize());
//            mCircleDrawable.setCenter(new Point(x, y));
//            mCircleDrawable.setColor(selectedItem.getHighlightColor());
//            mCircleDrawable.setAnimatorListener(new AnimatorListenerAdapter() {
//                @Override
//                public void onAnimationEnd(Animator animation) {
//                    // Post mode selection runnable to the end of the message queue
//                    // so that current UI changes can finish before mode initialization
//                    // clogs up UI thread.
//                    post(new Runnable() {
//                        @Override
//                        public void run() {
//                            // Select the focused item.
//                            selectedItem.setSelected(true);
//                            onModeSelected(modeId);
//                        }
//                    });
//                }
//            });

            // add fade out animator to a set, so we can freely add
            // the listener without having to worry about listener dupes
            AnimatorSet s = new AnimatorSet();
            s.play(mFadeOutAlphaAnimator);
            if (listener != null) {
                s.addListener(listener);
            }
//            mCircleDrawable.animate();
            s.start();
        }

        @Override
        public void startAnimation(Animator.AnimatorListener listener) {
            if (mPeepHoleAnimator != null && mPeepHoleAnimator.isRunning()) {
                return;
            }
            if (mPeepHoleCenterY == UNSET || mPeepHoleCenterX == UNSET) {
                mPeepHoleCenterX = mWidth / 2;
                mPeepHoleCenterY = mHeight / 2;
            }

            mCirclePaint.setAlpha(255);
            mCoverPaint.setAlpha(255);

            // add peephole and reveal animators to a set, so we can
            // freely add the listener without having to worry about
            // listener dupes
            AnimatorSet s = new AnimatorSet();
            s.play(mPeepHoleAnimator).with(mRevealAlphaAnimator);
            if (listener != null) {
                s.addListener(listener);
            }
            s.start();
        }

        @Override
        public void endAnimation() {
        }

        @Override
        public boolean cancelAnimation() {
            if (mPeepHoleAnimator == null || !mPeepHoleAnimator.isRunning()) {
                return false;
            } else {
                mPeepHoleAnimator.cancel();
                return true;
            }
        }
    }

    private class ShimmyState extends ModeListState {
        private boolean mStartHidingShimmyWhenWindowGainsFocus = false;
        private Animator mAnimator = null;
        private final Runnable mHideShimmy = new Runnable() {
            @Override
            public void run() {
                startHidingShimmy();
            }
        };

        public ShimmyState() {
            setVisibility(VISIBLE);
            // 该值控制字体的显示或隐藏 0 隐藏 1 显示
            mModeListOpenFactor = 0f;
            onModeListOpenRatioUpdate(0);
            int maxVisibleWidth = mModeSelectorItems[0].getMaxVisibleWidth();
            for (int i = 0; i < mModeSelectorItems.length; i++) {
                mModeSelectorItems[i].setVisibleWidth(maxVisibleWidth);
            }
            if (hasWindowFocus()) {
                hideShimmyWithDelay();
            } else {
                mStartHidingShimmyWhenWindowGainsFocus = true;
            }
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // Scroll happens during accordion animation.
            cancelAnimation();
            cancelForwardingTouchEvent();
            // Go to scrolling state
            mCurrentStateManager.setCurrentState(new ScrollingState());
            return true;
        }

        @Override
        public boolean shouldHandleTouchEvent(MotionEvent ev) {
            if (MotionEvent.ACTION_DOWN == ev.getActionMasked()) {
                if (isTouchInsideList(ev) &&
                        ev.getX() <= mModeSelectorItems[0].getMaxVisibleWidth()) {
                    mChildViewTouched = mModeSelectorItems[getFocusItem(ev.getX(), ev.getY())];
                    return true;
                }
                // If shimmy is on-going, reject the first down event, so that it can be handled
                // by the view underneath. If a swipe is detected, the same series of touch will
                // re-enter this function, in which case we will consume the touch events.
                if (mLastDownTime != ev.getDownTime()) {
                    mLastDownTime = ev.getDownTime();
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (MotionEvent.ACTION_DOWN == ev.getActionMasked()) {
                if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    mFocusItem = getFocusItem(ev.getX(), ev.getY());
                    setSwipeMode(true);
                }
            }
            forwardTouchEventToChild(ev);
            return true;
        }

        @Override
        public void onItemSelected(ModeSelectorItem selectedItem) {
            cancelAnimation();
            mCurrentStateManager.setCurrentState(new SelectedState(selectedItem));
        }

        private void hideShimmyWithDelay() {
            postDelayed(mHideShimmy, HIDE_SHIMMY_DELAY_MS);
        }

        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            // 执行Hide操作
            if (mStartHidingShimmyWhenWindowGainsFocus && hasFocus) {
                mStartHidingShimmyWhenWindowGainsFocus = false;
                hideShimmyWithDelay();
            }
        }

        /**
         * This starts the accordion animation, unless it's already running, in which
         * case the start animation call will be ignored.
         */
        private void startHidingShimmy() {
            if (mAnimator != null) {
                return;
            }
            int maxVisibleWidth = mModeSelectorItems[0].getMaxVisibleWidth();
            // Gusterpolator.INSTANC 插值器 可以自行实现，接受 0~1 的数值， 进行函数变化
            mAnimator = animateListToWidth(START_DELAY_MS * (-1), TOTAL_DURATION_MS,
                    Gusterpolator.INSTANCE, maxVisibleWidth, 0);
            mAnimator.addListener(new Animator.AnimatorListener() {
                private boolean mSuccess = true;
                @Override
                public void onAnimationStart(Animator animation) {
                    // Do nothing.
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mAnimator = null;
                    ShimmyState.this.onAnimationEnd(mSuccess);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mSuccess = false;
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                    // Do nothing.
                }
            });
        }

        /**
         * Cancels the pending/on-going animation.
         */
        private void cancelAnimation() {
            removeCallbacks(mHideShimmy);
            if (mAnimator != null && mAnimator.isRunning()) {
                mAnimator.cancel();
            } else {
                mAnimator = null;
                onAnimationEnd(false);
            }
        }

        @Override
        public void onCurrentState() {
            super.onCurrentState();
        }
        /**
         * Gets called when the animation finishes or gets canceled.
         *
         * @param success indicates whether the animation finishes successfully
         */
        private void onAnimationEnd(boolean success) {
            // If successfully finish hiding shimmy, then we should go back to
            // fully hidden state.
            if (success) {
                mModeListOpenFactor = 1;
                android.util.Log.d("kk",     "setCurrentState  new FullyHiddenState() "  );
                mCurrentStateManager.setCurrentState(new FullyHiddenState());
                return;
            }

            // If the animation was canceled before it's finished, animate the mode
            // list open factor from 0 to 1 to ensure a smooth visual transition.
            final ValueAnimator openFactorAnimator = ValueAnimator.ofFloat(mModeListOpenFactor, 1f);
            openFactorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mModeListOpenFactor = (Float) openFactorAnimator.getAnimatedValue();
                    onVisibleWidthChanged(mVisibleWidth);
                }
            });
            openFactorAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    // Do nothing.
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mModeListOpenFactor = 1f;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    // Do nothing.
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                    // Do nothing.
                }
            });
            openFactorAnimator.start();
        }

        @Override
        public void hide() {
            cancelAnimation();
            mCurrentStateManager.setCurrentState(new FullyHiddenState());
        }

        @Override
        public void hideAnimated() {
            cancelAnimation();
            animateListToWidth(0).addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mCurrentStateManager.setCurrentState(new FullyHiddenState());
                }
            });
        }
    }

    public static abstract class ModeListVisibilityChangedListener {
        private Boolean mCurrentVisibility = null;

        /** Whether the mode list is (partially or fully) visible. */
        public abstract void onVisibilityChanged(boolean visible);

        /**
         * Internal method to be called by the mode list whenever a visibility
         * even occurs.
         * <p>
         * Do not call {@link #onVisibilityChanged(boolean)} directly, as this
         * is only called when the visibility has actually changed and not on
         * each visibility event.
         *
         * @param visible whether the mode drawer is currently visible.
         */
        private void onVisibilityEvent(boolean visible) {
            if (mCurrentVisibility == null || mCurrentVisibility != visible) {
                mCurrentVisibility = visible;
                onVisibilityChanged(visible);
            }
        }
    }

}
