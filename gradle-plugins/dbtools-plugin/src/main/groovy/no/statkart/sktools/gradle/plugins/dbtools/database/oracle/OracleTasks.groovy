package no.statkart.sktools.gradle.plugins.dbtools.database.oracle

import org.gradle.api.Project
import no.statkart.sktools.gradle.plugins.dbtools.database.SQLTask
import no.statkart.sktools.gradle.plugins.dbtools.database.util.DatabaseTasksInterface
import no.statkart.sktools.gradle.plugins.dbtools.database.util.AbstractDatabaseConvention
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




    public static void addDefaultTools(String groupString, OracleTasksConvention conv) {

        Project project = conv.project

        conv.importTask = project.task([type: OracleImportTask], "${conv.prefix}Import") {
            group = groupString
            convention = conv
            description = 'Import av dump via Oracles eget verktøy'
        }


        conv.exportTask = project.task([type: OracleExportTask], "${conv.prefix}Export") {
            group = groupString
            convention = conv
            description = 'Export av dump via Oracles eget verktøy'
        }

        conv.infoTask = project.task("${conv.prefix}Info") {
            group = groupString
            description = 'Viser gjeldende konfigurasjon'

            doLast {
                conv.printInfo()
            }

        }
    }

}

