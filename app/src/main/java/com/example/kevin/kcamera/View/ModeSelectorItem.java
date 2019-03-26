package com.example.kevin.kcamera.View;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.kevin.kcamera.R;

public class ModeSelectorItem extends FrameLayout {

    private TextView mText;
    private ModeIconView mIcon;
    private int mVisibleWidth = 0;
    private final int mMinVisibleWidth;

    private int mWidth;
    private int mModeId;

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

    public void setHighlightColor(int highlightColor) {
        mIcon.setHighlightColor(highlightColor);
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


}
