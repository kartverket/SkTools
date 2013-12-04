package no.statkart.sktools.gradle.plugins.dbtools.testutils

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsConvention
import org.testng.Assert

/**
 * @since 1.3
 * @author Leif Lislegård
 */
class DbToolsPluginTestContext<T extends DbToolsPluginTestContext> extends DbToolsTestContext<T> {

    final Project project
    final DbtoolsConvention convention


    DbToolsPluginTestContext() {
        //forks a new project in a temp folder
        project = ProjectBuilder.builder().build()

        project.apply plugin: 'sktools-dbtools-plugin'

        convention = project.convention.plugins.db

    }



    T configureDatabasePlugin(Closure closure) {
        project.configureDatabasePlugin(closure)
        return (T) this
    }

    T configureProject(Closure closure) {
        project.configure(project, closure)
        return (T) this
    }


    File createNewFileWithDirsRelativeToProject(String path, String... texts) {
        File file = project.file(path)
        createFile(file, texts)
    }

    Task assertProjectContainsTask(String name, def message) {
        final Task task = project.tasks.findByPath(name)
        if (task == null) {
            Assert.fail "Forventet task med navn ${name} : ${message}"
        }
        return task

    }



}
