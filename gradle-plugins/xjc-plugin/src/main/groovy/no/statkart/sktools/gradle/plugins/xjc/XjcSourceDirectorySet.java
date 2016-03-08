package no.statkart.sktools.gradle.plugins.xjc;

import groovy.lang.Closure;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.tasks.SourceSet;
import org.gradle.util.ConfigureUtil;

/**
 * Source set som holder schema filene og konfigurasjon for xjc
 *
 * @author Leif Lislegård
 * @since 1.2 - endret til å været et vedheng på sourceSet
 */
public class XjcSourceDirectorySet extends DefaultSourceDirectorySet {
    private final XjcConfig config;

    public XjcSourceDirectorySet(String name, SourceSet sourceSet, FileResolver fileResolver) {
        super(name, String.format("%s XJC Schemas", name), fileResolver);
        this.config = new XjcConfig(this, sourceSet);
    }

    public XjcConfig getConfig() {
        return config;
    }

    public XjcSourceDirectorySet config(Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, getConfig());
        return this;
    }
}
