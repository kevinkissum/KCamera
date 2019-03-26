package com.example.kevin.kcamera.Interface;

import com.example.kevin.kcamera.CameraActivity;

/**
 * The controller at app level.
 */
public interface ModuleController {
        /** Preview is fully visible. */
        public static final int VISIBILITY_VISIBLE = 0;
        /** Preview is covered by e.g. the transparent mode drawer. */
        public static final int VISIBILITY_COVERED = 1;
        /** Preview is fully hidden, e.g. by the filmstrip. */
        public static final int VISIBILITY_HIDDEN = 2;

        /********************** Life cycle management **********************/

        /**
         * Initializes the module.
         *
         * @param activity The camera activity.
         * @param isSecureCamera Whether the app is in secure camera mode.
         * @param isCaptureIntent Whether the app is in capture intent mode.
         */
        public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent);

        /**
         * Resumes the module. Always call this method whenever it's being put in
         * the foreground.
         */
        public void resume();

        /**
         * Pauses the module. Always call this method whenever it's being put in the
         * background.
         */
        public void pause();

        /**
         * Destroys the module. Always call this method to release the resources used
         * by this module.
         */
        public void destroy();

        /********************** UI / Camera preview **********************/

        /**
         * Called when the preview becomes visible/invisible.
         *
         * @param visible Whether the preview is visible, one of
         *            {@link #VISIBILITY_VISIBLE}, {@link #VISIBILITY_COVERED},
         *            {@link #VISIBILITY_HIDDEN}
         */
        public void onPreviewVisibilityChanged(int visibility);

        /**
         * Called when the framework layout orientation changed.
         *
         * @param isLandscape Whether the new orientation is landscape or portrait.
         */
        public void onLayoutOrientationChanged(boolean isLandscape);

        /**
         * Called when back key is pressed.
         *
         * @return Whether the back key event is processed.
         */
        public abstract boolean onBackPressed();

        /********************** App-level resources **********************/

        /**
         * Called by the app when the camera is available. The module should use
         * {@link com.android.camera.app.AppController#}
         *
         * @param cameraProxy The camera device proxy.
         */
        public void onCameraAvailable();

    }

