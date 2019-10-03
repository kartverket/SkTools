package no.statkart.sktools.gradle.plugins.webstart.util;

import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
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
    protected static Logger log = Logging.getLogger(ArtifactMatcher.class);

    /**
     * Pattern for validering av filnavn.
     * <p/>
     * Groups
     * <ol>
     * <li><b>artifaktnavn</b>
     * <li><b>versjon</b>
     * <li><b>classifier</b>
     * <li><b>type/extension</b>
     * </ol>
     * Classifier anses som del av versjon.
     */
    public static Pattern artifactPattern = Pattern.compile("^([A-Za-z]\\w*(?:(?:-|\\.)[A-Za-z]\\w*)*)-((?:(?:(?:dev)?\\d[^-]*)|(?:trunk))[^\\.]*)\\.(\\w*)$");

    public static String findImplementationVersionInManifest(File file) {

        try (JarFile jarFile = new JarFile(file)) {
            Object value = jarFile.getManifest().getMainAttributes().get(new Attributes.Name("Implementation-Version"));
            return Objects.toString(value, null);
        } catch (IOException e) {
            //dont care
        }
        return null;
    }

    private String name, version, type;

    public ArtifactMatcher(File file) {
        Matcher matcher = artifactPattern.matcher(file.getName());
        if (matcher.matches()) {
            name = matcher.group(1);
            version = matcher.group(2);
            type = matcher.group(3).toLowerCase();
        } else {
            String fileName = file.getName();
            int i = fileName.lastIndexOf('.');

            if (i < 0) {
                throw new GradleException(String.format("Unable to parse filename %s", file.getPath()));
            }

            name = fileName.substring(0, i);
            type = fileName.substring(i + 1);

            log.warn("Filename not parsable, parsing manifest for version {}", file);
            version = ArtifactMatcher.findImplementationVersionInManifest(file);
            if (version == null) {
                log.error("Could not calculate version info from file {}", file);
                version = "unknown";
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getType() {
        return type;
    }
}
