package no.statkart.sktools.gradle.plugins.dbtools.testutils;

import org.gradle.util.GFileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class PatchTestutil {
    private final static String SimplePatch_sql = "/simple_patch.sql";

    public static File createSimplePatchFile(File destinationFile) throws IOException {
        GFileUtils.parentMkdirs(destinationFile);
        Files.copy(PatchTestutil.class.getResourceAsStream(SimplePatch_sql), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return destinationFile;
    }

}
