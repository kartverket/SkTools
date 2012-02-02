package no.statkart.sktools.gradle.plugins.dbtools.database.hsqldb

import no.statkart.sktools.gradle.plugins.dbtools.database.util.AbstractDatabaseConvention
import org.gradle.api.Project

/**
 * Convention object for HSQLDB database tools
 *
 * @author Leif Lislegård
 * @since 1.0
 */
class HsqldbTasksConvention extends AbstractDatabaseConvention {

    HsqldbTasksConvention(Project project, String propertyPrefix) {

        super(project, propertyPrefix, 'org.hsqldb.jdbc.JDBCDriver')
//        super(project, propertyPrefix, 'org.hsqldb.jdbcDriver')
    }

    public HsqldbTasksConvention addTasks(String path) {
        HsqldbTasks tasks = new HsqldbTasks(path, this)
        tasks.init(project)
        return this
    }
}
