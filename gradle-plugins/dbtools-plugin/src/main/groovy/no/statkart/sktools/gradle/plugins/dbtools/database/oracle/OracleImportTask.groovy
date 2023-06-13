package no.statkart.sktools.gradle.plugins.dbtools.database.oracle

import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

import static no.statkart.sktools.gradle.plugins.dbtools.database.oracle.OracleTasksConvention.parallelArgumentProvider

/**
 * Task for kjøring av import-script for oracle baser
 *
 * Det forutsettes at Oracle sqlClient er installer og finnes tilgjengelig på path.
 *
 * @author Leif Lislegård
 * @since 1.0
 */
class OracleImportTask extends DefaultTask {
    protected static final Logger logger = Logging.getLogger(OracleImportTask.class);

    @Internal
    final Property<String> directory = project.getObjects().property(String)

    @Internal
    final Property<String> dumpfile = project.getObjects().property(String)

    @Internal
    final ListProperty<String> schemas = project.getObjects().listProperty(String)

    @Internal
    final MapProperty<String, String> schemaMapping = project.getObjects().mapProperty(String, String)

    @Internal
    final Property<String> logfile = project.getObjects().property(String)

    @Internal
    final Property<String> tableExistsAction = project.getObjects().property(String).convention('REPLACE')

    @Internal
    final ListProperty<String> exclude = project.getObjects().listProperty(String)

    @Internal
    final ListProperty<String> include = project.getObjects().listProperty(String)

    @Internal
    final Property<Integer> parallel = project.getObjects().property(Integer).convention(parallelArgumentProvider(project))

    @Internal
    final Property<String> username = project.getObjects().property(String)

    @Internal
    final Property<String> password = project.getObjects().property(String)

    @Internal
    final Property<String> tns = project.getObjects().property(String)

    @Internal
    final Property<String> transform = project.getObjects().property(String).convention('SEGMENT_ATTRIBUTES:n,OID:n:TYPE')



    @TaskAction
    def exec() {

        def sout = new StringBuffer()
        def serr = new StringBuffer()
        List<String> command = ['impdp',
                "${username.get()}/${password.get()}@${tns.get()}",
                "DIRECTORY=${directory.get()}",
                "SCHEMAS=${schemas.get().join(',')}",
                "REMAP_SCHEMA=${schemaMapping.get().collect {key, value -> key + ':' + value}.join(',')}",
                "DUMPFILE=${dumpfile.get()}",
                "LOGFILE=${logfile.get()}",
                "TABLE_EXISTS_ACTION=${tableExistsAction.get()}",
                "TRANSFORM=${transform.get()}"
        ]

        if (!exclude.getOrElse([]).isEmpty()) {
            command += "EXCLUDE=${exclude.get().collect { Util.filterIncludeOrExcludeValue(it) }.join(',')}"
        }
        if (!include.getOrElse([]).isEmpty()) {
            command += "INCLUDE=${include.get().collect { Util.filterIncludeOrExcludeValue(it) }.join(',')}"
        }
        if (parallel.isPresent()) {
            command += "PARALLEL=${parallel.get()}"
        }

        logger.lifecycle('Calling impdp with user ' + username.get() + ', tns ' + tns.get());

        if (logger.isInfoEnabled()) {
            logger.info('Executing command: \n' + command.join(' ').replace(password.get(), '***'))
        }
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

    @Internal
    Logger getLogger() {
        return logger;
    }

}
