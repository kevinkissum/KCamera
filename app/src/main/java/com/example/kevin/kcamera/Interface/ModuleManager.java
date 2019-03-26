package com.example.kevin.kcamera.Interface;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.example.kevin.kcamera.CameraActivity;

import java.util.List;

public interface ModuleManager {

    public static int MODULE_INDEX_NONE = -1;


    public static interface ModuleAgent {

        /**
         * @return The module ID.
         */
        public int getModuleId();

        /**
         * @return Whether the module will request the app for the camera.
         */
        public boolean requestAppForCamera();

        /**
         * @return A string which is used to get the namespace for settings in
         * the module scope.
         */
        public String getScopeNamespace();

        /**
         * Creates the module.
         *
         * @param app The {@link com.android.camera.app.AppController} which
         *            creates this module.
         * @param intent The {@link android.content.Intent} which starts the activity.
         * @return The module.
         */
        public ModuleController createModule(CameraActivity app, Handler intent);
    }

    /**
     * Registers a module. A module will be available only if its agent is
     * registered. The registration might fail.
     *
     * @param agent The {@link com.android.camera.app.ModuleManager.ModuleAgent}
     *              of the module.
     * @throws java.lang.NullPointerException if the {@code agent} is null.
     * @throws java.lang.IllegalArgumentException if the module ID is
     * {@code MODULE_INDEX} or another module with the sameID is registered
     * already.
     */
    void registerModule(ModuleAgent agent);

    /**
     * Unregister a module.
     *
     * @param moduleId The module ID.
     * @return Whether the un-registration succeeds.
     */
    boolean unregisterModule(int moduleId);

    /**
     * @return A {@link java.util.List} of the
     * {@link com.android.camera.app.ModuleManager.ModuleAgent} of all the
     * registered modules.
     */
    List<ModuleAgent> getRegisteredModuleAgents();

    /**
     * @return A {@link java.util.List} of the
     * {@link com.android.camera.app.ModuleManager.ModuleAgent} of all the
     * registered modules' indices.
     */
    List<Integer> getSupportedModeIndexList();

    /**
     * Sets the default module index. No-op if the module index does not exist.
     *
     * @param moduleId The ID of the default module.
     * @return Whether the {@code moduleId} exists.
     */
    boolean setDefaultModuleIndex(int moduleId);

    /**
     * @return The default module index. {@code MODULE_INDEX_NONE} if not set.
     */
    int getDefaultModuleIndex();

    /**
     * Returns the {@link com.android.camera.app.ModuleManager.ModuleAgent} by
     * the module ID.
     *
     * @param moduleId The module ID.
     * @return The agent.
     */
    ModuleAgent getModuleAgent(int moduleId);

    /**
     * Gets the mode that can be switched to from the given mode id through
     * quick switch.
     *
     * @param moduleId index of the mode to switch from
     * @param settingsManager settings manager for querying last used camera module
     * @param context the context the activity is running in
     * @return mode id to quick switch to if index is valid, otherwise returns
     *         the given mode id itself
     */
}
