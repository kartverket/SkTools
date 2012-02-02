package no.statkart.sktools.gradle.plugins.dbtools.database.hsqldb

import no.statkart.sktools.gradle.plugins.dbtools.database.util.DatabaseTasksInterface
import no.statkart.sktools.gradle.plugins.dbtools.database.util.AbstractDatabaseTasks
import org.gradle.api.Project

/**
 * Tasks for HSQLDB databases
 */
class HsqldbTasks extends AbstractDatabaseTasks<HsqldbTasksConvention> {

    HsqldbTasks(String relativePath, HsqldbTasksConvention convention) {
        super(relativePath, convention)
    }
}
