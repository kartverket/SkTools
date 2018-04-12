package no.statkart.sktools.gradle.plugins.dbtools.database.oracle

import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsConvention
import no.statkart.sktools.gradle.plugins.dbtools.database.util.AbstractDatabaseConvention
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Task
import org.gradle.api.plugins.ExtraPropertiesExtension

import static org.gradle.api.Task.TASK_TYPE

/**
 * Convention object for Oracle database tools
 *
 *
 * @author Leif Lislegård
 * @since 1.0
 */
class OracleTasksConvention extends AbstractDatabaseConvention {

    protected OracleTasks tasks = new OracleTasks(this);

    OracleTasksConvention(DbtoolsConvention dbtoolsConvention, String propertyPrefix, String name) {
        super(dbtoolsConvention, propertyPrefix, name, 'oracle.jdbc.OracleDriver')

        ExtraPropertiesExtension ext = project.getExtensions().getExtraProperties()

        url = project.properties[propertyPrefix + 'db_jdbc_url']
        driver = ext.getProperties().get(propertyPrefix + 'db_jdbc_driver', this.driver)

        // setter konvensjonelle verdier
        project.afterEvaluate {

            // passord er det samme som brukernavn dersom spesifisert, ellers settes denne likt credentials
            addPropertyIfNotExist('db_password', property('db_username') ?: credentials.password)

            // benytter credentials username for dersom ikke spesifisert
            addPropertyIfNotExist('db_username', credentials.username)

            // schema er det samme som brukernavn dersom uspesifisert
            addPropertyIfNotExist('db_schema', property('db_username'))

            // db_oradata defaultes til prosjekt property med samme navn
            addPropertyIfNotExist('db_oradata', project.ext.properties['db_oradata'])

            // dersom db_oradata er blank settes denne til null
            if ("${this.property('db_oradata')}".trim().isEmpty()) {
                getProperties().put('db_oradata', null)
            }

            // oradataNN settes enten til standard verdi, eller til hva som er angitt i oradata
            addPropertyIfNotExist('db_oradata01', property('db_oradata') ?: 'F:\\Oradata')
            addPropertyIfNotExist('db_oradata02', property('db_oradata') ?: 'G:\\Oradata')
            addPropertyIfNotExist('db_oradata03', property('db_oradata') ?: 'J:\\Oradata')
            (4..9).each { int i ->
                addPropertyIfNotExist("db_oradata0${i}", property("db_oradata0${i - 3}"))
            }

            // schemas defaulter til brukernavn
            addPropertyIfNotExist('schemas', [getUsername()])

            // dumpfile
            addPropertyIfNotExist('dumpfile', "${schemas[0]}_${dateString}.DMP")


        }

        // setter konvensjonelle verdier
        // todo: properties med prefix kan trolig utgå (etter endring i versjon 1.2)..
        project.afterEvaluate {
            if (this.properties.containsKey("${propertyPrefix}db_username")) {

                // passord er det samme som brukernavn dersom uspesifisert
                addPropertyIfNotExist("${propertyPrefix}db_password", property("${propertyPrefix}db_username"))

                // schema er det samme som brukernavn dersom uspesifisert
                addPropertyIfNotExist("${propertyPrefix}db_schema", property("${propertyPrefix}db_username"))
            }

            // oradataNN settes enten til standard verdi, eller til hva som er angitt i oradata
            addPropertyIfNotExist("${propertyPrefix}db_oradata01", property('db_oradata') ?: 'F:\\Oradata')
            addPropertyIfNotExist("${propertyPrefix}db_oradata02", property('db_oradata') ?: 'G:\\Oradata')
            addPropertyIfNotExist("${propertyPrefix}db_oradata03", property('db_oradata') ?: 'J:\\Oradata')
            (4..9).each {
                addPropertyIfNotExist("${propertyPrefix}db_oradata0${it}", property("${propertyPrefix}db_oradata0${it - 3}"))
            }

        }


    }


    @Override
    OracleTasks getTasks() {
        return tasks;
    }

    // oracle attributes by convention... -->

    public String getUsername() {
        return credentials.username
    }

    public String getPassword() {
        return credentials.password
    }

    public String getHost() {
        return property('db_host')
    }

    public String getPort() {
        return property('db_port')
    }

    public String getSid() {
        return property('db_sid')
    }

