package no.statkart.sktools.gradle.plugins.dbtools.database.util

import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsConvention
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.util.ConfigureUtil
import org.gradle.util.GUtil

/**
 * Felles funksjonalitet for toolsets
 */
abstract class AbstractDatabaseConvention {
    public static final String TOOLSET_PROPERTIES = "toolsetProperties"

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
        if (!this.properties.containsKey(key)) {
            this.properties.put(key, value);
            if (project.logger.isInfoEnabled()) {
                project.logger.info("setting dbToolSets[${name != '' ? name : "''"}] property ${key} to ${value}")
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
        if (target == null) {
            return null;
        }
        //kan ikke bruke GUtil.toLowerCamelCase(prefix + ' ' + target) da target kan inneholde "." og andre spesialtegn (ikke anbefalt, men støttet)
        if (prefix != null && !prefix.isEmpty()) {
            return GUtil.toLowerCamelCase(prefix) + capitalize(target);
        } else {
            return uncapitalize(target);
        }
    }

    //beholder gammel oppførsel for navngivning (punktum "." beholdes)
    static String capitalize(String self) {
        if (self == null || self.isEmpty() || Character.isUpperCase(self.charAt(0))) {
            return self;
        }
        return String.valueOf(Character.toUpperCase(self.charAt(0))) + self.subSequence(1, self.length());
    }

    static String uncapitalize(String self) {
        if (self == null || self.isEmpty() || Character.isLowerCase(self.charAt(0))) {
            return self;
        }
        return String.valueOf(Character.toLowerCase(self.charAt(0))) + self.subSequence(1, self.length());
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

        ConfigureUtil.configure(closure, patch);

        patch.addDefaultTasks();

        getPatch().put(patch.name, new PatchUtil(patch));
    }


    public SQLTask sqlTask(Map params, String name, Closure closure = null) {
        SQLTask task = configureAbstractSQLTask(params, name, SQLTask.class, closure)
        return task
    }

    AbstractSQLTask configureAbstractSQLTask(Map params, String name, Class<? extends AbstractSQLTask> type, Closure closure) {
        validate()

        if (name == null || name.trim().isEmpty()) {
            throw new GradleException('name parameter not supplied for task!')
        }

        String taskName = getTaskName(name);
        AbstractSQLTask task = project.getTasks().create(taskName, type)

        task.getExtensions().add(TOOLSET_PROPERTIES, Collections.unmodifiableMap(this.properties))

        task.conventionMapping.with {
            map 'url', { this.url }
            map 'driver', { this.driver }
        }
        task.defaultCredentials = this.credentials
        task.useDefaultCredentials = true

        if (params.containsKey('sqlFile')) {
            params['sqlFile'] = project.file(params['sqlFile'])
        }
        ConfigureUtil.configureByMap(params, task)
        ConfigureUtil.configure(closure, task);

        getTasks().addTask(name, task)
        return task;
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

