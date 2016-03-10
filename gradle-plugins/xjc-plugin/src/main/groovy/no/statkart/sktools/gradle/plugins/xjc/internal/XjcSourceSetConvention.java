package no.statkart.sktools.gradle.plugins.xjc.internal;

import groovy.lang.Closure;
import org.gradle.util.ConfigureUtil;

/**
 * Vedhengsklasse til {@link org.gradle.api.tasks.SourceSet SourceSet}
 *
 * @author Leif Lislegård
 * @since 1.2
 */
public class XjcSourceSetConvention {
    final private XjcSchemaContainer xjcSchemas;

    public XjcSourceSetConvention(XjcSchemaContainer xjcSchemas) {
        this.xjcSchemas = xjcSchemas;
    }

    public XjcSchemaContainer getXjc() {
        return xjcSchemas;
    }

    //for configuration
    public XjcSourceSetConvention xjc(Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, getXjc());
        return this;
    }
}
