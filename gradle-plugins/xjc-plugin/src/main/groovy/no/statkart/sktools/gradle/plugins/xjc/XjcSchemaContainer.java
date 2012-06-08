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
public class XjcSchemaContainer extends AbstractList<XjcSchema> {
    private final List<XjcSchema> store = new ArrayList<XjcSchema>();
    private final SourceSet sourceSet;
    private final FileResolver fileResolver;

    private final List<Action<XjcSchema>> configActions = new ArrayList<Action<XjcSchema>>();

    XjcSchemaContainer(SourceSet sourceSet, FileResolver fileResolver) {
        this.sourceSet = sourceSet;
        this.fileResolver = fileResolver;
    }

    protected XjcSchema create(String name) throws InvalidUserDataException {
        String schemaName = sourceSet.getName() + StringUtils.capitalize(name);
        return new XjcSchema(sourceSet, fileResolver, schemaName);
    }

    protected XjcSchema create(String name, Closure configureClosure) throws InvalidUserDataException {
        XjcSchema xjcSchema = create(name);
        ConfigureUtil.configure(configureClosure, xjcSchema);
        return xjcSchema;
    }


    public boolean add(XjcSchema xjcSchema) {
        for (Action<XjcSchema> action : configActions) {
            action.execute(xjcSchema);
        }
        return store.add(xjcSchema);
    }

    //besørger optional lik struktur som for konfigurering
    public List<XjcSchema> getSchemas() {
        return this;
    }

    //call back funksjon for dynamisk konfigurasjon
    void all(Action<XjcSchema> action) {
        configActions.add(action);
        if (size() > 0) {
            throw new RuntimeException("State not yet implemented!");
        }
    }

    XjcSchemaContainer configure(final Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, new GroovyObjectSupport() {

            public XjcSchema schema(Closure configureClosure) {
                String schemaName = String.format("%sSchema", size());
                XjcSchema schema = create(schemaName, configureClosure);
                add(schema);
                return schema;
            }

        });

        return this;
    }

    // List implementation spesific methods ...

    @Override
    public XjcSchema get(int index) {
        return store.get(index);
    }

    @Override
    public int size() {
        return store.size();
    }


}