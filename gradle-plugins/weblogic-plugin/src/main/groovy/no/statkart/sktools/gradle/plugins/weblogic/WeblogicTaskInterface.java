package no.statkart.sktools.gradle.plugins.weblogic;

import org.gradle.api.file.FileCollection;

/**
 * Interface for {@link org.gradle.api.internal.ConventionTask}-implementasjoner
 *
 * Pluginet setter {@code weblogicClasspath} på bakgrunn av {@link WeblogicBasePlugin#WEBLOGIC_PROVIDED_CONFIGURATION_NAME}
 *
 * @author Leif Lislegård
 */
public interface WeblogicTaskInterface {

    //PS: implementerende klasser bør annotere denne med @Classpath
    public FileCollection getWeblogicClasspath();

    public void setWeblogicClasspath(FileCollection weblogicClasspath);


}
