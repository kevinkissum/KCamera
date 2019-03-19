package com.example.kevin.kcamera.View;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.example.kevin.kcamera.CaptureLayoutHelper;
import com.example.kevin.kcamera.R;

public class BottomBar extends FrameLayout {

    public final static String TAG = "BottomBar";


    private static final int CIRCLE_ANIM_DURATION_MS = 300;
    private static final int DRAWABLE_MAX_LEVEL = 10000;
    private static final int MODE_CAPTURE = 0;
    private static final int MODE_INTENT = 1;
    private static final int MODE_INTENT_REVIEW = 2;
    private static final int MODE_CANCEL = 3;

    private int mMode;

    private boolean mOverLayBottomBar;

    private FrameLayout mCaptureLayout;
    private FrameLayout mCancelLayout;

    private ShutterButton mShutterButton;
    private ImageButton mCancelButton;

    private int mBackgroundColor;
    private int mBackgroundPressedColor;
    private int mBackgroundAlpha = 0xff;

    private boolean mDrawCircle;
    private CaptureLayoutHelper mCaptureLayoutHelper = null;

    private ColorDrawable mColorDrawable;

    private RectF mRect = new RectF();

    public BottomBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

    }


    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mCaptureLayout =
                (FrameLayout) findViewById(R.id.bottombar_capture);
        mCancelLayout =
                (FrameLayout) findViewById(R.id.bottombar_cancel);
        mCancelLayout.setVisibility(View.GONE);

        mShutterButton =
                (ShutterButton) findViewById(R.id.shutter_button);
        mShutterButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEvent.ACTION_DOWN == event.getActionMasked()) {
                    setCaptureButtonDown();
                } else if (MotionEvent.ACTION_UP == event.getActionMasked() ||
                        MotionEvent.ACTION_CANCEL == event.getActionMasked()) {
                    setCaptureButtonUp();
                } else if (MotionEvent.ACTION_MOVE == event.getActionMasked()) {
                    mRect.set(0, 0, getWidth(), getHeight());
                    if (!mRect.contains(event.getX(), event.getY())) {
                        setCaptureButtonUp();
                    }
                }
                return false;
            }
        });

    }

    private void setCaptureButtonUp() {
        setPaintColor(mBackgroundAlpha, mBackgroundColor);
    }

    private void setCaptureButtonDown() {
        setPaintColor(mBackgroundAlpha, mBackgroundPressedColor);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int measureWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int measureHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (measureWidth == 0 || measureHeight == 0) {
            return;
        }

        if (mCaptureLayoutHelper == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            Log.e(TAG, "Capture layout helper needs to be set first.");
        } else {
            RectF bottomBarRect = mCaptureLayoutHelper.getBottomBarRect();
            super.onMeasure(MeasureSpec.makeMeasureSpec(
                    (int) bottomBarRect.width(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec((int) bottomBarRect.height(), MeasureSpec.EXACTLY)
            );
            boolean shouldOverlayBottomBar = mCaptureLayoutHelper.shouldOverlayBottomBar();
            setOverlayBottomBar(shouldOverlayBottomBar);
        }
    }

    private void setOverlayBottomBar(boolean overlay) {

    }

    public void setBackgroundAlpha(int alpha) {

    }

    private void setButtonImageLevels(int level) {

    }

    private void setPaintColor(int alpha, int color) {

    }

    private void setCancelBackgroundColor(int alpha, int color) {

    }


}
