package no.statkart.sktools.gradle.plugins.filterproperties;

import org.gradle.api.tasks.*;

import java.util.Collections;
import java.util.Map;

/**
 * @since 1.1
 * @author Leif Lislegård
 */
public class FilterResourcesTask extends Copy {
    private Map<String, String> properties;

    public FilterResourcesTask() {
        super();
    }

    @Override
    protected void copy() {
        filter(Collections.singletonMap("tokens", getProperties()), org.apache.tools.ant.filters.ReplaceTokens.class);
        super.copy();
    }

    @Input
    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

}
