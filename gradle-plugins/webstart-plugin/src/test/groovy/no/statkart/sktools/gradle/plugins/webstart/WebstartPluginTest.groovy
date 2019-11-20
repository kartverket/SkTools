package no.statkart.sktools.gradle.plugins.webstart


import no.statkart.sktools.gradle.testutils.TestKitBase
import org.assertj.core.util.Preconditions
import org.gradle.api.Project
import org.testng.Assert
import org.testng.annotations.Test

import java.util.zip.ZipFile

import static java.util.Collections.list
import static no.statkart.sktools.gradle.testutils.ProjectTestutil.extractDependsOn
import static no.statkart.sktools.gradle.testutils.SampleJarTestutil.writeSampleJar
import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.entry

/**
 * Test av {@link WebstartPlugin}
 *
 * @author Leif Lislegård
 * @author Tor Egil R. Strand
 */
class WebstartPluginTest extends TestKitBase {

    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void testApplyPlugin() {
        //forks a new project in a temp folder
        Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-webstart-plugin'
        }

        Assert.assertNotNull(project.convention.plugins.webstart)
    }

    /**
     * Tester generering av tom, default konfigurasjon.
     *
     * PS: merk at denne konfigurasjonen ikke er deploybar.
     */
    @Test
    void testDefaultWebstart() {
        writeSampleJar(file("lib/simple.jar"))

        writeFile("build.gradle", """
            plugins {
              id 'sktools.webstart'
            }
            version = 101

            webstart {
                client {
                    jarDependencies files('lib/simple.jar')
                    jnlp {
                    }
                }
            }
        """)

        assertNoFailures(testGradleBuild(":genClientJnlp"))

        //sjekker at filer er blitt opprettet
        assertThat(file("build/webstart/" + rootProjectName() + '.jnlp')).exists()
    }

    /**
     * Tester og demonstrerer angivelse av konfigurasjon for dependencies/resouces.
     */
    @Test
    void testConventionConfigurationResources() {

        writeFile("settings.gradle",
            "rootProject.name = 'root'",
            "include ':projectA'",
            "include ':projectB'",
        )

        writeFile("projectA/build.gradle", """
            plugins {
              id 'java'
            }
            version = '1.0'
           
            dependencies {
                runtime project(':projectB')    //dependency on projectB
            }

        """)

        File wsClientRuntimeJar = file('wsClientRuntime-1.0.jar')
        assert wsClientRuntimeJar.createNewFile()

        writeFile("projectB/build.gradle", """
            plugins {
              id 'java'
            }
            version = '1.2'
            
            dependencies {
                runtime files('../wsClientRuntime-1.0.jar')
            }
        """)


        File webstartHelperJar = file('webstartHelper.jar')
        assert webstartHelperJar.createNewFile()

        writeFile("build.gradle", """
            plugins {
              id 'sktools.webstart'
            }
            version = '2.0'
            
            configurations {
                webstartJars
            }

            dependencies {
                webstartJars files('webstartHelper.jar')
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
        """)

        testGradleBuild(":assemble")


        File warPath = file("build/libs/root-2.0.war")
        Assert.assertTrue(warPath.exists())

        ZipFile warFile = new ZipFile(warPath)
        try {
            assertThat(list(warFile.entries()))
                .extractingResultOf("getName")
                .contains('root.jnlp', 'lib/webstartHelper__Vunknown.jar', 'lib/wsClientRuntime__V1.0.jar', 'lib/projectA__V1.0.jar', 'lib/projectB__V1.2.jar')
        } finally {
            warFile.close()
        }
    }



    /**
     * SKTOOLS-118: main jar
     */
    @Test
    void jarFilesDefaultsAsMainJars() {
        final File wsClientRuntimeJar = file('wsClientRuntime-1.0.jar')
        wsClientRuntimeJar.createNewFile()

        final File wsClientExtrasJar = file('wsClientExtras-1.0.jar')
        wsClientExtrasJar.createNewFile()

        Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-webstart-plugin'

            configurations {
                clientRuntime1
                clientRuntime2
            }

            dependencies {
                clientRuntime1 files('wsClientRuntime-1.0.jar')
                clientRuntime2 files('wsClientRuntime-1.0.jar', 'wsClientExtras-1.0.jar')
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

        //verifying client1 with single jar
        assertThat(project.tasks['genClient1Jnlp'].mainJar.files as Iterable).containsExactly(wsClientRuntimeJar)
        assertThat(project.tasks['genClient1Jnlp'].jarResources.files as Iterable).containsExactly(wsClientRuntimeJar)

        //verifying client2 with multiple jars
        assertThat(project.tasks['genClient2Jnlp'].mainJar.files as Iterable).containsExactlyInAnyOrder(wsClientRuntimeJar, wsClientExtrasJar)
        assertThat(project.tasks['genClient2Jnlp'].jarResources.files as Iterable).containsExactlyInAnyOrder(wsClientRuntimeJar, wsClientExtrasJar)
    }


    /**
     * SKTOOLS-118: main jar
     */
    @Test
    void canSpecifyMainJar() {
        final File wsClientRuntimeJar = file('wsClientRuntime-1.0.jar')
        wsClientRuntimeJar.createNewFile()

        final File wsClientExtrasJar = file('wsClientExtras-1.0.jar')
        wsClientExtrasJar.createNewFile()

        Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-webstart-plugin'

            configurations {
                clientRuntime
            }

            dependencies {
                clientRuntime files('wsClientRuntime-1.0.jar', 'wsClientExtras-1.0.jar')
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

        //clients with multiple jars should have only one main jar specified
        assertThat(project.tasks['genClient1Jnlp'].mainJar.files as Iterable)
            .containsExactly(wsClientRuntimeJar);
        assertThat(project.tasks['genClient2Jnlp'].mainJar.files as Iterable)
            .containsExactly(wsClientRuntimeJar);
    }


    /**
     * Skal kunne spesifisere ekstra manifest informasjon som legges på før signering.
     */
    @Test
    void canSpecifyExtraManifestAttributes() {
        Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-webstart-plugin'
            apply plugin: 'java'

            webstart {
                client1 {
                    jarDependencies configurations.runtime
                    manifestAttributes codebase: 'https://*', dummy: 'testValue'
                    jnlp {
                        description 'Client1 description'
                        title 'Client1 title'
                    }
                }
            }
        }

        final JarSigner signTask = project.tasks['signClient1'] as JarSigner
        assertThat(signTask.getManifestAttributes())
            .containsOnly(
                entry('codebase', 'https://*'),
                entry('dummy', 'testValue'))
    }

    @Test //regression
    void jnlpTaskDependsOnSignJars() {
        Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-webstart-plugin'

            webstart {
                client {
                    jnlp {
                    }
                }
            }
        }

        assertThat(extractDependsOn(project.tasks.genClientJnlp))
            .contains(project.tasks.signClient)
    }

    /**
     * Clean task sletter cache dir for signerte jar filer
     */
    @Test
    void cleanJarSignerCacheDeletesCacheDir() {
        File customCacheDir = file("customCacheDir")
        writeFile("customCacheDir/willBeDeleted.txt")

        writeFile("build.gradle", """
            plugins {
              id 'sktools.webstart'
            }
            
            webstart {
                client {
                    sign(cacheDir: file('customCacheDir'))
                }
            }
        """)

        Preconditions.checkArgument(customCacheDir.exists(), "Riktig testoppsett")
        testGradleBuild(":cleanJarSignerCaches")

        assertThat(customCacheDir).doesNotExist();
    }

}