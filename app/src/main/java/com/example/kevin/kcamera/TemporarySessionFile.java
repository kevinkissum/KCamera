package com.example.kevin.kcamera;


import android.support.annotation.Nullable;

import com.example.kevin.kcamera.Interface.SessionStorageManager;

import java.io.File;
import java.io.IOException;


/**
 * Used to create temporary session files to be used by e.g. Photo Sphere to
 * write into.
 * <p>
 * This file also handles the correct creation of the file and makes sure that
 * it is available for writing into from e.g. native code.
 */
public class TemporarySessionFile {

    private final SessionStorageManager mSessionStorageManager;
    private final String mSessionDirectory;
    private final String mTitle;

    @Nullable
    private File mFile;

    public TemporarySessionFile(SessionStorageManager sessionStorageManager, String
            sessionDirectory, String title) {
        mSessionStorageManager = sessionStorageManager;
        mSessionDirectory = sessionDirectory;
        mTitle = title;
        mFile = null;
    }

    /**
     * Creates the file and all the directories it is in if necessary.
     * <p>
     * If file was prepared successfully, additional calls to this method will
     * be no-ops and 'true' will be returned.
     *
     * @return Whether the file could be created and is ready to be written to.
     */
//    public synchronized boolean prepare() {
//        if (mFile != null) {
//            return true;
//        }
//
//        try {
//            mFile = mSessionStorageManager.createTemporaryOutputPath(mSessionDirectory, mTitle);
//        } catch (IOException e) {
//            return false;
//        }
//        return true;
//    }

    /**
     * @return Whether the file has been created and is usable.
     */
    public synchronized boolean isUsable() {
        return mFile != null;
    }

    /**
     * @return The file or null, if {@link #prepare} has not be called yet or
     *         preparation failed.
     */
    @Nullable
    public synchronized File getFile() {
        return mFile;
    }

}
