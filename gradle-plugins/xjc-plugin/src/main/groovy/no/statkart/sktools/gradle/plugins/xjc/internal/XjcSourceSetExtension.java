package no.statkart.sktools.gradle.plugins.xjc.internal;

import groovy.lang.Closure;
import org.gradle.util.ConfigureUtil;

/**
 * Vedhengsklasse til {@link org.gradle.api.tasks.SourceSet SourceSet}
 *
 * @since 1.2
 * @author Leif Lislegård
 */
public class XjcSourceSetExtension {
    final private XjcSchemaContainer xjcSchemas;

    XjcSourceSetExtension(XjcSchemaContainer xjcSchemas) {
        this.xjcSchemas = xjcSchemas;
    }

    public XjcSchemaContainer getXjc() {
        return xjcSchemas;
    }

    //for configuration
    public XjcSourceSetExtension xjc(Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, getXjc());
        return this;
    }
}
