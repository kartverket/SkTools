package no.statkart.sktools.gradle.plugins.xjc;

import groovy.lang.Closure;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.tasks.SourceSet;
import org.gradle.util.ConfigureUtil;

/**
 * Source set som holder schema filene og konfigurasjon for xjc
 *
 * @since 1.2 - endret til å været et vedheng på sourceSet
 * @author Leif Lislegård
 */
public class XjcSchema extends DefaultSourceDirectorySet {
    private final SourceSet sourceSet;
    private final XjcConfig config;

    //optional java source for custom implementations
    private final SourceDirectorySet javaSource;

    public XjcSchema(SourceSet sourceSet, FileResolver fileResolver, String name) {
        super(name, String.format("%s%s XJC Schemas", sourceSet.getName(), name), fileResolver);
        this.sourceSet = sourceSet;
        this.config = new XjcConfig(this);
        this.javaSource = new DefaultSourceDirectorySet(getName(), getDisplayName() + " Source", fileResolver);
    }

    public XjcConfig getConfig() {
        return config;
    }

    public SourceDirectorySet getJava() {
        return javaSource;
    }

    public String getCompileXjcSchemaTaskName() {
        return sourceSet.getTaskName("compile", getName());
    }

    public String getGenerateXjcSchemaTaskName() {
        return sourceSet.getTaskName("gen", getName());
    }

    public Object getGeneratedSourcesDir() {
        return String.format("gen/%s/xjc/%s", sourceSet.getName(), getName());
    }

    public XjcSchema config(Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, getConfig());
        return this;
    }
}
