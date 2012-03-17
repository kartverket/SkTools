package no.statkart.sktools.gradle.plugins.weblogic

import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.internal.IConventionAware

/**
 *
 * @author Leif Lislegård
 */
public interface WeblogicTaskInterface extends IConventionAware {

    @InputFiles
    public FileCollection getWeblogicClasspath();

    public void setWeblogicClasspath(FileCollection weblogicClasspath);


}
