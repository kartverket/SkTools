package no.statkart.sktools.gradle.plugins.weblogic;

import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.internal.IConventionAware;

/**
 * Interface for {@link org.gradle.api.internal.ConventionTask}-implementasjoner
 *
 * Pluginet setter {@code weblogicClasspath} på bakgrunn av {@link WeblogicBasePlugin#WEBLOGIC_PROVIDED_CONFIGURATION_NAME}
 *
 * @author Leif Lislegård
 */
public interface WeblogicTaskInterface extends IConventionAware {

    @InputFiles
    public FileCollection getWeblogicClasspath();

    public void setWeblogicClasspath(FileCollection weblogicClasspath);


}
