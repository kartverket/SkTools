package no.statkart.sktools.gradle.plugins.ideaextensions

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.Assert
import org.testng.annotations.Test

/**
 * @author Leif Lislegård
 */
class IdeaExtensionPluginTest {

    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void testAppplyPlugin() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'sktools-ideaextensions-plugin'


        assert project.ideaExtensions != null
        Assert.assertTrue(project.ideaExtensions instanceof IdeaExtensionsPluginExtension)

    }

    /**
     * Tester og demonstrerer angivelse av konfigurasjon
     */
    @Test(enabled = false)
    void testConventionConfiguration() {
        //todo
    }

}
