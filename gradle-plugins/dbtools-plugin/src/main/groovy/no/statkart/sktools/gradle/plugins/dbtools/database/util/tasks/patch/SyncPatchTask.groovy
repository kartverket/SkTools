package no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch

import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.JavaExecSpec

/**
 * SKTOOLS-86 Task for re-patching av schema over JDBC.
 *
 * @author Leif Lislegård
 * @since 1.3
 */
class SyncPatchTask extends PatchTask {

    /**
     * Hvilke patch-typer som skal kjøres inn.
     */
    @Internal
    final ListProperty<String> patchTypes = project.getObjects().listProperty(String)
        .convention(['INDEX', 'TYPE', 'PACKAGE', 'FUNCTION']) //SKTOOLS-86

    SyncPatchTask() {
        failOnWarning.set(false)  //SKTOOLS-86
    }

    @TaskAction
    def exec() {
        File sqlFile = mappedSqlFile()

        project.javaexec { JavaExecSpec spec ->

            /** {@link no.statkart.sktools.utils.databasepatcher.DatabasePatcher#main } */
            spec.setArgs(['syncPatch', sqlFile.absolutePath, '-types', patchTypes.get().join(',')])

            configureDefaultSpec(spec)

            spec.systemProperties.put('singlestep', singlestep.get())

            if (logger.isDebugEnabled()) {
                logger.debug('Executing databasepatcher with command: ' + (spec.getArgs() + spec.getAllJvmArgs()).join('\n\t'))
            }

        }
    }

    @Override
    void validate() {
        super.validate();

        if (patchTypes.getOrElse([]).isEmpty()) {
            throw new Exception("no patchTypes specified!")
        }
    }

}

