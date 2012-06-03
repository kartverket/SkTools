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


}

