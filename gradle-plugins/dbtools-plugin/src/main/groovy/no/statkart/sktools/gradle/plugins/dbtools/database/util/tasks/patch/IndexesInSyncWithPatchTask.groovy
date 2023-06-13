package no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.JavaExecSpec

/**
 * Task som setter flagg i databasen om indexer er up-to-date for gjeldende patch eller ikke.
 *
 * @author Leif Lislegård
 * @since 1.2
 */
@SuppressWarnings("UnnecessaryQualifiedReference")
class IndexesInSyncWithPatchTask extends DatabasePatchTask {
    protected static final Logger logger = Logging.getLogger(IndexesInSyncWithPatchTask.class);

    @Internal
    final Property<Boolean> indexesUpToDate = project.getObjects().property(Boolean)

    @TaskAction
    def exec() {

        project.javaexec { JavaExecSpec spec ->

            /** {@link no.statkart.sktools.utils.databasepatcher.DatabasePatcher#main } */
            spec.setArgs(['setIndexesInSyncWithPatch', indexesUpToDate.get().toString()])

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
