package com.example.kevin.kcamera.Interface;

import android.net.Uri;

import java.io.File;

/**
 * Used to store images that belong to the same stack.
 */
public interface StackSaver {

    /**
     * Save a single image from a stack/burst.
     *
     * @param inputImagePath the input image for the image.
     * @param title the title of this image, without the file extension
     * @param width the width of the image in pixels
     * @param height the height of the image in pixels
     * @param imageOrientation the image orientation in degrees
     * @param captureTimeEpoch the capture time in millis since epoch
     * @param mimeType the mime type of the image
     * @return The Uri of the saved image, or null, of the image could not be
     *         saved.
     */
    public Uri saveStackedImage(File inputImagePath, String title, int width, int height,
                                int imageOrientation, long captureTimeEpoch, String mimeType);
}
