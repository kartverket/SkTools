package no.statkart.sktools.gradle.plugins.dbtools.testutils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class PatchTestutil {
    private final static String SimplePatch_sql = "/simple_patch.sql";

    public static File createSimplePatchFile(File destinationFile) throws IOException {
        Files.createDirectories(destinationFile.toPath().getParent());
        try (InputStream inputStream = PatchTestutil.class.getResourceAsStream(SimplePatch_sql)) {
            Files.copy(inputStream, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return destinationFile;
    }

}
