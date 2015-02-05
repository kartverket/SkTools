package no.statkart.sktools.gradle.plugins.wsdocgen.internal;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import no.statkart.sktools.gradle.plugins.wsdocgen.WsDocGenConvention;
import no.statkart.sktools.gradle.plugins.wsdocgen.WsDocGroup;
import org.apache.commons.lang.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.util.ConfigureUtil;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 * Konfigurasjon for en samling av grupper.
 *
 * @since 2.0
 */
public class WsDocGroupContainer extends AbstractList<WsDocGroup> {
    private final List<WsDocGroup> store = new ArrayList<WsDocGroup>();
    private final SourceSet sourceSet;
    private final WsDocGenConvention convention;

    private final List<Action<WsDocGroup>> configActions = new ArrayList<Action<WsDocGroup>>();

    public WsDocGroupContainer(SourceSet sourceSet, WsDocGenConvention convention) {
        this.sourceSet = sourceSet;
        this.convention = convention;
    }

    WsDocGroup create(String name) throws InvalidUserDataException {
        String docGroupName = StringUtils.capitalize(name);
        return new WsDocGroup(docGroupName, sourceSet, convention);
    }

    WsDocGroup create(String name, Closure configureClosure) throws InvalidUserDataException {
        WsDocGroup xjcSchema = create(name);
        if (configureClosure != null) {
            ConfigureUtil.configure(configureClosure, xjcSchema);
        }
        return xjcSchema;
    }


    public boolean add(WsDocGroup xjcSchema) {
        for (Action<WsDocGroup> action : configActions) {
            action.execute(xjcSchema);
        }
        return store.add(xjcSchema);
    }

    //besørger optional lik struktur som for konfigurering
    public List<WsDocGroup> getSchemas() {
        return this;
    }

    //call back funksjon for dynamisk konfigurasjon
    public void all(Action<WsDocGroup> action) {
        configActions.add(action);
        if (size() > 0) {
            throw new IllegalStateException("Add all elements before any actions!"); //no handling of this state
        }
    }


    //used for configuration
    public WsDocGroup group() {
        return group(null);
    }

    //used for configuration
    public WsDocGroup group(Closure configureClosure) {
        String schemaName = String.format("Group%d", size() + 1);
        WsDocGroup schema = create(schemaName, configureClosure);
        add(schema);
        return schema;
    }


    // List implementation specific methods ...

    @Override
    public WsDocGroup get(int index) {
        return store.get(index);
    }

    @Override
    public int size() {
        return store.size();
    }


}
