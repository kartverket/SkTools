package no.statkart.sktools.gradle.plugins.dbtools.database.util

import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import org.gradle.api.GradleException
import org.apache.commons.lang.StringUtils
import org.gradle.api.Task

/**
 *
 */
abstract class AbstractDatabaseConvention {

    protected final String name
    protected final Project project
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

    AbstractDatabaseConvention(Project project, String propertyPrefix, String name, String driver) {
        this.name = name
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

    public SQLTask sqlTask(Map params, String name, Closure closure = null) {
        validate()

        if (name == null || name.trim().isEmpty()) {
            throw new GradleException('name parameter not supplied for task!')
        }

        String taskName = getTaskName(name);
        SQLTask task = (SQLTask) project.tasks.add(name:taskName, type:SQLTask);
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


    /**
     * Printer viktige definerte properties
     */
    protected def addInfoTask(Project project) {
        Task infoTask = project.task("${prefix}Info") {
            description = "Viser gjeldende konfigurasjon for toolset ${name}"

            doLast {
                printInfo()
            }
        }
        getTasks().addTask('Info', infoTask)
    }

    protected void printInfo() {
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
    private Closure filterClosure = { SQLTask task ->

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

    }

    //SKIF-211
    String getEncoding() {
        Map<String, String> sysProperties = project.gradle.startParameter.getMergedSystemProperties()
        sysProperties.get('sql.file.encoding') ?: sysProperties.get('file.encoding')
    }

}