package no.statkart.sktools.gradle.plugins.xjc;

import groovy.lang.Closure;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.util.ConfigureUtil;

/**
 * Source set som holder schema filene og konfigurasjon for xjc
 *
 * @since 1.2 - endret til å været et vedheng på sourceSet
 * @author Leif Lislegård
 */
public class XjcSourceDirectorySet extends DefaultSourceDirectorySet {
    private final XjcConfig config;

    //optional java source for custom implementations
    private final SourceDirectorySet javaSource;

    XjcSourceDirectorySet(String name, FileResolver fileResolver) {
        super(name, String.format("%s XJC Schemas", name), fileResolver);
        this.config = new XjcConfig(this);
        this.javaSource = new DefaultSourceDirectorySet(getName(), getDisplayName() + " Source", fileResolver);
    }

    public XjcConfig getConfig() {
        return config;
    }

    public SourceDirectorySet getJava() {
        return javaSource;
    }

    public XjcSourceDirectorySet config(Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, getConfig());
        return this;
    }
}
