package com.example.kevin.kcamera;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Util {

    private static final String TAG = "Util";

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

    public static Size getLargestPictureSize(Context context, int cameraId) {
        List<Size> sizes = null;
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics
                    = cameraManager.getCameraCharacteristics(cameraId + "");
            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                return null;
            }
            // For still image captures, we use the largest available size.
            sizes = Arrays.asList(map.getOutputSizes(ImageFormat.JPEG));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        if (sizes == null) {
            return null;
        }
        Size maxSize = new Size(0, 0);
        int pixelsDiff = DEFAULT_CAPTURE_PIXELS;
        for (Size size : sizes) {
            Rational aspectRatio = getAspectRatio(size);
            int pixelNum = size.getWidth() * size.getHeight();
            int d = DEFAULT_CAPTURE_PIXELS - pixelNum;
            // Skip if the aspect ratio is not desired.
            if (!hasSameAspectRatio(aspectRatio, ASPECT_RATIO_16x9) && d < 0) {
//                Log.d("kk", "  continue  size " +  size.toString() );
                continue;
            }
            d = Math.abs(d);
//            Log.d("kk", "  continue  pixelNum " +  pixelNum );
            if (d < pixelsDiff) {
                pixelsDiff = d;
                maxSize = size;
//                Log.d("kk", "     maxSize " +  maxSize.toString() );
            }
        }

        return maxSize;
    }

    public static Size getBestPreViewSize(Context context, int cameraId) {
        List<Size> sizes = null;
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics
                    = cameraManager.getCameraCharacteristics(cameraId + "");
            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                return null;
            }
            // For Preview is SurfaceTexture
            // For VideoSize is MediaRecorder
            sizes = Arrays.asList(map.getOutputSizes(SurfaceTexture.class));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        if (sizes == null) {
            return null;
        }
        Size maxSize = new Size(0, 0);
        int pixelsDiff = DEFAULT_CAPTURE_PIXELS;
        for (Size size : sizes) {
            Rational aspectRatio = getAspectRatio(size);
            int pixelNum = size.getWidth() * size.getHeight();
            int d = DEFAULT_CAPTURE_PIXELS - pixelNum;
            // Skip if the aspect ratio is not desired.
            if (!hasSameAspectRatio(aspectRatio, ASPECT_RATIO_16x9) && d < 0) {
                continue;
            }
            d = Math.abs(d);
            if (d < pixelsDiff) {
                pixelsDiff = d;
                maxSize = size;
            }
        }

        return maxSize;
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


}
