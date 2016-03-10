package no.statkart.sktools.gradle.plugins.filterresources;

import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

import java.io.File;

/**
 * Plugin extension for {@link org.gradle.api.tasks.SourceSetOutput}
 *
 * @author Leif Lislegård
 */
class FilterResourcesSourceSetOutputConvention {
    final FilterResourcesTask filterResources;
    final Project project;
    private String displayName;


    FilterResourcesSourceSetOutputConvention(FilterResourcesTask filterResources, SourceSet sourceSet, Project project) {
        this.project = project;
        this.filterResources = filterResources;
        displayName = String.format("%s filtered output", sourceSet.getName());
    }

    /**
     * Configures the path for filtered resources output.
     *
     * @since 1.2 - SKIF-173
     * @return this
     */
    public FilterResourcesSourceSetOutputConvention filterResourcesOutput(Object filteredResourcesDir) {
        filterResources.setDestinationDir(project.file(filteredResourcesDir));
        return this;
    }

    /**
     * @since 1.2 - SKIF-173
     * @return output dir for filtered resources
     */
    public File getFilterResourcesOutputDir() {
        return filterResources.getDestinationDir();
    }


    @Override
    public String toString() {
        return displayName;
    }
}
