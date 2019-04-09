package com.example.kevin.kcamera.Ex;

public class CameraCapabilities {

    /**
     * Focus modes.
     */
    public enum FocusMode {
        /**
         * Continuous auto focus mode intended for taking pictures.
         * @see {@link android.hardware.Camera.Parameters#FOCUS_MODE_AUTO}.
         */
        AUTO,
        /**
         * Continuous auto focus mode intended for taking pictures.
         * @see {@link android.hardware.Camera.Parameters#FOCUS_MODE_CONTINUOUS_PICTURE}.
         */
        CONTINUOUS_PICTURE,
        /**
         * Continuous auto focus mode intended for video recording.
         * @see {@link android.hardware.Camera.Parameters#FOCUS_MODE_CONTINUOUS_VIDEO}.
         */
        CONTINUOUS_VIDEO,
        /**
         * Extended depth of field (EDOF).
         * @see {@link android.hardware.Camera.Parameters#FOCUS_MODE_EDOF}.
         */
        EXTENDED_DOF,
        /**
         * Focus is fixed.
         * @see {@link android.hardware.Camera.Parameters#FOCUS_MODE_FIXED}.
         */
        FIXED,
        /**
         * Focus is set at infinity.
         * @see {@link android.hardware.Camera.Parameters#FOCUS_MODE_INFINITY}.
         */
        // TODO: Unsupported on API 2
        INFINITY,
        /**
         * Macro (close-up) focus mode.
         * @see {@link android.hardware.Camera.Parameters#FOCUS_MODE_MACRO}.
         */
        MACRO,
    }


    /**
     * Scene modes.
     */
    public enum SceneMode {
        /**
         * No supported scene mode.
         */
        NO_SCENE_MODE,
        /**
         * Scene mode is off.
         * @see {@link android.hardware.Camera.Parameters#SCENE_MODE_AUTO}.
         */
        AUTO,
        /**
         * Take photos of fast moving objects.
         * @see {@link android.hardware.Camera.Parameters#SCENE_MODE_ACTION}.
         */
        ACTION,
        /**
         * Applications are looking for a barcode.
         * @see {@link android.hardware.Camera.Parameters#SCENE_MODE_BARCODE}.
         */
        BARCODE,
        /**
         * Take pictures on the beach.
         * @see {@link android.hardware.Camera.Parameters#SCENE_MODE_BEACH}.
         */
        BEACH,
        /**
         * Capture the naturally warm color of scenes lit by candles.
         * @see {@link android.hardware.Camera.Parameters#SCENE_MODE_CANDLELIGHT}.
         */
        CANDLELIGHT,
        /**
         * For shooting firework displays.
         * @see {@link android.hardware.Camera.Parameters#SCENE_MODE_FIREWORKS}.
         */
        FIREWORKS,
        /**
         * Capture a scene using high dynamic range imaging techniques.
         * @see {@link android.hardware.Camera.Parameters#SCENE_MODE_HDR}.
         */
        // Note: Supported as a vendor tag on the Camera2 API for some LEGACY devices.
        HDR,
        /**
         * Take pictures on distant objects.
         * @see {@link android.hardware.Camera.Parameters#SCENE_MODE_LANDSCAPE}.
         */
        LANDSCAPE,
        /**
         * Take photos at night.
         * @see {@link android.hardware.Camera.Parameters#SCENE_MODE_NIGHT}.
         */
        NIGHT,
        /**
         * Take people pictures at night.
         * @see {@link android.hardware.Camera.Parameters#SCENE_MODE_NIGHT_PORTRAIT}.
         */
        // TODO: Unsupported on API 2
        NIGHT_PORTRAIT,
        /**
         * Take indoor low-light shot.
         * @see {@link android.hardware.Camera.Parameters#SCENE_MODE_PARTY}.
         */
        PARTY,
        /**
         * Take people pictures.
         * @see {@link android.hardware.Camera.Parameters#SCENE_MODE_PORTRAIT}.
         */
        PORTRAIT,
        /**
         * Take pictures on the snow.
         * @see {@link android.hardware.Camera.Parameters#SCENE_MODE_SNOW}.
         */
        SNOW,
        /**
         * Take photos of fast moving objects.
         * @see {@link android.hardware.Camera.Parameters#SCENE_MODE_SPORTS}.
         */
        SPORTS,
        /**
         * Avoid blurry pictures (for example, due to hand shake).
         * @see {@link android.hardware.Camera.Parameters#SCENE_MODE_STEADYPHOTO}.
         */
        STEADYPHOTO,
        /**
         * Take sunset photos.
         * @see {@link android.hardware.Camera.Parameters#SCENE_MODE_SUNSET}.
         */
        SUNSET,
        /**
         * Take photos in a theater.
         * @see {@link android.hardware.Camera.Parameters#SCENE_MODE_THEATRE}.
         */
        THEATRE,
    }

}
