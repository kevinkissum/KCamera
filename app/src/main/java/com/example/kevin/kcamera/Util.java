package com.example.kevin.kcamera;

import android.app.Activity;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

public class Util {

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


//    public static int getSensorOrientation(final int cameraId, final int orientation) {
//        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
//    }
}
