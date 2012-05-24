package no.statkart.sktools.gradle.plugins.weblogic.deploy

import org.testng.annotations.Test
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.Assert
import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.WeblogicDeployProjectBuilder

/**
 * Test av weblogic deploy plugin.
 *
 * @author Tor Egil R. Strand
 * @since 1.2
 */
class WeblogicDeployPluginTest {
    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void testAppplyPlugin() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'sktools-weblogic-deploy-plugin'

        assert project.convention.plugins.weblogicDeployConvention != null
        Assert.assertTrue(project.convention.plugins.weblogicDeployConvention instanceof WeblogicDeployConvention)
    }

    /**
     * Tester og demonstrerer angivelse av konfigurasjon
     */
    @Test
    void testConventionConfiguration() {
        ProjectHelper projectHelper = WeblogicDeployProjectBuilder.builder().applyJavaPlugin().applyWeblogicDeployPlugin().build()
        WeblogicDeployConvention convention = (WeblogicDeployConvention) projectHelper.project.convention.plugins.get(WeblogicDeployPlugin.WEBLOGIC_DEPLOY_CONVENTION_NAME)

        projectHelper.initializeProject()

        projectHelper.configureProject {
            weblogicDeploy {
                protocol = "a"
                host = "b"
                port = "c"
            }
        }

        assert convention.weblogicDeploy.protocol == "a"
        assert convention.weblogicDeploy.host == "b"
        assert convention.weblogicDeploy.port == "c"
    }
}
