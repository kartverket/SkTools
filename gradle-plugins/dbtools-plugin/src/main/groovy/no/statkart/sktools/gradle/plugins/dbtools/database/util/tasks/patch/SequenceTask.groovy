package no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch

import org.gradle.api.DefaultTask
import org.gradle.api.Task

/**
 * Workaround i gradle da gradle 1.0 ikke har noen støtte for å deklarere rekkefølge på tasker.
 *
 * Algoritmen legger til et nytt lag av tasker med navn i stigende rekkefølge.
 * Dette da gradle eksekverer disse i stigende rekkefølge (ved ikke rekursive avhengigheter)
 *
 * @author Leif Lislegård
 * @since 1.2
 */
class SequenceTask extends DefaultTask {

    private int idx = 0

    @Override
    Task dependsOn(Object... paths) {
        def wrapperTask = project.task(String.format('%s_step%02d', name, ++idx))
        wrapperTask.dependsOn(paths)
        return super.dependsOn(wrapperTask)
    }
}
