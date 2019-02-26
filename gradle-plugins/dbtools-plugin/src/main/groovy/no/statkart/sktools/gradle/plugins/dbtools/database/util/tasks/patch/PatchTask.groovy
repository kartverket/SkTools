package no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.JavaExecSpec

/**
 * Task for patching av schema over JDBC.
 *
 * @author Leif Lislegård
 * @since 1.2
 */
@SuppressWarnings("UnnecessaryQualifiedReference")
class PatchTask extends DatabasePatchTask {
    protected static final Logger logger = Logging.getLogger(PatchTask.class);

    @Input
    File sqlFile

    Boolean singlestep
    void setSinglestep(def value) {
        if (value != null) {
            singlestep = Boolean.parseBoolean("${value}")
        } else {
            singlestep = null
        }
    }

    @Optional
    @Input
    boolean getSinglestep() {
        if (singlestep != null) {
            singlestep
        } else {
            project.gradle.startParameter.systemPropertiesArgs['singlestep']
        }
    }

    @TaskAction
    def exec() {

        project.javaexec { JavaExecSpec spec ->

            /** {@link no.statkart.sktools.utils.databasepatcher.DatabasePatcher#main } */
            spec.setArgs(['patch', sqlFile.absolutePath])

            configureDefaultSpec(spec)

            spec.systemProperties.put('singlestep', getSinglestep())

            if (logger.isDebugEnabled()) {
                logger.debug('Executing databasepatcher with command: ' + (spec.getArgs() + spec.getAllJvmArgs()).join('\n\t'))
            }

        }
    }

    @Override
    void validate() {
        super.validate();

        if (getSqlFile() == null) {
            throw new Exception("sqlFile må angis!")
        }

        if (!getSqlFile().exists()) {
            throw new Exception("File does not exist! sqlFile=${project.relativePath(getSqlFile())}")
        }
    }


    public Logger getLogger() {
        return logger;
    }

}

