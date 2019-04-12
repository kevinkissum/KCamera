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


import com.example.kevin.kcamera.Ex.exif.ExifInterface;

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

    public static Uri addImage(ContentResolver resolver, String title, long date,
                               Location location, int orientation, ExifInterface exif, byte[] jpeg, int width,
                               int height) throws IOException {

        return addImage(resolver, title, date, location, orientation, exif, jpeg, width, height,
                FilmstripItemData.MIME_TYPE_JPEG);
    }

    public static Uri addImage(ContentResolver resolver, String title,
                               long date, Location location, int orientation, ExifInterface exif, byte[] data,
                               int width, int height, String mimeType) throws IOException {
        String path = generateFilepath(title, mimeType);
        long fileLength = writeFile(path, data, exif);
        if (fileLength >= 0) {
            return addImageToMediaStore(resolver, title, date, location, orientation, fileLength,
                    path, width, height, mimeType);
        }
        return null;
    }

    public static Uri addImageToMediaStore(ContentResolver resolver, String title, long date,
                                           Location location, int orientation, long jpegLength, String path, int width, int height,
                                           String mimeType) {
        // Insert into MediaStore.
        ContentValues values =
                getContentValuesForData(title, date, location, orientation, jpegLength, path, width,
                        height, mimeType);

        Uri uri = null;
        try {
            uri = resolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Throwable th)  {
            // This can happen when the external volume is already mounted, but
            // MediaScanner has not notify MediaProvider to add that volume.
            // The picture is still safe and MediaScanner will find it and
            // insert it into MediaProvider. The only problem is that the user
            // cannot click the thumbnail to review the picture.
            Log.e(TAG, "Failed to write MediaStore" + th);
        }
        return uri;
    }

    // Get a ContentValues object for the given photo data
    public static ContentValues getContentValuesForData(String title,
                                                        long date, Location location, int orientation, long jpegLength,
                                                        String path, int width, int height, String mimeType) {

        File file = new File(path);
        long dateModifiedSeconds = TimeUnit.MILLISECONDS.toSeconds(file.lastModified());

        ContentValues values = new ContentValues(11);
        values.put(ImageColumns.TITLE, title);
        values.put(ImageColumns.DISPLAY_NAME, title + JPEG_POSTFIX);
        values.put(ImageColumns.DATE_TAKEN, date);
        values.put(ImageColumns.MIME_TYPE, mimeType);
        values.put(ImageColumns.DATE_MODIFIED, dateModifiedSeconds);
        // Clockwise rotation in degrees. 0, 90, 180, or 270.
        values.put(ImageColumns.ORIENTATION, orientation);
        values.put(ImageColumns.DATA, path);
        values.put(ImageColumns.SIZE, jpegLength);

        setImageSize(values, width, height);

        if (location != null) {
            values.put(ImageColumns.LATITUDE, location.getLatitude());
            values.put(ImageColumns.LONGITUDE, location.getLongitude());
        }
        return values;
    }

    private static void setImageSize(ContentValues values, int width, int height) {
        // The two fields are available since ICS but got published in JB
        if (ApiHelper.HAS_MEDIA_COLUMNS_WIDTH_AND_HEIGHT) {
            values.put(MediaColumns.WIDTH, width);
            values.put(MediaColumns.HEIGHT, height);
        }
    }

    private static String generateFilepath(String title, String mimeType) {
        return generateFilepath(DIRECTORY, title, mimeType);
    }

    public static String generateFilepath(String directory, String title, String mimeType) {
        String extension = null;
        if (FilmstripItemData.MIME_TYPE_JPEG.equals(mimeType)) {
            extension = JPEG_POSTFIX;
        } else if (FilmstripItemData.MIME_TYPE_GIF.equals(mimeType)) {
            extension = GIF_POSTFIX;
        } else {
            throw new IllegalArgumentException("Invalid mimeType: " + mimeType);
        }
        return (new File(directory, title + extension)).getAbsolutePath();
    }

    private static long writeFile(String path, byte[] data) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            out.write(data);
            return data.length;
        } catch (Exception e) {
            Log.e(TAG, "Failed to write data", e);
        } finally {
            try {
                out.close();
            } catch (Exception e) {
                Log.e(TAG, "Failed to close file after write", e);
            }
        }
        return -1;
    }

    public static long writeFile(String path, byte[] jpeg, ExifInterface exif) throws IOException {
        if (!createDirectoryIfNeeded(path)) {
            Log.e(TAG, "Failed to create parent directory for file: " + path);
            return -1;
        }
        if (exif != null) {
            exif.writeExif(jpeg, path);
            File f = new File(path);
            return f.length();
        } else {
            return writeFile(path, jpeg);
        }
//        return -1;
    }

    private static boolean createDirectoryIfNeeded(String filePath) {
        File parentFile = new File(filePath).getParentFile();

        // If the parent exists, return 'true' if it is a directory. If it's a
        // file, return 'false'.
        if (parentFile.exists()) {
            return parentFile.isDirectory();
        }

        // If the parent does not exists, attempt to create it and return
        // whether creating it succeeded.
        return parentFile.mkdirs();
    }


}