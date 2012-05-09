package no.statkart.sktools.gradle.plugins.filterproperties;

import groovy.lang.Closure;
import no.statkart.sktools.gradle.plugins.filterproperties.extention.PropertyUtils;
import org.gradle.api.Project;

import java.util.HashMap;
import java.util.Map;

/**
 * Konvensjon for plugin.
 * Se {@link #filterProperties(Closure)} for konfigurasjon.
 *
 * <p>
 * <p>
 * For eksempler på konfigurasjon, se {@link FilterPropertiesPluginTest#testConfiguration()}
 * 
 * 
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 */
public class FilterPropertiesConvention {
    final Project project;

    Map<String, Object> properties;


    FilterPropertiesConvention(Project project) {
        this.project = project;
    }

    /**
     * Konfigurasjon av convention skjer her.
     */
    public Object filterProperties(Closure closure) {
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


    /**
     * Convenient way of retrieving project properties in config clause
     * @deprecated since 1.2
     */
    @Deprecated
    public Map<String, Object> projectProperties() {
        logDeprecation("projectProperties()", String.format("project.%s.projectProperties()", FilterPropertiesPlugin.PROPERTY_UTILS_EXTENTION_NAME));
        return ((PropertyUtils)project.getExtensions().getByName(FilterPropertiesPlugin.PROPERTY_UTILS_EXTENTION_NAME)).projectProperties();
    }

    Map<String, Object> getProperties() {
        return properties;
    }


    /**
     * @depricated since 1.2 - bruk heller {@link #filterProperties(Closure)}.
     */
    @Deprecated
    public Object filteredProperties(Closure closure) {
        logDeprecation("filteredProperties(Closure)", "filterProperties(Closure)");
        return filterProperties(closure);
    }

    /**
     * @depricated since 1.0 - bruk heller {@link #filterProperties(Closure)}.
     */
    @Deprecated
    public Object statKartFilterProperties(Closure closure) {
        logDeprecation("statKartFilterProperties(Closure)", "filterProperties(Closure)");
        return filterProperties(closure);
    }

    private static void logDeprecation(String oldSyntax, String newSyntax) {
        System.out.println(String.format("%s in %s is now deprecated \n\t\t-use %s instead!", oldSyntax, FilterPropertiesConvention.class.getSimpleName(), newSyntax));
    }

}
