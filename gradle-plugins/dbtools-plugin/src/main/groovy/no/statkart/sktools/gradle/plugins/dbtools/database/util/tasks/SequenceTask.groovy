package no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks

import no.statkart.sktools.gradle.plugins.dbtools.database.util.SQLTask
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.internal.TaskInternal
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskDependency

/**
 * Workaround i gradle da gradle 1.0 ikke har noen støtte for å deklarere rekkefølge på tasker.
 *
 * Algoritmen setter {@link Task#mustRunAfter} for å instrumentere ønsket eksekverings-rekkefølge i Gradle
 * basert på sekvenser.
 *
 * SKTOOLS-152:
 * Det er at krav om at sekvenser ikke danner nye sykler da Gradle baseres på en DAG (Directed Acyclic Graph) av tasker.
 *
 * @author Leif Lislegård
 * @since 1.2
 */
class SequenceTask extends DefaultTask {

    @Internal
    List<Object> dependsOnList = new ArrayList<>()

    public boolean propagateOnlyIf = true

    @Override
    Task dependsOn(Object... paths) {
        Collections.addAll(dependsOnList, paths)
        return super.dependsOn(paths)
    }

    SequenceTask() {
        final SequenceTask thisTask = this
        project.gradle.taskGraph.whenReady { def taskGraph ->
            if (propagateOnlyIf && taskGraph.hasTask(thisTask)) {
                propagateOnlyIf(thisTask)
            }
        }
        project.gradle.projectsEvaluated {
            final LinkedHashSet sequence = []
            final Context context = new Context(thisTask)
            this.dependsOnList.each { Object child ->
                Task childTask
                if (child instanceof Task) {
                    childTask = child
                } else if (child instanceof CharSequence) {
                    childTask = project.tasks.getByPath(child.toString())
                }


                if (childTask != null) {
                    def mySequence = new ArrayList(sequence)

                    logger.info('...modding {} mustRunAfter {}', childTask, mySequence)
                    childTask.mustRunAfter(sequence.clone())

                    processChildrenOf(childTask, mySequence, context)
                }

                sequence << child
            }
        }
    }

    class Context {
        final Stack<Task> stack
        final LinkedHashSet processedChildren = []

        public Context(Task root) {
            stack = [root] as Stack
        }

        public String toString() {
            stack.collect{it.path}.join(' -> ')
        }
    }

    /**
     * @param parent
     * @param runAfter
     */
    void processChildrenOf(Task parent, List runAfter, Context context) {
        def children = parent.taskDependencies.getDependencies(parent)
        context.stack.push(parent)
        for (Task task : children) {
            if (!runAfter.isEmpty()) {
                if (children.isEmpty() || task instanceof SQLTask) {
                    if (runAfter.contains(task)) {
                        logger.error "CYCLE DETECTED ${context}\n on ${task.path} mustRunAfter ${runAfter}\n"
                    }
                    logger.info('...modding {} mustRunAfter {} due to {}', task, runAfter, context)
                    task.mustRunAfter(runAfter)
                }
            }

            processChildrenOf(task, runAfter, context)
        }
        context.stack.pop()
        context.processedChildren << parent
    }

    static Collection findChildrenOfTask(Task task) {
        List children = []
        for (Task child : task.taskDependencies.getDependencies(task)) {
            children.add(child);
            children.addAll(findChildrenOfTask(child))
        }
        return children
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
