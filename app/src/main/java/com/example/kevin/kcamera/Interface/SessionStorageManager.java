package com.example.kevin.kcamera.Interface;

import java.io.File;
import java.io.IOException;


/**
 * Interface for the session storage manager which handles management of storage
 * space that can be used for temporary session files.
 */
public interface SessionStorageManager {

    /**
     * Returns the directory that can be used for temporary sessions of a
     * specific type, defined by 'subDirectory'.
     * <p>
     * Before returning, this method will make sure the returned directory is
     * clean of expired session data.
     *
     * @param subDirectory The subdirectory to use/create within the temporary
     *            session space, e.g. "foo".
     * @return A valid file object that points to an existing directory.
     * @throws IOException If the directory could not be made available.
     */
//    public File getSessionDirectory(String subDirectory) throws IOException;

    /**
     * Initializes the directories for storing the final session output
     * temporarily before it is copied to the final location after calling
     * {@link #finish()}.
     * <p>
     * This method will make sure the directories and file exists and is
     * writeable, otherwise it will throw an exception.
     *
     * @param title the title of this session. Will be used to create a unique
     *            sub-directory.
     * @return The path to a JPEG file which can be used to write the final
     *         output to.
     * @throws IOException If the directory could not be created.
     */
//    @Nonnull
//    public File createTemporaryOutputPath(String subDirectory, String title) throws IOException;
}
