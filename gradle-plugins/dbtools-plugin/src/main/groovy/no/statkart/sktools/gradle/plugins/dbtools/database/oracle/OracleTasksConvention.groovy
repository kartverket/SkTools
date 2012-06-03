package no.statkart.sktools.gradle.plugins.dbtools.database.oracle

import org.gradle.api.Project

import org.gradle.api.InvalidUserDataException
import no.statkart.sktools.gradle.plugins.dbtools.database.util.AbstractDatabaseConvention
import org.gradle.api.Task
import org.gradle.api.plugins.ExtraPropertiesExtension

/**
 * Convention object for Oracle database tools
 *
 * @author Leif Lislegård
 * @since 1.0
 */
class OracleTasksConvention extends AbstractDatabaseConvention {

    OracleTasks tasks;

    OracleTasksConvention(Project project, String propertyPrefix) {
        super(project, propertyPrefix, 'oracle.jdbc.OracleDriver')

        ExtraPropertiesExtension ext = project.getExtensions().getExtraProperties()
        
        url = project.properties[propertyPrefix + 'db_jdbc_url']
        driver = ext.getProperties().get(propertyPrefix + 'db_jdbc_driver', this.driver)

        /**
         * passord er det samme som brukernavn dersom uspesifisert
         */
        if (!ext.has(propertyPrefix + 'db_password')) {
            if (ext.has(propertyPrefix + 'db_username')) {
                ext.set(propertyPrefix + 'db_password', ext.get(propertyPrefix + 'db_username'))
                project.logger.info "setting property ${propertyPrefix + 'db_password'} to ${project.properties[propertyPrefix + 'db_password']}"
            }
        }

        /**
         * schema er det samme som brukernavn dersom uspesifisert
         */
        if (!ext.has(propertyPrefix + 'db_schema')) {
            if (ext.has(propertyPrefix + 'db_username')) {
                ext.set(propertyPrefix + 'db_schema', ext.get(propertyPrefix + 'db_username'))
                project.logger.info "setting property ${propertyPrefix + 'db_schema'} to ${project.properties[propertyPrefix + 'db_schema']}"
            }
        }


        //oradataNN settes enten til standard verdi, eller til hva som er angitt i oradata
        if (!ext.has(propertyPrefix + 'db_oradata01')) {
            ext.set(propertyPrefix + 'db_oradata01', ext.getProperties().get('db_oradata', 'F:\\Oradata'))
        }
        if (!ext.has(propertyPrefix + 'db_oradata02')) {
            ext.set(propertyPrefix + 'db_oradata02', ext.getProperties().get('db_oradata', 'G:\\Oradata'))
        }
        if (!ext.has(propertyPrefix + 'db_oradata03')) {
            ext.set(propertyPrefix + 'db_oradata03', ext.getProperties().get('db_oradata', 'J:\\Oradata'))
        }
        if (!ext.has(propertyPrefix + 'db_oradata04')) {
            ext.set(propertyPrefix + 'db_oradata04', ext.get(propertyPrefix + 'db_oradata01'))
        }
        if (!ext.has(propertyPrefix + 'db_oradata05')) {
            ext.set(propertyPrefix + 'db_oradata05', ext.get(propertyPrefix + 'db_oradata02'))
        }
        if (!ext.has(propertyPrefix + 'db_oradata06')) {
            ext.set(propertyPrefix + 'db_oradata06', ext.get(propertyPrefix + 'db_oradata03'))
        }
        if (!ext.has(propertyPrefix + 'db_oradata07')) {
            ext.set(propertyPrefix + 'db_oradata07', ext.get(propertyPrefix + 'db_oradata04'))
        }
        if (!ext.has(propertyPrefix + 'db_oradata08')) {
            ext.set(propertyPrefix + 'db_oradata08', ext.get(propertyPrefix + 'db_oradata05'))
        }
        if (!ext.has(propertyPrefix + 'db_oradata09')) {
            ext.set(propertyPrefix + 'db_oradata09', ext.get(propertyPrefix + 'db_oradata06'))
        }
    }

