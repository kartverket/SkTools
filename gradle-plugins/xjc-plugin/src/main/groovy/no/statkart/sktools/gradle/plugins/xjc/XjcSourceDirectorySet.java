package no.statkart.sktools.gradle.plugins.xjc;

import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.tasks.SourceSet;

/**
 * Source set som holder schema filene og konfigurasjon for xjc
 *
 * @author Leif Lislegård
 * @since 1.2 - endret til å været et vedheng på sourceSet
 */
public class XjcSourceDirectorySet extends DefaultSourceDirectorySet {
    public XjcSourceDirectorySet(String name, FileResolver fileResolver) {
        super(name, String.format("%s XJC Schemas", name), fileResolver);
    }

}
