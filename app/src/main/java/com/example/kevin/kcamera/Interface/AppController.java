package com.example.kevin.kcamera.Interface;

import com.example.kevin.kcamera.ButtonManager;

public interface AppController {
    public ButtonManager getButtonManager();

    /**
     * Checks whether the shutter is enabled.
     */
    public boolean isShutterEnabled();

    void setShutterEnabled(boolean enabled);
}
