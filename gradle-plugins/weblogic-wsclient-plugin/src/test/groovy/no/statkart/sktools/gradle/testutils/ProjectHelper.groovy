package no.statkart.sktools.gradle.testutils

import org.gradle.api.Project
import org.gradle.api.ProjectState
import org.gradle.api.Task
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.testfixtures.ProjectBuilder
import org.testng.Assert

import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Hjelpemetoder til bruk i testing
 *
 * @author Leif Lislegård
 */
class ProjectHelper {

    final Project project

    ProjectHelper(Project project) {
        this.project = project
    }


    ProjectHelper configureProject(Closure closure) {
        if (closure != null) {
            project.configure(project, closure);
        }
        return this
    }


    /**
     * Bygger et nytt prosjekt. Prosjektet blir deretter konfigurert med closure hvor {@code project} er delegert.
     * @param closure
     */
    static ProjectHelper build(Closure closure) {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()
        return build(project, closure)
    }


    static ProjectHelper build(Project project, Closure closure) {
        ProjectHelper projectHelper = new ProjectHelper(project)
        if (closure) {
            closure.setResolveStrategy(Closure.DELEGATE_FIRST);
            closure.setDelegate(project)
            closure()
        }
        return projectHelper
    }

    /**
     * Kall denne der en avhenger av å ha kjørt {@link Project#afterEvaluate(Closure) afterEvaluate actions} for prosjektet.
     *
     * Eksekverer alle actions som er registrert på prosjektet + evt subprosjekter dersom parameterisert.
     */
    ProjectHelper initializeProject(boolean initializeSubprojects = false) {
        _initializeProject(project, initializeSubprojects)
        return this
    }

    private void _initializeProject(Project project, boolean initializeSubprojects) {
        ProjectState state = project.getState()
        ((ProjectInternal) project).getProjectEvaluationBroadcaster().afterEvaluate(project, state)
        if (initializeSubprojects) {
            project.getSubprojects().each {
                _initializeProject(it, initializeSubprojects)
            }
        }
    }



    /**
     * Setter properties på prosjektet.
     * Dersom prosjektet ikke allerede har propertyen, så legges den til ext.properties
     */
    void setProjectProperties(Map<String, ?> properties) {
        properties?.each {
            if (project.hasProperty(it.key)) {
                project.setProperty(it.key, it.value)
            } else {
                getExt().set(it.key, it.value);
            }
        }
    }

    ExtraPropertiesExtension getExt() {
        return project.getExtensions().getExtraProperties()
    }

    /**
     * Finner samtlige tasker som angitt task er avhengige av i task-grafen.
     */
    List<String> findDependsOnTaskNames(String taskName) {
        List<String> dependsOnTaskNames = []

        Stack<Task> unresolvedTasks = new Stack<Task>()
        unresolvedTasks.push(project.getTasks().getByName(taskName))

        while (!unresolvedTasks.isEmpty()) {
            Task task = unresolvedTasks.pop()
            dependsOnTaskNames.add(task.getName())
            task.getTaskDependencies().getDependencies(task).each {
                unresolvedTasks.push(it)
            }
        }

        return dependsOnTaskNames.reverse()
    }

    /**
     * Setter conventional {@code WEBLOGIC_HOME} og {@code WEBLOGIC_VERSION} property for prosjekt
     */
    public ProjectHelper withConventionalWEBLOGIC() {
        project.ext.set('WEBLOGIC_HOME', TestKitBase.testProperties.get('WEBLOGIC_HOME'))
        project.ext.set('WEBLOGIC_VERSION', TestKitBase.testProperties.get('WEBLOGIC_VERSION'))

        return this
    }



    /**
     * Debug string
     */
    public String toString() {
        return project.getProjectDir()
    }

}
