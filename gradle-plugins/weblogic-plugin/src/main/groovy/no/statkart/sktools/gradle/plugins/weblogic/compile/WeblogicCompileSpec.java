package no.statkart.sktools.gradle.plugins.weblogic.compile;

import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.compile.CompileOptions;

import java.io.File;

/**
 * @since 1.2
 * @author Leif Lislegård
 */
public interface WeblogicCompileSpec {

    CompileOptions getCompileOptions();

    Iterable<File> getWeblogicClasspath();

    void setWeblogicClasspath(Iterable<File> classpath);

    File getTempDir();

    void setTempDir(File tempDir);

//    File getWorkingDir();
//
//    void setWorkingDir(File workingDir);

    File getDestinationDir();

    void setDestinationDir(File destinationDir);

    FileCollection getSource();

    void setSource(FileCollection source);

    Iterable<File> getClasspath();

    void setClasspath(Iterable<File> classpath);

    String getSourceCompatibility();

    void setSourceCompatibility(String sourceCompatibility);

    String getTargetCompatibility();

    void setTargetCompatibility(String version);

}
