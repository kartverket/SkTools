package no.statkart.sktools.gradle.plugins.dbtools.database.oracle

import org.gradle.api.internal.ConventionTask
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 * Task for kjøring av import-script for oracle baser
 *
 * Det forutsettes at Oracle sqlClient er installer og finnes tilgjengelig på path.
 *
 * @author Leif Lislegård
 * @since 1.0
 */
class OracleImportTask extends ConventionTask {
    protected static final Logger logger = Logging.getLogger(OracleImportTask.class);

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

    //SKTOOLS-40
    @Optional
    @Input
    Integer parallel

    @Input
    String username

    @Input
    String password

    @Input
    String tns

    @Input
    String transform



    @TaskAction
    def exec() {

        def sout = new StringBuffer()
        def serr = new StringBuffer()
        List<String> command = ['impdp',
                "${getUsername()}/${getPassword()}@${getTns()}",
                "DIRECTORY=${getDirectory()}",
                "SCHEMAS=${getSchemas().join(',')}",
                "REMAP_SCHEMA=${getSchemaMapping().collect {key, value -> key + ':' + value}.join(',')}",
                "DUMPFILE=${getDumpfile()}",
                "LOGFILE=${getLogfile()}",
                "TABLE_EXISTS_ACTION=${getTableExistsAction()}",
                "TRANSFORM=${getTransform()}"
        ]

        if (getExclude() != null && !getExclude().isEmpty()) {
            command += "EXCLUDE=${getExclude().collect { Util.filterIncludeOrExcludeValue(it) }.join(',')}"
        }
        if (getInclude() != null && !getInclude().isEmpty()) {
            command += "INCLUDE=${getInclude().collect { Util.filterIncludeOrExcludeValue(it) }.join(',')}"
        }
        if (getParallel() != null) {
            command += "PARALLEL=${getParallel()}"
        }

        logger.lifecycle('Kaller impdp.exe med bruker ' + getUsername() + ', tns ' + getTns());

        logger.info 'Executing command: \n' + command.join(' ').replace(getPassword(), getPassword().replaceAll(/./, "*"))

        def impdb = Runtime.runtime.exec(command as String[], null, getProject().getProjectDir())

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
            logger.error( new StringBuilder()
                    .append("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                    .append("\n Exception during impdp:")
                    .append("\n").append(serr)
                    .append("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                    .toString()
            );
            throw new Exception("Exception during impdp. See log for details.");
        }

        logger.lifecycle("...oracle import OK")
    }


    public Logger getLogger() {
        return logger;
    }

}
