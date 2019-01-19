package com.example.kevin.kcamera;

/**
 * String namespaces to define a set of common settings in a scope. For instance
 * most 'Camera mode' related modules will store and retrieve settings with a
 * {@link #PHOTO} namespace.
 */
public final class SettingsScopeNamespaces {
    // Settings namespace for all typical photo modes (PhotoModule,
    // CaptureModule, CaptureIntentModule, etc.).
    public static final String PHOTO = "PhotoModule";
    public static final String VIDEO = "VideoModule";
    // Settings namespace for all panorama/wideangle modes.
    public static final String PANORAMA = "PanoramaModule";
    public static final String REFOCUS = "RefocusModule";
}
