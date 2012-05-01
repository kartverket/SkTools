package no.statkart.sktools.gradle.plugins.webstart

import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.WebstartProjectBuilder
import org.gradle.api.Project

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.testfixtures.ProjectBuilder
import org.testng.Assert
import org.testng.annotations.Test

/**
 * Test av {@link WebstartPlugin}
 *
 * @author Leif Lislegård
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


        assert project.convention.plugins.webstart != null
        Assert.assertTrue(project.convention.plugins.webstart instanceof WebstartConvention)

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
                client {
                    jnlp {
                        resources {
                            jars {
                                files(projectHelper.gradleJars[1])
                            }
                        }
                    }
                }
            }
        }

        projectHelper.initializeProject()

        def convention = projectHelper.project.convention.plugins.webstart

        projectHelper.executeTask('genWebstart')

        //sjekker at filer er blitt opprettet
        projectHelper.assertFileExists("build/generated/webstart/" + convention.clients[0].jnlpFile.name)
        projectHelper.assertFileExists("build/generated/webstart/lib/version.xml")
    }



    /**
     * Tester og demonstrerer angivelse av konfigurasjon.
     * <p>
     * <p>
     * Hopper her over testing av resources/dependencies. Se {@link #testConventionConfigurationResources()} for dette.
     */
    @Test
    void testConventionConfigurationNoResources() {
        ProjectHelper projectHelper = WebstartProjectBuilder.builder().withName('root').applyWebstartPlugin().build()
        projectHelper.setProjectProperties(version: 101, description: 'Project description')

        projectHelper.configureProject {
            webstart {
                client {
                    //default configuration
                }
                client {
                    signJars
                    outputPath 'gen/client1'
                    jnlpFile 'goodClient.jnlp'
                    jnlp {
                        description 'Description client1'
                        title 'Client1 title'
                        vendor 'MyCompany'
                        homepage 'http://intra.statkart.no'
                        applicationMainClass 'some.pkg.MyApplicationLauncher'
                        resources {
                            javaRuntime '1.5+'
                        }
                    }
                }
                client {
                    signJars false
                    outputPath project.file('gen/client2')
                    jnlpFile 'client2.jnlp'
                    jnlp.description project.description
                    jnlp {
                        title 'Client2 title'
                        vendor 'SomeCompany'
                        homepage '\\\\intra\\somefolder\\someproject\\index.html'
                        application.mainClass 'AnotherLauncher'
                        resources {
                            javaRuntime '1.6+', '128m', '256m', 'http://some.download/location' //shortcut notation
                            runtime {   //same as above but only with vmArgs set.
                                version '1.6+'
                                href 'http://some.download/location'
                                xms '128m'
                                xmx '256m'
                                vmArgs 'someargs'
                            }
                        }
                    }
                }
            }
        }

        projectHelper.initializeProject()


        final Project project = projectHelper.project
        WebstartConvention convention = project.convention.plugins[WebstartPlugin.CONVENTION_NAME]


        assert convention.clients[0].signJars == true
        assert convention.clients[1].signJars == true
        assert convention.clients[2].signJars == false

        assert convention.clients[0].outputDir == project.file('build/generated/webstart')
        assert convention.clients[1].outputDir == project.file('gen/client1')
        assert convention.clients[2].outputDir == project.file('gen/client2')

        assert convention.clients[0].jnlpFilePath == 'root.jnlp'
        assert convention.clients[1].jnlpFilePath == 'goodClient.jnlp'
        assert convention.clients[2].jnlpFilePath == 'client2.jnlp'

        assert convention.clients[0].jnlp.description == ""
        assert convention.clients[1].jnlp.description == "Description client1"
        assert convention.clients[2].jnlp.description == "Project description"

        assert convention.clients[0].jnlp.title == "root v101"
        assert convention.clients[1].jnlp.title == "Client1 title"
        assert convention.clients[2].jnlp.title == "Client2 title"

        assert convention.clients[0].jnlp.vendor == "Statens Kartverk"
        assert convention.clients[1].jnlp.vendor == "MyCompany"
        assert convention.clients[2].jnlp.vendor == "SomeCompany"

        assert convention.clients[0].jnlp.homepage == null
        assert convention.clients[1].jnlp.homepage == "http://intra.statkart.no"
        assert convention.clients[2].jnlp.homepage == "\\\\intra\\somefolder\\someproject\\index.html"

        assert convention.clients[0].jnlp.hasApplication() == false
        assert convention.clients[1].jnlp.application.mainClass == 'some.pkg.MyApplicationLauncher'
        assert convention.clients[2].jnlp.application.mainClass == 'AnotherLauncher'

        assert convention.clients[0].jnlp.resources[0].runtimes.size() == 1 //defaults adds one 1.6 runtime
        convention.clients[0].jnlp.resources[0].runtimes.each {
            assert it.version == "1.6+"
            assert it.href == "http://java.sun.com/products/autodl/j2se"
            assert it.xms == null
            assert it.xmx == "512"
            assert it.vmArgs == null
        }
        assert convention.clients[1].jnlp.resources[0].runtimes.size() == 1
        convention.clients[1].jnlp.resources[0].runtimes.each {
            assert it.version == '1.5+'
            assert it.href == "http://java.sun.com/products/autodl/j2se"
            assert it.xms == null
            assert it.xmx == "128m"
            assert it.vmArgs == null
        }
        assert convention.clients[2].jnlp.resources[0].runtimes.size() == 2
        convention.clients[2].jnlp.resources[0].runtimes.each {
            assert it.version == '1.6+'
            assert it.href == "http://some.download/location"
            assert it.xms == "128m"
            assert it.xmx == "256m"
        }
        assert convention.clients[2].jnlp.resources[0].runtimes[0].vmArgs == null
        assert convention.clients[2].jnlp.resources[0].runtimes[1].vmArgs == "someargs"
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
                client {
                    signJars false
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
        }

        projectHelper.initializeProject()


        final Project project = projectHelper.project
        WebstartConvention convention = project.convention.plugins[WebstartPlugin.CONVENTION_NAME]


        Configuration configuration = project.getConfigurations().getByName(WebstartPlugin.CONFIGURATION_NAME)
        Set<Dependency> allDependencies = configuration.getDependencies()

        //sjekker at alle dependencies er registrert til webstart konfigurasjon
        convention.clients[0].jnlp.resources.each { ResourcesConfiguration resources ->
            resources.jarDependencies.each {
                assert allDependencies.contains(it); //alle dependencies skal finnes i konfigurasjon
            }

        }

        assert convention.clients[0].jnlp.resources[0].systemProperties == [pop1: 'test', prop2: 'test2', 'jnlp.versionEnabled':true]

        //sjekker at jarFiler kommer med for dependencies
        def client0resources0Files = configuration.fileCollection(convention.clients[0].jnlp.resources[0].jarDependencies.toArray()).files.with {
            assert it.contains(webstartHelperJar)
            assert it.contains(wsClientRuntimeJar)
            assert it.contains(aProjectHelper.project.file('build/libs/projectA-1.0.jar'))
            assert it.contains(bProjectHelper.project.file('build/libs/projectB-1.2.jar'))
        }


    }


}