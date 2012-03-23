package no.statkart.sktools.gradle.plugins.filterproperties;

import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.tasks.SourceSet;
import org.gradle.util.ConfigureUtil;
import org.gradle.util.GUtil;

/**
 * @since 1.1
 * @author Leif Lislegård
 */
public class DefaultFilterResourcesSourceSet implements FilterResourcesSourceSet {
    private final SourceDirectorySet unfilteredResources;
    private final String name;


    public DefaultFilterResourcesSourceSet(String name, FileResolver fileResolver) {
        this.name = name;
        String displayName = String.format("%s Unfiltered Resources", GUtil.toWords(StringUtils.capitalize(name)));
        unfilteredResources = new DefaultSourceDirectorySet(displayName, fileResolver);

    }

    public SourceDirectorySet getUnfilteredResources() {
        return unfilteredResources;
    }


    /**
     * todo: dersom man ønsker å bygge inn støtte for konfigurering av slike source må dette implementeres.
     * Den beste måten er trolig å registrere en FilterResourcesSourceSetManager som holder styr på hvilke {@link FilterResourcesSourceSet} som er blitt registrert.
     * Denne kan registreres via {@code Project#getConvention().getPlugins().put("filterResources", filterResourcesSourceSetManager)}
     *
     */
    public FilterResourcesSourceSet filterResources(Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, getUnfilteredResources());
        return this;
    }

    public String getFilterResourcesTaskName() {
        return String.format(FILTER_RESOURCES_TASK_NAME_PATTERN, getTaskBaseName());
    }

    /**
     * På samme måte som {@link org.gradle.api.internal.tasks.DefaultSourceSet}
     * @return sourceSet navn med stor bokstav, eller tom streng for "main"-SourceSet.
     */
    private String getTaskBaseName() {
        return name.equals(SourceSet.MAIN_SOURCE_SET_NAME) ? "" : GUtil.toCamelCase(name);
    }
}
