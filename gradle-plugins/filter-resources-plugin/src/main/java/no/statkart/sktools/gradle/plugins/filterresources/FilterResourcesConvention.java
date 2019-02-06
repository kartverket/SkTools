package no.statkart.sktools.gradle.plugins.filterresources;

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.util.ConfigureUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Konvensjon for plugin.
 * Se {@link #filterResources(groovy.lang.Closure)} for konfigurasjon.
 *
 * @since 1.3
 * @author Leif Lislegård
 */
public class FilterResourcesConvention {
    final transient Project project;

    Map<String, Object> properties;


    FilterResourcesConvention(Project project) {
        this.project = project;
    }

    /**
     * Konfigurasjon av convention skjer her.
     */
    public Object filterResources(Closure closure) {
        return ConfigureUtil.configure(closure, this);
    }


    public void setProperties(Map<String, Object> propertiesToFilter) {
        if (properties != null) properties.clear();
        properties(propertiesToFilter);
    }


    public void properties(Map<String, Object> propertiesToFilter) {
        if (properties == null) {
            properties = new HashMap<>(propertiesToFilter);
        } else {
            properties.putAll(propertiesToFilter);
        }
    }


    Map<String, Object> getProperties() {
        return properties;
    }

}
