package no.statkart.sktools.gradle.plugins.dbtools.database.util

import org.gradle.api.file.FileCollection
import org.gradle.api.artifacts.Dependency
import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsConvention
import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsPlugin
import org.apache.commons.lang.StringUtils

/**
 *
 *
 * @author Leif Lislegård
 * @since 1.2 - SKTOOLS-33
 */
class PatchConfiguration {
    private final AbstractDatabaseConvention databaseConvention

    //SKTOOLS-34: navn for komponent som skal patches
    protected String name

    //default metodenavn
    protected String printPatchVersionTaskName,
                     setIndexesInSyncWithPatchTaskName,
                     unSetIndexesInSyncWithPatchTaskName

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
    }


    String getPatchTaskName(String verb) {
        String.format("%s%s", StringUtils.capitalize(verb), 'null'.equals(name) ? '' : StringUtils.capitalize(name))
    }

    String getName() {
        return name
    }

    /**
     * @since 1.2 - SKTOOLS-33
     */
    public PatchTask patchTask(Map params, String name, Closure closure = null) {
        PatchTask task = databaseConvention.configureAbstractSQLTask(params, getPatchTaskName(name), PatchTask.class, closure)
        task.conventionMapping.with {
            map 'component', { getName() }
            map 'classpath', { findJdbcDependencies() + findDbToolsDependencies() }
        }
        return task
    }

    /**
     * @since 1.2 - SKTOOLS-33
     */
    public PrintPatchversionTask printPatchVersionTask(Map params, String name = null, Closure closure = null) {
        if (name != null) {
            printPatchVersionTaskName = getPatchTaskName(name)
        }
        PrintPatchversionTask task = databaseConvention.configureAbstractSQLTask(params, printPatchVersionTaskName, PrintPatchversionTask.class, closure)
        task.conventionMapping.with {
            map 'component', { getName() }
            map 'classpath', { findJdbcDependencies() + findDbToolsDependencies() }
        }
        return task
    }

    /**
     * @since 1.2 - SKTOOLS-33
     */
    public IndexesInSyncWithPatchTask setIndexesInSyncWithPatchTask(Map params, String name = null, Closure closure = null) {
        if (name != null) {
            setIndexesInSyncWithPatchTaskName = getPatchTaskName(name)
        }
        IndexesInSyncWithPatchTask task = databaseConvention.configureAbstractSQLTask(params, setIndexesInSyncWithPatchTaskName, IndexesInSyncWithPatchTask.class, closure)
        task.conventionMapping.with {
            map 'component', { getName() }
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
            unSetIndexesInSyncWithPatchTaskName = getPatchTaskName(name)
        }
        IndexesInSyncWithPatchTask task = databaseConvention.configureAbstractSQLTask(params, unSetIndexesInSyncWithPatchTaskName, IndexesInSyncWithPatchTask.class, closure)
        task.conventionMapping.with {
            map 'classpath', { findJdbcDependencies() + findDbToolsDependencies() }
            map 'indexesUpToDate', { Boolean.FALSE }
        }
        return task
    }



    // util funksjoner

    private FileCollection findDbToolsDependencies() {
        DependencyUtil.getDatabasePatcherClasspath(databaseConvention.project.rootProject)
    }

    private FileCollection findJdbcDependencies() {
        databaseConvention.project.configurations.detachedConfiguration(dbtoolsConvention.jdbcDependencies.toArray(new Dependency[dbtoolsConvention.jdbcDependencies.size()]))
    }

    private DbtoolsConvention getDbtoolsConvention() {
        databaseConvention.project.convention.plugins[DbtoolsPlugin.CONVENTION_NAME]
    }

}