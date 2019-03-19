package com.example.kevin.kcamera.View;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import com.example.kevin.kcamera.CaptureLayoutHelper;
import com.example.kevin.kcamera.R;

public class StickyBottomCaptureLayout extends FrameLayout {

    public final static String TAG = "StickyBottomBar";
    private CaptureLayoutHelper mCaptureLayoutHelper;
    private BottomBar mBottomBar;


    public StickyBottomCaptureLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setCaptureLayoutHelper(CaptureLayoutHelper helper) {
        mCaptureLayoutHelper = helper;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mBottomBar = findViewById(R.id.bottom_bar);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (mCaptureLayoutHelper == null) {
            Log.e(TAG, "Capture layout helper needs to be set first.");
            return;
        }
        // Layout bottom bar.
        RectF bottomBarRect = mCaptureLayoutHelper.getBottomBarRect();
        mBottomBar.layout((int) bottomBarRect.left, (int) bottomBarRect.top,
                (int) bottomBarRect.right, (int) bottomBarRect.bottom);
    }
}
