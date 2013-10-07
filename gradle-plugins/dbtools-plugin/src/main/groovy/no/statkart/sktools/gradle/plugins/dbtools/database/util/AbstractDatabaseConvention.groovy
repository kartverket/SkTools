package no.statkart.sktools.gradle.plugins.dbtools.database.util

import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import org.gradle.api.GradleException
import org.apache.commons.lang.StringUtils
import org.gradle.api.Task

import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsConvention

/**
 * Felles funksjonalitet for toolsets
 */
abstract class AbstractDatabaseConvention {

    protected final DbtoolsConvention dbtoolsConvention

    protected final String name
    protected final Map<String, Object> properties = new HashMap<String, Object>() // HashMap allows null values

    /**
     * Prefix for alle tasks for tilknyttet denne konvensjonen
     */
    public final String prefix

    public final Credentials credentials

    /** kan settes via {@link #config(Closure) config closure} definert i prosjekt */
    public String driver

    /** kan settes via {@link #config(Closure) config closure} definert i prosjekt */
    public String url

    AbstractDatabaseConvention(DbtoolsConvention dbtoolsConvention, String propertyPrefix, String name, String driver) {
        this.dbtoolsConvention = dbtoolsConvention
        this.name = name
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

    protected Project getProject() {
        return dbtoolsConvention.project
    }

    /**
     * SKTOOLS-32: Mulighet til referering av toolset ifra andre scope
     * @return referanse til toolset som blir konfigurert
     */
    public AbstractDatabaseConvention getToolset() {
        return this;
    }

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

    protected Object property(String key) {
        return this.properties.get(key)
    }

    protected void addPropertyIfNotExist(String key, Object value) {
        if (!this.properties.containsKey(key.toString())) {
            this.properties.put(key.toString(), value);
            project.logger.info "setting dbToolSets[${name != '' ? name : "''"}] property ${key} to ${value}"
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

    /**
     * @since 1.2 - SKTOOLS-34
     */
    private final HashMap<String, PatchUtil> patch = new HashMap<String, PatchUtil>();
    public HashMap<String, PatchUtil> getPatch() {
        return patch;
    }

    /**
     * @since 1.2 - SKTOOLS-33
     */
    public def patch(Closure closure) {
        PatchConfiguration patch = new PatchConfiguration(this);
        patch.name = 'null' //for bakoverkompabilitet, se PatchInfo#DEFAULT_MODULE
        patch.printPatchVersionTaskName = 'PrintPatchVersion';
        patch.setIndexesInSyncWithPatchTaskName = 'SetIndexInSyncWithPatch';
        patch.unSetIndexesInSyncWithPatchTaskName = 'UnSetIndexInSyncWithPatch';
        patch.assertPatchVersionTaskName = 'AssertPatchVersion';

        ConfigureUtil.configure(closure, patch, false);

        patch.addDefaultTasks();

        getPatch().put(patch.name, new PatchUtil(patch));
    }




    public SQLTask sqlTask(Map params, String name, Closure closure = null) {
        SQLTask task = configureAbstractSQLTask(params, name, SQLTask.class, closure)
        return task
    }

    AbstractSQLTask configureAbstractSQLTask(Map params, String name, Class type, Closure closure) {
        validate()

        if (name == null || name.trim().isEmpty()) {
            throw new GradleException('name parameter not supplied for task!')
        }

        String taskName = getTaskName(name);
        AbstractSQLTask task = (AbstractSQLTask) project.task(type:type, taskName);
        task.doFirst(filterClosure)

        task.conventionMapping.with {
            map 'url', { this.url }
            map 'driver', { this.driver }
            map 'encoding', { this.getEncoding() }
        }
        task.defaultCredentials = this.credentials
        task.useDefaultCredentials = true

        if (params.containsKey('sqlFile')) {
            params['sqlFile'] = project.file(params['sqlFile'])
        }
        ConfigureUtil.configureByMap(params, task)

        ConfigureUtil.configure(closure, task, false);

        getTasks().addTask(name, task)
        return task;
    }


    public void printInfo() {
        80.times {project.print '*'}; project.println ''

        println "${this.class.simpleName} for \"${prefix}\":"
        println "  url -> ${url}"
        println "  driver -> ${driver}"
        if (credentials.hasUsername())
            println "  credentials.username -> ${credentials.username}"
        if (credentials.hasPassword())
            println "  credentials.password -> ${credentials.password}"

        println ''

        this.properties.sort().each { key, value ->
            println "  ${key} -> ${value}"
        }

        80.times {project.print '*'}; project.println ''
    }



    /**
     * Action for filtrering av sqlFile satt på task
     */
    private Closure filterClosure = { AbstractSQLTask task ->

        if (task.sqlFile) {
            def buildDir = "${project.buildDir}/dbtools/${prefix}/${task.name}"


            //work around until GRADLE-1267
            task.ant.copy(file: task.getSqlFile(), tofile: "${buildDir}/${task.getSqlFile().name}", encoding: task.getEncoding(), overwrite: true)
            {
                filterchain {
                    replaceTokens {
                        this.properties.each { key, value ->
                            token(key: key, value: value)
                        }
                    }
                }
            }

//            project.delete(buildDir)
//            project.copy {
//                from task.getSqlFile()
//                into buildDir
//                filter([tokens: this.properties, beginToken: '@', endToken: '@'], org.apache.tools.ant.filters.ReplaceTokens)
//            }


            task.sqlFile = project.file("${buildDir}/${task.sqlFile.name}")
        }

        if (task instanceof SQLTask && task.getSqlString()) {
            String sqlString = task.getSqlString()
            this.properties.each { key, value ->
                sqlString = sqlString.replaceAll("@${key}@", value.toString())
            }
            task.setSqlString(sqlString)
        }

    }

    //SKTOOLS-21
    String getEncoding() {
        Map<String, String> sysProperties = new HashMap<String, String>();
        sysProperties.putAll((Map) System.getProperties());
        sysProperties.putAll(project.gradle.startParameter.getSystemPropertiesArgs());

        sysProperties.get('sql.file.encoding') ?: sysProperties.get('file.encoding')
    }

    //registrerer opprettelse av sequence-task til dette toolset
    protected Task taskSequence(String verb, Closure config = null) {
        def taskName = getTaskName(verb)
        def task = dbtoolsConvention.taskSequence(taskName, config)

        getTasks().addTask(verb, task)
    }
    protected Task taskSequence(Map params, String verb, Closure config = null) {
        def taskName = getTaskName(verb)
        def task = dbtoolsConvention.taskSequence(params, taskName, config)

        getTasks().addTask(verb, task)
    }

    //registrerer opprettelse av task til dette toolset
    protected Task task(String verb, Closure config = null) {
        def taskName = getTaskName(verb)
        def task = project.task(taskName, config)

        getTasks().addTask(verb, task)
    }
    protected Task task(Map params, String verb, Closure config = null) {
        def taskName = getTaskName(verb)
        def task = project.task(params, taskName, config)

        getTasks().addTask(verb, task)
    }

}

