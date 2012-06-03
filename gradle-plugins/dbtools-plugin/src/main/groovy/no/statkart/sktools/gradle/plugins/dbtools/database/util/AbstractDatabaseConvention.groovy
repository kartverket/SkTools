package no.statkart.sktools.gradle.plugins.dbtools.database.util

import org.gradle.api.Project

/**
 *
 */
abstract class AbstractDatabaseConvention {

    protected final Project project

    /**
     * Prefix for alle tasks for tilknyttet denne konvensjonen
     */
    public final String prefix

    public final Credentials credentials

    /** kan settes via {@link #config(Closure) config closure} definert i prosjekt */
    public String driver

    /** kan settes via {@link #config(Closure) config closure} definert i prosjekt */
    public String url

    AbstractDatabaseConvention(Project project, String propertyPrefix, String driver) {
        this.project = project
        this.prefix = propertyPrefix

        this.credentials = new Credentials(project, "toolset:${prefix}")

        this.driver = driver

    }

    public def config(Closure closure) {
        closure.setDelegate(this)
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
   }

    public abstract AbstractDatabaseTasks getTasks()

    public abstract void addDefaultTasks(String groupString)


}