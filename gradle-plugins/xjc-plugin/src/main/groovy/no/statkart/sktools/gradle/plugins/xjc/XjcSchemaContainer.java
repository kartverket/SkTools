package no.statkart.sktools.gradle.plugins.xjc;

import groovy.lang.Closure;

import java.util.*;

import groovy.lang.GroovyObjectSupport;
import org.apache.commons.lang.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.tasks.SourceSet;
import org.gradle.util.ConfigureUtil;

/**
 * @since 1.2
 * @author Leif Lislegård
 */
public class XjcSchemaContainer extends AbstractList<XjcSourceDirectorySet> {
    private final List<XjcSourceDirectorySet> store = new ArrayList<XjcSourceDirectorySet>();
    private final SourceSet sourceSet;
    private final FileResolver fileResolver;

    private final List<Action<XjcSourceDirectorySet>> configActions = new ArrayList<Action<XjcSourceDirectorySet>>();

    XjcSchemaContainer(SourceSet sourceSet, FileResolver fileResolver) {
        this.sourceSet = sourceSet;
        this.fileResolver = fileResolver;
    }

    protected XjcSourceDirectorySet create(String name) throws InvalidUserDataException {
        String schemaName = sourceSet.getName() + StringUtils.capitalize(name);
        return new XjcSourceDirectorySet(schemaName, fileResolver);
    }

    protected XjcSourceDirectorySet create(String name, Closure configureClosure) throws InvalidUserDataException {
        XjcSourceDirectorySet xjcSchema = create(name);
        ConfigureUtil.configure(configureClosure, xjcSchema);
        return xjcSchema;
    }


    public boolean add(XjcSourceDirectorySet xjcSchema) {
        for (Action<XjcSourceDirectorySet> action : configActions) {
            action.execute(xjcSchema);
        }
        return store.add(xjcSchema);
    }

    //besørger optional lik struktur som for konfigurering
    public List<XjcSourceDirectorySet> getSchemas() {
        return this;
    }

    //call back funksjon for dynamisk konfigurasjon
    void all(Action<XjcSourceDirectorySet> action) {
        configActions.add(action);
        if (size() > 0) {
            throw new RuntimeException("State not yet implemented!");
        }
    }

    XjcSchemaContainer configure(final Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, new GroovyObjectSupport() {

            public XjcSourceDirectorySet schema(Closure configureClosure) {
                String schemaName = String.format("%sSchema", size());
                XjcSourceDirectorySet schema = create(schemaName, configureClosure);
                add(schema);
                return schema;
            }

        });

        return this;
    }

    public String getCompileXjcSchemaTaskName(XjcSourceDirectorySet schema) {
        return sourceSet.getTaskName("compile", schema.getName());
    }

    public String getGenerateXjcSchemaTaskName(XjcSourceDirectorySet schema) {
        return sourceSet.getTaskName("gen", schema.getName());
    }

    public String getGeneratedSourcesDir(XjcSourceDirectorySet schema) {
        return String.format("gen/%s/xjc/%s", sourceSet.getName(), schema.getName());
    }


    // List implementation specific methods ...

    @Override
    public XjcSourceDirectorySet get(int index) {
        return store.get(index);
    }

    @Override
    public int size() {
        return store.size();
    }


}