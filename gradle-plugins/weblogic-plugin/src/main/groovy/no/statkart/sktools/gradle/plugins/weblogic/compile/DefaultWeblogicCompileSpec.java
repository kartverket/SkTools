package no.statkart.sktools.gradle.plugins.weblogic.compile;

import org.gradle.api.internal.tasks.compile.DefaultJavaCompileSpec;
import org.gradle.api.tasks.compile.CompileOptions;

import java.io.File;

/**
 * @since 1.2
 * @author Leif Lislegård
 */
public class DefaultWeblogicCompileSpec extends DefaultJavaCompileSpec implements WeblogicCompileSpec {

    /**
     * Classpath for bruk av weblogic spesifike verktøy
     */
    private Iterable<File> weblogicClasspath;

    /**
     * Mappe som kan benyttes generering av temporære filer
     */
    private File tempDir;

    /**
     * @deprecated backwards compability to grade.version < 1.2
     */
    private CompileOptions compileOptions;

    /**
     * @deprecated backwards compability to grade.version < 1.2
     */
    public CompileOptions getCompileOptions() {
        return compileOptions;
    }

    /**
     * @deprecated backwards compability to grade.version < 1.2
     */
    public void setCompileOptions(CompileOptions compileOptions) {
        this.compileOptions = compileOptions;
    }


    public DefaultWeblogicCompileSpec() {
        setCompileOptions(new CompileOptions());
    }

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
