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

    /**
     * Emulerer eksekvering av task med dependencies.
     *
     * Dependencies blir ikke eksekvert i noen bestemt rekkefølge. Rekkefølge kan derav divergere ifra gradles egen evaluering.
     */
    Task executeTask(String taskName) {
        return execute(project.tasks[taskName])
    }

    private Task execute(Task task, final Set evaluatedTasks = [] as HashSet) {
        if (evaluatedTasks.contains(task)) return task;
        evaluatedTasks << task
        task.getTaskDependencies().getDependencies(task).each {execute(it, evaluatedTasks)}
        println "..executing task ${task.path}"

        /* I gradle 4.6 så får man
         "java.lang.IllegalStateException: Task information is not available, as this task execution graph has not been populated."
         dersom man forsøker execute() på en task uten actions.
         */
        if (!task.getActions().isEmpty()) {
            task.execute()
        }
        task
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


    public File assertFileNotExists(Object path, Closure testClosure = null) {
        return assertFile(path, '', testClosure, false);
    }
    public File assertFileExists(Object path, Closure testClosure = null) {
        return assertFile(path, '', testClosure, true);
    }
    public File assertFileExists(Object path, String message, Closure testClosure = null) {
        return assertFile(path, message, testClosure, true);
    }

    private File assertFile(Object path, String message, Closure testClosure, boolean expectFileExist) {
        File file = project.file(path)
        if (expectFileExist && !file.exists()) {
            Assert.fail("Forventet at filen ${path} finnes. ${message}")
        } else if (!expectFileExist && file.exists()) {
            Assert.fail("Forventet ikke at filen ${path} finnes. ${message}")
        }

        if (testClosure != null) {
            testClosure.setDelegate(this)
            testClosure.setResolveStrategy(Closure.DELEGATE_FIRST)
            testClosure.call(file)
        }
        return file
    }


    public Task assertTaskExecutedNotSkipped(String taskName, String message = '', Closure testClosure = null) {
        Task task = project.getTasks().getByName(taskName);
        Assert.assertTrue(task.state.executed, "Forventet at task ${task.path} ble eksekvert. ${message}" )
        Assert.assertFalse(task.state.skipped, "Forventet at task ${task.path} ikke ble skippet men fullført. ${message}" )

        if (testClosure != null) {
            testClosure.call(task)
        }
        return task
    }

    /**
     * Debug string
     */
    public String toString() {
        return project.getProjectDir()
    }

    public static void copyFile(File srcFile, File destFile) throws IOException {
        destFile.getParentFile().mkdirs()
        Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)
    }

    public static void copyFile(InputStream srcStream, File destFile) throws IOException {
        destFile.getParentFile().mkdirs()
        Files.copy(srcStream, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

}
