package com.example.kevin.kcamera.Interface;

import android.graphics.SurfaceTexture;

public interface IPhotoUIStatusListener {

    public void onPreviewUIReady(SurfaceTexture surface, int width, int height);
}
