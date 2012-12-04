package no.statkart.sktools.gradle.plugins.dbtools.database.util

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.JavaExecSpec
import org.gradle.api.tasks.Optional

/**
 * Task for patching av schema over JDBC.
 *
 * @author Leif Lislegård
 * @since 1.2
 */
class PatchTask extends DatabasePatchTask {

    @Input
    File sqlFile

    Boolean singlestep
    void setSinglestep(def value) {
        if (value != null) {
            singlestep = Boolean.parseBoolean(value)
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
            project.gradle.startParameter.mergedSystemProperties['singlestep']
        }
    }

    @TaskAction
    def exec() {

        project.javaexec { JavaExecSpec spec ->

            /** {@link no.statkart.sktools.utils.databasepatcher.DatabasePatcher#main } */
            spec.setArgs(['patch', sqlFile.absolutePath])

            configureDefaultSpec(spec)

            spec.systemProperties.put('singlestep', getSinglestep())
        }
    }

}
