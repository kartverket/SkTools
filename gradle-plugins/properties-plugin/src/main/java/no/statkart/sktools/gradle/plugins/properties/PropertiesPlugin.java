package no.statkart.sktools.gradle.plugins.properties;

import no.statkart.sktools.gradle.plugins.properties.extension.PropertyUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;


/**
 * SKTOOLS-44: Plugin for setting og ekspandering av properties
 *
 * @since 1.3
 * @author Leif Lislegård
 */
public class PropertiesPlugin implements Plugin<ProjectInternal> {
    public static final String PROPERTY_UTILS_EXTENSION_NAME = "propertyUtils";


    @Override
    public void apply(ProjectInternal project) {
        if (project == project.getRootProject()) { //extension to all projects if applied to root
            for (Project aProject : project.getAllprojects()) {
                augmentProjectWithExtension(aProject);
            }
        } else {
            augmentProjectWithExtension(project);
        }
    }

    private static void augmentProjectWithExtension(Project project) {
        final PropertyUtils propertyUtils = new PropertyUtils(project);
        project.getLogger().debug(String.format("assigning extension %s to project %s", PROPERTY_UTILS_EXTENSION_NAME, project.getPath()));
        project.getExtensions().add(PROPERTY_UTILS_EXTENSION_NAME, propertyUtils);
    }
}
