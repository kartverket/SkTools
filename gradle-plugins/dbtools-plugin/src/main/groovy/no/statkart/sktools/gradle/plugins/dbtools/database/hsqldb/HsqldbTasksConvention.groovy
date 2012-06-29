package no.statkart.sktools.gradle.plugins.dbtools.database.hsqldb

import no.statkart.sktools.gradle.plugins.dbtools.database.util.AbstractDatabaseConvention
import org.gradle.api.Project
import no.statkart.sktools.gradle.plugins.dbtools.database.util.AbstractDatabaseTasks

/**
 * Convention object for HSQLDB database tools
 *
 * @author Leif Lislegård
 * @since 1.0
 */
class HsqldbTasksConvention extends AbstractDatabaseConvention {

    protected HsqldbTasks tasks = new HsqldbTasks(this);

    HsqldbTasksConvention(Project project, String propertyPrefix) {

        super(project, propertyPrefix, 'org.hsqldb.jdbc.JDBCDriver')
//        super(project, propertyPrefix, 'org.hsqldb.jdbcDriver')
    }



    @Override
    HsqldbTasks getTasks() {
        return tasks;
    }


}
