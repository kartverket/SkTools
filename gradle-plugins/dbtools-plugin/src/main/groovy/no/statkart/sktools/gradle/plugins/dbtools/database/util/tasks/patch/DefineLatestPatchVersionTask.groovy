package no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.JavaExecSpec

/**
 * SKTOOLS-87 - Task for setting av siste eksisterende patchversjon.
 *
 * @author Leif Lislegård
 * @since 1.3
 */
class DefineLatestPatchVersionTask extends PatchTask {

    @Input
    File sqlFile


    @TaskAction
    def exec() {

        project.javaexec { JavaExecSpec spec ->

            /** {@link no.statkart.sktools.utils.databasepatcher.DatabasePatcher#main } */
            spec.setArgs(['setLatestVersionFromPatchfile', sqlFile.absolutePath])

            configureDefaultSpec(spec)

            logger.debug("Executing databasepatcher with command: " + (spec.getArgs() + spec.getAllJvmArgs()).join('\n\t'))
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

}
