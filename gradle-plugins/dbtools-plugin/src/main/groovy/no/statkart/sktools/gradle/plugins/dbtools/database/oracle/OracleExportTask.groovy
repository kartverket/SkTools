package no.statkart.sktools.gradle.plugins.dbtools.database.oracle

import org.gradle.api.internal.ConventionTask
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 * Task for kjøring av export-script for oracle baser
 *
 * Det forutsettes at Oracle sqlClient er installer og finnes tilgjengelig på path.
 *
 * @author Leif Lislegård
 * @since 1.0
 */
class OracleExportTask extends ConventionTask {
    protected static final Logger logger = Logging.getLogger(OracleExportTask.class);

    @Input
    String directory

    @Input
    String dumpfile

    @Input
    Collection<String> schemas

    @Input
    String logfile

    @Input
    @Optional
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
    String compression



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
        List<String> command = ['expdp',
                "USERID=${getUsername()}/${getPassword()}@${getTns()}",
                "DIRECTORY=${getDirectory()}",
                "SCHEMAS=${getSchemas().join(',')}",
                "DUMPFILE=${getDumpfile()}",
                "LOGFILE=${getLogfile()}",
                "COMPRESSION=${getCompression()}"
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

        def impdb = Runtime.runtime.exec(command as String[], null, getProject().getProjectDir())

        logger.lifecycle('Calling expdp with user ' + getUsername() + ', tns ' + getTns());

        if (logger.isInfoEnabled()) {
            logger.info 'Executing command: \n' + command.join(' ').replace(getPassword(), getPassword().replaceAll(/./, "*"))
        }

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
                    .append("\n Exception during expdp:")
                    .append("\n").append(serr)
                    .append("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                    .toString()
            );
            throw new Exception("Exception during expdp. See log for details.");
        }

        logger.lifecycle("...oracle export OK")
    }


    public Logger getLogger() {
        return logger;
    }

}

