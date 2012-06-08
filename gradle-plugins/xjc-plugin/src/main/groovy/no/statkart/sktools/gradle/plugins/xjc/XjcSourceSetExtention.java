package no.statkart.sktools.gradle.plugins.xjc;

import groovy.lang.Closure;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.tasks.SourceSet;

/**
 * Klasse for utvidelse av {@link org.gradle.api.tasks.SourceSet}
 *
 * @since 1.2
 * @author Leif Lislegård
 */
class XjcSourceSetExtention {
    final private XjcSchemaContainer schemas;

    XjcSourceSetExtention(SourceSet sourceSet, FileResolver fileResolver) {
        schemas = new XjcSchemaContainer(sourceSet, fileResolver);
    }

    XjcSchemaContainer getXjc() {
        return schemas;
    }

    XjcSourceSetExtention xjc(Closure configureClosure) {
        getXjc().configure(configureClosure);
        return this;
    }
}
