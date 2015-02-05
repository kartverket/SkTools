package no.statkart.sktools.gradle.plugins.wsdocgen.internal;

import groovy.lang.Closure;
import org.gradle.util.ConfigureUtil;

/**
 * Vedhengsklasse til {@link org.gradle.api.tasks.SourceSet SourceSet}
 *
 * @since 2.0
 * @author Leif Lislegård
 */
public class WsDocSourceSetExtension {
    private final WsDocGroupContainer groups;

    public WsDocSourceSetExtension(WsDocGroupContainer groups) {
        this.groups = groups;
    }


    //used for configuration
    public WsDocSourceSetExtension wsdoc(Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, getWsdoc());
        return this;
    }

    //used for configuration
    public WsDocGroupContainer getWsdoc() {
        return groups;
    }

    public WsDocGroupContainer getGroups() {
        return groups;
    }
}
