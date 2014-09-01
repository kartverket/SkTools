package no.statkart.sktools.gradle.plugins.filterresources;

import groovy.lang.Closure;
import org.gradle.api.file.SourceDirectorySet;

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


    String getFilterResourcesTaskName();

}
