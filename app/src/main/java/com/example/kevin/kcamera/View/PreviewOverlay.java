package com.example.kevin.kcamera.View;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class PreviewOverlay extends View {

    private GestureDetector mGestureDetector;

    public PreviewOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void GestureDetectorListener(GestureDetector gestureDetector) {
        mGestureDetector = gestureDetector;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(event);
        }
        return true;
    }
}