    public String getTns() {
        String tns;
        if (this.properties.containsKey('tns')) {
            tns = property('tns')
        } else {
            tns = "${host}:${port}/${sid}".toUpperCase()    //defaults to an EZCONNECT connect string
            if (!tns.toUpperCase().endsWith('.STATKART.NO')) {
                tns += '.STATKART.NO'
            }
        }
        return tns
    }

    public String getDirectory() {
        if (this.properties.containsKey('directory')) {
            return property('directory')
        } else {
            throw new InvalidUserDataException("property 'directory' not set!")
        }
    }

    public Collection<String> getSchemas() {
        if (this.properties.containsKey('schemas')) {
            return property('schemas')
        } else {
            throw new InvalidUserDataException("property 'schemas' not set!")
        }
    }

    public Map<String, String> getSchemaMapping() {
        Map schemaMapping
        if (this.properties.containsKey('schemaMapping')) {
            schemaMapping = property('schemaMapping')
        } else {
            schemaMapping = [:]
            getSchemas().each { it ->
                schemaMapping[it] = it
            }
        }
        return schemaMapping
    }

    public String getDumpfile() {
        if (this.properties.containsKey('dumpfile')) {
            String dumpfile = property('dumpfile')
            if (!dumpfile.toUpperCase().endsWith('.DMP')) {
                dumpfile += '.DMP'
            }
            return dumpfile
        } else {
            throw new InvalidUserDataException("property 'dumpfile' not set!")
        }
    }


    public OracleImportTask importTask(Closure closure = null) {
        return importTask([:], closure);
    }

    public OracleImportTask importTask(Map params, Closure closure = null) {
        params.put(TASK_TYPE, OracleImportTask.class)
        OracleImportTask task = (OracleImportTask) task(params, 'Import', closure)


        task.conventionMapping('directory', { getDirectory() })
        task.conventionMapping('dumpfile', { getDumpfile() })
        task.conventionMapping('schemas', { getSchemas() })
        task.conventionMapping('schemaMapping', { getSchemaMapping() })
        task.conventionMapping('logfile', { "${getUsername()}.import.${getDateString()}.LOG" })
        task.conventionMapping('tableExistsAction', { 'REPLACE' })
        task.conventionMapping('transform', { 'SEGMENT_ATTRIBUTES:n,OID:n:TYPE' })

        task.conventionMapping('username', { getUsername() })
        task.conventionMapping('password', { getPassword() })
        task.conventionMapping('tns', { getTns() })

        return task
    }


    public OracleExportTask exportTask(Closure closure = null) {
        return exportTask([:], closure);
    }

    public OracleExportTask exportTask(Map params, Closure closure = null) {
        params.put(TASK_TYPE, OracleExportTask.class)
        OracleExportTask task = (OracleExportTask) task(params, 'Export', closure)

        task.conventionMapping('directory', { getDirectory() })
        task.conventionMapping('dumpfile', { getDumpfile() })
        task.conventionMapping('schemas', { getSchemas() })
        task.conventionMapping('logfile', { "${getDumpfile()}.export.${getDateString()}.LOG" })
        task.conventionMapping('exclude', { ['STATISTICS', 'INDEX'] })
        task.conventionMapping('compression', { 'DATA_ONLY' })

        task.conventionMapping('username', { getUsername() })
        task.conventionMapping('password', { getPassword() })
        task.conventionMapping('tns', { getTns() })

        return task
    }

    public Task task(Map params, String name, Closure closure = null) {
        validate()

        if (name == null || name.trim().isEmpty()) {
            throw new GradleException('name parameter not supplied for task!')
        }

        if (params.containsKey(TASK_TYPE)) {
            Object type = params.get(TASK_TYPE)
            if (!type instanceof Class) {
                String typeAsString = String.valueOf(type)
                try {
                    Class taskType = Class.forName(typeAsString.startsWith(this.class.package.name) ? type : "${this.class.package.name}.${typeAsString}", true, this.class.getClassLoader())
                    params.put(TASK_TYPE, taskType)
                } catch (ClassNotFoundException cnfe) {
                    throw new Exception("Unknown task type: ${type}", cnfe);
                }
            }
        }
        String taskName = "${prefix}${name}"
        Task task = project.task(params, taskName, closure);
        task.group = "Database"

        getTasks().addTask(name, task)
        return task;
    }

    private static String getDateString() {
        return new Date().format('yyyy-MM-dd_hhmmss')
    }

}
