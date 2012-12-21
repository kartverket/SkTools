package no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch

import org.gradle.api.tasks.TaskAction
import org.gradle.process.JavaExecSpec

/**
 * Task som gir deg siste patchversjon.
 *
 * @author Leif Lislegård
 * @since 1.2
 */
class PrintPatchversionTask extends DatabasePatchTask {

    @TaskAction
    def exec() {

        project.javaexec { JavaExecSpec spec ->

            /** {@link no.statkart.sktools.utils.databasepatcher.DatabasePatcher#main } */
            spec.setArgs(['getVersion'])

            configureDefaultSpec(spec)

            logger.debug("Executing databasepatcher with command: " + (spec.getArgs() + spec.getAllJvmArgs()).join('\n\t'))
        }
    }


    //bruker ikke denne
    File getSqlFile() { }


}
