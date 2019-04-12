package com.example.kevin.kcamera;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.List;

public class CameraUtil {

    private static final String TAG = "CameraUtil";

    private static final double ASPECT_RATIO_TOLERANCE = 0.05;
    public static final Rational ASPECT_RATIO_16x9 = new Rational(16, 9);
    public static final Rational ASPECT_RATIO_4x3 = new Rational(4, 3);
    private static final int DEFAULT_CAPTURE_PIXELS = 1920 * 1080;

    public static void showToast(final Activity activity, final String text) {
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public static int getDisplayRotation() {
        WindowManager windowManager = AndroidServices.instance().provideWindowManager();
        int rotation = windowManager.getDefaultDisplay()
                .getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }

    public static Rational getAspectRatio(Size size) {
        int width = size.getWidth();
        int height = size.getHeight();
        int numerator = width;
        int denominator = height;
        if (height > width) {
            numerator = height;
            denominator = width;
        }
        return new Rational(numerator, denominator);
    }

    public static boolean hasSameAspectRatio(Rational ar1, Rational ar2) {
        return Math.abs(ar1.doubleValue() - ar2.doubleValue()) < ASPECT_RATIO_TOLERANCE;
    }

    public static int getCameraThemeColorId(int modeIndex, Context context) {
        TypedArray colorRes = context.getResources()
                .obtainTypedArray(R.array.camera_mode_theme_color);
        if (modeIndex >= colorRes.length() || modeIndex < 0) {
            // Mode index not found
            Log.e(TAG, "Invalid mode index: " + modeIndex);
            return 0;
        }
        return colorRes.getResourceId(modeIndex, 0);    }

    public static int getCameraModeIconResId(int modeIndex, Context context) {
        TypedArray cameraModesIcons = context.getResources()
                .obtainTypedArray(R.array.camera_mode_icon);
        if (modeIndex >= cameraModesIcons.length() || modeIndex < 0) {
            // Mode index not found
            Log.e(TAG, "Invalid mode index: " + modeIndex);
            return 0;
        }
        return cameraModesIcons.getResourceId(modeIndex, 0);    }

    public static CharSequence getCameraModeText(int modeIndex, Context context) {
        String[] cameraModesText = context.getResources()
                .getStringArray(R.array.camera_mode_text);
        if (modeIndex < 0 || modeIndex >= cameraModesText.length) {
            Log.e(TAG, "Invalid mode index: " + modeIndex);
            return new String();
        }
        return cameraModesText[modeIndex];    }


    public static RectF rectToRectF(Rect r) {
        return new RectF(r.left, r.top, r.right, r.bottom);
    }

    public static Rect rectFToRect(RectF rectF) {
        Rect rect = new Rect();
        inlineRectToRectF(rectF, rect);
        return rect;
    }

    public static void inlineRectToRectF(RectF rectF, Rect rect) {
        rect.left = Math.round(rectF.left);
        rect.top = Math.round(rectF.top);
        rect.right = Math.round(rectF.right);
        rect.bottom = Math.round(rectF.bottom);
    }

    public static com.example.kevin.kcamera.Size getDefaultDisplayRealSize() {
        WindowManager windowManager = AndroidServices.instance().provideWindowManager();
        Point res = new Point();
        Point  realRes = new Point();
        windowManager.getDefaultDisplay().getSize(res);
        windowManager.getDefaultDisplay().getRealSize(realRes);
        return new com.example.kevin.kcamera.Size(realRes);
    }

    public static com.example.kevin.kcamera.Size getOptimalPreviewSize(List<com.example.kevin.kcamera.Size> sizes, double targetRatio) {
        int optimalPickIndex = getOptimalPreviewSizeIndex(sizes, targetRatio);
        if (optimalPickIndex == -1) {
            return null;
        } else {
            return sizes.get(optimalPickIndex);
        }    }

    private static int getOptimalPreviewSizeIndex(List<com.example.kevin.kcamera.Size> sizes, double targetRatio) {
        final double aspectRatioTolerance = 0.02;

        return getOptimalPreviewSizeIndex(sizes, targetRatio, aspectRatioTolerance);    }

    private static int getOptimalPreviewSizeIndex(List<com.example.kevin.kcamera.Size> previewSizes, double targetRatio, Double aspectRatioTolerance) {
        if (previewSizes == null) {
            return -1;
        }

        // If no particular aspect ratio tolerance is set, use the default
        // value.
        if (aspectRatioTolerance == null) {
            return getOptimalPreviewSizeIndex(previewSizes, targetRatio);
        }

        int optimalSizeIndex = -1;
        double minDiff = Double.MAX_VALUE;

        // Because of bugs of overlay and layout, we sometimes will try to
        // layout the viewfinder in the portrait orientation and thus get the
        // wrong size of preview surface. When we change the preview size, the
        // new overlay will be created before the old one closed, which causes
        // an exception. For now, just get the screen size.
        com.example.kevin.kcamera.Size defaultDisplaySize = getDefaultDisplaySize();
        int targetHeight = Math.min(defaultDisplaySize.getWidth(), defaultDisplaySize.getHeight());
        // Try to find an size match aspect ratio and size
        for (int i = 0; i < previewSizes.size(); i++) {
            com.example.kevin.kcamera.Size size = previewSizes.get(i);
            double ratio = (double) size.getWidth() / size.getHeight();
            if (Math.abs(ratio - targetRatio) > aspectRatioTolerance) {
                continue;
            }

            double heightDiff = Math.abs(size.getHeight() - targetHeight);
            if (heightDiff < minDiff) {
                optimalSizeIndex = i;
                minDiff = heightDiff;
            } else if (heightDiff == minDiff) {
                // Prefer resolutions smaller-than-display when an equally close
                // larger-than-display resolution is available
                if (size.getHeight() < targetHeight) {
                    optimalSizeIndex = i;
                    minDiff = heightDiff;
                }
            }
        }
        // Cannot find the one match the aspect ratio. This should not happen.
        // Ignore the requirement.
        if (optimalSizeIndex == -1) {
            Log.w(TAG, "No preview size match the aspect ratio. available sizes: " + previewSizes);
            minDiff = Double.MAX_VALUE;
            for (int i = 0; i < previewSizes.size(); i++) {
                com.example.kevin.kcamera.Size size = previewSizes.get(i);
                if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                    optimalSizeIndex = i;
                    minDiff = Math.abs(size.getHeight() - targetHeight);
                }
            }
        }

        return optimalSizeIndex;
    }

    private static com.example.kevin.kcamera.Size getDefaultDisplaySize() {
        WindowManager windowManager = AndroidServices.instance().provideWindowManager();
        Point res = new Point();
        windowManager.getDefaultDisplay().getSize(res);
        return new com.example.kevin.kcamera.Size(res);
    }
}
