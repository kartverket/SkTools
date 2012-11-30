package no.statkart.sktools.gradle.plugins.dbtools.database.util

import org.gradle.api.tasks.TaskAction
import org.gradle.process.JavaExecSpec

/**
 * Task som gir deg siste patchversjon.
 *
 * @author Leif Lislegård
 * @since 1.2
 */
class PrintPatchversionTask extends DatabasePatchTask {

    //bruker ikke denne
    File sqlFile

    @TaskAction
    def exec() {

        project.javaexec { JavaExecSpec spec ->

            configureDefaultSpec(spec)

            /** {@link no.statkart.sktools.utils.databasepatcher.DatabasePatcher#main } */
            spec.setArgs(['getVersion'])
        }
    }


}
