package com.example.kevin.kcamera;


import java.util.HashMap;

/**
 * A class for storing default values and possible values of
 * SharedPreferences settings.  It is optional to store defaults
 * and possible values for a setting.  If a default is not specified,
 * the SettingsManager API chooses a default based on the type
 * requested:
 *
 * <ul>getString: default is null</ul>
 * <ul>getInteger: default is 0</ul>
 * <ul>getBoolean: default is false</ul>
 *
 * If possible values aren't specified for a
 * SharedPreferences key, then calling getIndexOfCurrentValue
 * and setValueByIndex will throw an IllegalArgumentException.
 */
class DefaultsStore {

    /**
     * A class for storing a default value and set of possible
     * values.  Since all settings values are saved as Strings in
     * SharedPreferences, the default and possible values are
     * Strings.  This simplifies default values management.
     */
    private static class Defaults {
        private String mDefaultValue;
        private String[] mPossibleValues;

        public Defaults(String defaultValue, String[] possibleValues) {
            mDefaultValue = defaultValue;
            mPossibleValues = possibleValues;
        }

        public String getDefaultValue() {
            return mDefaultValue;
        }

        public String[] getPossibleValues() {
            return mPossibleValues;
        }
    }

    /** Map of Defaults for SharedPreferences keys. */
    private static HashMap<String, Defaults> mDefaultsInternalStore =
            new HashMap<String, Defaults>();

    /**
     * Store a default value and a set of possible values
     * for a SharedPreferences key.
     */
    public void storeDefaults(String key, String defaultValue, String[] possibleValues) {
        Defaults defaults = new Defaults(defaultValue, possibleValues);
        mDefaultsInternalStore.put(key, defaults);
    }

    /**
     * Get the default value for a SharedPreferences key,
     * if one has been stored.
     */
    public String getDefaultValue(String key) {
        Defaults defaults = mDefaultsInternalStore.get(key);
        if (defaults == null) {
            return null;
        }
        return defaults.getDefaultValue();
    }

    /**
     * Get the set of possible values for a SharedPreferences key,
     * if a set has been stored.
     */
    public String[] getPossibleValues(String key) {
        Defaults defaults = mDefaultsInternalStore.get(key);
        if (defaults == null) {
            return null;
        }
        return defaults.getPossibleValues();
    }
}