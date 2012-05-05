package no.statkart.sktools.gradle.plugins.filterproperties;

import groovy.lang.Closure;
import org.gradle.api.file.SourceDirectorySet;

import java.io.File;

/**
 * @since 1.1
 * @author Leif Lislegård
 */
public interface FilterResourcesSourceSet {

    String FILTER_RESOURCES_TASK_NAME_PATTERN = "filter%sResources";

    /**
     * Returns the non-Java resources which are to be filtered into the class output directory.
     *
     * @return the resources. Never returns null.
     */
    SourceDirectorySet getFilterResources();

    /**
     * Configures the FilteredResource source for this set.
     *
     * <p>The given closure is used to configure the {@link SourceDirectorySet} which contains the Unfiltered resources source.
     *
     * @param configureClosure The closure to use to configure the source set.
     * @return this
     */
    FilterResourcesSourceSet filterResources(Closure configureClosure);


    /**
     * Configures the path for filtered resources output.
     *
     * @since 1.2 - SKIF-173
     * @return this
     */
    FilterResourcesSourceSet filterResourcesOutput(Object path);

    /**
     * @since 1.2 - SKIF-173
     * @return output dir for filtered resources
     */
    File getFilterResourcesOutputDir();

    String getFilterResourcesTaskName();
}