    /**
     * Printer viktige definerte proerties
     */
    public void printInfo() {
        80.times {project.print '*'}; project.println ''

        println "${this.class.simpleName} for \"${prefix}\":"
        println "  url -> ${url}"
        println "  driver -> ${driver}"
        if (credentials.hasUsername())
            println "  credentials.username -> ${credentials.username}"
        if (credentials.hasPassword())
            println "  credentials.password -> ${credentials.password}"
        println "  username -> ${username}"
        println "  host -> ${host}"
        println "  port -> ${port}"
        println "  sid -> ${sid}"
        println "  tns -> ${tns}"

        80.times {project.print '*'}; project.println ''
    }


    public OracleTasksConvention addTasks(String path) {
        if (tasks == null) {
            tasks = new OracleTasks(path, this)
            tasks.init(project)
        }
        return this
    }


    // oracle attributes by convention... -->

    public String getUsername() {
        return project.getExtensions().getExtraProperties().get(prefix + 'db_username')
    }
    public String getHost() {
        return project.getExtensions().getExtraProperties().get(prefix + 'db_host')
    }
    public String getPort() {
        return project.getExtensions().getExtraProperties().get(prefix + 'db_port')
    }
    public String getSid() {
        return project.getExtensions().getExtraProperties().get(prefix + 'db_sid')
    }

    public String getTns() {
        String tns;
        if (project.hasProperty('tns')) {
            tns = project.property('tns')
        } else {
            tns = "${host}:${port}/${sid}".toUpperCase()    //defaults to an EZCONNECT connect string
            if (!tns.toUpperCase().endsWith('.STATKART.NO')) {
                tns += '.STATKART.NO'
            }
        }
        return tns
    }
    public String getDirectory() {
        if (project.hasProperty('directory')) {
            return project.property('directory')
        }
        throw new InvalidUserDataException("property 'directory' not set!")
    }
    public Collection<String> getSchemas() {
        return Arrays.asList(getUsername())        //defaults to username
    }

    public Map<String, String> getSchemaMapping() {
        def schemaMapping = [:]
        getSchemas().each { it ->
            schemaMapping[it] = it
        }
        return schemaMapping
    }

    public String getDateString() {
        return new Date().format('yyyy-MM-dd_hhmmss')
    }

    public String getDumpfile() {
        return "${schemas[0]}_${dateString}.DMP"
    }

    public String getLogfileImport(String dumpfile) {
        return "${dumpfile}.import.${dateString}.LOG"
    }

    public String getLogfileExport(String dumpfile) {
        if (dumpfile == null) {
            dumpfile = getDumpfile()
        }
        return "${dumpfile}.export.${dateString}.LOG"
    }

    public Collection<String> getExcludesImport() {
        return 'USER'.split(',')
    }

    public Collection<String> getExcludesExport() {
        return 'STATISTICS,TABLESPACE_QUOTA,SYNONYM,VIEW'.split(',')
    }

    public String getCompression() {
        return 'DATA_ONLY'
    }

    public String getTableExistsAction() {
        return 'REPLACE'
    }


    @Deprecated
    public Task getImportTask() {
        project.println "importTask property is depricated since version 1.2 - Use tasks.import instead !"
        return tasks.getByName('import')
    }

    @Deprecated
    public Task getExportTask() {
        project.println "exportTask property is depricated since version 1.2 - Use tasks.export instead !"
        return tasks.getByName('export')
    }

    @Deprecated
    public Task getInfoTask() {
        project.println "infoTask property is depricated since version 1.2 - Use tasks.info instead !"
        return tasks.getByName('info')
    }


    public void addDefaultTasks(String groupString) {

        if (tasks.findByName('import')) return;

        Task importTask = project.task([type: OracleImportTask], "${prefix}Import") {
            group = groupString
            convention = this
            description = 'Import av dump via Oracles eget verktøy'
        }
        tasks.addTask('import', importTask)


        Task exportTask = project.task([type: OracleExportTask], "${prefix}Export") {
            group = groupString
            convention = this
            description = 'Export av dump via Oracles eget verktøy'
        }
        tasks.addTask('export', exportTask)

        Task infoTask = project.task("${prefix}Info") {
            group = groupString
            description = 'Viser gjeldende konfigurasjon'

            doLast {
                printInfo()
            }
        }
        tasks.addTask('info', infoTask)

    }
}
