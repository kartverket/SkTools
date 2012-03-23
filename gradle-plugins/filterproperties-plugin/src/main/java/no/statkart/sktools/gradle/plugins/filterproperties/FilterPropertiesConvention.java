package no.statkart.sktools.gradle.plugins.filterproperties;

import groovy.lang.Closure;
import org.gradle.api.Project;

import java.util.HashMap;
import java.util.Map;

/**
 * Konvensjon av plugin.
 * Se {@link #filteredProperties(Closure)} for konfigurasjon av convention.
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
    public Object filteredProperties(Closure closure) {
        closure.setDelegate(this); 
        return closure.call();
    }


    public void setProperties(Map<String, Object> propertiesToFilter) {
        properties = new HashMap<String, Object>(propertiesToFilter);
    }


    public void properties(Map<String, Object> propertiesToFilter) {
        if (properties == null) {
            setProperties(propertiesToFilter);
        } else {
            properties.putAll(propertiesToFilter);
        }
    }


    /**
     * Convenient way of retrieving project properties in config clause
     */
    public Map<String, Object> projectProperties() {
        HashMap<String, Object> filteredProjectProperties = new HashMap<String, Object>();
        for (Map.Entry<String, ?> entry : project.getProperties().entrySet()) {
            if (entry.getValue() instanceof String) {
                filteredProjectProperties.put(entry.getKey(), entry.getValue());
           }
        }
        return filteredProjectProperties;
    }

    Map<String, Object> getProperties() {
        return properties;
    }



    /**
     * @depricated since 1.0 - bruk heller {@link #filteredProperties(Closure)}.
     */
    @Deprecated
    public Object statKartFilterProperties(Closure closure) {
        logDeprecation("statKartFilterProperties(Closure)", "filteredProperties(Closure)");
        return filteredProperties(closure);
    }

    private static void logDeprecation(String oldSyntax, String newSyntax) {
        System.out.println(String.format("%s in %s is now deprecated \n\t\t-use %s instead!", oldSyntax, FilterPropertiesConvention.class.getSimpleName(), newSyntax));
    }

}
