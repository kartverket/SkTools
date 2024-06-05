package no.statkart.sktools.gradle.plugins.dbtools.testutils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class PatchTestutil {
    private final static String SimplePatch_sql = "/simple_patch.sql";

    public static File createSimplePatchFile(File destinationFile) throws IOException {
        Path parentDir = destinationFile.toPath().getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }
        Files.copy(PatchTestutil.class.getResourceAsStream(SimplePatch_sql), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return destinationFile;
    }

}
