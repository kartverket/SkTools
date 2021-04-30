package no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.JavaExecSpec

/**
 * SKTOOLS-34 - Task som definerer patchversion.
 *
 * @author Leif Lislegård
 * @since 1.2
 */
class DefinePatchversionTask  extends DatabasePatchTask {
    protected static final Logger logger = Logging.getLogger(DefinePatchversionTask.class);


    @Internal
    String dbVersion

    @Internal
    String patchNumber


    @TaskAction
    def exec() {

        project.javaexec { JavaExecSpec spec ->

            /** {@link no.statkart.sktools.utils.databasepatcher.DatabasePatcher#main } */
            spec.setArgs(['defineVersion', getDbVersion()])

            if (getPatchNumber() != null) {
                spec.args(getPatchNumber())
            }

            configureDefaultSpec(spec)

            if (logger.isDebugEnabled()) {
                logger.debug('Executing databasepatcher with command: ' + (spec.getArgs() + spec.getAllJvmArgs()).join('\n\t'))
            }
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
