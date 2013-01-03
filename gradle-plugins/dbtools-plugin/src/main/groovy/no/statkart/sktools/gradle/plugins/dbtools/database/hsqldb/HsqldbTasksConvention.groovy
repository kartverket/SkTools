package no.statkart.sktools.gradle.plugins.dbtools.database.hsqldb

import no.statkart.sktools.gradle.plugins.dbtools.database.util.AbstractDatabaseConvention
import org.gradle.api.Project
import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsConvention

/**
 * Convention object for HSQLDB database tools
 *
 * @author Leif Lislegård
 * @since 1.0
 */
class HsqldbTasksConvention extends AbstractDatabaseConvention {

    protected HsqldbTasks tasks = new HsqldbTasks(this);

    HsqldbTasksConvention(DbtoolsConvention dbtoolsConvention, String propertyPrefix, String name) {
        super(dbtoolsConvention, propertyPrefix, name, 'org.hsqldb.jdbc.JDBCDriver')
        addInfoTask(project)
    }



    @Override
    HsqldbTasks getTasks() {
        return tasks;
    }


}
