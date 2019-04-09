package com.example.kevin.kcamera;


import android.graphics.Matrix;
import android.graphics.RectF;

/**
 * Transform coordinates to and from preview coordinate space and camera driver
 * coordinate space.
 */
public class CameraCoordinateTransformer {
    // http://developer.android.com/guide/topics/media/camera.html#metering-focus-areas
    private static final RectF CAMERA_DRIVER_RECT = new RectF(-1000, -1000, 1000, 1000);

    private final Matrix mCameraToPreviewTransform;
    private final Matrix mPreviewToCameraTransform;

    /**
     * Convert rectangles to / from camera coordinate and preview coordinate space.
     *
     * @param mirrorX            if the preview is mirrored along the X axis.
     * @param displayOrientation orientation in degrees.
     * @param previewRect        the preview rectangle size and position.
     */
    public CameraCoordinateTransformer(boolean mirrorX, int displayOrientation,
                                       RectF previewRect) {
        if (!hasNonZeroArea(previewRect)) {
            throw new IllegalArgumentException("previewRect");
        }

        mCameraToPreviewTransform = cameraToPreviewTransform(mirrorX, displayOrientation,
                previewRect);
        mPreviewToCameraTransform = inverse(mCameraToPreviewTransform);
    }

    private boolean hasNonZeroArea(RectF rect) {
        return rect.width() != 0 && rect.height() != 0;
    }

    private Matrix cameraToPreviewTransform(boolean mirrorX, int displayOrientation,
                                            RectF previewRect) {
        Matrix transform = new Matrix();

        // Need mirror for front camera.
        transform.setScale(mirrorX ? -1 : 1, 1);

        // Apply a rotate transform.
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        transform.postRotate(displayOrientation);

        // Map camera driver coordinates to preview rect coordinates
        Matrix fill = new Matrix();
        fill.setRectToRect(CAMERA_DRIVER_RECT,
                previewRect,
                Matrix.ScaleToFit.FILL);

        // Concat the previous transform on top of the fill behavior.
        transform.setConcat(fill, transform);

        return transform;
    }

    private Matrix inverse(Matrix source) {
        Matrix newMatrix = new Matrix();
        source.invert(newMatrix);
        return newMatrix;
    }

}