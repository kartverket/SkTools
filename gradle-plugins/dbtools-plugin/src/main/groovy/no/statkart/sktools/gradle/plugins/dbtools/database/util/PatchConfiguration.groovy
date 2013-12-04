package no.statkart.sktools.gradle.plugins.dbtools.database.util

import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsConvention
import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsPlugin
import no.statkart.sktools.utils.databasepatcher.exception.ConfigurationException
import org.apache.commons.lang.StringUtils
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection
import no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch.*

/**
 *
 *
 * @author Leif Lislegård
 * @since 1.2 - SKTOOLS-33
 */
class PatchConfiguration {
    final AbstractDatabaseConvention databaseConvention

    //SKTOOLS-34: navn for komponent som skal patches
    protected String name

    //SKTOOLS-77: skjema for patchtabell (benyttes bla av systembruker der patchtabell ikke befinner seg i eget skjema)
    protected String schema

    //default metodenavn
    protected String printPatchVersionTaskName
    protected String setIndexesInSyncWithPatchTaskName
    protected String unSetIndexesInSyncWithPatchTaskName
    protected String assertPatchVersionTaskName

    protected final AbstractDatabaseTasks<PatchConfiguration> tasks = new AbstractDatabaseTasks(this) { }


    PatchConfiguration(AbstractDatabaseConvention databaseConvention) {
        this.databaseConvention = databaseConvention
    }


    //legger til std tasks dersom allerede ikke eksplisitt lagt til
    void addDefaultTasks() {
        if (!databaseConvention.getTasks().getTasks().containsKey(printPatchVersionTaskName)) {
            printPatchVersionTask([:], null, null)
        }
        if (!databaseConvention.getTasks().getTasks().containsKey(setIndexesInSyncWithPatchTaskName)) {
            setIndexesInSyncWithPatchTask([:], null, null)
        }
        if (!databaseConvention.getTasks().getTasks().containsKey(unSetIndexesInSyncWithPatchTaskName)) {
            unSetIndexesInSyncWithPatchTask([:], null, null)
        }
        if (!databaseConvention.getTasks().getTasks().containsKey(assertPatchVersionTaskName)) {
            assertPatchVersionTask([:], null, null)
        }

    }


    String getTaskName(String verb, String target = '') {
        String.format("%s%s%s", StringUtils.capitalize(verb), 'null'.equals(name) ? '' : StringUtils.capitalize(name), StringUtils.capitalize(target))
    }

    String getName() {
        return name
    }
    String getSchema() {
        return schema
    }

    /**
     * @since 1.2 - SKTOOLS-33
     */
    public PatchTask patchTask(Map params, String name, Closure closure = null) {
        configurePatchTask(params, name, 'patch', PatchTask.class, closure)
    }

    /**
     * @since 1.3 - SKTOOLS-86
     */
    public PatchTask syncPatchTask(Map params, String name, Closure closure = null) {
        SyncPatchTask task = configurePatchTask(params, name, 'syncPatch', SyncPatchTask.class, closure)
        //benytter default verdier definert av plugin..
//        task.conventionMapping.with { map 'patchTypes', { ['INDEX'] } }
        return task
    }


    /**
     * @since 1.2 - SKTOOLS-33
     */
    public PrintPatchversionTask printPatchVersionTask(Map params, String name = null, Closure closure = null) {
        if (name != null) {
            printPatchVersionTaskName = name
        }
        PrintPatchversionTask task = configureDatabasePatchTask(params, printPatchVersionTaskName, PrintPatchversionTask.class, closure)

        return task
    }

    /**
     * Task som verifiserer gitt pathcversjon for component.
     * @since 1.2 - SKTOOLS-34
     */
    public AssertPatchversionTask assertPatchVersionTask(Map params, String name, Closure closure = null) {
        if (name != null) {
            assertPatchVersionTaskName = name
        }
        AssertPatchversionTask task = configureDatabasePatchTask(params, assertPatchVersionTaskName, AssertPatchversionTask.class, closure)

        return task
    }

    /**
     * Task som setter pathcversjon for component.
     * @since 1.2 - SKTOOLS-34
     */
    public DefinePatchversionTask definePatchVersionTask(Map params = [:], String name, Closure closure = null) {
        if (name == null) {
            throw new ConfigurationException("Name is mandatory and have to be declared!")
        }
        String definePatchVersionTaskName = name

        DefinePatchversionTask task = configureDatabasePatchTask(params, definePatchVersionTaskName, DefinePatchversionTask.class, closure)

        return task
    }
    public DefinePatchversionTask definePatchVersionTask(String name, Closure closure) {
        return definePatchVersionTask([:], name, closure)
    }


