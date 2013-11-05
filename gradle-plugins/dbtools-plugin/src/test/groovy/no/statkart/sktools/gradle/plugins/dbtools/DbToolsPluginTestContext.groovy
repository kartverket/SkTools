package no.statkart.sktools.gradle.plugins.dbtools

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsConvention
import no.statkart.sktools.gradle.plugins.dbtools.testutils.DbToolsTestContext

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

    File createNewFileWithDirsRelativeToProject(String path, String... texts) {
        File file = project.file(path)
        createFile(file, texts)
    }




}
