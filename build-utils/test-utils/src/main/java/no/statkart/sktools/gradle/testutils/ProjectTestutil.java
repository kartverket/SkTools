package no.statkart.sktools.gradle.testutils;

import org.gradle.api.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class ProjectTestutil {

    /**
     * Finner samtlige tasker som angitt task er avgengige av i task-grafen.
     */
    public static List<Task> extractDependsOn(Task aTask) {
        List<Task> dependsOnTaskNames = new ArrayList<>();

        Stack<Task> unresolvedTasks = new Stack<Task>();
        unresolvedTasks.push(aTask);

        while (!unresolvedTasks.isEmpty()) {
            Task task = unresolvedTasks.pop();
            dependsOnTaskNames.add(task);
            for (Task dependency : task.getTaskDependencies().getDependencies(task)) {
                unresolvedTasks.push(dependency);
            }
        }

        Collections.reverse(dependsOnTaskNames);
        return dependsOnTaskNames;
    }

}
