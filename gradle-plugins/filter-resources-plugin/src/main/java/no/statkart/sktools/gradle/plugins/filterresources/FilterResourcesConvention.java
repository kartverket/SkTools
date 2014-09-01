package no.statkart.sktools.gradle.plugins.filterresources;

import groovy.lang.Closure;
import org.gradle.api.Project;

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
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.setDelegate(this);
        return closure.call();
    }


    public void setProperties(Map<String, Object> propertiesToFilter) {
        if (properties != null) properties.clear();
        properties(propertiesToFilter);
    }


    public void properties(Map<String, Object> propertiesToFilter) {
        if (properties == null) {
            properties = new HashMap<String, Object>(propertiesToFilter);
        } else {
            properties.putAll(propertiesToFilter);
        }
    }


    Map<String, Object> getProperties() {
        return properties;
    }



    private static void logDeprecation(String oldSyntax, String newSyntax) {
        System.out.println(String.format("%s in %s is now deprecated \n\t\t-use %s instead!", oldSyntax, FilterResourcesConvention.class.getSimpleName(), newSyntax));
    }

}
