package no.statkart.sktools.gradle.plugins.weblogic.compile;

import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.compile.CompileOptions;

import javax.inject.Inject;
import java.io.File;

/**
 * @since 1.2
 * @author Leif Lislegård
 */
public class DefaultWeblogicCompileSpec implements WeblogicCompileSpec {

    /**
     * Classpath for bruk av weblogic spesifike verktøy
     */
    private Iterable<File> weblogicClasspath;

    /**
     * Classpath for kompilering av kildekode
     */
    private Iterable<File> classpath;

    /**
     * Mappe som kan benyttes generering av temporære filer
     */
    private File tempDir;

    private CompileOptions compileOptions;

//    private File workingDir;
    private File destinationDir;
    private FileCollection source;

    private String sourceCompatibility;
    private String targetCompatibility;


    @Inject
    public DefaultWeblogicCompileSpec(CompileOptions compileOptions) {
        setCompileOptions(compileOptions);
    }

    public CompileOptions getCompileOptions() {
        return compileOptions;
    }

    public void setCompileOptions(CompileOptions compileOptions) {
        this.compileOptions = compileOptions;
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

    public String getSourceCompatibility() {
        return sourceCompatibility;
    }

    public void setSourceCompatibility(String sourceCompatibility) {
        this.sourceCompatibility = sourceCompatibility;
    }

    public Iterable<File> getClasspath() {
        return classpath;
    }

    public void setClasspath(Iterable<File> classpath) {
        this.classpath = classpath;
    }

    public File getDestinationDir() {
        return destinationDir;
    }

    public void setDestinationDir(File destinationDir) {
        this.destinationDir = destinationDir;
    }

    public FileCollection getSource() {
        return source;
    }

    public void setSource(FileCollection source) {
        this.source = source;
    }

    public String getTargetCompatibility() {
        return targetCompatibility;
    }

    public void setTargetCompatibility(String targetCompatibility) {
        this.targetCompatibility = targetCompatibility;
    }
}
