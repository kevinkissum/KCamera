package com.example.kevin.kcamera.View;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.kevin.kcamera.R;
import com.example.kevin.kcamera.Util;

public class ModeSelectorItem extends FrameLayout {

    private TextView mText;
    private ModeIconView mIcon;
    private int mVisibleWidth = 0;
    private final int mMinVisibleWidth;

    private int mWidth;
    private int mModeId;
    private VisibleWidthChangedListener mListener;

    public int getVisibleWidth() {
        return mVisibleWidth;
    }

    public void getIconCenterLocationInWindow(int[] loc) {
        mIcon.getLocationInWindow(loc);
        loc[0] += mMinVisibleWidth / 2;
        loc[1] += mMinVisibleWidth / 2;
    }

    public int getHighlightColor() {
        return mIcon.getHighlightColor();
    }

    public interface VisibleWidthChangedListener {
        public void onVisibleWidthChanged(int width);
    }

    public ModeSelectorItem(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        setClickable(true);
        mMinVisibleWidth = getResources()
                .getDimensionPixelSize(R.dimen.mode_selector_icon_block_width);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mIcon = (ModeIconView) findViewById(R.id.selector_icon);
        mText = (TextView) findViewById(R.id.selector_text);
        Typeface typeface;
        if (true) {
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        } else {
            // Load roboto_light typeface from assets.
            typeface = Typeface.createFromAsset(getResources().getAssets(),
                    "Roboto-Medium.ttf");
        }
        mText.setTypeface(typeface);
    }

    public void setVisibleWidthChangedListener(VisibleWidthChangedListener listener) {
        mListener = listener;
    }


    public void setHighlightColor(int highlightColor) {
        mIcon.setHighlightColor(highlightColor);
    }

    /**
     * Sets the alpha on the mode text.
     */
    public void setTextAlpha(float alpha) {
        mText.setAlpha(alpha);
    }

    public void setImageResource(int resource) {
        Drawable drawableIcon = getResources().getDrawable(resource);
        if (drawableIcon != null) {
            drawableIcon = drawableIcon.mutate();
        }
        mIcon.setIconDrawable(drawableIcon);
    }

    public void setText(CharSequence text) {
        mText.setText(text);
    }

    public void setModeId(int modeId) {
        mModeId = modeId;
    }

    public int getModeId() {
        return mModeId;
    }

    public ModeIconView getIcon() {
        return mIcon;
    }


    public void setVisibleWidth(int newWidth) {
        int fullyShownIconWidth = getMaxVisibleWidth();
        newWidth = Math.max(newWidth, 0);
        // Visible width should not be greater than view width
        newWidth = Math.min(newWidth, fullyShownIconWidth);
        if (mVisibleWidth != newWidth) {
            mVisibleWidth = newWidth;
            if (mListener != null) {
                // newWidth 0 ~ 216
                mListener.onVisibleWidthChanged(newWidth);
            }
        }
        /**
         * invalidate 和 requestLayout postInvalidate 区别
         * 子View调用requestLayout方法，会标记当前View及父容器，同时逐层向上提交，直到ViewRootImpl处理该事件，
         * ViewRootImpl会调用三大流程，从measure开始，对于每一个含有标记位的view及其子View都会进行测量、布局、绘制。
         *
         * 当子View调用了invalidate方法后，会为该View添加一个标记位，同时不断向父容器请求刷新，
         * 父容器通过计算得出自身需要重绘的区域，直到传递到ViewRootImpl中，
         * 最终触发performTraversals方法，进行开始View树重绘流程(只绘制需要重绘的视图) 即ondraw
         *
         * 这个方法与invalidate方法的作用是一样的，都是使View树重绘，
         * 但两者的使用条件不同，postInvalidate是在非UI线程中调用，invalidate则是在UI线程中调用。
         */
        invalidate();
    }

    public int getMaxVisibleWidth() {
        return mIcon.getLeft() + mMinVisibleWidth;
    }

    public void onSwipeModeChanged(boolean swipeIn) {
        mText.setTranslationX(0);
    }

    @Override
    public void draw(Canvas canvas) {
        float transX = 0f;
        // If the given width is less than the icon width, we need to translate icon
        if (mVisibleWidth < mMinVisibleWidth + mIcon.getLeft()) {
            transX = mMinVisibleWidth + mIcon.getLeft() - mVisibleWidth;
        }
        canvas.save();
        canvas.translate(-transX, 0);
        super.draw(canvas);
        canvas.restore();
    }
}
