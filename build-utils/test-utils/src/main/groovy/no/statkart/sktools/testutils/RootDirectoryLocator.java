package no.statkart.sktools.testutils;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.File;

public class RootDirectoryLocator {
    private static final Logger LOGGER = Logging.getLogger(RootDirectoryLocator.class);
    private static final String SETTINGS_GRADLE = "settings.gradle";

    private static File projectDevRoot = null;

    /**
     * @return current work directory or the project root directory for local source code repository
     */
    public static File getRootDirectory() {
        if (projectDevRoot == null) {
            projectDevRoot = findRootDirectory(new File("."));
        }
        return projectDevRoot;
    }

    private static File findRootDirectory(File file) {
        File suggestedRoot;

        if(file == null) {
            return null;
        }

        LOGGER.debug("Tester file: {}", file.getAbsolutePath());
        if (!file.isDirectory()) {
            return findRootDirectory(file.getAbsoluteFile().getParentFile());
        }
        File settingsFile = new File(file, SETTINGS_GRADLE);
        if (settingsFile.exists()) {
            return file;
        } else {
            LOGGER.debug("Ingen match i directory: {}", file.getAbsolutePath());
            suggestedRoot = findRootDirectory(file.getAbsoluteFile().getParentFile());
        }

        return suggestedRoot != null ? suggestedRoot : file;
    }

}
