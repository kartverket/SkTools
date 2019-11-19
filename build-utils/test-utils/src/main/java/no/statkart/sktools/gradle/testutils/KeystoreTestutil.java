package no.statkart.sktools.gradle.testutils;

import org.gradle.util.GFileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class KeystoreTestutil {
    public final static String KeystoreAlias = "selfsign";
    public final static String KeystorePassword = "meMyselfAndI";

    public enum KeystoreType {
        JKS("/keystore/selfsign.jks"),
        P12("/keystore/selfsign.p12");

        final String value;
        KeystoreType(String value) {
            this.value = value;
        }
    }

    /**
     * Kopierer ut <code>kodesignering.jks</code> til targetPath.
     */
    public static File writeKodesigneringssertifikat(KeystoreType type, File destinationFile) throws IOException {
        GFileUtils.parentMkdirs(destinationFile);
        Files.copy(KeystoreTestutil.class.getResourceAsStream(type.value), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return destinationFile;
    }
}
