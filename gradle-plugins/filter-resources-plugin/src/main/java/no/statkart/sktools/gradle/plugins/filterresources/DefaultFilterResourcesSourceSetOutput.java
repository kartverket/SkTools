package no.statkart.sktools.gradle.plugins.filterresources;

import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

import java.io.File;

/**
 * Plugin extension for {@link org.gradle.api.tasks.SourceSetOutput}
 *
 * @author Leif Lislegård
 */
class DefaultFilterResourcesSourceSetOutput implements FilterResourcesSourceSetOutput {
    final Project project;
    Object filteredResourcesDir;
    private String displayName;


    public DefaultFilterResourcesSourceSetOutput(SourceSet sourceSet, Project project) {
        this.project = project;
        displayName = String.format("%s filtered output", sourceSet.getName());
    }

    @Override
    public FilterResourcesSourceSetOutput filterResourcesOutput(Object filteredResourcesDir) {
        this.filteredResourcesDir = filteredResourcesDir;
        return this;
    }

    @Override
    public File getFilterResourcesOutputDir() {
        return project.file(filteredResourcesDir);
    }


    @Override
    public String toString() {
        return displayName;
    }
}
