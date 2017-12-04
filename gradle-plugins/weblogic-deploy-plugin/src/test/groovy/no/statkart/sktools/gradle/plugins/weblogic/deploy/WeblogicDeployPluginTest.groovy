package no.statkart.sktools.gradle.plugins.weblogic.deploy

import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.GradleProjectBuilder
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.Assert
import org.testng.annotations.Test

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
    void testConfiguration() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder().build {
            apply plugin: 'sktools-weblogic-deploy-plugin'

            weblogicDeploy {
                protocol = "a"
                host = "b"
                port = "c"

                deployTask('myDeploy', description: 'Testkonfigurasjon av deploy task')
                undeployTask('myUndeploy', description: 'Testkonfigurasjon av undeploy task') { url = 't3://test:port'}
            }
        }

        Project project = projectHelper.project

        assert project.tasks['myDeploy'].group == 'Deployment'
        assert project.tasks['myUndeploy'].group == 'Deployment'

        assert project.tasks['myDeploy'].description == 'Testkonfigurasjon av deploy task'
        assert project.tasks['myUndeploy'].description == 'Testkonfigurasjon av undeploy task'


        assert project.tasks['myDeploy'].url == 'a://b:c'
        assert project.tasks['myUndeploy'].url == 't3://test:port'
    }

    /**
     * Tester angivelse av dependsOn
     */
    @Test
    void testDependsOn() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder().build {
            apply plugin: 'sktools-weblogic-deploy-plugin'

            project.task 'myTask'

            weblogicDeploy {
                deployTask('deploy', dependsOn: [myTask])
                undeployTask('undeploy') {
                    dependsOn deployTask
                }
            }
        }

        Project project = projectHelper.project

        assert project.tasks['deploy'].dependsOn.contains(project.tasks['myTask'])
        assert project.tasks['undeploy'].dependsOn.contains(project.tasks['deploy'])
    }

    /**
     * SKTOOLS-164/SKTOOLS-165: Forenklet konfigurasjon via WEBLOGIC_VERSION.
     * Dersom denne property er gitt, så legges en standard classpath til task.
     */
    @Test
    void testConventionalWeblogicClasspath() {
        ProjectHelper projectHelper = GradleProjectBuilder.builder().build {
            apply plugin: 'sktools-weblogic-deploy-plugin'

            weblogicDeploy {
                deployTask('deploy')
                undeployTask('undeploy')
            }
        }

        Project project = projectHelper.project

        assert project.tasks['deploy'].classpath.isEmpty()
        assert project.tasks['undeploy'].classpath.isEmpty()

        projectHelper.withConventionalWEBLOGIC()

        assert !project.tasks['deploy'].classpath.isEmpty()
        assert !project.tasks['undeploy'].classpath.isEmpty()
    }

}
