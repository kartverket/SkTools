package no.statkart.sktools.gradle.plugins.filterproperties;

import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.tasks.SourceSet;
import org.gradle.util.ConfigureUtil;
import org.gradle.util.GUtil;

import java.io.File;

/**
 * @since 1.1
 * @author Leif Lislegård
 */
public class DefaultFilterResourcesSourceSet implements FilterResourcesSourceSet {
    final String name;
    final SourceDirectorySet filterResources;
    final FileResolver fileResolver;
    Object outputPath;


    public DefaultFilterResourcesSourceSet(String name, FileResolver fileResolver) {
        this.name = name;
        String displayName = String.format("%s Unfiltered Resources", GUtil.toWords(StringUtils.capitalize(name)));
        this.filterResources = new DefaultSourceDirectorySet(displayName, fileResolver);
        this.fileResolver = fileResolver;
    }

    public SourceDirectorySet getFilterResources() {
        return filterResources;
    }


    /**
     * SKIF-173 - konfigurering av source set via denne
     *
     * {@inheritDoc}
     */
    public FilterResourcesSourceSet filterResources(Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, getFilterResources());
        return this;
    }

    public String getFilterResourcesTaskName() {
        return String.format(FilterResourcesSourceSet.FILTER_RESOURCES_TASK_NAME_PATTERN, getTaskBaseName());
    }

    /**
     * På samme måte som {@link org.gradle.api.internal.tasks.DefaultSourceSet}
     * @return sourceSet navn med stor bokstav, eller tom streng for "main"-SourceSet.
     */
    private String getTaskBaseName() {
        return name.equals(SourceSet.MAIN_SOURCE_SET_NAME) ? "" : GUtil.toCamelCase(name);
    }


}
