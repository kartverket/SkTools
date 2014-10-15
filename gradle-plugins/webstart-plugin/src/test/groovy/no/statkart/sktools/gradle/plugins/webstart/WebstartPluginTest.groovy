package no.statkart.sktools.gradle.plugins.webstart

import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.WebstartProjectBuilder
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.Assert
import org.testng.annotations.Test

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
    void testApplyPlugin() {
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
                client {
                    jarDependencies files(projectHelper.gradleJars[1])
                    jnlp {
                    }
                }
            }
        }

        projectHelper.initializeProject()

        projectHelper.executeTask('genClientJnlp')

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
            configurations {
                webstartJars
            }

            dependencies {
                webstartJars files(webstartHelperJar)
                webstartJars project(path: ':projectA')
            }

            webstart {
                client {
                    mainJar 'wsClientRuntime'
                    jarDependencies configurations.webstartJars
                    jnlp {
                        description 'Client1 description'
                        title 'Client1 title'
                        resources {
                            systemProperties pop1: 'test', prop2: 'test2', 'jnlp.versionEnabled': true
                        }
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

        Assert.assertTrue(entryNames.containsAll(['root.jnlp', 'lib/webstartHelper__Vunknown.jar', 'lib/wsClientRuntime__V1.0.jar', 'lib/projectA__V1.0.jar', 'lib/projectB__V1.2.jar']))
    }

    /**
     * Tester problem med duplikate jar-filer dersom man har flere klienter.
     */
    @Test
    void testDuplicates() {
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
            configurations {
                webstartJars
            }

            dependencies {
                webstartJars files(webstartHelperJar)
                webstartJars project(path: ':projectA')
            }

            webstart {
                client1 {
                    mainJar 'wsClientRuntime'
                    jarDependencies configurations.webstartJars
                    jnlp {
                        jnlpFilename 'client1.jnlp'
                        description 'Client1 description'
                        title 'Client1 title'
                    }
                }
                client2 {
                    mainJar 'wsClientRuntime'
                    jarDependencies configurations.webstartJars
                    jnlp {
                        jnlpFilename 'client2.jnlp'
                        description 'Client2 description'
                        title 'Client2 title'
                    }
                }
            }
        }

        projectHelper.initializeProject()

        projectHelper.executeTask('assemble')

        final Project project = projectHelper.project

        if (project.gradle.gradleVersion.split(/\./)[1].toInteger() < 7) {
            println "...skipping test due to gradle version ${project.gradle.gradleVersion} < 1.7"
            return
        }

        File warPath = project.war.archivePath
        Assert.assertTrue(warPath.exists())

        ZipFile warFile = new ZipFile(warPath)
        List<String> entryNames = Collections.list(warFile.entries()).collect { it.name }
        warFile.close()

        Set<String> entryNames2 = new HashSet<String>(entryNames)
        Assert.assertEquals(entryNames2.size(), entryNames.size())
    }


    /**
     * SKTOOLS-118: main jar
     */
    @Test
    void jarFilesDefaultsAsMainJars() {
        final ProjectHelper projectHelper = WebstartProjectBuilder.builder().withName('root').applyWebstartPlugin().build()

        final File wsClientRuntimeJar = projectHelper.project.file('../wsClientRuntime-1.0.jar')
        wsClientRuntimeJar.createNewFile()

        final File wsClientExtrasJar = projectHelper.project.file('../wsClientExtras-1.0.jar')
        wsClientExtrasJar.createNewFile()

        projectHelper.configureProject {
            configurations {
                clientRuntime1
                clientRuntime2
            }

            dependencies {
                clientRuntime1 files('../wsClientRuntime-1.0.jar')
                clientRuntime2 files('../wsClientRuntime-1.0.jar', '../wsClientExtras-1.0.jar')
            }

            webstart {
                client1 {
//                    mainJar 'wsClientRuntime' //no declaration here - testing default behaviour
                    jarDependencies configurations.clientRuntime1
                    jnlp {
                        description 'Client1 description'
                        title 'Client1 title'
                    }
                }
                client2 {
//                    mainJar 'wsClientRuntime' //no declaration here - testing default behaviour
                    jarDependencies configurations.clientRuntime2
                    jnlp {
                        description 'Client2 description'
                        title 'Client2 title'
                    }
                }
            }

        }

        projectHelper.initializeProject()

        //verifying client1 with single jar
        projectHelper.project.with {
            Assert.assertEquals(tasks['genClient1Jnlp'].mainJar.files.size(), 1, 'mainJar')
            Assert.assertEquals(tasks['genClient1Jnlp'].mainJar.asPath, files(wsClientRuntimeJar).asPath, 'mainJar')
            Assert.assertEquals(tasks['genClient1Jnlp'].jarResources.asPath, files(wsClientRuntimeJar).asPath, 'jarResources')
        }

        //verifying client2 with multiple jars
        projectHelper.project.with {
            Assert.assertEquals(tasks['genClient2Jnlp'].mainJar.files.size(), 2, 'mainJar') //SKTOOLS-118: default behaviour to treat all as main jars (although this might lead to an exeption at runtime...)
            Assert.assertEquals(tasks['genClient2Jnlp'].mainJar.asPath, files(wsClientRuntimeJar, wsClientExtrasJar).asPath, 'mainJar')
            Assert.assertEquals(tasks['genClient2Jnlp'].jarResources.asPath, files(wsClientRuntimeJar, wsClientExtrasJar).asPath, 'jarResources')
        }
    }


    /**
     * SKTOOLS-118: main jar
     */
    @Test
    void canSpecifyMainJar() {
        final ProjectHelper projectHelper = WebstartProjectBuilder.builder().withName('root').applyWebstartPlugin().build()

        final File wsClientRuntimeJar = projectHelper.project.file('../wsClientRuntime-1.0.jar')
        wsClientRuntimeJar.createNewFile()

        final File wsClientExtrasJar = projectHelper.project.file('../wsClientExtras-1.0.jar')
        wsClientExtrasJar.createNewFile()

        projectHelper.configureProject {
            configurations {
                clientRuntime
            }

            dependencies {
                clientRuntime files('../wsClientRuntime-1.0.jar', '../wsClientExtras-1.0.jar')
            }

            webstart {
                client1 {
                    mainJar 'wsClientRuntime' //SKTOOLS-118: shorthand filter notation
                    jarDependencies configurations.clientRuntime
                    jnlp {
                        description 'Client1 description'
                        title 'Client1 title'
                    }
                }
                client2 {
                    mainJar {
                        it.name.contains('wsClientRuntime') //SKTOOLS-118: filter notation as closure
                    }
                    jarDependencies configurations.clientRuntime
                    jnlp {
                        description 'Client2 description'
                        title 'Client2 title'
                    }
                }
            }

        }

        projectHelper.initializeProject()

        ['genClient1Jnlp', 'genClient2Jnlp'].reverseEach { def genJnlpTaskName ->

            //clients with multiple jars should have only one main jar specified
            projectHelper.project.with {
                Assert.assertEquals(tasks[genJnlpTaskName].mainJar.files.size(), 1, "${genJnlpTaskName}.mainJar")
                Assert.assertEquals(tasks[genJnlpTaskName].mainJar.asPath, files(wsClientRuntimeJar).asPath, "${genJnlpTaskName}.mainJar")
                Assert.assertEquals(tasks[genJnlpTaskName].jarResources.asPath, files(wsClientRuntimeJar, wsClientExtrasJar).asPath, "${genJnlpTaskName}.jarResources")
            }

        }

    }


    /**
     * Skal kunne spesifisere ekstra manifest informasjon som legges på før signering.
     */
    @Test
    void canSpecifyExtraManifestAttributes() {
        final ProjectHelper projectHelper = WebstartProjectBuilder.builder().withName('root').applyWebstartPlugin().build()

        projectHelper.configureProject {
            apply: 'java'

            webstart {
                client1 {
                    jarDependencies configurations.runtime
                    manifestAttributes codebase: 'https://*', dummy: 'testValue'
                    jnlp {
                        description 'Client1 description'
                        title 'Client1 title'
                    }
                }
                client2 {
                    jarDependencies configurations.runtime
                    jnlp {
                        description 'Client2 description'
                        title 'Client2 title'
                    }
                }
            }

        }


        projectHelper.initializeProject()

        final JarSigner signTask = projectHelper.project.tasks['signClient1'] as JarSigner
        Assert.assertNotNull(signTask.getManifestAttributes())
        Assert.assertEquals(signTask.getManifestAttributes().size(), 2)
        Assert.assertEquals(signTask.getManifestAttributes(), [codebase: 'https://*', dummy: 'testValue'])

    }

}