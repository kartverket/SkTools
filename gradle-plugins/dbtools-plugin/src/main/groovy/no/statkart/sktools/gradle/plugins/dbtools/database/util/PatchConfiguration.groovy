package no.statkart.sktools.gradle.plugins.dbtools.database.util

import org.gradle.api.file.FileCollection
import org.gradle.api.artifacts.Dependency
import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsConvention
import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsPlugin
import org.apache.commons.lang.StringUtils
import org.gradle.api.Project
import no.statkart.sktools.utils.databasepatcher.exception.ConfigurationException
import no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch.AssertPatchversionTask
import no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch.DefinePatchversionTask
import no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch.PrintPatchversionTask
import no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch.DatabasePatchTask
import no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch.IndexesInSyncWithPatchTask
import no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch.PatchTask
import org.gradle.api.Task

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

    /**
     * @since 1.2 - SKTOOLS-33
     */
    public PatchTask patchTask(Map params, String name, Closure closure = null) {
        PatchTask task = configureAbstractSQLTask(params, name, 'patch', PatchTask.class, closure)
        task.conventionMapping.with {
            map 'component', { this.getName() }
            map 'classpath', { findJdbcDependencies() + findDbToolsDependencies() }
        }
        return task
    }

    /**
     * @since 1.2 - SKTOOLS-33
     */
    public PrintPatchversionTask printPatchVersionTask(Map params, String name = null, Closure closure = null) {
        if (name != null) {
            printPatchVersionTaskName = name
        }
        PrintPatchversionTask task = configureAbstractSQLTask(params, printPatchVersionTaskName, PrintPatchversionTask.class, closure)
        task.conventionMapping.with {
            map 'component', { this.getName() }
            map 'classpath', { findJdbcDependencies() + findDbToolsDependencies() }
        }
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
        AssertPatchversionTask task = configureAbstractSQLTask(params, assertPatchVersionTaskName, AssertPatchversionTask.class, closure)
        task.conventionMapping.with {
            map 'component', { this.getName() }
            map 'classpath', { findJdbcDependencies() + findDbToolsDependencies() }
        }
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

        DefinePatchversionTask task = configureAbstractSQLTask(params, definePatchVersionTaskName, DefinePatchversionTask.class, closure)
        task.conventionMapping.with {
            map 'component', { this.getName() }
            map 'classpath', { findJdbcDependencies() + findDbToolsDependencies() }
        }
        return task
    }
    public DefinePatchversionTask definePatchVersionTask(String name, Closure closure) {
        return definePatchVersionTask([:], name, closure)
    }


    /**
     * @since 1.2 - SKTOOLS-33
     */
    public IndexesInSyncWithPatchTask setIndexesInSyncWithPatchTask(Map params, String name = null, Closure closure = null) {
        if (name != null) {
            setIndexesInSyncWithPatchTaskName = name
        }
        IndexesInSyncWithPatchTask task = configureAbstractSQLTask(params, setIndexesInSyncWithPatchTaskName, IndexesInSyncWithPatchTask.class, closure)
        task.conventionMapping.with {
            map 'component', { this.getName() }
            map 'classpath', { findJdbcDependencies() + findDbToolsDependencies() }
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
        IndexesInSyncWithPatchTask task = configureAbstractSQLTask(params, unSetIndexesInSyncWithPatchTaskName, IndexesInSyncWithPatchTask.class, closure)
        task.conventionMapping.with {
            map 'component', { this.getName() }
            map 'classpath', { findJdbcDependencies() + findDbToolsDependencies() }
            map 'indexesUpToDate', { Boolean.FALSE }
        }
        return task
    }


    protected Task taskSequence(Map params = [], String name, Closure config = null) {
        def taskName = getTaskName(name)
        def task = databaseConvention.taskSequence(params, taskName, config)

        getTasks().addTask(name, task)
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
    }

    AbstractSQLTask configureAbstractSQLTask(Map params, String target, String verb = '', Class type, Closure closure) {
        def taskName = getTaskName(verb, target)
        def task = databaseConvention.configureAbstractSQLTask(params, taskName, type, closure)
        getTasks().addTask(target, task)
    }

    public AbstractDatabaseTasks getTasks() {
        return tasks;
    }

}