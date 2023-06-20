package no.statkart.sktools.gradle.plugins.wsdocgen

import no.statkart.sktools.gradle.testutils.TestKitBase
import org.gradle.api.Project
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GFileUtils
import org.testng.annotations.Test

import java.nio.file.Files
import java.nio.file.StandardCopyOption

import static no.statkart.sktools.gradle.testutils.filewriter.WsDocgenTestutilFilewriter.writeInterfaceServiceWSBean
import static no.statkart.sktools.gradle.testutils.filewriter.WsDocgenTestutilFilewriter.writeSimpleDemoServiceWSBean
import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.contentOf
import static org.testng.Assert.assertEquals
import static org.testng.Assert.assertNotNull
import static org.testng.Assert.assertNull
import static org.testng.Assert.assertTrue

/**
 * Test av {@link WsDocGenPlugin}
 *
 * <p>
 *     For testing av generering av dokumentasjon se {@link no.statkart.sktools.utils.wsdocgen.processor.WSDocProcessorTest}
 * </p>
 *
 * @author Leif Lislegård
 */
class WsDocGenPluginTest extends TestKitBase {

    static void writeServiceLayout(File file) {
        GFileUtils.parentMkdirs(file)
        Files.copy(WsDocGenPluginTest.class.getResourceAsStream('/DefaultTransform.xsl'), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    /**
     * Tester registrering av plugin via navn
     */
    @Test
    void applyPlugin() {
        Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-wsdocgen-plugin'
        }

        assertThat(project.getPlugins().getPlugin(WsDocGenPlugin.class)).isNotNull()
    }

    /**
     * Tester syntax før SKTOOLS-213
     */
    @Test
    void legacySyntax() {
        final Project project = projectBuilder().build().tap {
            apply plugin: 'java'
            apply plugin: 'sktools-wsdocgen-plugin'

            sourceSets {
                main.wsdoc.group { targetPath 'gen/doc' }
                other {
                    wsdoc {
                        group { targetPath 'gen/doc2' }
                    }
                }

            }
        }
        assertEquals project.sourceSets.main.wsdoc.group.targetPath.get(), project.file('gen/doc')
        assertEquals project.sourceSets.other.wsdoc.group.targetPath.get(), project.file('gen/doc2')
    }


    @Test
    void genWsDocGeneratesDocumentationForAllSourceSets() {
        writeFileUTF8("build.gradle", """\
            plugins {
              id 'java'
              id 'sktools.wsdoc'
            }

            sourceSets {
                main.wsdoc {
                    serviceXslt 'transform.xsl'
                }
                other.wsdoc {
                    serviceXslt 'transform.xsl'
                }
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                compileOnly 'com.sun.xml.ws:jaxws-rt:2.3.6'
                otherCompileOnly 'com.sun.xml.ws:jaxws-rt:2.3.6'
            }

        """)

        writeSimpleDemoServiceWSBean(file("src/main/java")) //generates simple source file
        writeSimpleDemoServiceWSBean(file("src/other/java")) //generates simple source file
        writeServiceLayout(file('transform.xsl'))

        assertNoFailures(testGradleBuild(WsDocGenPlugin.GEN_TASK_NAME))

        assertThat(file('build/main/wsdoc/TestService.html')).exists()
        assertThat(file('build/other/wsdoc/TestService.html')).exists()
    }


    @Test
    void tasksForVanillaConfiguration() {
        final Project project = projectBuilder().build().tap {
            apply plugin: 'java'
            apply plugin: 'sktools-wsdocgen-plugin'

            sourceSets {
                main.wsdoc {}
            }
        }

        assertNotNull project.tasks.findByName('genWsdoc'), "gen task"
        assertNotNull project.tasks.findByName('genMainWsdoc'), "gen task for source set"

        assertTrue project.tasks['genWsdoc'].dependsOn.collect{it.name}.contains('genMainWsdoc')
    }

    /**
     * Samletask for source sets skal ikke komme med dersom tomme wsdoc grupper (ikke annotert i konfigurasjon)
     */
    @Test
    void noTasksForNoneAnnotatedSourceSets() {
        final Project project = projectBuilder().build().tap {
            apply plugin: 'java'
            apply plugin: 'sktools-wsdocgen-plugin'

            sourceSets {
                main {}
                other.wsdoc {}
            }
        }

        assertNotNull project.tasks.findByName('genWsdoc'), "gen task"

        //negative test
        assertNull project.tasks.findByName('genMainWsdoc'), "gen task for source set"

        //positive test
        assertNotNull project.tasks.findByName('genOtherWsdoc'), "gen task for source set"
    }

    @Test
    void sourceSetForVanillaConfiguration() {
        final Project project = projectBuilder().build().tap {
            apply plugin: 'java'
            apply plugin: 'sktools-wsdocgen-plugin'

            sourceSets {
                main.wsdoc {}
                other
            }
        }

        assertNotNull project.sourceSets.main.wsdoc.group
        assertNull project.sourceSets.other.wsdoc.group

        assertEquals project.sourceSets.main.wsdoc.group.targetPath.get(), project.file('build/main/wsdoc')
    }


    @Test
    void canCustomizeOutputLocation() {
        final Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-wsdocgen-plugin'

            sourceSets {
                multi {
                    wsdoc {
                        targetPath 'gen/doc'
                        targetPath 'gen/doc2'
                    }
                }
                main.wsdoc { /*default*/ }
                other.wsdoc { targetPath 'gen/doc' }
            }
        }

        //test override
        assertEquals project.sourceSets.other.wsdoc.group.targetPath.get(), project.file('gen/doc')
        assertEquals project.tasks.genOtherWsdoc.destinationDir, project.file('gen/doc')

        //test multiple groups
        assertEquals project.sourceSets.multi.wsdoc.group.targetPath.get(), project.file('gen/doc2')
        assertEquals project.tasks.genMultiWsdoc.destinationDir, project.file('gen/doc2')
    }


