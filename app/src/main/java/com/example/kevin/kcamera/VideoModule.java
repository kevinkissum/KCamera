package com.example.kevin.kcamera;

import com.example.kevin.kcamera.Abstract.CameraModule;
import com.example.kevin.kcamera.Ex.CameraAgent;
import com.example.kevin.kcamera.Interface.ModuleController;

public class VideoModule extends CameraModule {
    public VideoModule(CameraActivity app) {
        super(app);

    }


    @Override
    public void init(CameraActivity activity) {

    }

    @Override
    public void resume() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void onPreviewVisibilityChanged(int visibility) {

    }

    @Override
    public void onLayoutOrientationChanged(boolean isLandscape) {

    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void onCameraAvailable(CameraAgent.CameraProxy cameraProxy) {

    }

    @Override
    public void onShutterButtonFocus(boolean pressed) {

    }

    @Override
    public void onShutterButtonClick() {

    }

    @Override
    public void onShutterButtonLongPressed() {

    }
}
