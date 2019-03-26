package com.example.kevin.kcamera.View;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.example.kevin.kcamera.CameraAppUI;
import com.example.kevin.kcamera.Util;

public class MainActivityLayout  extends FrameLayout {

    private CameraAppUI.NonDecorWindowSizeChangedListener mNonDecorWindowSizeChangedListener = null;


    public MainActivityLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mNonDecorWindowSizeChangedListener != null) {
            mNonDecorWindowSizeChangedListener.onNonDecorWindowSizeChanged(
                    MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec),
                    Util.getDisplayRotation());
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setNonDecorWindowSizeChangedListener(
            CameraAppUI.NonDecorWindowSizeChangedListener listener) {
        mNonDecorWindowSizeChangedListener = listener;
        if (mNonDecorWindowSizeChangedListener != null) {
            mNonDecorWindowSizeChangedListener.onNonDecorWindowSizeChanged(
                    getMeasuredWidth(), getMeasuredHeight(),
                    Util.getDisplayRotation());
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean result = super.dispatchTouchEvent(ev);
        android.util.Log.d("kk", "  disaptch touch event " + result);
        return result;
    }
}
