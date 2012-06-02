package no.statkart.sktools.gradle.plugins.weblogic.compile;

import org.gradle.api.internal.tasks.compile.DefaultJavaCompileSpec;
import org.gradle.api.tasks.compile.CompileOptions;

import java.io.File;

/**
 * @since 1.2
 * @author Leif Lislegård
 */
public class DefaultWeblogicCompileSpec extends DefaultJavaCompileSpec implements WeblogicCompileSpec {

    private Iterable<File> weblogicClasspath;
    private File tempDir;

    @Override
    public Iterable<File> getWeblogicClasspath() {
        return weblogicClasspath;
    }

    @Override
    public void setWeblogicClasspath(Iterable<File> classpath) {
        weblogicClasspath = classpath;
    }

    public File getTempDir() {
        return tempDir;
    }

    public void setTempDir(File tempDir) {
        this.tempDir = tempDir;
    }
}
