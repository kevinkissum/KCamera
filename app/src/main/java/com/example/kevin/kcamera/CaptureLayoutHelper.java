package com.example.kevin.kcamera;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Log;

public class CaptureLayoutHelper implements PhotoUI.NonDecorWindowSizeChangedListener{

    public static final String TAG = "CaptureLayoutHelper";

    private PositionConfiguration mPositionConfiguration = null;
    private int mWindowWidth, mWindowHeight;
    private float mAspectRatio;
    private int mRotation;
    private float mBottomBarOptimalHeight;
    private float mBottomBarMinHeight;
    private float mBottomBarMaxHeight;

    public CaptureLayoutHelper(int bottomBarMinHeight, int bottomBarMaxHeight, int bottomBarOptimalHeight) {
        mBottomBarMinHeight = bottomBarMinHeight;
        mBottomBarMaxHeight = bottomBarMaxHeight;
        mBottomBarOptimalHeight = bottomBarOptimalHeight;
        Log.d("kk", "updatePositionConfiguration: " +
                "mBottomBarMinHeight " + mBottomBarMinHeight
                + " mBottomBarMaxHeight " + mBottomBarMaxHeight
                + " mBottomBarOptimalHeight " + mBottomBarOptimalHeight);

    }

    public void setAspectRatio(float aspectRatio) {
        Log.d(TAG, "setAspectRatio: " + aspectRatio);
        mAspectRatio = aspectRatio;
        updatePositionConfiguration();

    }

    public RectF getBottomBarRect() {
        if (mPositionConfiguration == null) {
            updatePositionConfiguration();
        }
        // Not enough info to create a position configuration.
        if (mPositionConfiguration == null) {
            return new RectF();
        }
        return new RectF(mPositionConfiguration.mBottomBarRect);
    }

    public boolean shouldOverlayBottomBar() {
        if (mPositionConfiguration == null) {
            updatePositionConfiguration();
        }
        // Not enough info to create a position configuration.
        if (mPositionConfiguration == null) {
            return false;
        }
        return mPositionConfiguration.mBottomBarOverlay;
    }

    private void updatePositionConfiguration() {
        Log.d(TAG, "updatePositionConfiguration: mAspectRatio " + mAspectRatio + " mWindowWidth " + mWindowWidth + " mWindowHeight " + mWindowHeight + " mRotation " + mRotation);
        if (mWindowWidth == 0 || mWindowHeight == 0) {
            return;
        }
        mPositionConfiguration = getPositionConfiguration(mWindowWidth, mWindowHeight, mAspectRatio,
                mRotation);
    }

