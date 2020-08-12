package no.statkart.sktools.gradle.plugins.xjc.internal;

import groovy.lang.Closure;
import no.statkart.sktools.gradle.plugins.xjc.XjcConfig;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.util.ConfigureUtil;

/**
 * Vedhengsklasse til {@link org.gradle.api.tasks.SourceSet SourceSet}
 *
 * @author Leif Lislegård
 * @since 1.2
 */
public class XjcSourceSetConvention {
    final private NamedDomainObjectContainer<XjcConfig> xjcSchemas;

    public XjcSourceSetConvention(NamedDomainObjectContainer<XjcConfig> xjcSchemas) {
        this.xjcSchemas = xjcSchemas;
    }

    public NamedDomainObjectContainer<XjcConfig> getXjc() {
        return xjcSchemas;
    }

    //for configuration
    public XjcSourceSetConvention xjc(Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, getXjc());
        return this;
    }
}
