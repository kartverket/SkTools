package no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch

import org.gradle.api.tasks.TaskAction
import org.gradle.process.JavaExecSpec
import org.gradle.api.tasks.Input

/**
 * Task som setter flagg i databasen om indexer er up-to-date for gjeldende patch eller ikke.
 *
 * @author Leif Lislegård
 * @since 1.2
 */
class IndexesInSyncWithPatchTask extends DatabasePatchTask {


    @Input
    Boolean indexesUpToDate

    @TaskAction
    def exec() {

        project.javaexec { JavaExecSpec spec ->

            /** {@link no.statkart.sktools.utils.databasepatcher.DatabasePatcher#main } */
            spec.setArgs(['setIndexesInSyncWithPatch', getIndexesUpToDate()])

            configureDefaultSpec(spec)

            logger.debug("Executing databasepatcher with command: " + (spec.getArgs() + spec.getAllJvmArgs()).join('\n\t'))
        }
    }


    //bruker ikke denne
    File getSqlFile() { }

    @Override
    void validate() {
        super.validate();
    }
}
