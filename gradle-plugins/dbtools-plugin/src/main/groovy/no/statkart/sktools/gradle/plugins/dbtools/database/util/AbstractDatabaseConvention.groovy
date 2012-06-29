package no.statkart.sktools.gradle.plugins.dbtools.database.util

import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import org.gradle.api.GradleException
import org.apache.commons.lang.StringUtils

/**
 *
 */
abstract class AbstractDatabaseConvention {

    protected final Project project
    protected final Map<String, Object> properties = new Hashtable()

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

        this.credentials = new Credentials("toolset:${prefix}", properties)

        this.driver = driver

    }

    public def config(Closure closure) {
        closure.setDelegate(this)
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
   }

    public abstract AbstractDatabaseTasks getTasks()

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties.clear();
        addProperties(properties);
    }

    public void addProperties(Map<String, Object> properties) {
        for (Map.Entry<String, ?> entry : properties.entrySet()) {
            if (entry.getValue() instanceof CharSequence) {
                this.properties.put(entry.getKey(), entry.getValue().toString());
            }
        }
    }

    protected validate() {
        if (prefix == null) {
            throw new GradleException('prefix not defined for toolset!')
        }
        if (prefix == null) {
            throw new GradleException('prefix not defined for toolset!')
        }
    }

    public String getTaskName(String target) {
        if (target != null) {
            return StringUtils.uncapitalize(String.format("%s%s", prefix, StringUtils.capitalize(target)));
        }
        return null
    }

    public SQLTask sqlTask(Map params, String name, Closure closure = null) {
        validate()

        if (name == null || name.trim().isEmpty()) {
            throw new GradleException('name parameter not supplied for task!')
        }

        String taskName = getTaskName(name);
        SQLTask task = (SQLTask) project.tasks.add(name:taskName, type:SQLTask);
        task.doFirst(filterClosure)

        task.conventionMapping.with {
            map 'url', {this.url}
            map 'driver', {this.driver}
        }
        task.defaultCredentials = this.credentials
        task.useDefaultCredentials = true

        if (params.containsKey('sqlFile')) {
            params['sqlFile'] = project.file(params['sqlFile'])
//            task.sqlFile = project.file(params['sqlFile'])
//            params.remove('sqlFile')
        }
        ConfigureUtil.configureByMap(params, task)

        ConfigureUtil.configure(closure, task, false);

        getTasks().addTask(name, task)
        return task;
    }


    /**
     * Action for filtrering av sql (fil og/eller streng) satt på task
     */
    private Closure filterClosure = { SQLTask task ->

        File tempFile = null
        if (task.sqlString) {
            tempFile = project.file("${task.temporaryDir}/sqlString.sql")
            tempFile.parentFile.mkdirs()
            tempFile.createNewFile()
            tempFile.withPrintWriter {
                it.prinln task.sqlString
            }
        }

        def buildDir = "${project.buildDir}/dbtools/${prefix}/${task.name}"
        project.delete(buildDir)
        project.copy {
            from task.sqlFile, tempFile
            into buildDir
            filter([tokens: this.properties, beginToken: '@', endToken: '@'], org.apache.tools.ant.filters.ReplaceTokens)
        }

        if (task.sqlFile) {
            task.sqlFile = project.file("${buildDir}/${task.sqlFile.name}")
        }
        if (task.sqlString) {
            task.sqlString = project.file("${buildDir}/${tempFile.name}").text
        }

    }

}