package no.statkart.sktools.gradle.plugins.webstart

import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.WebstartProjectBuilder
import org.gradle.api.Project

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileTree
import org.gradle.testfixtures.ProjectBuilder
import org.testng.Assert
import org.testng.annotations.Test

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Test av {@link WebstartPlugin}
 *
 * @author Leif Lislegård
 * @author Tor Egil R. Strand
 */
class WebstartPluginTest {

    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void testAppplyPlugin() {
        //forks a new project in a temp folder
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'sktools-webstart-plugin'

        Assert.assertNotNull(project.convention.plugins.webstart)
    }

    /**
     * Tester generering av tom, default konfigurasjon.
     *
     * PS: merk at denne konfigurasjonen ikke er deploybar.
     */
    @Test
    void testDefaultWebstart() {
        //forks a new project in a temp folder
        ProjectHelper projectHelper = WebstartProjectBuilder.builder().withName('root').applyWebstartPlugin().build()
        projectHelper.setProjectProperties(version: 101)

        projectHelper.configureProject {
            webstart {
                jnlp {
                    resources {
                        jars {
                            files(projectHelper.gradleJars[1])
                        }
                    }
                }
            }
        }

        projectHelper.initializeProject()

        def convention = projectHelper.project.convention.plugins.webstart

        projectHelper.executeTask('webstart')

        //sjekker at filer er blitt opprettet
        projectHelper.assertFileExists("build/webstart/" + projectHelper.project.name + '.jnlp')
    }



    /**
     * Tester og demonstrerer angivelse av konfigurasjon for dependencies/resouces.
     */
    @Test
    void testConventionConfigurationResources() {


        ProjectHelper projectHelper = WebstartProjectBuilder.builder().withName('root').applyWebstartPlugin().build()
        projectHelper.setProjectProperties(version: '2.0')

        ProjectHelper aProjectHelper = WebstartProjectBuilder.builder().withName('projectA').withParent(projectHelper).applyJavaPlugin().build()
        aProjectHelper.setProjectProperties(version: '1.0')

        ProjectHelper bProjectHelper = WebstartProjectBuilder.builder().withName('projectB').withParent(projectHelper).applyJavaPlugin().build()
        bProjectHelper.setProjectProperties(version: '1.2')

        File wsClientRuntimeJar = bProjectHelper.project.file('../wsClientRuntime-1.0.jar')
        assert wsClientRuntimeJar.createNewFile()

        bProjectHelper.configureProject {
            dependencies {
                runtime files('../wsClientRuntime-1.0.jar')
            }
        }
        aProjectHelper.configureProject {
            dependencies {
                runtime project(':projectB')    //dependency on projectB
            }
        }

        File webstartHelperJar = projectHelper.project.file('webstartHelper.jar')
        assert webstartHelperJar.createNewFile()
        projectHelper.configureProject {
            webstart {
                jnlp {
                    description 'Description client1'
                    title 'Client1 title'
                    resources {
                        jars {
                            files(webstartHelperJar)
                            project(path: ':projectA')
                        }
                        systemProperties pop1: 'test', prop2: 'test2', 'jnlp.versionEnabled':true
                    }
                }
            }
        }

        projectHelper.initializeProject()

        projectHelper.executeTask('assemble')

        final Project project = projectHelper.project

        File warPath = project.war.archivePath
        Assert.assertTrue(warPath.exists())

        ZipFile warFile = new ZipFile(warPath)
        List<String> entryNames = Collections.list(warFile.entries()).collect { it.name }
        warFile.close()

        Assert.assertTrue(entryNames.containsAll(['root.jnlp','lib/webstartHelper__Vunknown.jar','lib/wsClientRuntime__V1.0.jar','lib/projectA__V1.0.jar','lib/projectB__V1.2.jar']))
    }


}