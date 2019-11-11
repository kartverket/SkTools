package no.statkart.sktools.gradle.testutils;

import org.gradle.util.GFileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class KeystoreTestutil {
    private final static String KeystorePath = "/keystore/selfsign.jks";
    public final static String KeystoreAlias = "selfsign";
    public final static String KeystorePassword = "meMyselfAndI";

    /**
     * Kopierer ut <code>kodesignering.jks</code> til targetPath.
     */
    public static File writeKodesigneringssertifikat(File destinationFile) throws IOException {
        GFileUtils.parentMkdirs(destinationFile);
        Files.copy(KeystoreTestutil.class.getResourceAsStream(KeystorePath), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return destinationFile;
    }
}
