package no.statkart.sktools.gradle.plugins.weblogic.compile;

import org.gradle.api.internal.tasks.compile.JvmLanguageCompileSpec;
import org.gradle.api.tasks.compile.CompileOptions;

import java.io.File;

/**
 * @since 1.2
 * @author Leif Lislegård
 */
public interface WeblogicCompileSpec extends JvmLanguageCompileSpec {

    CompileOptions getCompileOptions(); //same as {@link org.gradle.api.internal.tasks.compile.JavaCompileSpec}

    Iterable<File> getWeblogicClasspath();

    void setWeblogicClasspath(Iterable<File> classpath);

    void setTempDir(File tempDir);

    File getTempDir();

}
