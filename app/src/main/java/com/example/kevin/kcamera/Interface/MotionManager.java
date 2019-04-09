package com.example.kevin.kcamera.Interface;


import android.content.Context;

import java.util.LinkedList;

public class MotionManager {
    public static interface MotionListener {
        public void onMoving();
    }

    private final LinkedList<MotionListener> mListeners =
            new LinkedList<MotionListener>();

    public MotionManager(Context context) {
    }

    public void addListener(MotionListener listener) {
    }

    public void removeListener(MotionListener listener) {
    }

    public void reset() {
    }

    public void start() {
    }

    public void stop() {
    }

    public boolean isEnabled() {
        return false;
    }

    public void onGyroUpdate(long t, float x, float y, float z) {
    }
}
