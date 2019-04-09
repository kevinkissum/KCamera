package com.example.kevin.kcamera;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.util.LruCache;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class Storage {
    public static final String DCIM =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
    public static final String DIRECTORY = DCIM + "/Camera";
    public static final File DIRECTORY_FILE = new File(DIRECTORY);
    public static final String JPEG_POSTFIX = ".jpg";
    public static final String GIF_POSTFIX = ".gif";
    public static final long UNAVAILABLE = -1L;
    public static final long PREPARING = -2L;
    public static final long UNKNOWN_SIZE = -3L;
    public static final long ACCESS_FAILURE = -4L;
    public static final long LOW_STORAGE_THRESHOLD_BYTES = 50000000;
    public static final String CAMERA_SESSION_SCHEME = "camera_session";
    private static final String TAG = "CAM_Storage";
    private static final String GOOGLE_COM = "google.com";
    private static HashMap<Uri, Uri> sSessionsToContentUris = new HashMap<>();
    private static HashMap<Uri, Uri> sContentUrisToSessions = new HashMap<>();
    private static LruCache<Uri, Bitmap> sSessionsToPlaceholderBitmap =
            // 20MB cache as an upper bound for session bitmap storage
            new LruCache<Uri, Bitmap>(20 * 1024 * 1024) {
                @Override
                protected int sizeOf(Uri key, Bitmap value) {
                    return value.getByteCount();
                }
            };
    private static HashMap<Uri, Point> sSessionsToSizes = new HashMap<>();
    private static HashMap<Uri, Integer> sSessionsToPlaceholderVersions = new HashMap<>();


    public static long getAvailableSpace() {
        String state = Environment.getExternalStorageState();
        Log.d(TAG, "External storage state=" + state);
        if (Environment.MEDIA_CHECKING.equals(state)) {
            return PREPARING;
        }
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return UNAVAILABLE;
        }

        File dir = new File(DIRECTORY);
        dir.mkdirs();
        if (!dir.isDirectory() || !dir.canWrite()) {
            return UNAVAILABLE;
        }

        try {
            StatFs stat = new StatFs(DIRECTORY);
            return stat.getAvailableBlocks() * (long) stat.getBlockSize();
        } catch (Exception e) {
            Log.i(TAG, "Fail to access external storage", e);
        }
        return UNKNOWN_SIZE;
    }


}