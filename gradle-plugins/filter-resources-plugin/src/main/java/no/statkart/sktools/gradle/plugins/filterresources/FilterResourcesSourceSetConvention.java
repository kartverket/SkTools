package no.statkart.sktools.gradle.plugins.filterresources;

import groovy.lang.Closure;
import org.gradle.api.tasks.SourceSet;
import org.gradle.util.ConfigureUtil;
import org.gradle.util.GUtil;

/**
 * @author Leif Lislegård
 * @since 1.1
 */
class FilterResourcesSourceSetConvention {
    public final static String FILTER_RESOURCES_TASK_NAME_PATTERN = "filter%sResources";
    final FilterResourcesTask filterResources;


    FilterResourcesSourceSetConvention(SourceSet sourceSet, FilterResourcesTask filterResources) {
        this.filterResources = filterResources;

    }

    public FilterResourcesTask getFilterResources() {
        return filterResources;
    }


    /**
     * SKIF-173 - konfigurering av source set via denne
     * <p/>
     * {@inheritDoc}
     */
    public FilterResourcesSourceSetConvention filterResources(Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, getFilterResources());
        return this;
    }

    public String getFilterResourcesTaskName() {
        return filterResources.getName();
    }


    static public String getFilterResourcesTaskName(SourceSet sourceSet) {
        return String.format(FILTER_RESOURCES_TASK_NAME_PATTERN, getTaskBaseName(sourceSet.getName()));
    }

    /**
     * På samme måte som {@link org.gradle.api.internal.tasks.DefaultSourceSet}
     *
     * @return sourceSet navn med stor bokstav, eller tom streng for "main"-SourceSet.
     * @param name
     */
    static String getTaskBaseName(String name) {
        return name.equals(SourceSet.MAIN_SOURCE_SET_NAME) ? "" : GUtil.toCamelCase(name);
    }

}
