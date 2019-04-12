package com.example.kevin.kcamera.Abstract;

import com.example.kevin.kcamera.Interface.AppController;
import com.example.kevin.kcamera.Interface.CameraProvider;
import com.example.kevin.kcamera.Interface.CameraServices;
import com.example.kevin.kcamera.Interface.ModuleController;

public abstract class CameraModule implements ModuleController {

    public static final int PREVIEW_STOPPED = 0;
    public static final int IDLE = 1;  // preview is active
    // Focus is in progress. The exact focus state is in Focus.java.
    public static final int FOCUSING = 2;
    public static final int SNAPSHOT_IN_PROGRESS = 3;
    // Switching between cameras.
    public static final int SWITCHING_CAMERA = 4;

//    private final CameraServices mServices;
//    private final CameraProvider mCameraProvider;
//    protected float mMinRatio = 1.0f;
//    protected float mMaxRatio;
//
//    public CameraModule(AppController app) {
//        mServices = app.getServices();
//        mCameraProvider = app.getCameraProvider();
//    }

}
