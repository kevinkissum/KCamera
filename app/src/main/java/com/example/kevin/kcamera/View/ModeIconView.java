package com.example.kevin.kcamera.View;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.example.kevin.kcamera.R;

public class ModeIconView extends View {

    private final GradientDrawable mBackground;
    private final int mIconBackgroundSize;
    private int mHighlightColor;
    private final int mBackgroundDefaultColor;
    private final int mIconDrawableSize;
    private Drawable mIconDrawable = null;

    public ModeIconView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mBackgroundDefaultColor = getResources().getColor(R.color.mode_selector_icon_background);
        mIconBackgroundSize = getResources().getDimensionPixelSize(
                R.dimen.mode_selector_icon_block_width);
        mBackground = (GradientDrawable) getResources()
                .getDrawable(R.drawable.mode_icon_background).mutate();
        mBackground.setBounds(0, 0, mIconBackgroundSize, mIconBackgroundSize);
        mIconDrawableSize = getResources().getDimensionPixelSize(
                R.dimen.mode_selector_icon_drawable_size);
    }

    public void setHighlightColor(int highlightColor) {
        mHighlightColor = highlightColor;
    }

    public int getHighlightColor() {
        return mHighlightColor;
    }

    public void setIconDrawable(Drawable drawable) {
        mIconDrawable = drawable;

        // Center icon in the background.
        if (mIconDrawable != null) {
            mIconDrawable.setBounds(mIconBackgroundSize / 2 - mIconDrawableSize / 2,
                    mIconBackgroundSize / 2 - mIconDrawableSize / 2,
                    mIconBackgroundSize / 2 + mIconDrawableSize / 2,
                    mIconBackgroundSize / 2 + mIconDrawableSize / 2);
            invalidate();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        mBackground.draw(canvas);
        if (mIconDrawable != null) {
            mIconDrawable.draw(canvas);
        }
    }


}
