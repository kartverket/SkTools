package no.statkart.sktools.gradle.plugins.webstart.util;

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.util.GUtil;

import java.io.Serializable;
import java.util.*;

/**
 * Klasse for enkel konfigurering av Dependencies.
 *
 * @author Leif Lislegård
 */
public class DependencyHelper implements Iterable<Dependency>, Serializable {
    private final static long serialVersionUID = 1L;
    private final transient DependencyHandler dependencyHandler;
    private final transient Project project;

    protected final transient List<Dependency> dependencies = new ArrayList<Dependency>();


    public DependencyHelper(Project project) {
        this.project = project;
        this.dependencyHandler = project.getDependencies();
    }

    /**
     * Legger til dependency via gradle notation. <br />
     * Enkeltstående notations kan ha en optional Closure for konfigurasjon.
     *
     * @since 1.2
     */
    public Dependency library(Object notation, Closure configureClosure) {
        return library(Arrays.asList(notation, configureClosure));
    }

    /**
     * @since 1.2
     */
    public Dependency library(Object... notations) {
        return library(GUtil.collectionize(notations));
    }

    /**
     * Legger til dependencies via gradle notation. <br />
     * Kun enkeltstående notations kan ha en optional Closure for konfigurasjon.
     *
     * Utfører tilsvarende logikk som {@link org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler#methodMissing(String, Object)}
     *
     * @since 1.2
     */
    public Dependency library(Collection notationArgs) {
        Object[] normalizedArgs = notationArgs.toArray();

        if (normalizedArgs.length == 2 && normalizedArgs[1] instanceof Closure) {
            return doAdd(normalizedArgs[0], (Closure) normalizedArgs[1]);
        } else if (normalizedArgs.length == 1) {
            return doAdd(normalizedArgs[0], (Closure) null);
        }
        for (Object notation : normalizedArgs) {
            doAdd(notation, null);
        }
        return null;
    }

    public Dependency files(Object... paths) {
        return doAdd(project.files(paths), null);
    }

    public Dependency project(Map<String, ?> notation) {
        return doAdd(dependencyHandler.project(notation), null);
    }

    public Dependency project(Map<String, ?> notation, Closure configureClosure) {
        return doAdd(dependencyHandler.project(notation), configureClosure);
    }

    public Dependency module(Object notation) {
        return doAdd(dependencyHandler.module(notation), null);
    }

    public Dependency module(Object notation, Closure configureClosure) {
        return doAdd(dependencyHandler.module(notation, configureClosure), null);
    }


    private Dependency doAdd(Object dependencyNotation, Closure configureClosure) {
        Dependency dependency = dependencyHandler.create(dependencyNotation, configureClosure);
        dependencies.add(dependency);
        return dependency;
    }

    public List<Dependency> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    public Iterator<Dependency> iterator() {
        return dependencies.iterator();
    }

    public Dependency[] toArray() {
        return dependencies.toArray(new Dependency[dependencies.size()]);
    }

    /**
     * Config clause for a collection of resources .
     */
    public DependencyHelper configure(Closure closure) {
        closure.setDelegate(this);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.call();
        return this;
    }


}
