package no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskAction
import org.gradle.process.JavaExecSpec

/**
 * Task som gir deg siste patchversjon.
 *
 * @author Leif Lislegård
 * @since 1.2
 */
@SuppressWarnings("UnnecessaryQualifiedReference")
class PrintPatchversionTask extends DatabasePatchTask {
    protected static final Logger logger = Logging.getLogger(PrintPatchversionTask.class);

    @TaskAction
    def exec() {

        project.javaexec { JavaExecSpec spec ->

            /** {@link no.statkart.sktools.utils.databasepatcher.DatabasePatcher#main } */
            spec.setArgs(['getVersion'])

            configureDefaultSpec(spec)

            logger.debug("Executing databasepatcher with command: " + (spec.getArgs() + spec.getAllJvmArgs()).join('\n\t'))
        }
    }


    File getSqlFile() { null /* na*/ }

    @Override
    void validate() {
        super.validate();
    }

    public Logger getLogger() {
        return logger;
    }

}
