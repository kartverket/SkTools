package no.statkart.sktools.gradle.plugins.dbtools.database.oracle

import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.internal.ConventionTask

/**
 * Task for kjøring av import-script for oracle baser
 *
 * Det forutsettes at Oracle sqlClient er installer og finnes tilgjengelig på path.
 *
 * @author Leif Lislegård
 * @since 1.0
 */
class OracleImportTask extends ConventionTask {



    @Input
    String directory

    @Input
    String dumpfile

    @Input
    List<String> schemas

    @Input
    Map<String, String> schemaMapping

    @Input
    String logfile

    @Input
    String tableExistsAction

    @Optional
    @Input
    Collection<String> exclude

    //SKTOOLS-30
    @Optional
    @Input
    Collection<String> include


    @Input
    String username

    @Input
    String password

    @Input
    String tns




    @TaskAction
    def exec() {

        def sout = new StringBuffer()
        def serr = new StringBuffer()
        List<String> command = ['impdp.exe',
                "${getUsername()}/${getPassword()}@${getTns()}",
                "DIRECTORY=${getDirectory()}",
                "SCHEMAS=${getSchemas().join(',')}",
                "REMAP_SCHEMA=${getSchemaMapping().collect {key, value -> key + ':' + value}.join(',')}",
                "DUMPFILE=${getDumpfile()}",
                "LOGFILE=${getLogfile()}",
                "TABLE_EXISTS_ACTION=${getTableExistsAction()}",
                'TRANSFORM=SEGMENT_ATTRIBUTES:n'
        ]

        if (getExclude() != null && !getExclude().isEmpty()) {
            command += "EXCLUDE=${getExclude().join(',')}"
        }
        if (getInclude() != null && !getInclude().isEmpty()) {
            command += "INCLUDE=${getInclude().join(',')}"
        }

        def impdb = Runtime.runtime.exec(command as String[], null, getProject().getProjectDir())

        logger.debug('Kaller impdp.exe med bruker ' + getUsername() + ', tns ' + getTns());

        logger.info 'Executing command: \n' + command.join(' ').replace(getPassword(), getPassword().replaceAll(/./, "*"))

        def running = true
        def bufferPrinter = {buffer ->
            def lastIndex = 0
            while (running) {
                def length = buffer.length()
                if (length > lastIndex) {
                    print buffer.subSequence(lastIndex, length)
                    lastIndex = length
                }
                Thread.sleep(100)
            }
        }
        Thread.start bufferPrinter.curry(sout)
        Thread.start bufferPrinter.curry(serr)

        impdb.consumeProcessOutput(sout, serr)
        try {
            impdb.waitFor()
        }
        finally {
            running = false
        }

        if (serr.toString().contains('ORA-') || impdb.exitValue()) {
            println '!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!'
            println 'Feil under kjøring av impdp.exe:'
            print serr
            println '!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!'
            System.exit(1)
        }


        println '...oracle import OK'
        print sout
    }

}