    private PositionConfiguration getPositionConfiguration(int width, int height,
                                                           float previewAspectRatio, int rotation) {
        boolean landscape = width > height;

        // If the aspect ratio is defined as fill the screen, then preview should
        // take the screen rect.
        PositionConfiguration config = new PositionConfiguration();
        if (previewAspectRatio == /*TextureViewHelper.MATCH_SCREEN*/0f) {
            config.mPreviewRect.set(0, 0, width, height);
            config.mBottomBarOverlay = true;
            if (landscape) {
                config.mBottomBarRect.set(width - mBottomBarOptimalHeight, 0, width, height);
            } else {
                config.mBottomBarRect.set(0, height - mBottomBarOptimalHeight, width, height);
            }
        } else {
            if (previewAspectRatio < 1) {
                previewAspectRatio = 1 / previewAspectRatio;
            }
            // Get the bottom bar width and height.
            float barSize;
            int longerEdge = Math.max(width, height);
            int shorterEdge = Math.min(width, height);

            // Check the remaining space if fit short edge.
            float spaceNeededAlongLongerEdge = shorterEdge * previewAspectRatio;
            float remainingSpaceAlongLongerEdge = longerEdge - spaceNeededAlongLongerEdge;

            float previewShorterEdge;
            float previewLongerEdge;
            if (remainingSpaceAlongLongerEdge <= 0) {
                // Preview aspect ratio > screen aspect ratio: fit longer edge.
                previewLongerEdge = longerEdge;
                previewShorterEdge = longerEdge / previewAspectRatio;
                barSize = mBottomBarOptimalHeight;
                config.mBottomBarOverlay = true;

                if (landscape) {
                    config.mPreviewRect.set(0, height / 2 - previewShorterEdge / 2, previewLongerEdge,
                            height / 2 + previewShorterEdge / 2);
                    config.mBottomBarRect.set(width - barSize, height / 2 - previewShorterEdge / 2,
                            width, height / 2 + previewShorterEdge / 2);
                } else {
                    config.mPreviewRect.set(width / 2 - previewShorterEdge / 2, 0,
                            width / 2 + previewShorterEdge / 2, previewLongerEdge);
                    config.mBottomBarRect.set(width / 2 - previewShorterEdge / 2, height - barSize,
                            width / 2 + previewShorterEdge / 2, height);
                }
            } else if (previewAspectRatio > 14f / 9f) {
                // If the preview aspect ratio is large enough, simply offset the
                // preview to the bottom/right.
                // TODO: This logic needs some refinement.
                barSize = mBottomBarOptimalHeight;
                previewShorterEdge = shorterEdge;
                previewLongerEdge = shorterEdge * previewAspectRatio;
                config.mBottomBarOverlay = true;
                if (landscape) {
                    float right = width;
                    float left = right - previewLongerEdge;
                    config.mPreviewRect.set(left, 0, right, previewShorterEdge);
                    config.mBottomBarRect.set(width - barSize, 0, width, height);
                } else {
                    float bottom = height;
                    float top = bottom - previewLongerEdge;
                    config.mPreviewRect.set(0, top, previewShorterEdge, bottom);
                    config.mBottomBarRect.set(0, height - barSize, width, height);
                }
            } else if (remainingSpaceAlongLongerEdge <= mBottomBarMinHeight) {
                // Need to scale down the preview to fit in the space excluding the bottom bar.
                previewLongerEdge = longerEdge - mBottomBarMinHeight;
                previewShorterEdge = previewLongerEdge / previewAspectRatio;
                barSize = mBottomBarMinHeight;
                config.mBottomBarOverlay = false;
                if (landscape) {
                    config.mPreviewRect.set(0, height / 2 - previewShorterEdge / 2, previewLongerEdge,
                            height / 2 + previewShorterEdge / 2);
                    config.mBottomBarRect.set(width - barSize, height / 2 - previewShorterEdge / 2,
                            width, height / 2 + previewShorterEdge / 2);
                } else {
                    config.mPreviewRect.set(width / 2 - previewShorterEdge / 2, 0,
                            width / 2 + previewShorterEdge / 2, previewLongerEdge);
                    config.mBottomBarRect.set(width / 2 - previewShorterEdge / 2, height - barSize,
                            width / 2 + previewShorterEdge / 2, height);
                }
            } else {
                // Fit shorter edge.
                barSize = remainingSpaceAlongLongerEdge <= mBottomBarMaxHeight ?
                        remainingSpaceAlongLongerEdge : mBottomBarMaxHeight;
                previewShorterEdge = shorterEdge;
                previewLongerEdge = shorterEdge * previewAspectRatio;
                config.mBottomBarOverlay = false;
                if (landscape) {
                    float right = width - barSize;
                    float left = right - previewLongerEdge;
                    config.mPreviewRect.set(left, 0, right, previewShorterEdge);
                    config.mBottomBarRect.set(width - barSize, 0, width, height);
                } else {
                    float bottom = height - barSize;
                    float top = bottom - previewLongerEdge;
                    config.mPreviewRect.set(0, top, previewShorterEdge, bottom);
                    config.mBottomBarRect.set(0, height - barSize, width, height);
                }
            }
        }

        if (rotation >= 180) {
            // Rotate 180 degrees.
            Matrix rotate = new Matrix();
            rotate.setRotate(180, width / 2, height / 2);

            rotate.mapRect(config.mPreviewRect);
            rotate.mapRect(config.mBottomBarRect);
        }

        // Round the rect first to avoid rounding errors later on.
        round(config.mBottomBarRect);
        round(config.mPreviewRect);

        return config;
    }

    public static void round(RectF rect) {
        if (rect == null) {
            return;
        }
        float left = Math.round(rect.left);
        float top = Math.round(rect.top);
        float right = Math.round(rect.right);
        float bottom = Math.round(rect.bottom);
        rect.set(left, top, right, bottom);
    }

    @Override
    public void onNonDecorWindowSizeChanged(int width, int height, int rotation) {
        mWindowWidth = width;
        mWindowHeight = height;
        mRotation = rotation;
        updatePositionConfiguration();
    }

    public static final class PositionConfiguration {
        /**
         * This specifies the rect of preview on screen.
         */
        public final RectF mPreviewRect = new RectF();
        /**
         * This specifies the rect where bottom bar should be laid out in.
         */
        public final RectF mBottomBarRect = new RectF();
        /**
         * This indicates whether bottom bar should overlay itself on top of preview.
         */
        public boolean mBottomBarOverlay = false;
    }
}