    @Test
    void generatedFilesHasLookupPathWhenConfigured() {

        writeFileUTF8("build.gradle", """\
            plugins {
              id 'sktools.wsdoc'
            }

            sourceSets {
                main.wsdoc {
                    targetPath 'build/mydocs'
                    lookupPath '../wacky/path'
                    serviceXslt 'transform.xsl'
                }
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                compileOnly 'com.sun.xml.ws:jaxws-rt:2.3.6'
            }
        """)

        // eksempel-kildekode som har domene-klasse definert
        writeInterfaceServiceWSBean(file('src/main/java'))
        writeServiceLayout(file('transform.xsl'))

        assertNoFailures(testGradleBuild(WsDocGenPlugin.GEN_TASK_NAME))

        assertThat(file('build/mydocs/InterfaceService.html')).exists()
        assertThat(contentOf(file('build/mydocs/InterfaceService.html')))
            .describedAs("skal ha link som peker til domeneklasse (javadoc)")
            .contains("../wacky/path")
    }


    /**
     * Demonstrerer hvordan en kan spre kildekode over flere mapper
     */
    @Test
    void multipleSourceFoldersForSingleSourceSet() {

        //forks a new java project in a temp folder
        //ps: notice that the java plugin is applied after the plugin, at a  later stage.
        writeFileUTF8("build.gradle", """\
            plugins {
              id 'sktools.wsdoc'
              id 'java' //after
            }

            sourceSets.main {
                java.srcDir 'src/main/morejava'
                wsdoc {
                    targetPath 'build'
                    serviceXslt 'transform.xsl'
                }
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                compileOnly 'com.sun.xml.ws:jaxws-rt:2.3.6'
            }
        """)

        // eksempel-kildekode som har domene-klasse definert
        writeSimpleDemoServiceWSBean(file("src/main/java"))
        writeInterfaceServiceWSBean(file('src/main/morejava'))
        writeServiceLayout(file('transform.xsl'))


        assertNoFailures(testGradleBuild(WsDocGenPlugin.GEN_TASK_NAME))

        assertThat(file('build/InterfaceService.html')).exists()
        assertThat(file('build/TestService.html')).exists()
    }


    @Test
    void genWsdocIsUpToDate() {
        writeFileUTF8("build.gradle", """\
            plugins {
                id 'java'
                id 'sktools.wsdoc'
            }

            sourceSets.main {
                wsdoc {
                    serviceXslt 'transform.xsl'
                }
            }

            repositories {
                mavenCentral()
            }
            dependencies {
                compileOnly 'com.sun.xml.ws:jaxws-rt:2.3.6'
            }
        """)

        // eksempel-kildekode som har domene-klasse definert
        writeInterfaceServiceWSBean(file('src/main/java'))
        writeServiceLayout(file('transform.xsl'))


        BuildResult buildResult1 = testGradleBuild(':genWsdoc')

        assertThat(buildResult1.task(":genWsdoc").getOutcome()).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(file('build/main/wsdoc/InterfaceService.html')).exists()

        BuildResult buildResult2 = testGradleBuild(':genWsdoc')
        assertThat(buildResult2.task(":genWsdoc").getOutcome())
            .as("Task up to date ved andre gangs kjøring").isEqualTo(TaskOutcome.UP_TO_DATE)
        assertThat(file('build/main/wsdoc/InterfaceService.html')).exists()
    }
}
