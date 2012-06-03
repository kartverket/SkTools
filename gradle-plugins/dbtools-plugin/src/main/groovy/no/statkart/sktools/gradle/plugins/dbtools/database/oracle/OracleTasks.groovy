package no.statkart.sktools.gradle.plugins.dbtools.database.oracle

import org.gradle.api.Project
import no.statkart.sktools.gradle.plugins.dbtools.database.util.AbstractDatabaseTasks
import org.gradle.api.Task

/**
 * Tasks for Oracle databases
 *
 *
 * @author Leif Lislegård
 * @since 1.0
 */
class OracleTasks extends AbstractDatabaseTasks<OracleTasksConvention> {

    OracleTasks(String relativePath, OracleTasksConvention conv) {
        super(relativePath, conv)
    }




    public void addDefaultTools(String groupString) {

        Project project = convention.project

        if (tasks.containsKey('import')) return;

        Task importTask = project.task([type: OracleImportTask], "${convention.prefix}Import") {
            group = groupString
            it.convention = getConvention()
            description = 'Import av dump via Oracles eget verktøy'
        }
        tasks.put('import', importTask)


        Task exportTask = project.task([type: OracleExportTask], "${convention.prefix}Export") {
            group = groupString
            it.convention = getConvention()
            description = 'Export av dump via Oracles eget verktøy'
        }
        tasks.put('export', exportTask)

        Task infoTask = project.task("${convention.prefix}Info") {
            group = groupString
            description = 'Viser gjeldende konfigurasjon'

            doLast {
                convention.printInfo()
            }
        }
        tasks.put('info', infoTask)

    }

}

