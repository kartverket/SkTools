package no.statkart.sktools.gradle.plugins.filterresources;

import groovy.lang.Closure;
import org.gradle.api.tasks.SourceSet;
import org.gradle.util.ConfigureUtil;

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


    /**
     * @see #FILTER_RESOURCES_TASK_NAME_PATTERN
     */
    static public String getFilterResourcesTaskName(SourceSet sourceSet) {
        return sourceSet.getTaskName("filter", "Resources");
    }
}
