package no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.JavaExecSpec

import org.gradle.api.tasks.Optional

/**
 * SKTOOLS-34 - Task som definerer patchversion.
 *
 * @author Leif Lislegård
 * @since 1.2
 */
class DefinePatchversionTask  extends DatabasePatchTask {

    @Input
    String dbVersion

    @Optional
    @Input
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
