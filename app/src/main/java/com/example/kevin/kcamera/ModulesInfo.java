package com.example.kevin.kcamera;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.util.Log;

import com.example.kevin.kcamera.Interface.ModuleController;
import com.example.kevin.kcamera.Interface.ModuleManager;


public class ModulesInfo {

    public static final String TAG = "ModulesInfo";

    public static final String PHOTO = "PhotoModule";
    public static final String VIDEO = "VideoModule";
    // Settings namespace for all panorama/wideangle modes.
    public static final String PANORAMA = "PanoramaModule";
    public static final String REFOCUS = "RefocusModule";


    public static void setupModules(Context context, ModuleManager moduleManager) {
        Resources res = context.getResources();
        int photoModuleId = context.getResources().getInteger(R.integer.camera_mode_photo);
        registerPhotoModule(moduleManager, photoModuleId, PHOTO,
                false);
        moduleManager.setDefaultModuleIndex(photoModuleId);
        registerVideoModule(moduleManager, res.getInteger(R.integer.camera_mode_video),
                VIDEO);
//        if (PhotoSphereHelper.hasLightCycleCapture(context)) {
//            registerWideAngleModule(moduleManager, res.getInteger(R.integer.camera_mode_panorama),
//                    PANORAMA);
//            registerPhotoSphereModule(moduleManager,
//                    res.getInteger(R.integer.camera_mode_photosphere),
//                    PANORAMA);
//        }
//        if (RefocusHelper.hasRefocusCapture(context)) {
//            registerRefocusModule(moduleManager, res.getInteger(R.integer.camera_mode_refocus),
//                    REFOCUS);
//        }
//        if (GcamHelper.hasGcamAsSeparateModule(config)) {
//            registerGcamModule(moduleManager, res.getInteger(R.integer.camera_mode_gcam),
//                    PHOTO,
//                    config.getHdrPlusSupportLevel(OneCamera.Facing.BACK));
//        }
//        int imageCaptureIntentModuleId = res.getInteger(R.integer.camera_mode_capture_intent);
//        registerCaptureIntentModule(moduleManager, imageCaptureIntentModuleId,
//                PHOTO, config.isUsingCaptureModule());
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
            public ModuleController createModule(CameraActivity app, Intent handler) {
                Log.v(TAG, "EnableCaptureModule = " + enableCaptureModule);
                return new PhotoModule(app);
            }
        });
    }

    private static void registerVideoModule(ModuleManager moduleManager, final int moduleId,
                                            final String namespace) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public String getScopeNamespace() {
                return namespace;
            }

            @Override
            public ModuleController createModule(CameraActivity app, Intent handler) {
                return new VideoModule(app);
            }
        });
    }

//    private static void registerWideAngleModule(ModuleManager moduleManager, final int moduleId,
//                                                final String namespace) {
//        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
//            @Override
//            public int getModuleId() {
//                return moduleId;
//            }
//
//            @Override
//            public boolean requestAppForCamera() {
//                return true;
//            }
//
//            @Override
//            public String getScopeNamespace() {
//                return namespace;
//            }
//
//            @Override
//            public ModuleController createModule(CameraActivity app, Handler handler) {
//                return PhotoSphereHelper.createWideAnglePanoramaModule(app);
//            }
//        });
//    }
//
//    private static void registerPhotoSphereModule(ModuleManager moduleManager, final int moduleId,
//                                                  final String namespace) {
//        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
//            @Override
//            public int getModuleId() {
//                return moduleId;
//            }
//
//            @Override
//            public boolean requestAppForCamera() {
//                return true;
//            }
//
//            @Override
//            public String getScopeNamespace() {
//                return namespace;
//            }
//
//            @Override
//            public ModuleController createModule(CameraActivity app, Handler handler) {
//                return PhotoSphereHelper.createPanoramaModule(app);
//            }
//        });
//    }
//
//    private static void registerRefocusModule(ModuleManager moduleManager, final int moduleId,
//                                              final String namespace) {
//        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
//            @Override
//            public int getModuleId() {
//                return moduleId;
//            }
//
//            @Override
//            public boolean requestAppForCamera() {
//                return true;
//            }
//
//            @Override
//            public String getScopeNamespace() {
//                return namespace;
//            }
//
//            @Override
//            public ModuleController createModule(CameraActivity app, Handler handler) {
//                return RefocusHelper.createRefocusModule(app);
//            }
//        });
//    }
//
//    private static void registerGcamModule(ModuleManager moduleManager, final int moduleId,
//                                           final String namespace, final HdrPlusSupportLevel hdrPlusSupportLevel) {
//        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
//            @Override
//            public int getModuleId() {
//                return moduleId;
//            }
//
//            @Override
//            public boolean requestAppForCamera() {
//                return false;
//            }
//
//            @Override
//            public String getScopeNamespace() {
//                return namespace;
//            }
//
//            @Override
//            public ModuleController createModule(CameraActivity app, Handler handler) {
//                return GcamHelper.createGcamModule(app, hdrPlusSupportLevel);
//            }
//        });
//    }
//
//    private static void registerCaptureIntentModule(ModuleManager moduleManager, final int moduleId,
//                                                    final String namespace, final boolean enableCaptureModule) {
//        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
//            @Override
//            public int getModuleId() {
//                return moduleId;
//            }
//
//            @Override
//            public boolean requestAppForCamera() {
//                return !enableCaptureModule;
//            }
//
//            @Override
//            public String getScopeNamespace() {
//                return namespace;
//            }
//
//            @Override
//            public ModuleController createModule(CameraActivity app, Handler handler) {
//                if(enableCaptureModule) {
//                    try {
//                        return new CaptureIntentModule(app, intent, namespace);
//                    } catch (OneCameraException ignored) {
//                    }
//                }
//                return new PhotoModule(app);
//            }
//        });
//    }

}
