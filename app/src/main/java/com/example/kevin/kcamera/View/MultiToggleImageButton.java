package com.example.kevin.kcamera.View;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.kevin.kcamera.R;

import java.io.OptionalDataException;

public class MultiToggleImageButton extends ImageButton {

    private OnStateChangeListener mOnStateChangeListener;
    private OnStateChangeListener mOnStatePreChangeListener;
    private int mState;
    private boolean mClickEnabled;
    private int[] mImageIds;
    private int mLevel;
    public static final int ANIM_DIRECTION_VERTICAL = 0;
    public static final int ANIM_DIRECTION_HORIZONTAL = 1;

    private static final int ANIM_DURATION_MS = 250;
    private static final int UNSET = -1;

    public interface OnStateChangeListener {
        /*
         * @param view the MultiToggleImageButton that received the touch event
         * @param state the new state the button is in
         */
        public abstract void stateChanged(View view, int state);
    }

    public MultiToggleImageButton(Context context) {
        super(context);
        init();
    }

    public MultiToggleImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        parseAttributes(context, attrs);
        setState(0);
    }

    public MultiToggleImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
        parseAttributes(context, attrs);
        setState(0);
    }

    private void setState(int state) {
        setState(state, true);
    }

    private void setState(int state, boolean callListener) {
        setStateAnimatedInternal(state, callListener);
    }

    private void setStateAnimatedInternal(int state, boolean callListener) {
        if(callListener && mOnStatePreChangeListener != null) {
            mOnStatePreChangeListener.stateChanged(MultiToggleImageButton.this, mState);
        }

        if (mState == state || mState == UNSET) {
            setStateInternal(state, callListener);
            return;
        }

        if (mImageIds == null) {
            return;
        }

//        new AsyncTask<Integer, Void, Bitmap>() {
//            @Override
//            protected Bitmap doInBackground(Integer... params) {
//                return combine(params[0], params[1]);
//            }
//
//            @Override
//            protected void onPostExecute(Bitmap bitmap) {
//                if (bitmap == null) {
//                    setStateInternal(state, callListener);
//                } else {
//                    setImageBitmap(bitmap);
//
//                    int offset;
//                    if (mAnimDirection == ANIM_DIRECTION_VERTICAL) {
//                        offset = (mParentSize+getHeight())/2;
//                    } else if (mAnimDirection == ANIM_DIRECTION_HORIZONTAL) {
//                        offset = (mParentSize+getWidth())/2;
//                    } else {
//                        return;
//                    }
//
//                    mAnimator.setFloatValues(-offset, 0.0f);
//                    AnimatorSet s = new AnimatorSet();
//                    s.play(mAnimator);
//                    s.addListener(new AnimatorListenerAdapter() {
//                        @Override
//                        public void onAnimationStart(Animator animation) {
//                            setClickEnabled(false);
//                        }
//
//                        @Override
//                        public void onAnimationEnd(Animator animation) {
//                            setStateInternal(state, callListener);
//                            setClickEnabled(true);
//                        }
//                    });
//                    s.start();
//                }
//            }
//        }.execute(mState, state);
    }

    private void setStateInternal(int state, boolean callListener) {
        mState = state;
        if (mImageIds != null) {
            setImageByState(mState);
        }

//        if (mDescIds != null) {
//            String oldContentDescription = String.valueOf(getContentDescription());
//            String newContentDescription = getResources().getString(mDescIds[mState]);
//            if (oldContentDescription != null && !oldContentDescription.isEmpty()
//                    && !oldContentDescription.equals(newContentDescription)) {
//                setContentDescription(newContentDescription);
//                String announceChange = getResources().getString(
//                        R.string.button_change_announcement, newContentDescription);
//                announceForAccessibility(announceChange);
//            }
//        }
        super.setImageLevel(mLevel);

        if (callListener && mOnStateChangeListener != null) {
            mOnStateChangeListener.stateChanged(MultiToggleImageButton.this, getState());
        }
    }

    private int getState() {
        return mState;
    }

    private void parseAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.MultiToggleImageButton,
                0, 0);
        int imageIds = a.getResourceId(R.styleable.MultiToggleImageButton_imageIds, 0);
        if (imageIds > 0) {
            overrideImageIds(imageIds);
        }
        a.recycle();
    }

    private void overrideImageIds(int resId) {
        TypedArray ids = null;
        try {
            ids = getResources().obtainTypedArray(resId);
            mImageIds = new int[ids.length()];
            for (int i = 0; i < ids.length(); i++) {
                mImageIds[i] = ids.getResourceId(i, 0);
            }
        } finally {
            if (ids != null) {
                ids.recycle();
            }
        }

        if (mState >= 0 && mState < mImageIds.length) {
            setImageByState(mState);
        }
    }

    private void setImageByState(int state) {
        if (mImageIds != null) {
            setImageResource(mImageIds[state]);
        }
        super.setImageLevel(mLevel);
    }

    protected void init() {
        this.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickEnabled) {
                    nextState();
                }
            }
        });
        setScaleType(ImageView.ScaleType.MATRIX);

//        mAnimator = ValueAnimator.ofFloat(0.0f, 0.0f);
//        mAnimator.setDuration(ANIM_DURATION_MS);
//        mAnimator.setInterpolator(Gusterpolator.INSTANCE);
//        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator animation) {
//                mMatrix.reset();
//                if (mAnimDirection == ANIM_DIRECTION_VERTICAL) {
//                    mMatrix.setTranslate(0.0f, (Float) animation.getAnimatedValue());
//                } else if (mAnimDirection == ANIM_DIRECTION_HORIZONTAL) {
//                    mMatrix.setTranslate((Float) animation.getAnimatedValue(), 0.0f);
//                }
//
//                setImageMatrix(mMatrix);
//                invalidate();
//            }
//        });
    }

    private void nextState() {
        int state = mState + 1;
        if (state >= mImageIds.length) {
            state = 0;
        }
        setState(state);
    }

    public void setOnStateChangeListener(OnStateChangeListener onStateChangeListener) {
        mOnStateChangeListener = onStateChangeListener;
    }

    public void setOnPreChangeListener(OnStateChangeListener onStatePreChangeListener) {
        mOnStatePreChangeListener = onStatePreChangeListener;
    }
}
