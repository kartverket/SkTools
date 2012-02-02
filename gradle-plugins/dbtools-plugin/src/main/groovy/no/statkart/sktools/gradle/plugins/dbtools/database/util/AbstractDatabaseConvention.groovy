package no.statkart.sktools.gradle.plugins.dbtools.database.util

import org.gradle.api.Project

/**
 *
 */
abstract class AbstractDatabaseConvention {

    final Project project

    /**
     * Prefix for alle tasks for tilknyttet denne konvensjonen
     */
    final String prefix

    public Credentials credentials

    /** kan settes via {@link #config(Closure) config closure} definert i prosjekt */
    public String driver

    /** kan settes via {@link #config(Closure) config closure} definert i prosjekt */
    public String url

    AbstractDatabaseConvention(Project project, String propertyPrefix, String driver) {
        this.project = project
        this.prefix = propertyPrefix

        this.credentials = new Credentials(project)

        this.driver = driver

    }

    public def config(Closure closure) {
        closure.setDelegate(this)
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
   }


}