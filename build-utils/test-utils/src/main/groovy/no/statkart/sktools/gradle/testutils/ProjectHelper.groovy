package no.statkart.sktools.gradle.testutils

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Task
import org.testng.Assert
import org.gradle.api.ProjectState
import org.gradle.api.internal.project.ProjectInternal

/**
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
     * Dependencies blir ikke eksekvert i noen bestemt rekkefølge. Rekkefølgenn kan derav divergere ifra gradles egen evaluering.
     */
    Task executeTask(String taskName) {
        return execute(project.tasks[taskName])
    }

    private void execute(Task task) {
        if (task.state.executed) return;
        task.getTaskDependencies().getDependencies(task).each {execute(it)}
        println "..executing task ${task.path}"
        task.execute()
    }

    /**
     * Delegates closure to project
     */
    def project(Closure closure) {
        closure.delegate =  project
        closure()
    }

    ProjectHelper configureProject(Closure closure) {
        project.configure(project, closure);
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

    private void _initializeProject(ProjectInternal project, boolean initializeSubprojects) {
        ProjectState state = project.getState()
        project.getProjectEvaluationBroadcaster().afterEvaluate(project, state)
        if (initializeSubprojects) {
            project.getSubprojects().each {
                _initializeProject(it, initializeSubprojects)
            }
        }
    }

    /**
     * Sets properties on project
     */
    void setProjectProperties(Map properties) {
        properties.each {
            project.setProperty(it.key, it.value);
        }
    }

    /**
     * Finner samtlige tasker som angitt task er avgengige av i task-grafen.
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
     * Setter {@code WEBLOGIC_HOME} property for prosjekt
     */
    ProjectHelper defineWEBLOGIC_HOME() {
        ['WEBLOGIC_HOME'].each { val ->
            if (!project.hasProperty(val) && System.getenv(val) != null) {
                project.setProperty(val, System.getenv(val))
            }
        }
        if (!project.hasProperty('WEBLOGIC_HOME')) {
            //todo: finne en løsning her
            println 'Setting WEBLOGIC_HOME for IntelliJ...'
            project.WEBLOGIC_HOME = 'C:\\bea_wls10.3.1'
        }

        return this
    }

    /**
     * Beregner en weblogic classpath for weblogic jar avhengigheter.
     */
    FileCollection getWeblogicClasspath() {
        return project.fileTree(dir: "${project.WEBLOGIC_HOME}", includes: [
                    'wlserver_10.3/server/lib/weblogic.jar',
                    'wlserver_10.3/server/lib/webservices.jar',
                    'wlserver_10.3/server/lib/wljmsclient.jar',
                    'modules/javax.annotation_*.jar/',
                    'modules/javax.ejb_3*.jar',
                    'modules/javax.interceptor_*.jar',
                    'modules/javax.servlet_*.jar',
                    'modules/javax.transaction_*.jar',
                    'modules/com.bea.core.transaction_*.jar',
                    'modules/com.bea.core.datasource*.jar',
                    'modules/glassfish.jaxws.rt*.jar',
            ]).stopExecutionIfEmpty() //feilsituasjon dersom WEBLOGIC_HOME ikke er riktig satt
    }

    public File assertFileNotExists(String path, Closure testClosure = null) {
        return assertFile(path, '', testClosure, false);
    }
    public File assertFileExists(String path, Closure testClosure = null) {
        return assertFile(path, '', testClosure, true);
    }
    public File assertFileExists(String path, String message, Closure testClosure = null) {
        return assertFile(path, message, testClosure, true);
    }

    private File assertFile(String path, String message, Closure testClosure, boolean expectFileExist) {
        File file = project.file(path)
        if (expectFileExist && !file.exists()) {
            Assert.fail("Forventet at filen ${path} finnes. ${message}")
        } else if (!expectFileExist && file.exists()) {
            Assert.fail("Forventet ikke at filen ${path} finnes. ${message}")
        }

        if (testClosure != null) {
            testClosure.call(file)
        }
        return file
    }

    public File assertFileExistsInBuildDir(String path, Closure testClosure = null) {
        return assertFileExistsInBuildDir(path, '', testClosure)
    }
    public File assertFileExistsInBuildDir(String path, String message, Closure testClosure = null) {
        File file = project.file(project.getBuildDirName() + '/' + path)
        if (!file.exists()) {
            Assert.fail("Forventet at filen ${path} finnes. ${message}")
        }
        if (testClosure != null) {
            testClosure.delegate = file
            testClosure()
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
}
