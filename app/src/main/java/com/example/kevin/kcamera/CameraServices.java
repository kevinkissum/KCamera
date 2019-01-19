package com.example.kevin.kcamera;

/**
 * Functionality available to all modules and services.
 */
public interface CameraServices {

    /**
     * Returns the capture session manager instance that modules use to store
     * temporary or final capture results.
     */
//    public CaptureSessionManager getCaptureSessionManager();

    /**
     * Returns the memory manager which can be used to get informed about memory
     * status updates.
     */
//    public MemoryManager getMemoryManager();

    /**
     * Returns the motion manager which senses when significant motion of the
     * camera should unlock a locked focus.
     */
//    public MotionManager getMotionManager();

    /**
     * Returns the media saver instance.
     * <p>
     * Deprecated. Use {@link #getCaptureSessionManager()} whenever possible.
     * This direct access to media saver will go away.
     */
    @Deprecated
//    public MediaSaver getMediaSaver();

    /**
     * @return A listener to be informed by events interesting for remote
     *         capture apps. Will never return null.
     */
//    public RemoteShutterListener getRemoteShutterListener();

    /**
     * @return The settings manager which allows get/set of all app settings.
     */
    public SettingsManager getSettingsManager();
}

