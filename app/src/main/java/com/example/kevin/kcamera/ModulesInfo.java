package com.example.kevin.kcamera;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;

/**
 * Created by kevin on 2019/1/16.
 */

public class ModulesInfo {
    private static final String TAG = "ModulesInfo";

    public static void setupModules(Context context, ModuleManager moduleManager,
                                    OneCameraFeatureConfig config) {
        Resources res = context.getResources();
        int photoModuleId = context.getResources().getInteger(R.integer.camera_mode_photo);
        registerPhotoModule(moduleManager, photoModuleId, SettingsScopeNamespaces.PHOTO,
                /*config.isUsingCaptureModule()*/false);
        moduleManager.setDefaultModuleIndex(photoModuleId);
//        registerVideoModule(moduleManager, res.getInteger(R.integer.camera_mode_video),
//                SettingsScopeNamespaces.VIDEO);
//        if (PhotoSphereHelper.hasLightCycleCapture(context)) {
//            registerWideAngleModule(moduleManager, res.getInteger(R.integer.camera_mode_panorama),
//                    SettingsScopeNamespaces.PANORAMA);
//            registerPhotoSphereModule(moduleManager,
//                    res.getInteger(R.integer.camera_mode_photosphere),
//                    SettingsScopeNamespaces.PANORAMA);
//        }
//        if (RefocusHelper.hasRefocusCapture(context)) {
//            registerRefocusModule(moduleManager, res.getInteger(R.integer.camera_mode_refocus),
//                    SettingsScopeNamespaces.REFOCUS);
//        }
//        if (GcamHelper.hasGcamAsSeparateModule(config)) {
//            registerGcamModule(moduleManager, res.getInteger(R.integer.camera_mode_gcam),
//                    SettingsScopeNamespaces.PHOTO,
//                    config.getHdrPlusSupportLevel(OneCamera.Facing.BACK));
//        }
//        int imageCaptureIntentModuleId = res.getInteger(R.integer.camera_mode_capture_intent);
//        registerCaptureIntentModule(moduleManager, imageCaptureIntentModuleId,
//                SettingsScopeNamespaces.PHOTO, config.isUsingCaptureModule());
    }

    private static void registerPhotoModule(ModuleManager moduleManager, final int moduleId,
                                            final String namespace, final boolean enableCaptureModule) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {

            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                // The PhotoModule requests the old app camere, while the new
                // capture module is using OneCamera. At some point we'll
                // refactor all modules to use OneCamera, then the new module
                // doesn't have to manage it itself.
                return !enableCaptureModule;
            }

            @Override
            public String getScopeNamespace() {
                return namespace;
            }

            @Override
            public ModuleController createModule(AppController app, Intent intent) {
                Log.v(TAG, "EnableCaptureModule = " + enableCaptureModule);
                return /*enableCaptureModule ? new CaptureModule(app) :*/ new PhotoModule(app);
            }
        });
    }
}
