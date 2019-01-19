package com.example.kevin.kcamera;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

class SettingsManager {

    private final Object mLock;
    private final Context mContext;
    private final String mPackageName;
    private final SharedPreferences mDefaultPreferences;
    private SharedPreferences mCustomPreferences;
//    private final DefaultsStore mDefaultsStore = new DefaultsStore();

    public static final String MODULE_SCOPE_PREFIX = "_preferences_module_";
    public static final String CAMERA_SCOPE_PREFIX = "_preferences_camera_";

    public SettingsManager(Context context) {
        mLock = new Object();
        mContext = context;
        mPackageName = mContext.getPackageName();

        mDefaultPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }
//
//    /**
//     * Retrieve a setting's value as an Integer, manually specifying
//     * a default value.
//     */
//    public int getInteger(String scope, String key, Integer defaultValue) {
//        synchronized (mLock) {
//            String defaultValueString = Integer.toString(defaultValue);
//            String value = getString(scope, key, defaultValueString);
//            return convertToInt(value);
//        }
//    }
//
//    /**
//     * Retrieve a setting's value as an Integer, converting the default value
//     * stored in the DefaultsStore.
//     */
//    public int getInteger(String scope, String key) {
//        synchronized (mLock) {
//            return getInteger(scope, key, getIntegerDefault(key));
//        }
//    }
//
//
//    /**
//     * Retrieve a setting's value as a String, manually specifiying
//     * a default value.
//     */
//    public String getString(String scope, String key, String defaultValue) {
//        synchronized (mLock) {
//            SharedPreferences preferences = getPreferencesFromScope(scope);
//            try {
//                return preferences.getString(key, defaultValue);
//            } catch (ClassCastException e) {
//                Log.w(TAG, "existing preference with invalid type, removing and returning default", e);
//                preferences.edit().remove(key).apply();
//                return defaultValue;
//            }
//        }
//    }
//
//    /**
//     * Retrieve a setting's value as a String, using the default value
//     * stored in the DefaultsStore.
//     */
//    @Nullable
//    public String getString(String scope, String key) {
//        synchronized (mLock) {
//            return getString(scope, key, getStringDefault(key));
//        }
//    }

}
