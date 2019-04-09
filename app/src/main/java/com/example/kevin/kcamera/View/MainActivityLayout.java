package com.example.kevin.kcamera.View;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import com.example.kevin.kcamera.CameraAppUI;
import com.example.kevin.kcamera.R;
import com.example.kevin.kcamera.CameraUtil;

public class MainActivityLayout  extends FrameLayout {

    private static final String TAG = "MainActivityLayout";

    private static final int SWIPE_TIME_OUT = 500;
    private CameraAppUI.NonDecorWindowSizeChangedListener mNonDecorWindowSizeChangedListener = null;
    private ModeListView mModeList;
    //    private FilmstripLayout mFilmstripLayout;
    private boolean mCheckToIntercept;
    private MotionEvent mDown;
        private final int mSlop;
    private boolean mRequestToInterceptTouchEvents = false;
    private View mTouchReceiver = null;
    //    private final boolean mIsCaptureIntent;
    @Deprecated
    private boolean mSwipeEnabled = true;

    public MainActivityLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mNonDecorWindowSizeChangedListener != null) {
            mNonDecorWindowSizeChangedListener.onNonDecorWindowSizeChanged(
                    MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec),
                    CameraUtil.getDisplayRotation());
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setNonDecorWindowSizeChangedListener(
            CameraAppUI.NonDecorWindowSizeChangedListener listener) {
        mNonDecorWindowSizeChangedListener = listener;
        if (mNonDecorWindowSizeChangedListener != null) {
            mNonDecorWindowSizeChangedListener.onNonDecorWindowSizeChanged(
                    getMeasuredWidth(), getMeasuredHeight(),
                    CameraUtil.getDisplayRotation());
        }
    }

    /**
     * 点击屏幕， activity只会将事件传递给该view的ViewGroup，其它的View是无法拿到事件的.
     * 通过将PreviewOverlay设置成很小的区域可以发现，由于点击的地方是TextureView，
     * TextureView不会处理Touch事件， down事件都不处理， 导致后面的Touch事件均不会处理.
     * 所以PreviewOverlay需要将Touch事件接手. 同理，如果在PreviewOverlay中onTouch中down没有返回true，
     * 之后move事件也不会进行传递。MainLayoutView中接受到down事件！！！
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // 从Activity的点击事件第一个到此dispatch中
        // super即ViewGroup的dispatch，ViewGroup会遍历子View， 将事件继续传递子View的dispatch
        // MainView中的子View 有 PreviewOverviewLay && ModeListView ect.
        // 在传递之前还需要intercept的判断
        // !!!! 如果dispatch或者onTouchEvent返回true， 说明view消费了该事件， 传递即终止 !!!!
        boolean result = super.dispatchTouchEvent(ev);
        return result;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d("kk", " Main onTouchEvent " + mTouchReceiver);
        if (mTouchReceiver != null) {
            // 调试发现并不需要设置visible，也可以用, setVisible是因为原先设置为invisible
            // 将事件传递给modeListView
            mTouchReceiver.setVisibility(VISIBLE);
            return mTouchReceiver.dispatchTouchEvent(event);
        }
        return false;

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // ViewGroup 传给子View的事情均会在此进行判断
        // 调试发现， 当持续move的时候， move事件会跳过判断，直接走TouchEvent
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mCheckToIntercept = true;
            mDown = MotionEvent.obtain(ev);
            mTouchReceiver = null;
            mRequestToInterceptTouchEvents = false;
            return false;
        } else if (mRequestToInterceptTouchEvents) {
            mRequestToInterceptTouchEvents = false;
            onTouchEvent(mDown);
            return true;
        } else if (ev.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            // Do not intercept touch once child is in zoom mode
            mCheckToIntercept = false;
            return false;
        } else {
//            // TODO: This can be removed once we come up with a new design for b/13751653.
            if (!mCheckToIntercept) {
                return false;
            }
            if (ev.getEventTime() - ev.getDownTime() > SWIPE_TIME_OUT) {
                return false;
            }
            if (/*mIsCaptureIntent ||*/ !mSwipeEnabled) {
                return false;
            }
            int deltaX = (int) (ev.getX() - mDown.getX());
            int deltaY = (int) (ev.getY() - mDown.getY());
            if (ev.getActionMasked() == MotionEvent.ACTION_MOVE
                    && Math.abs(deltaX) > mSlop) {
                // 当开始Move的时候，在此进行拦截事件，调用onTouchEvent， 将touch事件传递给目标view
                // Intercept right swipe
                if (deltaX >= Math.abs(deltaY) * 2) {
                    mTouchReceiver = mModeList;
                    onTouchEvent(mDown);
                    return true;
                }
                // Intercept left swipe
                else if (deltaX < -Math.abs(deltaY) * 2) {
//                    mTouchReceiver = mFilmstripLayout;
                    onTouchEvent(mDown);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mModeList = (ModeListView) findViewById(R.id.mode_list_layout);
//        mFilmstripLayout = (FilmstripLayout) findViewById(R.id.filmstrip_layout);
    }


    public void redirectTouchEventsTo(View touchReceiver) {
        if (touchReceiver == null) {
            Log.e(TAG, "Cannot redirect touch to a null receiver.");
            return;
        }
        mTouchReceiver = touchReceiver;
        mRequestToInterceptTouchEvents = true;
    }
}
