package no.statkart.sktools.gradle.plugins.dbtools.database.util

import org.gradle.api.Task
import org.gradle.api.UnknownDomainObjectException

/**
 * Samling av tasks
 *
 * Denne klassen er noe kilen da all property aksess blir styrt via #getProperty
 */
abstract class AbstractDatabaseTasks<I> extends AbstractCollection<Task> {

    private final Map<String, Task> tasks = new LinkedHashMap<String, Task>()
    private final I convention

    protected AbstractDatabaseTasks(I convention) {
        this.convention = convention
    }

    //eksponerer disse internt
    protected Map<String, Task> getTasks() { return tasks }
    protected I getConvention() { return convention }


    public Task addTask(String name, Task task) {
        getTasks().put(name, task)
        return task
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
