package no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.specs.Spec
import org.gradle.api.internal.TaskInternal
import org.gradle.api.tasks.TaskDependency

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

    public boolean propagateOnlyIf = true

    @Override
    Task dependsOn(Object... paths) {
        def wrapperTask = project.task(String.format('%s_step%02d', name, ++idx))
        wrapperTask.dependsOn(paths)
        return super.dependsOn(wrapperTask)
    }

    SequenceTask() {
        TaskInternal thisTask = this
        project.gradle.taskGraph.whenReady { def taskGraph ->
            if (propagateOnlyIf && taskGraph.hasTask(thisTask)) {
                propagateOnlyIf(thisTask)
            }
        }
    }

    /**
     * Propagierer Task.onlyIf{} til dependent objekter.
     * OnlyIf vil kun slå ut dersom denne task-sekvensen blir utført
     */
    void propagateOnlyIf(Task task, int depth = 0) {
        TaskInternal thisTask = this
        TaskDependency taskDependencies = task.getTaskDependencies()

        if (task != thisTask && depth > 1 ) {
            task.onlyIf(new Spec<Task>() {
                boolean isSatisfiedBy(Task childTask) {
                    return thisTask.getOnlyIf().isSatisfiedBy(thisTask)
                }
            })
        }
        for (Task child : taskDependencies.getDependencies(task)) {
            propagateOnlyIf(child, depth+1)
        }
    }

}