    /**
     * Task som assigner siste pathcversjon.
     * @since 1.3 - SKTOOLS-87
     */
    public DefineLatestPatchVersionTask defineLatestPatchVersionTask(Map params = [:], String name, Closure closure = null) {
        if (name == null) {
            throw new ConfigurationException("Name is mandatory and have to be declared!")
        }
        String definePatchVersionTaskName = name

        DefineLatestPatchVersionTask task = configurePatchTask(params, definePatchVersionTaskName, DefineLatestPatchVersionTask.class, closure)
        return task
    }
    public DefineLatestPatchVersionTask defineLatestPatchVersionTask(String name, Closure closure) {
        return definePatchVersionTask([:], name, closure)
    }


    /**
     * @since 1.2 - SKTOOLS-33
     */
    public IndexesInSyncWithPatchTask setIndexesInSyncWithPatchTask(Map params, String name = null, Closure closure = null) {
        if (name != null) {
            setIndexesInSyncWithPatchTaskName = name
        }
        IndexesInSyncWithPatchTask task = configureDatabasePatchTask(params, setIndexesInSyncWithPatchTaskName, IndexesInSyncWithPatchTask.class, closure)
        task.conventionMapping.with {
            map 'indexesUpToDate', { Boolean.TRUE }
        }
        return task
    }

    /**
     * @since 1.2 - SKTOOLS-33
     */
    public IndexesInSyncWithPatchTask unSetIndexesInSyncWithPatchTask(Map params, String name = null, Closure closure = null) {
        if (name != null) {
            unSetIndexesInSyncWithPatchTaskName = name
        }
        IndexesInSyncWithPatchTask task = configureDatabasePatchTask(params, unSetIndexesInSyncWithPatchTaskName, IndexesInSyncWithPatchTask.class, closure)
        task.conventionMapping.with {
            map 'indexesUpToDate', { Boolean.FALSE }
        }
        return task
    }


    //registrerer opprettelse av taskSequence til dette toolset
    protected Task taskSequence(String verb, Closure config = null) {
        def taskName = getTaskName(verb)
        def task = databaseConvention.taskSequence(taskName, config)

        getTasks().addTask(verb, task)
    }
    protected Task taskSequence(Map params, String verb, Closure config = null) {
        def taskName = getTaskName(verb)
        def task = databaseConvention.taskSequence(params, taskName, config)

        getTasks().addTask(verb, task)
    }

    //registrerer opprettelse av task til dette toolset
    protected Task task(String verb, Closure config = null) {
        def taskName = getTaskName(verb)
        def task = databaseConvention.task(taskName, config)

        getTasks().addTask(verb, task)
    }
    protected Task task(Map params, String verb, Closure config = null) {
        def taskName = getTaskName(verb)
        def task = databaseConvention.task(params, taskName, config)

        getTasks().addTask(verb, task)
    }

    // util funksjoner

    private FileCollection findDbToolsDependencies() {
        DependencyUtil.getDatabasePatcherClasspath(databaseConvention.project.rootProject)
    }

    private FileCollection findJdbcDependencies() {
        Dependency[] dependenciesAsArray = dbtoolsConvention.jdbcDependencies.toArray(new Dependency[dbtoolsConvention.jdbcDependencies.size()])
//        println "files: " + databaseConvention.project.configurations[DbtoolsPlugin.DBTOOLS_CONFIGURATION].fileCollection(dependenciesAsArray).files
        databaseConvention.project.configurations[DbtoolsPlugin.DBTOOLS_CONFIGURATION].fileCollection(dependenciesAsArray)
    }

    private DbtoolsConvention getDbtoolsConvention() {
        databaseConvention.project.convention.plugins[DbtoolsPlugin.CONVENTION_NAME]
    }

    static assignConventionMappings(Project project) {
        project.tasks.withType(DatabasePatchTask.class) {
            it.conventionMapping.with {
                map 'component', { 'null' }
            }
        }

        project.tasks.withType(SyncPatchTask.class) {
            it.conventionMapping.with {
                map 'patchTypes', { ['INDEX', 'TYPE', 'PACKAGE', 'FUNCTION'] } //SKTOOLS-86
                map 'failOnWarning', { false } //SKTOOLS-86
            }
        }
    }

    PatchTask configurePatchTask(Map params, String name, String verb = 'patch', Class type, Closure closure) {
        PatchTask task = configureDatabasePatchTask(params, name, verb, type, closure)
        return task
    }

    DatabasePatchTask configureDatabasePatchTask(Map params, String target, String verb = '', Class type, Closure closure) {
        def taskName = getTaskName(verb, target)
        def task = databaseConvention.configureAbstractSQLTask(params, taskName, type, closure)
        task.conventionMapping.with {
            map 'schema', { this.getSchema() }
            map 'component', { this.getName() }
            map 'classpath', { findJdbcDependencies() + findDbToolsDependencies() }
        }
        getTasks().addTask(target, task)
    }

    public AbstractDatabaseTasks getTasks() {
        return tasks;
    }

}