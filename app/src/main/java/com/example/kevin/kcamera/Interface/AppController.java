package com.example.kevin.kcamera.Interface;

import android.content.Context;

import com.example.kevin.kcamera.ButtonManager;

public interface AppController {
    public ButtonManager getButtonManager();

    /**
     * Checks whether the shutter is enabled.
     */
    public boolean isShutterEnabled();

    void setShutterEnabled(boolean enabled);

    void onPreviewStarted();

    public CameraServices getServices();

    public CameraProvider getCameraProvider();


    public Context getAndroidContext();


}
