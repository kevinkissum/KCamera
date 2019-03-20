package com.example.kevin.kcamera;

public class SettingsManager {
    public interface OnSettingChangedListener {
        public void onSettingChanged(SettingsManager settingsManager, String key);
    }
}
