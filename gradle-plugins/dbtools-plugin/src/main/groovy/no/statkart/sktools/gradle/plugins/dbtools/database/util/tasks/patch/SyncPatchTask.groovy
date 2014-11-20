package no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch

import org.gradle.api.tasks.TaskAction
import org.gradle.process.JavaExecSpec

/**
 * SKTOOLS-86 Task for re-patching av schema over JDBC.
 *
 * @author Leif Lislegård
 * @since 1.3
 */
@SuppressWarnings("UnnecessaryQualifiedReference")
class SyncPatchTask extends PatchTask {


    Collection<String> patchTypes

    @TaskAction
    def exec() {

        project.javaexec { JavaExecSpec spec ->

            /** {@link no.statkart.sktools.utils.databasepatcher.DatabasePatcher#main } */
            spec.setArgs(['syncPatch', sqlFile.absolutePath, '-types', patchTypes.join(',')])

            configureDefaultSpec(spec)

            spec.systemProperties.put('singlestep', getSinglestep())

            logger.debug("Executing databasepatcher with command: " + (spec.getArgs() + spec.getAllJvmArgs()).join('\n\t'))
        }
    }

    @Override
    void validate() {
        super.validate();

        if (patchTypes == null || patchTypes.isEmpty()) {
            throw new Exception("no patchTypes specified!")
        }
        !patchTypes.isEmpty()
    }

}

