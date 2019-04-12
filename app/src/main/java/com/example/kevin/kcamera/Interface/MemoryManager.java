package com.example.kevin.kcamera.Interface;


import java.util.HashMap;

/**
 * Keeps track of memory used by the app and informs modules and services if
 * memory gets low.
 */
public interface MemoryManager {
    /**
     * Classes implementing this interface will be able to get updates about
     * memory status changes.
     */
    public static interface MemoryListener {
        /**
         * Called when the app is experiencing a change in memory state. Modules
         * should listen to these to not exceed the available memory.
         *
         * @param state the new state, one of {@link MemoryManager#STATE_OK},
         *            {@link MemoryManager#STATE_LOW_MEMORY},
         */
        public void onMemoryStateChanged(int state);

        /**
         * Called when the system is about to kill our app due to high memory
         * load.
         */
        public void onLowMemory();
    }

    /** The memory status is OK. The app can function as normal. */
    public static final int STATE_OK = 0;

    /** The memory is running low. E.g. no new media should be captured. */
    public static final int STATE_LOW_MEMORY = 1;

    /**
     * Add a new listener that is informed about upcoming memory events.
     */
    public void addListener(MemoryListener listener);

    /**
     * Removes an already registered listener.
     */
    public void removeListener(MemoryListener listener);

    /**
     * Returns the maximum amount of memory allowed to be allocated in native
     * code by our app (in megabytes).
     */
    public int getMaxAllowedNativeMemoryAllocation();

    /**
     * Queries the memory consumed, total memory, and memory thresholds for this app.
     *
     * @return HashMap containing memory metrics keyed by string labels
     *     defined in {@link MemoryQuery}.
     */
    public HashMap queryMemory();
}
