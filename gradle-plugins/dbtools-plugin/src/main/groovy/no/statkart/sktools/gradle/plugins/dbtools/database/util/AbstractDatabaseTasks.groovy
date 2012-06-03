package no.statkart.sktools.gradle.plugins.dbtools.database.util

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownDomainObjectException

/**
 * Samling av tasks
 *
 * Denne klasen er noe kilen da all property aksess blir styrt via #getProperty
 */
abstract class AbstractDatabaseTasks<I extends AbstractDatabaseConvention> extends AbstractCollection<Task> implements DatabaseTasksInterface<I> {

    private final Map<String, Task> tasks = new LinkedHashMap<String, Task>()
    private final String relativePath
    private final I convention

    protected AbstractDatabaseTasks(String relativePath, I convention) {
        this.relativePath = relativePath
        this.convention = convention
    }

    def init(Project project) {
        configureTargets(project, 'Database')
        return this
    }

    //eksponerer disse internt
    protected Map<String, Task> getTasks() { return tasks }
    protected String getRelativePath() { return relativePath }
    protected I getConvention() { return convention }


    public Task addTask(String name, Task task) {
        getTasks().put(name, task)
        return task
    }


    protected def configureTargets(Project project, String groupString) {
        I conv = getConvention()
        def buildSQLTask = project.convention.plugins.db.buildSQLTask

        def sourceRoot = new File(project.getProjectDir(), 'src')
        def files = project.fileTree(new File(sourceRoot, getRelativePath()))
        files.include '**/*.sql'

        files.each() { File file ->
            String taskName = file.name.substring(0, file.name.length() - 4)
            String taskNameWithPrefix = conv.prefix + taskName

            SQLTask task = (SQLTask) project.task([type: SQLTask, dependsOn: [buildSQLTask], group:groupString], taskNameWithPrefix)
            task.sqlFile = new File(project.getBuildDir(), file.getAbsolutePath().replace(sourceRoot.getAbsolutePath(), ''))
            task.useTaskCredentials = false
            task.conventionMapping.with {
                map 'username', {conv.credentials.username}
                map 'password', {conv.credentials.password}
                map 'url', {conv.url}
                map 'driver', {conv.driver}
            }

            addTask(taskName, task)
        }

    }



    @Override
    Iterator<Task> iterator() {
        return tasks.values().iterator();
    }

    @Override
    int size() {
        return tasks.size();
    }

    /**
     * Implementerer denne slik at denne collectionen kan aksesseres via tasks['navn'] i groovy
     */
    public Task getAt(String name) throws UnknownDomainObjectException {
        return getByName(name);
    }

    public Task getByName(String name) throws UnknownDomainObjectException {
        Task t = findByName(name);
        if (t == null) {
            throw new UnknownDomainObjectException(String.format("%s with name '%s' not found.", "Task", name));
        }
        return t;
    }

    public Task findByName(String name) {
        return tasks.get(name);
    }

    /**
     * Implementerer denne slik at denne collectionen kan aksesseres via tasks.'navn' i groovy
     */
    Task getProperty(String name) {
        return findByName(name);
    }

}
