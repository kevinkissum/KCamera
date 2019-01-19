package com.example.kevin.kcamera;

import android.content.Context;

/**
 * Functionality available to all modules and services.
 */
public class CameraServicesImpl implements CameraServices {
    /**
     * Fast, thread safe singleton initialization.
     */
    private static class Singleton {
        private static final CameraServicesImpl INSTANCE = new CameraServicesImpl(
                AndroidContext.instance().get());
    }

    /**
     * @return a single instance of of the global camera services.
     */
    public static CameraServicesImpl instance() {
        return Singleton.INSTANCE;
    }

//    private final MediaSaver mMediaSaver;
//    private final CaptureSessionManager mSessionManager;
//    private final MemoryManagerImpl mMemoryManager;
//    private final RemoteShutterListener mRemoteShutterListener;
//    private final MotionManager mMotionManager;
    private final SettingsManager mSettingsManager;

    private CameraServicesImpl(Context context) {
//        mMediaSaver = new MediaSaverImpl(context.getContentResolver());
//        PlaceholderManager mPlaceHolderManager = new PlaceholderManager(context);
//        SessionStorageManager mSessionStorageManager = SessionStorageManagerImpl.create(context);
//
//        StackSaverFactory mStackSaverFactory = new StackSaverFactory(Storage.DIRECTORY,
//                context.getContentResolver());
//        CaptureSessionFactory captureSessionFactory = new CaptureSessionFactoryImpl(
//                mMediaSaver, mPlaceHolderManager, mSessionStorageManager, mStackSaverFactory);
//        mSessionManager = new CaptureSessionManagerImpl(
//                captureSessionFactory, mSessionStorageManager, MainThread.create());
//        mMemoryManager = MemoryManagerImpl.create(context, mMediaSaver);
//        mRemoteShutterListener = RemoteShutterHelper.create(context);
        mSettingsManager = new SettingsManager(context);

//        mMotionManager = new MotionManager(context);
    }

//    @Override
//    public CaptureSessionManager getCaptureSessionManager() {
//        return mSessionManager;
//    }
//
//    @Override
//    public MemoryManager getMemoryManager() {
//        return mMemoryManager;
//    }
//
//    @Override
//    public MotionManager getMotionManager() {
//        return mMotionManager;
//    }
//
//    @Override
//    @Deprecated
//    public MediaSaver getMediaSaver() {
//        return mMediaSaver;
//    }
//
//    @Override
//    public RemoteShutterListener getRemoteShutterListener() {
//        return mRemoteShutterListener;
//    }

    @Override
    public SettingsManager getSettingsManager() {
        return mSettingsManager;
    }
}

