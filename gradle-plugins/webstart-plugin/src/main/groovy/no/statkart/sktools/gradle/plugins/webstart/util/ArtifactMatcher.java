package no.statkart.sktools.gradle.plugins.webstart.util;

import org.gradle.api.GradleException;

import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hjelpeklasse for å finne navn og versjon utifra filnavn til jar-biblioteker.
 *
 * @author Leif Lislegård
 * @author Tor Egil R. Strand
 */
public class ArtifactMatcher {

    /**
     * Pattern for validering av filnavn.
     * <p>
     * Groups
     * <ol>
     *     <li><b>artifaktnavn</b>
     *     <li><b>versjon</b>
     *     <li><b>classifier</b>
     *     <li><b>type/extension</b>
     * </ol>
     * Classifier anses som del av versjon.
     */
    public static Pattern artifactPattern = Pattern.compile("^([A-Za-z]\\w*(?:(?:-|\\.)[A-Za-z]\\w*)*)-((?:(?:\\d[^-]*)|(?:trunk))[^\\.]*)\\.(\\w*)$");


    public static String getArtifactVersion(File file) {
        checkArtifactMatcher(file);
        Matcher matcher = artifactPattern.matcher(file.getName());

        if (matcher.matches()) {
            return matcher.group(2);
        } else {
            throw new GradleException(String.format("Unable to extract artifact-version from filename %s", file.getPath()));
        }
    }


    public static String getArtifactName(File file) {
        checkArtifactMatcher(file);
        Matcher matcher = artifactPattern.matcher(file.getName());

        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            throw new GradleException(String.format("Unable to extract artifact-name from filename %s", file.getPath()));
        }
    }

    public static String getArtifactType(File file) {
        checkArtifactMatcher(file);
        Matcher matcher = artifactPattern.matcher(file.getName());

        if (matcher.matches()) {
            return matcher.group(3).toLowerCase();
        } else {
            throw new GradleException(String.format("Unable to extract artifact-type from filename %s", file.getPath()));
        }
    }


    public static boolean parsableFileName(File file) {
        return artifactPattern.matcher(file.getName()).matches();
    }

    private static void checkArtifactMatcher(File file) {
        if (!parsableFileName(file)) {
            throw new GradleException(String.format("Unable to parse filename %s", file.getPath()));
        }
    }


    public static String findImplementationVersionInManifest(File file) {

        try {
            Object value = new JarFile(file).getManifest().getMainAttributes().get(new Attributes.Name("Implementation-Version"));
            return value != null ? value.toString() : null;
        } catch (IOException e) {
            //dont care
        }
        return null;
    }

}
