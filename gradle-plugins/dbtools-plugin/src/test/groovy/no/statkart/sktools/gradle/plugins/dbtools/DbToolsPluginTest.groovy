package no.statkart.sktools.gradle.plugins.dbtools

import org.testng.annotations.Test
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert
import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsConvention

/**
 *
 */
class DbToolsPluginTest {


    /**
     * Tester registrering av pluginen
     */
    @Test
    void testAppplyPlugin() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()

        project.apply plugin:'sktools-dbtools-plugin'

        Assert.assertTrue(project.convention.plugins.db instanceof DbtoolsConvention)

    }

}
