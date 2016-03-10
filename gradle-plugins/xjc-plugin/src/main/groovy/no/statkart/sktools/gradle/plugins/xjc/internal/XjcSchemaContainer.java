package no.statkart.sktools.gradle.plugins.xjc.internal;

import groovy.lang.Closure;
import no.statkart.sktools.gradle.plugins.xjc.XjcConfig;
import org.apache.commons.lang.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.SourceSet;
import org.gradle.util.ConfigureUtil;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Leif Lislegård
 * @since 1.2
 */
public class XjcSchemaContainer extends AbstractList<XjcConfig> {
    private final List<XjcConfig> store = new ArrayList<XjcConfig>();
    private final SourceSet sourceSet;
    private final Project project;

    private final List<Action<XjcConfig>> configActions = new ArrayList<Action<XjcConfig>>();

    public XjcSchemaContainer(SourceSet sourceSet, Project project) {
        this.sourceSet = sourceSet;
        this.project = project;
    }

    protected XjcConfig create(String name) throws InvalidUserDataException {
        String schemaName = sourceSet.getName() + StringUtils.capitalize(name);
        ConfigurableFileCollection sourceFiles = project.files();
        return new XjcConfig(sourceSet, schemaName, sourceFiles);
    }

    protected XjcConfig create(String name, Closure configureClosure) throws InvalidUserDataException {
        XjcConfig xjcSchema = create(name);
        ConfigureUtil.configure(configureClosure, xjcSchema);
        return xjcSchema;
    }


    public boolean add(XjcConfig xjcSchema) {
        for (Action<XjcConfig> action : configActions) {
            action.execute(xjcSchema);
        }
        return store.add(xjcSchema);
    }

    //besørger optional lik struktur som for konfigurering
    public List<XjcConfig> getSchemas() {
        return this;
    }

    //call back funksjon for dynamisk konfigurasjon
    public void all(Action<XjcConfig> action) {
        configActions.add(action);
        if (size() > 0) {
            throw new IllegalStateException("Elements needs to be added before any actions!"); //no handling of this state
        }
    }

    //for configuration
    public XjcConfig schema(Closure configureClosure) {
        String schemaName = String.format("%dSchema", size());
        XjcConfig schema = create(schemaName, configureClosure);
        add(schema);
        return schema;
    }


    // List implementation specific methods ...

    @Override
    public XjcConfig get(int index) {
        return store.get(index);
    }

    @Override
    public int size() {
        return store.size();
    }


}