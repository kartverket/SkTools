package no.statkart.sktools.gradle.plugins.xjc;

import groovy.lang.Closure;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.tasks.SourceSet;

/**
 * Vedhengsklasse til {@link org.gradle.api.tasks.SourceSet}
 *
 * @since 1.2
 * @author Leif Lislegård
 */
class XjcSourceSetExtension {
    final private XjcSchemaContainer xjcSchemas;

    XjcSourceSetExtension(XjcSchemaContainer xjcSchemas) {
        this.xjcSchemas = xjcSchemas;
    }

    public XjcSchemaContainer getXjc() {
        return xjcSchemas;
    }

    public XjcSourceSetExtension xjc(Closure configureClosure) {
        getXjc().configure(configureClosure);
        return this;
    }
}
